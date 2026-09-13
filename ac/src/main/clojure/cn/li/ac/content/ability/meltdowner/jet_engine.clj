(ns cn.li.ac.content.ability.meltdowner.jet-engine
  "JetEngine parity rewrite.

  Server flow:
  - key down: enter marking phase
  - key tick: keep target marker synced while checking CP affordability
  - key up: settle resource once (cp+overload), then enter triggering phase
  - triggering ticks: move player toward target, apply segment hit every tick
  - terminate after trigger lifetime or abort/failure

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
                        [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :jet-engine)
(def ^:private jet-engine-skill-id :jet-engine)

; Phase timing/range stay as implementation constants; balance-facing values use skill-config below.
(def ^:private jet-target-range 12.0)
(def ^:private trigger-time-ticks 8)
(def ^:private trigger-lifetime-ticks 15)
(def ^:private min-segment-distance 1.0e-5)

(defn- hold-cp-requirement [player-id]
  (cfg-lerp :cost.hold.required-cp (skill-exp player-id)))

(defn- release-cp-cost [player-id]
  (cfg-lerp :cost.release.cp (skill-exp player-id)))

(defn- release-overload-cost [player-id]
  (cfg-lerp :cost.release.overload (skill-exp player-id)))

(defn- damage-amount [player-id]
  (cfg-lerp :combat.damage (skill-exp player-id)))

(defn- cooldown-ticks [player-id]
  (cfg-lerp-int :cooldown.ticks (skill-exp player-id)))

(defn- eye-pos-safe [player-id]
  (or (geom/eye-pos player-id)
      {:x 0.0 :y 64.0 :z 0.0}))

;; Unclamped, matching original's VecUtils.lerp/MathUtils.lerp (a + lambda*(b-a)):
;; the dash keeps extrapolating past the target for the remainder of the
;; trigger lifetime instead of stopping dead on arrival.
(defn- lerp-pos [a b t]
  (geom/v+ a (geom/v* (geom/v- b a) (double t))))

(defn- get-target-pos [player-id]
  (let [world-id (geom/world-id-of player-id)
        eye (eye-pos-safe player-id)
        look (when (raycast/available?)
               (raycast/player-look-vector player-id))]
    (when look
      (let [dir (geom/vnorm look)
            block-hit (when (raycast/available?)
                        (raycast/raycast-blocks
                                                world-id
                                                (:x eye) (:y eye) (:z eye)
                                                (:x dir) (:y dir) (:z dir)
                                                jet-target-range))]
        ;; Original getLookingPos returns result.hitVec — the precise
        ;; intersection point, not the integer block coordinate: the marker
        ;; and dash target must sit exactly where the crosshair lands.
        (if block-hit
          {:x (double (or (:hit-x block-hit) (:x block-hit)))
           :y (double (or (:hit-y block-hit) (:y block-hit)))
           :z (double (or (:hit-z block-hit) (:z block-hit)))}
          (geom/v+ eye (geom/v* dir jet-target-range)))))))

(defn- get-player-pos [player-id]
  (or (when (motion-effects/teleportation-available?)
        (motion-effects/player-position player-id))
      (eye-pos-safe player-id)))

(defn- update-skill-state-root!
  [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

;; Matches original's per-tick hold gate (s_onTick -> ctx.canConsumeCP(consumption)):
;; only CP availability is checked while holding. Overload is only checked
;; once, atomically, at release time via perform-resource! in jet-engine-up!
;; — same as original's ctx.consume() running once in s_onEnd.
(defn- can-hold? [player-id]
  (>= (skill-effects/current-cp player-id) (hold-cp-requirement player-id)))

;; Mark (targeting reticle) FX stays owner-only: original's l_spawnMark/
;; l_updateMark/l_endMark are all gated by isLocal even though MSG_MARK_END
;; itself is sent via sendToClient — the ripple mark is the caster's own aim
;; feedback, never rendered for bystanders.
(defn- send-mark-start! [ctx-id target]
  (fx/send! ctx-id {:topic :jet-engine/fx-start :mode :mark-start} nil {:target target}))

(defn- send-mark-update! [ctx-id target hold-ticks]
  (fx/send! ctx-id {:topic :jet-engine/fx-update :mode :mark-update} nil
            {:target target
             :hold-ticks (long hold-ticks)}))

(defn- send-mark-end! [ctx-id target]
  (fx/send! ctx-id {:topic :jet-engine/fx-end :mode :mark-end} nil {:target target}))

;; Trigger (dash) FX broadcasts: original's c_tStartEffect/c_tUpdateEffect
;; spawn the diamond-shield entity and particle burst unconditionally for
;; every MSG_TRIGGER recipient (owner + nearby, no isLocal check) — only the
;; walk-speed change inside those handlers is isLocal-gated, and that has no
;; port-side FX payload anyway.
(defn- send-trigger-start! [ctx-id start target velocity]
  (fx/send-local-and-nearby! ctx-id {:topic :jet-engine/fx-trigger-start :mode :trigger-start} nil {:start start
                            :target target
                            :velocity velocity}))

(defn- send-trigger-update! [ctx-id player-id pos trigger-ticks]
  ;; :pos is the ideal lerp point at EYE height, which is what the trail
  ;; geometry is drawn along. c_tUpdateEffect's burst is anchored on
  ;; player.posX/Y/Z instead — the caster's actual feet — so it travels with
  ;; them rather than sitting a head above the projected path.
  (fx/send-local-and-nearby! ctx-id {:topic :jet-engine/fx-trigger-update :mode :trigger-update} nil {:pos pos
                            :owner-pos (geom/body-pos player-id)
                            :trigger-ticks trigger-ticks}))

(defn- send-trigger-end! [ctx-id]
  (fx/send-local-and-nearby! ctx-id {:topic :jet-engine/fx-trigger-end :mode :trigger-end} nil nil))

(defn- mark-hit-and-damage!
  [player-id ctx-id world-id hit]
  (let [target-id (str (:uuid hit))
        self-id (str player-id)]
    (when-not (or (empty? target-id) (= target-id self-id))
      (when (entity-damage/available?)
        (entity-damage/apply-direct-damage!
          world-id
          target-id
          (damage-amount player-id)
          :magic))
      (md-damage/mark-target! player-id target-id
            {:ctx-id ctx-id
             :target-pos {:x (:x hit)
                    :y (:y hit)
                    :z (:z hit)}}))))

(defn- tick-triggering!
  [ctx-id player-id]
  (let [ctx-data (ctx-skill/get-context ctx-id)
        st (:skill-state ctx-data)
        trigger-ticks (long (or (:trigger-ticks st) 0))
        start-pos (:start-pos st)
        target-pos (:target-pos st)
        world-id (or (:world-id st) (geom/world-id-of player-id))]
    (if (or (nil? start-pos) (nil? target-pos) (>= trigger-ticks trigger-lifetime-ticks))
      (do
        (send-trigger-end! ctx-id)
        (send-mark-end! ctx-id target-pos)
        (ctx/terminate-context! ctx-id nil))
      (let [next-tick (inc trigger-ticks)
            t (/ (double next-tick) (double trigger-time-ticks))
            next-pos (lerp-pos start-pos target-pos t)
            prev-pos (or (:last-pos st) start-pos)
        segment (geom/v- next-pos prev-pos)
        segment-distance (geom/vlen segment)
        segment-dir (geom/vnorm segment)]
        (when (motion-effects/player-motion-available?)
          (motion-effects/dismount-riding! player-id))
        (when (motion-effects/player-motion-available?)
          ;; Drive the dash by VELOCITY, not teleport: the client then moves
          ;; the player through normal physics, which plays the walk/run
          ;; animation in third person (the original's c_triggerTick sets
          ;; motionX/Y/Z for the same reason). The constant per-tick velocity
          ;; reaches the target at tick 8 and keeps extrapolating past it for
          ;; the remaining lifetime, matching the original's
          ;; lerp(start, target, ticks/TIME).
          (motion-effects/reset-fall-damage! player-id)
          (when-let [{:keys [x y z]} (:velocity st)]
            (motion-effects/set-player-velocity! player-id x y z)))
        (let [hit (when (and (raycast/available?)
                             (> segment-distance min-segment-distance))
                    (raycast/raycast-entities
                                              world-id
                                              (:x prev-pos) (:y prev-pos) (:z prev-pos)
                                              (:x segment-dir) (:y segment-dir) (:z segment-dir)
                                              segment-distance))
              _ (when hit
                  (mark-hit-and-damage! player-id ctx-id world-id hit))]
          (update-skill-state-root! ctx-id merge
                                    {:trigger-ticks next-tick
                                     :last-pos next-pos})
          (send-trigger-update! ctx-id player-id next-pos next-tick))))))

(defn jet-engine-down!
  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when cost-ok?
    (let [target (get-target-pos player-id)]
      (ctx-skill/replace-skill-state! ctx-id {:phase :marking
                                     :hold-ticks 0
                                     :target-pos target
                                     :trigger-ticks 0})
      (send-mark-start! ctx-id target))))

(defn jet-engine-tick!
  [ctx-id player-id _skill-id _exp _cost-ok? hold-ticks _cost-stage _player-ref]
  (let [st (get-in (ctx-skill/get-context ctx-id) [:skill-state])
        phase (:phase st)]
    (case phase
      :triggering
      (tick-triggering! ctx-id player-id)

      :marking
      (if-not (can-hold? player-id)
        (do
          (send-mark-end! ctx-id (:target-pos st))
          (ctx/terminate-context! ctx-id nil))
        (let [target (or (get-target-pos player-id) (:target-pos st))]
          (update-skill-state-root! ctx-id merge
                                    {:hold-ticks (long hold-ticks)
                                     :target-pos target})
          (send-mark-update! ctx-id target hold-ticks)))

      nil)))

(defn jet-engine-up!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (let [st (get-in (ctx-skill/get-context ctx-id) [:skill-state])
          phase (:phase st)
          target-pos (or (:target-pos st) (get-target-pos player-id))]
      (if (not= phase :marking)
        nil
        (let [cp-needed (release-cp-cost player-id)
              overload-needed (release-overload-cost player-id)
              paid? (:success?
                     (skill-effects/perform-resource!
                       player-id
                       overload-needed
                       cp-needed
                       false))]
          (if-not (and paid? target-pos)
            (do
              (send-mark-end! ctx-id target-pos)
              (ctx/terminate-context! ctx-id nil))
            (let [start-pos (get-player-pos player-id)
                  travel (geom/v- target-pos start-pos)
                  velocity (geom/v* travel (/ 1.0 (double trigger-time-ticks)))]
              (update-skill-state-root! ctx-id merge
                                        {:phase :triggering
                                         :start-pos start-pos
                                         :target-pos target-pos
                                         :last-pos start-pos
                                         :velocity velocity
                                         :world-id (geom/world-id-of player-id)
                                         :trigger-ticks 0})
              (send-mark-end! ctx-id target-pos)
              (send-trigger-start! ctx-id start-pos target-pos velocity)
              (skill-effects/add-skill-exp! player-id jet-engine-skill-id (cfg-double :progression.exp-use))
              (skill-effects/set-main-cooldown! player-id jet-engine-skill-id (cooldown-ticks player-id)))))))
    (catch Exception e
      (log/warn "JetEngine up! failed" (ex-message e))
      (send-mark-end! ctx-id nil)
      (ctx/terminate-context! ctx-id nil))))

(defn jet-engine-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [st (get-in (ctx-skill/get-context ctx-id) [:skill-state])]
    (send-trigger-end! ctx-id)
    (send-mark-end! ctx-id (:target-pos st))))

(defskill jet-engine
  :id :jet-engine
  :category-id :meltdowner
  :name-key "ability.skill.meltdowner.jet_engine"
  :description-key "ability.skill.meltdowner.jet_engine.desc"
  :icon "textures/abilities/meltdowner/skills/jet_engine.png"
  :ui-position [170 32]
  :ctrl-id :jet-engine
  ;; Matches original's default.conf (empty jet_engine{} override -> inherits
  ;; the 1.0/1.0 default): declared :cost amounts are paid as-is, not zeroed.
  :cp-consume-speed 1.0
  :overload-consume-speed 1.0
  :pattern :release-cast
  :cooldown {:mode :manual}
  :input-policy {:terminate-on-key-up? false
                 :keep-active-on-key-up? true
                 :settle-perform-on-key-up? false}
  :actions {:down! jet-engine-down!
            :tick! jet-engine-tick!
            :up! jet-engine-up!
            :abort! jet-engine-abort!}
  :prerequisites [{:skill-id :meltdowner :min-exp 1.0}])


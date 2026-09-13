(ns cn.li.ac.content.ability.meltdowner.meltdowner
  "Meltdowner skill �?charge-window beam with block-breaking and reflection.

  Uses the escape-hatch pattern: fn hooks for overload-floor enforcement and
  min/max charge-window gating; :beam op (effect.beam) for the actual shot.

  Key mechanics:
  - Charge window: min 20, optimal cap 40, tolerant max 100 ticks
  - Down cost: overload lerp(200,170,exp)
  - Tick cost: CP lerp(10,15,exp)
  - Beam: width lerp(2,3,exp), damage timeRate(ct)*lerp(18,50,exp)
  - Block energy: timeRate(ct)*lerp(300,700,exp)
  - Reflection: delegates to vec-reflection via :reflect-can-fn/:reflect-shot-fn
  - Cooldown: timeRate(ct) * 20 * lerp(15,7,exp)  (manual)
  - EXP gain: timeRate(ct) * 0.002

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.util.balance :as bal]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.effects.beam :as beam]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.shared.vec-reflection-interaction :as vec-reflect]
                        [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Config helpers
;; ---------------------------------------------------------------------------

(def-skill-config-ops :meltdowner)
(def ^:private meltdowner-skill-id :meltdowner)

(defn- ticks-min [] (cfg-int :charge.min-ticks))
(defn- ticks-max [] (cfg-int :charge.max-ticks))
(defn- ticks-tolerant [] (cfg-int :charge.max-tolerant-ticks))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- time-rate [ct]
  (let [charge-window (max 1.0 (- (double (ticks-max)) (double (ticks-min))))]
    (cfg-lerp :charge.time-rate (/ (- (double ct) (double (ticks-min))) charge-window))))

(defn- to-charge-ticks [ticks]
  (int (min (ticks-max) (max (ticks-min) (int ticks)))))

(defn- normalize-look-dir
  [look-vec]
  {:x (double (or (:x look-vec) (:dx look-vec) 0.0))
   :y (double (or (:y look-vec) (:dy look-vec) 0.0))
   :z (double (or (:z look-vec) (:dz look-vec) 0.0))})

(defn- set-skill-state!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- enforce-overload-floor!
  [player-id floor-value]
  (skill-effects/enforce-overload-floor! player-id floor-value))

;; Original's MSG_TERMINATED (stops the charge loop sound / walk-speed
;; slowdown) is delivered to every recipient that has this context ALIVE —
;; owner + nearby, the same set that would have received fx-perform's beam.
(defn- send-end-fx! [ctx-id performed?]
  (fx/send-local-and-nearby! ctx-id {:topic :meltdowner/fx-end} nil {:performed? (boolean performed?)}))

;; ---------------------------------------------------------------------------
;; Vec-reflection interaction
;; ---------------------------------------------------------------------------

(defn- perform-reflection-shot!
  "Fire a reflected shot from the reflector player's perspective.
  Returns truthy if an entity was hit."
  [ctx-id reflector-player-id caster-exp]
  (let [start-pos (geom/eye-pos reflector-player-id)
        world-id  (geom/world-id-of reflector-player-id)
        look-vec  (when (raycast/available?)
                    (raycast/player-look-vector reflector-player-id))]
    (when look-vec
  (let [look-dir (normalize-look-dir look-vec)
    dir (geom/vnorm look-dir)
            end (geom/v+ start-pos (geom/v* dir (cfg-double :reflection.shot-distance)))]
        ;; Original's s_perform's reflection callback calls sendToClient(MSG_REFLECTED,...)
        ;; — every reflected shot spawns a world-space EntityMDRay visible to
        ;; owner + nearby, not just the caster.
        ;;
        ;; c_reflected builds the visual from the CASTER: it spawns the ray
        ;; along the caster's own look direction, moved out to the point on
        ;; that line level with the reflector. Only the damage trace
        ;; (s_reflected) uses the reflector's aim, which is what the raycast
        ;; below still does. Drawing the visual from the reflector's eye along
        ;; the reflector's aim made the reflected beam point somewhere the
        ;; original never draws it.
        (fx/send-local-and-nearby! ctx-id {:topic :meltdowner/fx-reflect :mode :reflect} nil
          (let [ctx-data (try (ctx-skill/get-context ctx-id) (catch Exception _ nil))
                caster-id (:player-uuid ctx-data)
                caster-eye (when caster-id
                             (try (geom/eye-pos caster-id) (catch Exception _ nil)))
                caster-look (when (and caster-id (raycast/available?))
                              (raycast/player-look-vector caster-id))]
            (if (and caster-eye caster-look)
              (let [cdir (geom/vnorm (normalize-look-dir caster-look))
                    dist (geom/vdist caster-eye start-pos)
                    spawn (geom/v+ caster-eye (geom/v* cdir dist))]
                {:start spawn
                 :end (geom/v+ spawn (geom/v* cdir (cfg-double :reflection.shot-distance)))})
              {:start start-pos :end end})))
        (let [hit (when (raycast/available?)
                    (raycast/raycast-entities
                  world-id
                  (:x start-pos) (:y start-pos) (:z start-pos)
                  (:x look-dir) (:y look-dir) (:z look-dir)
                  (cfg-double :reflection.shot-distance)))]
          (when (and (= (:hit-type hit) :entity) (entity-damage/available?))
            (entity-damage/apply-direct-damage!
                 world-id
                 (:uuid hit)
                 (* (cfg-double :reflection.damage-multiplier)
                    (cfg-lerp :reflection.base-damage caster-exp))
                 :magic)
            true))))))

;; ---------------------------------------------------------------------------
;; Main beam shot
;; ---------------------------------------------------------------------------

(defn- perform-meltdowner!
  "Fires the meltdowner beam. Returns :beam-result map (or {:performed? false})."
  [ctx-id player-id _skill-id exp hold-ticks]
  (let [ct       (to-charge-ticks hold-ticks)
        damage   (* (time-rate ct) (cfg-lerp :combat.damage exp))
        world-id (geom/world-id-of player-id)
        eye      (geom/eye-pos player-id)
        trace-pos (or (when (motion-effects/teleportation-available?)
                        (motion-effects/player-position player-id))
                      eye)
        look-vec (when (raycast/available?)
                   (raycast/player-look-vector player-id))]
    (if-not look-vec
      {:performed? false}
      (let [reflection (vec-reflect/build-reflection-callbacks
                         {:ctx-id ctx-id
                          :attacker-pos-fn (fn [] (geom/eye-pos player-id))
                          :reflect-shot-fn (fn [ctx-id* reflector-uuid]
                                             (perform-reflection-shot! ctx-id* reflector-uuid exp))})
            result (beam/execute-beam!
                    (merge {:player-id       player-id
                            :ctx-id          ctx-id
                            :world-id        world-id
                            :eye-pos         eye
                            :trace-pos       trace-pos
                            :look-dir        look-vec}
                           reflection)
                    {:radius          (cfg-lerp :beam.radius exp)
                     :query-radius    (cfg-double :beam.query-radius)
                     :step            (cfg-double :beam.step)
                     :max-distance    (cfg-double :beam.max-distance)
                     :visual-distance (cfg-double :beam.visual-distance)
                     :damage          damage
                     :damage-type     :magic
                     :break-blocks?   true
                     :skill-id        meltdowner-skill-id
                     :block-energy    (* (time-rate ct) (cfg-lerp :beam.block-energy exp))
                     :fx-topic        :meltdowner/fx-perform
                     :trace-pos       trace-pos})]
        (or (:beam-result result) {:performed? false})))))

;; ---------------------------------------------------------------------------
;; Action hooks
;; ---------------------------------------------------------------------------

(defn- meltdowner-on-down!
  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when cost-ok?
    ;; Matches original's overloadKeep = ctx.cpData.getOverload: snapshot the
    ;; actual resulting overload stat post-consumption, not the raw cost delta
    ;; — a player who already carried overload before activating must keep
    ;; that base amount reflected in the floor.
    (let [overload-floor (double (or (skill-effects/player-path player-id [:resource-data :cur-overload] 0.0) 0.0))]
      (set-skill-state! ctx-id [:overload-floor] overload-floor)
      ;; Charge visual start (original c_start: charge loop sound + the
      ;; client-side charge state that drives the ring/particles/walk-speed).
      (fx/send-local-and-nearby! ctx-id {:topic :meltdowner/fx-start} nil
        {:mode :start :player-id player-id}))))

(defn- meltdowner-on-tick!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ctx-data (ctx-skill/get-context ctx-id)
        ;; The key-tick payload carries no hold duration — the server-driven
        ;; per-tick advancement increments the charge counter here (original
        ;; g_tick's ticks += 1, which ran on both sides and fed l_keyUp).
        ticks (inc (long (or (get-in ctx-data [:skill-state :hold-ticks]) 0)))]
    (set-skill-state! ctx-id [:hold-ticks] ticks)
    ;; Charge visual update (original c_tick: charge ring pulse, particles,
    ;; walk-speed slow-down all scale with the charge ticks).
    (fx/send-local-and-nearby! ctx-id {:topic :meltdowner/fx-update} nil
              ;; Best-effort: a caster position we cannot resolve just means no
              ;; charge motes this tick, never a failed tick.
              (let [pos (try
                          (or (when (motion-effects/teleportation-available?)
                                (motion-effects/player-position player-id))
                              (geom/eye-pos player-id))
                          (catch Exception _ nil))]
                {:ticks ticks
                 :charge-ratio (max 0.0 (min 1.0 (/ (double ticks) (double (ticks-max)))))
                 :player-id player-id
                 ;; The charge particles ring the CASTER, so the client needs
                 ;; their position: it was building offsets like (r*sin, h,
                 ;; r*cos) and handing them to the particle queue, which takes
                 ;; absolute world coordinates — every mote spawned within a
                 ;; block of world origin.
                 :caster-x (some-> pos :x double)
                 :caster-y (some-> pos :y double)
                 :caster-z (some-> pos :z double)}))
    (when-let [floor (get-in ctx-data [:skill-state :overload-floor])]
      (enforce-overload-floor! player-id floor))
    (when (> ticks (ticks-tolerant))
      (send-end-fx! ctx-id false)
      (ctx/terminate-context! ctx-id nil)
      (log/debug "Meltdowner aborted: over tolerant ticks" ticks))))

(defn- meltdowner-on-up!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ticks (long (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :hold-ticks]) 0))]
    (if (< ticks (ticks-min))
      (do
        (send-end-fx! ctx-id false)
        (ctx/terminate-context! ctx-id nil)
        (log/debug "Meltdowner: insufficient charge ticks" ticks))
      (let [{:keys [performed? reflection-hit?]}
            (perform-meltdowner! ctx-id player-id _skill-id exp ticks)]
        (if performed?
          (let [ct (to-charge-ticks ticks)]
            (skill-effects/add-skill-exp! player-id meltdowner-skill-id
                                          (* (time-rate ct) (cfg-double :progression.exp-use)))
            (skill-effects/set-main-cooldown! player-id meltdowner-skill-id
                                             (int (* (time-rate ct)
                                                     (cfg-double :cooldown.base-multiplier)
                                                     (cfg-lerp :cooldown.ticks exp))))
            (send-end-fx! ctx-id true)
            (ctx/terminate-context! ctx-id nil)
            (log/debug "Meltdowner performed; reflection?" (boolean reflection-hit?)))
          (do
            (send-end-fx! ctx-id false)
            (ctx/terminate-context! ctx-id nil)
            (log/debug "Meltdowner: beam failed")))))))

(defn- meltdowner-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (send-end-fx! ctx-id false)
  (ctx/terminate-context! ctx-id nil))

(defn- meltdowner-cost-fail!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (when (= cost-stage :tick)
    (send-end-fx! ctx-id false))
  (ctx/terminate-context! ctx-id nil))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill meltdowner
  :id              :meltdowner
  :category-id     :meltdowner
  :name-key        "ability.skill.meltdowner.meltdowner"
  :description-key "ability.skill.meltdowner.meltdowner.desc"
  :icon            "textures/abilities/meltdowner/skills/meltdowner.png"
  :ui-position     [115 40]
  :ctrl-id         :meltdowner
  ;; Matches original's default.conf (empty meltdowner{} override -> inherits
  ;; the 1.0/1.0 default): declared :cost amounts are paid as-is, not zeroed.
  :cp-consume-speed 1.0
  :overload-consume-speed 1.0
  :cooldown-ticks (fn [{:keys [exp hold-ticks]}]
                    (let [exp* (double (or exp 0.0))
                          ct (long (or hold-ticks 20))
                          ct-clamped (max (ticks-min) (min (ticks-max) ct))
                          tr (cfg-lerp :charge.time-rate (/ (- (double ct-clamped) (double (ticks-min)))
                                                            (double (max 1 (- (ticks-max) (ticks-min))))))]
                      (int (* tr 20.0 (cfg-lerp :cooldown.ticks exp*)))))
  ;; matching original: timeRate(ct) * 20 * lerp(15, 7, exp)
  :pattern         :charge-window
  :cooldown        {:mode :manual}
  :cost            {:down {:overload (fn [{:keys [player-id]}]
                                       (cfg-lerp :cost.down.overload (skill-exp player-id)))}
                    :tick  {:cp (fn [{:keys [player-id]}]
                                  (cfg-lerp :cost.tick.cp (skill-exp player-id)))}}
  :fx              {:start  {:topic   :meltdowner/fx-start
                             :payload (fn [_] {:mode :start})}
                    :update {:topic   :meltdowner/fx-update
                             :payload (fn [{:keys [hold-ticks]}]
                                        (let [ticks (long (or hold-ticks 0))]
                                          {:ticks ticks
                                           :charge-ratio (bal/clamp01
                                                          (/ (double ticks)
                                                             (double (ticks-max))))}))}}
  :actions         {:down!      meltdowner-on-down!
                    :tick!      meltdowner-on-tick!
                    :up!        meltdowner-on-up!
                    :abort!     meltdowner-abort!
                    :cost-fail! meltdowner-cost-fail!}
  :prerequisites   [{:skill-id :scatter-bomb :min-exp 0.8}
                    {:skill-id :light-shield :min-exp 0.8}])


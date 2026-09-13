(ns cn.li.ac.content.ability.electromaster.thunder-clap
  "ThunderClap - channeled AOE lightning strike.

  Pattern: :charge-window (40..60 ticks)
  Start overload: lerp(390,252)
  Tick CP: lerp(18,25) while ticks <= 40
  Damage: lerp(36,72,exp) * lerp(1.0,1.2,extra-ratio)
  AOE radius: lerp(15,30,exp) with distance falloff
  Cooldown: ticks * lerp(10,6,exp)
  Exp: 0.003 per use"
  (:require
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.util.attack :as attack]
            [cn.li.ac.ability.util.balance :as bal]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.world :as world-op]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]))

(def-skill-config-ops :thunder-clap)
(defn- min-ticks [] (cfg-int :charge.min-ticks))
(defn- max-ticks [] (cfg-int :charge.max-ticks))
(defn- targeting-range [] (cfg-double :targeting.range))

(defn- compute-overcharge-ratio
  "Matching original: lerp(1.0, 1.2, (ticks - 40) / 60.0).
  Divisor is 60 (not the max-min span), giving a sub-1.0 ratio range."
  [ticks]
  (bal/clamp01 (/ (- (double (or ticks 0)) (double (min-ticks)))
                   (double (max-ticks)))))  ;; divisor = max-ticks (60), matching original

(defn- resolve-fallback-target
  [player-id]
  (let [eye (geom/eye-pos player-id)
        look (when (raycast/available?)
               (raycast/player-look-vector player-id))
        look* (or look {:x 0.0 :y 0.0 :z 1.0})
        range (targeting-range)]
    {:x (+ (double (:x eye)) (* (double (:x look*)) range))
     :y (+ (double (:y eye)) (* (double (:y look*)) range))
     :z (+ (double (:z eye)) (* (double (:z look*)) range))}))

(defn- resolve-raycast-target
  [player-id]
  (let [range (targeting-range)
        world-id (geom/world-id-of player-id)
        eye (geom/eye-pos player-id)
        look (when (raycast/available?)
               (raycast/player-look-vector player-id))
        hit (when (and (raycast/available?) look)
              ;; Original uses traceLiving(..., EntitySelectors.nothing()):
              ;; entities are deliberately ignored and only blocks stop the ray.
              (raycast/raycast-blocks
                world-id
                (:x eye) (:y eye) (:z eye)
                (double (or (:x look) 0.0))
                (double (or (:y look) 0.0))
                (double (or (:z look) 1.0))
                (double range)))]
    (if (= :block (attack/hit-kind hit))
      (attack/block-impact-point hit)
      (resolve-fallback-target player-id))))

(defn- current-target
  [ctx-id player-id]
  (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :hit-pos])
      (resolve-fallback-target player-id)))

(defn- update-skill-state-root!
  [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

;; Only the cost callbacks need the by-player scan — their arity is
;; [player-id skill-id exp] with no ctx-id. Everything dispatched with a
;; ctx-id uses ctx-skill/hold-ticks instead.
(defn- stored-hold-ticks [player-id skill-id]
  (ctx-skill/hold-ticks-for-player player-id skill-id))

(defn- stored-hold-ticks-by-ctx [ctx-id]
  (ctx-skill/hold-ticks ctx-id))

(defn- tick-cp-cost [player-id _skill-id exp]
  ;; Cost is evaluated before thunder-clap-tick! increments the stored tick
  ;; count for this dispatch, so check against the tick this call is about to
  ;; produce (matches upstream's post-increment "ticks <= MIN_TICKS" check).
  (if (<= (inc (stored-hold-ticks player-id :thunder-clap)) (min-ticks))
    (cfg-lerp :cost.tick.cp (bal/clamp01 (double (or exp 0.0))))
    0.0))

(defn- down-overload-cost [_player-id _skill-id exp]
  (cfg-lerp :cost.down.overload (double (or exp 0.0))))

(defn- mark-performed!
  [ctx-id performed? & {:as extra-state}]
  (update-skill-state-root! ctx-id merge
                            (merge {:performed? (boolean performed?)} extra-state))
  (boolean performed?))

(defn- performed?
  [ctx-id]
  (boolean (get-in (ctx-skill/get-context ctx-id) [:skill-state :performed?])))

(defn- scaled-target-damage
  "Match AbilityContext.attack: apply the per-target SkillAttack calculation
  after range falloff, then global and per-skill damage scaling."
  [player-id target-id raw-damage]
  (skill-effects/scale-damage
    (skill-registry/get-skill :thunder-clap)
    (ability-event/fire-calc-event!
      ability-event/CALC-SKILL-ATTACK
      raw-damage
      {:player-id player-id
       :target-id target-id
       :skill-id :thunder-clap})))

(defn- execute-thunder-clap-aoe!
  "Apply original attackRange semantics: caster exclusion, linear distance
  falloff, target-specific SkillAttack modifiers, skill/global scaling, and
  an attacker-attributed skill damage source."
  [{:keys [player-id world-id hit-pos]} radius amount]
  (if-not (entity-damage/available?)
    0
    (reduce
      (fn [hit-count {:keys [uuid x y z]}]
        (let [dx (- (double x) (double (:x hit-pos)))
              dy (- (double y) (double (:y hit-pos)))
              dz (- (double z) (double (:z hit-pos)))
              distance (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
              raw-damage (* (double amount)
                            (bal/falloff-linear distance radius))
              final-damage (scaled-target-damage player-id uuid raw-damage)]
          (if (and (> final-damage 0.0)
                   (entity-damage/apply-direct-damage!
                     world-id uuid final-damage :skill
                     {:attacker-uuid player-id
                      :skill-id :thunder-clap}))
            (inc hit-count)
            hit-count)))
      0
      (attack/aoe-victims world-id hit-pos radius #{player-id}))))

(defn- end-payload
  [{:keys [ctx-id player-id hold-ticks]}]
  (let [ticks (long (or hold-ticks 0))]
    {:performed?   (boolean (get-in (ctx-skill/get-context ctx-id) [:skill-state :performed?]))
     :charge-ticks ticks
     :ticks        ticks
     :charge-ratio (compute-overcharge-ratio ticks)
     :target       (current-target ctx-id player-id)
     :caster-pos   (geom/eye-pos player-id)}))

(defn- refresh-hit-pos!
  [ctx-id player-id]
  (update-skill-state-root! ctx-id assoc :hit-pos (resolve-raycast-target player-id))
  nil)

;; The generic dispatch pipeline's hold-ticks argument is never populated for
;; server-tick-driven charge-window contexts (context-manager's
;; tick-context-entry! passes {:ctx-id :skill-id} only, no :hold-ticks) — so
;; this self-tracks charge duration in :skill-state instead of trusting the
;; argument, matching railgun.clj/body_intensify.clj.

;; The skill's own :fx map (declared below in defskill) is never
;; auto-dispatched by the framework — cn.li.ac.ability.service.skill-effects/emit-fx!,
;; the only function that reads it, is never called from the dispatch
;; pipeline. Skills must call it (or fx/send! directly) themselves, matching
;; the pattern already used by railgun.clj/meltdowner.clj/mark_teleport.clj.
;;
;; Broadcast (not owner-only): matches original, where EntitySurroundArc is
;; publicly spawned and visible to everyone nearby — only the ripple mark
;; (EntityRippleMark) was isLocal-gated to the caster. The client-side
;; build-plan (impl/thunder_clap.clj) is what actually keeps the ripple mark
;; private despite the broadcast: it only draws target-mark-ops for the
;; state matching the viewer's own player-uuid.
(defn- emit-thunder-clap-fx! [stage evt]
  (skill-effects/emit-fx! (skill-registry/get-skill :thunder-clap) evt stage true))

(defn- thunder-clap-down!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (refresh-hit-pos! ctx-id player-id)
  (emit-thunder-clap-fx! :start {:ctx-id ctx-id :player-id player-id}))

(declare perform-thunder-clap!)

(defn- thunder-clap-tick!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ticks (inc (stored-hold-ticks-by-ctx ctx-id))]
    (update-skill-state-root! ctx-id assoc :hold-ticks ticks)
    (refresh-hit-pos! ctx-id player-id)
    (emit-thunder-clap-fx! :update {:ctx-id ctx-id :player-id player-id :hold-ticks ticks})
    ;; Original sends MSG_END to itself on MAX_TICKS and performs immediately,
    ;; even while the player is still holding the key.
    (when (>= ticks (max-ticks))
      (perform-thunder-clap! ctx-id player-id exp ticks)
      ;; Server-initiated end (upstream's sendToSelf(MSG_END) -> terminate()),
      ;; so nothing else will notify the client afterwards — unlike the key-up
      ;; path, where context-state terminates right after this returns. Passing
      ;; nil here would leave the client's context list believing the context
      ;; is still running after the auto-strike.
      (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))))

(defn- thunder-clap-abort!
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (mark-performed! ctx-id false)
  (emit-thunder-clap-fx! :end {:ctx-id ctx-id :player-id player-id
                               :hold-ticks (stored-hold-ticks-by-ctx ctx-id)}))

(defn- perform-thunder-clap!
  [ctx-id player-id exp ticks]
  (when-not (performed? ctx-id)
    (let [hit-pos  (current-target ctx-id player-id)
          world-id (geom/world-id-of player-id)
          exp*     (bal/clamp01 (double (or exp 0.0)))
          mult     (cfg-lerp :combat.overcharge-multiplier
                             (compute-overcharge-ratio ticks))
          dmg      (* (cfg-lerp :combat.damage exp*) mult)
          radius   (cfg-lerp :combat.aoe-radius exp*)
          cooldown (max 1 (int (* (double ticks)
                                   (cfg-lerp :cooldown.ticks-per-hold exp*))))
          skill-spec (skill-registry/get-skill :thunder-clap)
          evt {:player-id player-id :ctx-id ctx-id :world-id world-id
               :hit-pos hit-pos :exp exp*}]
      ;; Matches EntityLightningBolt(..., effectOnly=true): flash and thunder
      ;; only. Combat damage is the separate attackRange equivalent below.
      (world-op/execute-spawn-lightning! evt {:at :hit-pos :visual-only? true})
      (execute-thunder-clap-aoe! evt radius dmg)
      (skill-effects/set-main-cooldown! player-id :thunder-clap cooldown)
      (skill-effects/add-skill-exp! player-id :thunder-clap
                                    (cfg-double :progression.exp-use)
                                    (double (or (:exp-incr-speed skill-spec) 1.0)))
      (mark-performed! ctx-id true :final-target hit-pos)
      (ability-event/fire-ability-event!
        (ability-event/make-skill-perform-event player-id :thunder-clap))
      (emit-thunder-clap-fx! :perform {:ctx-id ctx-id :player-id player-id :hold-ticks ticks})
      (emit-thunder-clap-fx! :end {:ctx-id ctx-id :player-id player-id :hold-ticks ticks})
      true)))

(defn- thunder-clap-up!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [ticks (stored-hold-ticks-by-ctx ctx-id)]
    (if (< ticks (min-ticks))
      (do
        (mark-performed! ctx-id false :final-target (current-target ctx-id player-id))
        (emit-thunder-clap-fx! :end {:ctx-id ctx-id :player-id player-id :hold-ticks ticks}))
      (perform-thunder-clap! ctx-id player-id exp ticks))))

(defn- thunder-clap-cost-fail!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (let [ticks (stored-hold-ticks-by-ctx ctx-id)]
    ;; Upstream increments before payment. A failure exactly on MIN_TICKS
    ;; therefore still reaches s_onEnd with a valid charge and strikes.
    (if (and (= cost-stage :tick) (>= ticks (min-ticks)))
      (perform-thunder-clap! ctx-id player-id exp ticks)
      (do
        (mark-performed! ctx-id false)
        (emit-thunder-clap-fx! :end {:ctx-id ctx-id :player-id player-id
                                     :hold-ticks ticks}))))
  ;; Same as the max-charge auto-release: a cost failure ends the context from
  ;; the server side, with no key-up termination to follow.
  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))

(defskill thunder-clap
  :id              :thunder-clap
  :category-id     :electromaster
  :name-key        "ability.skill.electromaster.thunder_clap"
  :description-key "ability.skill.electromaster.thunder_clap.desc"
  :icon            "textures/abilities/electromaster/skills/thunder_clap.png"
  :ui-position     [204 80]
  :ctrl-id         :thunder-clap
  :pattern         :charge-window
  :input-policy    {:settle-perform-on-key-up?
                    (fn [{:keys [ctx-id]}]
                      (boolean (get-in (ctx-skill/get-context ctx-id) [:skill-state :performed?])))}
  :cooldown        {:mode :manual}
  :cost            {:down {:overload down-overload-cost}
                    :tick {:cp tick-cp-cost}}
  :fx              {:start  {:topic   :thunder-clap/fx-start
                             :payload (fn [{:keys [ctx-id player-id]}]
                                        {:charge-ticks 0
                                         :ticks        0
                                         :charge-ratio 0.0
                                         :target       (current-target ctx-id player-id)
                                         :caster-pos   (geom/eye-pos player-id)})}
                    :update {:topic   :thunder-clap/fx-update
                             :payload (fn [{:keys [hold-ticks ctx-id player-id]}]
                                        (let [ticks (long (or hold-ticks 0))]
                                          {:charge-ticks ticks
                                           :ticks        ticks
                                           :charge-ratio (compute-overcharge-ratio ticks)
                                           :target       (current-target ctx-id player-id)
                                           :caster-pos   (geom/eye-pos player-id)}))}
                    :perform {:topic   :thunder-clap/fx-perform
                              :payload (fn [{:keys [hold-ticks ctx-id player-id]}]
                                         (let [ticks (long (or hold-ticks 0))]
                                           {:performed?   true
                                            :charge-ticks ticks
                                            :ticks        ticks
                                            :charge-ratio (compute-overcharge-ratio ticks)
                                            :target       (current-target ctx-id player-id)
                                            :caster-pos   (geom/eye-pos player-id)}))}
                    :end    {:topic   :thunder-clap/fx-end
                             :payload end-payload}}
  :actions
  {:down!      thunder-clap-down!
   :tick!      thunder-clap-tick!
   :up!        thunder-clap-up!
   :cost-fail! thunder-clap-cost-fail!
   :abort!     thunder-clap-abort!}
  :prerequisites [{:skill-id :thunder-bolt :min-exp 1.0}])

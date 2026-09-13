(ns cn.li.ac.content.ability.electromaster.railgun
  "Railgun skill �?coin-QTE + iron-item charge mechanic.

  Complex skill using the escape-hatch pattern: fn hooks for the custom
  coin-QTE / item-charge logic; :beam op (effect.beam) for the actual shot.

  No Minecraft imports."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.beam :as beam]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.shared.vec-reflection-interaction :as vec-reflect]
            [cn.li.ac.ability.item-actions :as item-actions]
                        [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Constants
;; ---------------------------------------------------------------------------

(def-skill-config-ops :railgun)
(def ^:private accepted-item-ids
  #{"minecraft:iron_ingot" "minecraft:iron_block"})

(defn- coin-active-threshold [] (cfg-double :qte.coin-active-threshold))
(defn- coin-perform-threshold
  "Fire threshold, clamped to ≤ the active threshold: the client ring's gold
  zone IS the fire zone. The display curve leads the server coin's real
  progress by ~1 spawn tick (~50 ms), so a strict 0.7 boundary made presses
  at the displayed 70-80 % land below it — unstable. (Also self-heals any
  persisted qte.coin-perform-threshold above the active threshold.)"
  []
  (min (cfg-double :qte.coin-perform-threshold) (coin-active-threshold)))
(defn- item-charge-ticks [] (cfg-int :charge.item-charge-ticks))
(defn- reflection-distance [] (cfg-double :reflection.distance))
(defn- reflection-damage [] (cfg-double :reflection.damage))
(defn- railgun-exp-gain [_hit? reflection-hit?]
  ;; Original: hitEntity only set on reflection hit �?0.01; otherwise 0.005
  (if reflection-hit?
    (cfg-double :progression.exp-reflection-hit)   ;; 0.01 reflection hit only
    (cfg-double :progression.exp-hit)))             ;; 0.005 normal hit or miss
(defn- railgun-cooldown-ticks [exp]
  (cfg-lerp-int :cooldown.manual-ticks exp))

(defn- railgun-damage
  "Apply the global/per-skill scaling and SkillAttack calc event used by the
  original Context.attack path."
  [player-id target-id raw-damage]
  (skill-effects/scale-damage
    (skill-registry/get-skill :railgun)
    (ability-event/fire-calc-event!
      ability-event/CALC-SKILL-ATTACK
      raw-damage
      {:player-id player-id
       :target-id target-id
       :skill-id :railgun})))

;; ---------------------------------------------------------------------------
;; Player / item helpers
;; ---------------------------------------------------------------------------

(defn- body-pos [player-id]
  (geom/body-pos player-id))

(defn- resolve-player-ref
  "Live player ref for player-id.

  The dispatch pipeline's positional player-ref argument is only populated on
  the network-driven callbacks. Server-tick-driven ones get nil — see
  context-manager's tick-context-entry!, which builds its payload as
  {:ctx-id :skill-id} with no :player — and the client deliberately does not
  send MSG-SLOT-KEY-TICK (it would double-dispatch costs), so the tick
  callback's player is nil in production, always. Same idiom mag-movement
  uses for its creative check."
  [player-id]
  (try
    (when-let [fw-atom (fw/fw-atom)]
      (when (platform/get-adapter fw-atom :runtime-interop)
        (platform/call-adapter fw-atom :runtime-interop :get-player-entity player-id)))
    (catch Exception _
      nil)))

(defn- accepted-item-in-hand? [player]
  (when player
    (contains? accepted-item-ids (entity/player-get-main-hand-item-id player))))

(defn- consume-item-for-shot! [player]
  (if (or (nil? player) (entity/player-creative? player))
    true
    (entity/player-consume-main-hand-item! player 1)))

(defn- set-skill-state!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(declare active-railgun-ctx-id send-coin-charge-start!)

(defn register-coin-throw!
  "Register a railgun coin throw state.
  Called from the platform item-action hook when a coin is used in ability mode.
  If the player is currently in item-charge mode, that charge is aborted first
  (mirrors original informThrowCoin �?onKeyAbort() behavior)."
  [player-id payload]
  (let [_now-ms (long (or (:timestamp-ms payload) (System/currentTimeMillis)))]
    ;; Abort any in-progress item charge so the coin QTE takes priority.
    (doseq [[_ctx-key ctx-data] (ctx/get-all-contexts)]
      (when (and (= (:player-uuid ctx-data) player-id)
                 (= :item-charge (get-in ctx-data [:skill-state :mode])))
        (ctx-skill/replace-skill-state! (:id ctx-data) {:fired false
                                       :mode :item-charge-cancelled
                                       :charge-ticks 0})))
    ;; New coin throw resets one-shot judgement lock.
    (skill-effects/clear-railgun-coin-judged! player-id)
    (when-let [ctx-id (active-railgun-ctx-id player-id)]
      ;; Upstream broadcasts the one-shot RailgunHandEffect for coin throws.
      (send-coin-charge-start! ctx-id player-id))
    ;; Upstream ItemCoin plays the flipcoin sound on every throw, with no
    ;; railgun-context requirement (sound at 0.5/1.0).
    (when (world-effects/available?)
      (when-let [pos (geom/eye-pos player-id)]
        (when-let [world-id (geom/world-id-of player-id)]
          (world-effects/play-sound!
            world-id (:x pos) (:y pos) (:z pos)
            (modid/namespaced-path "entity.flipcoin")
            :players 0.5 1.0))))
    true))

(def ^:private coin-entity-registry-id (modid/namespaced-path "entity_coin_throwing"))

(defn- coin-entity?
  "Whether ent (a world-effects/find-entities-in-radius result map) is the
  coin-throwing entity. Resolved via entity-get-type-id* (registry-key format, e.g.
  \"modid:entity_coin_throwing\") rather than the ad-hoc :type/:entity-id
  fields on the entity map, whose format differs across Forge (registry key)
  and Fabric (description id) and never equals the bare registry name either
  way."
  [world-id ent]
  (= coin-entity-registry-id (entity/get-type-id world-id (:uuid ent))))

(defn- discard-coin-entity!
  "Kills a specific coin entity by UUID. Falls back to nearby cleanup when UUID is nil."
  [player-id coin-uuid]
  (when (and (world-effects/available?) (motion-effects/entity-motion-available?))
    (let [pos (geom/eye-pos player-id)
          world-id (geom/world-id-of player-id)]
      (when (and pos world-id)
        (doseq [ent (world-effects/find-entities-in-radius
                      world-id (:x pos) (:y pos) (:z pos) 4.0)]
          (when (and (coin-entity? world-id ent)
                     (or (nil? coin-uuid)
                         (= coin-uuid (:uuid ent))))
            (motion-effects/discard-entity!
                                           world-id (:uuid ent))))))))

(defn- qte-status [p]
  (let [progress (double (max 0.0 (min 1.0 (or p 0.0))))]
    {:has-window? true
     :progress    progress
     :active?     (>= progress (coin-active-threshold))
     :perform?    (> progress (coin-perform-threshold))}))

(defn- coin-candidates [world-id entities]
  (->> entities
       (filter (partial coin-entity? world-id))
      (sort-by (fn [ent] (double (or (:motion-progress ent) 0.0))) >)))

(defn- coin-judged-uuid [player-id]
  (skill-effects/player-path player-id [:runtime :railgun :coin-judged-uuid]))

(defn- mark-coin-judged! [player-id coin-uuid]
  (when (some? coin-uuid)
    (skill-effects/mark-railgun-coin-judged! player-id coin-uuid)))

(defn- read-coin-qte-status [player-id]
  (if-not (world-effects/available?)
    {:has-window? false :active? false :perform? false :progress 0.0}
    (let [pos (geom/eye-pos player-id)
          world-id (geom/world-id-of player-id)
          entities (when (and pos world-id)
                     (world-effects/find-entities-in-radius
                      world-id (:x pos) (:y pos) (:z pos) 4.0))
          judged-uuid (coin-judged-uuid player-id)
          candidates (coin-candidates world-id entities)
          coin (some (fn [ent]
                       (when (not= judged-uuid (:uuid ent)) ent))
                     candidates)]
      (if coin
        (assoc (qte-status (double (or (:motion-progress coin) 0.0)))
               :coin-uuid (:uuid coin))
        (do
          ;; Clear stale lock when all coins are gone.
          (skill-effects/clear-railgun-coin-judged! player-id)
          {:has-window? false :active? false :perform? false :progress 0.0})))))

;; ---------------------------------------------------------------------------
;; Vec-reflection interaction
;; ---------------------------------------------------------------------------

(defn- perform-reflection-shot!  "Fire a secondary shot from the reflector player's perspective.
  Returns truthy if an entity was hit."
  [ctx-id reflector-player-id]
  (let [start-pos (geom/eye-pos reflector-player-id)
        world-id  (geom/world-id-of reflector-player-id)
        look-vec  (when (raycast/available?)
                    (raycast/player-look-vector reflector-player-id))]
    (when look-vec
      (let [max-distance (reflection-distance)
            ;; player-look-vector only ever returns {:x :y :z} — there is
            ;; no :dx/:dy/:dz key on this bridge's result.
            look-x (double (or (:x look-vec) 0.0))
            look-y (double (or (:y look-vec) 0.0))
            look-z (double (or (:z look-vec) 1.0))
            entity-hit (raycast/raycast-from-player
                         reflector-player-id max-distance true)
            block-hit  (raycast/raycast-blocks
                         world-id
                         (:x start-pos) (:y start-pos) (:z start-pos)
                         look-x look-y look-z
                         max-distance)
            entity-dist (double (or (:distance entity-hit)
                                    Double/POSITIVE_INFINITY))
            block-dist  (double (or (:distance block-hit)
                                    Double/POSITIVE_INFINITY))
            hit-uuid    (when (< entity-dist block-dist)
                          (or (:uuid entity-hit) (:entity-id entity-hit)))
            attacker-id (or (:player-uuid (ctx-skill/get-context ctx-id))
                            reflector-player-id)
            dir         {:x look-x :y look-y :z look-z}
            end-pos     (geom/v+ start-pos (geom/v* dir max-distance))]
        ;; Original's reflectServer uses NetworkMessage.sendToAllAround (owner
        ;; + 20-block radius) for the reflected beam — a world-space shot,
        ;; visible to anyone nearby, not just the caster.
        (fx/send-local-and-nearby! ctx-id {:topic :railgun/fx-reflect :mode :reflect} nil {:start        start-pos
                                  :end          end-pos
                                  :hit-distance max-distance})
        (when (and hit-uuid (entity-damage/available?))
          (entity-damage/apply-direct-damage!
                                              world-id hit-uuid
                                              (railgun-damage
                                                attacker-id
                                                hit-uuid
                                                (reflection-damage))
                                              :generic)
          true)))))

;; ---------------------------------------------------------------------------
;; Main beam shot
;; ---------------------------------------------------------------------------

(defn- hand-muzzle-pos
  "First-person railgun muzzle at the caster's hand, not the eye.

  Upstream EntityRailgunFX spawns at eye height, but its first-person solid
  look relies on the RendererRayCylinder paraboloid head; the port's closest
  equivalent is a hand start (like current-charging's hand-center beam): the
  camera leaves the beam axis, so the flat halo billboard and the tube ray
  are both visible and read as a solid rod instead of an on-axis hollow pipe.
  Hand ≈ eye + forward·0.35 + right·0.3 − up·0.15."
  [eye-pos look-vec]
  (let [lx (double (or (:x look-vec) 0.0))
        ly (double (or (:y look-vec) 0.0))
        lz (double (or (:z look-vec) 1.0))
        dir (geom/vnorm {:x lx :y ly :z lz})
        [right up] (geom/orthonormal-basis dir)]
    (geom/v+ (geom/v+ (geom/v+ eye-pos (geom/v* dir 0.35))
                      (geom/v* right 0.3))
             (geom/v* up -0.15))))

(defn- perform-main-shot!
  "Fires the railgun beam. Returns :beam-result map (or {:performed? false})."
  [player-id ctx-id exp]
  (let [world-id  (geom/world-id-of player-id)
        eye       (geom/eye-pos player-id)
        trace-pos (body-pos player-id)
        look-vec  (when (raycast/available?)
                    (raycast/player-look-vector player-id))]
    (if-not look-vec
      {:performed? false}
      (let [muzzle (hand-muzzle-pos eye look-vec)
            damage   (cfg-lerp :beam.damage exp)
            reflection (vec-reflect/build-reflection-callbacks
                         {:ctx-id ctx-id
                          :attacker-pos-fn (fn [] eye)
                          :reflect-shot-fn perform-reflection-shot!})
                result   (beam/execute-beam!
                    (merge {:player-id       player-id
                            :ctx-id          ctx-id
                            :world-id        world-id
                            :eye-pos         muzzle
                            :look-dir        look-vec}
                           reflection)                    {:radius          (cfg-double :beam.radius)
                     :query-radius    (cfg-double :beam.query-radius)
                     :step            (cfg-double :beam.step)
                     :max-distance    (cfg-double :beam.max-distance)
                     :visual-distance (cfg-double :beam.visual-distance)
                     :damage          damage
                     :damage-transform-fn
                     (fn [target-id raw-damage]
                       (railgun-damage player-id target-id raw-damage))
                     :damage-type     :generic
                     :break-blocks?   true
                     :skill-id        :railgun
                     :block-energy    (cfg-lerp :beam.block-energy exp)
                     :fx-topic        :railgun/fx-shot
                     ;; execute-beam! reads :trace-pos from params (the evt
                     ;; entry above was dead): the damage/block/visual-end
                     ;; anchor is the feet, upstream startPos — only the fx
                     ;; start stays at the hand muzzle.
                     :trace-pos       trace-pos})
            beam-result (or (:beam-result result) {:performed? false})]
        (when (and (:performed? beam-result) (world-effects/available?))
          (world-effects/play-sound!
                                     world-id
                                     (:x eye) (:y eye) (:z eye)
                                     (modid/namespaced-path "em.railgun")
                                     :ambient
                                     0.5
                                     1.0))
        beam-result))))

(defn- creeper-hit?
  [world-id hit-uuids]
  (boolean
    (some (fn [entity-uuid]
            (= "minecraft:creeper"
               (entity/get-type-id world-id entity-uuid)))
          hit-uuids)))

;; ---------------------------------------------------------------------------
;; Cost hooks (private, passed as fns in defskill)
;; ---------------------------------------------------------------------------

(defn- active-railgun-ctx-id [player-id]
  (some (fn [[ctx-id ctx-data]]
          (when (and (= (:player-uuid ctx-data) player-id)
                     (= :railgun (:skill-id ctx-data)))
            ctx-id))
        (ctx/get-all-contexts)))

(defn- item-charge-at-fire-point? [ctx-id]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx-data)]
      (and (= (:mode skill-state) :item-charge)
           (<= (max 0 (int (or (:charge-ticks skill-state) 0))) 1)))))

(defn- item-charge-ready? [ctx-id player]
  (and (item-charge-at-fire-point? ctx-id)
       (accepted-item-in-hand? player)))

(defn- cost-creative? [_player-id _skill-id _exp]
  false)

(defn- down-cost-cp [player-id _skill-id exp]
  (let [qte (read-coin-qte-status player-id)]
    (if (:perform? qte) (cfg-lerp :cost.down.cp exp) 0.0)))

(defn- down-cost-overload [player-id _skill-id exp]
  (let [qte (read-coin-qte-status player-id)]
    (if (:perform? qte) (cfg-lerp :cost.down.overload exp) 0.0)))

(defn- tick-cost-cp [player-id _skill-id exp]
  (if-let [ctx-id (active-railgun-ctx-id player-id)]
    (if (item-charge-at-fire-point? ctx-id)
      (cfg-lerp :cost.tick.cp exp)
      0.0)
    0.0))

(defn- tick-cost-overload [player-id _skill-id exp]
  (if-let [ctx-id (active-railgun-ctx-id player-id)]
    (if (item-charge-at-fire-point? ctx-id)
      (cfg-lerp :cost.tick.overload exp)
      0.0)
    0.0))

;; ---------------------------------------------------------------------------
;; Charge FX markers.
;;
;; Two jobs, both broadcast (owner + nearby), matching original's
;; MSG_CHARGE_EFFECT (sendToAllAround, 30-block radius):
;;
;; 1. Idle-gating: without a live :railgun-shot level-effect entry during the
;;    charge-only window (no beam yet exists), level-effects' idle-gating
;;    never invokes build-plan at all. Mirrors jet-engine's
;;    send-mark-start!/-update!/-end! pattern.
;; 2. The actual charge-hand glow: original's RailgunHandEffect is a
;;    PlayerRenderHook attached to the caster's model, rendered by whoever
;;    can see that model. Ported as a world-anchored scripted-effect entity
;;    (entities/all.clj's railgun_charge, :follow-owner? true) spawned on
;;    fx-charge-start and despawned on fx-charge-end — see
;;    railgun_fx.clj's :immediate channel handlers. It's keyed by
;;    :source-player-id, not the local viewer, so every recipient's client
;;    spawns their own copy anchored to the CASTER, not themselves.
;; ---------------------------------------------------------------------------

(defn- send-coin-charge-start! [ctx-id player-id]
  (fx/send-local-and-nearby! ctx-id {:topic :railgun/fx-charge-start :mode :charge-start} nil
            {:source-player-id player-id}))

(defn- send-charge-start! [ctx-id player-id]
  ;; Item fallback's RailgunHandEffect is local-only in the original.
  (fx/send! ctx-id {:topic :railgun/fx-charge-start
                    :mode :charge-start
                    :to :client}
            nil
            {:source-player-id player-id}))

(defn- send-charge-update! [ctx-id player-id charge-ticks-left]
  (fx/send! ctx-id {:topic :railgun/fx-charge-update
                    :mode :charge-update
                    :to :client}
            nil
            {:source-player-id player-id
             :charge-ticks-left (long charge-ticks-left)}))

(defn- send-charge-end! [ctx-id player-id]
  (fx/send! ctx-id {:topic :railgun/fx-charge-end
                    :mode :charge-end
                    :to :client}
            nil
            {:source-player-id player-id}))

;; ---------------------------------------------------------------------------
;; Action handlers
;; ---------------------------------------------------------------------------

(defn- railgun-on-key-down
  "Coin-QTE path fires immediately; otherwise starts 20-tick iron-item charge."
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage player]
  (let [qte (read-coin-qte-status player-id)]
    (cond
      (:perform? qte)
      (do
        (mark-coin-judged! player-id (:coin-uuid qte))
        ;; Kill the coin entity unconditionally (mirrors coin.setDead() before performServer).
        (discard-coin-entity! player-id (:coin-uuid qte))
        (if cost-ok?
          (let [{:keys [performed? reflection-hit? normal-hit-count hit-uuids]} (perform-main-shot! player-id ctx-id exp)]
            (when performed?
              (skill-effects/add-skill-exp! player-id :railgun (railgun-exp-gain (pos? (long (or normal-hit-count 0))) reflection-hit?))
              (when (and (pos? (long (or normal-hit-count 0)))
                         (creeper-hit? (geom/world-id-of player-id) hit-uuids))
                (ach-dispatcher/trigger-custom-event! player-id "electromaster.attack_creeper"))
              (skill-effects/set-main-cooldown! player-id :railgun
                                                (railgun-cooldown-ticks exp))
              (ctx-skill/replace-skill-state! ctx-id
                                     {:fired       true
                                      :mode        :performed
                                      :hit-count   normal-hit-count}))
            (log/debug "Railgun coin-QTE perform" player-id))
          (ctx-skill/replace-skill-state! ctx-id {:fired false :mode :coin-qte-no-resource})))

      (:has-window? qte)
      (do
        (mark-coin-judged! player-id (:coin-uuid qte))
        (ctx-skill/replace-skill-state! ctx-id {:fired false :mode :coin-qte-miss})
        (log/debug "Railgun coin-QTE miss" player-id (:progress qte)))

      (accepted-item-in-hand? player)
      (do
        (ctx-skill/replace-skill-state! ctx-id
                               {:fired        false
                                :mode         :item-charge
                                :charge-ticks (item-charge-ticks)
                                :hit-count    0})
        (send-charge-start! ctx-id player-id))

      :else
      (ctx-skill/replace-skill-state! ctx-id {:fired false :mode :idle-no-trigger}))))

(defn- railgun-on-key-tick
  "Item-charge path: countdown; auto-fires when charge-ticks reaches zero."
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      ;; player-ref is nil on this path — the tick comes from the server tick
      ;; loop, not the network — so resolve the live player from player-id.
      ;; Without it accepted-item-in-hand? saw nil, answered false, and the
      ;; item charge could never reach its shot: the charge ran its full
      ;; second and then silently landed in :item-charge-failed.
      (let [player (or player-ref (resolve-player-ref player-id))
            skill-state (:skill-state ctx-data)]
        (when (= (:mode skill-state) :item-charge)
          (let [ticks-left (max 0 (int (or (:charge-ticks skill-state) 0)))]
            (if (<= ticks-left 1)
              (do
                (if (and (accepted-item-in-hand? player)
                         (consume-item-for-shot! player)
                         cost-ok?)
                  (let [{:keys [performed? reflection-hit? normal-hit-count hit-uuids]}
                        (perform-main-shot! player-id ctx-id exp)]
                    (when performed?
                      (skill-effects/add-skill-exp! player-id :railgun (railgun-exp-gain (pos? (long (or normal-hit-count 0))) reflection-hit?))
                      (when (and (pos? (long (or normal-hit-count 0)))
                                 (creeper-hit? (geom/world-id-of player-id) hit-uuids))
                        (ach-dispatcher/trigger-custom-event! player-id "electromaster.attack_creeper"))
                      (skill-effects/set-main-cooldown! player-id :railgun
                                                        (railgun-cooldown-ticks exp))
                      (ctx-skill/replace-skill-state! ctx-id
                                             {:fired     true
                                              :mode      :performed
                                              :hit-count normal-hit-count})))
                  (ctx-skill/replace-skill-state! ctx-id
                                         (assoc skill-state :fired false :mode :item-charge-failed)))
                (set-skill-state! ctx-id [:charge-ticks] 0)
                (send-charge-end! ctx-id player-id))
              (do
                (set-skill-state! ctx-id [:charge-ticks] (dec ticks-left))
                (send-charge-update! ctx-id player-id (dec ticks-left))))))))
    (catch Exception e
      (log/debug "railgun-on-key-tick error" (ex-message e)))))

(defn- railgun-on-key-up
  "Cancels an unfinished item charge. Cooldown is only applied on successful perform."
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx)
          mode        (:mode skill-state)]
      (when (and (= mode :item-charge) (not (:fired skill-state)))
        (ctx-skill/replace-skill-state! ctx-id
                               (assoc skill-state :mode :item-charge-cancelled :charge-ticks 0))
        (send-charge-end! ctx-id player-id))
      (when (:fired skill-state)
        (log/debug "Railgun completed")))))

(defn- railgun-on-key-abort
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (ctx-skill/clear-skill-state! ctx-id)
  ;; Unconditional, matching jet-engine's abort handler: a harmless no-op on
  ;; the client (dissoc of an absent owner-key) when no charge was active.
  (send-charge-end! ctx-id player-id)
  (log/debug "Railgun aborted"))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(declare railgun)

(defskill railgun
  :id              :railgun
  :category-id     :electromaster
  :name-key        "ability.skill.electromaster.railgun"
  :description-key "ability.skill.electromaster.railgun.desc"
  :icon            "textures/abilities/electromaster/skills/railgun.png"
  :ui-position     [164 59]
  :ctrl-id         :railgun
  :cp-consume-speed 1.0
  :overload-consume-speed 1.0
  :cooldown-ticks  1
  :pattern         :charge-window
  :cooldown        {:mode :manual}
  :cost            {:down {:cp       down-cost-cp
                           :overload down-cost-overload
                           :creative? cost-creative?}
                    :tick {:cp       tick-cost-cp
                           :overload tick-cost-overload
                           :creative? cost-creative?}}
  :actions         {:down!  railgun-on-key-down
                    :tick!  railgun-on-key-tick
                    :up!    railgun-on-key-up
                    :abort! railgun-on-key-abort}
  :prerequisites   [{:skill-id :thunder-bolt :min-exp 0.3}
                    {:skill-id :mag-manip    :min-exp 1.0}])

(defn init!
  []
  (item-actions/register-item-action! "ac:coin" :railgun-coin-throw)
  (item-actions/register-item-action! (modid/namespaced-path "coin") :railgun-coin-throw)
  (item-actions/register-action-handler! :railgun-coin-throw register-coin-throw!)
  ;; Spawn ids must carry the mod namespace: the entity type is registered as
  ;; <modid>:entity_coin_throwing, and a bare path would resolve to
  ;; minecraft:entity_coin_throwing (→ DefaultedRegistry pig fallback).
  (item-actions/register-item-entity-spawn!
    "ac:coin"
    {:entity-id (modid/namespaced-path "entity_coin_throwing") :speed 0.0 :unique-per-owner? true})
  (item-actions/register-item-entity-spawn!
    (modid/namespaced-path "coin")
    {:entity-id (modid/namespaced-path "entity_coin_throwing") :speed 0.0 :unique-per-owner? true})
  nil)

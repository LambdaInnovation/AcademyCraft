(ns cn.li.ac.content.ability.vecmanip.vec-reflection
  "VecReflection skill - advanced reflection (toggle).

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.vecmanip.arbitration :as arbitration]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.service.skill-effects :as fx-common]
            [cn.li.ac.ability.service.player-runtime-commands :as prt-cmd]
            [cn.li.ac.ability.service.reflection-damage :as reflection-damage]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]
                        [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :vec-reflection)
(def ^:private vec-reflection-skill-id :vec-reflection)
(def ^:private reflection-target-distance 20.0)

(def ^:dynamic *reflection-chain-id* nil)

(defn- current-reflection-chain-id
  []
  *reflection-chain-id*)

(defn- parse-difficulty-entry [entry]
  (try
    (let [entry* (str entry)
          idx (.lastIndexOf ^String entry* ":")]
      (when (pos? idx)
        [(subs entry* 0 idx)
         (Double/parseDouble (subs entry* (inc idx)))]))
    (catch Exception _
      nil)))

(defn- affected-entity-difficulty []
  (into {}
        (keep parse-difficulty-entry)
        (skill-config/tunable-string-list vec-reflection-skill-id :targeting.affected-entity-difficulty)))

(defn- excluded-entity-ids []
  (cfg-string-set :targeting.excluded-entity-ids))

(defn- large-fireball-ids []
  (cfg-string-set :targeting.large-fireball-ids))

(defn- small-fireball-ids []
  (cfg-string-set :targeting.small-fireball-ids))

(defn- fireball-entity?
  [entity-id]
  (or (contains? (large-fireball-ids) entity-id)
      (contains? (small-fireball-ids) entity-id)))

(defn- current-cp
  [player-id]
  (fx-common/current-cp player-id))

(defn- consume-cp!
  [player-id cp]
  (boolean (:success? (fx-common/perform-resource! player-id 0.0 (double cp) false))))

(defn- enforce-overload-floor!
  [player-id floor-value]
  (fx-common/enforce-overload-floor! player-id floor-value))

(defn vec-reflection-cost-tick-cp
  [_player-id _skill-id exp]
  (cfg-lerp :cost.tick.cp (double (or exp 0.0))))

(defn- get-player-position [player-id]
  (motion-effects/player-position player-id))

(defn- caster-rotation
  "WaveEffect freezes eff.rotationYaw/Pitch to the CASTER's at spawn, and the
  effect is spawned on every client that gets the message — so the angles have
  to travel with it rather than be read off whoever is watching. Recovered from
  the look vector: look = (-sin y cos p, -sin p, cos y cos p)."
  [player-id]
  ;; Purely cosmetic: an angle lookup must never be able to interrupt the
  ;; skill's resource accounting further down the tick.
  (try
    (when-let [look (and (raycast/available?) (raycast/player-look-vector player-id))]
      (let [lx (double (or (:x look) 0.0))
            ly (double (or (:y look) 0.0))
            lz (double (or (:z look) 0.0))]
        {:yaw-rad (Math/atan2 (- lx) lz)
         :pitch-rad (Math/asin (max -1.0 (min 1.0 (- ly))))}))
    (catch Exception _ nil)))

(defn- entity-registry-id [entity]
  (or (:entity-id entity) (:type entity) ""))

(defn- affect-difficulty-with-snapshot [entity excluded-ids difficulty-map]
  (let [eid (entity-registry-id entity)]
    (when-not (or (contains? excluded-ids eid)
                  (:item? entity)
                  (:living? entity)
                  (:mob? entity)
                  (:multipart? entity))
      (double (get difficulty-map eid 1.0)))))

(defn- now-ms []
  (System/currentTimeMillis))

(defn- visited-ttl-ms []
  (* 50 (long (max 1 (cfg-int :tracking.visited-ttl-ticks)))))

(defn- visited-max-size []
  (int (max 16 (cfg-int :tracking.visited-max-size))))

(defn- max-reflections []
  (int (max 1 (cfg-int :combat.max-reflections))))

(defn- pair-key [player-id attacker-id]
  (let [a (str (or player-id ""))
        b (str (or attacker-id ""))]
    (if (neg? (compare a b))
      [a b]
      [b a])))

(defn- new-reflection-chain-id []
  (str (java.util.UUID/randomUUID)))

(defn reflection-owner-key
  "Return the owner key used for VecReflection recursion state.
  Public for diagnostics/tests; gameplay code should call reflect-damage."
  [player-id attacker-id ctx-id chain-id]
  {:ctx-id (or ctx-id :no-context)
   :player-id (str (or player-id ""))
   :pair (pair-key player-id attacker-id)
   :chain-id (or chain-id :no-chain)})

(defn- try-enter-reflection!
  [player-id owner-key]
  (boolean
    (:granted?
     (prt-cmd/run-for-player!
      player-id
      {:command :enter-vec-reflection :owner-key owner-key}))))

(defn- leave-reflection!
  [player-id owner-key]
  (prt-cmd/run-for-player!
   player-id
   {:command :leave-vec-reflection :owner-key owner-key})
  nil)

(defn reset-reflection-runtime-for-test!
  [player-id]
  (prt-cmd/run-for-player!
   player-id
   {:command :reset-vec-reflection-runtime})
  nil)

(defn reflection-runtime-snapshot
  [player-id]
  (prt-cmd/vec-reflection-state player-id))

(defn mark-reflecting-for-test!
  [player-id attacker-id ctx-id chain-id]
  (let [owner-key (reflection-owner-key player-id attacker-id ctx-id chain-id)]
    (prt-cmd/run-for-player! player-id {:command :enter-vec-reflection :owner-key owner-key})
    owner-key))

(defn set-reflection-depth-for-test!
  [player-id attacker-id ctx-id chain-id depth]
  (let [owner-key (reflection-owner-key player-id attacker-id ctx-id chain-id)]
    (prt-cmd/run-for-player!
     player-id
     {:command :set-vec-reflection-depth :owner-key owner-key :depth (long depth)})
    owner-key))

(defn- increment-reflection-depth!
  [player-id owner-key]
  (let [state (prt-cmd/vec-reflection-state player-id)
        next-depth (inc (long (or (get-in state [:reflection-depths owner-key]) 0)))]
    (prt-cmd/run-for-player!
     player-id
     {:command :set-vec-reflection-depth :owner-key owner-key :depth next-depth})
    (prt-cmd/vec-reflection-state player-id)))

(defn- normalize-visited-map [visited now]
  (cond
    (map? visited)
    (into {}
          (keep (fn [[k v]]
                  (when k
                    [(str k) (long (if (number? v) v now))])))
          visited)

    (set? visited)
    (into {}
          (map (fn [uuid] [(str uuid) now]))
          visited)

    :else
    {}))

(defn- prune-visited-map [visited now ttl-ms max-size]
  (let [cutoff (- now (long ttl-ms))
        alive (into {}
                    (filter (fn [[_uuid ts]]
                              (>= (long ts) cutoff)))
                    visited)]
    (if (<= (count alive) max-size)
      alive
      (->> alive
           (sort-by (fn [[_uuid ts]] (long ts)) >)
           (take max-size)
           (into {})))))

(defn- entity-uuid-str-in-map?
  [visited entity]
  (contains? visited (str (:uuid entity))))

(defn- active-vec-reflection-ctx-id
  "First context of `player-id` whose vec-reflection toggle is active,
  optionally excluding `exclude-ctx-id`. Returns the context's plain string id
  (ctx-data :id), NOT the transport key — get-all-contexts keys can be
  `[:side route ctx-id]` vectors, and the toggle/fx state the activate path
  wrote is keyed on the string id carried by the key-down message, so the
  deactivate path must use the same id or its fx-end never matches the
  fx-start owner-key on the client."
  ([player-id]
   (active-vec-reflection-ctx-id player-id nil))
  ([player-id exclude-ctx-id]
   (->> (ctx/get-all-contexts)
        (keep (fn [[ctx-id ctx-data]]
                (let [id (or (:id ctx-data)
                             (if (vector? ctx-id) (last ctx-id) ctx-id))]
                  (when (and (not= id exclude-ctx-id)
                             (= (:player-uuid ctx-data) player-id)
                             (toggle/is-toggle-active? ctx-data :vec-reflection))
                    id))))
        first)))

(defn- set-skill-state-key!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- update-skill-state-root!
  [ctx-id f]
  (ctx-skill/update-skill-state-root! ctx-id f))

(defn- deactivate-and-terminate!
  [ctx-id reason]
  (toggle/remove-toggle! ctx-id :vec-reflection)
  (update-skill-state-root!
   ctx-id
   #(dissoc % :vec-reflection-visited :vec-reflection-visited-map :vec-reflection-overload-keep))
  (fx/send! ctx-id {:topic :vec-reflection/fx-end :mode :end})
  ;; Notify the client so its mirror context is cleaned up — plain
  ;; terminate-context! with nil leaves the client-side context registered
  ;; forever.
  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!)
  (log/debug "VecReflection: Deactivated" reason)
  nil)

(defn- add-exp! [player-id amount]
  (fx-common/add-skill-exp! player-id vec-reflection-skill-id amount))

;; Original's MSG_REFLECT_ENTITY/MSG_EFFECT are both sendToClient, and their
;; c_reflectEntity/reflectEffect client handlers (WaveEffect spawn + world
;; sound) have no isLocal gate — bystanders see/hear the reflection too.
(defn- send-fx-reflect-entity!
  "c_reflectEntity re-runs reflect(ent, player) on the client so the projectile
  turns the instant the message lands rather than holding its old course until
  the next velocity sync. Rather than have the client redo the aim solve --
  which would need its own raycast and could disagree with the server's -- the
  velocity the server just applied travels with the message."
  ([ctx-id player-id entity] (send-fx-reflect-entity! ctx-id player-id entity nil))
  ([ctx-id player-id entity velocity]
   (fx/send-local-and-nearby! ctx-id {:topic :vec-reflection/fx-reflect-entity :mode :reflect-entity} nil
             (merge
               {:x (double (or (:x entity) 0.0))
                :y (double (+ (double (or (:y entity) 0.0))
                              (double (or (:eye-height entity)
                                          (:height entity)
                                          0.0))))
                :z (double (or (:z entity) 0.0))
                :reflected? true}
               (when-let [uuid (:uuid entity)]
                 {:entity-uuid (str uuid)})
               (when velocity
                 {:vx (double (:x velocity))
                  :vy (double (:y velocity))
                  :vz (double (:z velocity))})
               (caster-rotation player-id)))))

(defn- send-fx-play! [ctx-id player-id pos]
  (fx/send-local-and-nearby! ctx-id {:topic :vec-reflection/fx-play :mode :play} nil
            (merge
              {:x (double (or (:x pos) 0.0))
               :y (double (or (:y pos) 0.0))
               :z (double (or (:z pos) 0.0))}
              (caster-rotation player-id))))

(defn notify-beam-reflected!
  "Upstream onReflect(ReflectEvent): when someone's reflectible attack is
  cancelled by this player's VecReflection, the DEFENDER's context spawns a
  wave just in front of itself —

    player.pos + (0, ranged(0.4, 1.3), 0) + normalize(attackerHead - selfHead) * 0.5

  — not at the attacker. That is a second, separate wave from the one
  handleAttack puts on the attacker, and the port had no counterpart for it:
  a beam bounced off you produced the reflected beam and nothing on you.

  Called by the beam skills through vec-reflection-interaction, since the wave
  belongs to the reflector's own context, not the caster's."
  [reflector-player-id attacker-pos]
  (try
    (when-let [ctx-id (some (fn [[_ ctx-data]]
                              (when (and (= (:player-uuid ctx-data) reflector-player-id)
                                         (toggle/is-toggle-active? ctx-data :vec-reflection))
                                (:id ctx-data)))
                            (ctx/get-all-contexts))]
      (when-let [self (get-player-position reflector-player-id)]
        (let [sx (double (:x self)) sy (double (:y self)) sz (double (:z self))
              ;; entityHeadPos on both sides cancels out into the horizontal
              ;; direction from us to them, so eye height does not matter here.
              dx (- (double (or (:x attacker-pos) sx)) sx)
              dy (- (double (or (:y attacker-pos) sy)) sy)
              dz (- (double (or (:z attacker-pos) sz)) sz)
              len (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
              [nx ny nz] (if (> len 1.0e-6)
                           [(/ dx len) (/ dy len) (/ dz len)]
                           [0.0 0.0 0.0])]
          (send-fx-play! ctx-id reflector-player-id
                         {:x (+ sx (* nx 0.5))
                          :y (+ sy (+ 0.4 (rand 0.9)) (* ny 0.5))
                          :z (+ sz (* nz 0.5))}))))
    (catch Exception e
      (log/warn "VecReflection: beam-reflect wave failed:" (ex-message e)))))

(defn- try-find-attacker-pos [player-id attacker-id]
  (or (when-let [st (fx-common/get-player-state attacker-id)]
        (get st :position))
      (when-let [self-pos (get-player-position player-id)]
        (when (world-effects/available?)
          (first (filter (fn [ent] (= (:uuid ent) attacker-id))
                         (world-effects/find-entities-in-radius
                          (:world-id self-pos)
                          (:x self-pos)
                          (:y self-pos)
                          (:z self-pos)
                          (cfg-double :targeting.attacker-search-radius))))))))

(defn- nearer-hit
  [block-hit entity-hit]
  (cond
    (nil? block-hit) (when entity-hit (assoc entity-hit :hit-type :entity))
    (nil? entity-hit) (assoc block-hit :hit-type :block)
    (<= (double (or (:distance block-hit) Double/POSITIVE_INFINITY))
        (double (or (:distance entity-hit) Double/POSITIVE_INFINITY)))
    (assoc block-hit :hit-type :block)
    :else
    (assoc entity-hit :hit-type :entity)))

(defn- reflection-target-position
  [player-id player-pos look-vec]
  (let [ray-pos (or (raycast/player-position player-id) player-pos)
        world-id (or (:world-id ray-pos) (:world-id player-pos))
        start-x (double (or (:x ray-pos) (:x player-pos) 0.0))
        start-y (double (or (:eye-y ray-pos)
                            (+ (double (or (:y ray-pos) (:y player-pos) 0.0))
                               1.62)))
        start-z (double (or (:z ray-pos) (:z player-pos) 0.0))
        dir-x (double (or (:x look-vec) 0.0))
        dir-y (double (or (:y look-vec) 0.0))
        dir-z (double (or (:z look-vec) 0.0))
        block-hit (raycast/raycast-blocks
                    world-id start-x start-y start-z
                    dir-x dir-y dir-z reflection-target-distance)
        entity-hit (raycast/raycast-from-player
                     player-id reflection-target-distance false)
        hit (nearer-hit block-hit entity-hit)]
    (if hit
      {:x (double (or (:hit-x hit) (:x hit) start-x))
       :y (+ (double (or (:hit-y hit) (:y hit) start-y))
             (if (= :entity (:hit-type hit))
               (* 0.6 (double (or (:eye-height hit) 0.0)))
               0.0))
       :z (double (or (:hit-z hit) (:z hit) start-z))}
      {:x (+ start-x (* dir-x reflection-target-distance))
       :y (+ start-y (* dir-y reflection-target-distance))
       :z (+ start-z (* dir-z reflection-target-distance))})))

(defn- reflected-velocity
  [target-pos entity entity-vel]
  (let [speed (Math/sqrt (+ (Math/pow (double (or (:x entity-vel) 0.0)) 2.0)
                            (Math/pow (double (or (:y entity-vel) 0.0)) 2.0)
                            (Math/pow (double (or (:z entity-vel) 0.0)) 2.0)))
        head-x (double (or (:x entity) 0.0))
        head-y (+ (double (or (:y entity) 0.0))
                  (double (or (:eye-height entity)
                              (:height entity)
                              0.0)))
        head-z (double (or (:z entity) 0.0))
        dx (- (double (:x target-pos)) head-x)
        dy (- (double (:y target-pos)) head-y)
        dz (- (double (:z target-pos)) head-z)
        length (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
        scale (if (pos? length) (/ speed length) 0.0)]
    {:x (* dx scale)
     :y (* dy scale)
     :z (* dz scale)}))

(defn- reflection-target-id
  [attacker-id damage-source]
  (or (when (and damage-source (entity-damage/available?))
        (entity-damage/reflection-target-entity-id damage-source))
      attacker-id))

(defn- reflected-damage-source?
  [damage-source]
  (boolean
   (and damage-source
        (entity-damage/available?)
        (entity-damage/vec-reflection-damage-source? damage-source))))

(defn vec-reflection-on-key-down
  "Activate on the first press and terminate the context on the next press.

  The client's slot ctx-id is cleared at key-up, so the second press arrives
  on a NEW context — deactivate the still-toggle-active context of a previous
  press instead (the original's press-again-to-exit, ActivateHandlers
  terminatesContext)."
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (if-let [active-ctx-id (active-vec-reflection-ctx-id player-id ctx-id)]
        (do
          (deactivate-and-terminate! active-ctx-id :manual)
          (deactivate-and-terminate! ctx-id :manual))
        (let [exp (double (or exp 0.0))]
          (toggle/activate-toggle! ctx-id :vec-reflection)
          (set-skill-state-key! ctx-id :vec-reflection-visited-map {})
          (let [overload-cost (cfg-lerp :cost.overload-keep exp)]
            (fx-common/perform-resource! player-id overload-cost 0.0 false)
            (let [overload-keep (double
                                 (fx-common/player-path
                                  player-id
                                  [:resource-data :cur-overload]
                                  overload-cost))]
              (set-skill-state-key! ctx-id :vec-reflection-overload-keep overload-keep)
              (enforce-overload-floor! player-id overload-keep)))
          (fx/send! ctx-id {:topic :vec-reflection/fx-start :mode :start})
          (log/debug "VecReflection: Activated"))))
    (catch Exception e
      (log/warn "VecReflection key-down failed:" (ex-message e)))))

(defn- vec-reflection-on-key-tick-body
  [player-id ctx-id exp cost-ok?]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (when (toggle/is-toggle-active? ctx-data :vec-reflection)
      (let [exp (double (or exp 0.0))
            overload-keep (get-in ctx-data [:skill-state :vec-reflection-overload-keep] 0.0)]
        (toggle/update-toggle-tick! ctx-id :vec-reflection)
        (enforce-overload-floor! player-id overload-keep)

        (when-not cost-ok?
          (deactivate-and-terminate! ctx-id :insufficient-cp))

        (when (and cost-ok?
                   (toggle/is-toggle-active? (or (ctx-skill/get-context ctx-id) ctx-data) :vec-reflection))
          (when-let [pos (get-player-position player-id)]
            (when (world-effects/available?)
              (let [world-id (:world-id pos)
                    x (:x pos)
                    y (:y pos)
                    z (:z pos)
                    now (now-ms)
                    ttl-ms (visited-ttl-ms)
                    max-size (visited-max-size)
                    excluded-ids (excluded-entity-ids)
                    difficulty-map (affected-entity-difficulty)
                    dual-active? (arbitration/dual-active? player-id)
                    arbitration-allowed? (or (not dual-active?)
                                             (arbitration/skill-allowed-in-dual-active? :vec-reflection))
                    entities (world-effects/find-entities-in-radius
                                                                    world-id x y z (cfg-double :targeting.radius))
                    visited (normalize-visited-map
                             (or (get-in ctx-data [:skill-state :vec-reflection-visited-map])
                                 (get-in ctx-data [:skill-state :vec-reflection-visited]))
                             now)
                    fresh-entities (remove (partial entity-uuid-str-in-map? visited)
                                           entities)
                    replacement-ids (volatile! #{})]
                (doseq [entity fresh-entities]
                  (let [entity-id (:uuid entity)
                        eid (entity-registry-id entity)
                        difficulty (affect-difficulty-with-snapshot entity excluded-ids difficulty-map)]
                    (when (and entity-id (not= entity-id player-id) difficulty)
                      (when-let [look-vec (and (raycast/available?)
                                               (raycast/player-look-vector player-id))]
                        (when (and arbitration-allowed?
                                   (arbitration/claim-projectile! player-id :vec-reflection entity-id))
                          (let [target-pos (reflection-target-position player-id pos look-vec)
                                entity-vel (when (motion-effects/entity-motion-available?)
                                             (motion-effects/entity-velocity
                                              world-id
                                              entity-id))
                                reflected-vel (reflected-velocity target-pos entity entity-vel)
                                vel-x (:x reflected-vel)
                                vel-y (:y reflected-vel)
                                vel-z (:z reflected-vel)
                                reflect-cost (* difficulty (cfg-lerp :cost.reflect-entity.cp exp))]
                            (when (consume-cp! player-id reflect-cost)
                              (if (and (world-effects/available?)
                                       (fireball-entity? eid))
                                  (let [spawn-result (world-effects/spawn-projectile!
                                                                                    world-id
                                                                                    {:entity-id eid
                                                                                     :x (double (or (:x entity) 0.0))
                                                                                     :y (double (or (:y entity) 0.0))
                                                                                     :z (double (or (:z entity) 0.0))
                                                                                     :vx vel-x
                                                                                     :vy vel-y
                                                                                     :vz vel-z
                                                                                     :owner-uuid (:owner-uuid entity)
                                                                                     :explosion-power (:explosion-power entity)})
                                        spawned? (boolean (:success? spawn-result))]
                                    (when-let [spawned-id (and spawned? (:uuid spawn-result))]
                                      (vswap! replacement-ids conj (str spawned-id)))
                                    (when (and spawned? (motion-effects/entity-motion-available?))
                                      (motion-effects/discard-entity! world-id entity-id))
                                    (when (and (not spawned?) (motion-effects/entity-motion-available?))
                                      (motion-effects/set-entity-velocity!
                                                                   world-id
                                                                   entity-id vel-x vel-y vel-z)))
                                (when (motion-effects/entity-motion-available?)
                                  (motion-effects/set-entity-velocity!
                                   world-id
                                   entity-id vel-x vel-y vel-z)))
                              (add-exp! player-id (* difficulty (cfg-double :progression.exp-reflect-entity-scale)))
                              ;; A replaced fireball is a NEW entity, so the
                              ;; client has nothing of ours to redirect; only
                              ;; the in-place reflects carry a velocity.
                              (send-fx-reflect-entity! ctx-id player-id entity reflected-vel)
                              (log/debug "VecReflection: Reflected entity" entity-id))))))))
                (let [visited'
                      (persistent!
                       (reduce (fn [acc uuid]
                                 (assoc! acc uuid now))
                               (reduce (fn [acc entity]
                                         (if-let [uuid (:uuid entity)]
                                           (assoc! acc (str uuid) now)
                                           acc))
                                       (transient visited)
                                       entities)
                               @replacement-ids))
                      pruned (prune-visited-map visited' now ttl-ms max-size)]
                  (update-skill-state-root! ctx-id #(-> %
                                                        (assoc :vec-reflection-visited-map pruned)
                                                        (dissoc :vec-reflection-visited))))))))))))

(defn vec-reflection-on-key-tick
  "Tick handler - consume resources and reflect nearby projectiles."
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (vec-reflection-on-key-tick-body player-id ctx-id exp cost-ok?)
    (catch Exception e
      (log/debug "VecReflection key-tick failed:" (ex-message e)))))

(defn vec-reflection-on-key-up
  "No-op for toggle skills."
  [_ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  nil)

(defn vec-reflection-on-key-abort
  "Deactivate on abort."
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (toggle/remove-toggle! ctx-id :vec-reflection)
    (update-skill-state-root! ctx-id #(dissoc % :vec-reflection-visited :vec-reflection-visited-map :vec-reflection-overload-keep))
    (fx/send! ctx-id {:topic :vec-reflection/fx-end :mode :end})
    (log/debug "VecReflection aborted")
    (catch Exception e
      (log/warn "VecReflection key-abort failed:" (ex-message e)))))

(defn reflect-damage
  "Reflect incoming damage back to attacker when VecReflection is active.
  Returns tuple [performed? reduced-damage]."
  [player-id attacker-id original-damage]
  (try
    (let [chain-id (or (current-reflection-chain-id) (new-reflection-chain-id))]
      (binding [*reflection-chain-id* chain-id]
        (let [ctx-id (active-vec-reflection-ctx-id player-id)
              owner-key (reflection-owner-key player-id attacker-id ctx-id chain-id)]
          (if-not (try-enter-reflection! player-id owner-key)
            [false original-damage]
            (try
              (if-let [state (fx-common/get-player-state player-id)]
                (let [depth-state (increment-reflection-depth! player-id owner-key)
                      depth (max 0 (dec (long (or (get-in depth-state [:reflection-depths owner-key]) 1))))
                      exp (skill-exp player-id)
                      max-depth (max-reflections)
                      reflect-multiplier (* (cfg-lerp :combat.damage-multiplier exp)
                                            (Math/pow 0.5 (double depth)))
                      reflected-damage (* original-damage reflect-multiplier)
                      requested-consumption (* original-damage (cfg-lerp :cost.damage.cp exp))
                      consumption (min (current-cp player-id) requested-consumption)]
                  (if (< depth max-depth)
                    (do
                      (when (pos? consumption)
                        (consume-cp! player-id consumption))
                      (when (and attacker-id (entity-damage/available?))
                        (let [world-id (or (get-in state [:position :world-id])
                                           (fx-common/player-path attacker-id [:position :world-id])
                                           "minecraft:overworld")]
                           (reflection-damage/enqueue!
                            {:world-id world-id
                             :caster-id player-id
                             :target-id attacker-id
                             :damage reflected-damage
                             :chain-id chain-id})))
                      (add-exp! player-id (* original-damage (cfg-double :progression.exp-damage-scale)))
                      (when ctx-id
                        (when-let [attacker-pos (and attacker-id (try-find-attacker-pos player-id attacker-id))]
                          (send-fx-play! ctx-id player-id attacker-pos)))
                      [true (max 0.0 (- original-damage reflected-damage))])
                    [false original-damage]))
                [false original-damage])
              (finally
                (leave-reflection! player-id owner-key)))))))
    (catch Exception e
      (log/warn "VecReflection reflect-damage failed:" (ex-message e))
      [false original-damage])))

(defn can-cancel-attack?
  "Pure precheck for Attack-stage cancel semantics.
  Mirrors original passby gate: only cancels when reflection can actually perform."
  ([player-id attacker-id original-damage]
   (can-cancel-attack? player-id attacker-id original-damage nil))
  ([player-id _attacker-id original-damage damage-source]
   (try
     (if (and (not (reflected-damage-source? damage-source))
              (fx-common/get-player-state player-id))
       (let [ctx-id (active-vec-reflection-ctx-id player-id)
             exp (skill-exp player-id)
             reflected-damage (* original-damage (cfg-lerp :combat.damage-multiplier exp))
             min-reflected-damage (cfg-double :combat.min-reflected-damage)]
         (and ctx-id
              (>= reflected-damage min-reflected-damage)))
       false)
     (catch Exception e
       (log/warn "VecReflection can-cancel-attack failed:" (ex-message e))
       false))))

(defn- on-precheck-cancel-side-effect!
  "Run reflection side-effects during precheck cancel path so platforms
  without mutable hurt-stage hooks still execute reflection behavior."
  [player-id attacker-id original-damage damage-source]
  (when (can-cancel-attack? player-id attacker-id original-damage damage-source)
    (reflect-damage player-id
                    (reflection-target-id attacker-id damage-source)
                    original-damage)
    true))

(declare vec-reflection)

(defskill vec-reflection
  :id :vec-reflection
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.vec_reflection"
  :description-key "ability.skill.vecmanip.vec_reflection.desc"
  :icon "textures/abilities/vecmanip/skills/vec_reflection.png"
  :ui-position [210 50]
  :ctrl-id :vec-reflection
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks 0
  :pattern :release-cast
  :input-policy {:terminate-on-key-up? false
                 :keep-active-on-key-up? true}
  :cost {:tick {:cp vec-reflection-cost-tick-cp}}
  :actions {:down! vec-reflection-on-key-down
            :tick! vec-reflection-on-key-tick
            :up! vec-reflection-on-key-up
            :abort! vec-reflection-on-key-abort}
  :prerequisites [{:skill-id :vec-deviation :min-exp 0.0}])

(defn init!
  []
  (damage-handler/register-toggle-damage-handler!
    :vec-reflection-damage
    :vec-reflection
    (fn [player-id attacker-id damage damage-source]
      (if (reflected-damage-source? damage-source)
        [damage {:handler :vec-reflection :skipped :reflection-source}]
        (let [target-id (reflection-target-id attacker-id damage-source)
              [_performed reduced-damage] (reflect-damage player-id target-id damage)]
          [reduced-damage {:handler :vec-reflection}])))
    60)
  (damage-handler/register-attack-cancel-check!
    :vec-reflection
    can-cancel-attack?)
  (damage-handler/register-attack-precheck-side-effect!
    :vec-reflection
    on-precheck-cancel-side-effect!)
  nil)

(ns cn.li.ac.content.ability.vecmanip.vec-deviation
  "VecDeviation skill - persistent projectile deflection toggle.

  Matches the original AcademyCraft context mechanics while retaining the
  modern renderer and projectile-arbitration enhancements."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.content.ability.vecmanip.arbitration :as arbitration]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :vec-deviation)
(def ^:private vec-deviation-skill-id :vec-deviation)

(defn- parse-difficulty-entry [entry]
  (try
    (let [entry* (str entry)
          idx (.lastIndexOf ^String entry* ":")]
      (when (pos? idx)
        [(subs entry* 0 idx)
         (Double/parseDouble (subs entry* (inc idx)))]))
    (catch Exception _ nil)))

(defn- affected-entity-difficulty
  "Preserve the original EntityAffection.find(...).toList bug: only the first
  valid configured class receives its configured difficulty."
  []
  (into {}
        (comp (keep parse-difficulty-entry) (take 1))
        (skill-config/tunable-string-list
         vec-deviation-skill-id
         :targeting.affected-entity-difficulty)))

(defn- excluded-entity-ids []
  (cfg-string-set :targeting.excluded-entity-ids))

(defn- large-fireball-ids []
  (cfg-string-set :targeting.large-fireball-ids))

(defn- small-fireball-ids []
  (cfg-string-set :targeting.small-fireball-ids))

(defn- current-cp [player-id]
  (skill-effects/current-cp player-id))

(defn- consume-cp! [player-id cp]
  (boolean
   (:success?
    (skill-effects/perform-resource! player-id 0.0 (double cp) false))))

(defn- get-player-position [player-id]
  (motion-effects/player-position player-id))

(defn- entity-registry-id [entity]
  (or (:entity-id entity) (:type entity) ""))

(defn- excluded-entity? [entity]
  (let [entity-id (entity-registry-id entity)]
    (or (contains? (excluded-entity-ids) entity-id)
        (:item? entity)
        (:living? entity)
        (:mob? entity)
        (:multipart? entity))))

(defn- affect-difficulty [entity]
  (let [entity-id (entity-registry-id entity)]
    (when-not (excluded-entity? entity)
      (double (get (affected-entity-difficulty) entity-id 1.0)))))

(defn- active-vec-deviation-ctx-id
  "First context of `player-id` whose vec-deviation toggle is active,
  optionally excluding `exclude-ctx-id`. Returns the context's plain string id
  (ctx-data :id), NOT the transport key — get-all-contexts keys can be
  `[:side route ctx-id]` vectors, and the toggle/fx state the activate path
  wrote is keyed on the string id carried by the key-down message, so the
  deactivate path must use the same id or its fx-end never matches the
  fx-start owner-key on the client."
  ([player-id]
   (active-vec-deviation-ctx-id player-id nil))
  ([player-id exclude-ctx-id]
   (->> (ctx/get-all-contexts)
        (keep (fn [[ctx-id ctx-data]]
                (let [id (or (:id ctx-data)
                             (if (vector? ctx-id) (last ctx-id) ctx-id))]
                  (when (and (not= id exclude-ctx-id)
                             (= (:player-uuid ctx-data) player-id)
                             (toggle/is-toggle-active? ctx-data :vec-deviation))
                    id))))
        first)))

(defn- set-skill-state-key! [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- update-skill-state-root! [ctx-id f]
  (ctx-skill/update-skill-state-root! ctx-id f))

(defn- add-exp! [player-id amount]
  (skill-effects/add-skill-exp! player-id :vec-deviation amount))

(defn- caster-rotation
  "WaveEffect freezes eff.rotationYaw/Pitch to the CASTER's at spawn and the
  effect plays on every client that gets the message, so the angles travel with
  it. Recovered from the look vector: look = (-sin y cos p, -sin p, cos y cos p)."
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

(defn- send-fx-stop-entity! [ctx-id player-id entity marked?]
  (fx/send-local-and-nearby!
   ctx-id
   {:topic :vec-deviation/fx-stop-entity :mode :stop-entity}
   nil
   (merge
     {:x (double (or (:x entity) 0.0))
      :y (+ (double (or (:y entity) 0.0))
            (if marked?
              (double (or (:eye-height entity) (:height entity) 0.0))
              0.0))
      :z (double (or (:z entity) 0.0))
      :marked? (boolean marked?)}
     ;; c_stopEntity stops the projectile locally whether or not it is marked,
     ;; so the uuid rides along even for the fireball cases.
     (when-let [uuid (:uuid entity)]
       {:entity-uuid (str uuid)})
     (caster-rotation player-id))))

(defn- send-fx-play! [ctx-id pos]
  (fx/send-local-and-nearby!
   ctx-id
   {:topic :vec-deviation/fx-play :mode :play}
   nil
   {:x (double (or (:x pos) 0.0))
    :y (double (or (:y pos) 0.0))
    :z (double (or (:z pos) 0.0))}))

(defn- entity-unvisited? [visited entity]
  (not (contains? visited (:uuid entity))))

(defn- clear-vec-deviation-state! [ctx-id]
  (update-skill-state-root!
   ctx-id
   #(dissoc %
            :vec-deviation-visited
            :vec-deviation-marked
            :vec-deviation-overload-floor))
  nil)

(defn- deactivate-and-terminate! [ctx-id reason]
  (toggle/remove-toggle! ctx-id :vec-deviation)
  (clear-vec-deviation-state! ctx-id)
  (fx/send! ctx-id {:topic :vec-deviation/fx-end :mode :end})
  ;; Notify the client so its mirror context (and the wave-ring scan) is
  ;; cleaned up — plain terminate-context! with nil leaves the client-side
  ;; context registered forever.
  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!)
  (log/debug "VecDeviation: Deactivated" reason)
  nil)

(defn vec-deviation-on-key-down
  "Activate on the first press and terminate the context on the next press.

  The client's slot ctx-id is cleared at key-up, so the second press arrives
  on a NEW context — deactivate the still-toggle-active context of a previous
  press instead (the original's press-again-to-exit, ActivateHandlers
  terminatesContext)."
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (if-let [active-ctx-id (active-vec-deviation-ctx-id player-id ctx-id)]
        (do
          (deactivate-and-terminate! active-ctx-id :manual)
          (deactivate-and-terminate! ctx-id :manual))
        (let [activation-overload
              (cfg-lerp :cost.activation.overload (double (or exp 0.0)))]
          (toggle/activate-toggle! ctx-id :vec-deviation)
          (skill-effects/perform-resource!
           player-id activation-overload 0.0 false)
          (let [overload-floor
                (double
                 (skill-effects/player-path
                  player-id
                  [:resource-data :cur-overload]
                  0.0))]
            (set-skill-state-key! ctx-id :vec-deviation-visited #{})
            (set-skill-state-key! ctx-id :vec-deviation-marked #{})
            (set-skill-state-key!
             ctx-id :vec-deviation-overload-floor overload-floor)
            (skill-effects/enforce-overload-floor! player-id overload-floor))
          (fx/send! ctx-id {:topic :vec-deviation/fx-start :mode :start})
          (log/debug "VecDeviation: Activated"))))
    (catch Exception e
      (log/warn "VecDeviation key-down failed:" (ex-message e)))))

(defn- consume-normal-tick-cost! [player-id exp]
  ;; Original g_tick runs on the server in addition to s_tick.
  (skill-effects/perform-resource!
   player-id
   (cfg-lerp :cost.tick.normal-overload exp)
   (cfg-lerp :cost.tick.normal-cp exp)
   false)
  nil)

(defn- process-entity! [ctx-id player-id exp world-id entity]
  (let [entity-uuid (:uuid entity)
        entity-id (entity-registry-id entity)
        difficulty (affect-difficulty entity)
        large-fireball? (contains? (large-fireball-ids) entity-id)
        small-fireball? (contains? (small-fireball-ids) entity-id)]
    (when (and entity-uuid difficulty)
      (let [base-cost (cfg-lerp :cost.deflect.cp exp)
            actual-cost (min (current-cp player-id) base-cost)]
        ;; consumeWithForce spends the remaining CP but never blocks deflection.
        (when (pos? actual-cost)
          (skill-effects/perform-resource!
           player-id 0.0 actual-cost false))
        (cond
          large-fireball?
          (do
            (when (motion-effects/entity-motion-available?)
              (motion-effects/discard-entity! world-id entity-uuid))
            (world-effects/create-explosion!
             world-id
             (double (or (:x entity) 0.0))
             (double (or (:y entity) 0.0))
             (double (or (:z entity) 0.0))
             (double (or (:explosion-power entity)
                         (cfg-double :combat.fireball-explosion-radius)))
             true))

          small-fireball?
          (when (motion-effects/entity-motion-available?)
            (motion-effects/discard-entity! world-id entity-uuid))

          :else
          (when (motion-effects/entity-motion-available?)
            (when (or (:arrow? entity) (= entity-id "minecraft:arrow"))
              (motion-effects/set-projectile-damage!
               world-id entity-uuid 0.0))
            (motion-effects/set-entity-velocity!
             world-id entity-uuid 0.0 0.0 0.0)
            (motion-effects/add-entity-tag!
             world-id entity-uuid "ac_vm_deviated")))
        (add-exp!
         player-id
         (* (cfg-double :progression.exp-deflect-scale) difficulty))
        (let [generic-mark? (not (or large-fireball? small-fireball?))]
          (when generic-mark?
            (update-skill-state-root!
             ctx-id
             #(update %
                      :vec-deviation-marked
                      (fnil conj #{})
                      entity-uuid)))
          (send-fx-stop-entity! ctx-id player-id entity generic-mark?))
        (log/debug
         "VecDeviation: Deflected entity" entity-uuid "difficulty" difficulty)))))

(defn vec-deviation-tick!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (when-let [ctx-data (ctx-skill/get-context ctx-id)]
      (when (toggle/is-toggle-active? ctx-data :vec-deviation)
        (let [exp (double (or exp 0.0))
              scan-cost-result
              (skill-effects/perform-resource!
               player-id 0.0 (cfg-lerp :cost.tick.cp exp) false)]
          (if-not (:success? scan-cost-result)
            (deactivate-and-terminate! ctx-id :insufficient-cp)
            (do
              (when-let [floor
                         (get-in
                          ctx-data
                          [:skill-state :vec-deviation-overload-floor])]
                (skill-effects/enforce-overload-floor! player-id floor))
              (when-let [pos (get-player-position player-id)]
                (when (world-effects/available?)
                  (let [world-id (:world-id pos)
                        entities
                        (world-effects/find-entities-in-radius
                         world-id
                         (:x pos)
                         (:y pos)
                         (:z pos)
                         (cfg-double :targeting.radius))
                        visited
                        (get-in
                         ctx-data
                         [:skill-state :vec-deviation-visited]
                         #{})
                        marked
                        (get-in
                         ctx-data
                         [:skill-state :vec-deviation-marked]
                         #{})
                        dual-active? (arbitration/dual-active? player-id)
                        arbitration-allowed?
                        (or (not dual-active?)
                            (arbitration/skill-allowed-in-dual-active?
                             :vec-deviation))
                        fresh-entities
                        (filter (partial entity-unvisited? visited) entities)]
                    (doseq [entity fresh-entities]
                      (let [entity-uuid (:uuid entity)]
                        (when (and entity-uuid
                                   (not= entity-uuid player-id)
                                   (not (:vec-deviation-marked? entity))
                                   (not (contains? marked entity-uuid))
                                   (not (excluded-entity? entity))
                                   (toggle/is-toggle-active?
                                    (or (ctx-skill/get-context ctx-id)
                                        ctx-data)
                                    :vec-deviation)
                                   arbitration-allowed?
                                   (arbitration/claim-projectile!
                                    player-id
                                    :vec-deviation
                                    entity-uuid))
                          (process-entity!
                           ctx-id player-id exp world-id entity))))
                    (let [visited-ids (into #{} (keep :uuid) entities)]
                      (update-skill-state-root!
                       ctx-id
                       #(update %
                                :vec-deviation-visited
                                (fnil into #{})
                                visited-ids))))))
              (consume-normal-tick-cost! player-id exp))))))
    (catch Exception e
      (log/debug "VecDeviation tick! failed:" (ex-message e)))))

(defn vec-deviation-on-key-up
  [_ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  nil)

(defn vec-deviation-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (toggle/remove-toggle! ctx-id :vec-deviation)
  (clear-vec-deviation-state! ctx-id)
  (fx/send! ctx-id {:topic :vec-deviation/fx-end :mode :end}))

(defn reduce-damage
  "Reduce incoming damage while the registered toggle handler is active."
  [player-id original-damage]
  (try
    (let [damage (double original-damage)]
      (if (skill-effects/get-player-state player-id)
        (if (or (not (Double/isFinite damage))
                (> damage (cfg-double :combat.damage-ignore-threshold)))
          original-damage
          (let [exp (skill-exp player-id)
                reduction-rate (cfg-lerp :combat.damage-reduction exp)
                max-consumption (cfg-lerp :cost.damage.cp exp)
                consumption
                (min (current-cp player-id) (double max-consumption))]
            (when (pos? consumption)
              (consume-cp! player-id consumption))
            (add-exp!
             player-id
             (* damage
                (cfg-double :progression.exp-damage-scale)))
            (when-let [pos (get-player-position player-id)]
              (when-let [ctx-id (active-vec-deviation-ctx-id player-id)]
                (send-fx-play! ctx-id pos)))
            (* damage (- 1.0 reduction-rate))))
        original-damage))
    (catch Exception e
      (log/warn "VecDeviation reduce-damage failed:" (ex-message e))
      original-damage)))

(defskill vec-deviation
  :id :vec-deviation
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.vec_deviation"
  :description-key "ability.skill.vecmanip.vec_deviation.desc"
  :icon "textures/abilities/vecmanip/skills/vec_deviation.png"
  :ui-position [145 53]
  :ctrl-id :vec-deviation
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks 0
  :pattern :release-cast
  :cooldown {:mode :manual}
  :input-policy {:terminate-on-key-up? false
                 :keep-active-on-key-up? true}
  :actions {:down! vec-deviation-on-key-down
            :tick! vec-deviation-tick!
            :up! vec-deviation-on-key-up
            :abort! vec-deviation-abort!}
  :prerequisites [{:skill-id :vec-accel :min-exp 0.0}])

(defn init! []
  (damage-handler/register-toggle-damage-handler!
   :vec-deviation-damage
   :vec-deviation
   (fn [player-id _attacker-id damage _damage-source]
     (let [reduced-damage (reduce-damage player-id damage)]
       [reduced-damage {:handler :vec-deviation}]))
   50)
  nil)

(ns cn.li.ac.ability.effects.world
  (:require [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.util.log :as log]))

(defn execute-spawn-entity-from-player!
  [evt {:keys [entity-id speed]}]
  (when-let [player (:player evt)]
    (entity/player-spawn-entity-by-id! player
                                       (str entity-id)
                                       (double (or speed 0.0))))
  evt)

(defn execute-spawn-lightning!
  [evt {:keys [at visual-only?]}]
  (if-not (world-effects/available?)
    (log/warn "Lightning not spawned: world-effects unavailable" {:evt-keys (keys evt)})
    (let [{:keys [x y z]} (or (when (map? at) at) (get evt at))]
      ;; spawn-lightning! answers false when the level cannot be resolved or
      ;; addFreshEntity refuses the bolt. Swallowing that left "no lightning
      ;; appeared" indistinguishable from "the strike never ran at all".
      (when-not (world-effects/spawn-lightning!
                  (:world-id evt)
                  (double x) (double y) (double z)
                  (boolean visual-only?))
        (log/warn "Lightning spawn refused by the level"
                  {:world-id (:world-id evt) :x x :y y :z z}))))
  evt)

(defn execute-create-explosion!
  [evt {:keys [at radius fire?]}]
  (when (world-effects/available?)
    (let [{:keys [x y z]} (or (when (map? at) at) (get evt at))]
      (world-effects/create-explosion!
        (:world-id evt)
        (double x) (double y) (double z)
        (double (or radius 4.0))
        (boolean fire?))))
  evt)

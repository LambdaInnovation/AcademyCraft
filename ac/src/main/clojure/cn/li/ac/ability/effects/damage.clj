(ns cn.li.ac.ability.effects.damage
  (:require [cn.li.ac.ability.util.balance :as bal]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]))

(defn execute-damage-direct!
  [evt {:keys [target amount damage-type]}]
  (when-let [uuid (or (when (map? target) (:uuid target))
                      (when (keyword? target) (get evt target))
                      (when (string? target) target))]
    (when (entity-damage/available?)
      (entity-damage/apply-direct-damage!
        (:world-id evt)
        uuid
        (double (or amount 0.0))
        (or damage-type :generic))))
  evt)

(defn execute-damage-aoe!
  [evt {:keys [center radius amount damage-type exclude]}]
  (when (and (world-effects/available?) (entity-damage/available?))
    (let [center* (or (when (map? center) center) (get evt center))
          world-id (:world-id evt)
          victims (world-effects/find-entities-in-radius
                                                         world-id
                                                         (double (:x center*))
                                                         (double (:y center*))
                                                         (double (:z center*))
                                                         (double radius))
          excluded (set (concat [(:player-id evt)] (or exclude [])))]
      (doseq [{:keys [uuid x y z]} victims]
        (when-not (contains? excluded uuid)
          (let [dx (- (double x) (double (:x center*)))
                dy (- (double y) (double (:y center*)))
                dz (- (double z) (double (:z center*)))
                dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))
                final (* (double amount) (bal/falloff-linear dist radius))]
            (when (> final 0.0)
              (entity-damage/apply-direct-damage!
                world-id uuid final
                (or damage-type :generic))))))))
  evt)

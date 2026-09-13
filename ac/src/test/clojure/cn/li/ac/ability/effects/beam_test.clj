(ns cn.li.ac.ability.effects.beam-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.effects.beam :as beam]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.fx :as fx]))

(deftest beam-accepts-look-dir-with-delta-keys-test
  (let [evt {:ctx-id "ctx-dxyz"
             :player-id "p1"
             :world-id "w1"
             :eye-pos {:x 10.0 :y 64.0 :z 10.0}
             :look-dir {:dx 0.0 :dy 0.0 :dz 1.0}}
        out (beam/execute-beam! evt {:max-distance 8.0
                                     :visual-distance 6.0
                                     :damage 0.0
                                     :break-blocks? false
                                     :block-energy 0.0})]
    (is (true? (get-in out [:beam-result :performed?])))
    (is (= 6.0 (double (get-in out [:beam-result :visual-distance]))))))

(deftest beam-accepts-look-dir-with-xyz-keys-test
  (let [evt {:ctx-id "ctx-xyz"
             :player-id "p1"
             :world-id "w1"
             :eye-pos {:x 10.0 :y 64.0 :z 10.0}
             :look-dir {:x 0.0 :y 0.0 :z 1.0}}
        out (beam/execute-beam! evt {:max-distance 8.0
                                     :visual-distance 6.0
                                     :damage 0.0
                                     :break-blocks? false
                                     :block-energy 0.0})]
    (is (true? (get-in out [:beam-result :performed?])))
    (is (= 6.0 (double (get-in out [:beam-result :visual-distance]))))))

(deftest reflection-exp-flag-requires-secondary-hit-test
  (let [evt {:ctx-id "ctx-reflect"
             :player-id "p1"
             :world-id "w1"
             :eye-pos {:x 0.0 :y 0.0 :z 0.0}
             :look-dir {:x 1.0 :y 0.0 :z 0.0}
             :reflect-can-fn (fn [_ _] true)}
        params {:radius 2.0
                :query-radius 50.0
                :max-distance 50.0
                :visual-distance 45.0
                :damage 60.0
                :break-blocks? false}]
    (with-redefs [world-effects/available? (constantly true)
                  world-effects/find-entities-in-radius
                  (fn [& _] [{:uuid "reflector" :x 5.0 :y 0.0 :z 0.0}])
                  fx/send-local-and-nearby! (fn [& _] nil)]
      (let [miss (beam/execute-beam!
                   (assoc evt :reflect-shot-fn (fn [& _] false))
                   params)
            hit (beam/execute-beam!
                  (assoc evt :reflect-shot-fn (fn [& _] true))
                  params)]
        (is (false? (get-in miss [:beam-result :reflection-hit?])))
        (is (true? (get-in hit [:beam-result :reflection-hit?])))))))

(deftest damage-transform-runs-after-radial-falloff-test
  (let [seen* (atom nil)]
    (with-redefs [world-effects/available? (constantly true)
                  world-effects/find-entities-in-radius
                  (fn [& _] [{:uuid "target" :x 5.0 :y 0.0 :z 0.0}])
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] nil)]
      (beam/execute-beam!
        {:player-id "p1"
         :world-id "w1"
         :eye-pos {:x 0.0 :y 0.0 :z 0.0}
         :look-dir {:x 1.0 :y 0.0 :z 0.0}
         :damage-transform-fn
         (fn [target-id raw]
           (reset! seen* [target-id raw])
           (* 2.0 raw))}
        {:radius 2.0
         :query-radius 50.0
         :max-distance 50.0
         :damage 60.0
         :break-blocks? false})
      (is (= "target" (first @seen*)))
      (is (= 60.0 (double (second @seen*)))))))

(deftest block-beam-skips-air-and-reaches-solid-block-test
  (let [broken* (atom [])]
    (with-redefs [block-manip/available? (constantly true)
                  block-manip/get-block-hardness
                  (fn [_world x _y _z] (if (= 2 x) 1.0 0.0))
                  block-manip/get-block
                  (fn [_world x _y _z] (when (= 2 x) "minecraft:stone"))
                  block-manip/can-break-block? (fn [& _] true)
                  block-manip/break-block!
                  (fn [_player _world x y z _drop?]
                    (swap! broken* conj [x y z])
                    true)]
      (#'beam/break-blocks!
       :test-skill "p1" "w1"
       {:x 0.1 :y 0.1 :z 0.1}
       {:x 1.0 :y 0.0 :z 0.0}
       3.0 10.0 0.0 0.9)
      (is (some #{[2 0 0]} @broken*)))))

(deftest block-beam-stops-at-unbreakable-block-test
  (let [broken* (atom [])]
    (with-redefs [block-manip/available? (constantly true)
                  block-manip/get-block-hardness
                  (fn [_world x _y _z]
                    (cond
                      (= 1 x) -1.0
                      (= 2 x) 1.0
                      :else 0.0))
                  block-manip/get-block (fn [& _] nil)
                  block-manip/can-break-block? (fn [& _] true)
                  block-manip/break-block!
                  (fn [& args] (swap! broken* conj args))]
      (#'beam/break-blocks!
       :test-skill "p1" "w1"
       {:x 0.1 :y 0.1 :z 0.1}
       {:x 1.0 :y 0.0 :z 0.0}
       3.0 10.0 0.0 0.9)
      (is (empty? @broken*)))))

(deftest block-beam-spares-disk-plane-cell-under-shooter-test
  ;; Reported bug: meltdowner's beam destroyed the ground under the shooter's
  ;; feet. Upstream RangedRayDamage.processLine starts its Plotter one cell in
  ;; from the disk point, so the disk-plane cell itself — the ground block at
  ;; the shooter's feet for a horizontal look — is never visited and the
  ;; player does not fall into their own crater; the beam still carves the
  ;; cells in front of the disk plane.
  (let [broken* (atom [])]
    (with-redefs [block-manip/available? (constantly true)
                  block-manip/get-block-hardness
                  (fn [_world _x _y z] (if (pos? z) 1.0 5.0))
                  block-manip/get-block
                  (fn [_world _x _y z] (when (pos? z) "minecraft:stone"))
                  block-manip/can-break-block? (fn [& _] true)
                  block-manip/break-block!
                  (fn [_player _world x y z _drop?]
                    (swap! broken* conj [x y z])
                    true)]
      (#'beam/break-blocks!
       :test-skill "p1" "w1"
       {:x 0.0 :y 63.0 :z 0.0}
       {:x 0.0 :y 0.0 :z 1.0}
       3.0 10.0 1.0 0.9)
      (is (not-any? (fn [[_ _ z]] (zero? z)) @broken*)
          "the disk-plane cells (ground under the shooter) survive")
      (is (some #{[0 63 1]} @broken*)
          "the cells in front of the disk plane are still carved"))))

(deftest beam-visual-ray-follows-upstream-eye-to-feet-shape-test
  ;; Upstream EntityMDRay renders from eye + look×1 to feet + look×length —
  ;; a ray that descends eye→feet as it travels, unlike the old flat
  ;; eye-level start/end. The fx payload must use that shape so the visual
  ;; coincides with the damage/block origin (trace) at the far end.
  (let [captured* (atom nil)]
    (with-redefs [fx/send-local-and-nearby!
                  (fn [& args] (reset! captured* (last args)))]
      (beam/execute-beam!
        {:ctx-id "ctx-vis"
         :player-id "p1"
         :world-id "w1"
         :eye-pos {:x 10.0 :y 20.0 :z 30.0}
         :look-dir {:x 0.0 :y 0.0 :z 1.0}}
        {:max-distance 8.0
         :visual-distance 5.0
         :damage 0.0
         :break-blocks? false
         :block-energy 0.0
         :fx-topic :test/fx
         ;; execute-beam! reads :trace-pos from params, not evt.
         :trace-pos {:x 10.0 :y 10.0 :z 30.0}}))
    (is (= {:x 10.0 :y 20.0 :z 31.0} (:start @captured*))
        "start = eye + look×1, one unit ahead of the eye")
    (is (= {:x 10.0 :y 10.0 :z 35.0} (:end @captured*))
        "end = trace + look×visual-dist — anchored at the feet plane")
    (is (= 5.0 (double (:hit-distance @captured*))))))

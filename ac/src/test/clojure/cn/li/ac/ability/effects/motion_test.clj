(ns cn.li.ac.ability.effects.motion-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.effects.motion :as motion-effects]))

(deftest set-player-velocity-records-when-bound-test
  (let [calls (atom [])
        evt {:player-id "p1"}]
    (with-redefs [motion-effects/player-motion-available? (constantly true)
                  motion-effects/set-player-velocity! (fn [pid x y z]
                                                 (swap! calls conj [:vel pid x y z])
                                                 true)]
      (is (= evt (motion/execute-set-player-velocity! evt {:x 1.0 :y 2.0 :z -0.5}))))
    (is (= [[:vel "p1" 1.0 2.0 -0.5]] @calls))))

(deftest set-player-velocity-noop-when-unbound-test
  (let [evt {:player-id "p2"}]
    (with-redefs [motion-effects/player-motion-available? (constantly false)]
      (is (= evt (motion/execute-set-player-velocity! evt {:x 1.0 :y 0.0 :z 0.0}))))))

(deftest add-entity-velocity-resolves-target-test
  (let [calls (atom [])
        evt {:world-id "dim1" :tgt "ent-uuid"}]
    (with-redefs [motion-effects/entity-motion-available? (constantly true)
                  motion-effects/add-entity-velocity! (fn [wid uuid x y z]
                                                (swap! calls conj [:add wid uuid x y z])
                                                true)]
      (is (= evt (motion/execute-add-entity-velocity! evt {:target :tgt :x 0.1 :y 0.2 :z 0.3}))))
    (is (= [[:add "dim1" "ent-uuid" 0.1 0.2 0.3]] @calls))))

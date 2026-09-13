(ns cn.li.ac.ability.effects.damage-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.effects.damage :as damage]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(deftest damage-direct-applies-when-bound-test
  (let [calls (atom [])
        evt {:player-id "att" :world-id "overworld" :target :victim :victim "u1" :amount 7.0 :damage-type :magic}]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id entity-uuid damage source-type]
                                                       (swap! calls conj {:world-id world-id
                                                                          :uuid entity-uuid
                                                                          :damage damage
                                                                          :source source-type})
                                                       true)]
      (is (= evt (damage/execute-damage-direct! evt {:target :victim :amount 7.0 :damage-type :magic}))))
    (is (= [{:world-id "overworld" :uuid "u1" :damage 7.0 :source :magic}] @calls))))

(deftest damage-direct-skips-when-unbound-test
  (let [evt {:player-id "p" :world-id "w" :target :t :t "u9" :amount 3.0}]
    (with-redefs [entity-damage/available? (constantly false)]
      (is (= evt (damage/execute-damage-direct! evt {:target :t :amount 3.0}))))))

(deftest damage-aoe-falloff-and-exclude-test
  (let [dcalls (atom [])
        wcalls (atom [])
        evt {:player-id "att"
             :world-id "dim"
             :center {:x 0.0 :y 0.0 :z 0.0}
             :radius 10.0
             :amount 100.0
             :damage-type :generic}]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id entity-uuid damage _]
                                                       (swap! dcalls conj {:world-id world-id
                                                                           :uuid entity-uuid
                                                                           :damage damage}))
                  world-effects/available? (constantly true)
                  world-effects/find-entities-in-radius (fn [_ _ _ _ r]
                                                          (swap! wcalls conj {:radius r})
                                                          [{:uuid "v1" :x 0.0 :y 0.0 :z 0.0}
                                                           {:uuid "att" :x 1.0 :y 0.0 :z 0.0}
                                                           {:uuid "v2" :x 10.0 :y 0.0 :z 0.0}])]
      (damage/execute-damage-aoe! evt {:center :center
                                       :radius 10.0
                                       :amount 100.0
                                       :damage-type :generic
                                       :exclude ["v2"]}))
    (is (= 1 (count @dcalls)))
    (is (= "v1" (:uuid (first @dcalls))))
    (is (= 100.0 (:damage (first @dcalls))))))

(ns cn.li.ac.content.ability.meltdowner.mine-ray-expert-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.content.ability.meltdowner.mine-ray-expert :as expert]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]))

(deftest mine-ray-expert-tick-delegates-with-cfg-test
  (let [calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id field-id _exp]
                                             (case field-id
                                               :mining.break-speed 0.4
                                               0.0))
                  skill-config/lerp-int (fn [_skill-id field-id _exp]
                                          (case field-id
                                            :cooldown.ticks 60
                                            0))
                  skill-config/tunable-double (fn [_skill-id field-id]
                                                (case field-id
                                                  :targeting.range 20.0
                                                  :progression.exp-block 0.001
                                                  0.0))
                  base/mining-ray-tick! (fn [cfg & _]
                                          (swap! calls* conj [cfg])
                                          nil)]
      (cb/apply-invoke expert/mine-ray-expert-tick! :player-id "p1" :ctx-id "ctx-1"))
    (is (= 1 (count @calls*)))
    (is (= {:range 20.0 :break-speed 0.4 :skill-id :mine-ray-expert :exp-block 0.001
            :tool-tier-capped? false :fortune-level 0 :cooldown-ticks 60}
          (ffirst @calls*)))))

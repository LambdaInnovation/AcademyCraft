(ns cn.li.ac.content.ability.meltdowner.mine-ray-luck-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.meltdowner.mine-ray-luck :as luck]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- context-mocks
  [initial]
  (let [ctx* (atom initial)]
    {:ctx* ctx*
     :get-context (fn [_] @ctx*)
     :update-skill-state-root! (fn [_ f & args]
                        (swap! ctx* (fn [ctx-data]
                                      (let [current (or (:skill-state ctx-data) {})
                                            next-state (if (and (= f identity) (= 1 (count args)))
                                                         (first args)
                                                         (apply f current args))]
                                        (assoc ctx-data :skill-state next-state)))))}))

(deftest mine-ray-luck-tick-delegates-with-fortune-cfg-test
  (let [calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id field-id _exp]
                                             (case field-id
                                               :mining.break-speed 1.0
                                               0.0))
                  skill-config/lerp-int (fn [_skill-id field-id _exp]
                                          (case field-id
                                            :cooldown.ticks 60
                                            0))
                  skill-config/tunable-double (fn [_skill-id field-id]
                                                (case field-id
                                                  :targeting.range 20.0
                                                  :progression.exp-block 0.002
                                                  0.0))
                  base/mining-ray-tick! (fn [cfg & _]
                                          (swap! calls* conj [cfg])
                                          nil)]
      (cb/apply-invoke luck/mine-ray-luck-tick! :player-id "p1" :ctx-id "ctx-1"))
    (is (= 1 (count @calls*)))
    (is (= {:range 20.0
            :break-speed 1.0
            :skill-id :mine-ray-luck
            :fortune-level 3
            :tool-tier-capped? false
            :exp-block 0.002
            :cooldown-ticks 60}
           (ffirst @calls*)))))

(deftest mine-ray-luck-breaks-block-with-fortune-level-test
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x 1 :target-y 64 :target-z 2
                                      :hardness-left 0.5 :starting-hardness 1.0}})
        break-calls* (atom [])
        exp-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp-calls* conj args)
                                                 nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-blocks (fn [& _] {:x 1 :y 64 :z 2})
                  bm/available? (constantly true)
                  bm/get-block-hardness (fn [& _] 1.0)
                  bm/can-break-block? (fn [& _] true)
                  bm/break-block! (fn [& args]
                                     (swap! break-calls* conj (vec args))
                                     true)]
      (base/mining-ray-tick! {:range 16.0
                              :break-speed 1.0
                              :skill-id :mine-ray-luck
                              :fortune-level 3
                              :exp-block 0.002}
                             "ctx-1" "p1" :mine-ray-luck 0.0 true 0 nil nil))
    (is (= [["p1" "w" 1 64 2 true 3]] @break-calls*))
    (is (= [["p1" :mine-ray-luck 0.002]] @exp-calls*))
    (is (= {:target-x nil :target-y nil :target-z nil
            :hardness-left (double Float/MAX_VALUE)
            :starting-hardness (double Float/MAX_VALUE)}
           (:skill-state @ctx*)))))

(deftest mine-ray-luck-resets-stale-state-when-raycast-is-unavailable-test
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x 1 :target-y 64 :target-z 2
                                      :hardness-left 0.6 :starting-hardness 1.0}})]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly false)
                  bm/available? (constantly false)]
      (base/mining-ray-tick! {:range 16.0
                              :break-speed 1.0
                              :skill-id :mine-ray-luck
                              :fortune-level 3
                              :exp-block 0.002}
                             "ctx-1" "p1" :mine-ray-luck 0.0 true 0 nil nil))
    (is (= {:target-x nil :target-y nil :target-z nil
            :hardness-left (double Float/MAX_VALUE)
            :starting-hardness (double Float/MAX_VALUE)}
           (:skill-state @ctx*)))))

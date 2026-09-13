(ns cn.li.ac.content.ability.meltdowner.mine-ray-basic-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.content.ability.meltdowner.mine-ray-basic :as basic]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]))

(deftest mine-ray-basic-tick-delegates-with-cfg-test
  (let [calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id field-id _exp]
                                             (case field-id
                                               :mining.break-speed 0.15
                                               0.0))
                  skill-config/lerp-int (fn [_skill-id field-id _exp]
                                          (case field-id
                                            :cooldown.ticks 40
                                            0))
                  skill-config/tunable-double (fn [_skill-id field-id]
                                                (case field-id
                                                  :targeting.range 10.0
                                                  :progression.exp-block 0.001
                                                  0.0))
                  base/mining-ray-tick! (fn [cfg & _]
                                          (swap! calls* conj [cfg])
                                          nil)]
      (cb/apply-invoke basic/mine-ray-basic-tick! :player-id "p1" :ctx-id "ctx-1"))
    (is (= 1 (count @calls*)))
    (is (= {:range 10.0 :break-speed 0.15 :skill-id :mine-ray-basic :exp-block 0.001
            :tool-tier-capped? true :cooldown-ticks 40}
          (ffirst @calls*)))))

(deftest mine-ray-basic-up-and-abort-delegate-reset-test
  (let [calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id field-id _exp]
                                             (case field-id
                                               :mining.break-speed 0.15
                                               0.0))
                  skill-config/lerp-int (fn [_skill-id field-id _exp]
                                          (case field-id
                                            :cooldown.ticks 40
                                            0))
                  skill-config/tunable-double (fn [_skill-id field-id]
                                                (case field-id
                                                  :targeting.range 10.0
                                                  :progression.exp-block 0.001
                                                  0.0))
                  base/mining-ray-up! (fn [cfg & _]
                                        (swap! calls* conj [:up cfg])
                                        nil)
                  base/mining-ray-abort! (fn [cfg & _]
                                           (swap! calls* conj [:abort cfg])
                                           nil)]
      (cb/apply-invoke basic/mine-ray-basic-up! :player-id "p1" :ctx-id "ctx-2")
      (cb/apply-invoke basic/mine-ray-basic-abort! :player-id "p1" :ctx-id "ctx-2"))
    (is (= [:up :abort] (mapv first @calls*)))))

(deftest mine-ray-basic-down-snapshots-actual-post-consumption-overload-test
  ;; Matches original's overloadKeep = ctx.cpData.getOverload: snapshot the
  ;; actual resulting overload stat, not the raw cost delta.
  (let [state-calls* (atom [])
        fx-sends* (atom [])]
    (with-redefs [ctx-skill/replace-skill-state! (fn [ctx-id state]
                                                    (swap! state-calls* conj [ctx-id state])
                                                    nil)
                  skill-effects/player-path (fn [_player-id path _default]
                                              (when (= path [:resource-data :cur-overload]) 137.0))
                  fx/send-local-and-nearby! (fn [ctx-id channel _payload-map payload]
                                              (swap! fx-sends* conj [ctx-id channel payload])
                                              nil)]
      (cb/apply-invoke basic/mine-ray-basic-down! :player-id "p1" :ctx-id "ctx-3" :cost-ok? true))
    (is (= [["ctx-3" {:target-x nil :target-y nil :target-z nil
                       :hardness-left (double Float/MAX_VALUE)
                       :starting-hardness (double Float/MAX_VALUE)
                       :overload-floor 137.0}]]
           @state-calls*))
    ;; Original c_start (MSG_MADEALIVE) spawns the ray + loop sound — the
    ;; down handler must broadcast the fx-start with the variant.
    (is (= [["ctx-3" {:topic :mine-ray/fx-start :mode :start}
             {:variant :basic :source-player-id "p1"}]]
           @fx-sends*))))

(deftest mine-ray-basic-cost-fail-on-tick-terminates-and-applies-cooldown-test
  (let [cooldown-calls* (atom [])
        terminated* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id field-id _exp]
                                             (case field-id
                                               :mining.break-speed 0.15
                                               0.0))
                  skill-config/lerp-int (fn [_skill-id field-id _exp]
                                          (case field-id
                                            :cooldown.ticks 40
                                            0))
                  skill-config/tunable-double (fn [_skill-id field-id]
                                                (case field-id
                                                  :targeting.range 10.0
                                                  :progression.exp-block 0.001
                                                  0.0))
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                      (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                      true)
                  ctx/terminate-context! (fn [ctx-id terminate-fn]
                                           (swap! terminated* conj [ctx-id terminate-fn])
                                           nil)]
      (cb/apply-invoke basic/mine-ray-basic-cost-fail! :player-id "p1" :ctx-id "ctx-4" :cost-stage :tick))
    (is (= [["p1" :mine-ray-basic 40]] @cooldown-calls*))
    (is (= [["ctx-4" nil]] @terminated*))))

(deftest mine-ray-basic-cost-fail-on-down-is-a-noop-test
  (let [cooldown-calls* (atom [])
        terminated* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double (fn [_skill-id _field-id _exp] 0.0)
                  skill-config/lerp-int (fn [_skill-id _field-id _exp] 0)
                  skill-config/tunable-double (fn [_skill-id _field-id] 0.0)
                  skill-effects/set-main-cooldown! (fn [& args]
                                                      (swap! cooldown-calls* conj args)
                                                      true)
                  ctx/terminate-context! (fn [& args]
                                           (swap! terminated* conj args)
                                           nil)]
      (cb/apply-invoke basic/mine-ray-basic-cost-fail! :player-id "p1" :ctx-id "ctx-5" :cost-stage :down))
    (is (empty? @cooldown-calls*))
    (is (empty? @terminated*))))

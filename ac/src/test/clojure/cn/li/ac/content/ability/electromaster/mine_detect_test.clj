(ns cn.li.ac.content.ability.electromaster.mine-detect-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.ac.content.ability.electromaster.mine-detect :as mine-detect]))

(deftest perform-sends-minimal-activation-payload-and-awards-exp-test
  (testing "MineDetect now sends a compact activation payload instead of a scanned ore snapshot"
    (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)
          potion* (atom [])
          cooldown-exp* (atom nil)]
      (with-redefs [skill-effects/skill-exp (fn [_ _] 0.608)
                    skill-config/lerp-double (fn [_ field-id exp]
                                               (case field-id
                                                 :targeting.range 24.0
                                                 :cooldown.ticks (do
                                                                   (reset! cooldown-exp* exp)
                                                                   596.75)
                                                 0.0))
                    skill-config/tunable-double (fn [_ field-id]
                                                  (case field-id
                                                    :progression.exp-cast 0.008
                                                    0.0))
                    skill-config/tunable-int (fn [_ field-id]
                                               (case field-id
                                                 :effect.blindness-duration-ticks 100
                                                 :effect.blindness-amplifier 0
                                                 0))
                    skill-effects/player-path (fn [player-id path & [default]]
                                                (if (= path [:ability-data :level])
                                                  4
                                                  default))
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! potion* conj [:exp player-id skill-id amount])
                                                   nil)
                    skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                       (swap! potion* conj [:cooldown player-id skill-id ticks])
                                                       nil)
                    fx/send! send!
                    potion-effects/available? (constantly true)
                    potion-effects/apply-effect! (fn [& args]
                                                           (swap! potion* conj args)
                                                           nil)]
        (cb/apply-invoke mine-detect/mine-detect-perform!
                         :player-id "mine-detect-player"
                         :ctx-id "ctx-1"
                         :exp 0.6))
      (is (= ["ctx-1" :mine-detect/fx-perform :perform
              {:life-ticks 100
               :rescan-interval 5
               :range 24.0
               :advanced? true}]
             (first @calls*)))
      (is (= [["mine-detect-player" :blindness 100 0]
              [:exp "mine-detect-player" :mine-detect 0.008]
              [:cooldown "mine-detect-player" :mine-detect 596]]
             @potion*))
      (is (= 0.608 @cooldown-exp*)))))

(deftest failed-resource-consumption-does-not-perform-test
  (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        effects* (atom [])]
    (with-redefs [fx/send! send!
                  potion-effects/available? (constantly true)
                  potion-effects/apply-effect! (fn [& args] (swap! effects* conj [:potion args]))
                  skill-effects/add-skill-exp! (fn [& args] (swap! effects* conj [:exp args]))
                  skill-effects/set-main-cooldown! (fn [& args] (swap! effects* conj [:cooldown args]))]
      (cb/apply-invoke mine-detect/mine-detect-perform!
                       :player-id "mine-detect-player"
                       :ctx-id "ctx-cost-fail"
                       :exp 0.6
                       :cost-ok? false))
    (is (empty? @calls*))
    (is (empty? @effects*))))

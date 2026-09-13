(ns cn.li.ac.content.ability.meltdowner.light-shield-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.content.ability.meltdowner.light-shield :as ls]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.ac.ability.effects.potion :as potion-effects]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- reduce-damage-fn []
  (var-get #'cn.li.ac.content.ability.meltdowner.light-shield/light-shield-reduce-damage))

(defn- with-light-shield-env [f]
  (skill-ctx/with-server-skill-context f))

(deftest activate-does-not-manually-send-fx-test
  (testing "activate! initializes state but does not manually emit client FX"
    (with-light-shield-env
      #(with-redefs [ctx-skill/assoc-skill-state! (fn [& _] nil)
                    toggle/activate-toggle! (fn [& _] nil)
                    fx/send! (fn [& _]
                               (throw (ex-info "manual fx send should not happen" {})))
                    skill-effects/player-path (fn [& _] 12.0)
                    skill-config/tunable-int (fn [& _] 1)]
         (is (nil? (cb/apply-invoke ls/light-shield-activate! :ctx-id "ctx-1" :player-id "p-1")))))))

(deftest reduce-damage-respects-absorb-interval-window-test
  (testing "damage is unchanged when absorb interval has not elapsed"
    (let [consume-calls* (atom 0)
          reduce-damage! (reduce-damage-fn)]
      (with-redefs [ctx/get-all-contexts (fn [] {[:server :session "ctx-1"]
                                                 {:player-uuid "p-1"
                                                  :skill-state {:light-shield {:ticks 100
                                                                               :last-absorb-tick 95}
                                                               :toggle {:light-shield {:active true}}}}})
                    toggle/is-toggle-active? (fn [_ _] true)
                    skill-config/tunable-int (fn [_skill-id field-id]
                                               (case field-id
                                                 :combat.absorb-interval-ticks 18
                                                 1))
                    skill-effects/perform-resource! (fn [& _]
                                                      (swap! consume-calls* inc)
                                                      {:success? true})]
        (is (= [10.0 nil]
               (reduce-damage! "p-1" nil 10.0 :magic)))
        (is (= 0 @consume-calls*))))))

(deftest reduce-damage-absorbs-and-updates-last-absorb-tick-test
  (testing "successful absorb clamps by absorb cap and records last absorb tick"
    (let [assoc-calls* (atom [])
          exp-calls* (atom [])
          reduce-damage! (reduce-damage-fn)]
      (with-redefs [ctx/get-all-contexts (fn [] {[:server :session "ctx-2"]
                                                 {:player-uuid "p-2"
                                                  :skill-state {:light-shield {:ticks 100
                                                                               :last-absorb-tick 70}
                                                               :toggle {:light-shield {:active true}}}}})
                    ctx-skill/assoc-skill-state! (fn [& args]
                                                   (swap! assoc-calls* conj args)
                                                   nil)
                    toggle/is-toggle-active? (fn [_ _] true)
                    skill-effects/skill-exp (fn [& _] 0.0)
                    skill-config/tunable-int (fn [_skill-id field-id]
                                               (case field-id
                                                 :combat.absorb-interval-ticks 18
                                                 1))
                    skill-config/lerp-double (fn [_skill-id field-id _exp]
                                               (case field-id
                                                 :combat.absorb-damage 6.0
                                                 :cost.absorb.cp 5.0
                                                 :cost.absorb.overload 0.5
                                                 0.0))
                    skill-config/tunable-double (fn [_skill-id field-id]
                                                    (case field-id
                                                      :progression.exp-attacked 0.001
                                                      0.0))
                    skill-effects/perform-resource! (fn [& _] {:success? true})
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp-calls* conj [player-id skill-id amount])
                                                   nil)]
        (is (= [4.0 {:absorbed 6.0}]
               (reduce-damage! "p-2" nil 10.0 :magic)))
        (is (= 1 (count @assoc-calls*)))
        (is (= [:server :session "ctx-2"] (first (first @assoc-calls*))))
        (is (= 1 (count @exp-calls*)))))))

(deftest reduce-damage-absorb-hit-applies-mark-and-exp-test
  (testing "front-cone absorb updates interval state and scales exp gain"
    (let [assoc-calls* (atom [])
          exp-calls* (atom [])
          reduce-damage! (reduce-damage-fn)]
      (with-redefs [ctx/get-all-contexts (fn [] {[:server :session "ctx-3"]
                                                 {:player-uuid "p-3"
                                                  :skill-state {:light-shield {:ticks 120
                                                                               :last-touch-tick 0
                                                                               :last-absorb-tick 0
                                                                               :overload-floor 10.0}
                                                               :toggle {:light-shield {:active true}}}}})
                    ctx-skill/assoc-skill-state! (fn [& args]
                                                   (swap! assoc-calls* conj args)
                                                   nil)
                    toggle/is-toggle-active? (fn [_ _] true)
                    skill-effects/skill-exp (fn [& _] 0.0)
                    skill-effects/perform-resource! (fn [& _] {:success? true})
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp-calls* conj [player-id skill-id amount])
                                                   nil)
                    skill-config/tunable-int (fn [_skill-id field-id]
                                               (case field-id
                                                 :combat.absorb-interval-ticks 18
                                                 :effect.slowness-duration-ticks 100
                                                 :effect.slowness-amplifier 1
                                                 1))
                    skill-config/lerp-double (fn [_skill-id field-id _exp]
                                               (case field-id
                                                 :combat.absorb-damage 6.0
                                                 :combat.touch-damage 3.0
                                                 :cost.absorb.cp 5.0
                                                 :cost.absorb.overload 0.5
                                                 0.0))
                    skill-config/tunable-double (fn [_skill-id field-id]
                                                  (case field-id
                                                    :progression.exp-attacked 0.001
                                                    :combat.front-cone-degrees 60.0
                                                    :combat.touch-radius 3.0
                                                    0.0))
                    motion-effects/player-position (fn [_]
                                                         {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                    motion-effects/entity-position (fn [_world-id _entity-id]
                                                     {:x 1.0 :y 64.0 :z 0.0})
                    raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                    world-effects/available? (constantly true)
                    world-effects/find-entities-in-radius (fn [& _]
                                                             [{:uuid "enemy-1"
                                                               :x 1.0 :y 64.0 :z 0.0
                                                               :living? true}])]
        (is (= [4.0 {:absorbed 6.0}]
               (reduce-damage! "p-3" "enemy-1" 10.0 :magic)))
        (is (= [["p-3" :light-shield 0.001]] @exp-calls*))
        (is (seq @assoc-calls*))))))

(deftest tick-timeout-deactivates-toggle-test
  (testing "tick exceeding max-active-ticks removes toggle and applies deactivation effects"
    (let [remove-calls* (atom 0)
          cooldown-calls* (atom [])
          potion-calls* (atom 0)
          ctx-data {:player-uuid "p-3"
                    :skill-state {:light-shield {:ticks 5
                                                 :last-touch-tick 0
                                                 :overload-floor 10.0}
                                  :toggle {:light-shield {:active true}}}}]
      (with-light-shield-env
        (fn []
          (with-redefs [ctx/get-context (fn ([_ctx-id] ctx-data) ([_ _ctx-id] ctx-data))
                        ctx-skill/assoc-skill-state! (fn [& _] nil)
                        ctx-skill/update-skill-state-root! (fn [_ f] (f {:light-shield {:ticks 5}}) nil)
                        toggle/is-toggle-active? (fn [_ _] true)
                        toggle/remove-toggle! (fn [& _] (swap! remove-calls* inc))
                        skill-effects/skill-exp (fn [& _] 0.0)
                        motion-effects/player-position (fn [_]
                                                             {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                        raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                        skill-config/lerp-int (fn [_skill-id field-id _exp]
                                                (case field-id
                                                  :timing.max-active-ticks 5
                                                  1))
                        skill-config/tunable-int (fn [_skill-id field-id]
                                                   (case field-id
                                                     :effect.slowness-duration-ticks 100
                                                     :effect.slowness-amplifier 1
                                                     1))
                        skill-config/tunable-double (fn [_skill-id field-id]
                                                     (case field-id
                                                       :progression.exp-tick 0.000001
                                                       :combat.touch-radius 3.0
                                                       :combat.front-cone-degrees 60.0
                                                       0.0))
                        skill-effects/enforce-overload-floor! (fn [& _] true)
                        skill-effects/add-skill-exp! (fn [& _] nil)
                        ctx/terminate-context! (fn [& _] nil)
                        skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                            true)
                        potion-effects/available? (constantly true)
                        potion-effects/apply-effect! (fn [& _]
                                                               (swap! potion-calls* inc)
                                                               nil)]
            (cb/apply-invoke ls/light-shield-tick! :player-id "p-3" :ctx-id "ctx-3" :cost-ok? true)
            (is (= 1 @remove-calls*))
            (is (= 1 @potion-calls*))
            (is (= 1 (count @cooldown-calls*))))))))

(deftest abort-applies-same-slowness-and-cooldown-as-deactivate-test
  ;; Matches original: keyup and keyabort both funnel through the same
  ;; s_onEnd (slowness + ticks-based cooldown) — abort is not a free pass.
  (testing "abort! applies the unified slowness+cooldown and clears skill state"
    (let [potion-calls* (atom [])
          remove-calls* (atom 0)
          update-calls* (atom [])
          cooldown-calls* (atom [])
          ctx-data {:player-uuid "p-4"
                    :skill-state {:light-shield {:ticks 10}}}]
      (with-light-shield-env
        (fn []
          (with-redefs [ctx/get-context (fn ([_ctx-id] ctx-data) ([_ _ctx-id] ctx-data))
                        ctx-skill/update-skill-state-root! (fn [& args]
                                                             (swap! update-calls* conj args)
                                                             nil)
                        toggle/remove-toggle! (fn [& _] (swap! remove-calls* inc))
                        skill-effects/skill-exp (fn [& _] 0.0)
                        skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                            true)
                        skill-config/tunable-int (fn [_skill-id field-id]
                                                   (case field-id
                                                     :effect.slowness-duration-ticks 100
                                                     :effect.slowness-amplifier 1
                                                     1))
                        potion-effects/available? (constantly true)
                        potion-effects/apply-effect! (fn [& args]
                                                               (swap! potion-calls* conj args)
                                                               nil)]
            (cb/apply-invoke ls/light-shield-abort! :player-id "p-4" :ctx-id "ctx-4")
            (is (= 1 @remove-calls*))
            (is (= 1 (count @potion-calls*)))
            ;; ticks=10, exp=0.0 -> toggle-cooldown-ticks = 10*(2-0.0) = 20
            (is (= [["p-4" :light-shield 20]] @cooldown-calls*))
            (is (seq @update-calls*)))))))))

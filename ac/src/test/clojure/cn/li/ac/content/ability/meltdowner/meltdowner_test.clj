(ns cn.li.ac.content.ability.meltdowner.meltdowner-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.content.ability.meltdowner.meltdowner :as meltdowner]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.beam :as beam]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- context-mocks []
  (let [{:keys [messages* send!]} (fx-mocks/capture-ctx-fx-messages!)
        terminated* (atom [])]
    {:messages* messages*
     :terminated* terminated*
     :fx-send! send!
     :terminate! (fn [ctx-id terminate-fn]
                   (swap! terminated* conj [ctx-id terminate-fn])
                   nil)}))

(deftest meltdowner-up-insufficient-charge-ends-context-test
  (let [{:keys [messages* terminated* fx-send! terminate!]} (context-mocks)
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        beam-params* (atom nil)]
    (with-redefs [fx/send! fx-send!
                  ctx/terminate-context! terminate!
                  ctx-skill/get-context (fn [& _] {:skill-state {:hold-ticks 10}})
                  skill-effects/skill-exp (fn [_ _] 0.3)
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp-calls* conj args))
                  skill-effects/set-main-cooldown! (fn [& args] (swap! cooldown-calls* conj args))
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 20
                                               :charge.max-ticks 40
                                               :charge.max-tolerant-ticks 100
                                               0))]
      (cb/apply-invoke #'cn.li.ac.content.ability.meltdowner.meltdowner/meltdowner-on-up! :player-id "p1" :ctx-id "ctx-1")
      (is (= [{:ctx-id "ctx-1" :topic :meltdowner/fx-end :payload {:performed? false}}
              {:ctx-id "ctx-1" :topic :meltdowner/fx-end :payload {:performed? false}}]
             @messages*) "fanned out to owner + nearby")
      (is (= [["ctx-1" nil]] @terminated*))
      (is (empty? @exp-calls*))
      (is (empty? @cooldown-calls*)))))

(deftest meltdowner-up-success-applies-rewards-and-terminates-test
  (let [{:keys [messages* terminated* fx-send! terminate!]} (context-mocks)
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        beam-params* (atom nil)]
    (with-redefs [fx/send! fx-send!
                  ctx/terminate-context! terminate!
                  ctx-skill/get-context (fn [& _] {:skill-state {:hold-ticks 30}})
                  skill-effects/skill-exp (fn [_ _] 0.6)
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp-calls* conj args))
                  skill-effects/set-main-cooldown! (fn [& args] (swap! cooldown-calls* conj args))
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 20
                                               :charge.max-ticks 40
                                               :charge.max-tolerant-ticks 100
                                               0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :progression.exp-use 0.002
                                                  :cooldown.base-multiplier 20.0
                                                  :beam.query-radius 30.0
                                                  :beam.step 0.9
                                                  :beam.max-distance 50.0
                                                  :beam.visual-distance 45.0
                                                  0.0))
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :charge.time-rate 1.1
                                               :cooldown.ticks 12.0
                                               :combat.damage 30.0
                                               :beam.radius 2.5
                                               :beam.block-energy 500.0
                                               0.0))
                  raycast/available? (constantly true)
                  raycast/player-look-vector (constantly {:dx 0.0 :dy 0.0 :dz 1.0})
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (constantly {:x 0.0 :y 62.38 :z 0.0})
                  beam/execute-beam! (fn [_ params]
                                       (reset! beam-params* params)
                                       {:beam-result {:performed? true :reflection-hit? false}})
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})]
      (cb/apply-invoke #'cn.li.ac.content.ability.meltdowner.meltdowner/meltdowner-on-up! :player-id "p1" :ctx-id "ctx-2")
      (is (= [["p1" :meltdowner 0.0022]] @exp-calls*))
      (is (= [["p1" :meltdowner 264]] @cooldown-calls*))
      (is (= {:x 0.0 :y 62.38 :z 0.0} (:trace-pos @beam-params*)))
      (is (= [{:ctx-id "ctx-2" :topic :meltdowner/fx-end :payload {:performed? true}}
              {:ctx-id "ctx-2" :topic :meltdowner/fx-end :payload {:performed? true}}]
             @messages*) "fanned out to owner + nearby")
      (is (= [["ctx-2" nil]] @terminated*)))))

(deftest meltdowner-tick-over-tolerant-aborts-test
  (let [{:keys [messages* terminated* fx-send! terminate!]} (context-mocks)
        overload-calls* (atom [])]
    (with-redefs [fx/send! fx-send!
                  ctx/terminate-context! terminate!
                  ctx-skill/get-context (fn [& _] {:skill-state {:hold-ticks 100 :overload-floor 123.0}})
                  ctx-skill/assoc-skill-state! (fn [& _] nil)
                  skill-effects/enforce-overload-floor! (fn [player-id floor]
                                                          (swap! overload-calls* conj [player-id floor]))
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 20
                                               :charge.max-ticks 40
                                               :charge.max-tolerant-ticks 100
                                               0))]
      (cb/apply-invoke #'cn.li.ac.content.ability.meltdowner.meltdowner/meltdowner-on-tick! :player-id "p1" :ctx-id "ctx-3")
      (is (= [["p1" 123.0]] @overload-calls*))
      ;; :caster-* rides along so the client can ring the charge motes around
      ;; the caster; unresolvable here (no bound session), which is fine.
      (is (= [{:ctx-id "ctx-3" :topic :meltdowner/fx-update
               :payload {:ticks 101 :charge-ratio 1.0 :player-id "p1"
                         :caster-x nil :caster-y nil :caster-z nil}}
              {:ctx-id "ctx-3" :topic :meltdowner/fx-update
               :payload {:ticks 101 :charge-ratio 1.0 :player-id "p1"
                         :caster-x nil :caster-y nil :caster-z nil}}
              {:ctx-id "ctx-3" :topic :meltdowner/fx-end :payload {:performed? false}}
              {:ctx-id "ctx-3" :topic :meltdowner/fx-end :payload {:performed? false}}]
             @messages*) "charge update + fanned-out end")
      (is (= [["ctx-3" nil]] @terminated*)))))

(deftest reflection-shot-supports-delta-look-vector-test
  (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        ray-input* (atom nil)
        damage-calls* (atom [])]
    (with-redefs [geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                  geom/world-id-of (fn [_] "w")
                  geom/vnorm (fn [v] v)
                  geom/v* (fn [v dist] {:x (* (:x v) dist) :y (* (:y v) dist) :z (* (:z v) dist)})
                  geom/v+ (fn [a b] {:x (+ (:x a) (:x b)) :y (+ (:y a) (:y b)) :z (+ (:z a) (:z b))})
                  fx/send! send!
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :reflection.shot-distance 10.0
                                                  :reflection.damage-multiplier 0.5
                                                  0.0))
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :reflection.base-damage 40.0
                                               0.0))
                  raycast/available? (constantly true)
                  raycast/player-look-vector (constantly {:dx 0.0 :dy 0.0 :dz 1.0})
                  raycast/raycast-entities (fn [world-id sx sy sz dx dy dz max-distance]
                                              (reset! ray-input* {:world-id world-id
                                                                  :start [sx sy sz]
                                                                  :dir [dx dy dz]
                                                                  :max-distance max-distance})
                                              {:hit-type :entity :uuid "target-1"})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id entity-id damage source-type]
                                                        (swap! damage-calls* conj [world-id entity-id damage source-type])
                                                        true)]
      (is (true?
           (#'cn.li.ac.content.ability.meltdowner.meltdowner/perform-reflection-shot!
            "ctx-r" "reflector-p" 0.0)))
      (is (= [0.0 0.0 1.0] (:dir @ray-input*)))
      (is (= [["w" "target-1" 20.0 :magic]] @damage-calls*))
      (is (some #(= :meltdowner/fx-reflect (nth % 1)) @calls*)))))

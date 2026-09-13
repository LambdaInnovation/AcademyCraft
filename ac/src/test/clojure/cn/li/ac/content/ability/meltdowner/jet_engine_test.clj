(ns cn.li.ac.content.ability.meltdowner.jet-engine-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.content.ability.meltdowner.jet-engine :as jet]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- context-mocks [initial]
  (let [ctx* (atom initial)
        terminated* (atom [])
        {:keys [messages* send!]} (fx-mocks/capture-ctx-fx-messages!)]
    {:ctx* ctx*
     :terminated* terminated*
     :messages* messages*
     :get-context (fn [_] @ctx*)
     :update-skill-state-root! (fn [_ f & args]
                                 (swap! ctx* (fn [ctx-data]
                                               (let [current (or (:skill-state ctx-data) {})
                                                     next-state (if (and (= f identity) (= 1 (count args)))
                                                                  (first args)
                                                                  (apply f current args))]
                                                 (assoc ctx-data :skill-state next-state)))))
     :terminate-context! (fn [ctx-id terminate-fn]
                           (swap! terminated* conj [ctx-id terminate-fn])
                           nil)
     :send! send!}))

(deftest jet-engine-down-enters-marking-phase-test
  (let [{:keys [ctx* get-context update-skill-state-root! send!]} (context-mocks {})]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly false)]
      (cb/apply-invoke jet/jet-engine-down! :ctx-id "ctx-1" :player-id "p1" :cost-ok? true)
      (is (= :marking (get-in @ctx* [:skill-state :phase])))
      (is (= 0 (get-in @ctx* [:skill-state :hold-ticks]))))))

(deftest jet-engine-up-success-enters-triggering-and-settles-test
  (let [{:keys [ctx* get-context update-skill-state-root! terminate-context! send! terminated*]} (context-mocks {:skill-state {:phase :marking
                                                                                                            :target-pos {:x 8.0 :y 65.0 :z 8.0}}})
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        resource-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :cost.release.cp 60.0
                                               :cost.release.overload 170.0
                                               10.0))
                  skill-config/lerp-int (fn [_ _ _] 60)
                  skill-config/tunable-double (fn [_ _] 0.004)
                  skill-effects/perform-resource! (fn [player-id overload cp simulate?]
                                                    (swap! resource-calls* conj
                                                           [player-id overload cp simulate?])
                                                    {:success? true})
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp-calls* conj args))
                  skill-effects/set-main-cooldown! (fn [& args] (swap! cooldown-calls* conj args))
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 1.0})
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/teleport-player! (fn [& _] true)
                  motion-effects/reset-fall-damage! (fn [& _] true)
                  motion-effects/player-position (fn [_] {:world-id "w" :x 1.0 :y 64.0 :z 1.0})]
      (cb/apply-invoke jet/jet-engine-up! :player-id "p1" :ctx-id "ctx-1")
      (is (= :triggering (get-in @ctx* [:skill-state :phase])))
      (is (= [["p1" 170.0 60.0 false]] @resource-calls*))
      (is (seq @exp-calls*))
      (is (seq @cooldown-calls*))
      (is (empty? @terminated*)))))

(deftest jet-engine-up-failure-terminates-without-cooldown-test
  (let [{:keys [ctx* get-context update-skill-state-root! terminate-context! send! terminated*]} (context-mocks {:skill-state {:phase :marking
                                                                                                            :target-pos {:x 8.0 :y 65.0 :z 8.0}}})
        cooldown-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.0)
                  skill-config/lerp-double (fn [_ _ _] 10.0)
                  skill-config/lerp-int (fn [_ _ _] 60)
                  skill-config/tunable-double (fn [_ _] 0.004)
                  skill-effects/perform-resource! (fn [& _] {:success? false})
                  skill-effects/set-main-cooldown! (fn [& args] (swap! cooldown-calls* conj args))
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 1.0})]
      (cb/apply-invoke jet/jet-engine-up! :player-id "p1" :ctx-id "ctx-1")
      (is (seq @terminated*))
      (is (empty? @cooldown-calls*))
      (is (= :marking (get-in @ctx* [:skill-state :phase]))))))

(deftest jet-engine-triggering-repeats-hit-and-dismount-each-tick-test
  (let [{:keys [ctx* get-context update-skill-state-root! terminate-context! send!]} (context-mocks {:skill-state {:phase :triggering
                                                                                                            :start-pos {:x 0.0 :y 64.0 :z 0.0}
                                                                                                            :target-pos {:x 4.0 :y 64.0 :z 0.0}
                                                                                                            :last-pos {:x 0.0 :y 64.0 :z 0.0}
                                                                                                            :velocity {:x 0.5 :y 0.0 :z 0.0}
                                                                                                            :world-id "w"
                                                                                                            :trigger-ticks 0
                                                                                                            :hit-uuids #{}}})
        damage-calls* (atom [])
        velocity-calls* (atom [])
        dismount-calls* (atom 0)
        marks* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.0)
                  md-damage/mark-target! (fn [player-id target-id fx-context]
                                           (swap! marks* conj [player-id target-id fx-context])
                                           true)
                  motion-effects/teleportation-available? (constantly false)
                  motion-effects/teleport-player! (fn [& _] true)
                  motion-effects/reset-fall-damage! (fn [& _] true)
                  motion-effects/player-motion-available? (constantly true)
                  motion-effects/dismount-riding! (fn [& _]
                                                    (swap! dismount-calls* inc)
                                                    true)
                  motion-effects/player-velocity (fn [& _] {:x 0.0 :y 0.0 :z 0.0})
                  motion-effects/set-player-velocity! (fn [& args]
                                                        (swap! velocity-calls* conj [(nth args 1) (nth args 2) (nth args 3)])
                                                        true)
                  raycast/available? (constantly true)
                  raycast/player-position (fn [& _] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/raycast-entities (fn [& _] {:uuid "target-1"})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& args]
                                                        (swap! damage-calls* conj (nth args 1))
                                                        true)]
      (cb/apply-invoke jet/jet-engine-tick! :player-id "p1" :ctx-id "ctx-1" :hold-ticks 1)
      (cb/apply-invoke jet/jet-engine-tick! :player-id "p1" :ctx-id "ctx-1" :hold-ticks 2)
      (is (= ["target-1" "target-1"] @damage-calls*))
      (is (= 2 @dismount-calls*))
      (is (= [[0.5 0.0 0.0] [0.5 0.0 0.0]] @velocity-calls*)
          "dash driven by constant velocity (no teleport)")
      (is (= [["p1" "target-1" {:ctx-id "ctx-1"
                                  :target-pos {:x nil :y nil :z nil}}]
              ["p1" "target-1" {:ctx-id "ctx-1"
                                  :target-pos {:x nil :y nil :z nil}}]]
             @marks*)))))

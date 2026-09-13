(ns cn.li.ac.content.ability.teleporter.tp-skill-helper-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]
            [cn.li.ac.content.ability.teleporter.passive-hooks :as passive-hooks]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as h]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.ability.effects.motion :as motion-effects]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(deftest skill-exp-test
  (testing "missing player state yields 0.0"
    (is (= 0.0 (skill-effects/skill-exp "no-one" :any))))
  (testing "reads exp from ability data"
    (store/reset-store!)
    (let [ad (-> (ad/new-ability-data)
                 (ad/learn-skill :foo)
                 (ad/set-skill-exp :foo 0.42))]
      (store/set-player-state! ps-fix/test-session-id "p1" {:ability-data ad})
      (is (= 0.42 (skill-effects/skill-exp "p1" :foo)))
      (store/reset-store!))))

(deftest player-look-and-position-nil-without-bindings-test
  (is (nil? (h/player-look-vec "p")))
  (is (nil? (h/player-position "p"))))

(deftest player-look-vec-delegates-test
  (with-redefs [raycast/available? (constantly true)
                raycast/player-look-vector (fn [player-id]
                                                  (when (= "p1" player-id)
                                                    {:x 0.0 :y 1.0 :z 0.0}))]
    (is (= {:x 0.0 :y 1.0 :z 0.0} (h/player-look-vec "p1")))))

(deftest teleport-to-success-and-fall-reset-test
  (let [tp-calls (atom [])
        reset-calls (atom [])]
    (with-redefs [motion-effects/teleportation-available? (constantly true)
                  motion-effects/teleport-player! (fn [pid wid x y z]
                                                    (swap! tp-calls conj [pid wid x y z])
                                                    true)
                  motion-effects/reset-fall-damage! (fn [pid]
                                                      (swap! reset-calls conj pid)
                                                      true)]
      (is (true? (h/teleport-to! "u1" "minecraft:overworld" 1.0 2.0 3.0))))
    (is (= [["u1" "minecraft:overworld" 1.0 2.0 3.0]] @tp-calls))
    (is (= ["u1"] @reset-calls))))

(deftest teleport-to-failure-skips-fall-reset-test
  (let [tp-called? (atom false)
        reset-called? (atom false)]
    (with-redefs [motion-effects/teleportation-available? (constantly true)
                  motion-effects/teleport-player! (fn [& _]
                                                    (reset! tp-called? true)
                                                    false)
                  motion-effects/reset-fall-damage! (fn [& _]
                                                      (reset! reset-called? true)
                                                      true)]
      (is (false? (h/teleport-to! "u1" "w" 0 0 0))))
    (is (true? @tp-called?))
    (is (false? @reset-called?))))

(defn- with-raycast-from-player [result f]
  (with-redefs [raycast/available? (constantly true)
                raycast/player-position (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                raycast/player-look-vector (fn [_] {:dx 0.0 :dy 0.0 :dz 1.0})
                raycast/raycast-combined-from-player (fn [_ _ _] result)]
    (f)))

(deftest raycast-entity-filters-self-and-nil-miss-test
  (let [hit {:hit-type :entity :entity-uuid "other" :x 1.0 :y 2.0 :z 3.0}]
    (is (nil? (with-raycast-from-player nil #(h/raycast-entity "p" 8.0))))
    (is (nil? (with-raycast-from-player hit #(h/raycast-entity "other" 8.0))))
    (is (= (assoc hit :hit-entity true :entity-x 1.0 :entity-y 2.0 :entity-z 3.0)
           (with-raycast-from-player hit #(h/raycast-entity "self-id" 8.0))))))

(deftest deal-magic-damage-plain-arity-test
  (is (nil? (h/deal-magic-damage! "world" "e1" 5.5)))
  (let [last-args (atom nil)]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [w u dmg st]
                                                       (reset! last-args [w u dmg st])
                                                       true)]
      (is (true? (h/deal-magic-damage! "world" "e1" 5.5)))
      (is (= ["world" "e1" 5.5 :magic] @last-args)))))

(defn- lerp-double-for-levels [level0 level1 level2]
  (fn [_ field-id _]
    (case field-id
      :critical.level0-probability level0
      :critical.level1-probability level1
      :critical.level2-probability level2
      0.0)))

(defn- with-crit-config [level0 level1 level2 f]
  (let [lerp (lerp-double-for-levels level0 level1 level2)
        exp-double (fn [_ field-id]
                     (case field-id
                       :progression.exp-per-crit-level 0.005
                       :progression.exp-critical 0.0001
                       0.0))]
    (with-redefs [skill-config/lerp-double lerp
                  skill-config/tunable-double-list (fn [_ _] [1.3 1.6 2.6])
                  skill-config/tunable-double exp-double]
      (f))))

;; Upstream TPSkillHelper fires the crit event and passive progression BEFORE it
;; attempts ctx.attack, so armor / PvP rules / invulnerability rejecting the hit
;; must not retract the crit. crit-applied? therefore keys off :critical? alone —
;; it used to also require :applied?, which is what this pair asserted before.
(deftest crit-applied-ignores-whether-the-damage-landed-test
  (is (true? (h/crit-applied? {:critical? true :applied? true})))
  (is (true? (h/crit-applied? {:critical? true :applied? false})))
  (is (false? (h/crit-applied? {:critical? false :applied? true})))
  (is (false? (h/crit-applied? nil))))

(deftest deal-magic-damage-crit-branch-test
  (let [last-damage (atom nil)
        exp-calls (atom [])
        events (atom [])
        feedback-calls (atom [])
        attacker "att"
        attacker-ad (-> (ad/new-ability-data)
                        (ad/learn-skill :dim-folding-theorem)
                        (ad/learn-skill :space-fluct)
                        (ad/set-skill-exp :dim-folding-theorem 1.0)
                        (ad/set-skill-exp :space-fluct 1.0))]
    (store/reset-store!)
    (try
      (store/set-player-state! ps-fix/test-session-id attacker {:ability-data attacker-ad})
      (with-crit-config 1.0 0.0 0.0
        (fn []
          (with-redefs [entity-damage/available? (constantly true)
                        entity-damage/apply-direct-damage! (fn [_ _ dmg _ _]
                                                              (reset! last-damage dmg)
                                                              true)
                        skill-effects/add-skill-exp! (fn [pid sid amount]
                                                       (swap! exp-calls conj [pid sid amount])
                                                       nil)
                        passive-hooks/send-chat-message! (fn [pid message args translate?]
                                                               (swap! feedback-calls conj [pid message args translate?])
                                                               true)
                        ach-dispatcher/trigger-custom-event! (fn [pid event-id]
                                                               (swap! events conj [pid event-id])
                                                               nil)]
            (let [result (h/deal-magic-damage! attacker "w" "victim" 10.0)]
              (is (= true (:critical? result)))
              (is (= 0 (:crit-level result)))
              (is (= 1.3 (double (:crit-rate result))))
              (is (= 10.0 (double (:damage-before result))))
              (is (= 13.0 (double (:damage-after result))))
              (is (= true (:applied? result)))
              (is (= "ability.teleporter.critical_hit" (:message-key result)))
              (is (= ["x1.3"] (:message-args result)))
              (is (= ["teleporter.critical_attack"] (:events result)))))))
      (is (= 13.0 (double @last-damage)))
      (is (= [["att" :dim-folding-theorem 0.005]
              ["att" :space-fluct 1.0E-4]]
             @exp-calls))
      (is (= [["att" "ability.teleporter.critical_hit" ["x1.3"] true]]
             @feedback-calls))
      (is (= [["att" "teleporter.critical_attack"]]
             @events))
      (finally
        (store/reset-store!)))))

(deftest deal-magic-damage-non-crit-branch-test
  (let [last-damage (atom nil)
        attacker "att"
        attacker-ad (-> (ad/new-ability-data)
                        (ad/learn-skill :dim-folding-theorem)
                        (ad/learn-skill :space-fluct)
                        (ad/set-skill-exp :dim-folding-theorem 1.0)
                        (ad/set-skill-exp :space-fluct 1.0))]
    (store/reset-store!)
    (try
      (store/set-player-state! ps-fix/test-session-id attacker {:ability-data attacker-ad})
      (with-crit-config 0.0 0.0 0.0
        (fn []
          (with-redefs [entity-damage/available? (constantly true)
                        entity-damage/apply-direct-damage! (fn [_ _ dmg _ _]
                                                              (reset! last-damage dmg)
                                                              true)
                        skill-effects/add-skill-exp! (fn [& _] (is false "no exp on non-crit"))
                        ach-dispatcher/trigger-custom-event! (fn [& _] (is false "no achievement event on non-crit"))]
            (let [result (h/deal-magic-damage! attacker "w" "victim" 10.0)]
              (is (= false (:critical? result)))
              (is (nil? (:crit-level result)))
              (is (= 1.0 (double (:crit-rate result))))
              (is (= 10.0 (double (:damage-before result))))
              (is (= 10.0 (double (:damage-after result))))
              (is (= true (:applied? result)))
              (is (= [] (:events result)))))))
      (is (= 10.0 (double @last-damage)))
      (finally
        (store/reset-store!)))))

(deftest deal-magic-damage-level2-crit-branch-test
  (let [last-damage (atom nil)
        exp-calls (atom [])
        events (atom [])
        attacker "att"
        attacker-ad (-> (ad/new-ability-data)
                        (ad/learn-skill :dim-folding-theorem)
                        (ad/learn-skill :space-fluct)
                        (ad/set-skill-exp :dim-folding-theorem 1.0)
                        (ad/set-skill-exp :space-fluct 1.0))]
    (store/reset-store!)
    (try
      (store/set-player-state! ps-fix/test-session-id attacker {:ability-data attacker-ad})
      (with-crit-config 0.0 0.0 1.0
        (fn []
          (with-redefs [entity-damage/available? (constantly true)
                        entity-damage/apply-direct-damage! (fn [_ _ dmg _ _]
                                                              (reset! last-damage dmg)
                                                              true)
                        skill-effects/add-skill-exp! (fn [pid sid amount]
                                                       (swap! exp-calls conj [pid sid amount])
                                                       nil)
                        passive-hooks/send-chat-message! (fn [& _] true)
                        ach-dispatcher/trigger-custom-event! (fn [pid event-id]
                                                               (swap! events conj [pid event-id])
                                                               nil)]
            (let [result (h/deal-magic-damage! attacker "w" "victim" 10.0)]
              (is (= true (:critical? result)))
              (is (= 2 (:crit-level result)))
              (is (= 2.6 (double (:crit-rate result))))
              (is (= ["teleporter.critical_attack" "teleporter.mastery"]
                     (:events result)))))))
      (is (= 26.0 (double @last-damage)))
      (is (= [["att" :dim-folding-theorem 0.015]
              ["att" :space-fluct 1.0E-4]]
             @exp-calls))
      (is (= [["att" "teleporter.critical_attack"]
              ["att" "teleporter.mastery"]]
             @events))
      (finally
        (store/reset-store!)))))

(deftest deal-magic-damage-unlearned-passives-never-crit-test
  (let [last-damage (atom nil)
        attacker "att"
        attacker-ad (ad/new-ability-data)]
    (store/reset-store!)
    (try
      (store/set-player-state! ps-fix/test-session-id attacker {:ability-data attacker-ad})
      (with-crit-config 1.0 1.0 1.0
        (fn []
          (with-redefs [entity-damage/available? (constantly true)
                        entity-damage/apply-direct-damage! (fn [_ _ dmg _ _]
                                                              (reset! last-damage dmg)
                                                              true)
                        skill-effects/add-skill-exp! (fn [& _] (is false "no exp when passives unlearned"))
                        passive-hooks/send-chat-message! (fn [& _] (is false "no feedback when passives unlearned"))
                        ach-dispatcher/trigger-custom-event! (fn [& _] (is false "no events when passives unlearned"))]
            (let [result (h/deal-magic-damage! attacker "w" "victim" 10.0)]
              (is (= false (:critical? result)))
              (is (nil? (:crit-level result)))
              (is (= 1.0 (double (:crit-rate result))))))))
      (is (= 10.0 (double @last-damage)))
      (finally
        (store/reset-store!)))))

;; Counterpart to crit-applied-ignores-whether-the-damage-landed-test, at the
;; deal-magic-damage! level: a crit that rolls but whose damage is rejected
;; still pays out exp / chat feedback / achievements, matching upstream's
;; ordering. This used to assert the opposite (no side effects when !applied?).
(deftest deal-magic-damage-critical-side-effects-survive-rejected-damage-test
  (let [exp-calls (atom [])
        feedback-calls (atom [])
        events (atom [])
        attacker "att"
        attacker-ad (-> (ad/new-ability-data)
                        (ad/learn-skill :dim-folding-theorem)
                        (ad/learn-skill :space-fluct)
                        (ad/set-skill-exp :dim-folding-theorem 1.0)
                        (ad/set-skill-exp :space-fluct 1.0))]
    (store/reset-store!)
    (try
      (store/set-player-state! ps-fix/test-session-id attacker {:ability-data attacker-ad})
      (with-crit-config 1.0 0.0 0.0
        (fn []
          (with-redefs [entity-damage/available? (constantly true)
                        entity-damage/apply-direct-damage! (fn [& _] false)
                        skill-effects/add-skill-exp! (fn [pid sid amount]
                                                       (swap! exp-calls conj [pid sid amount])
                                                       nil)
                        passive-hooks/send-chat-message! (fn [pid message args translate?]
                                                             (swap! feedback-calls conj [pid message args translate?])
                                                             true)
                        ach-dispatcher/trigger-custom-event! (fn [pid event-id]
                                                               (swap! events conj [pid event-id])
                                                               nil)]
            (let [result (h/deal-magic-damage! attacker "w" "victim" 10.0)]
              (is (= true (:critical? result)))
              (is (= false (:applied? result)))))))
      (is (= [["att" :dim-folding-theorem 0.005] ["att" :space-fluct 1.0E-4]] @exp-calls))
      (is (seq @feedback-calls) "crit chat feedback still fires")
      (finally
        (store/reset-store!)))))

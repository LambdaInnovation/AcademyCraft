(ns cn.li.ac.content.ability.vecmanip.blood-retrograde-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.vecmanip.blood-retrograde :as br]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- mock-cfg-int [field]
  (case field
    :charge.max-ticks 30
    0))

(defn- mock-cfg-double [field]
  (case field
    :targeting.distance 2.0
    :charge.fx-ratio-ticks 20.0
    :progression.exp-hit 0.002
    0.0))

(defn- mock-cfg-lerp [field _exp]
  (case field
    :cost.release.cp 280.0
    :cost.release.overload 55.0
    :combat.damage 30.0
    ;; cooldown reads the double curve and truncates
    :cooldown.ticks 90.0
    0.0))

(defn- mock-cfg-lerp-int [field _exp]
  (case field
    :cooldown.ticks 90
    0))

(defn- make-context-mocks [initial-ctx]
  (let [ctx-state (atom initial-ctx)
        terminate-calls (atom [])]
    {:ctx-state ctx-state
     :terminate-calls terminate-calls
     :get-context (fn [_] @ctx-state)
     :update-skill-state-root! (fn [_ctx-id f & args]
                                 (swap! ctx-state update :skill-state
                                        (fn [ss]
                                          (let [current (or ss {})]
                                            (if (and (= f identity) (= 1 (count args)))
                                              (first args)
                                              (apply f current args))))))
     :terminate-context! (fn [ctx-id _]
                           (swap! terminate-calls conj ctx-id))}))

;; The declarative `:cost {:tick … :up …}` map these two used to poke at went
;; away in "fix: align blood retrograde with AcademyCraft": the release cost is
;; now charged imperatively inside try-perform!, and the spec just declares no
;; per-tick drain. Same two invariants, asserted against the current shape.

(deftest release-cost-is-not-drained-per-tick-test
  (testing "no per-tick CP/overload drain — the release cost is a one-shot charge"
    (is (= 0.0 (:cp-consume-speed br/blood-retrograde)))
    (is (= 0.0 (:overload-consume-speed br/blood-retrograde)))))

(deftest release-cost-only-charged-on-hit-test
  (testing "up-stage release cost only applies when the release raycast hits"
    (let [resource-calls* (atom [])]
      (with-redefs [br/cfg-lerp mock-cfg-lerp
                    br/cfg-double mock-cfg-double
                    br/skill-exp (fn [_] 0.0)
                    geom/world-id-of (fn [_] "w")
                    fx/send! (fn [& _] nil)
                    entity-damage/available? (constantly false)
                    skill-effects/set-main-cooldown! (fn [& _] nil)
                    skill-effects/add-skill-exp! (fn [& _] nil)
                    skill-effects/perform-resource!
                    (fn [player-id overload cp _]
                      (swap! resource-calls* conj [player-id overload cp])
                      {:success? true})]
        (is (nil? (#'br/try-perform! "p1" "ctx-1" nil)))
        (is (empty? @resource-calls*) "no target — nothing charged")
        (is (true? (#'br/try-perform! "p1" "ctx-1" {:entity-id "e-1"})))
        (is (= [["p1" 55.0 280.0]] @resource-calls*) "charged once, at the release rates")))))

(deftest auto-release-at-max-tick-applies-side-effects-test
  (let [{:keys [ctx-state get-context update-skill-state-root! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:ticks 29
                                           :executed? false
                                           :ended? false}})
        damage-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])
        perform-calls* (atom [])
        end-calls* (atom [])]
    (with-redefs [br/cfg-int mock-cfg-int
            br/cfg-double mock-cfg-double
            br/cfg-lerp mock-cfg-lerp
            br/cfg-lerp-int mock-cfg-lerp-int
            br/skill-exp (fn [_] 0.0)
          br/get-player-look (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  geom/world-id-of (fn [_] "w")
                  raycast/available? (constantly true)
                  raycast/player-position (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  entity-damage/available? (constantly true)
                  raycast/raycast-from-player (fn [& _]
                                                {:entity-id "target-1"
                                                 :x 1.0 :y 2.0 :z 3.0})
                  raycast/raycast-blocks (fn [& _]
                                           {:x 1.0 :y 2.0 :z 3.0 :face :up})
                  fx/send! (fn [ctx-id entry _evt payload]
                            (case (:topic entry)
                              :blood-retrograde/fx-perform
                              (swap! perform-calls* conj [ctx-id (:topic entry) (:mode entry) payload])
                              :blood-retrograde/fx-end
                              (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload])
                              nil))
                  skill-effects/perform-resource! (fn [& _] {:success? true})
                  skill-effects/scale-damage (fn [_skill damage] damage)
                  entity-damage/apply-direct-damage! (fn [world-id target-id damage kind & _opts]
                                                      (swap! damage-calls* conj [world-id target-id damage kind]))
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                     (swap! cooldown-calls* conj [player-id skill-id ticks]))
                  skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                 (swap! exp-calls* conj [player-id skill-id amount]))]
      (cb/apply-invoke br/blood-retrograde-on-key-tick :player-id "p1" :ctx-id "ctx-1" :cost-ok? true))
    (is (= [["w" "target-1" 30.0 :skill]] @damage-calls*))
    (is (= [["p1" :blood-retrograde 90]] @cooldown-calls*))
    (is (= [["p1" :blood-retrograde 0.002]] @exp-calls*))
    (is (= 2 (count @perform-calls*)) "fx-perform fanned out to owner + nearby")
    (is (= 1 (count @end-calls*)))
    (is (= ["ctx-1"] @terminate-calls))
    (is (true? (get-in @ctx-state [:skill-state :executed?])))
    (is (true? (get-in @ctx-state [:skill-state :ended?])))
    (is (true? (get-in @ctx-state [:skill-state :performed?])))))

(deftest manual-release-without-hit-only-ends-context-test
  (let [{:keys [ctx-state get-context update-skill-state-root! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:ticks 12
                                           :executed? false
                                           :ended? false}})
        damage-calls* (atom [])
        end-calls* (atom [])]
    (with-redefs [br/cfg-double mock-cfg-double
            br/cfg-lerp mock-cfg-lerp
            br/cfg-lerp-int mock-cfg-lerp-int
            br/skill-exp (fn [_] 0.0)
          br/get-player-look (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  geom/world-id-of (fn [_] "w")
                  raycast/available? (constantly true)
                  entity-damage/available? (constantly true)
                  raycast/raycast-from-player (fn [& _] nil)
                  fx/send! (fn [ctx-id entry _evt payload]
                            (when (= :blood-retrograde/fx-end (:topic entry))
                              (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))
                            nil)
                  entity-damage/apply-direct-damage! (fn [& _]
                                                      (swap! damage-calls* conj :damage))]
        (cb/apply-invoke br/blood-retrograde-on-key-up :player-id "p1" :ctx-id "ctx-2" :cost-ok? true))
    (is (empty? @damage-calls*))
    (is (= [["ctx-2" :blood-retrograde/fx-end :end {:performed? false}]] @end-calls*))
    (is (= ["ctx-2"] @terminate-calls))
    (is (false? (get-in @ctx-state [:skill-state :performed?] false)))
    (is (true? (get-in @ctx-state [:skill-state :ended?])))))


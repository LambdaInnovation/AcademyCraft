(ns cn.li.ac.content.ability.meltdowner.ray-barrage-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.meltdowner.ray-barrage :as rb]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(defn- stub-lerp-double [_skill-id field-id _exp]
  (case field-id
    :combat.damage.plain 25.0
    :combat.damage.scattered 10.0
    :cost.down.cp 450.0
    :cost.down.overload 300.0
    :cooldown.ticks 100.0
    0.0))

(defn- stub-lerp-int [_skill-id field-id _exp]
  (case field-id
    :cooldown.ticks 100
    0))

(defn- stub-tunable-double [_skill-id field-id]
  (case field-id
    :targeting.range 20.0
    :scatter.cone-angle-degrees 55.0
    :progression.exp-hit 0.005
    0.0))

(defn- reset-runtime-fixture [f]
  (rb/reset-ray-barrage-state-for-test!)
  (try
    (f)
    (finally
      (rb/reset-ray-barrage-state-for-test!))))

(use-fixtures :each reset-runtime-fixture)

(defn- capture-fx-topic! [fx*]
  (fn [_ entry _evt _payload]
    (swap! fx* conj (:topic entry))
    nil))

(deftest ray-barrage-plain-branch-hits-single-entity-test
  (let [damage-calls* (atom [])
        mark-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])
        fx* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  geom/world-id-of (fn [_] "w")
                  geom/body-pos (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  fx/send! (capture-fx-topic! fx*)
                  raycast/available? (constantly true)
                  raycast/raycast-combined (fn [& _]
                                              {:hit-type :entity
                                               :uuid "enemy-1"
                                               :type "entity.academy.enemy"
                                               :x 0.0 :y 64.0 :z 10.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id target-id damage damage-type]
                                                        (swap! damage-calls* conj [world-id target-id damage damage-type])
                                                        true)
                  md-damage/mark-target! (fn [player-id target-id fx-context]
                                           (swap! mark-calls* conj [player-id target-id fx-context])
                                           true)
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                      (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                      true)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp-calls* conj args)
                                                 nil)]
      (cb/apply-invoke rb/ray-barrage-perform! :player-id "p1" :ctx-id "ctx-1"))

    (is (= [["w" "enemy-1" 25.0 :magic]] @damage-calls*))
    (is (= [["p1" "enemy-1" {:ctx-id "ctx-1"}]] @mark-calls*))
    (is (= [["p1" :ray-barrage 100]] @cooldown-calls*))
    (is (= [["p1" :ray-barrage 0.005]] @exp-calls*))
    ;; send-local-and-nearby! fans out to owner + nearby, so each topic
    ;; appears twice.
    (is (= 2 (count (filter #{:ray-barrage/fx-preray} @fx*))))
    (is (empty? (filter #{:ray-barrage/fx-barrage} @fx*)))))

(deftest ray-barrage-plain-branch-miss-still-grants-cooldown-and-exp-test
  ;; Matches original: ctx.setCooldown/addSkillExp run unconditionally on
  ;; every execution (except the silbarn-null edge case), not gated on hit.
  (let [damage-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  geom/world-id-of (fn [_] "w")
                  geom/body-pos (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  fx/send! (fn [& _] nil)
                  raycast/available? (constantly true)
                  raycast/raycast-combined (fn [& _] nil)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& args]
                                                        (swap! damage-calls* conj args)
                                                        true)
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                      (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                      true)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp-calls* conj args)
                                                 nil)]
      (cb/apply-invoke rb/ray-barrage-perform! :player-id "p1" :ctx-id "ctx-1"))

    (is (empty? @damage-calls*))
    (is (= [["p1" :ray-barrage 100]] @cooldown-calls*))
    (is (= [["p1" :ray-barrage 0.005]] @exp-calls*))))

(deftest ray-barrage-scatter-branch-triggers-silbarn-and-hits-cone-entities-test
  ;; Cone is centered on the player's own aim (+Z), not the silbarn's
  ;; position. enemy-front is within the yaw/pitch cone; enemy-behind is
  ;; not (negative Z, i.e. behind the player) and must be excluded.
  (let [trigger-calls* (atom [])
        damage-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])
        fx* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  geom/world-id-of (fn [_] "w")
                  geom/body-pos (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  fx/send! (capture-fx-topic! fx*)
                  raycast/available? (constantly true)
                  raycast/raycast-combined (fn [& _]
                                              {:hit-type :entity
                                               :uuid "silbarn-1"
                                               :type "entity.academy.silbarn"
                                               :is-hit false
                                               :x 0.0 :y 64.0 :z 15.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  world-effects/available? (constantly true)
                  world-effects/find-entities-in-aabb (fn [& _]
                                                        [{:uuid "enemy-front" :x 0.0 :y 64.0 :z 10.0 :eye-height 1.6}
                                                         {:uuid "enemy-behind" :x 0.0 :y 64.0 :z -10.0 :eye-height 1.6}
                                                         {:uuid "silbarn-1" :x 0.0 :y 64.0 :z 15.0 :eye-height 0.2}])
                  world-effects/trigger-behavior-hit! (fn [world-id uuid]
                                                       (swap! trigger-calls* conj [world-id uuid])
                                                       true)
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id target-id damage damage-type]
                                                        (swap! damage-calls* conj [world-id target-id damage damage-type])
                                                        true)
                  md-damage/mark-target! (fn [& _] true)
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                      (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                      true)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp-calls* conj args)
                                                 nil)]
      (cb/apply-invoke rb/ray-barrage-perform! :player-id "p1" :ctx-id "ctx-2"))

    (is (= [["w" "silbarn-1"]] @trigger-calls*))
    (is (= [["w" "enemy-front" 10.0 :magic]] @damage-calls*))
    (is (= [["p1" :ray-barrage 100]] @cooldown-calls*))
    (is (= [["p1" :ray-barrage 0.005]] @exp-calls*))
    (is (= 2 (count (filter #{:ray-barrage/fx-preray} @fx*))))
    (is (= 2 (count (filter #{:ray-barrage/fx-barrage} @fx*))))))

(deftest ray-barrage-already-hit-silbarn-falls-back-to-plain-branch-test
  (let [trigger-calls* (atom [])
        damage-calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  geom/world-id-of (fn [_] "w")
                  geom/body-pos (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  fx/send! (fn [& _] nil)
                  raycast/available? (constantly true)
                  raycast/raycast-combined (fn [& _]
                                              {:hit-type :entity
                                               :uuid "silbarn-1"
                                               :type "entity.academy.silbarn"
                                               :is-hit true
                                               :x 0.0 :y 64.0 :z 10.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  world-effects/trigger-behavior-hit! (fn [& args]
                                                       (swap! trigger-calls* conj args)
                                                       true)
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& args]
                                                        (swap! damage-calls* conj args)
                                                        true)
                  md-damage/mark-target! (fn [& _] true)
                  skill-effects/set-main-cooldown! (fn [& _] true)
                  skill-effects/add-skill-exp! (fn [& _] nil)]
      (cb/apply-invoke rb/ray-barrage-perform! :player-id "p1" :ctx-id "ctx-3"))

    (is (empty? @trigger-calls*))
    (is (= [["w" "silbarn-1" 25.0 :magic]] @damage-calls*))))

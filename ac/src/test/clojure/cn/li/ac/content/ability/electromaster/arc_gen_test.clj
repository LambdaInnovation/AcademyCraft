(ns cn.li.ac.content.ability.electromaster.arc-gen-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.content.ability.electromaster.arc-gen :as arc]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]))

(defn- stub-lerp
  [_skill-id field-id exp]
  (case field-id
    :combat.damage (+ 5.0 (* 4.0 exp))
    :targeting.range (+ 6.0 (* 9.0 exp))
    :effect.ignite-probability (* 0.6 exp)
    :cost.down.cp (+ 30.0 (* 40.0 exp))
    :cost.down.overload (- 18.0 (* 7.0 exp))
    :cooldown.ticks (- 15.0 (* 10.0 exp))
    0.0))

(defn- stub-double
  [_skill-id field-id]
  (case field-id
    :effect.fishing-exp-threshold 0.5
    :effect.fishing-probability 0.1
    :effect.creeper-charge-chance 0.3
    0.0))

(defn- stub-double-list
  [_skill-id field-id]
  (case field-id
    :progression.exp-entity [0.0048 0.0024]
    :progression.exp-block [0.0018 0.0009]
    [0.0 0.0]))

(deftest miss-follows-original-non-entity-branch-test
  (let [exp-calls* (atom [])
        cooldown-calls* (atom [])
        ignite-calls* (atom [])
        fx-calls* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector
                  (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-collidable-blocks-or-water
                  (fn [& _] nil)
                  raycast/raycast-from-player
                  (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos
                  (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  geom/body-pos
                  (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  block-manip/available? (constantly true)
                  block-manip/get-block
                  (fn [& _] "minecraft:air")
                  block-manip/set-block!
                  (fn [& args]
                    (swap! ignite-calls* conj args)
                    true)
                  skill-effects/add-skill-exp!
                  (fn [& args]
                    (swap! exp-calls* conj args)
                    nil)
                  skill-effects/skill-exp (fn [& _] 0.25)
                  skill-effects/set-main-cooldown!
                  (fn [& args]
                    (swap! cooldown-calls* conj args)
                    true)
                  skill-config/lerp-double stub-lerp
                  skill-config/tunable-double stub-double
                  skill-config/tunable-double-list stub-double-list
                  skill-config/probability
                  (fn [skill-id field-id]
                    (stub-double skill-id field-id))
                  fx/send!
                  (fn [ctx-id entry _evt payload]
                    (swap! fx-calls* conj
                           [ctx-id (:topic entry) (:to entry) payload])
                    nil)
                  clojure.core/rand (constantly 0.0)]
      (cb/apply-invoke
        arc/arc-gen-perform!
        :player-id "p1"
        :ctx-id "ctx-1"
        :player-ref {:id "player-obj"}
        :exp 0.5)
      (is (= [["p1" :arc-gen 0.00225]]
             @exp-calls*))
      (is (= [["w" 0 65 10 "minecraft:fire"]]
             @ignite-calls*))
      ;; Updated exp 0.25 gives 12.5 ticks; Java's int cast truncates to 12.
      (is (= [["p1" :arc-gen 12]]
             @cooldown-calls*))
      (is (= 2 (count @fx-calls*)))
      (is (= [:client :except-local]
             (mapv #(nth % 2) @fx-calls*)))
      (let [payload (nth (first @fx-calls*) 3)]
        (is (= :miss (:hit-type payload)))
        (is (= {:x 0.0 :y 64.0 :z 10.5}
               (:end payload)))
        (is (= {:x 0.0 :y 62.38 :z 0.0}
               (:sound-pos payload)))
        (is (= "p1" (:source-player-id payload)))))))

(deftest water-hit-spawns-fish-at-hit-vector-test
  (let [spawn-calls* (atom [])
        ignite-calls* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector
                  (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-collidable-blocks-or-water
                  (fn [& _]
                    {:x 1 :y 64 :z 6
                     :hit-x 1.25 :hit-y 64.5 :hit-z 6.75
                     :distance 5.0})
                  raycast/raycast-from-player (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos
                  (fn [_] {:x 1.0 :y 64.0 :z 1.0})
                  geom/body-pos
                  (fn [_] {:x 1.0 :y 62.38 :z 1.0})
                  block-manip/available? (constantly true)
                  block-manip/get-block
                  (fn [& _] "minecraft:water")
                  block-manip/set-block!
                  (fn [& args]
                    (swap! ignite-calls* conj args)
                    true)
                  pitem/stack-by-id
                  (fn [_ _]
                    {:item-id "minecraft:cooked_cod" :count 1})
                  server-bridge/server-bridge-available?
                  (constantly true)
                  server-bridge/spawn-item-stack-at!
                  (fn [& args]
                    (swap! spawn-calls* conj args)
                    true)
                  skill-effects/add-skill-exp!
                  (fn [& args]
                    (swap! exp-calls* conj args)
                    nil)
                  skill-effects/skill-exp (fn [& _] 0.75)
                  skill-effects/set-main-cooldown!
                  (fn [& args]
                    (swap! cooldown-calls* conj args)
                    true)
                  skill-config/lerp-double stub-lerp
                  skill-config/tunable-double stub-double
                  skill-config/tunable-double-list stub-double-list
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [& _] nil)
                  clojure.core/rand (constantly 0.0)]
      (cb/apply-invoke
        arc/arc-gen-perform!
        :player-id "p2"
        :ctx-id "ctx-2"
        :player-ref {:id "player-obj"}
        :exp 0.75)
      (is (= [[{:id "player-obj"}
               "w"
               1.25 64.5 6.75
               {:item-id "minecraft:cooked_cod" :count 1}]]
             @spawn-calls*))
      (is (empty? @ignite-calls*))
      (is (= ["p2" :arc-gen]
             (vec (take 2 (first @exp-calls*)))))
      (is (< (Math/abs
               (- 0.002475
                  (double (nth (first @exp-calls*) 2))))
             1.0e-12))
      (is (= [["p2" :arc-gen 7]]
             @cooldown-calls*)))))

(deftest entity-hit-uses-original-attack-pipeline-and-creeper-effect-test
  (let [calc-calls* (atom [])
        scale-calls* (atom [])
        damage-calls* (atom [])
        power-calls* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector
                  (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  ;; Entity wins the original's equal-distance tie.
                  raycast/raycast-collidable-blocks-or-water
                  (fn [& _]
                    {:x 0 :y 64 :z 5
                     :hit-x 0.0 :hit-y 64.0 :hit-z 5.0
                     :distance 5.0})
                  raycast/raycast-from-player
                  (fn [& _]
                    {:entity-id "creeper-1"
                     :x 0.0 :y 64.0 :z 5.0
                     :eye-height 1.7
                     :distance 5.0})
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos
                  (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  geom/body-pos
                  (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage!
                  (fn [& args]
                    (swap! damage-calls* conj args)
                    true)
                  ability-event/fire-calc-event!
                  (fn [& args]
                    (swap! calc-calls* conj args)
                    (inc (double (second args))))
                  skill-registry/get-skill
                  (fn [_] {:id :arc-gen :damage-scale 2.0})
                  skill-effects/scale-damage
                  (fn [spec raw]
                    (swap! scale-calls* conj [spec raw])
                    (* (:damage-scale spec) raw))
                  entity/entity-type-id-fn-available? (constantly true)
                  entity/get-type-id
                  (fn [_world-id _uuid] "minecraft:creeper")
                  motion/entity-motion-available? (constantly true)
                  motion/power-creeper!
                  (fn [& args]
                    (swap! power-calls* conj args)
                    true)
                  skill-effects/add-skill-exp!
                  (fn [& args]
                    (swap! exp-calls* conj args)
                    nil)
                  skill-effects/skill-exp (fn [& _] 1.0)
                  skill-effects/set-main-cooldown!
                  (fn [& args]
                    (swap! cooldown-calls* conj args)
                    true)
                  skill-config/lerp-double stub-lerp
                  skill-config/tunable-double stub-double
                  skill-config/tunable-double-list stub-double-list
                  skill-config/probability
                  (fn [skill-id field-id]
                    (stub-double skill-id field-id))
                  fx/send! (fn [& _] nil)
                  clojure.core/rand (constantly 0.0)]
      (cb/apply-invoke
        arc/arc-gen-perform!
        :player-id "p3"
        :ctx-id "ctx-3"
        :player-ref {:id "player-obj"}
        :exp 1.0)
      (is (= [[ability-event/CALC-SKILL-ATTACK
               9.0
               {:player-id "p3"
                :target-id "creeper-1"
                :skill-id :arc-gen}]]
             @calc-calls*))
      (is (= [[{:id :arc-gen :damage-scale 2.0} 10.0]]
             @scale-calls*))
      (is (= [["w" "creeper-1" 20.0 :skill
               {:attacker-uuid "p3" :skill-id :arc-gen}]]
             @damage-calls*))
      (is (= [["w" "creeper-1"]]
             @power-calls*))
      (is (= [["p3" :arc-gen 0.0072]]
             @exp-calls*))
      (is (= [["p3" :arc-gen 5]]
             @cooldown-calls*)))))

(deftest skill-declares-manual-truncating-cooldown-test
  (with-redefs [skill-config/lerp-double stub-lerp]
    (is (= {:mode :manual} (:cooldown arc/arc-gen)))
    (is (= 12
           ((:cooldown-ticks arc/arc-gen)
            "p" :arc-gen 0.25)))))

(ns cn.li.ac.content.ability.electromaster.thunder-bolt-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.electromaster.thunder-bolt :as thunder-bolt]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.potion :as potion-effects]))

(defn- stub-lerp-double [_skill-id field-id exp]
  (case field-id
    :combat.direct-damage (+ 10.0 (* 15.0 exp))
    :combat.aoe-damage (+ 6.0 (* 9.0 exp))
    :cost.down.cp (+ 280.0 (* 140.0 exp))
    :cost.down.overload (- 50.0 (* 23.0 exp))
    :cooldown.ticks (- 120.0 (* 70.0 exp))
    0.0))

(defn- stub-double [_skill-id field-id]
  (case field-id
    :targeting.range 20.0
    :combat.aoe-radius 8.0
    :effect.slowness-exp-threshold 0.2
    :effect.creeper-charge-chance 0.3
    :progression.exp-effective 0.005
    :progression.exp-ineffective 0.003
    0.0))

(defn- stub-int [_skill-id field-id]
  (case field-id
    :effect.slowness-duration-ticks 40
    :effect.slowness-aoe-retry-duration-ticks 20
    :effect.slowness-amplifier 3
    0))

(deftest miss-sends-fallback-fx-and-grants-ineffective-exp-test
  (let [fx* (atom [])
        exp* (atom [])
        cooldown* (atom [])
        damage* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _] nil)
                  world-effects/find-entities-in-radius (fn [& _] [])
                  entity-damage/apply-direct-damage! (fn [& args]
                                                       (swap! damage* conj args)
                                                       true)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp* conj args)
                                                 nil)
                  skill-effects/set-main-cooldown! (fn [& args]
                                                     (swap! cooldown* conj args)
                                                     nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [ctx-id entry _evt payload]
                             (swap! fx* conj [ctx-id (:topic entry) payload])
                             nil)]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform! :player-id "p1" :ctx-id "ctx-1" :exp 0.5)
      (is (empty? @damage*))
      (is (= 2 (count @fx*)) "fanned out to owner + nearby")
      (let [[_ _ payload] (first @fx*)]
        (is (= :miss (:hit-kind payload)))
        (is (= {:x 0.0 :y 64.0 :z 20.0} (:end payload)))
        (is (= "p1" (:source-player-id payload)))
        (is (= "w" (:world-id payload))))
      (is (= [["p1" :thunder-bolt 0.003]] @exp*))
      (is (= [["p1" :thunder-bolt 85]] @cooldown*)))))

(deftest entity-hit-applies-direct-and-aoe-without-double-hit-test
  (let [damage* (atom [])
        exp* (atom [])
        fx* (atom [])
        potion* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 66.0 :z 1.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _]
                                             {:hit-type :entity
                                              :uuid "mob-1"
                                              :x 10.0 :y 64.0 :z 10.0
                                              :hit-x 10.2 :hit-y 65.0 :hit-z 10.2
                                              :eye-height 1.8})
                  world-effects/find-entities-in-radius (fn [& _]
                                                          [{:uuid "mob-1" :x 10.0 :y 64.0 :z 10.0 :living? true}
                                                           {:uuid "mob-2" :x 10.5 :y 64.0 :z 9.5 :eye-height 1.6 :living? true}])
                  entity-damage/apply-direct-damage! (fn [world-id target-id damage source-type opts]
                                                       (swap! damage* conj [world-id target-id damage source-type opts])
                                                       true)
                  potion-effects/apply-effect! (fn [& args]
                                                        (swap! potion* conj args)
                                                        nil)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp* conj args)
                                                 nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-effects/scale-damage (fn [_spec value] (* 2.0 value))
                  skill-registry/get-skill (fn [_] {})
                  ability-event/fire-calc-event! (fn [event-type value extra]
                                                   (is (= ability-event/CALC-SKILL-ATTACK event-type))
                                                   (is (= :thunder-bolt (:skill-id extra)))
                                                   value)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [ctx-id entry _evt payload]
                             (swap! fx* conj [ctx-id (:topic entry) payload])
                             nil)
                  rand (fn [] 0.0)]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform! :player-id "p2" :ctx-id "ctx-2" :exp 0.6)
      (is (= 2 (count @damage*)))
      (is (= 1 (count (filter #(= "mob-1" (second %)) @damage*))))
      (is (= 1 (count (filter #(= "mob-2" (second %)) @damage*))))
      (is (< (Math/abs (- 38.0 (double (nth (first @damage*) 2)))) 1.0e-9))
      (is (< (Math/abs (- 22.8 (double (nth (second @damage*) 2)))) 1.0e-9))
      (is (every? #(= :skill (nth % 3)) @damage*))
      (is (every? #(= {:attacker-uuid "p2" :skill-id :thunder-bolt}
                      (nth % 4))
                  @damage*))
      (is (= [["mob-1" :slowness 40 3]
              ["mob-1" :slowness 20 3]]
             @potion*))
      (is (= ["p2" :thunder-bolt 0.005] (first @exp*)))
      (let [[_ _ payload] (first @fx*)]
        (is (= :entity (:hit-kind payload)))
        ;; c_spawnEffect spawns the three main arcs with mainArc.length =
        ;; RANGE and never calls setFromTo, so they run the full range along
        ;; the caster's look direction and pass through the target; only the
        ;; AOE arcs start at AttackData.point.
        (is (= {:x 1.0 :y 66.0 :z 21.0} (:end payload)))
        (is (= {:x 10.2 :y 65.0 :z 10.2} (:aoe-origin payload)))
        (is (= 1 (count (:aoe-points payload))))))))

(deftest block-hit-applies-aoe-and-effective-exp-test
  (let [damage* (atom [])
        exp* (atom [])
        potion* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 2.0 :y 64.0 :z 2.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _]
                                             {:hit-type :block
                                              :hit-x 8.0 :hit-y 65.0 :hit-z 8.0
                                              :x 8.0 :y 65.0 :z 8.0})
                  world-effects/find-entities-in-radius (fn [& _]
                                                          [{:uuid "mob-a" :x 8.0 :y 65.0 :z 8.0 :eye-height 1.2 :living? true}
                                                           {:uuid "mob-b" :x 9.0 :y 65.0 :z 8.0 :eye-height 1.4 :living? true}
                                                           {:uuid "item" :x 8.0 :y 65.0 :z 8.0 :living? false}
                                                           {:uuid "cube-corner" :x 15.0 :y 72.0 :z 8.0 :living? true}])
                  entity-damage/apply-direct-damage! (fn [_world-id target-id damage _source-type _opts]
                                                       (swap! damage* conj [target-id damage])
                                                       true)
                  potion-effects/apply-effect! (fn [& args]
                                                        (swap! potion* conj args)
                                                        nil)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp* conj args)
                                                 nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [& _] nil)]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform! :player-id "p3" :ctx-id "ctx-3" :exp 0.4)
      (is (= 2 (count @damage*)))
      (is (empty? @potion*))
      (is (= ["p3" :thunder-bolt 0.005] (first @exp*))))))

(deftest slowness-requires-exp-threshold-test
  (let [potion* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 66.0 :z 1.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _]
                                             {:hit-type :entity
                                              :uuid "mob-low"
                                              :x 10.0 :y 64.0 :z 10.0
                                              :eye-height 1.8})
                  world-effects/find-entities-in-radius (fn [& _] [])
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  potion-effects/apply-effect! (fn [& args]
                                                        (swap! potion* conj args)
                                                        nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [& _] nil)
                  rand (fn [] 0.0)]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform! :player-id "p4" :ctx-id "ctx-4" :exp 0.1)
      (is (empty? @potion*)))))

(deftest miss-still-applies-original-endpoint-aoe-test
  (let [damage* (atom [])
        exp* (atom [])
        fx* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  motion/entity-motion-available? (constantly false)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _] nil)
                  world-effects/find-entities-in-radius
                  (fn [& _]
                    [{:uuid "endpoint-mob" :x 0.0 :y 64.0 :z 19.0
                      :eye-height 1.5 :living? true}
                     {:uuid "endpoint-item" :x 0.0 :y 64.0 :z 20.0
                      :living? false}])
                  entity-damage/apply-direct-damage!
                  (fn [world-id target-id damage source-type opts]
                    (swap! damage* conj [world-id target-id damage source-type opts])
                    true)
                  skill-effects/scale-damage (fn [_spec value] value)
                  skill-registry/get-skill (fn [_] {})
                  ability-event/fire-calc-event! (fn [_event-type value _extra] value)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp* conj args))
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [_ctx-id _entry _evt payload]
                             (swap! fx* conj payload))]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform!
                       :player-id "p-miss-aoe" :ctx-id "ctx-miss-aoe" :exp 0.5)
      (is (= [["w" "endpoint-mob" 10.5 :skill
               {:attacker-uuid "p-miss-aoe" :skill-id :thunder-bolt}]]
             @damage*))
      (is (= [["p-miss-aoe" :thunder-bolt 0.005]] @exp*))
      (is (= :miss (:hit-kind (first @fx*))))
      (is (= {:x 0.0 :y 64.0 :z 20.0} (:aoe-origin (first @fx*))))
      (is (= [{:x 0.0 :y 65.5 :z 19.0}] (:aoe-points (first @fx*)))))))

(deftest aoe-victim-rerolls-short-slowness-on-direct-target-test
  (let [potion* (atom [])
        rolls* (atom [0.9 0.0])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  motion/entity-motion-available? (constantly false)
                  potion-effects/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined
                  (fn [& _]
                    {:hit-type :entity
                     :uuid "direct"
                     :x 0.0 :y 64.0 :z 10.0
                     :eye-height 1.5})
                  world-effects/find-entities-in-radius
                  (fn [& _]
                    [{:uuid "direct" :x 0.0 :y 64.0 :z 10.0 :living? true}
                     {:uuid "aoe" :x 1.0 :y 64.0 :z 10.0 :living? true}])
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  potion-effects/apply-effect! (fn [& args]
                                                 (swap! potion* conj args))
                  skill-effects/scale-damage (fn [_spec value] value)
                  skill-registry/get-skill (fn [_] {})
                  ability-event/fire-calc-event! (fn [_event-type value _extra] value)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 0.8)
                  fx/send! (fn [& _] nil)
                  rand (fn []
                         (let [value (first @rolls*)]
                           (swap! rolls* subvec 1)
                           value))]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform!
                       :player-id "p-retry" :ctx-id "ctx-retry" :exp 0.6)
      (is (= [["direct" :slowness 20 3]] @potion*))
      (is (empty? @rolls*)))))

(deftest direct-and-aoe-hits-roll-creeper-charge-independently-test
  ;; Matches original EMDamageHelper.attack: creeper-charging is a flat-chance
  ;; side effect tied to entities the skill actually damaged (direct hit and
  ;; each AOE victim), not a real lightning-bolt entity's own area effect.
  (let [charge-calls* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  world-effects/available? (constantly true)
                  entity-damage/available? (constantly true)
                  potion-effects/available? (constantly true)
                  motion/entity-motion-available? (constantly true)
                  entity/entity-type-id-fn-available? (constantly true)
                  entity/get-type-id (fn [_world-id entity-uuid]
                                       (when (#{"creeper-1" "creeper-2"} entity-uuid)
                                         "minecraft:creeper"))
                  motion/power-creeper! (fn [world-id entity-uuid]
                                          (swap! charge-calls* conj [world-id entity-uuid])
                                          true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 66.0 :z 1.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined (fn [& _]
                                             {:hit-type :entity
                                              :uuid "creeper-1"
                                              :x 10.0 :y 64.0 :z 10.0
                                              :eye-height 1.8})
                  world-effects/find-entities-in-radius (fn [& _]
                                                          [{:uuid "creeper-1" :x 10.0 :y 64.0 :z 10.0 :living? true}
                                                           {:uuid "creeper-2" :x 10.5 :y 64.0 :z 9.5 :eye-height 1.6 :living? true}])
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  potion-effects/apply-effect! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-double
                  skill-config/tunable-int stub-int
                  skill-config/probability (fn [& _] 1.0)
                  fx/send! (fn [& _] nil)
                  rand (fn [] 0.0)]
      (cb/apply-invoke thunder-bolt/thunder-bolt-perform! :player-id "p5" :ctx-id "ctx-5" :exp 0.6)
      (is (= 2 (count @charge-calls*)))
      (is (every? #(= "w" (first %)) @charge-calls*))
      (is (= #{"creeper-1" "creeper-2"} (set (map second @charge-calls*)))))))

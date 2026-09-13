(ns cn.li.ac.content.ability.meltdowner.electron-bomb-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.meltdowner.electron-bomb :as electron-bomb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity :as entity]))

(defn- stub-lerp-double [_skill-id field-id _exp]
  (case field-id
    :combat.damage 12.5
    :cost.down.cp 210.0
    :cost.down.overload 100.0
    0.0))

(defn- stub-tunable-double [_skill-id field-id]
  (case field-id
    :progression.exp-hit 0.005
    :charge.improved-exp-threshold 0.8
    0.0))

(defn- stub-tunable-int [_skill-id field-id]
  (case field-id
    :charge.settle-ticks 20
    :charge.settle-ticks-improved 5
    0))

(defn- stub-lerp-int [_skill-id field-id _exp]
  (case field-id
    :cooldown.ticks 15
    0))

(deftest perform-schedules-delayed-settlement-and-spawn-fx-test
  (let [spawn-calls* (atom [])
        fx-calls* (atom [])
        scheduled* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.5)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  skill-config/tunable-int stub-tunable-int
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  entity/player-spawn-tracked-entity-by-id! (fn [& args]
                                                              (swap! spawn-calls* conj args)
                                                              "ball-uuid-1")
                  fx/send! (fn [ctx-id entry _evt payload]
                             (swap! fx-calls* conj [ctx-id (:topic entry) payload])
                             nil)
                  skill-effects/add-skill-exp! (fn [& args]
                                                 (swap! exp-calls* conj args)
                                                 nil)
                  skill-effects/set-main-cooldown! (fn [& args]
                                                     (swap! cooldown-calls* conj args)
                                                     nil)
                  delayed-projectiles/mdball-near-expire-delay (fn [life-ticks offset-ticks]
                                                                 (- life-ticks offset-ticks))
                  delayed-projectiles/schedule-electron-bomb-beam! (fn [task]
                                                                     (swap! scheduled* conj task)
                                                                     nil)]
      ;; exp defaults to 0.0 in cb/apply-invoke (below the 0.8 improved-exp
      ;; threshold), so this exercises the un-improved 20-tick settle branch.
      ;; The ball spawns with a life override equal to the settle life, so the
      ;; ball dies exactly when the ray fires (original death callback).
      (cb/apply-invoke electron-bomb/electron-bomb-perform! :player-id "p1" :ctx-id "ctx-1" :player-ref {:id "player-obj"})
      (is (= [[{:id "player-obj"} "academy:entity_md_ball" 0.0 20]]
             @spawn-calls*))
      (is (= [["ctx-1"
               :electron-bomb/fx-spawn
               {:x 1.0 :y 64.0 :z 2.0
                :dx 0.0 :dy 0.0 :dz 1.0}]
              ["ctx-1"
               :electron-bomb/fx-spawn
               {:x 1.0 :y 64.0 :z 2.0
                :dx 0.0 :dy 0.0 :dz 1.0}]]
             @fx-calls*) "fanned out to owner + nearby")
      ;; Exp is granted immediately/unconditionally at cast time, matching
      ;; original's ctx.addSkillExp(.005f) firing in s_Execute.
      (is (= [["p1" :electron-bomb 0.005]] @exp-calls*))
      (is (= [["p1" :electron-bomb 15]] @cooldown-calls*))
      (is (= [{:player-id "p1"
               :ctx-id "ctx-1"
               :damage 12.5
               :ball-uuid "ball-uuid-1"
               :delay-ticks 18}]
             @scheduled*)))))

(deftest perform-uses-improved-settle-ticks-above-exp-threshold-test
  (let [scheduled* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.9)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/lerp-int stub-lerp-int
                  skill-config/tunable-double stub-tunable-double
                  skill-config/tunable-int stub-tunable-int
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  entity/player-spawn-tracked-entity-by-id! (constantly "ball-uuid-2")
                  fx/send! (constantly nil)
                  skill-effects/add-skill-exp! (constantly nil)
                  skill-effects/set-main-cooldown! (constantly nil)
                  delayed-projectiles/mdball-near-expire-delay (fn [life-ticks offset-ticks]
                                                                 (- life-ticks offset-ticks))
                  delayed-projectiles/schedule-electron-bomb-beam! (fn [task]
                                                                     (swap! scheduled* conj task)
                                                                     nil)]
      (cb/apply-invoke electron-bomb/electron-bomb-perform! :player-id "p1" :ctx-id "ctx-1" :exp 0.9 :player-ref {:id "player-obj"})
      (is (= [3] (mapv :delay-ticks @scheduled*))))))

(deftest perform-without-look-vector-is-noop-test
  (let [spawn-calls* (atom [])
        fx-calls* (atom [])
        scheduled* (atom [])]
    (with-redefs [skill-effects/skill-exp (fn [& _] 0.5)
                  skill-config/lerp-double stub-lerp-double
                  skill-config/tunable-double stub-tunable-double
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [& _] nil)
                  entity/player-spawn-tracked-entity-by-id! (fn [& args]
                                                              (swap! spawn-calls* conj args)
                                                              "ball-uuid-1")
                  fx/send! (fn [ctx-id entry _evt payload]
                             (swap! fx-calls* conj [ctx-id (:topic entry) payload])
                             nil)
                  delayed-projectiles/mdball-near-expire-delay (fn [] 15)
                  delayed-projectiles/schedule-electron-bomb-beam! (fn [task]
                                                                     (swap! scheduled* conj task)
                                                                     nil)]
      (cb/apply-invoke electron-bomb/electron-bomb-perform! :player-id "p1" :ctx-id "ctx-1" :player-ref {:id "player-obj"})
      (is (empty? @spawn-calls*))
      (is (empty? @fx-calls*))
      (is (empty? @scheduled*)))))

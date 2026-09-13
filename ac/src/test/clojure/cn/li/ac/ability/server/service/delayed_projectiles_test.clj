(ns cn.li.ac.ability.server.service.delayed-projectiles-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.player-runtime-commands :as prt-cmd]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.service.delayed-projectiles :as dp]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(defn- with-fresh-delayed-projectile-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (ps-fix/seed-player-state! "p1" {})
      (ps-fix/seed-player-state! "p2" {})
      (try
        (f)
        (finally
          (dp/reset-pending-tasks-for-test!)
          (store/reset-store!))))))

(use-fixtures :each with-fresh-delayed-projectile-runtime)

(deftest mdball-near-expire-delay-test
  (is (= 15 (dp/mdball-near-expire-delay)))
  (is (= 1 (dp/mdball-near-expire-delay 1)))
  (is (= 5 (dp/mdball-near-expire-delay 10))))

(deftest electron-bomb-settlement-hit-path-test
  (testing "eye/look-dir are re-fetched fresh at settle time, not read from the scheduled task"
    (let [calls (atom [])]
      (with-redefs [raycast/available? (constantly true)
                    entity-damage/available? (constantly true)
                    world-effects/available? (constantly true)
                    geom/world-id-of (fn [_] "w")
                    geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                    geom/body-pos (fn [_] {:x 1.0 :y 62.38 :z 2.0})
                    raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                    world-effects/find-entities-in-aabb
                    (fn [& _] [{:uuid "ball-9" :x 1.5 :y 63.0 :z 2.2}])
                    ;; Two traces now: getDest's from the eye (misses here, so
                    ;; the destination falls back to the FEET plus look * 15),
                    ;; then the damage trace from the ball.
                    raycast/raycast-combined-excluding
                    (fn [_world sx sy sz & _]
                      (when-not (= [sx sy sz] [1.0 64.0 2.0])
                        {:hit-type :entity
                         :uuid "target-1"
                         :x 4.0
                         :y 65.0
                         :z 6.0
                         :hit-x 4.0
                         :hit-y 65.0
                         :hit-z 6.0
                         :distance 9.0}))
                    entity-damage/apply-direct-damage! (fn [& args]
                                                        (swap! calls conj [:damage (vec args)])
                                                        true)
                    md-damage/mark-target! (fn [& args]
                                             (swap! calls conj [:mark (vec args)])
                                             true)
                    skill-effects/add-skill-exp! (fn [& args]
                                                   (swap! calls conj [:exp (vec args)])
                                                   true)
                    ctx-mgr/push-channel-to-player! (fn [& args]
                                                     (swap! calls conj [:fx (vec args)])
                                                     true)
                    ctx-mgr/push-channel-to-nearby-players! (fn [& args]
                                                             (swap! calls conj [:fx-nearby (vec args)])
                                                             true)]
        (dp/schedule-electron-bomb-beam!
         {:player-id "p1"
          :ctx-id "ctx-1"
          :damage 12.5
          :ball-uuid "ball-9"
          :delay-ticks 1})
        (dp/tick-player! "p1")
        ;; Exp is no longer granted here — original grants it unconditionally
        ;; at cast time, so the skill's perform! now owns that call.
        (is (= [[:damage ["w" "target-1" 12.5 :magic]]
            [:mark ["p1" "target-1" {:ctx-id "ctx-1"
                    :target-pos {:x 4.0 :y 65.0 :z 6.0}}]]
                [:fx ["p1"
                      "ctx-1"
                      :electron-bomb/fx-beam
                      {:mode :perform
                       :start {:x 1.5 :y 63.0 :z 2.2}
                       :end {:x 1.0 :y 62.38 :z 17.0}
                       :hit-distance 15.0
                       :performed? true
                     :target-uuid "target-1"}]]
                  [:fx-nearby ["p1"
                       "ctx-1"
                       :electron-bomb/fx-beam
                       {:mode :perform
                        :start {:x 1.5 :y 63.0 :z 2.2}
                        :end {:x 1.0 :y 62.38 :z 17.0}
                        :hit-distance 15.0
                        :performed? true
                        :target-uuid "target-1"}]]]
               @calls))
        (is (empty? (dp/pending-tasks-snapshot "p1")))))))

(deftest electron-bomb-dest-clamps-raise-into-target-box-test
  (testing "a low hit on the target clamps the 0.6*eyeHeight raise inside the entity box so the ball ray cannot skim over the head"
    (let [calls (atom [])]
      (with-redefs [raycast/available? (constantly true)
                    entity-damage/available? (constantly true)
                    world-effects/available? (constantly true)
                    geom/world-id-of (fn [_] "w")
                    geom/eye-pos (fn [_] {:x 1.0 :y 65.62 :z 2.0})
                    geom/body-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                    raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                    world-effects/find-entities-in-aabb
                    (fn [& _] [{:uuid "ball-9" :x 1.5 :y 65.0 :z 2.2}])
                    ;; Eye trace (from the eye) hits the pig LOW on its body;
                    ;; the ball trace hits it too.
                    raycast/raycast-combined-excluding
                    (fn [_world sx sy sz & _]
                      (if (= [sx sy sz] [1.0 65.62 2.0])
                        {:hit-type :entity
                         :uuid "pig-1"
                         :x 3.0 :y 64.0 :z 5.0
                         :hit-x 3.0 :hit-y 64.7 :hit-z 5.0
                         :eye-height 0.765 :height 0.9}
                        {:hit-type :entity
                         :uuid "pig-1"
                         :x 3.0 :y 64.0 :z 5.0
                         :hit-x 3.4 :hit-y 64.9 :hit-z 5.0
                         :eye-height 0.765 :height 0.9}))
                    entity-damage/apply-direct-damage! (fn [& args]
                                                         (swap! calls conj [:damage (vec args)])
                                                         true)
                    md-damage/mark-target! (fn [& _] true)
                    ctx-mgr/push-channel-to-player! (fn [_ _ _ payload]
                                                      (swap! calls conj [:fx (:end payload)])
                                                      true)
                    ctx-mgr/push-channel-to-nearby-players! (fn [& _] true)]
        (dp/schedule-electron-bomb-beam!
         {:player-id "p1" :ctx-id "ctx-1" :damage 12.0 :ball-uuid "ball-9" :delay-ticks 1})
        (dp/tick-player! "p1")
        ;; dest.y = clamp(64.7 + 0.6*0.765, 64.7, 64 + 0.9*0.9) = 64.81 — inside
        ;; the pig's box (64..64.9), not the uncapped raise 65.159 which would
        ;; put the ball ray above the target's head.
        (is (= 64.81 (get-in @calls [1 1 :y])))
        (is (= [:damage ["w" "pig-1" 12.0 :magic]] (first @calls)))))))

(deftest electron-bomb-settlement-rays-from-ball-position-test
  (testing "the settle ray originates from the tracked MdBall's orbit position, matching the original's callback (ray from ball.pos toward getDest(player))"
    (let [calls (atom [])
          raycast-args* (atom nil)]
      (with-redefs [raycast/available? (constantly true)
                    entity-damage/available? (constantly true)
                    world-effects/available? (constantly true)
                    geom/world-id-of (fn [_] "w")
                    geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                    geom/body-pos (fn [_] {:x 1.0 :y 62.38 :z 2.0})
                    raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                    world-effects/find-entities-in-aabb
                    (fn [& _] [{:uuid "ball-9" :x 1.5 :y 63.0 :z 2.2}])
                    raycast/raycast-combined-excluding
                    (fn [& args]
                      (when-not (= (vec (take 4 args)) ["w" 1.0 64.0 2.0])
                        (reset! raycast-args* args)
                        {:hit-type :entity :uuid "target-1"
                         :x 4.0 :y 65.0 :z 6.0
                         :hit-x 4.0 :hit-y 65.0 :hit-z 6.0}))
                    entity-damage/apply-direct-damage! (fn [& args]
                                                         (swap! calls conj [:damage (vec args)])
                                                         true)
                    md-damage/mark-target! (fn [& args]
                                             (swap! calls conj [:mark (vec args)])
                                             true)
                    ctx-mgr/push-channel-to-player! (fn [& args]
                                                      (swap! calls conj [:fx (vec args)])
                                                      true)
                    ctx-mgr/push-channel-to-nearby-players! (fn [& args]
                                                              (swap! calls conj [:fx-nearby (vec args)])
                                                              true)]
        (dp/schedule-electron-bomb-beam!
         {:player-id "p1"
          :ctx-id "ctx-1"
          :damage 12.5
          :ball-uuid "ball-9"
          :delay-ticks 1})
        (dp/tick-player! "p1")
        ;; Raycast starts at the ball's orbit position, not the player's eye.
        (is (= ["w" 1.5 63.0 2.2]
               (vec (take 4 @raycast-args*))))
        (is (= [[:fx ["p1"
                      "ctx-1"
                      :electron-bomb/fx-beam
                      {:mode :perform
                       :start {:x 1.5 :y 63.0 :z 2.2}
                       :end {:x 1.0 :y 62.38 :z 17.0}
                       :hit-distance 15.0
                       :performed? true
                       :target-uuid "target-1"}]]
                [:fx-nearby ["p1"
                             "ctx-1"
                             :electron-bomb/fx-beam
                             {:mode :perform
                              :start {:x 1.5 :y 63.0 :z 2.2}
                              :end {:x 1.0 :y 62.38 :z 17.0}
                              :hit-distance 15.0
                              :performed? true
                              :target-uuid "target-1"}]]]
               (filter #(#{:fx :fx-nearby} (first %)) @calls)))))))

(deftest electron-bomb-settlement-without-look-vector-is-noop-test
  (let [calls (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 1.0 :y 64.0 :z 2.0})
                  raycast/player-look-vector (fn [_] nil)
                  raycast/raycast-entities (fn [& _]
                                             (swap! calls conj :raycast)
                                             nil)
                  entity-damage/apply-direct-damage! (fn [& _]
                                                      (swap! calls conj :damage)
                                                      true)
                  md-damage/mark-target! (fn [& _]
                                           (swap! calls conj :mark)
                                           true)
                  skill-effects/add-skill-exp! (fn [& _]
                                                 (swap! calls conj :exp)
                                                 true)
                  ctx-mgr/push-channel-to-player! (fn [& _]
                                                   (swap! calls conj :fx)
                                                   true)]
      (dp/schedule-electron-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :damage 12.5
        :delay-ticks 1})
      (dp/tick-player! "p1")
      (is (empty? @calls))
      (is (empty? (dp/pending-tasks-snapshot "p1"))))))

(deftest scatter-bomb-settlement-order-and-cleanup-test
  (let [calls (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  entity-damage/available? (constantly true)
                  ;; The scatter ray traces blocks as well as entities
                  ;; (Raytrace.perform with EntitySelectors.everything),
                  ;; so the stub answers the combined trace and says
                  ;; what it hit.
                  raycast/raycast-combined-all (fn [& _] {:hit-type :entity :uuid "target-1"})
                  entity-damage/apply-direct-damage! (fn [world-id target-id damage source-type opts]
                                                        (swap! calls conj [:damage world-id target-id damage source-type opts])
                                                        true)
                  md-damage/mark-target! (fn [player-id target-id fx-context]
                                           (swap! calls conj [:mark player-id target-id fx-context])
                                           true)
                  ctx-mgr/push-channel-to-player! (fn [player-id ctx-id ch payload]
                                                   (swap! calls conj [:fx player-id ctx-id ch payload])
                                                   true)
                  ctx-mgr/push-channel-to-nearby-players! (fn [player-id ctx-id ch payload]
                                                           (swap! calls conj [:fx-nearby player-id ctx-id ch payload])
                                                           true)]
      (dp/schedule-scatter-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :world-id "w"
        :origin {:x 1.0 :y 64.0 :z 2.0}
        :dest {:x 1.0 :y 64.0 :z 17.0}
        :damage 7.0
        :delay-ticks 1})
      (dp/schedule-scatter-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :world-id "w"
        :origin {:x 1.0 :y 64.0 :z 2.0}
        :dest {:x 1.0 :y 64.0 :z 17.0}
        :damage 7.0
        :delay-ticks 2})

      (dp/tick-player! "p1")
  (is (= 4 (count @calls)))
      (is (= [:damage "w" "target-1" 7.0 :magic {:reset-invulnerable-time? true}] (first @calls)))
      (is (= [:mark "p1" "target-1" {:ctx-id "ctx-1"}] (second @calls)))
      (is (= 1 (count (dp/pending-tasks-snapshot "p1"))))

      (dp/tick-player! "p1")
  (is (= 8 (count @calls)))
      (is (empty? (dp/pending-tasks-snapshot "p1"))))))

(deftest pending-tasks-are-player-keyed-and-clearable-test
  (dp/schedule-task! "p1" 2 {:kind :unknown :payload 1})
  (dp/schedule-task! "p2" 2 {:kind :unknown :payload 2})
  (is (= 1 (count (dp/pending-tasks-snapshot "p1"))))
  (is (= 1 (count (dp/pending-tasks-snapshot "p2"))))
  (dp/clear-player-tasks! "p1")
  (is (empty? (dp/pending-tasks-snapshot "p1")))
  (is (= 1 (count (dp/pending-tasks-snapshot "p2")))))

(deftest delayed-projectile-tasks-are-per-player-state-test
  (dp/schedule-task! "p1" 2 {:kind :unknown :payload :a})
  (dp/schedule-task! "p2" 2 {:kind :unknown :payload :b})
  (is (= [{:kind :unknown :payload :a :ticks-left 2}]
         (dp/pending-tasks-snapshot "p1")))
  (is (= [{:kind :unknown :payload :b :ticks-left 2}]
         (dp/pending-tasks-snapshot "p2")))
  (dp/clear-player-tasks! "p1")
  (is (empty? (dp/pending-tasks-snapshot "p1")))
  (is (= 1 (count (dp/pending-tasks-snapshot "p2")))))

(deftest scatter-bomb-settlement-uses-task-origin-and-dest-test
  (let [run-op-inputs* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  raycast/raycast-combined-all (fn [world-id sx sy sz dx dy dz max-dist]
                                                 (swap! run-op-inputs* conj {:origin {:x sx :y sy :z sz}
                                                                             :dir {:x dx :y dy :z dz}
                                                                             :max-dist max-dist})
                                                 nil)
                  ctx-mgr/push-channel-to-player! (fn [& _] true)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] true)
                  md-damage/mark-target! (fn [& _] true)]
      (dp/schedule-scatter-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :world-id "w"
        :origin {:x 1.0 :y 64.0 :z 2.0}
        :dest {:x 11.0 :y 64.0 :z 2.0}
        :damage 7.0
        :delay-ticks 1})
      (dp/schedule-scatter-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :world-id "w"
        :origin {:x 1.0 :y 64.0 :z 2.0}
        :dest {:x 1.0 :y 74.0 :z 2.0}
        :damage 7.0
        :delay-ticks 1})
      (dp/tick-player! "p1")
      (is (= [{:origin {:x 1.0 :y 64.0 :z 2.0} :dir {:x 1.0 :y 0.0 :z 0.0} :max-dist 10.0}
              {:origin {:x 1.0 :y 64.0 :z 2.0} :dir {:x 0.0 :y 1.0 :z 0.0} :max-dist 10.0}]
             @run-op-inputs*)))))

(deftest clear-player-tasks-prevents-later-execution-test
  (let [run-count* (atom 0)]
    (with-redefs [raycast/available? (constantly true)
                  raycast/raycast-combined-all (fn [& _]
                                                 (swap! run-count* inc)
                                                 nil)
                  ctx-mgr/push-channel-to-player! (fn [& _] true)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] true)
                  md-damage/mark-target! (fn [& _] true)]
      (dp/schedule-scatter-bomb-beam!
       {:player-id "p1"
        :ctx-id "ctx-1"
        :world-id "w"
        :origin {:x 1.0 :y 64.0 :z 2.0}
        :dest {:x 1.0 :y 64.0 :z 17.0}
        :damage 7.0
        :delay-ticks 1})
      (dp/clear-player-tasks! "p1")
      (dp/tick-player! "p1")
      (is (= 0 @run-count*))
      (is (empty? (dp/pending-tasks-snapshot "p1"))))))

(deftest tick-player-with-no-pending-tasks-dispatches-no-command-test
  (testing "idle players (no pending tasks) never reach command-runtime"
    (is (empty? (dp/pending-tasks-snapshot "p1")))
    (let [calls (atom 0)
          original prt-cmd/run-for-player!]
      (with-redefs [prt-cmd/run-for-player!
                    (fn
                      ([uuid cmd] (swap! calls inc) (original uuid cmd {}))
                      ([uuid cmd opts] (swap! calls inc) (original uuid cmd opts)))]
        (dp/tick-player! "p1"))
      (is (zero? @calls)))))


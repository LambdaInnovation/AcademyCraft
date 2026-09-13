(ns cn.li.ac.content.ability.meltdowner.electron-missile-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.electron-missile-fx :as em-fx]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (em-fx/reset-electron-missile-fx-for-test!)
          (f)
          (finally
            (em-fx/reset-electron-missile-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

;; ElectronMissile has no arc-beam impl — it owns its enqueue/tick fns and
;; registers them through fx-spec, so tests drive those directly.
(defn- enqueue!
  [enqueue-state! ctx-id channel payload]
  (level-effects/update-effect-state! :electron-missile
    (fn [store] (enqueue-state! store ctx-id channel [:ctx ctx-id] payload)))
  nil)

(defn- tick!
  [tick-state!]
  (level-effects/update-effect-state! :electron-missile
    (fn [store] (tick-state! store)))
  nil)

(deftest init-registers-owner-aware-electron-missile-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (em-fx/init!)
      (is (= :electron-missile (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:electron-missile/fx-start
               :electron-missile/fx-update
               :electron-missile/fx-end
               :electron-missile/fx-fire}
             @registered-topics*)))))

(deftest fx-handler-routes-events-with-ctx-metadata-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (em-fx/init!)
      ((get @handlers* :electron-missile/fx-start) "ctx-em" :electron-missile/fx-start {:source-player-id "player-a"})
      ((get @handlers* :electron-missile/fx-update) "ctx-em" :electron-missile/fx-update {:ticks 12
                                                        :balls 3
                                                        :x 8.0 :y 64.0 :z -3.0
                                                        :source-player-id "player-a"})
      ((get @handlers* :electron-missile/fx-fire) "ctx-em" :electron-missile/fx-fire {:start {:x 0.0 :y 64.0 :z 0.0}
                                                      :end {:x 1.0 :y 65.5 :z 2.0}
                                                      :source-player-id "player-a"})
      ((get @handlers* :electron-missile/fx-end) "ctx-em" :electron-missile/fx-end {:source-player-id "player-a"})
      (is (= [[:electron-missile "ctx-em" :electron-missile/fx-start
               {:mode :start :source-player-id "player-a"}
               [:owner-key [:ctx "ctx-em"]]]
              [:electron-missile "ctx-em" :electron-missile/fx-update
               {:mode :update :ticks 12 :balls 3 :x 8.0 :y 64.0 :z -3.0
                :source-player-id "player-a"}
               [:owner-key [:ctx "ctx-em"]]]
              [:electron-missile "ctx-em" :electron-missile/fx-fire
               {:mode :fire
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 1.0 :y 65.5 :z 2.0}
                :source-player-id "player-a"}
               [:owner-key [:ctx "ctx-em"]]]
              [:electron-missile "ctx-em" :electron-missile/fx-end
               {:mode :end :source-player-id "player-a"}
               [:owner-key [:ctx "ctx-em"]]]]
             @enqueued*)))))

(deftest fire-adds-beam-and-end-clears-state-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/tick-state!)
        particles* (atom [])
        sounds* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "electron-missile-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                             (swap! particles* conj args)
                                                             nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                        (swap! sounds* conj args)
                                                        nil)]
      (enqueue! enqueue-state! "ctx-a" :electron-missile/fx-update
                {:mode :update
                 :ticks 8
                 :balls 2
                 :x 10.0 :y 64.0 :z -20.0
                 :source-player-id "player-a"})
      (is (= 2 (get-in (em-fx/electron-missile-fx-snapshot) [:charge-state [:ctx "ctx-a"] :balls])))
      (enqueue! enqueue-state! "ctx-a" :electron-missile/fx-fire
                {:mode :fire
                 :start {:x 0.0 :y 64.0 :z 0.0}
                 :end {:x 1.0 :y 65.5 :z 2.0}
                 :source-player-id "player-a"})
      (is (seq (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-a"]])))
      (dotimes [_ 14]
        (tick! tick-state!))
      (is (empty? (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-a"]])))
      (enqueue! enqueue-state! "ctx-a" :electron-missile/fx-end
                {:mode :end
                 :source-player-id "player-a"})
      (is (nil? (get-in (em-fx/electron-missile-fx-snapshot) [:charge-state [:ctx "ctx-a"]])))
      (is (seq @particles*))
      (is (seq @sounds*)))))

(deftest end-keeps-in-flight-beams-test
  ;; Upstream's rays are world entities with their own lives; MSG_TERMINATED
  ;; kills only the orbiting balls. The port's :end wiped :beams, cutting
  ;; every beam fired in the last 0.7s of the hold short at key-up.
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/enqueue-state!)]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "test-session" :player-uuid "test-player"})
                  client-sounds/queue-sound-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-keep" :electron-missile/fx-fire
                {:mode :fire
                 :start {:x 0.0 :y 64.0 :z 0.0}
                 :end {:x 1.0 :y 65.5 :z 2.0}
                 :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-keep" :electron-missile/fx-end
                {:mode :end
                 :source-player-id "player-a"})
      (is (seq (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-keep"]]))
          "in-flight beams must survive the release"))))

(deftest beam-ttl-matches-md-ray-small-life-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/tick-state!)
        build-plan (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/build-plan)
        particles* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "electron-missile-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particles* conj args)
                                                            nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-em" :electron-missile/fx-fire
                {:mode :fire
                 :start {:x 0.0 :y 64.0 :z 0.0}
                 :end {:x 1.0 :y 65.5 :z 2.0}
                 :source-player-id "player-a"})

      ;; EntityMdRaySmall lives 14 ticks, not the port's old 10.
      (is (= 14 (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-em"] 0 :ttl])))

      (tick! tick-state!)

      (is (= 13 (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-em"] 0 :ttl])))
      (is (some? (build-plan {:x 0.0 :y 64.0 :z 8.0} nil 0 nil)))
      (is (= 1 (count @particles*))
          "onUpdate spawns exactly one trail particle per tick per live ray")

      (dotimes [_ 13]
        (tick! tick-state!))

      (is (nil? (get-in (em-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-em"]])))
      (is (nil? (build-plan {:x 0.0 :y 64.0 :z 8.0} nil 0 nil))))))

(deftest charge-particles-orbit-the-caster-not-the-world-origin-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.electron-missile-fx/tick-state!)
        particles* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "electron-missile-fx-test"})
                  client-particles/queue-particle-effect! (fn [_owner cmd]
                                                            (swap! particles* conj cmd)
                                                            nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-em" :electron-missile/fx-update
                {:mode :update
                 :ticks 3 :balls 1
                 :x 100.0 :y 64.0 :z -50.0
                 :source-player-id "player-a"})
      (dotimes [_ 20]
        (tick! tick-state!))

      (is (<= 20 (count @particles*)) "1-2 particles per tick, as rangei(1, 3)")
      (is (<= (count @particles*) 40))
      ;; player.pos + (r*sin, getHeightFix 1.6 + h, r*cos), r in [0.5, 1),
      ;; h in [-1.2, 0) — they must ride the caster, and the old build put every
      ;; one of them at the world origin.
      (doseq [{:keys [x y z offset-y]} @particles*]
        (is (<= 99.0 (double x) 101.0))
        (is (<= -51.0 (double z) -49.0))
        (is (<= (+ 64.0 0.4) (double y) (+ 64.0 1.6)))
        (is (pos? (double offset-y)) "the original's motion drifts upwards")))))

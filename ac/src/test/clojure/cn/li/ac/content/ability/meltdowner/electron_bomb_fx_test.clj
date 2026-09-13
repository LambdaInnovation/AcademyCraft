(ns cn.li.ac.content.ability.meltdowner.electron-bomb-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.electron-bomb-fx :as electron-bomb-fx]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (electron-bomb-fx/reset-fx-for-test!)
          (f)
          (finally
            (electron-bomb-fx/reset-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(defn- event [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-electron-bomb-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (electron-bomb-fx/init!)
      (is (= :electron-bomb (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:electron-bomb/fx-spawn
               :electron-bomb/fx-beam
               :electron-bomb/fx-end}
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
      (electron-bomb-fx/init!)
      ((get @handlers* :electron-bomb/fx-spawn) "ctx-eb" :electron-bomb/fx-spawn {:x 1.0 :y 64.0 :z 2.0 :dx 0.0 :dy 0.0 :dz 1.0})
      ((get @handlers* :electron-bomb/fx-beam) "ctx-eb" :electron-bomb/fx-beam {:start {:x 1.0 :y 64.0 :z 2.0}
                                                   :end {:x 1.0 :y 64.0 :z 17.0}
                                                   :performed? true
                                                   :target-uuid "target-1"})
      ((get @handlers* :electron-bomb/fx-end) "ctx-eb" :electron-bomb/fx-end {})
      (is (= [[:electron-bomb "ctx-eb" :electron-bomb/fx-spawn
               {:mode :spawn
                :x 1.0 :y 64.0 :z 2.0
                :dx 0.0 :dy 0.0 :dz 1.0}
               [:owner-key [:ctx "ctx-eb"]]]
              [:electron-bomb "ctx-eb" :electron-bomb/fx-beam
               {:mode :beam
                :start {:x 1.0 :y 64.0 :z 2.0}
                :end {:x 1.0 :y 64.0 :z 17.0}
                :performed? true
                :target-uuid "target-1"}
               [:owner-key [:ctx "ctx-eb"]]]
              [:electron-bomb "ctx-eb" :electron-bomb/fx-end
               {:mode :end}
               [:owner-key [:ctx "ctx-eb"]]]]
             @enqueued*)))))

(deftest spawn-beam-and-tick-state-test
  (let [
        particles* (atom [])
        sounds* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "electron-bomb-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                             (swap! particles* conj args)
                                                             nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                        (swap! sounds* conj args)
                                                        nil)]
      (arc-beam/enqueue-for-test! :electron-bomb "ctx-a" :electron-bomb/fx-spawn
               {:mode :spawn :x 1.0 :y 64.0 :z 2.0 :dx 0.0 :dy 0.0 :dz 1.0})
      (is (some? (get-in (electron-bomb-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
      ;; The ball is the spawned EntityMdBall and renders itself; casting
      ;; contributes no level-plan geometry of its own. The port used to draw a
      ;; spinning line at the caster's EYE here, a second ball in the wrong
      ;; place.
      (is (nil? (arc-beam/effect-build-plan :electron-bomb {:x 0.0 :y 65.0 :z 0.0} nil 0)))
      (arc-beam/enqueue-for-test! :electron-bomb "ctx-a" :electron-bomb/fx-beam
               {:mode :beam
                :start {:x 1.0 :y 64.0 :z 2.0}
                :end {:x 1.0 :y 64.0 :z 17.0}
                :performed? true
                :target-uuid "target-1"})
      (let [snapshot (electron-bomb-fx/fx-snapshot)]
        (is (nil? (get-in snapshot [:effect-state [:ctx "ctx-a"]])))
        (is (seq (get-in snapshot [:beams [:ctx "ctx-a"]]))))
      (is (seq (:ops (arc-beam/effect-build-plan :electron-bomb {:x 0.0 :y 65.0 :z 0.0} nil 0))))
      ;; Original EntityMdRaySmall life is 14 ticks — beam must outlive the
      ;; old 8-tick window.
      (dotimes [_ 14]
        (level-effects/update-effect-state! :electron-bomb
          (fn [store] (arc-beam/effect-tick-state! :level :electron-bomb store))))
      (is (nil? (arc-beam/effect-build-plan :electron-bomb {:x 0.0 :y 65.0 :z 0.0} nil 0)))
      (is (seq @particles*))
      (is (seq @sounds*)))))

(deftest electron-bomb-beam-trail-and-ttl-test
  (let [particles* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "electron-bomb-fx-test"})
                  client-particles/queue-particle-effect! (fn [_owner cmd]
                                                            (swap! particles* conj cmd)
                                                            nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)]
      (arc-beam/enqueue-for-test! :electron-bomb "ctx-cadence" :electron-bomb/fx-spawn
               {:mode :spawn :x 1.0 :y 64.0 :z 2.0 :dx 0.0 :dy 0.0 :dz 1.0})

      ;; Casting emits no particles of its own — the ball entity is the visual.
      (dotimes [_ 20]
        (level-effects/update-effect-state! :electron-bomb
          (fn [store] (arc-beam/effect-tick-state! :level :electron-bomb store))))
      (is (empty? @particles*))

      (arc-beam/enqueue-for-test! :electron-bomb "ctx-cadence" :electron-bomb/fx-beam
               {:mode :beam
                :start {:x 1.0 :y 64.0 :z 2.0}
                :end {:x 1.0 :y 64.0 :z 17.0}})
      (is (seq (:ops (arc-beam/effect-build-plan :electron-bomb {:x 0.0 :y 65.0 :z 0.0} nil 0))))
      (dotimes [_ 14]
        (level-effects/update-effect-state! :electron-bomb
          (fn [store] (arc-beam/effect-tick-state! :level :electron-bomb store))))
      (is (nil? (arc-beam/effect-build-plan :electron-bomb {:x 0.0 :y 65.0 :z 0.0} nil 0))
          "beam flash plan should disappear when ttl decays to zero")

      ;; onUpdate: one md_particle per tick per live ray, on the ray itself.
      (is (= 14 (count @particles*)))
      (is (every? #(= "academy:md_particle" (:particle-type %)) @particles*))
      (doseq [{:keys [x y z]} @particles*]
        (is (= 1.0 (double x)))
        (is (= 64.0 (double y)))
        (is (<= 2.0 (double z) 12.0) "0-10 blocks along the ray")))))

(deftest settlement-beam-outlives-its-context-test
  ;; Upstream's EntityMdRaySmall is a spawned world entity with its own 14-tick
  ;; life. This skill is :instant, so its context ends right after the press —
  ;; long before the delayed settlement beam arrives — and clear-effect-owner!
  ;; (client_ui_hooks, MSG-CTX-TERMINATED) used to take the beam with it.
  (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "eb-clear-test"})
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-sounds/queue-sound-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :electron-bomb "ctx-clear" :electron-bomb/fx-beam
      {:mode :beam
       :start {:x 1.0 :y 64.0 :z 2.0}
       :end {:x 1.0 :y 64.0 :z 17.0}
       :performed? true})
    (is (= 1 (count (get (:beams (electron-bomb-fx/fx-snapshot)) [:ctx "ctx-clear"]))))
    (electron-bomb-fx/clear-fx-owner! [:ctx "ctx-clear"])
    (is (= 1 (count (get (:beams (electron-bomb-fx/fx-snapshot)) [:ctx "ctx-clear"])))
        "a fired settlement beam survives its context ending")
    (is (nil? (get (:effect-state (electron-bomb-fx/fx-snapshot)) [:ctx "ctx-clear"]))
        "the context-bound ball state is still cleared")))


(ns cn.li.ac.content.ability.meltdowner.light-shield-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.light-shield-fx :as ls-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (ls-fx/reset-fx-for-test!)
          (f)
          (finally
            (ls-fx/reset-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-light-shield-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (ls-fx/init!)
      (is (= :light-shield (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:light-shield/fx-start
               :light-shield/fx-tick
               :light-shield/fx-end}
             @registered-topics*)))))

(deftest start-end-update-state-and-build-plan-test
  (let [
        particles* (atom [])
        sounds* (atom [])
        bridge* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "light-shield-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                             (swap! particles* conj args)
                                                             nil)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! bridge* conj [effect-key payload])
                                                     nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                        (swap! sounds* conj args)
                                                        nil)
                  ;; the spark emitter is a 30%-per-tick roll; force it on
                  rand (fn ([] 0.0) ([n] (* 0.0 n)))]
      (arc-beam/enqueue-for-test! :light-shield "ctx-ls" :light-shield/fx-start {:mode :start :source-player-id "player-a"})
      (is (some? (get-in (ls-fx/fx-snapshot) [:effect-state [:ctx "ctx-ls"]])))
      ;; No lookingPos, no particles: they are world-space and the caster's
      ;; position only arrives on the per-tick channel.
      (level-effects/update-effect-state! :light-shield
        (fn [store] (arc-beam/effect-tick-state! :level :light-shield store)))
      (is (empty? @particles*))
      (arc-beam/enqueue-for-test! :light-shield "ctx-ls" :light-shield/fx-tick
                                  {:mode :tick :pos {:x 20.0 :y 65.6 :z -8.0}
                                   :source-player-id "player-a"})
      (dotimes [_ 5]
        (level-effects/update-effect-state! :light-shield
          (fn [store] (arc-beam/effect-tick-state! :level :light-shield store))))
      (is (seq @particles*))
      ;; lookingPos(player, 1) +/- 0.5 on each axis — never the world origin.
      (doseq [[_owner {:keys [x y z]}] @particles*]
        (is (<= 19.5 (double x) 20.5))
        (is (<= 65.1 (double y) 66.1))
        (is (<= -8.5 (double z) -7.5)))
      ;; The shield itself is the spawned entity_md_shield (spinning-shield
      ;; profile), so this effect contributes no level-plan ops at all — see
      ;; impl/light_shield's build-plan. Asserting a map here only passed while
      ;; that namespace happened not to be loaded and the arc-beam default
      ;; answered instead.
      (is (nil? (arc-beam/effect-build-plan :light-shield {:x 0.0 :y 64.0 :z 0.0} {:player-uuid "player-a" :x 0.0 :y 64.0 :z 0.0} 12)))
      ;; c_spawn: md.shield_startup at 0.5 plus a FollowEntitySound loop of
      ;; md.shield_loop that runs until c_end stops it. The port used to play a
      ;; made-up md.shield_on, no loop at all, and then fired md.shield_loop
      ;; once as a shutdown cue the original does not have.
      (is (= [{:type :sound :sound-id "academy:md.shield_startup" :volume 0.5 :pitch 1.0}]
             (mapv last @sounds*)))
      (is (= [:mcmod/start-loop-sound-at-player] (mapv first @bridge*)))
      (is (= {:key "light-shield/ctx-ls"
              :sound-id "academy:md.shield_loop"
              :owner-uuid "player-a"
              :volume 1.0
              :pitch 1.0}
             (second (first @bridge*))))
      (arc-beam/enqueue-for-test! :light-shield "ctx-ls" :light-shield/fx-end {:mode :end :source-player-id "player-a"})
      (is (nil? (get-in (ls-fx/fx-snapshot) [:effect-state [:ctx "ctx-ls"]])))
      (is (= [:mcmod/start-loop-sound-at-player :mcmod/stop-loop-sound]
             (mapv first @bridge*)))
      (is (= {:key "light-shield/ctx-ls"} (second (second @bridge*))))
      ;; ...and nothing else joined the one-shot sounds on the way out.
      (is (= 1 (count @sounds*))))))

;; Upstream rolls the spark emitter at 30% per tick rather than on a fixed
;; cadence, so the gate is pinned by driving `rand` from both sides.
(defn- light-shield-particles-over-10-ticks
  [roll]
  (let [particles* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "light-shield-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particles* conj args)
                                                            nil)
                  client-bridge/run-client-effect! (fn [& _] nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)
                  rand (fn ([] roll) ([n] (* roll n)))]
      (arc-beam/enqueue-for-test! :light-shield "ctx-cadence" :light-shield/fx-start {:mode :start :source-player-id "player-a"})
      (arc-beam/enqueue-for-test! :light-shield "ctx-cadence" :light-shield/fx-tick
                                  {:mode :tick :pos {:x 0.0 :y 65.6 :z 0.0}
                                   :source-player-id "player-a"})
      (dotimes [_ 10]
        (level-effects/update-effect-state! :light-shield
          (fn [store] (arc-beam/effect-tick-state! :level :light-shield store))))
      (let [ticks (get-in (ls-fx/fx-snapshot) [:effect-state [:ctx "ctx-cadence"] :ticks])]
        (arc-beam/enqueue-for-test! :light-shield "ctx-cadence" :light-shield/fx-end {:mode :end :source-player-id "player-a"})
        {:ticks ticks
         :particles (count @particles*)
         :cleared? (nil? (get-in (ls-fx/fx-snapshot) [:effect-state [:ctx "ctx-cadence"]]))}))))

(deftest light-shield-particle-cadence-test
  (let [always (light-shield-particles-over-10-ticks 0.0)
        never (light-shield-particles-over-10-ticks 0.9)]
    (is (= 10 (:ticks always)))
    (is (= 10 (:particles always)) "one spark per tick when the roll always passes")
    (is (= 0 (:particles never)) "no sparks when the roll always fails")
    (is (true? (:cleared? always)))
    (is (true? (:cleared? never)))))

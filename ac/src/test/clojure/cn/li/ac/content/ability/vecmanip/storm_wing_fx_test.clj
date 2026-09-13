(ns cn.li.ac.content.ability.vecmanip.storm-wing-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [clojure.string :as str]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.tornado :as tornado]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.vecmanip.storm-wing-fx :as swfx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge])
  (:import [cn.li.mcmod.math V3]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (swfx/reset-storm-wing-fx-for-test!)
        (f)
        (finally
          (swfx/reset-storm-wing-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

;; StormWing has no arc-beam impl — it owns its enqueue/tick fns and registers
;; them through fx-spec, so tests drive those directly.
(defn- enqueue!
  [enqueue-state! ctx-id payload]
  (level-effects/update-effect-state! :storm-wing
    (fn [store]
      (enqueue-state! store ctx-id :storm-wing/fx-update [:ctx ctx-id] payload)))
  nil)

(defn- tick!
  [tick-state!]
  (level-effects/update-effect-state! :storm-wing
    (fn [store] (tick-state! store)))
  nil)

(deftest init-registers-owner-aware-storm-wing-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (swfx/init!)
      (is (= :storm-wing (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:storm-wing/fx-start
               :storm-wing/fx-update
               :storm-wing/fx-end}
             @registered-topics*)))))

(deftest flying-build-plan-queues-particles-and-loop-sound-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/tick-state!)
        build-plan (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/build-plan)
        client-effects* (atom [])
        particle-calls* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "storm-wing-test"})
                  client-bridge/run-client-effect! (fn [effect & args]
                                                     (swap! client-effects* conj [effect (vec args)])
                                                     nil)
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particle-calls* conj args)
                                                            nil)
                  rand (fn [] 0.5)]
      (enqueue! enqueue-state! "ctx-main" {:mode :start :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-main" {:mode :update :phase :flying :charge-ticks 40 :charge-ratio 1.0 :source-player-id "player-a"})
      (dotimes [_ 10]
        (tick! tick-state!))
      (let [plan (build-plan nil {:x 0.0 :y 64.0 :z 0.0 :player-uuid "player-a"} 0 nil)]
        (is (= 1 (count @client-effects*)) "loop sound started once via client bridge")
        (is (= :mcmod/start-loop-sound-at-player (ffirst @client-effects*)))
        (is (str/includes? (str (get-in (first @client-effects*) [1 0 :sound-id]))
                           "vecmanip.storm_wing"))
        (is (= 12 (count @particle-calls*)))
        (is (pos? (count (:ops plan))) "tornado ring quads emitted")
        (is (= 10 (get-in (swfx/storm-wing-fx-snapshot)
                          [:effect-state [:ctx "ctx-main"] :ticks])))))))

(deftest end-stops-loop-sound-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/enqueue-state!)
        client-effects* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "storm-wing-test"})
                  client-bridge/run-client-effect! (fn [effect & args]
                                                     (swap! client-effects* conj [effect (vec args)])
                                                     nil)]
      (enqueue! enqueue-state! "ctx-main" {:mode :start :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-main" {:mode :end :performed? true :source-player-id "player-a"})
      (let [tick-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/tick-state!)]
        (tick! tick-state!)
        (is (= [:mcmod/start-loop-sound-at-player :mcmod/stop-loop-sound]
               (mapv first @client-effects*)))
        (is (= "storm-wing/ctx-main" (get-in (second @client-effects*) [1 0 :key])))
        ;; Original StormWingEffect keeps rendering for TERMINATE_TICK (15)
        ;; more ticks, fading alpha out, before setDead().
        (is (true? (get-in (swfx/storm-wing-fx-snapshot)
                           [:effect-state [:ctx "ctx-main"] :active?]))
            "still rendering during the terminate fade-out")
        (dotimes [_ 15] (tick! tick-state!))
        (is (nil? (get-in (swfx/storm-wing-fx-snapshot)
                          [:effect-state [:ctx "ctx-main"] :active?]))
            "state entry is dropped once the fade-out completes")))))


(deftest improved-noise-matches-reference-implementation-test
  (let [fade tornado/fade
        noise tornado/perlin-noise]
    ;; 6t^5 - 15t^4 + 10t^3: an S-curve pinned to [0,1] over [0,1]. The old
    ;; port folded the +10 into the product, giving fade(1) = -90 and noise
    ;; values in the hundreds.
    (is (= 0.0 (fade 0.0)))
    (is (= 1.0 (fade 1.0)))
    (is (< (Math/abs (- 0.5 (double (fade 0.5)))) 1.0e-12))
    ;; Perlin's improved noise is bounded by ~sqrt(2)/2 in 3D and is exactly 0
    ;; on integer lattice points.
    (is (= 0.0 (noise 1.0 2.0 3.0)))
    (doseq [i (range 200)]
      (let [v (double (noise (* 0.137 i) (* 0.371 i) (if (odd? i) 1.0 0.0)))]
        (is (<= -1.0 v 1.0) (str "noise out of range at i=" i ": " v))))))

(deftest ring-quads-tile-the-texture-once-around-the-column-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/tick-state!)
        build-plan (var-get #'cn.li.ac.content.ability.vecmanip.storm-wing-fx/build-plan)]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "storm-wing-test"})
                  client-bridge/run-client-effect! (fn [& _] nil)
                  client-particles/queue-particle-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-main" {:mode :start :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-main" {:mode :update :phase :flying :charge-ratio 1.0
                                           :source-player-id "player-a"})
      (tick! tick-state!)
      (let [ops (:ops (build-plan nil {:player-x 0.0 :player-y 64.0 :player-z 0.0
                                       :player-body-yaw-rad 0.0 :player-pitch-rad 0.0
                                       :x 0.0 :y 65.6 :z 0.0 :player-uuid "player-a"}
                                  0 nil))
            widths (into #{} (map (fn [{:keys [u0 u1]}] (Math/round (* 1.0e6 (- u1 u0))))) ops)]
        ;; Original: u spans exactly 1/div per segment, so 20 segments wrap the
        ;; texture once around the ring (not once per segment).
        (is (= #{50000} widths))
        (is (every? (fn [{:keys [v0 v1]}] (and (= 0.0 v0) (= 1.0 v1))) ops))
        ;; eff.alpha (0.7 while flying) * 0.7 at render time = 0.49.
        (is (= #{(int (* 255.0 0.7 0.7))} (into #{} (map (comp :a :color)) ops)))
        ;; Ring radii stay in the original's ~0.16-scale ballpark: every corner
        ;; is within a couple of blocks of the caster, never tens of blocks.
        (is (every? (fn [op]
                      (every? (fn [^V3 p]
                                (< (Math/abs (- (.-y p) 64.0)) 4.0))
                              [(:p0 op) (:p1 op) (:p2 op) (:p3 op)]))
                    ops))))))



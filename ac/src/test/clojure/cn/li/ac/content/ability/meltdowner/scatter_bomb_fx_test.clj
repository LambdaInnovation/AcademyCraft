(ns cn.li.ac.content.ability.meltdowner.scatter-bomb-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.scatter-bomb-fx :as sb-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (sb-fx/reset-scatter-bomb-fx-for-test!)
          (f)
          (finally
            (sb-fx/reset-scatter-bomb-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

;; ScatterBomb has no arc-beam impl — it owns its enqueue/tick fns and registers
;; them through fx-spec, so tests drive those directly.
(defn- enqueue!
  [enqueue-state! ctx-id channel payload]
  (level-effects/update-effect-state! :scatter-bomb
    (fn [store] (enqueue-state! store ctx-id channel [:ctx ctx-id] payload)))
  nil)

(defn- tick!
  [tick-state!]
  (level-effects/update-effect-state! :scatter-bomb
    (fn [store] (tick-state! store)))
  nil)

(deftest init-registers-owner-aware-scatter-bomb-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (sb-fx/init!)
      (is (= :scatter-bomb (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:scatter-bomb/fx-start
               :scatter-bomb/fx-ball
               :scatter-bomb/fx-beam
               :scatter-bomb/fx-end}
             @registered-topics*)))))

(deftest start-ball-beam-end-manage-state-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/tick-state!)
        particles* (atom [])
        sounds* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                  client-particles/queue-current-particle-effect! (fn [& args]
                                                                     (swap! particles* conj args)
                                                                     nil)
                  client-sounds/queue-current-sound-effect! (fn [& args]
                                                                (swap! sounds* conj args)
                                                                nil)]
      (enqueue! enqueue-state! "ctx-sb" :scatter-bomb/fx-start {:mode :start :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-sb" :scatter-bomb/fx-ball {:mode :ball
                                                :x 1.0 :y 64.0 :z 2.0
                                                :count 3
                                                :source-player-id "player-a"})
      (is (= 3 (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-sb"] :balls])))
      (tick! tick-state!)
      (is (= 1 (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-sb"] :ticks])))
      (enqueue! enqueue-state! "ctx-sb" :scatter-bomb/fx-beam {:mode :beam
                                                 :start {:x 1.0 :y 64.0 :z 2.0}
                                                 :end {:x 2.0 :y 64.0 :z 3.0}
                                                 :source-player-id "player-a"})
      ;; The release beam is stored and renders billboard ops (replacing the
      ;; old fixed-direction md_ray_small entity spawn).
      (is (= 1 (count (get-in (sb-fx/scatter-bomb-fx-snapshot) [:beams [:ctx "ctx-sb"]]))))
      (let [plan ((var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/build-plan)
                  {:x 0.0 :y 65.0 :z 0.0} nil 0)]
        (is (seq (:ops plan))))
      (enqueue! enqueue-state! "ctx-sb" :scatter-bomb/fx-end {:mode :end :source-player-id "player-a"})
      (is (nil? (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-sb"]])))
      ;; The release beams arrive AFTER :end (the server schedules them one
      ;; tick out of the delayed-projectiles queue), so :end must not wipe
      ;; them — wiping raced their arrival and deleted every ray the moment
      ;; it appeared.
      (is (= 1 (count (get-in (sb-fx/scatter-bomb-fx-snapshot) [:beams [:ctx "ctx-sb"]]))))
      (is (seq @particles*))
      (is (seq @sounds*)))))

(deftest scatter-bomb-tick-cadence-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/tick-state!)]
    (with-redefs [client-particles/queue-current-particle-effect! (fn [& _] nil)
                  client-sounds/queue-current-sound-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-cadence" :scatter-bomb/fx-start {:mode :start :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-cadence" :scatter-bomb/fx-ball {:mode :ball
                                                     :x 1.0 :y 64.0 :z 2.0
                                                     :count 4
                                                     :source-player-id "player-a"})

      (dotimes [_ 5]
        (tick! tick-state!))

      (is (= 5 (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-cadence"] :ticks])))
      (is (= 4 (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-cadence"] :balls])))

      (enqueue! enqueue-state! "ctx-cadence" :scatter-bomb/fx-end {:mode :end :source-player-id "player-a"})
      (is (nil? (get-in (sb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-cadence"]]))))))

;; ---------------------------------------------------------------------------
;; EntityMdRaySmall parity
;; ---------------------------------------------------------------------------

(def ^:private enqueue-state!* (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/enqueue-state!))
(def ^:private tick-state!* (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/tick-state!))
(def ^:private build-plan* (var-get #'cn.li.ac.content.ability.meltdowner.scatter-bomb-fx/build-plan))

(defn- fire-ray! [ctx-id]
  (enqueue! enqueue-state!* ctx-id :scatter-bomb/fx-beam
            {:mode :beam
             :start {:x 0.0 :y 64.0 :z 0.0}
             :end {:x 10.0 :y 64.0 :z 0.0}
             :source-player-id "player-a"}))

(defn- axis-radius [op key]
  (let [^cn.li.mcmod.math.V3 p (get op key)]
    (Math/sqrt (+ (Math/pow (- (.-y p) 64.0) 2) (Math/pow (.-z p) 2)))))

(deftest ray-uses-the-mdray-small-composite-test
  ;; SmallMdRayRender: glow width 0.3 (halved by drawBoard), cylinderOut radius
  ;; 0.045 rgba(106,242,106,50), cylinderIn radius 0.03 rgba(216,248,216,230).
  ;; The port had a 0.3 HALF-width outer quad wearing the outer cylinder's
  ;; colour, the outer radius carrying a made-up alpha, and the inner cylinder
  ;; reduced to a one-pixel line.
  (with-redefs [client-particles/queue-current-particle-effect! (fn [& _] nil)
                client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (fire-ray! "ctx-ray")
    (let [ops (:ops (build-plan* {:x 0.0 :y 70.0 :z 0.0} nil 0))
          glow (filter #(re-find #"effects/mdray_small/" (str (:texture %))) ops)
          tubes (remove #(re-find #"effects/mdray_small/" (str (:texture %))) ops)
          radii (map #(axis-radius % :p0) tubes)]
      (is (= 3 (count glow)) "blend_in / tile / blend_out")
      (is (seq tubes))
      ;; both tubes are present, and neither is anywhere near the old 0.3
      (is (< 0.04 (apply max radii) 0.05) "the outer cylinder is 0.045 in radius")
      (is (< (apply min (remove zero? radii)) 0.031)
          "and the inner one is still a tube at 0.03, not a line")
      (is (empty? (filter #(= :line (:kind %)) ops))))))

(deftest ray-shrinks-over-the-last-500ms-test
  ;; getWidth() ramps 1 -> 0 across the final 500ms (10 of the 14 ticks); the
  ;; port cut the ray off outright with 210ms left.
  (with-redefs [client-particles/queue-current-particle-effect! (fn [& _] nil)
                client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (fire-ray! "ctx-shrink")
    (let [radius-now (fn []
                       (let [ops (:ops (build-plan* {:x 0.0 :y 70.0 :z 0.0} nil 0))
                             tubes (remove #(re-find #"effects/mdray_small/" (str (:texture %))) ops)]
                         (when (seq tubes) (apply max (map #(axis-radius % :p0) tubes)))))]
      (dotimes [_ 4] (tick! tick-state!*))          ;; ttl 10 — shrink starts here
      (let [full (radius-now)]
        (dotimes [_ 5] (tick! tick-state!*))        ;; ttl 5 — halfway down
        (let [half (radius-now)]
          (is (some? full))
          (is (some? half))
          (is (< 0.4 (/ half full) 0.6) "half the width with half the shrink window left")))
      (dotimes [_ 5] (tick! tick-state!*))
      (is (nil? (radius-now)) "and gone once the ray expires"))))

(deftest ray-plays-its-own-sound-at-the-shot-test
  ;; EntityMdRaySmall.onFirstUpdate: md.ray_small at 0.8, positioned at the ray.
  ;; The port played md.eb_explode (the electron bomb's detonation) at 0.4/1.2
  ;; with no coordinates, so it came from the listener.
  (let [sounds* (atom [])]
    (with-redefs [client-particles/queue-current-particle-effect! (fn [& _] nil)
                  client-sounds/queue-current-sound-effect! (fn [& args]
                                                              (swap! sounds* conj (first args))
                                                              nil)]
      (fire-ray! "ctx-sound")
      (let [snd (first (filter #(re-find #"ray_small" (str (:sound-id %))) @sounds*))]
        (is (some? snd) (str "expected md.ray_small, got " (mapv :sound-id @sounds*)))
        (is (= 0.8 (:volume snd)))
        (is (= 1.0 (:pitch snd)))
        (is (= [0.0 64.0 0.0] [(:x snd) (:y snd) (:z snd)]))))))

(deftest ray-leaves-a-particle-trail-test
  ;; EntityMdRaySmall.onUpdate spawns one MdParticle per tick at a random point
  ;; 0-10 blocks along the ray; the port only puffed sparks at the endpoint.
  ;; The trail queues via the owner captured at :beam enqueue (the tick path
  ;; has no owner binding), so it lands in queue-particle-effect!, not the
  ;; current-owner queue.
  (let [particles* (atom [])
        owners* (atom [])]
    (with-redefs [client-particles/queue-particle-effect! (fn [owner particle-cmd]
                                                            (swap! particles* conj particle-cmd)
                                                            (swap! owners* conj owner)
                                                            nil)
                  client-sounds/queue-current-sound-effect! (fn [& _] nil)]
      (fire-ray! "ctx-trail")
      (reset! particles* [])
      (dotimes [_ 5] (tick! tick-state!*))
      (let [motes (filter #(re-find #"md_particle" (str (:particle-type %))) @particles*)]
        (is (= 5 (count motes)) "one per tick while the ray lives")
        (is (every? (fn [p] (<= 0.0 (:x p) 10.0)) motes)
            "spread along the ray, not piled on the endpoint")
        (is (> (count (distinct (map :x motes))) 1))
        (is (every? #(some? (:client-session-id %)) @owners*)
            "queued under the owner captured at enqueue, not the unbound tick path")))))

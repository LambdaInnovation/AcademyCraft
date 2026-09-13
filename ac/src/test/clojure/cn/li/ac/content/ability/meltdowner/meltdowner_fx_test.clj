(ns cn.li.ac.content.ability.meltdowner.meltdowner-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.meltdowner-fx :as md-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (md-fx/reset-fx-for-test!)
          (f)
          (finally
            (md-fx/reset-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-meltdowner-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (md-fx/init!)
      (is (= :meltdowner (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:meltdowner/fx-start
               :meltdowner/fx-update
               :meltdowner/fx-end
               :meltdowner/fx-perform
               :meltdowner/fx-reflect}
             @registered-topics*)))))

(deftest fx-handler-routes-meltdowner-channels-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (md-fx/init!)
      ((get @handlers* :meltdowner/fx-start) "ctx-md" :meltdowner/fx-start {:source-player-id "player-a"})
      ((get @handlers* :meltdowner/fx-update) "ctx-md" :meltdowner/fx-update {:ticks 9
                                                  :charge-ratio 0.7
                                                  ;; the caster position rides along so the
                                                  ;; charge motes can ring them
                                                  :caster-x 1.0 :caster-y 64.0 :caster-z 2.0
                                                  :source-player-id "player-a"})
      ((get @handlers* :meltdowner/fx-perform) "ctx-md" :meltdowner/fx-perform {:start {:x 0.0 :y 64.0 :z 0.0}
                                                   :end {:x 2.0 :y 64.0 :z 2.0}
                                                   :charge-ticks 18
                                                   :beam-length 24.0
                                                   :source-player-id "player-a"})
      ((get @handlers* :meltdowner/fx-reflect) "ctx-md" :meltdowner/fx-reflect {:start {:x 0.0 :y 64.0 :z 0.0}
                                                   :end {:x 1.0 :y 65.0 :z 1.0}
                                                   :source-player-id "player-a"})
      (is (= [[:meltdowner "ctx-md" :meltdowner/fx-start
               {:source-player-id "player-a" :mode :start}
               [:owner-key [:ctx "ctx-md"]]]
              [:meltdowner "ctx-md" :meltdowner/fx-update
               {:source-player-id "player-a" :mode :update
                :ticks 9 :charge-ratio 0.7
                :caster-x 1.0 :caster-y 64.0 :caster-z 2.0}
               [:owner-key [:ctx "ctx-md"]]]
              [:meltdowner "ctx-md" :meltdowner/fx-perform
               {:source-player-id "player-a" :mode :perform
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 2.0 :y 64.0 :z 2.0}
                :charge-ticks 18
                :beam-length 24.0}
               [:owner-key [:ctx "ctx-md"]]]
              [:meltdowner "ctx-md" :meltdowner/fx-reflect
               {:source-player-id "player-a" :mode :reflect
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 1.0 :y 65.0 :z 1.0}}
               [:owner-key [:ctx "ctx-md"]]]]
             @enqueued*)))))

(deftest start-update-perform-end-manage-state-test
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :meltdowner "ctx-a" :meltdowner/fx-start {:mode :start :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :meltdowner "ctx-a" :meltdowner/fx-update {:mode :update
                                             :ticks 10
                                             :charge-ratio 0.5
                                             :source-player-id "player-a"})
    (is (some? (get-in (md-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
    (arc-beam/enqueue-for-test! :meltdowner "ctx-a" :meltdowner/fx-perform {:mode :perform
                                              :start {:x 1.0 :y 64.0 :z 0.0}
                                              :end {:x 2.0 :y 64.0 :z 1.0}
                                              :charge-ticks 20
                                              :beam-length 30.0
                                              :source-player-id "player-a"})
    (is (some? (get-in (md-fx/fx-snapshot) [:rays [:ctx "ctx-a"]])))
    (arc-beam/enqueue-for-test! :meltdowner "ctx-a" :meltdowner/fx-end {:mode :end
                                          :performed? true
                                          :source-player-id "player-a"})
    (let [snapshot (md-fx/fx-snapshot)]
      (is (false? (get-in snapshot [:effect-state [:ctx "ctx-a"] :active?])))
      (is (some? (get-in snapshot [:rays [:ctx "ctx-a"]]))))))

(deftest build-plan-and-tick-state-test
  (let [
        sounds* (atom [])
        bridge* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& args]
                                                     (swap! bridge* conj args)
                                                     nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                       (swap! sounds* conj args)
                                                       nil)
                  client-sounds/current-effect-owner (fn [] :test-owner)
                  rand-int (fn [_] 0)]
      (arc-beam/enqueue-for-test! :meltdowner "ctx-main" :meltdowner/fx-start {:mode :start :source-player-id "player-a"})
      (arc-beam/enqueue-for-test! :meltdowner "ctx-main" :meltdowner/fx-update {:mode :update
                                                  :ticks 8
                                                  :charge-ratio 0.8
                                                  :source-player-id "player-a"})
      (arc-beam/enqueue-for-test! :meltdowner "ctx-main" :meltdowner/fx-perform {:mode :perform
                                                   :start {:x 0.0 :y 64.0 :z 0.0}
                                                   :end {:x 2.0 :y 64.0 :z 2.0}
                                                   :source-player-id "player-a"})
      (is (= [[:mcmod/start-loop-sound-at-player
               {:key "meltdowner/ctx-main" :sound-id "academy:md.md_charge"
                :owner-uuid "player-a" :volume 1.0 :pitch 1.0
                  ;; FollowEntitySound with setVolume(1.0) and no setLoop()
                  :loop? false}]]
             @bridge*)
          ":start starts the FollowEntitySound loop attached to the caster")
      (is (some? (arc-beam/effect-build-plan :meltdowner {:x 0.0 :y 65.0 :z 0.0}
                             {:player-uuid "player-a" :x 0.0 :y 64.0 :z 0.0}
                             0)))
      (level-effects/update-effect-state! :meltdowner
        (fn [store] (arc-beam/effect-tick-state! :level :meltdowner store)))
      (is (seq @sounds*) "fire sound queued from :perform")
      (is (= 1 (count @bridge*)) "no loop re-queue on tick")
      (is (some? (arc-beam/effect-build-plan :meltdowner {:x 0.0 :y 65.0 :z 0.0}
                             {:player-uuid "player-a" :x 0.0 :y 64.0 :z 0.0}
                             1))))))

(deftest charge-loop-lifecycle-and-ray-expiry-test
  (let [
        sounds* (atom [])
        bridge* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& args]
                                                     (swap! bridge* conj args)
                                                     nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                       (swap! sounds* conj args)
                                                       nil)
                  client-sounds/current-effect-owner (fn [] :test-owner)
                  rand-int (fn [_] 0)]
      (arc-beam/enqueue-for-test! :meltdowner "ctx-cadence" :meltdowner/fx-start {:mode :start :source-player-id "player-a"})
      (arc-beam/enqueue-for-test! :meltdowner "ctx-cadence" :meltdowner/fx-perform {:mode :perform
                                                      :start {:x 0.0 :y 64.0 :z 0.0}
                                                      :end {:x 2.0 :y 64.0 :z 2.0}
                                                      :source-player-id "player-a"})

      ;; FollowEntitySound: started once on :start, never re-queued while
      ;; charging, stopped on :end (original c_terminate's sound.stop()).
      ;; EntityMDRay.life is a flat 50 ticks (the port used to roll 16-23).
      (dotimes [_ 50]
        (level-effects/update-effect-state! :meltdowner
          (fn [store] (arc-beam/effect-tick-state! :level :meltdowner store))))
      (is (= 1 (count @bridge*))
          "loop sound started once, no per-tick re-queue")
      (is (some? (arc-beam/effect-build-plan :meltdowner {:x 0.0 :y 65.0 :z 0.0}
                             {:player-uuid "player-a" :x 0.0 :y 64.0 :z 0.0}
                             16))
          "charge plan still builds while active")
      (is (nil? (get-in (md-fx/fx-snapshot) [:rays [:ctx "ctx-cadence"]]))
          "ray expired after its ttl")

      (arc-beam/enqueue-for-test! :meltdowner "ctx-cadence" :meltdowner/fx-end
        {:mode :end :performed? true :source-player-id "player-a"})
      (is (= [[:mcmod/start-loop-sound-at-player
               {:key "meltdowner/ctx-cadence" :sound-id "academy:md.md_charge"
                :owner-uuid "player-a" :volume 1.0 :pitch 1.0 :loop? false}]
              [:mcmod/stop-loop-sound {:key "meltdowner/ctx-cadence"}]]
             @bridge*)
          "loop stopped on :end"))))

(deftest fired-ray-outlives-its-context-test
  ;; Upstream c_perform spawns EntityMDRay into the world; c_terminate only
  ;; restores walk speed and stops the charge loop sound, so the ray lives out
  ;; its own life. clear-effect-owner! (MSG-CTX-TERMINATED) must not take it.
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)]
   (arc-beam/enqueue-for-test! :meltdowner "ctx-clear" :meltdowner/fx-perform
    {:mode :perform
     :start {:x 1.0 :y 64.0 :z 0.0}
     :end {:x 2.0 :y 64.0 :z 1.0}
     :charge-ticks 20
     :beam-length 30.0
     :source-player-id "player-a"})
  (is (some? (get (:rays (md-fx/fx-snapshot)) [:ctx "ctx-clear"])))
  (md-fx/clear-fx-owner! [:ctx "ctx-clear"])
  (is (some? (get (:rays (md-fx/fx-snapshot)) [:ctx "ctx-clear"]))
      "a fired ray survives its context ending")
   (is (nil? (get (:effect-state (md-fx/fx-snapshot)) [:ctx "ctx-clear"]))
       "the context-bound charge state is still cleared")))


;; ---------------------------------------------------------------------------
;; EntityMDRay parity
;; ---------------------------------------------------------------------------

(defn- fire-ray! [ctx-id]
  (arc-beam/enqueue-for-test! :meltdowner ctx-id :meltdowner/fx-perform
    {:mode :perform
     :start {:x 0.0 :y 64.0 :z 0.0}
     :end {:x 20.0 :y 64.0 :z 0.0}
     :source-player-id "player-a"}))

(defn- tick-fx! [n]
  (dotimes [_ n]
    (level-effects/update-effect-state! :meltdowner
      (fn [store] (arc-beam/effect-tick-state! :level :meltdowner store)))))

(defn- ray-ops []
  (:ops (arc-beam/effect-build-plan :meltdowner {:x 0.0 :y 70.0 :z 0.0}
                                    {:player-uuid "other" :x 0.0 :y 64.0 :z 0.0} 0)))

(defn- axis-radius
  "Perpendicular distance (yz plane) from a vertex to the fitted ray axis.
  The test ray runs along +x at y = 64, z = 0. view-fix-rays shifts only the
  START by a hand offset (upstream 'don't fix end'), so the tube is slightly
  tilted — fit the axis from the near and far ring centres instead of an
  absolute one."
  [op key near far]
  (let [^cn.li.mcmod.math.V3 p (get op key)
        [ay az] near
        [by bz] far
        dy (- by ay)
        dz (- bz az)
        len (Math/sqrt (+ (* dy dy) (* dz dz)))]
    (if (< len 1.0e-6)
      (Math/sqrt (+ (Math/pow (- (.-y p) ay) 2)
                    (Math/pow (- (.-z p) az) 2)))
      (/ (Math/abs (- (* dy (- (.-z p) az))
                      (* dz (- (.-y p) ay))))
         len))))

(defn- ray-axis
  "Fit the ray axis (near ring centre, far ring centre) in the yz plane from
  the tube's own vertices."
  [tubes]
  (let [p0-x (fn [op] (.-x ^cn.li.mcmod.math.V3 (:p0 op)))
        xs (map p0-x tubes)
        min-x (apply min xs)
        max-x (apply max xs)
        ring-mean (fn [pred]
                    (let [pts (filter pred tubes)
                          n (count pts)
                          ys (map (fn [op] (.-y ^cn.li.mcmod.math.V3 (:p0 op))) pts)
                          zs (map (fn [op] (.-z ^cn.li.mcmod.math.V3 (:p0 op))) pts)]
                      [(/ (reduce + ys) n) (/ (reduce + zs) n)]))]
    [(ring-mean #(< (p0-x %) (+ min-x 0.5)))
     (ring-mean #(> (p0-x %) (- max-x 0.5)))]))

(deftest ray-uses-the-mdray-composite-test
  ;; MDRayRender: cylinderIn radius 0.17 rgba(216,248,216,230), cylinderOut
  ;; 0.22 rgba(106,242,106,50), glow width 1.5 white at 0.8. The port drew
  ;; 0.09 and 0.09*0.42 in colours of its own.
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                client-sounds/queue-sound-effect! (fn [& _] nil)]
    (fire-ray! "ctx-ray")
    (tick-fx! 5)
    (let [ops (ray-ops)
          glow (filter #(re-find #"effects/mdray/" (str (:texture %))) ops)
          tubes (remove #(re-find #"effects/mdray/" (str (:texture %))) ops)
          [near far] (ray-axis tubes)
          radii (map #(axis-radius % :p0 near far) tubes)]
      (is (= 3 (count glow)) "blend_in / tile / blend_out")
      (is (seq tubes))
      (is (< 0.2 (apply max radii) 0.26)
          "the outer cylinder is 0.22 in radius, not 0.09")
      (is (some (fn [r] (< 0.15 r 0.2)) radii)
          "and the inner one is a 0.17 tube, not a hairline")
      (is (some #(= 230 (:a (:color %))) tubes) "inner alpha 230")
      (is (some #(= 50 (:a (:color %))) tubes) "outer alpha 50"))))

(deftest ray-lives-fifty-ticks-with-upstream-blends-test
  ;; EntityMDRay: life 50, blendIn 200ms, blendOut 700ms. The port rolled a
  ;; 16-23 tick life and faded linearly across the whole of it.
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                client-sounds/queue-sound-effect! (fn [& _] nil)]
    (fire-ray! "ctx-life")
    (let [ray (first (get (:rays (md-fx/fx-snapshot)) [:ctx "ctx-life"]))]
      (is (= 50 (:ttl ray)))
      (is (= 50 (:max-ttl ray))))
    (let [alpha-now (fn []
                      (let [tubes (remove #(re-find #"effects/mdray/" (str (:texture %))) (ray-ops))]
                        (when (seq tubes) (apply max (map #(:a (:color %)) tubes)))))]
      (tick-fx! 1)
      (is (< (alpha-now) 100) "still blending in after one tick")
      (tick-fx! 4)
      (is (= 230 (alpha-now)) "full by the 200ms mark")
      (tick-fx! 38)                                   ;; ttl 7, inside the 700ms tail
      (is (< (alpha-now) 130) "fading out over the last 700ms")
      (tick-fx! 8)
      (is (nil? (alpha-now)) "gone at 50"))))

(deftest ray-leaves-a-particle-trail-test
  ;; EntityMDRay.onUpdate: on 80% of ticks, one MdParticle 0-10 blocks along
  ;; the ray. The port drew the ray with no trail at all.
  (let [particles* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)
                  cn.li.ac.ability.client.effects.particles/queue-particle-effect!
                  (fn [_owner cmd] (swap! particles* conj cmd) nil)]
      (fire-ray! "ctx-trail")
      (tick-fx! 20)
      (let [motes (filter #(re-find #"md_particle" (str (:particle-type %))) @particles*)]
        (is (< 10 (count motes) 21) "roughly 80% of ticks")
        (is (every? (fn [p] (<= -0.1 (:x p) 10.1)) motes)
            "spread along the first ten blocks of the ray")))))

(deftest charge-motes-ring-the-caster-test
  ;; The particle queue takes absolute world coordinates; the charge motes were
  ;; handing it raw (r*sin, h, r*cos) offsets, so every one spawned within a
  ;; block of world origin.
  (let [particles* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)
                  cn.li.ac.ability.client.effects.particles/queue-particle-effect!
                  (fn [_owner cmd] (swap! particles* conj cmd) nil)]
      (arc-beam/enqueue-for-test! :meltdowner "ctx-motes" :meltdowner/fx-start
        {:mode :start :source-player-id "player-a"})
      (arc-beam/enqueue-for-test! :meltdowner "ctx-motes" :meltdowner/fx-update
        {:mode :update :ticks 1 :charge-ratio 0.1 :source-player-id "player-a"
         :caster-x 100.0 :caster-y 64.0 :caster-z -50.0})
      (tick-fx! 3)
      (let [motes (filter #(re-find #"md_particle" (str (:particle-type %))) @particles*)]
        (is (seq motes))
        (is (every? (fn [p] (and (< 98.9 (:x p) 101.1)
                                 (< 64.3 (:y p) 65.7)
                                 (< -51.1 (:z p) -48.9)))
                    motes)
            (str "motes ring the caster at (100, 64, -50): "
                 (mapv (juxt :x :y :z) (take 3 motes))))))))

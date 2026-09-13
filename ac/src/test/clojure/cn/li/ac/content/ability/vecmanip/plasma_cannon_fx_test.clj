(ns cn.li.ac.content.ability.vecmanip.plasma-cannon-fx-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            ;; arc-beam MUST precede the impl (AOT classes don't self-require)
            ;; so the [:plasma-cannon :level] defmethods are registered before
            ;; the stateful tests enqueue — otherwise effect-initial-state
            ;; falls through to the :default arc state.
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.plasma-cannon :as pcimpl]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.tornado :as tornado]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.vecmanip.plasma-cannon-fx :as pcfx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (pcfx/reset-fx-for-test!)
        (f)
        (finally
          (pcfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :plasma-cannon/fx-update
   :owner-key [:ctx ctx-id]})

(deftest init-registers-plasma-cannon-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (pcfx/init!)
      (is (= :plasma-cannon (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:plasma-cannon/fx-start
               :plasma-cannon/fx-update
               :plasma-cannon/fx-perform
               :plasma-cannon/fx-end}
             @registered-topics*)))))

(deftest fx-handler-routes-start-update-perform-end-payloads-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (pcfx/init!)
      ((get @handlers* :plasma-cannon/fx-start) "ctx-1" :plasma-cannon/fx-start {:charge-pos {:x 1.0 :y 64.0 :z 1.0}
                                                                                 :player-id "caster-1"})
      ((get @handlers* :plasma-cannon/fx-update) "ctx-1" :plasma-cannon/fx-update {:charge-ticks 24
                                                                :fully-charged? true
                                                                :release-ready? true
                                                                :charge-pos {:x 1.0 :y 64.0 :z 1.0}
                                                                :flight-ticks 2
                                                                :state :go
                                                                :destination {:x 4.0 :y 64.0 :z 4.0}
                                                                :player-id "caster-1"})
      ((get @handlers* :plasma-cannon/fx-perform) "ctx-1" :plasma-cannon/fx-perform {:pos {:x 2.0 :y 65.0 :z 2.0}})
      ((get @handlers* :plasma-cannon/fx-end) "ctx-1" :plasma-cannon/fx-end {:performed? true})
      (is (= [[:plasma-cannon "ctx-1" :plasma-cannon/fx-start
               ;; :tornado-base is the server-resolved ground point the charge
               ;; tornado stands on; absent here, the client falls back to 20
               ;; blocks below the charge position (the original's ray miss).
               ;; :source-player-id is the caster the client attaches the
               ;; FollowEntitySound to — the bridge rejects a blank owner uuid,
               ;; so dropping it here silenced the charge sound entirely.
               {:mode :start :charge-pos {:x 1.0 :y 64.0 :z 1.0} :tornado-base nil
                :player-id "caster-1" :source-player-id "caster-1"}
               [:owner-key [:ctx "ctx-1"]]]
              [:plasma-cannon "ctx-1" :plasma-cannon/fx-update
               {:mode :update
                :charge-ticks 24
                :fully-charged? true
                :release-ready? true
                :charge-pos {:x 1.0 :y 64.0 :z 1.0}
                :flight-ticks 2
                :state :go
                :destination {:x 4.0 :y 64.0 :z 4.0}
                :player-id "caster-1" :source-player-id "caster-1"}
               [:owner-key [:ctx "ctx-1"]]]
              [:plasma-cannon "ctx-1" :plasma-cannon/fx-perform
               {:mode :perform :pos {:x 2.0 :y 65.0 :z 2.0}}
               [:owner-key [:ctx "ctx-1"]]]
              [:plasma-cannon "ctx-1" :plasma-cannon/fx-end
               {:mode :end :performed? true}
               [:owner-key [:ctx "ctx-1"]]]]
             @enqueued*)))))

(deftest tick-build-plan-and-perform-effects-test
  (let [
        sound-calls* (atom [])
        particle-calls* (atom [])
        bridge-calls* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "plasma-cannon-test"})
                  client-sounds/queue-sound-effect! (fn [& args]
                                                      (swap! sound-calls* conj args)
                                                      nil)
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particle-calls* conj args)
                                                            nil)
                  client-bridge/run-client-effect! (fn [& args]
                                                     (swap! bridge-calls* conj args)
                                                     nil)
                  rand (fn [] 0.5)]
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-main" :plasma-cannon/fx-update {:mode :start :charge-pos {:x 1.0 :y 64.0 :z 1.0}})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-main" :plasma-cannon/fx-update {:mode :update
                             :charge-ticks 24
                             :fully-charged? true
                             :charge-pos {:x 1.0 :y 64.0 :z 1.0}
                             :flight-ticks 2
                             :state :go
                             :destination {:x 4.0 :y 64.0 :z 4.0}})
      (dotimes [_ 10]
        (level-effects/update-effect-state! :plasma-cannon
          (fn [store] (arc-beam/effect-tick-state! :level :plasma-cannon store))))
      (let [plan (arc-beam/effect-build-plan :plasma-cannon nil nil 0)]
        ;; The charge sound is one FollowEntitySound started via the bridge.
        ;; Upstream never calls setLoop() on it, so it plays its 5.9s clip once
        ;; at full volume and is cut short by stop().
        (is (= [[:mcmod/start-loop-sound-at-player
                 {:key "plasma-cannon/ctx-main" :sound-id "academy:vecmanip.plasma_cannon"
                  :owner-uuid "" :volume 1.0 :pitch 1.0 :loop? false}]]
               @bridge-calls*))
        ;; The charged cue is `isLocal`-only upstream; this payload carries no
        ;; source player, so no client is the caster and nothing is queued.
        (is (= 0 (count @sound-calls*)))
        (is (= 10 (count @particle-calls*)))
        (is (= 1 (count (filter #(= :plasma-body (:kind %)) (:ops plan)))))
        ;; The charge tornado is dead once the shot is in flight, but still
        ;; fading out (10 of its 30 dead ticks elapsed), so it still renders.
        (is (seq (filter #(= tornado/ring-texture (:texture %)) (:ops plan))))
        (is (= 10 (get-in (pcfx/fx-snapshot)
                          [:effect-state [:ctx "ctx-main"] :ticks]))))
      (reset! sound-calls* [])
      (reset! particle-calls* [])
      (reset! bridge-calls* [])
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-main" :plasma-cannon/fx-update {:mode :perform :pos {:x 2.0 :y 65.0 :z 2.0}})
      ;; The server-side Explosion already plays vanilla's explosion sound —
      ;; queuing another here doubled it up.
      (is (= 0 (count @sound-calls*)))
      (is (= 13 (count @particle-calls*))))))

(deftest charge-tornado-renders-while-charging-test
  ;; The original spawns a Tornado entity in c_begin and only kills it when the
  ;; shot goes (STATE_GO); the port rendered nothing at all during the charge.
  (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "plasma-cannon-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :plasma-cannon "ctx-main" :plasma-cannon/fx-start
      {:mode :start
       :charge-pos {:x 1.0 :y 79.0 :z 1.0}
       :tornado-base {:x 1.0 :y 64.0 :z 1.0}})
    (arc-beam/enqueue-for-test! :plasma-cannon "ctx-main" :plasma-cannon/fx-update
      {:mode :update :state :charging :charge-ticks 5
       :charge-pos {:x 1.0 :y 79.0 :z 1.0}})
    (dotimes [_ 10]
      (level-effects/update-effect-state! :plasma-cannon
        (fn [store] (arc-beam/effect-tick-state! :level :plasma-cannon store))))
    (let [rings (filter #(= tornado/ring-texture (:texture %))
                        (:ops (arc-beam/effect-build-plan :plasma-cannon nil nil 0)))]
      (is (seq rings) "tornado ring quads are part of the charge plan")
      ;; Every ring segment covers 1/20 of the texture (TornadoRenderer div).
      (is (= #{50000} (into #{} (map (fn [{:keys [u0 u1]}] (Math/round (* 1.0e6 (- u1 u0))))) rings)))
      ;; Tornado.alpha at tick 10 = 10/20, halved in onUpdate, then *0.7 at
      ;; render time.
      (is (= #{(int (* 255.0 0.5 0.5 0.7))} (into #{} (map (comp :a :color)) rings)))
      ;; The column stands on the ground point from the start payload, not at
      ;; the plasma body 15 blocks up.
      (is (every? (fn [op]
                    (every? (fn [^cn.li.mcmod.math.V3 p] (< (.-y p) 78.0))
                            [(:p0 op) (:p1 op) (:p2 op) (:p3 op)]))
                  rings)))))

;; ---------------------------------------------------------------------------
;; PlasmaBodyEffect parity: cluster size, alpha curve, death fade, prediction
;; ---------------------------------------------------------------------------

(def ^:private rendered-charge-pos @#'pcimpl/rendered-charge-pos)

(defn- with-fx-stubs [f]
  (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "plasma-cannon-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (f)))

(defn- tick-fx! [n]
  (dotimes [_ n]
    (level-effects/update-effect-state! :plasma-cannon
      (fn [store] (arc-beam/effect-tick-state! :level :plasma-cannon store)))))

(defn- start! [ctx-id charge-pos]
  (arc-beam/enqueue-for-test! :plasma-cannon ctx-id :plasma-cannon/fx-start
    {:mode :start :charge-pos charge-pos :tornado-base {:x 0.0 :y 0.0 :z 0.0}}))

(defn- charge-x
  "The authoritative (tick-resolution) position, before render interpolation."
  [ctx-id]
  (get-in (pcfx/fx-snapshot) [:effect-state [:ctx ctx-id] :charge-pos :x]))

(defn- plasma-op []
  (first (filter #(= :plasma-body (:kind %))
                 (:ops (arc-beam/effect-build-plan :plasma-cannon nil nil 0)))))

(deftest plasma-cluster-matches-upstream-ball-spread-test
  ;; PlasmaBodyEffect's constructor: 4 core balls (size 1..1.5, centre ±1.5,
  ;; orbit amplitude 1.4..2) plus 4-5 satellites (size 0.1..0.3, centre ±3,
  ;; amplitude ×2.5). The hand-rolled 6-ball cluster it replaced spanned barely
  ;; a block, an order of magnitude smaller than the 22-block billboard it is
  ;; drawn in.
  (with-fx-stubs
    (fn []
      (start! "ctx-cluster" {:x 0.0 :y 64.0 :z 0.0})
      (tick-fx! 60)
      (let [{:keys [balls]} (plasma-op)
            sizes (map :size balls)
            offsets (map (fn [b] (Math/sqrt (+ (* (:x b) (:x b))
                                               (* (- (:y b) 64.0) (- (:y b) 64.0))
                                               (* (:z b) (:z b)))))
                         balls)]
        (is (<= 8 (count balls) 9) "4 core + rangei(4,6) satellites")
        (is (<= (count balls) 16) "the shader packs at most 16 balls")
        (is (<= 0.1 (apply min sizes)))
        (is (<= (apply max sizes) 1.5))
        (is (> (apply max offsets) 2.0)
            "the cloud is metres across, not a single tight blob")))))

(deftest plasma-alpha-follows-the-upstream-fade-curve-test
  ;; updateAlpha: moveTowards(alpha, 1, dt*0.3) — 0 to 1 over ~3.3s of real
  ;; time — and the renderer's uniform is that value SQUARED.
  (with-fx-stubs
    (fn []
      (start! "ctx-alpha" {:x 0.0 :y 64.0 :z 0.0})
      (is (nil? (plasma-op)) "alpha starts at 0, so nothing is drawn yet")
      (tick-fx! 20)                                   ;; 1s at 0.3/s -> 0.3
      (let [a (:alpha (plasma-op))]
        (is (< 0.05 a 0.12) "0.3^2 after one second"))
      (tick-fx! 60)                                   ;; 4s in total -> clamped 1
      (is (< 0.99 (:alpha (plasma-op)) 1.01)))))

(deftest both-halves-fade-out-after-termination-test
  ;; The body fades to 0 at 1.0/s and only then setDead; the tornado fades for
  ;; 20 ticks and is removed at 30. The state used to be wiped on :end, so
  ;; everything vanished in the same frame.
  (with-fx-stubs
    (fn []
      (start! "ctx-fade" {:x 0.0 :y 64.0 :z 0.0})
      (tick-fx! 80)
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-fade" :plasma-cannon/fx-update
        {:mode :end :performed? false})
      (tick-fx! 5)
      (let [op (plasma-op)]
        (is (some? op) "still drawing while it fades")
        (is (< (:alpha op) 1.0)))
      (is (seq (filter #(= tornado/ring-texture (:texture %))
                       (:ops (arc-beam/effect-build-plan :plasma-cannon nil nil 0))))
          "the tornado fades too, instead of disappearing with the context")
      (tick-fx! 30)
      (is (nil? (plasma-op)))
      (is (nil? (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-fade"]]))
          "and the owner is dropped once both have played out"))))

(deftest flight-is-predicted-between-position-syncs-test
  ;; c_tick runs the same tryMove as the server (1 block/tick) instead of
  ;; waiting for the 5-tick sync, which otherwise left the body a quarter
  ;; second behind the explosion.
  (with-fx-stubs
    (fn []
      (start! "ctx-fly" {:x 0.0 :y 64.0 :z 0.0})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-fly" :plasma-cannon/fx-update
        {:mode :update :state :go
         :charge-pos {:x 0.0 :y 64.0 :z 0.0}
         :destination {:x 40.0 :y 64.0 :z 0.0}})
      (tick-fx! 3)
      (is (= 3.0 (charge-x "ctx-fly")) "three ticks, three blocks")
      ;; The flight sync carries :charge-pos and :flight-ticks only. Defaulting
      ;; the fields it omits reset :state to :charging, which switched the
      ;; prediction off — the body then only moved when a sync landed, five
      ;; blocks at a time.
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-fly" :plasma-cannon/fx-update
        {:mode :update :charge-pos {:x 5.0 :y 64.0 :z 0.0} :flight-ticks 5})
      (is (= :go (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-fly"] :state]))
          "a partial sync must not knock the state back to :charging")
      (is (= {:x 40.0 :y 64.0 :z 0.0}
             (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-fly"] :destination]))
          "nor forget where the shot is headed")
      (tick-fx! 2)
      (is (< 5.0 (charge-x "ctx-fly")) "prediction keeps running after a sync"))))

(deftest a-sync-is-absorbed-gradually-not-snapped-test
  ;; Both sides walk the same deterministic line, but the client only starts
  ;; when the fire message lands, so it runs a constant latency behind. Snapping
  ;; that gap shut on every sync is what the flight stuttered on: forward lurch,
  ;; then several frames of walking backwards, four times a second.
  (with-fx-stubs
    (fn []
      (start! "ctx-recon" {:x 0.0 :y 64.0 :z 0.0})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-recon" :plasma-cannon/fx-update
        {:mode :update :state :go
         :charge-pos {:x 0.0 :y 64.0 :z 0.0}
         :destination {:x 40.0 :y 64.0 :z 0.0}})
      (tick-fx! 3)
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-recon" :plasma-cannon/fx-update
        {:mode :update :charge-pos {:x 5.0 :y 64.0 :z 0.0} :flight-ticks 5})
      (is (= 3.0 (charge-x "ctx-recon")) "the sync does not move the body itself")
      (let [xs (vec (for [_ (range 8)] (do (tick-fx! 1) (charge-x "ctx-recon"))))
            steps (mapv - xs (cons 3.0 (butlast xs)))]
        (is (every? (fn [d] (<= 1.0 d 1.1500001)) steps)
            (str "each tick is one block plus a sliver of correction: " steps))
        (is (< (last xs) 12.3) "the correction is spread out, never a jump"))))

  (testing "the ±1 tick of message jitter is not an error worth chasing"
    ;; Client and server advance the same block per tick, and the client starts
    ;; exactly as late as the sync it receives is stale, so in steady state they
    ;; agree to within a tick. Correcting that jitter surged the speed 25% every
    ;; 250ms — periodic, always forward, and the last of the stutter.
    (with-fx-stubs
      (fn []
        (start! "ctx-jitter" {:x 0.0 :y 64.0 :z 0.0})
        (arc-beam/enqueue-for-test! :plasma-cannon "ctx-jitter" :plasma-cannon/fx-update
          {:mode :update :state :go
           :charge-pos {:x 0.0 :y 64.0 :z 0.0}
           :destination {:x 100.0 :y 64.0 :z 0.0}})
        (let [xs (atom [])]
          (doseq [tick (range 12)]
            (tick-fx! 1)
            ;; a sync every 5 ticks, one tick's worth of jitter each way
            (when (zero? (mod (inc tick) 5))
              (arc-beam/enqueue-for-test! :plasma-cannon "ctx-jitter" :plasma-cannon/fx-update
                {:mode :update :flight-ticks (inc tick)
                 :charge-pos {:x (double (+ (inc tick) (if (zero? (mod tick 2)) 1 -1)))
                              :y 64.0 :z 0.0}}))
            (swap! xs conj (charge-x "ctx-jitter")))
          (let [steps (mapv - @xs (cons 0.0 (butlast @xs)))]
            (is (every? (fn [d] (= 1.0 d)) steps)
                (str "constant speed, every tick: " steps)))))))

  (testing "a real desync snaps rather than crawling"
    (with-fx-stubs
      (fn []
        (start! "ctx-desync" {:x 0.0 :y 64.0 :z 0.0})
        (arc-beam/enqueue-for-test! :plasma-cannon "ctx-desync" :plasma-cannon/fx-update
          {:mode :update :state :go
           :charge-pos {:x 0.0 :y 64.0 :z 0.0}
           :destination {:x 100.0 :y 64.0 :z 0.0}})
        (tick-fx! 2)
        (arc-beam/enqueue-for-test! :plasma-cannon "ctx-desync" :plasma-cannon/fx-update
          {:mode :update :charge-pos {:x 50.0 :y 64.0 :z 0.0} :flight-ticks 50})
        (is (= 50.0 (charge-x "ctx-desync")))))))

(deftest flight-never-moves-backwards-across-syncs-test
  ;; End-to-end shape check at render resolution: 15 ticks with a sync every
  ;; 5th, sampled three times per tick. Before reconciliation this produced
  ;; -0.64 frame deltas right after each sync.
  (with-fx-stubs
    (fn []
      (start! "ctx-shape" {:x 0.0 :y 64.0 :z 0.0})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-shape" :plasma-cannon/fx-update
        {:mode :update :state :go
         :charge-pos {:x 0.0 :y 64.0 :z 0.0}
         :destination {:x 100.0 :y 64.0 :z 0.0}})
      (let [samples (atom [])]
        (doseq [tick (range 15)]
          (tick-fx! 1)
          ;; the server is two ticks ahead of what the client predicted
          (when (zero? (mod (inc tick) 5))
            (arc-beam/enqueue-for-test! :plasma-cannon "ctx-shape" :plasma-cannon/fx-update
              {:mode :update :charge-pos {:x (double (+ tick 3)) :y 64.0 :z 0.0}
               :flight-ticks (inc tick)}))
          (let [st (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-shape"]])
                moved (long (or (:moved-ms st) 0))]
            (doseq [frame (range 3)]
              (swap! samples conj (:x (rendered-charge-pos st (+ moved (* 16 frame))))))))
        (let [deltas (mapv - (rest @samples) @samples)]
          (is (every? (fn [d] (<= 0.0 d 0.5)) deltas)
              (str "no backward or oversized frame steps: " (vec deltas))))))))

(deftest render-position-is-interpolated-across-the-tick-test
  ;; Vanilla draws an entity at lastTickPos + (pos - lastTickPos) * partialTick.
  ;; Without that the body teleports a whole block 20 times a second, which
  ;; reads as a stutter — the level-effect plan only gets whole game ticks, so
  ;; the elapsed wall clock since the move stands in for the partial tick.
  (let [prev {:x 0.0 :y 64.0 :z 0.0}
        cur {:x 1.0 :y 64.0 :z 0.0}
        at (fn [ms-ago]
             (:x (rendered-charge-pos {:prev-charge-pos prev :charge-pos cur
                                       :moved-ms (- (System/currentTimeMillis) ms-ago)})))]
    (is (< (at 0) 0.1) "a fresh move still draws at the old position")
    (is (< 0.4 (at 25) 0.7) "halfway through the tick, halfway along")
    (is (= 1.0 (at 200)) "and never runs past the authoritative position")
    (is (= cur (rendered-charge-pos {:charge-pos cur}))
        "nothing to interpolate from before the first move")))

(deftest charged-cue-is-caster-only-test
  ;; l_tick guards the cue with `if (isLocal)`: every nearby client runs the
  ;; context, but only the caster's own client plays the sound.
  (let [sounds* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "plasma-cannon-test"})
                  client-sounds/queue-sound-effect! (fn [& args] (swap! sounds* conj args) nil)
                  client-particles/queue-particle-effect! (fn [& _] nil)
                  client-bridge/run-client-effect! (fn [& _] nil)
                  client-bridge/local-player-uuid (fn [] "caster-1")]
      (start! "ctx-cue" {:x 0.0 :y 64.0 :z 0.0})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-cue" :plasma-cannon/fx-update
        {:mode :update :fully-charged? true :source-player-id "someone-else"})
      (is (= 0 (count @sounds*)) "a bystander's client stays quiet")
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-cue" :plasma-cannon/fx-update
        {:mode :update :fully-charged? true :source-player-id "caster-1"})
      (is (= 1 (count @sounds*))))))

(deftest slot-shows-charge-then-active-test
  ;; PlasmaCannonContext implements IStateProvider: CHARGE while
  ;; localTicker < chargeTime, ACTIVE after. Nothing in this port ever puts a
  ;; context into the :charge input-state, so the slot needs this override to
  ;; show the golden charge glow at all.
  (with-fx-stubs
    (fn []
      (is (nil? (pcimpl/charge-visual-state "caster-1")) "no context, no override")
      (start! "ctx-state" {:x 0.0 :y 64.0 :z 0.0})
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-state" :plasma-cannon/fx-update
        {:mode :update :state :charging :charge-ticks 5 :source-player-id "caster-1"})
      (is (= :charge (pcimpl/charge-visual-state "caster-1")))
      (is (nil? (pcimpl/charge-visual-state "someone-else")))
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-state" :plasma-cannon/fx-update
        {:mode :update :state :charging :charge-ticks 60 :release-ready? true
         :source-player-id "caster-1"})
      (is (= :active (pcimpl/charge-visual-state "caster-1")))
      (arc-beam/enqueue-for-test! :plasma-cannon "ctx-state" :plasma-cannon/fx-update
        {:mode :end :performed? true})
      (is (nil? (pcimpl/charge-visual-state "caster-1")) "terminated contexts stop overriding"))))

(deftest flight-survives-the-real-handler-path-test
  ;; The tests above enqueue payloads straight into the effect, which skips the
  ;; fx spec's own projection — and that projection was the bug: it rebuilt
  ;; every field with a default, stamping :state :charging onto the 5-tick
  ;; flight sync (which carries only :charge-pos/:flight-ticks). The client fell
  ;; out of :go, stopped predicting, and moved five blocks per sync. Drive the
  ;; registered handlers here so the projection is part of the test.
  (let [handlers* (atom {})]
    (with-redefs [fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)]
      (pcfx/init!))
    (with-fx-stubs
      (fn []
        (let [fire! (get @handlers* :plasma-cannon/fx-update)]
          ((get @handlers* :plasma-cannon/fx-start) "ctx-real" :plasma-cannon/fx-start
           {:charge-pos {:x 0.0 :y 64.0 :z 0.0} :tornado-base {:x 0.0 :y 0.0 :z 0.0}
            :player-id "caster"})
          ;; key-up: the shot is fired
          (fire! "ctx-real" :plasma-cannon/fx-update
                 {:state :go :charge-pos {:x 0.0 :y 64.0 :z 0.0}
                  :destination {:x 40.0 :y 64.0 :z 0.0} :player-id "caster"})
          (tick-fx! 3)
          (is (= :go (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-real"] :state])))
          (is (= 3.0 (charge-x "ctx-real")) "prediction runs after the fire message")
          ;; ...and the 5-tick position sync, exactly as tick-flight! sends it
          (fire! "ctx-real" :plasma-cannon/fx-update
                 {:charge-pos {:x 5.0 :y 64.0 :z 0.0} :flight-ticks 5 :player-id "caster"})
          (is (= :go (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-real"] :state]))
              "a sync must not knock the client out of flight")
          (is (= {:x 40.0 :y 64.0 :z 0.0}
                 (get-in (pcfx/fx-snapshot) [:effect-state [:ctx "ctx-real"] :destination])))
          (tick-fx! 2)
          ;; two ticks of prediction plus the bounded correction for the two
          ;; blocks the client was behind — not a five-block jump on the sync.
          (is (< 5.2 (charge-x "ctx-real") 5.4)))))))

(ns cn.li.ac.content.ability.electromaster.railgun-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.arc-fx]
            [cn.li.ac.ability.client.effects.rv3]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.content.ability.electromaster.railgun-fx :as railgun-fx]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (railgun-fx/reset-fx-for-test!)
        (railgun-fx/reset-charge-glows-for-test!)
        (f)
        (finally
          (railgun-fx/reset-fx-for-test!)
          (railgun-fx/reset-charge-glows-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-railgun-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (railgun-fx/init!)
      (is (= :railgun-shot (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:railgun/fx-shot :railgun/fx-reflect
               :railgun/fx-charge-start :railgun/fx-charge-update :railgun/fx-charge-end}
             @registered-topics*)))))

(deftest fx-handler-routes-with-ctx-metadata-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (railgun-fx/init!)
      ((get @handlers* :railgun/fx-shot) "ctx-rail" :railgun/fx-shot {:mode :block-hit
                                               :start {:x 0.0 :y 64.0 :z 0.0}
                                               :end {:x 8.0 :y 64.0 :z 0.0}
                                               :world-id "minecraft:overworld"})
      (is (= [[:railgun-shot
               "ctx-rail"
               :railgun/fx-shot
               {:mode :block-hit
                :start {:x 0.0 :y 64.0 :z 0.0}
                :end {:x 8.0 :y 64.0 :z 0.0}
                :world-id "minecraft:overworld"}
               '(:owner-key [:ctx "ctx-rail"])]]
             @enqueued*)))))

(deftest enqueue-perform-adds-beam-and-builds-plan-test
  (do
    (arc-beam/enqueue-for-test! :railgun-shot "ctx-main" :railgun/fx-shot {:start {:x 0.0 :y 64.0 :z 0.0}
                                           :end {:x 3.0 :y 64.0 :z 3.0}
                                           :hit-distance 18.0})
    ;; Upstream beam length blends from zero over its first 150 ms.
    (level-effects/update-effect-state! :railgun-shot
      (fn [store] (arc-beam/effect-tick-state! :level :railgun-shot store)))
    (let [plan (arc-beam/effect-build-plan :railgun-shot {:x 0.0 :y 65.0 :z 0.0} nil 0)]
      (is (some? plan))
      (is (seq (:ops plan)))
      (is (= 13 (count (filter #(= :line (:kind %)) (:ops plan))))
          "enhanced center highlight plus 12-segment endpoint ring remain layered on the beam"))
    (is (= 1 (count (get (:beam-effects (railgun-fx/fx-snapshot)) [:ctx "ctx-main"]))))))

(deftest two-owners-keep-railgun-beams-independent-test
  (do
    (arc-beam/enqueue-for-test! :railgun-shot "ctx-a" :railgun/fx-shot {:start {:x 0.0 :y 64.0 :z 0.0}
                                         :end {:x 6.0 :y 64.0 :z 0.0}})
    (arc-beam/enqueue-for-test! :railgun-shot "ctx-b" :railgun/fx-reflect {:start {:x 0.0 :y 65.0 :z 0.0}
                                           :end {:x 6.0 :y 65.0 :z 0.0}})
    (let [snapshot (railgun-fx/fx-snapshot)]
      (is (= 1 (count (get (:beam-effects snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:beam-effects snapshot) [:ctx "ctx-b"]))))
      (railgun-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (railgun-fx/fx-snapshot)]
        ;; Clearing an owner leaves its live beams alone — they are one-shot
        ;; world visuals that expire on their own ttl (see the regression test
        ;; below) — and never touches another owner's.
        (is (= 1 (count (get (:beam-effects after-clear) [:ctx "ctx-a"]))))
        (is (= 1 (count (get (:beam-effects after-clear) [:ctx "ctx-b"]))))))))

(deftest fired-beam-outlives-its-context-test
  ;; Railgun's context ends on the same tick the shot goes out (the charge
  ;; window closes), and client_ui_hooks' MSG-CTX-TERMINATED handler calls
  ;; level-effects/clear-effect-owner!. Upstream's EntityRailgunFX is a world
  ;; entity with its own ~2.5 s life that the ability context never kills, so
  ;; the beam has to survive that: clearing the owner used to delete it a tick
  ;; or two after firing, and the shot rendered nothing at all.
  (arc-beam/enqueue-for-test! :railgun-shot "ctx-fire" :railgun/fx-shot
    {:start {:x 0.0 :y 64.0 :z 0.0}
     :end {:x 0.0 :y 64.0 :z 30.0}
     :hit-distance 30.0})
  (railgun-fx/clear-fx-owner! [:ctx "ctx-fire"])
  (is (= 1 (count (get (:beam-effects (railgun-fx/fx-snapshot)) [:ctx "ctx-fire"])))
      "a fired beam survives its context ending")
  (is (seq (:ops (arc-beam/effect-build-plan :railgun-shot {:x 0.0 :y 65.0 :z 0.0} nil 0)))
      "and still renders")
  ;; It is the ttl, not the context, that ends it.
  (dotimes [_ 51]
    (level-effects/update-effect-state! :railgun-shot
      (fn [store] (arc-beam/effect-tick-state! :level :railgun-shot store))))
  (is (empty? (get (:beam-effects (railgun-fx/fx-snapshot)) [:ctx "ctx-fire"]))
      "and expires on its own ttl"))

(deftest clear-owner-still-stops-the-charge-marker-test
  ;; The charge marker is context-bound: it exists to keep the effect non-idle
  ;; while charging, so an externally aborted context must still stop it.
  (arc-beam/enqueue-for-test! :railgun-shot "ctx-charge" :railgun/fx-charge-start
    {:mode :charge-start :source-player-id "player-a"})
  (is (some? (get (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-charge"])))
  (railgun-fx/clear-fx-owner! [:ctx "ctx-charge"])
  (is (nil? (get (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-charge"]))))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:beam-effects {} :charging {}}
         (railgun-fx/fx-snapshot))))

(deftest charging-lifecycle-keeps-one-shot-animation-after-charge-end-test
  ;; :charging is a pure idle-gating marker (see impl/railgun_shot.clj):
  ;; before this fix, no level-effect state existed during the charge-only
  ;; window (no beam yet), so level-effects' idle-gating suppressed build-plan
  ;; entirely and the charge-hand animation never rendered until a beam
  ;; existed. default-empty-state? only treats :railgun-shot as idle once
  ;; BOTH :beam-effects and :charging are empty.
  (do
    (is (empty? (:charging (railgun-fx/fx-snapshot))))
    (arc-beam/enqueue-for-test! :railgun-shot "ctx-charge" :railgun/fx-charge-start {:mode :charge-start})
    (is (contains? (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-charge"]))

    (arc-beam/enqueue-for-test! :railgun-shot "ctx-charge" :railgun/fx-charge-update {:mode :charge-update :charge-ticks-left 5})
    (is (contains? (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-charge"]))

    (arc-beam/enqueue-for-test! :railgun-shot "ctx-charge" :railgun/fx-charge-end {:mode :charge-end})
    (is (contains? (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-charge"])
        "upstream RailgunHandEffect continues its full 1.6-second animation")))

(deftest charge-hand-visual-renders-once-charging-marker-is-live-test
  ;; RailgunHandEffect has two branches and upstream picks exactly one: the
  ;; scaled hand quad in first person, the 2x2 billboard on the player model
  ;; otherwise. The railgun_charge entity is this port's second branch, and it
  ;; is spawned on every client including the caster's — so the hand quad has
  ;; to stay out of third person or the caster plays both at once.
  (let [hand {:x 1.0 :y 65.0 :z 1.0 :player-uuid "p1" :first-person? true
              :player-yaw-rad 0.0 :player-pitch-rad 0.0}]
    (arc-beam/enqueue-for-test!
      :railgun-shot "ctx-charge" :railgun/fx-charge-start
      {:mode :charge-start :source-player-id "p1"})
    (let [plan (arc-beam/effect-build-plan
                 :railgun-shot {:x 0.0 :y 65.0 :z 0.0} hand 0)]
      (is (seq (:ops plan)))
      (is (every? #(= :quad (:kind %)) (:ops plan))
          "no beam exists yet — the only ops should be the charge-hand quad"))
    (let [plan (arc-beam/effect-build-plan
                 :railgun-shot {:x 0.0 :y 65.0 :z 0.0}
                 (assoc hand :first-person? false) 0)]
      (is (nil? (:ops plan))
          "in third person the entity-anchored billboard is the only animation"))))

(deftest charge-start-spawns-glow-anchored-to-caster-test
  (let [handlers* (atom {})
        run-calls* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  client-bridge/run-client-effect! (fn [op payload]
                                                      (swap! run-calls* conj [op payload])
                                                      (when (= op :mcmod/spawn-scripted-effect-at-player)
                                                        "entity-uuid-1"))]
      (railgun-fx/init!)
      ((get @handlers* :railgun/fx-charge-start) "ctx-glow" :railgun/fx-charge-start
       {:mode :charge-start :source-player-id "caster-uuid"})
      (is (= [[:mcmod/spawn-scripted-effect-at-player
               {:effect-id "railgun_charge" :owner-uuid "caster-uuid"}]]
             @run-calls*)
          "spawns anchored to the CASTER's uuid from the payload, not the local viewer"))))

(deftest charge-start-without-source-player-id-does-not-spawn-test
  (let [handlers* (atom {})
        run-calls* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  client-bridge/run-client-effect! (fn [op payload]
                                                      (swap! run-calls* conj [op payload])
                                                      nil)]
      (railgun-fx/init!)
      ((get @handlers* :railgun/fx-charge-start) "ctx-glow" :railgun/fx-charge-start
       {:mode :charge-start})
      (is (empty? @run-calls*)))))

(deftest charge-end-clears-enhanced-world-glow-without-affecting-hand-animation-test
  (let [handlers* (atom {})
        run-calls* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  client-bridge/run-client-effect! (fn [op payload]
                                                      (swap! run-calls* conj [op payload])
                                                      (when (= op :mcmod/spawn-scripted-effect-at-player)
                                                        "entity-uuid-2"))]
      (railgun-fx/init!)
      ((get @handlers* :railgun/fx-charge-start) "ctx-glow" :railgun/fx-charge-start
       {:mode :charge-start :source-player-id "caster-uuid"})
      ((get @handlers* :railgun/fx-charge-end) "ctx-glow" :railgun/fx-charge-end
       {:mode :charge-end :source-player-id "caster-uuid"})
      (is (= [:mcmod/spawn-scripted-effect-at-player
              :mcmod/remove-local-scripted-effect]
             (mapv first @run-calls*)))
      (is (= [:mcmod/remove-local-scripted-effect
              {:entity-uuid "entity-uuid-2"}]
             (last @run-calls*))))))

(deftest charge-end-without-a-prior-start-is-a-no-op-test
  ;; Matches railgun.clj's abort handler comment: fx-charge-end fires
  ;; unconditionally on abort even when no charge was ever active.
  (let [handlers* (atom {})
        run-calls* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  client-bridge/run-client-effect! (fn [op payload]
                                                      (swap! run-calls* conj [op payload])
                                                      nil)]
      (railgun-fx/init!)
      ((get @handlers* :railgun/fx-charge-end) "ctx-never-started" :railgun/fx-charge-end
       {:mode :charge-end :source-player-id "caster-uuid"})
      (is (empty? @run-calls*)))))

(deftest charging-marker-expires-via-ttl-without-explicit-end-test
  (do
    (arc-beam/enqueue-for-test! :railgun-shot "ctx-stale" :railgun/fx-charge-start {:mode :charge-start})
    (is (contains? (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-stale"]))
    (dotimes [_ 32]
      (level-effects/update-effect-state! :railgun-shot
        (fn [store] (arc-beam/effect-tick-state! :level :railgun-shot store))))
    (is (not (contains? (:charging (railgun-fx/fx-snapshot)) [:ctx "ctx-stale"])))))

;; ---------------------------------------------------------------------------
;; EntityRailgunFX geometry + SubArc lifecycle parity
;; ---------------------------------------------------------------------------

(defn- fire-beam! [ctx-id length]
  (arc-beam/enqueue-for-test! :railgun-shot ctx-id :railgun/fx-shot
    {:mode :perform
     :start {:x 0.0 :y 64.0 :z 0.0}
     :end {:x (double length) :y 64.0 :z 0.0}
     :hit-distance (double length)}))

(defn- tick-fx! [n]
  (dotimes [_ n]
    (level-effects/update-effect-state! :railgun-shot
      (fn [store] (arc-beam/effect-tick-state! :level :railgun-shot store)))))

(defn- beam-state [ctx-id]
  (first (get (:beam-effects (railgun-fx/fx-snapshot)) [:ctx ctx-id])))

(defn- edge-width
  "Distance between a quad's two leading corners — the strip's full width at
  that point, whatever axis the camera-facing basis put it on."
  [op]
  (let [^cn.li.mcmod.math.V3 a (:p0 op)
        ^cn.li.mcmod.math.V3 b (:p1 op)]
    (Math/sqrt (+ (Math/pow (- (.-x a) (.-x b)) 2)
                  (Math/pow (- (.-y a) (.-y b)) 2)
                  (Math/pow (- (.-z a) (.-z b)) 2)))))

(defn- axis-radius
  "Distance from a vertex to the beam axis. Test beams run along +x at
  y = 64, z = 0, so this is the tube's radius at that vertex."
  [op key]
  (let [^cn.li.mcmod.math.V3 p (get op key)]
    (Math/sqrt (+ (Math/pow (- (.-y p) 64.0) 2)
                  (Math/pow (.-z p) 2)))))

(defn- quad-extent
  "Largest distance between any two quad corners in the op list."
  [ops]
  (let [pts (mapcat (fn [op] (keep op [:p0 :p1 :p2 :p3])) ops)]
    (if (< (count pts) 2)
      0.0
      (apply max
             (for [^cn.li.mcmod.math.V3 a pts
                   ^cn.li.mcmod.math.V3 b pts]
               (let [dx (- (.-x a) (.-x b))
                     dy (- (.-y a) (.-y b))
                     dz (- (.-z a) (.-z b))]
                 (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))))))))

(deftest sub-arcs-are-drawn-at-upstream-scale-test
  ;; SubArcHandler.drawAll scales every template by 0.3 and centres it on its
  ;; placement point. Without the scale a 2-3 unit template covered 2-3 blocks
  ;; instead of 0.6-0.9, more than three times the original.
  (fire-beam! "ctx-arc" 20.0)
  ;; sub-arcs start hidden and flicker on, so give them a few ticks
  (tick-fx! 6)
  (let [beam (beam-state "ctx-arc")
        placement (first (filter :draw? (:arc-placements beam)))]
    (when placement
      (let [ops (cn.li.ac.ability.client.effects.arc-fx/railgun-arc-ops
                  (cn.li.ac.ability.client.effects.rv3/v3 0.0 70.0 0.0)
                  (assoc beam :arc-placements [placement])
                  {})
            centre (cn.li.ac.ability.client.effects.rv3/v3
                     (double (:distance placement)) 64.0 0.0)
            extent (quad-extent ops)
            offsets (map (fn [op]
                           (let [^cn.li.mcmod.math.V3 p (:p0 op)]
                             (Math/sqrt (+ (Math/pow (- (.-x p) (.-x centre)) 2)
                                           (Math/pow (- (.-y p) (.-y centre)) 2)
                                           (Math/pow (- (.-z p) (.-z centre)) 2)))))
                         ops)]
        (is (seq ops))
        (is (< extent 1.2)
            (str "a 2-3 unit template at 0.3 scale spans under a block, was " extent))
        (is (< (apply max offsets) 0.8)
            "and it straddles its placement point instead of trailing off it")))))

(deftest sub-arcs-flicker-and-expire-like-upstream-test
  (fire-beam! "ctx-life" 20.0)
  (let [initial (:arc-placements (beam-state "ctx-life"))]
    (is (seq initial))
    (is (every? (comp false? :draw?) initial)
        "SubArc.draw starts false — an arc flickers on before it is ever seen")
    (is (every? #(and (<= 0 (:tex-id %)) (< (:tex-id %) 15)) initial))
    ;; over 30 ticks the visibility toggles rather than staying constant
    (let [seen (atom #{})
          tex-ids (atom #{})]
      (dotimes [_ 25]
        (tick-fx! 1)
        (let [arc (first (:arc-placements (beam-state "ctx-life")))]
          (swap! seen conj (boolean (:draw? arc)))
          (swap! tex-ids conj (:tex-id arc))))
      (is (= #{true false} @seen) "a single arc blinks on and off")
      (is (> (count @tex-ids) 1) "and cycles through templates as it goes"))
    ;; EntityRailgunFX.onUpdate clears the whole handler at age 30
    (tick-fx! 10)
    (is (empty? (:arc-placements (beam-state "ctx-life")))
        "and all of them are gone for the beam's last 20 ticks")
    (is (some? (beam-state "ctx-life")) "while the beam itself is still alive")))

(deftest beam-bore-matches-the-original-cylinders-test
  ;; cylinderOut radius 0.13, cylinderIn 0.09, glow width 1.1 (halved by
  ;; drawBoard). The port had 0.45/0.28/1.5 — three times the original.
  (fire-beam! "ctx-bore" 10.0)
  (tick-fx! 4)
  (let [plan (arc-beam/effect-build-plan
               :railgun-shot {:x 0.0 :y 70.0 :z 0.0} nil 0)
        radii (keep (fn [op] (when (re-find #"effects/solid\.png" (str (:texture op)))
                               (axis-radius op :p0)))
                    (:ops plan))]
    (is (seq radii))
    ;; getWidth() adds a [0, 0.3] wiggle on top of the shrink factor, so the
    ;; bore peaks at 1.3x nominal.
    (is (< (apply max radii) 0.17)
        "the outer cylinder is 0.13 in radius, not the 0.45 the port had")))

(deftest glow-is-three-boards-with-soft-caps-test
  ;; RendererRayGlow.draw lays blend_in over the first `width` units, tile over
  ;; the middle and blend_out over the last `width`, after extending the ray by
  ;; startFix -0.3 / endFix +0.3. The port had a single glow_circle.png
  ;; stretched over the whole ray, so the beam ended in a hard square edge.
  (fire-beam! "ctx-glow" 20.0)
  ;; blendInTime is 150ms, so the ray reaches full length on the 3rd tick
  (tick-fx! 4)
  (let [ops (:ops (arc-beam/effect-build-plan
                    :railgun-shot {:x 0.0 :y 70.0 :z 0.0} nil 0))
        glow (filter #(re-find #"effects/railgun/" (str (:texture %))) ops)
        by-tex (group-by :texture glow)
        tex-of (fn [suffix]
                 (some (fn [[t _]] (when (re-find (re-pattern suffix) (str t)) t))
                       by-tex))
        x-of (fn [op key] (.-x ^cn.li.mcmod.math.V3 (get op key)))]
    (is (= 3 (count glow)) "one board per section")
    (is (every? some? [(tex-of "blend_in") (tex-of "tile") (tex-of "blend_out")]))
    (let [in-board (first (get by-tex (tex-of "blend_in")))
          tile-board (first (get by-tex (tex-of "tile")))
          out-board (first (get by-tex (tex-of "blend_out")))]
      ;; Corners run [start-left, start-right, end-right, end-left] so u runs
      ;; along the beam and v across the width — upstream drawBoard's layout
      ;; (the tile texture tapers across v, which is what softens the board's
      ;; long edges; the axes the other way round left hard edges at the
      ;; sides).
      ;; startFix pulls the ray back behind its origin, endFix past its end
      (is (< (x-of in-board :p0) 0.0) "blend_in starts behind the muzzle (startFix -0.3)")
      (is (> (x-of out-board :p2) 20.0) "blend_out runs past the endpoint (endFix +0.3)")
      ;; the caps are as long as the board is wide, the body covers the rest
      ;; each cap is as long as the board is wide: 1.1, up to 1.3x with wiggle
      (is (< 0.0 (- (x-of in-board :p2) (x-of in-board :p0)) 1.5))
      (is (> (- (x-of tile-board :p2) (x-of tile-board :p0)) 15.0))
      ;; glow.width 1.1, and drawBoard halves it either side of the axis;
      ;; p0/p1 are the two sides at the board's near end
      (let [cross (fn [op]
                    (let [^cn.li.mcmod.math.V3 a (:p0 op)
                          ^cn.li.mcmod.math.V3 d (:p1 op)]
                      (Math/sqrt (+ (Math/pow (- (.-x a) (.-x d)) 2)
                                    (Math/pow (- (.-y a) (.-y d)) 2)
                                    (Math/pow (- (.-z a) (.-z d)) 2)))))]
        (is (< (cross tile-board) 1.5))))))

(deftest ray-tapers-into-a-paraboloid-nose-test
  ;; RendererRayCylinder draws a y = sqrt(x) head at each end of the tube
  ;; (D = 4 segments, headFix 0.98 on the inner one). Flattened to a billboard
  ;; that is a taper: the strip must narrow to nothing at both tips instead of
  ;; stopping at full width.
  (fire-beam! "ctx-nose" 20.0)
  (tick-fx! 4)
  (let [ops (:ops (arc-beam/effect-build-plan
                    :railgun-shot {:x 0.0 :y 70.0 :z 0.0} nil 0))
        beam (filter #(and (= :quad (:kind %))
                           (re-find #"effects/solid\.png" (str (:texture %))))
                     ops)
        xs (fn [op] (.-x ^cn.li.mcmod.math.V3 (:p0 op)))
        radii (sort-by first (map (juxt xs #(axis-radius % :p0)) beam))]
    (is (>= (count beam) 100) "two tubes, each a multi-segment surface")
    (is (< (second (first radii)) 0.01)
        "the tube starts at zero radius — the nose tip")
    (is (< 0.12 (apply max (map second radii)) 0.18)
        "and reaches the 0.13 outer radius in the body (plus the width wiggle)")
    ;; the sqrt profile: a quarter into the nose it is already half the final
    ;; radius, which a straight cone would not be. Both layers taper, so take
    ;; the widest sample in that band.
    (let [nose (filter (fn [[x _]] (< 0.0 x 0.13)) radii)]
      (is (seq nose))
      (is (> (apply max (map second nose)) (* 0.4 0.13))
          "sqrt profile — already ~half the radius a quarter in, not a cone"))))

(deftest ray-is-a-tube-not-a-flat-strip-test
  ;; RendererRayCylinder is a DIV=12 surface of revolution. A flat strip keeps
  ;; its width only from the one angle it is turned to and thins out from any
  ;; other, which is what made the ray read as a flat sliver.
  (fire-beam! "ctx-tube" 20.0)
  (tick-fx! 4)
  (let [ops (:ops (arc-beam/effect-build-plan
                    :railgun-shot {:x 0.0 :y 70.0 :z 0.0} nil 0))
        beam (filter #(and (= :quad (:kind %))
                           (re-find #"effects/solid\.png" (str (:texture %))))
                     ops)
        ;; sample the full-bore vertices and look at where the surface sits
        ;; around the axis: a tube covers every angle, a strip only two
        body (filter (fn [op] (> (axis-radius op :p0) 0.1)) beam)
        angles (into #{}
                     (map (fn [op]
                            (let [^cn.li.mcmod.math.V3 p (:p0 op)]
                              ;; quantise to 45-degree buckets
                              (int (Math/floor (/ (+ Math/PI (Math/atan2 (.-z p) (- (.-y p) 64.0)))
                                                  (/ Math/PI 4.0)))))))
                     body)]
    (is (seq body))
    (is (>= (count angles) 6)
        (str "the surface wraps the axis, buckets hit: " (sort angles)))))

(deftest stranded-glow-bookkeeping-is-pruned-by-age-test
  ;; on-charge-end! normally drops the entry, but that message does not always
  ;; arrive — a caster who disconnects mid-charge never sends one. The entity
  ;; disposes itself either way (life-ticks 32); this is about the map not
  ;; growing for a whole session.
  (let [handlers* (atom {})
        clock (atom 1000000)]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)
                  client-bridge/run-client-effect! (fn [op _]
                                                     (when (= op :mcmod/spawn-scripted-effect-at-player)
                                                       (str "entity-" @clock)))
                  ;; drive the prune clock
                  railgun-fx/now-ms (fn [] @clock)]
      (railgun-fx/init!)
      (let [start! (get @handlers* :railgun/fx-charge-start)]
        (start! "ctx-abandoned" :railgun/fx-charge-start
                {:mode :charge-start :source-player-id "caster"})
        (is (= 1 (count (railgun-fx/active-charge-glows))))
        ;; a second charge well after the first one's entity has expired
        (reset! clock (+ 1000000 5000))
        (start! "ctx-fresh" :railgun/fx-charge-start
                {:mode :charge-start :source-player-id "caster"})
        (is (= ["ctx-fresh"] (keys (railgun-fx/active-charge-glows)))
            "the stranded entry is gone, the live one is kept")))))

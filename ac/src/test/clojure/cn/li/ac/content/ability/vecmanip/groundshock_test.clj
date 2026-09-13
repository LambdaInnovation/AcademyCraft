(ns cn.li.ac.content.ability.vecmanip.groundshock-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.vecmanip.groundshock :as gs]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects])
  (:import [java.util HashSet]))

(deftest horizontal-look-fallback-toggle-test
  (testing "fallback disabled returns nil when no horizontal look vector is available"
    (with-redefs [skill-config/tunable-boolean (fn [_ _] false)
                  raycast/available? (constantly false)]
      (is (nil? (@#'cn.li.ac.content.ability.vecmanip.groundshock/horizontal-look-with-fallback "p1")))))
  (testing "fallback enabled returns +Z direction when no horizontal look vector is available"
    (with-redefs [skill-config/tunable-boolean (fn [_ _] true)
                  raycast/available? (constantly false)]
      (is (= {:x 0.0 :y 0.0 :z 1.0}
             (@#'cn.li.ac.content.ability.vecmanip.groundshock/horizontal-look-with-fallback "p1"))))))

(deftest get-player-position-no-default-fallback-test
  (testing "returns nil when teleportation runtime is unavailable"
    (is (nil? (@#'cn.li.ac.content.ability.vecmanip.groundshock/get-player-position "p1"))))
  (testing "reads position from teleportation protocol when available"
    (with-redefs [motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [impl-player-id]
                                                       {:world-id "w" :x 1.0 :y 2.0 :z 3.0 :player impl-player-id})]
      (is (= {:world-id "w" :x 1.0 :y 2.0 :z 3.0 :player "p1"}
             (@#'cn.li.ac.content.ability.vecmanip.groundshock/get-player-position "p1"))))))

(deftest affect-entities-living-only-test
  (let [damage-calls* (atom [])
        velocity-calls* (atom [])
        exp-calls* (atom [])
        affected* (java.util.HashSet.)]
    (with-redefs [entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [world-id entity-id damage _ _opts]
                                                       (swap! damage-calls* conj [world-id entity-id damage]))
                  motion-effects/entity-motion-available? (constantly true)
                  motion-effects/entity-velocity (fn [& _] {:x 0.25 :y -0.5 :z -0.4})
                  motion-effects/set-entity-velocity! (fn [world-id entity-id vx vy vz]
                                                        (swap! velocity-calls* conj [world-id entity-id vx vy vz]))
                  cn.li.ac.ability.registry.event/fire-calc-event!
                  (fn [_ damage _] damage)
                  skill-effects/scale-damage (fn [_ damage] damage)
                  cn.li.ac.ability.registry.skill/get-skill (fn [_] {})
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp-calls* conj args) nil)
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :combat.entity-search-radius 2.0
                                                  :progression.exp-entity 0.002
                                                  0.0))]
      (@#'cn.li.ac.content.ability.vecmanip.groundshock/affect-entities!
       "player" "w" 0 64 0 5.0 0.8
       [{:uuid "living-1" :living? true :x 0.5 :y 64.0 :z 0.5 :width 0.6 :height 1.8}
        {:uuid "item-1" :living? false :x 0.5 :y 64.0 :z 0.5 :width 0.25 :height 0.25}
        {:uuid "player" :living? true :x 0.5 :y 64.0 :z 0.5 :width 0.6 :height 1.8}]
       affected*)
      (is (= #{"living-1"} (set affected*)))
      (is (= [["w" "living-1" 5.0]] @damage-calls*))
      (is (= [["w" "living-1" 0.25 0.8 -0.4]] @velocity-calls*))
      (is (= 1 (count @exp-calls*))))))

(deftest key-up-missing-position-sends-fx-end-test
  (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context (fn [_] {:skill-state {:charge-ticks 10 :performed? false}})
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 5
                                               0))
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/perform-resource! (fn [& _] {:success? true})
                  motion-effects/teleportation-available? (constantly false)
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  fx/send! send!
                  motion-effects/player-motion-available? (constantly true)
                  motion-effects/player-on-ground? (constantly true)]
      (cb/apply-invoke gs/groundshock-on-key-up :player-id "p1" :ctx-id "ctx-1" :cost-ok? true))
    (is (= [["ctx-1" :groundshock/fx-end :end {:performed? false}]] @calls*))))

(deftest key-up-cost-fail-sends-fx-end-test
  (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context (fn [_] {:skill-state {:charge-ticks 10 :performed? false}})
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 5
                                               0))
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/perform-resource! (fn [& _] {:success? false})
                  fx/send! send!
                  motion-effects/player-motion-available? (constantly true)
                  motion-effects/player-on-ground? (constantly true)]
      (cb/apply-invoke gs/groundshock-on-key-up :player-id "p1" :ctx-id "ctx-2" :cost-ok? false))
    (is (= [["ctx-2" :groundshock/fx-end :end {:performed? false}]] @calls*))))

(deftest key-up-missing-direction-sends-fx-end-test
  (let [{:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context (fn [_] {:skill-state {:charge-ticks 10 :performed? false}})
                  skill-config/tunable-int (fn [_ field-id]
                                             (case field-id
                                               :charge.min-ticks 5
                                               0))
                  skill-config/tunable-boolean (fn [_ _] false)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/perform-resource! (fn [& _] {:success? true})
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [_] {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 1.0 :z 0.0})
                  fx/send! send!
                  motion-effects/player-motion-available? (constantly true)
                  motion-effects/player-on-ground? (constantly true)]
      (cb/apply-invoke gs/groundshock-on-key-up :player-id "p1" :ctx-id "ctx-3" :cost-ok? true))
    (is (= [["ctx-3" :groundshock/fx-end :end {:performed? false}]] @calls*))))

;; ---------------------------------------------------------------------------
;; Upstream block-range alignment (Groundshock.s_perform)
;; ---------------------------------------------------------------------------
;;
;; Vec3d.rotateYaw(90) rotates POSITIVE-yaw: x' = x*cos - z*sin,
;; z' = x*sin + z*cos (90 RADIANS in the original). The old port swapped the
;; two signs (the -90 rotation). floor() is not linear under scaling, so the
;; mirrored +/-2*rot cells landed on DIFFERENT blocks than upstream and the
;; energy drain — and therefore the plowed path length — drifted. These tests
;; pin the upstream geometry.

(deftest rotated-spread-vector-matches-upstream-rotate-yaw-test
  (let [rot (@#'gs/rotated-spread-vector {:x 0.0 :y 0.0 :z 1.0})
        cos90 (Math/cos 90.0)
        sin90 (Math/sin 90.0)
        ref-x (- (* 0.0 cos90) (* 1.0 sin90))
        ref-z (+ (* 0.0 sin90) (* 1.0 cos90))]
    (is (< (Math/abs (- (:x rot) ref-x)) 1.0e-12))
    (is (< (Math/abs (- (:z rot) ref-z)) 1.0e-12))
    (is (= 0.0 (:y rot)) "yaw rotation leaves y unchanged")
    ;; sanity: the mirrored (-90) rotation gives a different x — the regression
    (is (not (< (Math/abs (- (:x rot) (+ (* 0.0 cos90) (* 1.0 sin90)))) 1.0e-9)))))

(deftest propagation-positions-floor-to-upstream-delta-cells-test
  ;; For a pure +Z look, upstream rot = (-0.894, 0, -0.448) so the delta cells
  ;; floor to [0 0 0] (zero), [-1 0 -1] (rot), [0 0 0] (-rot duplicates the
  ;; zero cell), [-2 0 -1] (2*rot), [1 0 0] (-2*rot). The old mirrored port
  ;; produced [0 0 -1] / [-1 0 0] / [1 0 -1] / [-2 0 0] instead.
  (let [spread (@#'gs/rotated-spread-vector {:x 0.0 :y 0.0 :z 1.0})
        cells (mapv (fn [[delta _]]
                      [(int (Math/floor (:x delta)))
                       (int (Math/floor (:y delta)))
                       (int (Math/floor (:z delta)))])
                    (@#'gs/propagation-positions spread))]
    (is (= [[0 0 0] [-1 0 -1] [0 0 0] [-2 0 -1] [1 0 0]] cells))))

;; --- upstream transcription (Plotter.java + Groundshock.s_perform) ----------

(defn- up-rotate-yaw
  [v angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    {:x (- (* (:x v) c) (* (:z v) s))
     :y (:y v)
     :z (+ (* (:x v) s) (* (:z v) c))}))

(defn- up-scaled [v k]
  {:x (* (:x v) k) :y (* (:y v) k) :z (* (:z v) k)})

(defn- up-make-plotter [x0 y0 z0 dx dy dz]
  (let [adx (Math/abs dx) ady (Math/abs dy) adz (Math/abs dz)]
    (cond
      (and (> adz ady) (> adz adx))
      {:axis :z :x0 (int z0) :y0 (int y0) :z0 (int x0)
       :x (int z0) :y (int y0) :z (int x0)
       :dyx (/ (double dy) (double dz)) :dzx (/ (double dx) (double dz))
       :dirflag (if (pos? dz) 1 -1)}
      (pos? adx)
      {:axis :x :x0 (int x0) :y0 (int y0) :z0 (int z0)
       :x (int x0) :y (int y0) :z (int z0)
       :dyx (/ (double dy) (double dx)) :dzx (/ (double dz) (double dx))
       :dirflag (if (pos? dx) 1 -1)}
      :else nil)))

(defn- up-plotter-next
  [plotter]
  (let [{:keys [x0 y0 z0 x y z dyx dzx dirflag axis]} plotter
        next-x (+ x dirflag)
        val-y (+ y0 (* (- next-x x0) dyx))
        val-z (+ z0 (* (- next-x x0) dzx))
        next-state (cond
                     (> (Math/abs (- val-y y)) 0.5)
                     (assoc plotter :y (+ y (* (Math/signum (double dyx)) dirflag)))
                     (> (Math/abs (- val-z z)) 0.5)
                     (assoc plotter :z (+ z (* (Math/signum (double dzx)) dirflag)))
                     :else
                     (assoc plotter :x next-x))
        nx (int (:x next-state)) ny (int (:y next-state)) nz (int (:z next-state))]
    [(case axis :x [nx ny nz] :z [nz ny nx]) next-state]))

(defn- up-break-with-force!
  [world* energy* broken* x y z drop? drop-rate]
  (let [state (get @world* [x y z])
        hardness (if state (case state
                             "minecraft:stone" 1.5
                             "minecraft:grass_block" 0.6
                             "minecraft:dirt" 0.5
                             "minecraft:cobblestone" 2.0
                             1.5) 0.0)]
    (when (and (>= (aget ^doubles energy* 0) hardness)
               (not= state "minecraft:farmland")
               (not= state "minecraft:water")
               (not= state "minecraft:lava"))
      (aset-double ^doubles energy* 0 (- (aget ^doubles energy* 0) hardness))
      (when (and drop? (< (rand) drop-rate))
        nil)
      (swap! world* dissoc [x y z])
      (when state
        (.add ^HashSet broken* [x y z])))))

(defn- up-perform!
  "Transcription of Groundshock.s_perform's block-affecting part."
  [world* p-look px py pz exp]
  (let [energy* (double-array [(+ 60.0 (* 60.0 exp))])
        max-iter (int (+ 10.0 (* 15.0 exp)))
        drop-rate (+ 0.3 (* 0.7 exp))
        _ (rand) ;; upstream ySpeed = RandUtils.rangef(...)
        rot (up-rotate-yaw p-look 90.0)
        deltas [[{:x 0.0 :y 0.0 :z 0.0} 1.0]
                [rot 0.7]
                [(up-scaled rot -1.0) 0.7]
                [(up-scaled rot 2.0) 0.3]
                [(up-scaled rot -2.0) 0.3]]
        plotter (up-make-plotter (int (Math/floor px))
                                 (dec (int (Math/floor py)))
                                 (int (Math/floor pz))
                                 (:x p-look) 0.0 (:z p-look))
        dejavu (HashSet.)
        broken (HashSet.)]
    (loop [iter 0 plotter plotter]
      (if (or (nil? plotter)
              (<= (aget ^doubles energy* 0) 0.0)
              (>= iter max-iter))
        {:dejavu dejavu :broken broken}
        (let [[[x y z] p2] (up-plotter-next plotter)]
          (doseq [[delta prob] deltas]
            (when (< (rand) prob)
              (let [bx (int (Math/floor (+ x (:x delta))))
                    by (int (Math/floor (+ y (:y delta))))
                    bz (int (Math/floor (+ z (:z delta))))
                    pos-key [bx by bz]
                    state (get @world* pos-key)]
                (when (and state (not (.contains dejavu pos-key)))
                  (.add dejavu pos-key)
                  (aset-double ^doubles energy* 0
                               (- (aget ^doubles energy* 0)
                                  (case state
                                    "minecraft:stone" 0.4
                                    "minecraft:grass_block" 0.2
                                    "minecraft:farmland" 0.1
                                    0.5)))
                  (case state
                    "minecraft:stone" (swap! world* assoc pos-key "minecraft:cobblestone")
                    "minecraft:grass_block" (swap! world* assoc pos-key "minecraft:dirt")
                    nil)
                  (when (< (rand) 0.3)
                    (up-break-with-force! world* energy* broken x y z false 0.0)))))
            (doseq [d (range 1 4)]
              (up-break-with-force! world* energy* broken x (+ y d) z false 0.0)))
          (recur (inc iter) p2))))))

(defn- up-mastery-ring!
  [world* broken* px py pz drop-rate exp]
  (when (= 1.0 exp)
    (let [energy* (double-array [Double/MAX_VALUE])
          x0 (int px) y0 (int py) z0 (int pz)]
      (loop [x (- x0 5)]
        (when (< x (+ x0 5))
          (loop [y (- y0 1)]
            (when (< y (+ y0 1))
              (loop [z (- z0 5)]
                (when (< z (+ z0 5))
                  (let [state (get @world* [x y z])
                        hardness (if state
                                   (case state
                                     "minecraft:stone" 1.5
                                     "minecraft:grass_block" 0.6
                                     "minecraft:dirt" 0.5
                                     "minecraft:cobblestone" 2.0
                                     1.5)
                                   0.0)]
                    (when (<= hardness 0.6)
                      (up-break-with-force! world* energy* broken* x y z true drop-rate)))
                  (recur (inc z))))
              (recur (inc y))))
          (recur (inc x)))))))

(defn- build-flat-stone-world []
  (let [w (atom {})]
    (doseq [x (range -30 40)
            z (range -60 30)
            y (range 60 67)]
      (swap! w assoc [x y z] "minecraft:stone"))
    w))

(defn- propagation-mocks
  "with-redefs-fn binding MAP for the platform + config surface that
  propagate-shockwave! / break-mastery-ring! touch (upstream GroundShock
  hardcodes these values)."
  [world*]
  {#'skill-config/lerp-double
   (fn [_ field-id e]
     (case field-id
       :effect.init-energy (+ 60.0 (* 60.0 e))
       :effect.max-iterations (+ 10.0 (* 15.0 e))
       :combat.damage (+ 4.0 (* 2.0 e))
       :movement.launch-scale (+ 0.8 (* 0.5 e))
       :breaking.drop-rate (+ 0.3 (* 0.7 e))
       0.0))
   #'skill-config/tunable-double
   (fn [_ field-id]
     (case field-id
       :effect.energy-cost.stone 0.4
       :effect.energy-cost.grass-block 0.2
       :effect.energy-cost.farmland 0.1
       :effect.energy-cost.default-block 0.5
       :breaking.ground-break-probability 0.3
       :breaking.mastery-hardness-cap 0.6
       :movement.launch-random-base 0.6
       :movement.launch-random-span 0.3
       :combat.entity-search-radius 2.0
       0.0))
   #'skill-config/tunable-int
   (fn [_ field-id]
     (case field-id
       :breaking.mastery-radius 5
       0))
   #'block-manip/available? (constantly true)
   #'block-manip/can-break-block? (constantly true)
   #'block-manip/get-block (fn [_ x y z] (get @world* [x y z]))
   #'block-manip/get-block-hardness
   (fn [_ x y z]
     (if-let [state (get @world* [x y z])]
       (case state
         "minecraft:stone" 1.5
         "minecraft:grass_block" 0.6
         "minecraft:dirt" 0.5
         "minecraft:cobblestone" 2.0
         1.5)
       0.0))
   #'block-manip/break-block! (fn [_ _ x y z _] (swap! world* dissoc [x y z]) true)
   #'block-manip/set-block! (fn [_ x y z id] (swap! world* assoc [x y z] id) true)
   #'block-manip/farmland-block? (fn [_ _ _ _] false)
   #'block-manip/liquid-block? (fn [_ _ _ _] false)
   #'world-effects/available? (constantly false)
   #'cn.li.ac.ability.service.skill-effects/skill-destroy-allowed? (constantly true)})

(defn- run-ours-geometry!
  "Our propagate-shockwave! + mastery ring against the mocked world. Mirror
  groundshock-on-key-up's start-y = floor(player y) - 1 (the upstream Plotter
  starts a row below the feet). `idx*` is the shared rand-stream index atom;
  :prop-idx captures it right after the propagation, before the ring."
  [world* p-look px py pz exp idx*]
  (let [broken* (HashSet.)]
    (with-redefs-fn (propagation-mocks world*)
      (fn []
        (let [result (@#'gs/propagate-shockwave! "p1" "w" px (dec py) pz p-look exp)
              prop-idx @idx*
              _ (@#'gs/break-mastery-ring! "p1" "w" {:x px :y py :z pz} exp broken*)]
          {:affected (set (map (fn [b] [(:x b) (:y b) (:z b)]) (:affected-blocks result)))
           :broken (set (map (fn [b] [(:x b) (:y b) (:z b)]) (:broken-blocks result)))
           :prop-idx prop-idx})))))

(defn- compare-upstream-geometry [exp p-look px py pz]
  (let [vals (vec (repeatedly 4000 #(rand)))
        ref-idx (atom 0)
        ref-prop-idx (atom 0)
        our-idx (atom 0)
        world-ref (build-flat-stone-world)
        world-ours (build-flat-stone-world)
        ref-result (with-redefs [clojure.core/rand (fn [] (nth vals (swap! ref-idx inc)))]
                     (let [r (up-perform! world-ref p-look px py pz exp)]
                       (reset! ref-prop-idx @ref-idx)
                       (up-mastery-ring! world-ref (:broken r) px py pz (+ 0.3 (* 0.7 exp)) exp)
                       r))
        ours (with-redefs [clojure.core/rand (fn [] (nth vals (swap! our-idx inc)))]
               (run-ours-geometry! world-ours p-look px py pz exp our-idx))
        ref-dejavu (set (map vec (seq (:dejavu ref-result))))
        ref-broken (set (map vec (seq (:broken ref-result))))]
    (is (= ref-dejavu (:affected ours))
        "converted (affected) cells match upstream exactly")
    (is (= ref-broken (:broken ours))
        "destroyed blocks match upstream exactly")
    (is (= @world-ref @world-ours)
        "final world state (conversions + breaks) matches upstream")
    ;; The propagation-phase roll count must match exactly (same values, same
    ;; order). The mastery ring afterwards draws differently: upstream rolls
    ;; the drop chance for every eligible cell including AIR (a no-op break),
    ;; ours gates on the block existing first — that shifts only the
    ;; post-ring RNG stream, never the geometry.
    (is (= @ref-prop-idx (:prop-idx ours))
        "propagation rand rolls identical")))

(deftest propagation-range-matches-upstream-exactly-test
  ;; Flat stone terrain: per-step cost ~5-6 energy out of 120 at exp 1, so the
  ;; path length is energy-limited near the maxIter cap — the case where a
  ;; small energy-accounting drift changes the visible RANGE by a step or two
  ;; (the pre-fix port plowed 24 steps vs upstream's 23).
  (compare-upstream-geometry 1.0 {:x 0.0 :y 0.0 :z 1.0} 5.2 65.3 -2.8)
  ;; horizontal-ish look with a y component exercises the delta-y row shift
  (let [l (Math/sqrt (+ (* 0.55 0.55) (* 0.35 0.35) (* 0.8 0.8)))]
    (compare-upstream-geometry 0.0 {:x (/ 0.55 l) :y (/ -0.35 l) :z (/ 0.8 l)}
                               0.3 65.7 -40.2))
  ;; negative x look — exercises the plotter dirflag sign
  (let [l (Math/sqrt (+ (* 0.9 0.9) (* 0.4 0.4) (* 0.2 0.2)))]
    (compare-upstream-geometry 0.5 {:x (/ -0.9 l) :y (/ -0.4 l) :z (/ 0.2 l)}
                               -3.7 66.3 12.1)))

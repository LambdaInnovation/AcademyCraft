(ns cn.li.ac.ability.client.effects.academy-arc
  "Deterministic client-side port of AcademyCraft's ArcFactory, EntityArc,
  EntitySurroundArc, and SubArc state.

  The original keeps a small bank of pre-generated white, textured L-system
  arcs. EntityArc swaps templates and toggles visibility on client ticks;
  EntitySurroundArc anchors several independently animated templates to the
  surface of a cube. Keeping those states here prevents render-frame rate and
  network packet cadence from changing the visual result."
  (:require [cn.li.ac.ability.client.effects.rv3 :as v]
            [cn.li.ac.ability.client.render-util :as ru]
            [cn.li.ac.config.modid :as modid])
  (:import [cn.li.mcmod.math V3]
           [java.util Random]))

(def ^:private line-texture
  (modid/asset-path "textures" "effects/arc/line_segment.png"))

(defn- white-argb
  [alpha]
  (let [a (int (max 0 (min 255 (long alpha))))]
    (unchecked-int
     (bit-or (bit-shift-left a 24)
             0x00FFFFFF))))

(def ^:private profiles
  {:charging
   {:template-count 20
    :length-lo 20.0
    :length-hi 20.0
    :width 0.1
    :max-offset 1.2
    :passes 5
    :branch-factor 0.3
    :width-shrink 0.7
    :render-scale 1.0}

   :normal
   {:template-count 10
    :spark-count 6
    :length-lo 3.0
    :length-hi 4.0
    :width 0.3
    :max-offset 0.8
    :passes 3
    :branch-factor 0.7
    :width-shrink 0.9
    :render-scale 0.3}

   :thin
   {:template-count 10
    :spark-count 4
    :length-lo 1.5
    :length-hi 2.0
    :width 0.2
    :max-offset 0.8
    :passes 3
    :branch-factor 0.7
    :width-shrink 0.9
    :render-scale 0.3}})

(def ^:private profile-seeds
  {:charging 17011
   :normal 27011
   :thin 37011})

(defonce ^:private template-cache* (atom {}))

(defn reset-template-cache-for-test!
  []
  (reset! template-cache* {})
  nil)

(defn- point [pos width]
  {:pos pos :width (double width)})

(defn- segment [start end alpha]
  {:start start :end end :alpha (double alpha)})

(defn- rotate-x
  ^V3 [^V3 p angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    (v/v3 (.-x p)
          (- (* (.-y p) c) (* (.-z p) s))
          (+ (* (.-y p) s) (* (.-z p) c)))))

(defn- rotate-y
  ^V3 [^V3 p angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    (v/v3 (+ (* (.-x p) c) (* (.-z p) s))
          (.-y p)
          (+ (* (- (.-x p)) s) (* (.-z p) c)))))

(defn- rotate-z
  ^V3 [^V3 p angle]
  (let [c (Math/cos angle)
        s (Math/sin angle)]
    (v/v3 (- (* (.-x p) c) (* (.-y p) s))
          (+ (* (.-x p) s) (* (.-y p) c))
          (.-z p))))

(defn- random-small-rotation
  ^V3 [^Random rng ^V3 dir]
  (let [range (/ (* 10.0 Math/PI) 180.0)
        angle (fn [] (- (* 2.0 range (.nextDouble rng)) range))]
    (-> dir
        (rotate-x (angle))
        (rotate-y (angle))
        (rotate-z (angle)))))

(defn- split-line
  [line offset {:keys [branch-factor width-shrink]} ^Random rng]
  (reduce
   (fn [{:keys [main branches] :as acc} seg]
     (let [start (:start seg)
           end (:end seg)
           start-pos ^V3 (:pos start)
           end-pos ^V3 (:pos end)
           middle (v/v* (v/v+ start-pos end-pos) 0.5)
           theta (* 2.0 Math/PI (.nextDouble rng))
           distance (* (double offset) (.nextDouble rng))
           displaced (v/v+ middle
                            (v/v3 0.0
                                  (* distance (Math/sin theta))
                                  (* distance (Math/cos theta))))
           middle-point (point displaced
                               (* 0.5 (+ (:width start) (:width end))))
           first-half (assoc seg :end middle-point)
           second-half (segment middle-point end (:alpha seg))
           acc* (assoc acc :main (conj main first-half second-half))]
       (if (< (.nextDouble rng) (double branch-factor))
         (let [branch-dir (random-small-rotation
                           rng
                           (v/v* (v/v- displaced start-pos) 0.7))
               branch-width (* (:width middle-point) (double width-shrink))
               branch (segment
                       (point displaced branch-width)
                       (point (v/v+ displaced branch-dir) branch-width)
                       (* (:alpha seg) 0.9))]
           (assoc acc* :branches (conj branches [branch])))
         acc*)))
   {:main [] :branches []}
   line))

(defn- generate-template
  [profile-key template-id]
  (let [{:keys [length-lo length-hi width max-offset passes] :as profile}
        (get profiles profile-key)
        seed (+ (long (get profile-seeds profile-key))
                (* 104729 (long template-id)))
        rng (Random. seed)
        length (+ (double length-lo)
                  (* (.nextDouble rng)
                     (- (double length-hi) (double length-lo))))
        initial [[(segment (point (v/v3 0.0 0.0 0.0) width)
                           (point (v/v3 length 0.0 0.0) width)
                           1.0)]]]
    (loop [lines initial
           pass 0
           offset (double max-offset)]
      (if (>= pass (int passes))
        {:profile profile-key
         :template-id template-id
         :length length
         :lines lines}
        (let [next-lines
              (reduce
               (fn [result line]
                 (let [{:keys [main branches]} (split-line line offset profile rng)]
                   (into (conj result main) branches)))
               []
               lines)]
          (recur next-lines (inc pass) (* 0.5 offset)))))))

(defn- templates
  [profile-key]
  (or (get @template-cache* profile-key)
      (locking template-cache*
        (or (get @template-cache* profile-key)
            (let [count* (get-in profiles [profile-key :template-count])
                  generated (mapv #(generate-template profile-key %)
                                  (range count*))]
              (swap! template-cache* assoc profile-key generated)
              generated)))))

(defn template-snapshot
  "Test/diagnostic view of the fixed original-style template bank."
  [profile-key]
  (mapv (fn [{:keys [length lines]}]
          {:length length
           :line-count (count lines)
           :segment-count (reduce + 0 (map count lines))})
        (templates profile-key)))

(defn- template-for
  [profile-key template-id]
  (let [bank (templates profile-key)]
    (nth bank (mod (int template-id) (count bank)))))

(defn- template-ops
  "`extra-op-keys` is merged onto every emitted quad (render-state flags that
  differ per call site — see surround-arc-ops' depth-write note)."
  [camera-pos {:keys [lines]} point-transform width-scale max-local-x effect-part
   extra-op-keys]
  (let [;; Upstream ArcFactory.handleSegment: width axis = cross(segDir,
        ;; normal) against the template's FIXED normal (local +Z), carried
        ;; across segments as lastDir. The fixed normal keeps every width
        ;; axis in one plane — camera-facing per-segment axes can rotate past
        ;; 90° into anti-parallel (segments straddling the camera), twisting
        ;; the quad into a self-intersecting bowtie whose GL fill tears a
        ;; visible gap, worst on the wide outer layer.
        normal-raw (v/v- (point-transform (v/v3 0.0 0.0 1.0))
                         (point-transform (v/v3 0.0 0.0 0.0)))
        normal (if (> (v/vlen normal-raw) 1.0e-5)
                 (v/vnorm normal-raw)
                 v/unit-x)]
    (vec
     (mapcat
      (fn [line]
        (let [segs (vec
                    (keep (fn [{:keys [start end alpha]}]
                            (let [start-local ^V3 (:pos start)]
                              (when (or (nil? max-local-x)
                                        (<= (.-x start-local) (double max-local-x)))
                                {:p0 (point-transform start-local)
                                 :p1 (point-transform ^V3 (:pos end))
                                 :start-width (* (double width-scale)
                                                 (double (:width start)))
                                 :end-width (* (double width-scale)
                                               (double (:width end)))
                                 :alpha (double alpha)})))
                          line))
              laterals (loop [i 0
                              acc []
                              prev v/unit-x]
                         (if (>= i (count segs))
                           acc
                           (let [seg (nth segs i)
                                 dir-vec (v/v- (:p1 seg) (:p0 seg))
                                 raw (v/vcross dir-vec normal)
                                 lateral (if (> (v/vlen raw) 1.0e-5)
                                           (v/vnorm raw)
                                           ;; segment parallel to the normal —
                                           ;; keep the carried axis (lastDir).
                                           prev)]
                             (recur (inc i) (conj acc lateral) lateral))))]
          (mapv (fn [seg-idx {:keys [p0 p1 start-width end-width alpha]}]
                  ;; Same carry-across as upstream handleSegment's lastDir: the
                  ;; quad's START width axis is the previous segment's, so
                  ;; adjacent quads share one axis at the junction and the strip
                  ;; stays watertight through every kink. The template's widths
                  ;; are already continuous at the joints — split-line halves
                  ;; the shared midpoint's width.
                  (let [start-right (nth laterals (if (zero? seg-idx) 0 (dec seg-idx)))
                        end-right (nth laterals seg-idx)
                        s-off (v/v* start-right start-width)
                        e-off (v/v* end-right end-width)]
                    (cond-> (assoc
                             (ru/quad-op line-texture
                                         (v/v+ p0 s-off)
                                         (v/v- p0 s-off)
                                         (v/v- p1 e-off)
                                         (v/v+ p1 e-off)
                                         (white-argb (* 255.0 alpha)))
                             :effect-part effect-part)
                      extra-op-keys (merge extra-op-keys))))
                (range (count segs)) segs)))
      lines))))

(defn entity-arc-ops
  "Render the original chargingArc template from start toward end.

  AcademyCraft does not scale its 20-block template to the target distance:
  EntityArc.draw(length) draws the prefix whose segment starts are within
  `length`. This deliberately preserves that behavior."
  [camera-pos ^V3 start ^V3 end template-id origin-offset]
  (let [template (template-for :charging template-id)
        direction-vector (v/v- end start)
        length (v/vlen direction-vector)]
    (when (> length 1.0e-6)
      (let [x-axis (v/vnorm direction-vector)
            reference (if (< (Math/abs (.-y ^V3 x-axis)) 0.99)
                        v/unit-y
                        v/unit-x)
            z-axis (v/vnorm (v/vcross x-axis reference))
            y-axis (v/vnorm (v/vcross z-axis x-axis))
            origin (if origin-offset (v/v+ start origin-offset) start)
            transform (fn [^V3 p]
                        (v/v+ origin
                              (v/v+ (v/v* x-axis (.-x p))
                                    (v/v+ (v/v* y-axis (.-y p))
                                          (v/v* z-axis (.-z p))))))]
        (template-ops camera-pos template transform 1.0 length
                      :current-charging/beam nil)))))

(defn- state-rng
  ^Random [salt generation spark-index visual-tick stream]
  (Random. (long (hash [salt generation spark-index visual-tick stream]))))

(defn- new-sparks
  [arc-type salt generation]
  (let [count* (get-in profiles [arc-type :spark-count])
        template-count (get-in profiles [arc-type :template-count])]
    (mapv
     (fn [idx]
       {:index idx
        :life 0
        ;; EntitySurroundArc generates and ticks its SubArcs before the first
        ;; useful render. Keep one spark visible in the freshly generated
        ;; batch so an effective target never depends on a later/random
        ;; client tick before any surround geometry can exist.
        :draw? (zero? idx)
        :dead? false
        :template-id (.nextInt
                      (state-rng salt generation idx 0 :initial-template)
                      (int template-count))})
     (range count*))))

(defn initial-surround-state
  [arc-type salt]
  {:arc-type arc-type
   :generation 0
   :sparks (new-sparks arc-type salt 0)})

(defn- advance-spark
  [spark arc-type salt generation visual-tick]
  (if (:dead? spark)
    spark
    (let [idx (:index spark)
          template-count (get-in profiles [arc-type :template-count])
          swap? (< (.nextDouble
                    (state-rng salt generation idx visual-tick :template-swap))
                   0.3)
          template-id (if swap?
                        (.nextInt
                         (state-rng salt generation idx visual-tick :template-pick)
                         (int template-count))
                        (:template-id spark))
          life (if (< (.nextDouble
                       (state-rng salt generation idx visual-tick :life))
                      0.9)
                 (inc (int (:life spark)))
                 (int (:life spark)))
          dead? (= life 30)
          draw? (if (:draw? spark)
                  (not (< (.nextDouble
                           (state-rng salt generation idx visual-tick :hide))
                          0.28))
                  (< (.nextDouble
                      (state-rng salt generation idx visual-tick :show))
                     0.21))]
      (assoc spark
             :life life
             :dead? dead?
             :draw? draw?
             :template-id template-id))))

(defn- keep-batch-visible
  "Keep a live surround batch from collapsing to zero render geometry.

  Upstream advances six independently flickering SubArcs as one entity. In
  this render-plan port an all-hidden batch means the surround effect has no
  ops at all for that frame, which is indistinguishable from a missing target
  update. Preserve the individual transition probabilities, then promote the
  first live spark only when the whole batch would otherwise be invisible."
  [sparks]
  (if (some #(and (:draw? %) (not (:dead? %))) sparks)
    sparks
    (if-let [visible-index
             (first
              (keep-indexed (fn [idx spark]
                              (when-not (:dead? spark) idx))
                            sparks))]
      (assoc-in sparks [visible-index :draw?] true)
      sparks)))

(defn tick-surround-state
  "Advance SubArc exactly once per client tick.

  frameRate=.6 gives a 30% template swap chance. switchRate=.7 gives
  28% visible->hidden and 21% hidden->visible transitions. Each sub-arc's
  life counter advances with 90% probability and the batch regenerates only
  after every member is dead."
  [state arc-type salt visual-tick]
  (let [state* (if (= arc-type (:arc-type state))
                 state
                 (initial-surround-state arc-type salt))
        generation (:generation state*)
        advanced
        (update state* :sparks
                #(keep-batch-visible
                  (mapv (fn [spark]
                          (advance-spark spark arc-type salt generation visual-tick))
                        %)))]
    ;; Regenerate after advancing as well as for an already-expired incoming
    ;; batch. Otherwise the tick that kills the final SubArc produces an empty
    ;; render plan and the replacement is delayed until the following tick.
    (if (every? :dead? (:sparks advanced))
      (let [next-generation (inc (long generation))]
        (assoc advanced
               :generation next-generation
               :sparks (new-sparks arc-type salt next-generation)))
      advanced)))

(defn- cube-surface-anchor
  ^V3 [{:keys [width height depth]} ^Random rng]
  (let [w (double width)
        h (double height)
        d (double depth)
        x-offset (* -0.5 w)
        z-offset (* -0.5 d)
        face (.nextInt rng 6)
        a (.nextDouble rng)
        b (.nextDouble rng)]
    (case face
      0 (v/v3 (+ x-offset (* a w)) 0.0 (+ z-offset (* b d)))
      1 (v/v3 (+ x-offset (* a w)) h (+ z-offset (* b d)))
      2 (v/v3 (+ x-offset (* b w)) (* a h) z-offset)
      3 (v/v3 (+ x-offset (* b w)) (* a h) (+ z-offset d))
      4 (v/v3 x-offset (* a h) (+ z-offset (* b d)))
      5 (v/v3 (+ x-offset w) (* a h) (+ z-offset (* b d))))))

(defn surround-arc-ops
  "Render active SubArcs on a CubePointFactory-compatible body.

  body uses the original entity convention: x/y/z is the bottom-center,
  while width/height/depth describe the cube dimensions."
  [camera-pos body arc-type {:keys [generation sparks]} salt]
  (let [body-origin (v/v3 (double (:x body))
                          (double (:y body))
                          (double (:z body)))
        body-yaw (double (or (:yaw-rad body) 0.0))
        render-scale (get-in profiles [arc-type :render-scale])]
    (vec
     (mapcat
      (fn [{:keys [index draw? dead? template-id]}]
        (when (and draw? (not dead?))
          (let [pose-rng (state-rng salt generation index 0 :pose)
                anchor-rng (state-rng salt generation index 0 :anchor)
                anchor (cube-surface-anchor body anchor-rng)
                rx (* 2.0 Math/PI (.nextDouble pose-rng))
                ry (* 2.0 Math/PI (.nextDouble pose-rng))
                rz (* 2.0 Math/PI (.nextDouble pose-rng))
                template (template-for arc-type template-id)
                half-length (* 0.5 (:length template))
                transform
                (fn [^V3 p]
                  (let [centered (v/v3 (- (.-x p) half-length)
                                       (.-y p)
                                       (.-z p))
                        scaled (v/v* centered render-scale)
                        posed (-> scaled
                                  (rotate-x rx)
                                  (rotate-y ry)
                                  (rotate-z rz))
                        body-local (v/v+ anchor posed)
                        yawed (rotate-y body-local body-yaw)]
                    (v/v+ body-origin yawed)))]
            (template-ops camera-pos template transform render-scale nil
                          :current-charging/surround
                          ;; SubArcHandler.drawAll wraps the whole batch in
                          ;; glDepthMask(false): the sparks depth-TEST against
                          ;; the world (the half buried in the block stays
                          ;; hidden) but never write depth, so they blend with
                          ;; each other instead of the nearest one punching a
                          ;; hole through the rest. EntityArc's own renderer
                          ;; has no such call, so the beam keeps depth write.
                          {:no-depth-write? true}))))
      sparks))))

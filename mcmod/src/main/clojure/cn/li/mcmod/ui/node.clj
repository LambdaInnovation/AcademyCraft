(ns cn.li.mcmod.ui.node
  "Node model & kind table. Replaces old CGUI atom-per-field map.

   Node = Java POJO with mutable primitive fields + dslots/oslots arrays.
   All access via INode Java interface (cross-ns/module safe, remap-safe).

   Pure kind definition table: S0 static immutable def (not in registry).
   Render backend :render!/:bake! installed by mc1201 via install-adapter!
   into [:platform :ui-kinds]."
  (:import [cn.li.mcmod.ui.node INode Node]))

;; ============================================================================
;; Bitmask constants
;; ============================================================================

(def ^:const FLAG-LAYOUT-DIRTY 1)
(def ^:const FLAG-RENDER-DIRTY 2)
(def ^:const FLAG-CLIP 4)
(def ^:const FLAG-HAS-TRANSFORM 8)
(def ^:const FLAG-HOVERED 16)
(def ^:const FLAG-FOCUSED 32)

;; ============================================================================
;; Factory
;; ============================================================================

(defn create-node
  "Create Node. Props: {:x :y :w :h :scale :z :pivot-x :pivot-y
   :align-w :align-h :visible? :clip? :transform?}."
  [idx id kind props dslot-count oslot-count static-props]
  (let [{:keys [x y w h scale z pivot-x pivot-y align-w align-h]
         :or {x 0.0 y 0.0 w 0.0 h 0.0 scale 1.0 z 0.0
              pivot-x 0.0 pivot-y 0.0
              align-w :left align-h :top}}
        props
        visible? (boolean (get props :visible? true))
        clip? (boolean (get props :clip? false))
        transform? (boolean (get props :transform? false))
        flags-val (int (bit-or (if clip? FLAG-CLIP 0)
                               (if transform? FLAG-HAS-TRANSFORM 0)
                               FLAG-LAYOUT-DIRTY FLAG-RENDER-DIRTY))
        aw (byte (case align-w :center 1 :right 2 0))
        ah (byte (case align-h :middle 1 :bottom 2 0))]
    (Node. (int idx) id kind
           nil                                   ;; parent
           nil                                   ;; children (POJO allocates)
           (double x) (double y) (double w) (double h) (double scale) (double z)
           0.0 0.0 1.0                           ;; abs-x/y, cum-scale
           (double pivot-x) (double pivot-y)
           aw ah
           flags-val
           visible?
           static-props
           (double-array dslot-count)
           (object-array oslot-count))))

(defn- dev-asserts-enabled? []
  (Boolean/getBoolean "mcmod.ui.devAsserts"))

(defn dev-assert-child-count!
  "Dev/CI only — verify childCount matches non-nil children array entries."
  [^INode node]
  (when (dev-asserts-enabled?)
    (let [n (.getChildCount node)
          ^objects cs (.getChildrenArr node)
          actual (loop [i 0 c 0]
                   (if (>= i n) c
                       (recur (unchecked-inc-int i)
                              (if (some? (aget cs i)) (inc c) c))))]
      (when (not= n actual)
        (throw (ex-info "childCount invariant violated"
                        {:node-id (.getId node)
                         :child-count n
                         :non-nil-children actual}))))))

(defn child-count
  "O(1) child count — maintained by add-child!/remove-child!/clear-children!."
  [^INode node]
  (.getChildCount node))

(defn add-child!
  "Add child to parent's children array (auto-grow). Sets parent back-ref."
  [^INode parent ^INode child]
  (let [n (.getChildCount parent)
        ^objects old (.getChildrenArr parent)
        cap (alength old)]
    (if (< n cap)
      (aset ^objects old n child)
      (let [new-arr (make-array INode (max 8 (* 2 cap)))]
        (System/arraycopy old 0 new-arr 0 n)
        (aset ^objects new-arr n child)
        (.setChildrenArr parent new-arr)))
    (.setParentNode child parent)
    (.setChildCount parent (unchecked-inc-int n))
    (dev-assert-child-count! parent)
    parent))

(defn remove-child!
  "Remove child from parent's children array by identity."
  [^INode parent ^INode child]
  (let [n (.getChildCount parent)
        ^objects cs (.getChildrenArr parent)]
    (loop [i 0]
      (when (< i n)
        (if (identical? child (aget cs i))
          (do
            (loop [j i]
              (when (< j (dec n))
                (aset cs j (aget cs (unchecked-inc-int j)))
                (recur (unchecked-inc-int j))))
            (aset cs (dec n) nil)
            (.setChildCount parent (dec n))
            (dev-assert-child-count! parent))
          (recur (unchecked-inc-int i))))))
  parent)

;; ============================================================================
;; Kind definition table (S0 static def)
;;
;; :prop-writers values are declarative maps consumed by cn.li.mcmod.ui.slot-write
;; {:slot :dslot|:oslot :idx N :sig :d|:o :dirty :render|:layout :coerce ...}
;; ============================================================================

(def kinds
  {:group
   {:dslots {}
    :oslots {}
    :oslots-backend-base 0
    :prop-writers {}
    :hit? false}

   :box
   {:dslots {:fill-argb 0 :outline-argb 1 :outline-width 2 :tint-argb 3 :hover-tint-argb 4}
    :oslots {}
    :oslots-backend-base 4
    :prop-writers {:fill       {:slot :dslot :idx 0 :sig :d}
                   :outline    {:slot :dslot :idx 1 :sig :d}
                   :tint       {:slot :dslot :idx 3 :sig :d}
                   :hover-tint {:slot :dslot :idx 4 :sig :d}}
    :hit? true}

   :image
   {:dslots {:alpha 0 :u 1 :v 2 :tex-w 3 :tex-h 4}
    :oslots {:src 0 :tint 1}
    :oslots-backend-base 2
    :prop-writers {:src   {:slot :oslot :idx 0 :sig :o}
                   :alpha {:slot :dslot :idx 0 :sig :d}
                   :tint  {:slot :oslot :idx 1 :sig :o :coerce :tint-rgb}
                   :u     {:slot :dslot :idx 1 :sig :d}
                   :v     {:slot :dslot :idx 2 :sig :d}
                   :tex-w {:slot :dslot :idx 3 :sig :d}
                   :tex-h {:slot :dslot :idx 4 :sig :d}}
    :hit? true}

   :text
   {:dslots {:font-size 0 :x-offset 1 :y-offset 2}
    :oslots {:text 0 :color 1 :font 2 :align 3}
    :oslots-backend-base 8
    :prop-writers {:text      {:slot :oslot :idx 0 :sig :o}
                   :color     {:slot :oslot :idx 1 :sig :d}
                   :font-size {:slot :dslot :idx 0 :sig :d}}
    :hit? true}

   :progress
   {:dslots {:progress 0 :corner 1 :hint-percent 2 :scroll-offset 3 :alpha 4}
    :oslots {:color-stops 0 :bg-src 1 :fg-src 2 :icon-src 3}
    :oslots-backend-base 8
    :prop-writers {:progress      {:slot :dslot :idx 0 :sig :d}
                   :hint          {:slot :dslot :idx 2 :sig :d}
                   :color-stops   {:slot :oslot :idx 0 :sig :o}
                   :bg-src        {:slot :oslot :idx 1 :sig :o}
                   :fg-src        {:slot :oslot :idx 2 :sig :o}
                   :icon-src      {:slot :oslot :idx 3 :sig :o}
                   :scroll-offset {:slot :dslot :idx 3 :sig :d}
                   :alpha         {:slot :dslot :idx 4 :sig :d}}
    :hit? false}

   :crosshair
   {:dslots {:phase 0 :intensity 1}
    :oslots {}
    :oslots-backend-base 0
    :prop-writers {}
    :hit? false}

   ;; Stamps a texture's opaque area into the depth buffer without drawing any
   ;; colour — the mask half of depth layering (see render.clj). Static props:
   ;; :z, :alpha-cutoff.
   :depth-mask
   {:dslots {}
    :oslots {:src 0}
    :oslots-backend-base 1
    :prop-writers {:src {:slot :oslot :idx 0 :sig :o}}
    :hit? false}

   :shader-quad
   {:dslots {:alpha 0}
    :oslots {:shader-props 0}
    :oslots-backend-base 4
    :prop-writers {:alpha {:slot :dslot :idx 0 :sig :d}
                   :shader-props {:slot :oslot :idx 0 :sig :o}}
    :hit? false}

   :shader-ring
   {:dslots {:progress 0 :inner 1 :outer 2}
    :oslots {:shader-props 0}
    :oslots-backend-base 4
    :prop-writers {:progress {:slot :dslot :idx 0 :sig :d}
                   :shader-props {:slot :oslot :idx 0 :sig :o}}
    :hit? false}

   :shader-progress
   {:dslots {:progress 0}
    :oslots {:shader-props 0}
    :oslots-backend-base 4
    :prop-writers {:progress {:slot :dslot :idx 0 :sig :d}
                   :shader-props {:slot :oslot :idx 0 :sig :o}}
    :hit? false}

   :gradient
   {:dslots {:alpha 0 :angle 1}
    :oslots {:stops 0}
    :oslots-backend-base 2
    :prop-writers {:stops {:slot :oslot :idx 0 :sig :o}
                   :alpha {:slot :dslot :idx 0 :sig :d}}
    :hit? false}

   :line
   {:dslots {:x1 0 :y1 1 :x2 2 :y2 3 :thickness 4 :alpha 5}
    :oslots {:color 0}
    :oslots-backend-base 2
    :prop-writers {:color {:slot :oslot :idx 0 :sig :d}
                   :alpha {:slot :dslot :idx 5 :sig :d}}
    :hit? false}

   :list
   {:dslots {:spacing 0 :scroll-offset 1}
    :oslots {:template 0}
    :oslots-backend-base 1
    :prop-writers {:spacing       {:slot :dslot :idx 0 :sig :d :dirty :layout}
                   :scroll-offset {:slot :dslot :idx 1 :sig :d :dirty :layout}}
    :hit? true}

   :nine-slice
   {:dslots {:margin 0}
    :oslots {:src 0 :line-tex 1}
    :oslots-backend-base 2
    :prop-writers {:src      {:slot :oslot :idx 0 :sig :o}
                   :line-tex {:slot :oslot :idx 1 :sig :o}
                   :margin   {:slot :dslot :idx 0 :sig :d}}
    :hit? false}

   :glow-line
   {:dslots {:x0 0 :x1 1 :y 2 :line-w 3 :glow-sz 4 :tint 5 :no-center 6}
    :oslots {}
    :oslots-backend-base 0
    :prop-writers {}
    :hit? false}

   :preview-item
   {:dslots {}
    :oslots {:item-id 0}
    :oslots-backend-base 2
    :prop-writers {:item-id {:slot :oslot :idx 0 :sig :o}}
    :hit? false}

   :preview-3d
   {:dslots {:rotation-speed 0 :scale 1 :y-offset 2}
    :oslots {:render-type 0 :block-id 1 :item-id 2}
    :oslots-backend-base 4
    :prop-writers {:render-type {:slot :oslot :idx 0 :sig :o}
                   :block-id   {:slot :oslot :idx 1 :sig :o}
                   :item-id    {:slot :oslot :idx 2 :sig :o}
                   :rotation-speed {:slot :dslot :idx 0 :sig :d}
                   :scale          {:slot :dslot :idx 1 :sig :d}
                   :y-offset       {:slot :dslot :idx 2 :sig :d}}
    :hit? false}})

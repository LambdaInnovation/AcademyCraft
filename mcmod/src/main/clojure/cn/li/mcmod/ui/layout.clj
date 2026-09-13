(ns cn.li.mcmod.ui.layout
  "Layout math & render tape flattening.

   Converges the duplicated pos/scale/pivot/align math from traversal.clj
   and renderer.clj into one copy. LambdaLib2 semantics (scaled-dimension
   alignment offset, pivot shift).

   Render tape:
   - Rebuilt only when tree-dirty -> painter-order Object[] (Node + sentinels)
   - Sentinels: ::push-clip ::pop-clip ::push-transform ::pop-transform
   - Siblings sorted by z (stable)
   - Invisible subtrees skipped entirely."
  (:require [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [java.util ArrayList]))

;; ============================================================================
;; Sentinel markers (identity comparison, zero allocation in render loop)
;; ============================================================================

(def push-clip-sentinel ::push-clip)
(def pop-clip-sentinel ::pop-clip)
(def push-transform-sentinel ::push-transform)
(def pop-transform-sentinel ::pop-transform)

;; ============================================================================
;; Alignment offset helpers
;; ============================================================================

(defn- align-offset-x [align parent-w scaled-w]
  (case (int align)
    1 (/ (- parent-w scaled-w) 2.0)
    2 (- parent-w scaled-w)
    0.0))

(defn- align-offset-y [align parent-h scaled-h]
  (case (int align)
    1 (/ (- parent-h scaled-h) 2.0)
    2 (- parent-h scaled-h)
    0.0))

;; ============================================================================
;; compute-abs-pos! — single node layout
;; ============================================================================

(defn compute-abs-pos!
  [^INode node
   parent-abs-x parent-abs-y
   parent-scale parent-w parent-h
   & [{:keys [local-y-offset]}]]
  (let [own-scale (.getScale node)
        cum-scale (* parent-scale own-scale)
        sw (* (.getW node) own-scale)
        sh (* (.getH node) own-scale)
        align-off-x (align-offset-x (.getAlignW node) parent-w sw)
        align-off-y (align-offset-y (.getAlignH node) parent-h sh)
        pivot-shift-x (* (.getPivotX node) (.getW node))
        pivot-shift-y (* (.getPivotY node) (.getH node))
        y-extra (double (or local-y-offset 0.0))
        child-x (+ align-off-x (.getX node) (- pivot-shift-x))
        child-y (+ align-off-y (.getY node) y-extra (- pivot-shift-y))
        abs-x (+ parent-abs-x (* child-x parent-scale))
        abs-y (+ parent-abs-y (* child-y parent-scale))]
    (.setAbsX node abs-x)
    (.setAbsY node abs-y)
    (.setCumScale node cum-scale)
    (.clearFlag node node/FLAG-LAYOUT-DIRTY)
    node))

;; ============================================================================
;; Recursive child layout (forward-declared for ensure-layout!)
;; ============================================================================

(declare ensure-children-layout!)

(defn ensure-layout!
  "Top-down layout recomputation: only dirty subtrees. Root anchored at (0,0)."
  [^UiRt rt]
  (let [root (rt/node-by-idx rt 0)]
    (when root
      (let [root-recomputed? (.hasFlag ^INode root node/FLAG-LAYOUT-DIRTY)]
        (when root-recomputed?
          (compute-abs-pos! root 0.0 0.0 1.0
                            (rt/screen-w rt) (rt/screen-h rt)))
        (ensure-children-layout! root
                                 (.getAbsX ^INode root) (.getAbsY ^INode root)
                                 (.getCumScale ^INode root)
                                 (.getW ^INode root) (.getH ^INode root)
                                 root-recomputed?)))))

(defn- ensure-children-layout!
  "Recurse: re-layout dirty children and their subtrees.

   ancestor-recomputed? cascades: a child's abs position derives from its
   parent's, so once ANY ancestor recomputed, every descendant must too —
   without this, moving/scaling a container (carousel pages, popups) left
   all its visible content frozen at the old coordinates while non-layout
   channels (alpha, fills) still responded."
  [^INode parent
   parent-abs-x parent-abs-y
   parent-scale parent-w parent-h
   ancestor-recomputed?]
  (let [list-scroll (when (= (.getKind parent) :list)
                      (.getDSlot parent 1))
        n (.getChildCount parent)]
    (loop [i 0]
      (when (< i n)
        (let [^INode child (.getChild parent i)]
          (when (and child (.isVisible child))
            (let [recompute? (or ancestor-recomputed?
                                 (.hasFlag child node/FLAG-LAYOUT-DIRTY)
                                 (= (.getKind parent) :list))]
              (when recompute?
                (compute-abs-pos! child parent-abs-x parent-abs-y parent-scale parent-w parent-h
                                  {:local-y-offset (when list-scroll (- list-scroll))}))
              (ensure-children-layout! child
                                       (.getAbsX child) (.getAbsY child)
                                       (.getCumScale child)
                                       (.getW child) (.getH child)
                                       recompute?)))
          (recur (unchecked-inc-int i)))))))

;; ============================================================================
;; Tape flattening (only when tree-dirty)
;; ============================================================================

(defn- flatten-into!
  "Flatten node subtree into tape-arr (ArrayList). Invisible subtrees skipped.
   Group with clip? -> PushClip/PopClip sentinels.
   Group with transform? -> PushTransform/PopTransform sentinels.
   Siblings sorted by z (stable)."
  [^INode node ^ArrayList tape-arr]
  (when (.isVisible node)
    (let [has-clip (.hasFlag node node/FLAG-CLIP)
          has-tf   (.hasFlag node node/FLAG-HAS-TRANSFORM)]
      (when has-clip (.add tape-arr push-clip-sentinel))
      (when has-tf   (.add tape-arr push-transform-sentinel))
      (.add tape-arr node)
      (let [n (.getChildCount node)]
        (when (pos? n)
          (let [visible-children
                (loop [i 0 acc (transient [])]
                  (if (< i n)
                    (let [^INode c (.getChild node i)]
                      (if (and c (.isVisible c))
                        (recur (unchecked-inc-int i) (conj! acc c))
                        (recur (unchecked-inc-int i) acc)))
                    (persistent! acc)))]
            (doseq [^INode c (sort-by #(.getZ ^INode %) visible-children)]
              (flatten-into! c tape-arr)))))
      (when has-tf   (.add tape-arr pop-transform-sentinel))
      (when has-clip (.add tape-arr pop-clip-sentinel)))))

(defn ensure-tape!
  "Rebuild render tape when tree-dirty, else no-op. Returns tape Object[]."
  [^UiRt rt]
  (when (rt/tree-dirty? rt)
    (let [root (rt/node-by-idx rt 0)
          tape-arr (ArrayList. 128)]
      (when root
        (flatten-into! ^INode root tape-arr))
      (let [arr (object-array (.size tape-arr))]
        (.toArray tape-arr arr)
        (rt/set-tape-arr! rt arr))
      (rt/clear-tree-dirty! rt)))
  (rt/get-tape-arr rt))

;; ============================================================================
;; hit-test — deepest hit on cached tape
;; ============================================================================

(defn- ^:static point-in-node?
  "Check if (mx,my) is within node's render bounds (cached abs-x/y/cum-scale)."
  [^INode node ^double mx ^double my]
  (let [x0 (.getAbsX node)
        y0 (.getAbsY node)
        w  (* (.getW node) (.getCumScale node))
        h  (* (.getH node) (.getCumScale node))]
    (and (>= mx x0) (< mx (+ x0 w))
         (>= my y0) (< my (+ y0 h)))))

(defn hit-test
  "Iterate cached tape in painter order; return deepest INode containing (mx,my).

   Clip sentinels bracket clipped subtrees: a node that lies OUTSIDE its clip
   viewport does not hit. Without this, scrolled content (group moved inside
   a clip=true viewport, e.g. the about app's scroll area) keeps answering
   hit-tests at its pre-scroll position — once it rolls up over UI declared
   earlier in the tape (tab buttons, other overlays), clicks on that UI land
   on the invisible rolled-out text instead. The sentinel is followed by the
   clipping node itself, whose abs rect is the subtree's clip region.
   Call after ensure-layout! and ensure-tape!."
  [^UiRt rt ^double mx ^double my]
  (let [^"[Ljava.lang.Object;" tape (rt/get-tape-arr rt)
        n (alength tape)]
    (loop [i 0 best nil clips []]
      (if (< i n)
        (let [entry (aget tape i)]
          (cond
            (identical? entry push-clip-sentinel)
            ;; The entry right after the sentinel is the clipping node; its
            ;; abs rect clips every descendant. It also participates in
            ;; hit-testing itself (the next iteration handles it as a node).
            (recur (unchecked-inc-int i) best (conj clips (aget tape (inc i))))

            (identical? entry pop-clip-sentinel)
            (recur (unchecked-inc-int i) best (pop clips))

            (instance? INode entry)
            (let [^INode nd entry
                  inside? (and (point-in-node? nd mx my)
                               (every? (fn [^INode c] (point-in-node? c mx my))
                                       clips))]
              (recur (unchecked-inc-int i) (if inside? nd best) clips))

            :else
            (recur (unchecked-inc-int i) best clips)))
        best))))

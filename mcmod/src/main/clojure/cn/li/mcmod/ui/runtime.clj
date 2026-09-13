(ns cn.li.mcmod.ui.runtime
  "UI Runtime (UiRt) — one instance per screen/overlay.
   UiRt is a Java POJO (cn.li.mcmod.uipojo.runtime.UiRt) for AOT/reflection=fail compliance."
  (:require [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.node-props-spec :as props-spec])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.uipojo.signal SigD SigL IApply Binding SignalSupport]
           [cn.li.mcmod.ui.node INode]
           [clojure.lang IPersistentMap PersistentArrayMap]
           [java.util ArrayList HashMap]))

(defn create-runtime ^UiRt []
  (UiRt. (SigL. 0 (SignalSupport/newOuts 16))
         (SigD. 0.0 (SignalSupport/newOuts 16))
         (SigL. 0 (SignalSupport/newOuts 16))))

(defn clock-ms-sig ^SigL [^UiRt rt] (.getClockMs rt))
(defn partial-ticks-sig ^SigD [^UiRt rt] (.getPartialTicks rt))
(defn game-ticks-sig ^SigL [^UiRt rt] (.getGameTicks rt))

(defn register-node! [^UiRt rt id ^INode node]
  (.add ^ArrayList (.getNodes rt) node)
  (when id (.setIdToNode rt (assoc (.getIdToNode rt) id node)))
  node)

(defn node-by-id ^INode [^UiRt rt id] (get (.getIdToNode rt) id))

(defn node-by-idx ^INode [^UiRt rt ^long idx]
  (let [^ArrayList ns (.getNodes rt)]
    (when (and (>= idx 0) (< idx (.size ns)))
      (.get ns (int idx)))))

(defn flush! [^UiRt rt]
  (let [^ArrayList q (.getDirtyBindings rt) n (.size q)]
    (when (pos? n)
      (loop [i 0]
        (when (< i n)
          (.applyBinding ^IApply (.get q i))
          (recur (unchecked-inc-int i))))
      (.clear q))))

(defn resize! [^UiRt rt ^double w ^double h]
  (when (or (not= w (.getScreenW rt)) (not= h (.getScreenH rt)))
    (.setScreenW rt w)
    (.setScreenH rt h)
    (.setTreeDirty rt true)
    ;; Centered / edge-aligned roots depend on screen size; dirty layout so
    ;; abs positions recompute instead of staying at the previous size.
    (when-let [^INode root (node-by-idx rt 0)]
      (.setFlag root node/FLAG-LAYOUT-DIRTY))))

(defn screen-w ^double [^UiRt rt] (.getScreenW rt))
(defn screen-h ^double [^UiRt rt] (.getScreenH rt))

(defn mark-tree-dirty! [^UiRt rt] (.setTreeDirty rt true))
(defn tree-dirty? [^UiRt rt] (.isTreeDirty rt))
(defn clear-tree-dirty! [^UiRt rt] (.setTreeDirty rt false))

(defn get-tape-arr [^UiRt rt] (.getTape rt))
(defn set-tape-arr! [^UiRt rt arr] (.setTape rt arr))
(defn get-dirty-bindings-q ^ArrayList [^UiRt rt] (.getDirtyBindings rt))

(defn hovered-idx [^UiRt rt] (.getHoveredIdx rt))

(defn set-hovered-idx! [^UiRt rt idx]
  (.setHoveredIdx rt (int idx)))

(defn focus-idx [^UiRt rt] (.getFocusIdx rt))

(defn set-focus-idx! [^UiRt rt idx]
  (.setFocusIdx rt (int idx)))

(defn drag-node-idx [^UiRt rt] (.getDragNodeIdx rt))
(defn set-drag-node-idx! [^UiRt rt ^long idx] (.setDragNodeIdx rt (int idx)))
(defn dragging? [^UiRt rt] (.isDragging rt))
(defn set-dragging?! [^UiRt rt v] (.setDragging rt (boolean v)))
(defn drag-start-mx [^UiRt rt] (.getDragStartMx rt))
(defn set-drag-start-mx! [^UiRt rt ^double v] (.setDragStartMx rt v))
(defn drag-start-my [^UiRt rt] (.getDragStartMy rt))
(defn set-drag-start-my! [^UiRt rt ^double v] (.setDragStartMy rt v))
(defn drag-start-ms [^UiRt rt] (.getDragStartMs rt))
(defn set-drag-start-ms! [^UiRt rt ^long v] (.setDragStartMs rt v))

(defn register-event! [^UiRt rt ^long node-idx event-key handler-fn]
  (let [events (.getEvents rt)
        node-events (get events (int node-idx) {})
        handlers (get node-events event-key [])]
    (.setEvents rt (assoc events (int node-idx) (assoc node-events event-key (conj handlers handler-fn)))))
  nil)

(defn get-event-handlers [^UiRt rt ^long node-idx event-key]
  (get-in (.getEvents rt) [(int node-idx) event-key]))

(defn remove-node-events! [^UiRt rt ^long node-idx]
  (.setEvents rt (dissoc (.getEvents rt) (int node-idx)))
  nil)

(defn register-binding! [^UiRt rt ^long node-idx ^Binding b]
  (let [^HashMap bindings (.getBindingsByNode rt)
        node-key (Integer/valueOf (int node-idx))
        ^ArrayList node-bindings (or (.get bindings node-key)
                                     (let [created (ArrayList. 4)]
                                       (.put bindings node-key created)
                                       created))]
    (.add node-bindings b))
  nil)

(defn unbind-node-bindings! [^UiRt rt ^long node-idx]
  (let [^HashMap bindings (.getBindingsByNode rt)
        node-key (Integer/valueOf (int node-idx))]
    (when-let [^ArrayList node-bindings (.remove bindings node-key)]
      (loop [index 0]
        (when (< index (.size node-bindings))
          (sig/unbind! ^Binding (.get node-bindings index))
          (recur (unchecked-inc-int index))))))
  nil)

(defn clear-rt-bindings! [^UiRt rt]
  (let [^HashMap bindings (.getBindingsByNode rt)]
    (doseq [^ArrayList node-bindings (.values bindings)]
      (loop [index 0]
        (when (< index (.size node-bindings))
          (sig/unbind! ^Binding (.get node-bindings index))
          (recur (unchecked-inc-int index)))))
    (.clear bindings))
  nil)

(defn binding-count
  "Test/diagnostic: total registered bindings for this runtime."
  ^long [^UiRt rt]
  (let [^HashMap bindings (.getBindingsByNode rt)]
    (loop [remaining (seq (.values bindings))
           total 0]
      (if remaining
        (recur (next remaining)
               (+ total (.size ^ArrayList (first remaining))))
        (long total)))))

(defn unbind-subtree!
  "Remove all signal bindings and event handlers from node and descendants."
  [^UiRt rt ^INode node]
  (unbind-node-bindings! rt (.getIdx node))
  (remove-node-events! rt (.getIdx node))
  (let [n (.getChildCount node)]
    (loop [i 0]
      (when (< i n)
        (when-let [^INode c (.getChild node i)] (unbind-subtree! rt c))
        (recur (unchecked-inc-int i))))))

(defn put-user-signal! [^UiRt rt id s]
  (.setUserSignals rt (assoc (.getUserSignals rt) id s))
  nil)

(defn user-signal [^UiRt rt id] (get (.getUserSignals rt) id))

(defn dispose! [^UiRt rt]
  (when-not (.isDisposed rt)
    (.setDisposed rt true)
    (.clear ^ArrayList (.getNodes rt))
    (.setIdToNode rt PersistentArrayMap/EMPTY)
    (.clear ^ArrayList (.getDirtyBindings rt))
    (.setTape rt (object-array 0))
    (.setEvents rt PersistentArrayMap/EMPTY)
    (.setUserSignals rt PersistentArrayMap/EMPTY)
    (clear-rt-bindings! rt))
  nil)

(defn disposed? [^UiRt rt] (.isDisposed rt))

(defn- init-node-props!
  [^INode n kdef props]
  (let [kind-kw (.getKind n)
        writers (set (keys (:prop-writers kdef)))]
    (doseq [[prop-key slot-idx] (:dslots kdef)]
      (when-some [v (get props prop-key)]
        (when-not (contains? writers prop-key)
          (when (number? v)
            (slot-write/write-dslot! n (int slot-idx) (double v) :render)))))
    (doseq [[prop-key slot-idx] (:oslots kdef)]
      (when-some [v (get props prop-key)]
        (when-not (contains? writers prop-key)
          (slot-write/write-oslot! n (int slot-idx) v :render))))
    (doseq [prop-key writers]
      (when-some [v (get props prop-key)]
        (slot-write/apply-prop! n kind-kw prop-key v)))
    ;; Defaults: alpha = 1.0 (nodes without explicit alpha must be visible)
    (when (and (contains? #{:image :shader-quad} kind-kw) (zero? (.getDSlot n 0)))
      (.setDSlot n 0 1.0))))

(declare build-node!)

(defn- build-node!
  [^UiRt rt spec parent-node]
  (let [{:keys [kind props children]} spec
        ;; Specs declare node id either at the top level ({:kind .. :id ..})
        ;; or inside :props ({:kind .. :props {:id ..}}). Both conventions are
        ;; used across content screens, so resolve from whichever is present;
        ;; top level wins. Without this, props-only ids never reach
        ;; register-node! and every node-by-id lookup returns nil.
        id (or (:id spec) (:id props))
        kdef (or (get node/kinds kind) (throw (ex-info (str "Unknown kind: " kind) {:kind kind})))
        props* (when props (props-spec/validate-build-props! kind props))
        dslot-cnt (max (count (:dslots kdef)) 1)
        oslot-cnt (+ (long (:oslots-backend-base kdef (count (:oslots kdef)))) 4)
        ^ArrayList ns (.getNodes rt)
        idx (.size ns)
        n (node/create-node idx id kind props* dslot-cnt oslot-cnt props*)]
    (register-node! rt id n)
    (init-node-props! n kdef props*)
    (when parent-node (node/add-child! parent-node n))
    (doseq [c children]
      (when c (build-node! rt c n)))
    n))

(defn build! [^UiRt rt spec]
  (let [root (build-node! rt spec nil)]
    (.setTreeDirty rt true)
    (.getIdx ^INode root)))

(defn build-child!
  ^INode [^UiRt rt spec ^INode parent-node]
  (let [root (build-node! rt spec parent-node)]
    (.setTreeDirty rt true)
    root))

(defn clear-children!
  [^UiRt rt ^INode parent-node]
  (let [n (.getChildCount parent-node)
        ^objects cs (.getChildrenArr parent-node)]
    (loop [i 0]
      (when (< i n)
        (when-let [^INode c (aget cs i)]
          (unbind-subtree! rt c)
          (aset cs i nil))
        (recur (unchecked-inc-int i))))
    (.setChildCount parent-node 0)
    (node/dev-assert-child-count! parent-node)
    (.setTreeDirty rt true)
    nil))

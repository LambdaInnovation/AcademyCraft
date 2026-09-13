(ns cn.li.mcmod.ui.core
  "High-level convenience API over signal + node + runtime.
   Provides bind!/on!/list-set!/with-nodes for consumer code."
  (:require [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.slot-write :as slot-write]
            [cn.li.mcmod.ui.node-props-spec :as props-spec]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.events :as events])
  (:import [cn.li.mcmod.uipojo.signal ISigD ISigO ISigL]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]
           [java.util ArrayList]))

;; ============================================================================
;; bind! — high-level signal-to-node-property binding
;; ============================================================================

(defn bind!
  "Bind a signal to a node property. Resolves node by id, looks up prop-writer
   from kind table, creates Binding linked to rt's flush queue.
   Usage: (ui/bind! rt :cp-bar :progress cp-percent-sig)"
  [^UiRt rt id prop-key source]
  (let [^INode n (rt/node-by-id rt id)]
    (when-not n (throw (ex-info (str "bind!: node not found: " id) {:id id})))
    (let [kind-kw (.getKind n)
          kdef (get node/kinds kind-kw)
          spec (slot-write/prop-writer-spec kdef prop-key)
          writer (slot-write/resolve-sig-writer kdef prop-key)]
      (when-not writer (throw (ex-info (str "bind!: no prop-writer for " kind-kw "/" prop-key)
                                        {:kind kind-kw :prop-key prop-key})))
      (slot-write/assert-sig-matches! source spec)
      (let [b (sig/bind! source n writer (rt/get-dirty-bindings-q rt))]
        (rt/register-binding! rt (.getIdx n) b)
        nil))))

;; ============================================================================
;; on! — high-level event registration
;; ============================================================================

(defn on!
  "Register event handler on node by id.
   Usage: (ui/on! rt :btn-teleport :left-click handle-teleport)"
  [^UiRt rt id event-key handler-fn]
  (events/on! rt id event-key handler-fn))

;; ============================================================================
;; node-by-id — lookup helper
;; ============================================================================

(defn node
  "Look up node by id. Returns INode or nil."
  ^cn.li.mcmod.ui.node.INode [^UiRt rt id]
  (rt/node-by-id rt id))

;; ============================================================================
;; set-prop! — direct property setter (non-signal)
;; ============================================================================

(defn set-prop!
  "Set a static property directly on a node (non-signal, immediate).
   Useful for one-time setup like texture URLs, initial text, etc.
   Usage: (ui/set-prop! rt :label :text \"Hello World\")"
  [^UiRt rt id prop-key value]
  (let [^INode n (rt/node-by-id rt id)]
    (when-not n (throw (ex-info (str "set-prop!: node not found: " id) {:id id})))
    (let [kind-kw (.getKind n)]
      (when (props-spec/pilot-kind? kind-kw)
        (props-spec/validate-kind-prop! kind-kw prop-key value))
      (slot-write/apply-prop! n kind-kw prop-key value)
      nil)))

;; ============================================================================
;; with-nodes macro — id destructuring
;; ============================================================================

(defmacro with-nodes
  "Destructure nodes by id for cleaner consumer code.
   Usage: (with-nodes rt [label :label, btn :btn-teleport]
            (ui/bind! rt label :text some-signal)
            (ui/on! rt btn :left-click handle-click))"
  [rt bindings & body]
  (let [pairs (partition 2 bindings)]
    `(let [~@(mapcat (fn [[sym id]] [sym `(rt/node-by-id ~rt ~id)]) pairs)]
       ~@body)))

;; ============================================================================
;; list-set! — keyed list reconciliation
;; ============================================================================

(defn- find-in-subtree
  "Find a node by id within a subtree (depth-first). Returns INode or nil."
  ^cn.li.mcmod.ui.node.INode [^INode subtree-root target-id]
  (if (= target-id (.getId subtree-root))
    subtree-root
    (let [n (.getChildCount subtree-root)]
      (loop [i 0]
        (when (< i n)
          (if-let [^INode c (.getChild subtree-root i)]
            (or (find-in-subtree c target-id)
                (recur (unchecked-inc-int i)))
            (recur (unchecked-inc-int i))))))))

(defn- template-item-h
  "Resolve list row height from the template root, else its first child.
   XML often wraps content as <template><box size=…/></template> with no size
   on the template itself — falling back to 24 then stacks rows on top of each
   other (settings/media)."
  [template]
  (let [h0 (double (get-in template [:props :h] 0.0))
        h1 (double (get-in template [:children 0 :props :h] 0.0))]
    (cond (pos? h0) h0
          (pos? h1) h1
          :else 24.0)))

(defn list-set!
  "Rebuild :list node children from items using the list's template spec.
   - Template spec comes from the list node's :template static prop (or oslot 0)
   - Old children have their event handlers removed (leak prevention)
   - per-item-fn: (fn [rt item-root-node item]) — set props / attach handlers;
     use (ui/item-node item-root sub-id) to find sub-nodes within the item.
   - Optional per-item `:row-h` overrides template height (category spacers).
   Usage: (ui/list-set! rt :entries locations
            (fn [rt item-root loc]
              (ui/set-node-prop! rt (ui/item-node item-root :label) :text (:name loc))))"
  [^UiRt rt list-id items per-item-fn]
  (let [^INode list-node (rt/node-by-id rt list-id)]
    (when-not list-node
      (throw (ex-info (str "list-set!: node not found: " list-id) {:id list-id})))
    (let [template (or (.getOSlot list-node 0)
                       (get (.getStaticProps list-node) :template))
          ;; Honor explicit spacing=\"0\" (upstream ElementList.spacing=0). Default
          ;; 4 only when the list never declared :spacing in props.
          spacing (if (contains? (.getStaticProps list-node) :spacing)
                    (double (.getDSlot list-node 0))
                    4.0)
          default-h (template-item-h template)]
      (when-not template
        (throw (ex-info (str "list-set!: no template on list node " list-id) {:id list-id})))
      ;; Clear existing children (removes their event handlers)
      (rt/clear-children! rt list-node)
      ;; Instantiate template per item — cumulative Y so :row-h spacers work.
      (loop [y 0.0
             remaining (seq items)]
        (when remaining
          (let [item (first remaining)
                item-h (double (or (:row-h item) default-h))
                item-spec (-> template
                              (assoc-in [:props :y] y)
                              (assoc-in [:props :h] item-h))
                item-root (rt/build-child! rt item-spec list-node)]
            (per-item-fn rt item-root item)
            (recur (+ y item-h spacing) (next remaining)))))
      (rt/mark-tree-dirty! rt)
      nil)))

(defn item-node
  "Find a sub-node by id within a list item subtree."
  ^cn.li.mcmod.ui.node.INode [^INode item-root sub-id]
  (find-in-subtree item-root sub-id))

(defn set-node-prop!
  "Set a property directly on an INode (used with item-node from list-set!)."
  [^UiRt _rt ^INode n prop-key value]
  (when n
    (let [kind-kw (.getKind n)]
      (when (props-spec/pilot-kind? kind-kw)
        (props-spec/validate-kind-prop! kind-kw prop-key value))
      (slot-write/apply-prop! n kind-kw prop-key value)
      nil)))

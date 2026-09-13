(ns cn.li.mcmod.ui.slot-write
  "Declarative slot writes — equality short-circuit + dirty flags.
   All user-prop writes go through here; bake backends keep direct setOSlot in mc1201."
  (:require [cn.li.mcmod.ui.node :as node])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigD ISigO]))

(defn- dirty-mask
  [dirty-kw]
  (case dirty-kw
    :layout node/FLAG-LAYOUT-DIRTY
    node/FLAG-RENDER-DIRTY))

(defn write-dslot!
  [^INode n ^long idx ^double v dirty-kw]
  (when-not (== v (.getDSlot n idx))
    (.setDSlot n idx v)
    (.setFlag n (dirty-mask dirty-kw))))

(defn write-oslot!
  [^INode n ^long idx v dirty-kw]
  (when-not (= v (.getOSlot n idx))
    (.setOSlot n idx v)
    (.setFlag n (dirty-mask dirty-kw))))

(defn- tint-rgb-value
  "Convert tint to an RGBA channel vector in 0-255 space (the image render layer
   divides by 255 and folds the alpha into the node's draw alpha). Accepts a
   vector [r g b] / [r g b a] (already 0-255, e.g. HUD wave tints), an ARGB long
   from parse-color, or nil → opaque white. The long branch must NOT normalize to
   0-1: doing so double-divides at render time and every tinted image goes black.
   The alpha byte matters — the wireless/tech-ui icons encode their dimming (0xB2,
   0x99, 0xCC …) in the tint alpha, so dropping it renders them fully opaque."
  [source]
  (cond
    (vector? source) (mapv #(double %) source)
    (number? source)
    (let [l (long source)]
      [(double (bit-and (bit-shift-right l 16) 0xFF))
       (double (bit-and (bit-shift-right l 8) 0xFF))
       (double (bit-and l 0xFF))
       (double (bit-and (bit-shift-right l 24) 0xFF))])
    :else [255.0 255.0 255.0 255.0]))

(defn- coerce-static-value
  [spec v]
  (cond
    (= (:coerce spec) :tint-rgb) (tint-rgb-value v)
    (= (:sig spec) :d) (double v)
    :else v))

(defn write-prop-value!
  "Write a static/coerced value per declarative prop spec."
  [^INode n spec v]
  (let [{:keys [slot idx dirty transform]} spec
        dirty-kw (or dirty :render)
        v* (if transform (transform v) (coerce-static-value spec v))]
    (case slot
      :dslot (write-dslot! n idx (double v*) dirty-kw)
      :oslot (write-oslot! n idx v* dirty-kw))))

(defn apply-prop!
  "Apply a user prop value to a node (build/set-prop!/events)."
  [^INode node kind-kw prop-key value]
  (if-let [spec (get-in node/kinds [kind-kw :prop-writers prop-key])]
    (write-prop-value! node spec value)
    (throw (ex-info "apply-prop!: no prop-writer"
                    {:kind kind-kw :prop-key prop-key}))))

(defn assert-sig-matches!
  [source spec]
  (case (:sig spec)
    :d (when-not (instance? ISigD source)
         (throw (ex-info "bind!: expected ISigD signal"
                         {:sig :d :source-class (class source)})))
    :o (when-not (instance? ISigO source)
         (throw (ex-info "bind!: expected ISigO signal"
                         {:sig :o :source-class (class source)})))
    (throw (ex-info "bind!: invalid prop-writer spec" {:spec spec}))))

(defn resolve-sig-writer
  "Return IApply fn for signal binding from kind def + prop key."
  [kdef prop-key]
  (when-let [spec (get-in kdef [:prop-writers prop-key])]
    (fn [^INode node source]
      (let [{:keys [slot idx sig dirty transform coerce]} spec
            raw (case sig
                  :d (.dGet ^ISigD source)
                  :o (.sGet ^ISigO source))
            v (cond
                transform (transform raw)
                (= coerce :tint-rgb) (tint-rgb-value raw)
                (= sig :d) raw
                :else raw)
            dirty-kw (or dirty :render)]
        (case slot
          :dslot (write-dslot! node idx (double v) dirty-kw)
          :oslot (write-oslot! node idx v dirty-kw))))))

(defn prop-writer-spec
  [kdef prop-key]
  (get-in kdef [:prop-writers prop-key]))

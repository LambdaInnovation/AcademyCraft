(ns cn.li.mcmod.ui.signal
  "Fine-grained explicit-dependency signal core.

   Implementation: Java POJOs in cn.li.mcmod.uipojo.signal.* (AOT/reflection=fail safe).
   This namespace is a thin Clojure API over those classes."
  (:import [cn.li.mcmod.uipojo.signal SigD SigL SigO ComputedD ComputedO Binding
            ISigD ISigL ISigO IApply IDoubleSource ISupportsOuts IDep SignalSupport]
           [cn.li.mcmod.uipojo.runtime IUiNode]
           [java.util ArrayList]))

(defn signal-d ^SigD [^double init]
  (SigD. init (SignalSupport/newOuts 4)))

(defn signal-l ^SigL [^long init]
  (SigL. init (SignalSupport/newOuts 4)))

(defn signal-o ^SigO [init]
  (SigO. init (SignalSupport/newOuts 4)))

(defn sget-d ^double [^ISigD s]
  (.dGet s))

(defn sget-l ^long [^ISigL s]
  (.lGet s))

(defn sget-o [^ISigO s]
  (.sGet s))

(defn sset-d! [^ISigD s ^double v]
  (.dSet s v))

(defn sset-l! [^ISigL s ^long v]
  (.lSet s v))

(defn sset-o! [^ISigO s v]
  (.sSet s v))

(defn ^:no-doc add-dep! [^ArrayList outs ^IDep dep]
  (SignalSupport/addDep outs dep))

(defn ^:no-doc remove-dep! [^ArrayList outs ^IDep dep]
  (SignalSupport/removeDep outs dep))

(defn- to-more-double-sources [srcs]
  (when (> (count srcs) 3)
    (into-array IDoubleSource (subvec srcs 3))))

(defn computed-d ^ComputedD [sources f]
  (let [srcs (vec sources)
        n (count srcs)
        s0 (nth srcs 0 nil)
        s1 (nth srcs 1 nil)
        s2 (nth srcs 2 nil)
        more (to-more-double-sources srcs)
        c (ComputedD. s0 s1 s2 more f (int n) 0.0 true (SignalSupport/newOuts 4))]
    (doseq [^ISupportsOuts s srcs]
      (add-dep! (SignalSupport/outsOf s) c))
    c))

(defn computed-o ^ComputedO [sources f]
  "Universal object-valued computed signal.  Sources may be ISigO, IDoubleSource,
   or ISigD — recompute() dispatches on type.  Use for any Object-producing
   computation (string, keyword, vector) regardless of source types."
  (let [srcs (vec sources)
        n (count srcs)
        s0 (nth srcs 0 nil)
        s1 (nth srcs 1 nil)
        s2 (nth srcs 2 nil)
        more (when (> n 3) (into-array Object (subvec srcs 3)))
        c (ComputedO. s0 s1 s2 more f (int n) nil true (SignalSupport/newOuts 4))]
    (doseq [^ISupportsOuts s srcs]
      (add-dep! (SignalSupport/outsOf s) c))
    c))

(defn bind! ^Binding [^ISupportsOuts source ^IUiNode node apply-fn ^ArrayList flush-queue]
  (let [b (Binding. source node apply-fn false flush-queue)]
    (add-dep! (SignalSupport/outsOf source) b)
    b))

(defn unbind! [^Binding b]
  (remove-dep! (SignalSupport/outsOf (.getSource b)) b)
  nil)

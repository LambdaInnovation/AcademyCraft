(ns cn.li.mcbase.gui.reactive.bake-slots
  "Backend oslot type registry — dev/CI only, not on render hot path.

  Version modules install the native id class (ResourceLocation / Identifier)."
  (:import [cn.li.mcmod.ui.node INode]))

(defonce ^:private id-class-atom (atom nil))

(defn install-id-class!
  "Install native Minecraft id Class for bake-slot type asserts."
  [^Class c]
  (reset! id-class-atom c)
  c)

(defn- id-class []
  (let [c @id-class-atom]
    (when (nil? c)
      (throw (IllegalStateException. "bake-slots id class not installed")))
    c))

(defn- bake-slot-specs []
  (let [id-class (id-class)]
    {:image   {2 id-class}
     :text    {8 clojure.lang.IPersistentMap}
     :progress {8 id-class
                9 id-class}}))

(defn bake-asserts-enabled?
  []
  (Boolean/getBoolean "mcmod.ui.bakeAsserts"))

(defn assert-bake-slots!
  "Verify backend oslots hold expected types after bake. Dev/CI only."
  [^INode node]
  (when-let [specs (get (bake-slot-specs) (.getKind node))]
    (doseq [[idx expected-type] specs]
      (let [v (.getOSlot node (int idx))]
        (when (some? v)
          (when-not (instance? expected-type v)
            (throw (ex-info "Bake slot type mismatch"
                            {:kind (.getKind node)
                             :slot idx
                             :expected expected-type
                             :actual (class v)
                             :value v})))))))
  node)

(defn maybe-assert-bake-slots!
  [^INode node]
  (when (bake-asserts-enabled?)
    (assert-bake-slots! node))
  node)

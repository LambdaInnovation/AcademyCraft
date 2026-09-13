(ns cn.li.fabricbase.runtime
  "Loader-wide Fabric runtime state. Minecraft-version adapters provide the
  native callback functions; this namespace owns lifecycle ordering only.")

(defonce ^:private runtime*
  (atom {:installed? false
         :callbacks {}}))

(defn install!
  [callbacks]
  (when-not (map? callbacks)
    (throw (ex-info "Fabric shared callbacks must be a map" {:callbacks callbacks})))
  (swap! runtime* assoc :installed? true :callbacks callbacks)
  nil)

(defn installed?
  []
  (:installed? @runtime*))

(defn callbacks
  []
  (:callbacks @runtime*))

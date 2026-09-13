(ns cn.li.platform.loader-hook-support
  "Loader-keyed scripted-entity hook impl-key support matrix.

  Reads META-INF/academy-loader-hook-support.properties (see also
  docs/dev/loader-hook-support.properties). Used by platform hook registration
  to skip unsupported impl keys for the active target (Fabric filtering gate)."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [cn.li.platform.target :as target]
            [cn.li.mcmod.util.log :as log]))

(def ^:private resource-path "META-INF/academy-loader-hook-support.properties")

(defn- parse-csv-keys
  [raw]
  (->> (str/split (or raw "") #",")
       (map str/trim)
       (remove str/blank?)
       (map keyword)
       set))

(defn- load-properties!
  []
  (if-let [url (io/resource resource-path)]
    (with-open [r (io/reader url)]
      (doto (java.util.Properties.)
        (.load r)))
    (do
      (log/warn "Loader hook support matrix missing" {:resource resource-path})
      (java.util.Properties.))))

(def ^:private props-delay (delay (load-properties!)))

(defn- prop
  [^java.util.Properties props target-id kind field]
  (.getProperty props (str target-id "." field "." (name kind) ".impl.keys") ""))

(defn- current-target-id
  []
  (try
    (name (target/current-target-key!))
    (catch Exception _
      nil)))

(defn supported-impl-key?
  "Return true when `impl-key` is allowed for `hook-kind` on `target-id`.

  Rules: not in unsupported list, and (supported list empty OR key listed).
  When `target-id` is nil (no platform metadata), all keys are treated as supported."
  ([hook-kind impl-key]
   (supported-impl-key? (current-target-id) hook-kind impl-key))
  ([target-id hook-kind impl-key]
   (let [impl-key (cond-> impl-key (string? impl-key) keyword)]
     (boolean
      (cond
        (nil? impl-key) false
        (nil? target-id) true
        :else
        (let [props @props-delay
              unsupported (parse-csv-keys (prop props target-id hook-kind "unsupported"))
              supported (parse-csv-keys (prop props target-id hook-kind "supported"))]
          (and (not (contains? unsupported impl-key))
               (or (empty? supported)
                   (contains? supported impl-key)))))))))

(defn filter-impl-key->hook-class
  "Keep only supported entries of an impl-key → hook-class map for the active target."
  ([hook-kind impl-key->hook-class]
   (filter-impl-key->hook-class (current-target-id) hook-kind impl-key->hook-class))
  ([target-id hook-kind impl-key->hook-class]
   (if (nil? target-id)
     impl-key->hook-class
     (into {}
           (filter (fn [[impl-key _]]
                     (let [ok? (supported-impl-key? target-id hook-kind impl-key)]
                       (when-not ok?
                         (log/warn "Skipping unsupported scripted hook impl key for loader"
                                   {:target target-id
                                    :hook-kind hook-kind
                                    :impl-key impl-key}))
                       ok?))
                   impl-key->hook-class)))))

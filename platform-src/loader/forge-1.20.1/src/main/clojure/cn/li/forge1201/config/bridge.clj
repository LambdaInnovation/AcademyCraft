(ns cn.li.forge1201.config.bridge
  (:require [clojure.string :as str]
            [cn.li.platform.neutral.config :as config-reg]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.forge1201.bridge ConfigEventBridge]
           [java.util.function Consumer]
           [java.util.function Predicate]
           [java.util ArrayList Collection]
            [net.minecraftforge.common ForgeConfigSpec$Builder ForgeConfigSpec$ConfigValue]
           [net.minecraftforge.eventbus.api IEventBus]
           [net.minecraftforge.fml ModLoadingContext]
           [net.minecraftforge.fml.event.config ModConfigEvent]
           [net.minecraftforge.fml.config ModConfig ModConfig$Type]))

(def ^:private registered-configs
  "Map of config file-name -> domain-info. Lock-free CAS updates replace the
   prior ^:dynamic var + Object lock."
  (atom {}))

(defn- registered-configs-snapshot
  []
  @registered-configs)

(defn- assoc-registered-config!
  [file-name domain-info]
  (swap! registered-configs assoc file-name domain-info)
  nil)

(defn- domain->file-name
  [domain extension]
  (let [ns-part (namespace domain)
        name-part (name domain)]
    (config-reg/config-file-path (str ns-part "-" name-part extension))))

(defn- section-segments
  [descriptor]
  (->> (str/split (or (some-> descriptor :section name)
                      (str/join "." (butlast (str/split (:path descriptor) #"\."))))
                  #"\.")
       (remove str/blank?)))

(defn- leaf-name
  [descriptor]
  (last (str/split (:path descriptor) #"\.")))

(defn- comment-builder
  [^ForgeConfigSpec$Builder builder comment]
  (if (str/blank? (str comment))
    builder
    (.comment builder ^"[Ljava.lang.String;" (into-array String [(str comment)]))))

(defn- list-value-predicate
  ^Predicate
  [descriptor-type]
  (reify Predicate
    (test [_ value]
      (case descriptor-type
        :string-list (string? value)
        :int-list (number? value)
        :double-list (number? value)
        false))))

(defn- define-list-entry!
  [^ForgeConfigSpec$Builder builder ^String leaf default descriptor-type]
  (let [^ArrayList default-list (ArrayList.)
        ^Predicate predicate (list-value-predicate descriptor-type)]
    (.addAll default-list ^Collection (or default []))
    (.defineList builder leaf default-list predicate)))

(defn- define-entry!
  [^ForgeConfigSpec$Builder builder descriptor]
  (let [default (:default descriptor)
        min-val (:min descriptor)
        max-val (:max descriptor)
      ^String leaf (leaf-name descriptor)
        ^ForgeConfigSpec$Builder builder (comment-builder builder (:comment descriptor))]
    (case (:type descriptor)
      :int (.defineInRange builder leaf (int default) (int (or min-val Integer/MIN_VALUE)) (int (or max-val Integer/MAX_VALUE)))
      :double (.defineInRange builder leaf (double default) (double (or min-val (- Double/MAX_VALUE))) (double (or max-val Double/MAX_VALUE)))
      :boolean (.define builder leaf (boolean default))
      :string (.define builder leaf (str default))
      :string-list (define-list-entry! builder leaf default :string-list)
      :int-list (define-list-entry! builder leaf default :int-list)
      :double-list (define-list-entry! builder leaf default :double-list)
      (throw (ex-info "Unsupported Forge config descriptor type"
                      {:descriptor descriptor})))))

(defn- build-domain-spec
  [domain descriptors]
  (let [builder (ForgeConfigSpec$Builder.)
        entries (into {}
                      (for [descriptor descriptors]
                        (let [sections (section-segments descriptor)]
                          (doseq [section sections]
                            (.push builder ^String section))
                          (let [entry (define-entry! builder descriptor)]
                            (dotimes [_ (count sections)]
                              (.pop builder))
                            [(:key descriptor) entry]))))
        spec (.build builder)
        file-name (domain->file-name domain ".toml")]
    {:domain domain
     :file-name file-name
     :spec spec
     :entries entries}))

(defn- load-domain-values!
  [{:keys [domain file-name entries]}]
  (let [values (reduce-kv (fn [m config-key entry]
                           (assoc m config-key (.get ^ForgeConfigSpec$ConfigValue entry)))
                         {}
                         entries)]
    (config-reg/set-config-values! domain values)
    (log/debug "Loaded Forge config domain" domain "from" file-name)))

(defn- handle-config-event!
  [^ModConfigEvent event]
  (let [^ModConfig mod-config (.getConfig event)
        file-name (.getFileName mod-config)]
    ;; Capture ModConfig reference for later .save() calls
    (swap! registered-configs assoc-in [file-name :mod-config] mod-config)
    (when-let [domain-info (get (registered-configs-snapshot) file-name)]
      (load-domain-values! domain-info))))

(defn register-all!
  [^IEventBus mod-bus]
  (let [domains (seq (config-reg/get-all-config-domains))]
    (doseq [domain domains]
      (let [descriptors (config-reg/get-config-descriptors domain)]
        (when (seq descriptors)
          (let [{:keys [file-name spec] :as domain-info} (build-domain-spec domain descriptors)]
            (.registerConfig (ModLoadingContext/get) ModConfig$Type/COMMON spec file-name)
            (assoc-registered-config! file-name domain-info)
            (log/debug "Registered Forge config file" file-name "for domain" domain)))))
    (ConfigEventBridge/addConfigListeners mod-bus
                                          (reify Consumer
                                            (accept [_ event]
                                              (handle-config-event! event))))
    nil))

(defn registered-config-for-domain
  "Find the domain-info map by domain keyword. Returns nil if not found."
  [domain]
  (some (fn [[_ v]] (when (= (:domain v) domain) v))
        (registered-configs-snapshot)))

(def ^:private config-value-write-lock
  "Serializes the Java-side ConfigValue.set()+ModConfig.save() sequence below
   — a genuine external mutation race, not covered by the registered-configs
   atom's own CAS semantics. Kept intentionally (JVM_PRIMITIVE_KEEP)."
  (Object.))

(defn set-config-value!
  "Set a single config value via Forge's ConfigValue.set() and persist to TOML file.
  domain — e.g. :cn.li.example/config-domain
  key    — e.g. :some-setting
  value  — the new value (boolean/int/double/string)"
  [domain key value]
  (locking config-value-write-lock
    (if-let [domain-info (registered-config-for-domain domain)]
      (if-let [^ForgeConfigSpec$ConfigValue cfg-value (get (:entries domain-info) key)]
        (do
          (.set cfg-value value)
          ;; Persist to TOML file
          (when-let [^ModConfig mod-cfg (:mod-config domain-info)]
            (.save mod-cfg))
          ;; Also update in-memory registry for immediate getter visibility
          (config-reg/set-config-value! domain key value)
          true)
        (do
          (log/warn "set-config-value!: unknown key" {:domain domain :key key :available (keys (:entries domain-info))})
          false))
      (do
        (log/warn "set-config-value!: unknown domain" {:domain domain})
        false))))

(defn install-config-persist-op!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :config-persist {:persist! #'set-config-value!}))
  nil)

;; This namespace intentionally contains only ForgeConfigSpec/event plumbing.
;; Config domains, defaults, and typed accessors live in content/shared modules.

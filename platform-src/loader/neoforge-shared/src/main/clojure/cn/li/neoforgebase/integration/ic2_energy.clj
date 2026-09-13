(ns cn.li.neoforgebase.integration.ic2-energy
  "IC2 Energy integration - exposes AC energy converters as IC2 EU providers/consumers.

  SANCTIONED REFLECTION ISLAND: optional third-party ic2.api.* types are not
  Minecraft/Forge symbols and are allowlisted by verifyNoPlatformReflection."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.integration-runtime :as energy-hooks])
  (:import [cn.li.mcmod.energy IEnergyCapable]
           [java.lang.reflect InvocationHandler Proxy]))


(def ^:private resolved-vars
  "Per-symbol requiring-resolve memoization cache. requiring-resolve is
   idempotent, so a lock-free CAS race just recomputes the same value at
   worst — no locking needed, unlike the prior ^:dynamic var + Object lock."
  (atom {}))

(defn- resolve-var
  [var-sym]
  (or (get @resolved-vars var-sym)
      (let [v (requiring-resolve var-sym)]
        (swap! resolved-vars assoc var-sym v)
        v)))

(defn- cap-call
  [var-sym & args]
  (apply (resolve-var var-sym) args))

(def ^:private ic2-source-class-name "ic2.api.energy.tile.IEnergySource")
(def ^:private ic2-sink-class-name "ic2.api.energy.tile.IEnergySink")

(defn- context-class-loader
  []
  (or (.getContextClassLoader (Thread/currentThread))
      (.getClassLoader (class *ns*))))

(defn- resolve-class
  [class-name]
  (Class/forName class-name false (context-class-loader)))

(defn- class-present?
  [class-name]
  (try
    (some? (resolve-class class-name))
    (catch ClassNotFoundException _
      false)
    (catch LinkageError e
      (log/warn "Optional IC2 class could not be linked:" class-name (ex-message e))
      false)))

(defn- primitive-default
  [^Class return-type]
  (cond
    (= Boolean/TYPE return-type) false
    (= Character/TYPE return-type) (char 0)
    (= Byte/TYPE return-type) (byte 0)
    (= Short/TYPE return-type) (short 0)
    (= Integer/TYPE return-type) (int 0)
    (= Long/TYPE return-type) (long 0)
    (= Float/TYPE return-type) (float 0)
    (= Double/TYPE return-type) (double 0)
    :else nil))

(defn- create-interface-proxy
  [^Class iface method-handlers label]
  (Proxy/newProxyInstance
    (.getClassLoader iface)
    (into-array Class [iface])
    (reify InvocationHandler
      (invoke [_ proxy method args]
        (let [method-name (.getName method)
              ^objects args (or args (object-array 0))]
          (case method-name
            "toString" (str label " proxy")
            "hashCode" (System/identityHashCode proxy)
            "equals" (identical? proxy (aget args 0))
            (if-let [handler (get method-handlers method-name)]
              (handler args)
              (primitive-default (.getReturnType method)))))))))

;; IC2 detection

(defn ic2-available?
  "Check if IC2 mod is available at runtime.

  Uses requiring-resolve to safely check for IC2 classes without
  causing ClassNotFoundException if IC2 is not present."
  []
  (and (class-present? ic2-source-class-name)
       (class-present? ic2-sink-class-name)))

;; IC2 conversion rates

(def ^:private default-eu-conversion-rate
  "Default conversion rate: 1 content energy unit = 1 EU."
  1.0)

(defn eu-conversion-rate
  "Get the EU conversion rate from config or default."
  []
  (double (or (energy-hooks/ic2-energy-conversion-rate)
              default-eu-conversion-rate)))

(defn content-to-eu
  "Convert content energy units to EU (Energy Units).

  Args:
    content-amount - Amount in content energy units

  Returns:
    Amount in EU"
  [content-amount]
  (* content-amount (eu-conversion-rate)))

(defn eu-to-content
  "Convert EU (Energy Units) to content energy units.

  Args:
    eu-amount - Amount in EU

  Returns:
    Amount in content energy units"
  [eu-amount]
  (/ eu-amount (eu-conversion-rate)))

;; IC2 capability implementation

(defn- create-ic2-energy-sink
  "Create an IC2 IEnergySink implementation that wraps an IEnergyCapable.

  This allows IC2 machines to push EU into descriptor-declared content endpoints.

  Args:
    energy-capable - The IEnergyCapable implementation
    tier - IC2 energy tier (1-4)

  Returns:
    IEnergySink proxy object"
  [^IEnergyCapable energy-capable tier]
  (try
    (let [^IEnergyCapable ec energy-capable
          iface (resolve-class ic2-sink-class-name)]
      (create-interface-proxy
        iface
        {"getDemandedEnergy" (fn [_]
                                (let [current (.getEnergyStored ec)
                                      max-energy (.getMaxEnergyStored ec)
                                      space (- max-energy current)]
                                  (double (content-to-eu space))))
         "getSinkTier" (fn [_] (int tier))
         "injectEnergy" (fn [^objects args]
                           (let [amount (double (aget args 1))
                                 content-amount (eu-to-content amount)
                                 received (.receiveEnergy ec (int content-amount) false)
                                 eu-received (content-to-eu received)]
                             (double (- amount eu-received))))
         "acceptsEnergyFrom" (fn [_] true)}
        "IC2 IEnergySink"))
    (catch Exception e
      (log/stacktrace "Failed to create IC2 energy sink:" e)
      nil)))

(defn- create-ic2-energy-source
  "Create an IC2 IEnergySource implementation that wraps an IEnergyCapable.

  This allows IC2 machines to pull EU from descriptor-declared content endpoints.

  Args:
    energy-capable - The IEnergyCapable implementation
    tier - IC2 energy tier (1-4)

  Returns:
    IEnergySource proxy object"
  [^IEnergyCapable energy-capable tier]
  (try
    (let [^IEnergyCapable ec energy-capable
          iface (resolve-class ic2-source-class-name)]
      (create-interface-proxy
        iface
        {"getOfferedEnergy" (fn [_]
                               (double (content-to-eu (.getEnergyStored ec))))
         "drawEnergy" (fn [^objects args]
                         (let [amount (double (aget args 0))
                                 content-amount (eu-to-content amount)]
                               (.extractEnergy ec (int content-amount) false)
                           nil))
         "getSourceTier" (fn [_] (int tier))
         "emitsEnergyTo" (fn [_] true)}
        "IC2 IEnergySource"))
    (catch Exception e
      (log/stacktrace "Failed to create IC2 energy source:" e)
      nil)))

(defn get-ic2-capability
  "Get IC2 energy capability for a block entity.

  This is called by the capability bridge when IC2 queries for energy
  capabilities on descriptor-declared content endpoints.

  Args:
    be - The block entity
    side - The side being queried (or nil for any side)
    mode - Content-declared transfer mode ('import' or 'export')

  Returns:
    IC2 energy interface (IEnergySink or IEnergySource) or nil"
  [be _side mode]
  (try
    ;; Get the content energy capability first. Descriptor-specific binding is
    ;; handled by the content module before this optional integration is used.
    (when-let [content-energy-cap (cap-call 'cn.li.mcmod.capability.registry/get-capability be :content-energy nil)]
      (when (cap-call 'cn.li.mcmod.capability.registry/is-present? content-energy-cap)
        (let [content-energy (cap-call 'cn.li.mcmod.capability.registry/or-else content-energy-cap nil)
              tier 2] ;; Default to tier 2 (Medium Voltage, 128 EU/t)
          (when content-energy
            ;; Create appropriate IC2 interface based on mode
            (case mode
              "import" (create-ic2-energy-sink content-energy tier)
              "export" (create-ic2-energy-source content-energy tier)
              nil)))))
    (catch Exception e
      (log/stacktrace "Error creating IC2 capability:" e)
      nil)))

(defn register-ic2-capability!
  "Register IC2 energy capability for descriptor-declared content endpoints.

  This allows IC2 machines to interact with content-provided energy endpoints using EU.

  Returns:
    true if successful, false if IC2 not available"
  []
  (if (ic2-available?)
    (try
      ;; Register IC2 energy capabilities
      ;; Note: IC2 uses event-based registration, not Forge's capability bridge
      ;; We'll need to handle EnergyTileLoadEvent/UnloadEvent in the tile entity

      (log/info "IC2 integration available - EU conversion enabled")
      (log/info (format "Conversion rate: 1 content energy unit = %.1f EU" (eu-conversion-rate)))
      true
      (catch Exception e
        (log/stacktrace "Failed to register IC2 capability:" e)
        false))
    (do
      (log/info "IC2 not detected - EU conversion disabled")
      false)))

(defn init-ic2-energy!
  "Initialize IC2 energy integration.

  Called during mod initialization to set up IC2 integration if available.
  This is a no-op if IC2 is not present."
  []
  (when (ic2-available?)
    (log/info "Initializing IC2 energy integration...")
    (register-ic2-capability!)
    (log/debug "IC2 energy integration initialized")))

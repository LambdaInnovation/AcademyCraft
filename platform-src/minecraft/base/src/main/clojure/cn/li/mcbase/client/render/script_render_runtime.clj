(ns cn.li.mcbase.client.render.script-render-runtime
  "ScriptRender runtime cache + kill-switch.

  This namespace is queried by Java dispatchers to decide whether a renderer-id
  should use the scripted rendering path." 
  (:require [cn.li.mcbase.client.render.script-render-compiler :as compiler]
            [cn.li.platform.neutral.client-render :as script-abi]
            [cn.li.platform.neutral.config :as render-config]
            [cn.li.platform.neutral.client-render :as registry]
            [cn.li.mcmod.runtime.deferred :as deferred]
            [cn.li.mcmod.util.log :as log]))

(defn default-script-render-runtime-state
  []
  {:scripted-render-enabled? true
   :draw-plan-cache {}
   :renderer-overrides {}
   :config-initialized? false})

(defn create-script-render-runtime
  ([]
   (create-script-render-runtime {}))
  ([initial-state]
   {:state (atom (merge (default-script-render-runtime-state)
                        initial-state))}))

(def ^:private default-script-render-runtime-holder
  (deferred/deferred #(create-script-render-runtime)))

(def ^:private script-render-runtime-override
  "Plain root var, nil in production. Test-only swap target for
   call-with-script-render-runtime — replaces the prior ^:dynamic +
   binding pair. Single-threaded test execution only."
  nil)

(defn current-script-render-runtime
  []
  (or script-render-runtime-override
      @default-script-render-runtime-holder))

(defn call-with-script-render-runtime
  [runtime f]
  (let [prev script-render-runtime-override]
    (alter-var-root #'script-render-runtime-override (constantly runtime))
    (try
      (f)
      (finally
        (alter-var-root #'script-render-runtime-override (constantly prev))))))

(defmacro with-script-render-runtime
  [runtime & body]
  `(call-with-script-render-runtime ~runtime (fn [] ~@body)))

(defn script-render-runtime-state-atom
  []
  (:state (current-script-render-runtime)))

(defn script-render-runtime-state-snapshot
  []
  @(script-render-runtime-state-atom))

(defn update-script-render-runtime!
  [f & args]
  (apply swap! (script-render-runtime-state-atom) f args))

(defn clear-script-render-runtime!
  []
  (reset! (script-render-runtime-state-atom)
          (default-script-render-runtime-state))
  nil)

(defn reset-script-render-runtime-for-test!
  ([]
   (clear-script-render-runtime!))
  ([state]
   (reset! (script-render-runtime-state-atom)
           (merge (default-script-render-runtime-state)
                  state))
   nil))

(defn- ensure-config-initialized!
  []
  (let [state-atom (script-render-runtime-state-atom)]
    (when-not (:config-initialized? @state-atom)
      (locking state-atom
        (when-not (:config-initialized? @state-atom)
          (try
            (render-config/init-descriptors!)
            (swap! state-atom assoc :config-initialized? true)
            (catch Throwable t
              (swap! state-atom assoc :config-initialized? false)
              (throw t)))))))
  nil)

;; Scripted-effect kind membership is now delegated to script-abi/supported-kinds
;; so content modules only need to call script-abi/register-scripted-effect-kind!.
;; This atom is kept for runtime caching of the check; it is refreshed lazily.

(def ^:private scripted-marker-kinds
  #{:billboard-cross})

(def ^:private scripted-ray-kinds
  #{:ray-composite
    :ray-composite-lite})

(defn scripted-render-enabled?
  []
  (ensure-config-initialized!)
  (and (:scripted-render-enabled? (script-render-runtime-state-snapshot))
       (render-config/script-render-enabled?)))

(defn set-scripted-render-enabled!
  [enabled?]
  (let [enabled? (boolean enabled?)]
    (update-script-render-runtime! assoc :scripted-render-enabled? enabled?)
    (when-not enabled?
      (update-script-render-runtime! assoc :draw-plan-cache {}))
    (:scripted-render-enabled? (script-render-runtime-state-snapshot))))

(defn set-renderer-enabled!
  [renderer-id enabled?]
  (ensure-config-initialized!)
  (when (and (string? renderer-id) (seq renderer-id))
    (update-script-render-runtime! update :renderer-overrides assoc renderer-id (boolean enabled?)))
  nil)

(defn clear-renderer-overrides!
  []
  (ensure-config-initialized!)
  (update-script-render-runtime! assoc :renderer-overrides {})
  nil)

(defn clear-cache!
  []
  (update-script-render-runtime! assoc :draw-plan-cache {})
  nil)

(defn cache-size
  []
  (count (:draw-plan-cache (script-render-runtime-state-snapshot))))

(defn rebuild-cache!
  []
  (if-not (scripted-render-enabled?)
    (do
      (clear-cache!)
      {})
    (let [compiled (compiler/compile-profiles (registry/snapshot))]
      (update-script-render-runtime! assoc :draw-plan-cache compiled)
      (log/debug "ScriptRender cache rebuilt, size=" (count compiled))
      compiled)))

(defn get-draw-plan
  [renderer-id]
  (when (and (scripted-render-enabled?)
             (string? renderer-id)
             (seq renderer-id))
    (or (get-in (script-render-runtime-state-snapshot) [:draw-plan-cache renderer-id])
        (when-let [profile (registry/get-profile renderer-id)]
          (let [plan (compiler/compile-profile profile)]
            (update-script-render-runtime! update :draw-plan-cache assoc renderer-id plan)
            plan)))))

(defn- normalize-render-kind
  [render-kind]
  (cond
    (keyword? render-kind) render-kind
    (string? render-kind) (keyword render-kind)
    :else :effect))

(defn draw-plan-kind
  [renderer-id]
  (when-let [plan (get-draw-plan renderer-id)]
    (when-let [kind (:kind plan)]
      (name kind))))

(defn draw-plan-renderer-key
  "Return the platform-neutral renderer key for the draw-plan's kind.
   Resolves through script-abi/kind-renderer-key mapping so the Java dispatcher
   can switch on neutral keys instead of content-owned kind strings."
  [renderer-id]
  (when-let [plan (get-draw-plan renderer-id)]
    (when-let [kind (:kind plan)]
      (when-let [rk (script-abi/resolve-kind-renderer-key kind)]
        (name rk)))))

(defn draw-plan-param-double
  [renderer-id param-key default-value]
  (let [k (cond
            (keyword? param-key) param-key
            (string? param-key) (keyword param-key)
            :else nil)
        value (when (and k (some? renderer-id))
                (get-in (get-draw-plan renderer-id) [:params k]))]
    (double (if (number? value) value default-value))))

(defn draw-plan-param-int
  [renderer-id param-key default-value]
  (let [k (cond
            (keyword? param-key) param-key
            (string? param-key) (keyword param-key)
            :else nil)
        value (when (and k (some? renderer-id))
                (get-in (get-draw-plan renderer-id) [:params k]))]
    (int (if (number? value) value default-value))))

(defn draw-plan-param-string
  [renderer-id param-key default-value]
  (let [k (cond
            (keyword? param-key) param-key
            (string? param-key) (keyword param-key)
            :else nil)
        value (when (and k (some? renderer-id))
                (get-in (get-draw-plan renderer-id) [:params k]))]
    (if (string? value) value default-value)))

(defn use-scripted-renderer?
  [renderer-id render-kind]
  (let [{:keys [renderer-overrides]} (script-render-runtime-state-snapshot)
        kind (normalize-render-kind render-kind)
        override (get renderer-overrides renderer-id ::unset)
        config-disabled? (contains? (render-config/disabled-renderer-ids) renderer-id)
        override-allowed? (and (not= override false)
                               (not config-disabled?))
        plan (get-draw-plan renderer-id)]
    (and (some? plan)
         (:enabled? plan)
         override-allowed?
         (case kind
           :effect (contains? (script-abi/supported-kinds) (:kind plan))
           :marker (contains? scripted-marker-kinds (:kind plan))
           :ray (contains? scripted-ray-kinds (:kind plan))
           false))))

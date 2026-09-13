(ns cn.li.mcbase.runtime.spi.server-context
  "Shared server-context SPI for loader-specific runtime adapters.")

(def ^:private spi-state
  "Single atom backing SPI impl + lifecycle callback registration.
   Lock-free CAS updates replace the prior ^:dynamic vars + Object lock."
  (atom {:impl nil
         :runtime nil
         :callbacks {:available [] :unavailable []}}))

(defn- server-context-impl
  []
  (:impl @spi-state))

(defn- lifecycle-callbacks
  []
  (:callbacks @spi-state))

(defn reset-server-context-spi-for-test!
  []
  (reset! spi-state {:impl nil :runtime nil :callbacks {:available [] :unavailable []}})
  nil)

(defn current-server-runtime
  "Return the runtime owned by the current server, if one has been installed.

  Platform tick adapters call this once per server tick, never once per
  player.  Runtime consumers receive the resulting handle explicitly."
  []
  (:runtime @spi-state))

(defn install-server-runtime!
  [runtime]
  (when-not runtime
    (throw (IllegalArgumentException. "install-server-runtime! requires runtime")))
  (swap! spi-state assoc :runtime runtime)
  runtime)

(defn clear-server-runtime!
  []
  (let [[old _] (swap-vals! spi-state assoc :runtime nil)]
    (:runtime old)))

(defn- register-callback!
  [callback-key callback-id callback]
  (let [[old new] (swap-vals! spi-state
                    (fn [state]
                      (let [existing (get-in state [:callbacks callback-key] [])]
                        (if (some #(= (:id %) callback-id) existing)
                          state
                          (update-in state [:callbacks callback-key]
                                     (fn [cbs] (conj (vec cbs) {:id callback-id :callback callback})))))))]
    (not= old new)))

(defn- run-callbacks!
  [callback-key value]
  (doseq [{:keys [callback]} (get (lifecycle-callbacks) callback-key)]
    (callback value)))

(defn register-server-context-impl!
  [{:keys [get-current-server install!] :as impl}]
  (when-not (fn? get-current-server)
    (throw (ex-info "server-context SPI requires :get-current-server fn" {:impl impl})))
  (swap! spi-state assoc :impl {:get-current-server get-current-server
                                :install! install!})
  nil)

(defn server-state
  []
  (cond
    (nil? (server-context-impl)) :unregistered
    (some? ((:get-current-server (server-context-impl)))) :available
    :else :unavailable))

(defn install-server-context!
  []
  (when-let [install! (:install! (server-context-impl))]
    (install!))
  nil)

(defn get-current-server
  []
  (if-let [f (:get-current-server (server-context-impl))]
    (f)
    nil))

(defn require-current-server
  []
  (or (get-current-server)
      (throw (ex-info "Server context unavailable; ensure platform runtime installed server-context SPI"
                      {:hint "Call install-server-context! during runtime init"}))))

(defn on-server-available!
  ([callback]
   (on-server-available! callback callback))
  ([callback-id callback]
   (when-not (fn? callback)
     (throw (ex-info "server-context available callback must be a function" {:callback callback})))
   (when (register-callback! :available callback-id callback)
     (when-let [server (get-current-server)]
       (callback server)))
   nil))

(defn on-server-unavailable!
  ([callback]
   (on-server-unavailable! callback callback))
  ([callback-id callback]
   (when-not (fn? callback)
     (throw (ex-info "server-context unavailable callback must be a function" {:callback callback})))
   (register-callback! :unavailable callback-id callback)
   nil))

(defn notify-server-available!
  [server]
  (run-callbacks! :available server)
  nil)

(defn notify-server-unavailable!
  ([]
   (notify-server-unavailable! nil))
  ([server]
   (run-callbacks! :unavailable server)
   (clear-server-runtime!)
   nil))

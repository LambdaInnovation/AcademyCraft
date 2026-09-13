(ns cn.li.mcbase.client.session-cleanup-core
  "Shared client owner cleanup/orchestration for disconnects and server switches.

  Version modules install :clear-walk-speed! (level-renderer) before use."
  (:require [cn.li.mcbase.client.overlay.state :as overlay-state]
            [cn.li.mcbase.gui.reactive.overlay-host-core :as overlay-host]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.runtime.deferred :as deferred]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcver McClientAccess]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.player LocalPlayer]))

(defonce ^:private hooks-atom (atom nil))

(defn install-session-cleanup-hooks!
  "Install map with :clear-walk-speed! (fn [owner local-player])."
  [m]
  (reset! hooks-atom m)
  m)

(defn- hooks []
  (let [m @hooks-atom]
    (when (nil? m)
      (throw (IllegalStateException. "session-cleanup hooks not installed")))
    m))

(def ^:private default-cleanup-state
  {:connection-key nil
   :owner nil})

(defn create-session-cleanup-runtime
  []
  {::runtime ::session-cleanup-runtime
   :lifecycle-state* (atom default-cleanup-state)})

(def ^:private default-session-cleanup-runtime-holder
  (deferred/deferred #(create-session-cleanup-runtime)))

(def ^:private session-cleanup-runtime-override
  "Plain root var, nil in production. Test-only swap target."
  nil)

(defn- session-cleanup-runtime?
  [runtime]
  (and (map? runtime)
       (= ::session-cleanup-runtime (::runtime runtime))
       (some? (:lifecycle-state* runtime))))

(defn call-with-session-cleanup-runtime
  [runtime f]
  (when-not (session-cleanup-runtime? runtime)
    (throw (ex-info "Expected session cleanup runtime"
                    {:runtime runtime})))
  (let [prev session-cleanup-runtime-override]
    (alter-var-root #'session-cleanup-runtime-override (constantly runtime))
    (try
      (f)
      (finally
        (alter-var-root #'session-cleanup-runtime-override (constantly prev))))))

(defmacro with-session-cleanup-runtime
  [runtime & body]
  `(call-with-session-cleanup-runtime ~runtime (fn [] ~@body)))

(defn- current-session-cleanup-runtime
  []
  (or session-cleanup-runtime-override
      @default-session-cleanup-runtime-holder))

(defn- lifecycle-state-atom
  []
  (:lifecycle-state* (current-session-cleanup-runtime)))

(defn cleanup-state-snapshot
  []
  @(lifecycle-state-atom))

(defn reset-cleanup-state-for-test!
  ([]
   (reset-cleanup-state-for-test! default-cleanup-state))
  ([snapshot]
   (reset! (lifecycle-state-atom) {:connection-key (:connection-key snapshot)
                                   :owner (:owner snapshot)})
   nil))

(defn- local-player
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (.player mc)))

(defn- close-current-screen!
  []
  (McClientAccess/closeScreen (Minecraft/getInstance))
  nil)

(defn clear-owner-state!
  ([owner]
   (clear-owner-state! owner {}))
  ([owner {:keys [clear-owner-input-state!]
           :or {clear-owner-input-state! nil}}]
   (let [owner* (or owner {})
         session-id (:client-session-id owner*)]
     (close-current-screen!)
     (when (fn? clear-owner-input-state!)
       (clear-owner-input-state! owner*))
     (runtime-hooks/client-clear-owner-state! owner*)
     (overlay-state/clear-client-activated! owner*)
     (overlay-host/dispose-overlay! (:client-session-id owner*))
     ((:clear-walk-speed! (hooks)) owner* ^LocalPlayer (local-player))
     (when session-id
       (net-client/clear-client-session-state! session-id))
     nil)))

(defn tick-connection-change!
  ([]
   (tick-connection-change! {}))
  ([opts]
   (let [current-connection-key (client-session/connection-key)
         current-owner (client-session/current-local-player-owner)
         {:keys [connection-key owner]} (cleanup-state-snapshot)]
     (when (and owner
                (not= connection-key current-connection-key))
       (try
         (clear-owner-state! owner opts)
         (catch Exception e
           (log/stacktrace "Failed to clear client owner state during connection transition" e))))
     (reset! (lifecycle-state-atom) {:connection-key current-connection-key
                                     :owner current-owner})
     nil)))

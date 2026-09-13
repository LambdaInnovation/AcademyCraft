(ns cn.li.fabric262.adapter.server-context
  "Fabric runtime server holder for adapter-style runtime APIs."
  (:require [cn.li.mcbase.runtime.spi.server-context :as server-context-spi]
            [cn.li.mcmod.runtime.deferred :as deferred]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.fabric.api.event.lifecycle.v1 ServerLifecycleEvents
            ServerLifecycleEvents$ServerStarted
            ServerLifecycleEvents$ServerStopped]
           [net.minecraft.server MinecraftServer]))

(defn create-server-context-runtime
  ([]
   (create-server-context-runtime nil))
  ([initial-server]
   {:current-server (atom initial-server)}))

(def ^:private default-server-context-runtime-holder
  (deferred/deferred #(create-server-context-runtime)))

(def ^:private server-context-runtime-override
  "Plain root var, nil in production. Test-only swap target for
   call-with-server-context-runtime — replaces the prior ^:dynamic +
   binding pair. Single-threaded test execution only."
  nil)

(defn current-server-context-runtime
  []
  (or server-context-runtime-override
      @default-server-context-runtime-holder))

(defn call-with-server-context-runtime
  [runtime f]
  (let [prev server-context-runtime-override]
    (alter-var-root #'server-context-runtime-override (constantly runtime))
    (try
      (f)
      (finally
        (alter-var-root #'server-context-runtime-override (constantly prev))))))

(defmacro with-server-context-runtime
  [runtime & body]
  `(call-with-server-context-runtime ~runtime (fn [] ~@body)))

(defn current-server-atom
  []
  (:current-server (current-server-context-runtime)))

(defn current-server-snapshot
  []
  @(current-server-atom))

(defn- set-current-server!
  [server]
  (reset! (current-server-atom) server)
  server)

(defn clear-current-server!
  []
  (set-current-server! nil)
  nil)

(defn reset-current-server-for-test!
  ([]
   (clear-current-server!))
  ([server]
   (set-current-server! server)
   nil))

(declare install-server-context!)

(defn get-server
  ^MinecraftServer
  []
  (current-server-snapshot))

(defn- register-server-context-spi!
  []
  (install/framework-once! ::spi-registered
    #(server-context-spi/register-server-context-impl!
       {:get-current-server get-server
        :install! install-server-context!}))
  nil)

(defn install-server-context!
  []
  (register-server-context-spi!)
  ;; Fabric's ServerLifecycleEvents bus is a static JVM-process singleton —
  ;; re-registering listeners on Framework reinjection would double-fire
  ;; after every world reload, so this is process-scoped, not framework-scoped.
  (install/process-once! ::server-lifecycle-listeners
    #(do
       (.register ServerLifecycleEvents/SERVER_STARTED
                  (reify ServerLifecycleEvents$ServerStarted
                    (onServerStarted [_ server]
                      (set-current-server! server)
                      (server-context-spi/notify-server-available! server))))
       (.register ServerLifecycleEvents/SERVER_STOPPED
                  (reify ServerLifecycleEvents$ServerStopped
                    (onServerStopped [_ server]
                      (clear-current-server!)
                      (server-context-spi/notify-server-unavailable! server))))
       (log/debug "Fabric runtime server context installed")))
  nil)

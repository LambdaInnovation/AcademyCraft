(ns cn.li.fabric1201.integration.side
  "Runtime side detection for Fabric 1.20.1."
  (:require [cn.li.mcmod.runtime.require-lock :as require-lock]
            [cn.li.mcmod.util.log :as log])
  (:import [net.fabricmc.api EnvType]
           [net.fabricmc.loader.api FabricLoader]))

(defn- env-type
  []
  (try
    (.getEnvironmentType (FabricLoader/getInstance))
    (catch Throwable _ nil)))

(defn client-side?
  "Returns true if running on physical client."
  []
  (= EnvType/CLIENT (env-type)))

(defn server-side?
  "Returns true if running on dedicated server."
  []
  (= EnvType/SERVER (env-type)))

(defn require-client-ns
  "Safely require client-only namespace."
  [ns-sym]
  (when (client-side?)
    (try
      (require-lock/safe-require ns-sym)
      true
      (catch Throwable t
        (log/warn "Failed requiring client namespace" ns-sym (ex-message t))
        nil))))

(defn resolve-client-fn
  "Safely resolve client-only function."
  [ns-sym fn-name]
  (when (require-client-ns ns-sym)
    (try
      (ns-resolve ns-sym fn-name)
      (catch Throwable t
        (log/warn "Failed resolving client function" ns-sym fn-name (ex-message t))
        nil))))

(defn call-client-fn
  "Safely call client-only function."
  [ns-sym fn-name & args]
  (when-let [f (resolve-client-fn ns-sym fn-name)]
    (apply f args)))

(defmacro when-client
  [& body]
  `(when (cn.li.fabric1201.integration.side/client-side?)
     ~@body))

(defmacro when-server
  [& body]
  `(when (cn.li.fabric1201.integration.side/server-side?)
     ~@body))

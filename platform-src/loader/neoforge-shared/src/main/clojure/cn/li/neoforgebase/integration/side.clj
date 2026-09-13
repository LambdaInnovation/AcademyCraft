(ns cn.li.neoforgebase.integration.side
  "Runtime side detection for NeoForge loaders (1.21.1 + 26.2)."
  (:require [cn.li.mcmod.runtime.require-lock :as require-lock]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.neoforgebase.bridge DistAccess]))

(defn client-side?
  []
  (DistAccess/isClient))

(defn server-side?
  []
  (DistAccess/isDedicatedServer))

(defn require-client-ns
  [ns-sym]
  (when (client-side?)
    (try
      (require-lock/safe-require ns-sym)
      ns-sym
      (catch Exception e
        (log/stacktrace (str "Failed to load client namespace " ns-sym) e)
        nil))))

(defn resolve-client-fn
  [var-sym]
  (when (client-side?)
    (try
      (require-lock/safe-require (symbol (namespace var-sym)))
      (when-let [v (find-var var-sym)]
        (when (bound? v) @v))
      (catch Exception e
        (log/stacktrace (str "Failed to resolve client function " var-sym) e)
        nil))))

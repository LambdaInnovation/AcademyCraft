(ns cn.li.ac.ability.client.fx-registry
  "Frozen client FX channel registry with direct HashMap dispatch."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]))

(defonce ^:private ^HashMap handlers (HashMap.))
(defonce ^:private frozen (boolean-array 1))

(defn create-fx-registry-runtime []
  {:handlers handlers :frozen frozen})

(defn- assert-registry-open! []
  (when (aget ^booleans frozen 0)
    (throw (ex-info "FX channel registry is frozen" {}))))

(defn register-fx-channel! [channel-key handler-fn]
  (when-not (and (keyword? channel-key) (fn? handler-fn))
    (throw (IllegalArgumentException.
             "register-fx-channel!: channel-key must be keyword, handler-fn must be fn")))
  (assert-registry-open!)
  (when-not (.containsKey handlers channel-key)
    (.put handlers channel-key handler-fn))
  nil)

(defn register-fx-channels! [channel-keys handler-fn]
  (when-not (and (every? keyword? channel-keys) (fn? handler-fn))
    (throw (IllegalArgumentException.
             "register-fx-channels!: channel-keys must be keywords, handler-fn must be fn")))
  (doseq [channel-key channel-keys]
    (register-fx-channel! channel-key handler-fn))
  nil)

(defn freeze-fx-registry! []
  (aset-boolean ^booleans frozen 0 true)
  nil)

(defn fx-registry-snapshot []
  {:handlers (into {} handlers) :frozen? (aget ^booleans frozen 0)})

(defn reset-fx-registry-for-test! []
  (.clear handlers)
  (aset-boolean ^booleans frozen 0 false)
  nil)

(defn registered-channels []
  (set (.keySet handlers)))

(defn dispatch-fx-channel! [ctx-id channel payload]
  (if-let [handler-fn (.get handlers channel)]
    (do (handler-fn ctx-id channel payload) true)
    (do (log/debug "No FX handler for channel" channel) false)))

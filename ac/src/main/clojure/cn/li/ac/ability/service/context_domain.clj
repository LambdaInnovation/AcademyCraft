(ns cn.li.ac.ability.service.context-domain
  "Pure context lifecycle state-machine helpers."
  (:require [clojure.string :as str]))

(def status-constructed :constructed)
(def status-alive :alive)
(def status-terminated :terminated)

(def keepalive-timeout-ms
  "Server-side keepalive timeout threshold in milliseconds. Frozen at runtime
  bootstrap; shared by context-manager (policy) and context-dispatcher
  (deadline-bucket scheduling) to avoid a manager->dispatcher dependency."
  1500)

(def terminated-context-grace-ms
  "Grace window before terminated contexts are purged from the registry.
  Frozen at runtime bootstrap."
  1000)

(defn status-valid-transition?
  [from to]
  (case [from to]
    [:constructed :alive] true
    [:constructed :terminated] true
    [:alive :terminated] true
    false))

(defn active-context?
  [ctx]
  (not= status-terminated (:status ctx)))

(defn transition-to-alive
  [ctx server-id now-ms]
  (if (status-valid-transition? (:status ctx) status-alive)
    (assoc ctx
           :status status-alive
           :server-id server-id
           :message-buffer []
           :last-keepalive-ms now-ms
           :terminated-at-ms nil)
    ctx))

(defn transition-to-terminated
  [ctx now-ms]
  (if (= (:status ctx) status-terminated)
    ctx
    (assoc ctx :status status-terminated :terminated-at-ms now-ms)))

(defn positive-long-prop
  [prop-key default-value]
  (let [raw (System/getProperty prop-key)]
    (if (and raw (not (str/blank? raw)))
      (try
        (let [parsed (Long/parseLong raw)]
          (if (pos? parsed) parsed default-value))
        (catch Exception _
          default-value))
      default-value)))

(ns cn.li.ac.energy.service.subscription
  "Subscription registry helpers for energy change notifications."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [java.util UUID]))

(defn subscription-id []
  (str (UUID/randomUUID)))

(defn subscribe!
  "Register a callback for a provider id and return the subscription id."
  [subscriptions id callback]
  (let [sid (subscription-id)]
    (swap! subscriptions assoc sid {:id id :callback callback})
    sid))

(defn unsubscribe!
  "Remove a subscription by id."
  [subscriptions sid]
  (swap! subscriptions dissoc sid)
  nil)

(defn notify!
  "Notify subscribers whose provider id matches `source-id`."
  [subscriptions source-id old-value new-value]
  (doseq [[_ {:keys [id callback]}] @subscriptions]
    (when (= id source-id)
      (try
        (callback old-value new-value)
        (catch Exception e
          (log/warn (str "Energy subscription callback failed for " source-id ": " (.getMessage e))))))))

(defn clear!
  "Clear all subscriptions."
  [subscriptions]
  (reset! subscriptions {})
  nil)

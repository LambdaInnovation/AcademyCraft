(ns cn.li.mcmod.integration.energy-hooks
  "Energy integration hook registry.

  State is stored in Framework [:registry :integrations :energy].
  Registered hooks start empty; readers apply built-in defaults when content has
  not claimed a slot yet."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private default-forge-energy-conversion-rate (fn [] 1.0))
(def ^:private default-ic2-energy-conversion-rate (fn [] 1.0))

(def ^:private energy-path [:registry :integrations :energy])

(defn- registered-hooks
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom energy-path {})
    {}))

(defn register-energy-integration-hooks!
  [hooks]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in energy-path
           (fn [current]
             (let [base (or current {})]
               (reduce-kv (fn [m k v]
                            (if (and (contains? m k) (not= (get m k) v))
                              (throw (ex-info "Conflicting energy integration hook" {:key k}))
                              (assoc m k v)))
                          base hooks)))))
  nil)

(defn reset-energy-integration-hooks-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in energy-path {}))
  nil)

(defn forge-energy-conversion-rate []
  ((or (:forge-energy-conversion-rate (registered-hooks))
       default-forge-energy-conversion-rate)))

(defn ic2-energy-conversion-rate []
  ((or (:ic2-energy-conversion-rate (registered-hooks))
       default-ic2-energy-conversion-rate)))

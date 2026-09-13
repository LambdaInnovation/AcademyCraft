(ns cn.li.mcmod.command.runtime-hooks
  "Content-owned command initialization hooks."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private noop (fn [] nil))
(def ^:private cmd-hooks-path [:registry :commands :hooks])

(defn- registered-hooks
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom cmd-hooks-path {})
    {}))

(defn register-command-hooks!
  [hooks]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in cmd-hooks-path
           (fn [current]
             (let [base (or current {})]
               (reduce-kv (fn [m k v]
                            (if (and (contains? m k) (not= (get m k) v))
                              (throw (ex-info "Conflicting command hook" {:key k}))
                              (assoc m k v)))
                          base hooks)))))
  nil)

(defn reset-command-hooks-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in cmd-hooks-path {}))
  nil)

(defn init-commands!
  []
  ((or (:init-commands! (registered-hooks)) noop)))

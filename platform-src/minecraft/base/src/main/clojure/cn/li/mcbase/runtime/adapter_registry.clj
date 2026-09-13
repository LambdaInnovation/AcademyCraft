(ns cn.li.mcbase.runtime.adapter-registry
  "Shared contract helpers for declarative runtime adapter installation."
  (:require [cn.li.mcmod.util.log :as log]))

(defn step
  "Create a standard runtime installation step descriptor."
  [id install-fn]
  {:id id
   :install-fn install-fn})

(defn run-install-steps!
  "Execute declarative install steps in order with lightweight logging."
  [label install-steps]
  (doseq [{:keys [id install-fn]} install-steps]
    (log/info "[RUNTIME] install step" {:label label :step id})
    (install-fn))
  nil)

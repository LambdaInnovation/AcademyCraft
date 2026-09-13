(ns cn.li.mcbase.platform.menu-inventory-ops
  (:require [cn.li.mcmod.framework :as fw]))

(def menu-inventory-ops-keys #{:inventory-owner :menu-container-id})

(defn install-menu-inventory-ops! [impl-map _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :menu-inventory-ops] impl-map)) nil)

(defn- call [k & args] (when-let [f (get-in @(fw/fw-atom) [:platform :menu-inventory-ops k])] (apply f args)))

(defn inventory-owner   [adapter inventory] (call :inventory-owner adapter inventory))
(defn menu-container-id [adapter menu]      (call :menu-container-id adapter menu))

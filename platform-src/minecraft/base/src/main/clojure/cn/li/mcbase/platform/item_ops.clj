(ns cn.li.mcbase.platform.item-ops
  (:require [cn.li.mcmod.framework :as fw]))

(def item-ops-keys #{:item-registry-name :block-registry-name :item-stack-of
                      :create-item-stack-by-id :item-stack-empty?})

(defn install-item-platform-ops! [impl-map _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :item-ops-platform] impl-map)) nil)

(defn- call [k & args] (when-let [f (get-in @(fw/fw-atom) [:platform :item-ops-platform k])] (apply f args)))

(defn item-registry-name       [adapter item] (call :item-registry-name adapter item))
(defn block-registry-name      [adapter block] (call :block-registry-name adapter block))
(defn item-stack-of            [adapter nbt] (call :item-stack-of adapter nbt))
(defn create-item-stack-by-id  [adapter item-id count] (call :create-item-stack-by-id adapter item-id count))
(defn item-stack-empty?        [adapter stack] (call :item-stack-empty? adapter stack))

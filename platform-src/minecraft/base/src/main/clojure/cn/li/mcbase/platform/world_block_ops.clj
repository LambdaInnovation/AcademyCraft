(ns cn.li.mcbase.platform.world-block-ops
  (:require [cn.li.mcmod.framework :as fw]))

(def world-block-ops-keys #{:world-place-block-by-id})

(defn install-world-block-ops! [impl-map _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :world-block-ops] impl-map)) nil)

(defn- call [k & args] (when-let [f (get-in @(fw/fw-atom) [:platform :world-block-ops k])] (apply f args)))

(defn world-place-block-by-id [adapter level block-id pos flags] (call :world-place-block-by-id adapter level block-id pos flags))

(ns cn.li.mcbase.platform.class-access
  (:require [cn.li.mcmod.framework :as fw]))

(def class-access-keys #{:entity-class :player-class :server-player-class :local-player-class
                          :inventory-class :menu-class :item-stack-class :item-class
                          :block-state-class :level-class :scripted-be-class})

(defn install-class-access!
  [impl-map _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :class-access] impl-map)) nil)

(defn- call [k] (get-in @(fw/fw-atom) [:platform :class-access k]))

(defn entity-class        [] (call :entity-class))
(defn player-class        [] (call :player-class))
(defn server-player-class [] (call :server-player-class))
(defn local-player-class  [] (call :local-player-class))
(defn inventory-class     [] (call :inventory-class))
(defn menu-class          [] (call :menu-class))
(defn item-stack-class    [] (call :item-stack-class))
(defn item-class          [] (call :item-class))
(defn block-state-class   [] (call :block-state-class))
(defn level-class         [] (call :level-class))
(defn scripted-be-class   [] (call :scripted-be-class))

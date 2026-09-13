(ns cn.li.mcbase.platform.player-ops
  (:require [cn.li.mcmod.framework :as fw]))

(def player-ops-keys #{:player-level :player-container-menu :count-player-item-by-id
                        :consume-player-item-by-id! :drop-player-main-hand-item-at!
                        :give-player-item-stack! :spawn-entity-by-id! :spawn-tracked-entity-by-id!
                        :raytrace-block})

(defn install-player-ops-platform! [impl-map _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :player-ops-platform] impl-map)) nil)

(defn- call [k & args] (when-let [f (get-in @(fw/fw-atom) [:platform :player-ops-platform k])] (apply f args)))

(defn player-level                  [adapter player] (call :player-level adapter player))
(defn player-container-menu         [adapter player] (call :player-container-menu adapter player))
(defn count-player-item-by-id       [adapter player item-id] (call :count-player-item-by-id adapter player item-id))
(defn consume-player-item-by-id!    [adapter player item-id amount] (call :consume-player-item-by-id! adapter player item-id amount))
(defn drop-player-main-hand-item-at! [adapter player amount x y z] (call :drop-player-main-hand-item-at! adapter player amount x y z))
(defn give-player-item-stack!       [adapter player stack] (call :give-player-item-stack! adapter player stack))
(defn spawn-entity-by-id!           [adapter player entity-id speed] (call :spawn-entity-by-id! adapter player entity-id speed))
(defn spawn-tracked-entity-by-id!   [adapter player entity-id speed] (call :spawn-tracked-entity-by-id! adapter player entity-id speed nil))
(defn spawn-tracked-entity-by-id-with-life! [adapter player entity-id speed life] (call :spawn-tracked-entity-by-id! adapter player entity-id speed life))
(defn raytrace-block                [adapter player reach fluid-source-only?] (call :raytrace-block adapter player reach fluid-source-only?))

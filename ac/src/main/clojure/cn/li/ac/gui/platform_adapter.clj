(ns cn.li.ac.gui.platform-adapter
  "AC-owned GUI platform callback installation.

  Loader modules must not import this namespace. AC calls `install-into-mcmod!`
  during content load to register container behavior into mcmod."
  (:require [cn.li.ac.gui.slot-validators :as slot-validators]
            [cn.li.ac.wireless.gui.container.dispatcher :as dispatcher]
            [cn.li.mcmod.gui.adapter.platform-registry :as platform-registry]
            [cn.li.mcmod.gui.registry :as gui-registry]))

(defn- player-inventory-start-for
  [container]
  (if-let [gui-id (some-> container dispatcher/get-container-type gui-registry/get-gui-id-for-type)]
    (let [[main-start _] (or (gui-registry/get-slot-range gui-id :player-main) [0 -1])
          [hotbar-start _] (or (gui-registry/get-slot-range gui-id :player-hotbar) [0 -1])
          starts (filter #(and (integer? %) (<= 0 %)) [main-start hotbar-start])]
      (if (seq starts) (apply min starts) 0))
    0))

(defn execute-quick-move-forge
  [_menu container slot-index _slot _stack]
  (let [player-inventory-start (player-inventory-start-for container)]
    (dispatcher/execute-quick-move container slot-index player-inventory-start)))

(defn get-gui-id-for-container
  [container]
  (let [container-type (dispatcher/get-container-type container)]
    (when (not= container-type :unknown)
      (gui-registry/get-gui-id-for-type container-type))))

(defn install-into-mcmod!
  "Install AC GUI callbacks into mcmod platform-registry."
  []
  (slot-validators/register-default-slot-validators!)
  (platform-registry/register-gui-platform-impl!
    {:server-menu-sync! dispatcher/server-menu-sync!
     :safe-validate dispatcher/safe-validate
     :safe-close! dispatcher/safe-close!
     :slot-count dispatcher/slot-count
     :slot-get-item dispatcher/slot-get-item
     :slot-set-item! dispatcher/slot-set-item!
     :slot-changed! dispatcher/slot-changed!
     :slot-can-place? dispatcher/slot-can-place?
     :execute-quick-move-forge execute-quick-move-forge
     :get-container-type dispatcher/get-container-type
     :get-gui-id-for-container get-gui-id-for-container})
  nil)

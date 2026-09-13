(ns cn.li.mcmod.runtime.tabbed-gui-provider
  "Neutral provider for the platform tabbed-menu facade."
  (:require [cn.li.mcmod.gui.tabbed-gui :as tabbed-gui]))

(defn runtime-provider
  [_]
  {:tabbed-container? #'tabbed-gui/tabbed-container?
   :slots-active? #'tabbed-gui/slots-active?
   :slots-active-for-menu? #'tabbed-gui/slots-active-for-menu?
   :detach-tab-sync! #'tabbed-gui/detach-tab-sync!})

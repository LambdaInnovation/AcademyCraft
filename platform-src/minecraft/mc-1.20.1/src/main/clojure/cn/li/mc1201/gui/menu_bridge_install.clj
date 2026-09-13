(ns cn.li.mc1201.gui.menu-bridge-install
  "Install versioned DelegatingCMenuBridge factory into shared menu proxy."
  (:require [cn.li.mcbase.gui.menu.proxy :as menu-proxy])
  (:import [cn.li.mc1201.shim DelegatingCMenuBridge]))

(defn install!
  []
  (menu-proxy/install-new-menu-bridge!
    (fn [menu-type window-id]
      (DelegatingCMenuBridge. menu-type (int window-id))))
  nil)

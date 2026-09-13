(ns cn.li.mc1211.bootstrap.platform-init
  "Install versioned installer/accessor hooks into shared platform-init."
  (:require [cn.li.mcbase.bootstrap.platform-init :as shared]
            [cn.li.mc1211.gui.menu-bridge-install :as menu-bridge-install]
            [cn.li.mc1211.bootstrap.installer-core :as core]
            [cn.li.mc1211.runtime.accessor-registry :as accessor-registry]))

(shared/install-platform-init-hooks!
  {:install-platform-core! core/install-platform-core!
   :install-platform-services! core/install-platform-services!
   :init-default-accessors! accessor-registry/init-default-accessors!})

(menu-bridge-install/install!)

(def install-platform-core! shared/install-platform-core!)
(def install-platform-services! shared/install-platform-services!)

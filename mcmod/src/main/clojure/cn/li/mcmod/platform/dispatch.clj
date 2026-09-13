;; Platform/version dispatch — stored in Framework [:platform :platform-version].
(ns cn.li.mcmod.platform.dispatch
  "Dynamic platform/version selector. Stored in Framework atom, no ThreadLocal."
  (:require [cn.li.mcmod.framework :as fw]))

(defn install-platform-version!
  [platform-key _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :platform-version] platform-key)) nil)

(defn current-platform-version
  []
  (get-in @(fw/fw-atom) [:platform :platform-version]))

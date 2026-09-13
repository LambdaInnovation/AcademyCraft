(ns cn.li.ac.block.imag-fusor.handlers
  "Imaginary Fusor network handlers.

  Imag Fusor reuses the :developer message domain handlers registered by the
  Developer block. The link!/unlink!/get-linked-node logic is tile-generic:
  `get-node-conn-by-receiver` works for any IWirelessReceiver tile.
  The GUI uses (wireless-tab/create-wireless-panel {:role :receiver}) which
  sends :developer messages → handled by developer's registered handlers."
  (:require [cn.li.mcmod.util.log :as log]))

(defn register-network-handlers!
  []
  ;; :developer domain handlers already registered by developer block.
  ;; Imag Fusor GUI uses (wireless-tab/create-wireless-panel {:role :receiver})
  ;; which sends :developer messages → handled by developer's registered handlers.
  (log/debug "Imaginary Fusor network handlers registered (reuses :developer domain)"))

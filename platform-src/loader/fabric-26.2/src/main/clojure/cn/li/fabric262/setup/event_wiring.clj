(ns cn.li.fabric262.setup.event-wiring
  "Fabric event wiring phase extracted from mod entry."
  (:require [cn.li.fabric262.integration.events :as events]))

(defn register-events!
  []
  (events/register-events)
  nil)

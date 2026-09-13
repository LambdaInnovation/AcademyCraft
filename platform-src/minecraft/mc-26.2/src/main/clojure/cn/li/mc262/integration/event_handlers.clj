(ns cn.li.mc262.integration.event-handlers
  "Thin re-export of cn.li.mcbase.integration.event-handlers."
  (:require [cn.li.mcbase.integration.event-handlers :as shared]))

(def handle-block-place shared/handle-block-place)
(def handle-block-break shared/handle-block-break)
(def handle-block-right-click shared/handle-block-right-click)

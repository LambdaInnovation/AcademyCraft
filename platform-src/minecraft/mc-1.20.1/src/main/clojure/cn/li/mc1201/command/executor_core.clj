(ns cn.li.mc1201.command.executor-core
  "Thin re-export of cn.li.mcbase.command.executor-core."
  (:require [cn.li.mcbase.command.executor-core :as shared]))

(def execute-send-message-action shared/execute-send-message-action)
(def grant-advancement! shared/grant-advancement!)
(def execute-grant-advancement-action shared/execute-grant-advancement-action)

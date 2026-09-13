(ns cn.li.mcmod.runtime.command-runtime-provider
  (:require [cn.li.mcmod.command.context :as context]
            [cn.li.mcmod.command.actions :as actions]
            [cn.li.mcmod.command.metadata :as metadata]
            [cn.li.mcmod.command.runtime-hooks :as hooks]))

(defn runtime-provider [_]
  {:create-context #'context/create-context
   :execute #'actions/execute
   :execute-action-impl #'actions/execute-action-impl
   :get-all-command-ids #'metadata/get-all-command-ids
   :get-command-spec #'metadata/get-command-spec
   :init-commands! #'hooks/init-commands!})

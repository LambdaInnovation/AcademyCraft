(ns cn.li.ac.ability.server.handlers.level-handler
	"Level-up request network handler."
	(:require [cn.li.ac.ability.server.handlers.common :as common]
						[cn.li.ac.ability.util.uuid :as uuid]
						[cn.li.ac.ability.service.command-runtime :as command-rt]))

(defn handle-level-up-request
	[_payload player]
	(let [uuid  (uuid/player-uuid player)
				session-id (common/current-server-session-id)
				_result (command-rt/run-command-in-session! session-id uuid {:command :level-up})]
		nil))
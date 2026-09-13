(ns cn.li.ac.ability.server.handlers.activation-handler
	"Activation toggle request network handler."
	(:require [cn.li.ac.ability.server.handlers.common :as common]
						[cn.li.ac.ability.util.uuid :as uuid]
						[cn.li.ac.ability.service.command-runtime :as command-rt]
						[cn.li.mcmod.util.log :as log]))

(defn handle-set-activated-request
	[{:keys [activated]} player]
	(let [uuid  (uuid/player-uuid player)
				session-id (common/current-server-session-id)
				state (common/get-state uuid)
				ability-data (:ability-data state)
				rd    (:resource-data state)
				requested (boolean activated)
				before (boolean (:activated rd))
				result (if (and requested (nil? (:category-id ability-data)))
						 {:state state :events [] :effects []}
						 (command-rt/run-command-in-session! session-id uuid {:command :set-activated
																	 :activated requested}))
				next-state (get result :state state)
				after (boolean (get-in next-state [:resource-data :activated]))]
		(log/debug "[V-TRACE][AC][SERVER][REQ-SET-ACTIVATED]"
							{:uuid uuid
							 :requested requested
							 :before before
							 :after after
							 :has-category? (some? (:category-id ability-data))
							 :events (count (:events result))})))
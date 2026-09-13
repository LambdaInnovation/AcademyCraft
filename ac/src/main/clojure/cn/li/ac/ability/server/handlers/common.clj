(ns cn.li.ac.ability.server.handlers.common
	"Shared handler helpers for ability server network handlers."
	(:require [cn.li.ac.ability.service.command-runtime :as command-rt]
						[cn.li.ac.ability.service.runtime-store :as store]
						[cn.li.mcmod.hooks.core :as runtime-hooks]
						[cn.li.mcmod.runtime.owner :as owner]))

(defn current-server-session-id
	[]
	(runtime-hooks/player-state-server-session-id))

(defn require-server-session-id
	[reason]
	(or (current-server-session-id)
			(runtime-hooks/require-player-state-server-session-id reason)))

(defn server-context-owner
	[player-uuid]
	(let [server-session-id (require-server-session-id "Server context owner")]
		{:logical-side :server
		 :server-session-id server-session-id
		 :player-uuid (str player-uuid)}))

(defn run-player-command!
	[owner player-uuid command & [opts]]
	(let [owner* (or owner (server-context-owner player-uuid))
				session-id (owner/store-session-id owner*)
				uuid (or (owner/player-uuid owner*) (str player-uuid))]
		(command-rt/run-command-in-session! session-id uuid command opts)))

(defn get-state
	[uuid]
	(let [session-id (current-server-session-id)]
		(when-not session-id
			(throw (ex-info "Server handler state read requires bound :server-session-id"
							{:uuid uuid
							 :player-state-owner (runtime-hooks/current-player-state-owner)})))
		(store/get-or-create-player-state! session-id uuid)))

(ns cn.li.ac.ability.server.handlers.input-handler
	"Input lifecycle request network handlers for ability contexts."
	(:require [cn.li.ac.ability.util.uuid :as uuid]
						[clojure.string :as str]
						[cn.li.ac.ability.service.command-runtime :as command-rt]
						[cn.li.ac.ability.service.context-manager :as ctx-mgr]
						[cn.li.ac.ability.service.context-dispatcher :as ctx]
						[cn.li.ac.ability.service.context-state :as ctx-rt]
						[cn.li.ac.ability.server.handlers.common :as handlers-common]))

(defn- valid-ctx-id?
	[ctx-id]
	(and (string? ctx-id) (not (str/blank? ctx-id))))

(defn- server-context-owner
	[player-uuid]
	(handlers-common/server-context-owner player-uuid))

(defn- resolve-owned-alive-context
	[ctx-id player]
	(let [player-uuid (uuid/player-uuid player)
			owner (when player-uuid (server-context-owner player-uuid))
			ctx-map (when (and owner (valid-ctx-id? ctx-id))
								(ctx/get-context owner ctx-id))]
		(cond
			(not (valid-ctx-id? ctx-id))
			{:ok? false :reason :payload-invalid}

			(nil? player-uuid)
			{:ok? false :reason :player-uuid-missing}

			(nil? ctx-map)
			{:ok? false :reason :ctx-not-found}

			(not= player-uuid (:player-uuid ctx-map))
			{:ok? false :reason :ctx-not-owner}

			(not= (:status ctx-map) ctx/STATUS-ALIVE)
			{:ok? false :reason :ctx-not-alive}

			:else
			{:ok? true :ctx ctx-map :owner owner})))

(defn- sync-keepalive-command!
	[owner ctx-id]
	(handlers-common/run-player-command!
	 owner
	 (:player-uuid owner)
	 {:command :touch-context-keepalive
		:ctx-id ctx-id
		:timestamp-ms (System/currentTimeMillis)}))

(defn- refresh-owned-alive-context!
	[ctx-id player]
	(let [{:keys [ok? owner]} (resolve-owned-alive-context ctx-id player)]
		(if ok?
			(do
				(sync-keepalive-command! owner ctx-id)
				owner)
			nil)))

(defn- dispatch-active-input!
	[ctx-id payload player dispatch-fn]
	(when-let [owner (refresh-owned-alive-context! ctx-id player)]
		(if (= ctx-rt/INPUT-ACTIVE (:input-state (ctx/get-context owner ctx-id)))
			(dispatch-fn owner ctx-id (assoc payload :player player) ctx-mgr/send-terminated-context!)
			nil)))

(defn handle-key-down-skill
	[{:keys [ctx-id] :as payload} player]
	(when-let [owner (refresh-owned-alive-context! ctx-id player)]
		(ctx-rt/handle-key-down! owner ctx-id (assoc payload :player player) ctx-mgr/send-terminated-context!)))

(defn handle-key-tick-skill
	[{:keys [ctx-id] :as payload} player]
	(dispatch-active-input! ctx-id payload player ctx-rt/handle-key-tick!))

(defn handle-key-up-skill
	[{:keys [ctx-id] :as payload} player]
	(dispatch-active-input! ctx-id payload player ctx-rt/handle-key-up!))

(defn handle-key-abort-skill
	[{:keys [ctx-id] :as payload} player]
	(when-let [owner (refresh-owned-alive-context! ctx-id player)]
		(ctx-rt/handle-key-abort! owner ctx-id (assoc payload :player player) ctx-mgr/send-terminated-context!)))

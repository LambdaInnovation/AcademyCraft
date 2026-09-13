(ns cn.li.ac.ability.server.handlers.context-handler
	"Context lifecycle request network handlers."
	(:require [cn.li.ac.ability.util.uuid :as uuid]
						[clojure.string :as str]
						[cn.li.ac.ability.service.command-runtime :as command-rt]
						[cn.li.ac.ability.service.context-manager :as ctx-mgr]
						[cn.li.ac.ability.service.context-dispatcher :as ctx]
						[cn.li.ac.ability.server.handlers.common :as handlers-common]))

(defn- valid-ctx-id?
	[ctx-id]
	(and (string? ctx-id) (not (str/blank? ctx-id))))

(defn- server-context-owner
	[player-uuid]
	(handlers-common/server-context-owner player-uuid))

(defn handle-begin-link-context
	[{:keys [ctx-id skill-id]} player]
	(cond
		(or (not (valid-ctx-id? ctx-id)) (not (keyword? skill-id)))
		nil

		(nil? (uuid/player-uuid player))
		nil

		:else
		(let [player-uuid (uuid/player-uuid player)
				owner (server-context-owner player-uuid)]
			(let [existing (ctx/get-context owner ctx-id)]
				(if (or (nil? existing)
						(= player-uuid (:player-uuid existing)))
					(ctx-mgr/establish-context! player-uuid ctx-id skill-id)
					nil)))))

(defn- resolve-owned-context
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

			:else
			{:ok? true :ctx ctx-map :owner owner})))

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

(defn handle-keepalive-context
	[{:keys [ctx-id]} player]
	(let [{:keys [ok? owner]} (resolve-owned-alive-context ctx-id player)]
		(if ok?
			(do
				(sync-keepalive-command! owner ctx-id))
			nil)))

(defn handle-terminate-context
	[{:keys [ctx-id]} player]
	(let [{:keys [ok? owner]} (resolve-owned-context ctx-id player)]
		(if ok?
			(ctx/terminate-context! owner ctx-id ctx-mgr/send-terminated-context!)
			nil)))

(defn handle-channel-context
	[{:keys [ctx-id channel payload]} player]
	(let [{:keys [ok? owner]} (if (keyword? channel)
													(resolve-owned-alive-context ctx-id player)
													{:ok? false :reason :payload-invalid})]
		(if ok?
			(do
				(sync-keepalive-command! owner ctx-id)
				(ctx/ctx-send-to-local! owner ctx-id channel payload))
			nil)))
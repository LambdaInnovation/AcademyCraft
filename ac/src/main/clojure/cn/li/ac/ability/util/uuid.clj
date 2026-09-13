(ns cn.li.ac.ability.util.uuid
	"Shared UUID extraction helpers for ability and related subsystems."
	(:require [cn.li.mcmod.platform.entity :as entity]))

(defn player-uuid
	"Safely get player UUID string. Returns nil when unavailable."
	[player]
	(some-> (try (entity/player-get-uuid player)
							 (catch Exception _ nil))
					str))
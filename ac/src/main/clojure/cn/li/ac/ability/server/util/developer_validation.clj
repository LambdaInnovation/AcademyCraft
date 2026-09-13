(ns cn.li.ac.ability.server.util.developer-validation
	"Validation helpers for developer controller stations used by ability learning handlers."
	(:require [cn.li.mcmod.platform.entity :as entity]
						[cn.li.mcmod.platform.position :as pos]
						[cn.li.mcmod.platform.be :as platform-be]
						[cn.li.ac.ability.domain.developer :as developer]))

(def ^:private max-station-distance 8.0)

(defn developer-type-for-tile
	[tile]
	(or (developer/developer-type-for-block-id (platform-be/get-block-id tile))
			:normal))

(defn dist-sq-ok-for-station?
	[player tile]
	(let [raw-pos (try (pos/block-pos tile) (catch Exception _ nil))]
		(boolean
			(when raw-pos
				(let [bx (+ 0.5 (double (or (try (pos/pos-x raw-pos) (catch Exception _ nil))
																		(:x raw-pos))))
							by (+ 0.5 (double (or (try (pos/pos-y raw-pos) (catch Exception _ nil))
																		(:y raw-pos))))
							bz (+ 0.5 (double (or (try (pos/pos-z raw-pos) (catch Exception _ nil))
																		(:z raw-pos))))]
					(< (entity/entity-distance-to-sqr player bx by bz)
						 (* max-station-distance max-station-distance)))))))

(defn developer-controller-tile?
	[tile]
	(developer/controller-block? (platform-be/get-block-id tile)))
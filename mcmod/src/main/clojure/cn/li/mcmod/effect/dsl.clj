(ns cn.li.mcmod.effect.dsl
	"Effect DSL - declarative MobEffect definitions"
	(:require [clojure.string :as str]
						[cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
						[cn.li.mcmod.util.log :as log]))





(defrecord EffectSpec [id registry-name category color tick-interval damage-per-tick properties])

(defn create-effect-spec
	[effect-id options]
	(map->EffectSpec
		{:id effect-id
		 :registry-name (or (:registry-name options)
												(str/replace effect-id #"-" "_"))
		 :category (or (:category options) :harmful)
		 :color (int (or (:color options) 0xAA0000))
		 :tick-interval (int (or (:tick-interval options) 20))
		 :damage-per-tick (float (or (:damage-per-tick options) 0.0))
		 :properties (or (:properties options) {})}))

(defn register-effect!
	[effect-spec]
	(when-not (string? (:id effect-spec))
		(throw (ex-info "Effect :id must be string" {:effect-spec effect-spec})))
	(when-not (string? (:registry-name effect-spec))
		(throw (ex-info "Effect :registry-name must be string" {:effect-spec effect-spec})))
	(log/debug "Registering effect:" (:id effect-spec) "->" (:registry-name effect-spec))
	(when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:registry :effects (:id effect-spec)] effect-spec))
	effect-spec)

(defn get-effect
	[effect-id]
	(get-in @(fw/fw-atom) [:registry :effects effect-id]))

(defn list-effects
	[]
	(keys (get-in @(fw/fw-atom) [:registry :effects])))

(defmacro defeffect
	[effect-name & options]
	(if (map? effect-name)
		(let [options-map effect-name
					effect-id (:id options-map)]
			(when-not (string? effect-id)
				(throw (ex-info "Map-form defeffect requires string :id" {:form options-map})))
			`(register-effect!
				 (create-effect-spec ~effect-id ~(dissoc options-map :id))))
		(let [options-map (if (and (= 1 (count options)) (map? (first options)))
												(first options)
												(apply hash-map options))
					effect-id (or (:id options-map) (name effect-name))]
			`(def ~effect-name
				 (register-effect!
					 (create-effect-spec ~effect-id ~(dissoc options-map :id)))))))

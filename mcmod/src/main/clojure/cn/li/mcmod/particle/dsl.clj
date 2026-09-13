(ns cn.li.mcmod.particle.dsl
	"Particle DSL - declarative particle type definitions.

  Registry stored in Framework [:registry :particles]."
	(:require [clojure.string :as str]
						[cn.li.mcmod.framework :as fw]
						[cn.li.mcmod.util.log :as log]))

(defrecord ParticleSpec [id registry-name always-show? properties])

(defn create-particle-spec
	[particle-id options]
	(map->ParticleSpec
		{:id particle-id
		 :registry-name (or (:registry-name options)
												(str/replace particle-id #"-" "_"))
		 :always-show? (boolean (:always-show? options))
		 :properties (or (:properties options) {})}))

(defn register-particle!
	"Register a particle spec. No-op during AOT compilation."
	[particle-spec]
	(when-not (string? (:id particle-spec))
		(throw (ex-info "Particle :id must be string" {:particle-spec particle-spec})))
	(log/debug "Registering particle:" (:id particle-spec) "->" (:registry-name particle-spec))
	(when-let [fw-atom (fw/fw-atom)]
		(swap! fw-atom assoc-in [:registry :particles (:id particle-spec)] particle-spec))
	particle-spec)

(defn get-particle
	"Look up a particle spec by id."
	[particle-id]
	(if-let [fw-atom (fw/fw-atom)]
		(get-in @fw-atom [:registry :particles particle-id])
		nil))

(defn list-particles
	"List all registered particle ids."
	[]
	(if-let [fw-atom (fw/fw-atom)]
		(keys (get-in @fw-atom [:registry :particles]))
		()))

(defmacro defparticle
	[particle-name & options]
	(if (map? particle-name)
		(let [options-map particle-name
					particle-id (:id options-map)]
			(when-not (string? particle-id)
				(throw (ex-info "Map-form defparticle requires string :id" {:form options-map})))
			`(register-particle!
				 (create-particle-spec ~particle-id ~(dissoc options-map :id))))
		(let [options-map (if (and (= 1 (count options)) (map? (first options)))
												(first options)
												(apply hash-map options))
					particle-id (or (:id options-map) (name particle-name))]
			`(def ~particle-name
				 (register-particle!
					 (create-particle-spec ~particle-id ~(dissoc options-map :id)))))))

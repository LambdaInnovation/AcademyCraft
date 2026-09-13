(ns cn.li.mcmod.sound.dsl
	"Sound DSL - declarative sound event definitions"
	(:require [clojure.string :as str]
						[cn.li.mcmod.protocol.core :as registry-core]
            [cn.li.mcmod.framework :as fw]
						[cn.li.mcmod.util.log :as log]))




(defrecord SoundSpec [id registry-name subtitle-key properties])

(defn create-sound-spec
	"Create a sound specification from options."
	[sound-id options]
	(map->SoundSpec
		{:id sound-id
		 :registry-name (or (:registry-name options)
												(str/replace sound-id #"-" "_"))
		 :subtitle-key (:subtitle-key options)
		 :properties (or (:properties options) {})}))

(defn validate-sound-spec
	[sound-spec]
	(when-not (:id sound-spec)
		(throw (ex-info "Sound must have an :id" {:spec sound-spec})))
	(when-not (string? (:id sound-spec))
		(throw (ex-info "Sound :id must be a string" {:id (:id sound-spec)})))
	(when-not (string? (:registry-name sound-spec))
		(throw (ex-info "Sound :registry-name must be a string"
										{:id (:id sound-spec)
										 :registry-name (:registry-name sound-spec)})))
	true)

(defn register-sound!
	[sound-spec]
	(validate-sound-spec sound-spec)
	(log/debug "Registering sound:" (:id sound-spec) "->" (:registry-name sound-spec))
	(when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:registry :sounds (:id sound-spec)] sound-spec))
	sound-spec)

(defn get-sound
	[sound-id]
	(get-in @(fw/fw-atom) [:registry :sounds sound-id]))

(defn list-sounds
	[]
	(keys (get-in @(fw/fw-atom) [:registry :sounds])))

(defmacro defsound
	"Define a sound event.

	Supports two forms:
	1) Map form: (defsound {:id \"content.event_strong\" ...})
	2) Symbol form: (defsound content-event-strong :id \"content.event_strong\" ...)
	"
	[sound-name & options]
	(if (map? sound-name)
		(let [options-map sound-name
					sound-id (:id options-map)]
			(when-not (string? sound-id)
				(throw (ex-info "Map-form defsound requires string :id"
												{:form options-map})))
			`(register-sound!
				 (create-sound-spec ~sound-id ~(dissoc options-map :id))))
		(let [options-map (if (and (= 1 (count options)) (map? (first options)))
												(first options)
												(apply hash-map options))
					sound-id (or (:id options-map)
											 (name sound-name))]
			`(def ~sound-name
				 (register-sound!
					 (create-sound-spec ~sound-id ~(dissoc options-map :id)))))))

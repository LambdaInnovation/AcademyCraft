
(ns cn.li.forge1201.registry.state
	"Centralized Forge registry runtime state.

	Keeps mutable registry atoms and lookup helpers out of the mod entry namespace
	so mod orchestration stays focused on bootstrapping/event wiring."
	(:require [cn.li.platform.registry.metadata :as registry-metadata])
	(:import [net.minecraftforge.registries RegistryObject]))

;; Storage for registered objects populated during mod initialization.
(defn create-forge-registry-runtime
	[]
	{:registered-blocks {}
	 :registered-items {}
	 :registered-entities {}
	 :registered-block-entities {}
	 :registered-fluid-types {}
	 :registered-fluids-source {}
	 :registered-fluids-flowing {}
	 :registered-sounds {}
	 :registered-effects {}
	 :registered-particles {}})

(def ^:private forge-registry-runtime
	"Lock-free CAS updates replace the prior ^:dynamic var + Object lock."
	(atom (create-forge-registry-runtime)))

(defn registry-runtime-state
	[]
	@forge-registry-runtime)

(defn update-registry-runtime!
	[f & args]
	(apply swap! forge-registry-runtime f args)
	nil)

(defn- registry-bucket
	[k]
	(get (registry-runtime-state) k))

(defn- registry-put!
	[k entry-id registered-obj]
	(update-registry-runtime! update k assoc entry-id registered-obj))

(defn registered-blocks-snapshot [] (registry-bucket :registered-blocks))
(defn registered-items-snapshot [] (registry-bucket :registered-items))
(defn registered-entities-snapshot [] (registry-bucket :registered-entities))
(defn registered-block-entities-snapshot [] (registry-bucket :registered-block-entities))
(defn registered-fluid-types-snapshot [] (registry-bucket :registered-fluid-types))
(defn registered-fluids-source-snapshot [] (registry-bucket :registered-fluids-source))
(defn registered-fluids-flowing-snapshot [] (registry-bucket :registered-fluids-flowing))
(defn registered-sounds-snapshot [] (registry-bucket :registered-sounds))
(defn registered-effects-snapshot [] (registry-bucket :registered-effects))
(defn registered-particles-snapshot [] (registry-bucket :registered-particles))

(defn register-block! [block-id registered-obj]
	(registry-put! :registered-blocks block-id registered-obj))

(defn register-item! [item-id registered-obj]
	(registry-put! :registered-items item-id registered-obj))

(defn register-entity! [entity-id registered-obj]
	(registry-put! :registered-entities entity-id registered-obj))

(defn register-block-entity! [tile-id registered-obj]
	(registry-put! :registered-block-entities tile-id registered-obj))

(defn register-fluid-type! [fluid-id registered-obj]
	(registry-put! :registered-fluid-types fluid-id registered-obj))

(defn register-fluid-source! [fluid-id registered-obj]
	(registry-put! :registered-fluids-source fluid-id registered-obj))

(defn register-fluid-flowing! [fluid-id registered-obj]
	(registry-put! :registered-fluids-flowing fluid-id registered-obj))

(defn register-sound! [sound-id registered-obj]
	(registry-put! :registered-sounds sound-id registered-obj))

(defn register-effect! [effect-id registered-obj]
	(registry-put! :registered-effects effect-id registered-obj))

(defn register-particle! [particle-id registered-obj]
	(registry-put! :registered-particles particle-id registered-obj))

(defn get-registered-block-ro [block-id]
	(get (registered-blocks-snapshot) block-id))

(defn get-registered-item-ro [item-id]
	(get (registered-items-snapshot) item-id))

(defn get-registered-entity-ro [entity-id]
	(get (registered-entities-snapshot) entity-id))

(defn get-registered-block-entity-ro [tile-id]
	(get (registered-block-entities-snapshot) tile-id))

(defn get-registered-fluid-source-ro [fluid-id]
	(get (registered-fluids-source-snapshot) fluid-id))

(defn get-registered-fluid-flowing-ro [fluid-id]
	(get (registered-fluids-flowing-snapshot) fluid-id))

(defn- safe-registry-object-get
	[^RegistryObject registered-obj]
	(when (and registered-obj (.isPresent registered-obj))
		(.get registered-obj)))

(defn get-registered-entity-type
	"Get a registered EntityType by entity-id."
	[entity-id]
	(when-let [registered-obj (get-registered-entity-ro entity-id)]
		(safe-registry-object-get ^RegistryObject registered-obj)))

(defn get-registered-block-entity-type
	"Get a registered BlockEntityType by tile-id or block-id."
	[tile-or-block-id]
	(let [tile-id (or (when (contains? (registered-block-entities-snapshot) tile-or-block-id)
											tile-or-block-id)
										(registry-metadata/get-block-tile-id tile-or-block-id))]
		(when-let [registered-obj (and tile-id (get-registered-block-entity-ro tile-id))]
			(safe-registry-object-get ^RegistryObject registered-obj))))

(defn get-registered-block
	"Get a registered block by its DSL id."
	[block-id]
	(when-let [registered-obj (get-registered-block-ro block-id)]
		(safe-registry-object-get ^RegistryObject registered-obj)))

(defn get-registered-item
	"Get a registered item by its DSL id."
	[item-id]
	(when-let [registered-obj (get-registered-item-ro item-id)]
		(safe-registry-object-get ^RegistryObject registered-obj)))

(defn get-registered-block-item
	"Get a registered block item by block DSL id."
	[block-id]
	(when-let [registered-obj (get-registered-item-ro (str block-id "-item"))]
		(safe-registry-object-get ^RegistryObject registered-obj)))

(defn get-registered-fluid-source
	[fluid-id]
	(when-let [registered-obj (get-registered-fluid-source-ro fluid-id)]
		(safe-registry-object-get ^RegistryObject registered-obj)))

(defn get-registered-fluid-flowing
	[fluid-id]
	(when-let [registered-obj (get-registered-fluid-flowing-ro fluid-id)]
		(safe-registry-object-get ^RegistryObject registered-obj)))

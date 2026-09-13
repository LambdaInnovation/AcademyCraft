(ns cn.li.forge1201.integration.events.entity-attributes
  "Register default living-entity attributes for scripted-mob entity types.

  Forge requires each LivingEntity subtype to have its AttributeSupplier registered
  via EntityAttributeCreationEvent. Without this, Forge's DefaultAttributes map has
  no entry for scripted-mob types and logs \"Entity X has no attributes\" on spawn.

  The actual Java interop (PathfinderMob.createMobAttributes().build() + event.put)
  lives in ModEntities/registerMobDefaultAttributes to avoid clj-kondo import issues."
  (:require [cn.li.forge1201.registry.state :as registry-state]
            [cn.li.mcmod.entity.dsl :as edsl]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.forge1201.entity ModEntities]
           [net.minecraftforge.registries RegistryObject]))

(defn handle-entity-attribute-creation
  "Register PathfinderMob default attributes for every :scripted-mob entity type."
  [^net.minecraftforge.event.entity.EntityAttributeCreationEvent event]
  (doseq [entity-id (edsl/list-entities)]
    (let [entity-spec (edsl/get-entity entity-id)]
      (when (= :scripted-mob (:entity-kind entity-spec))
        (if-let [^RegistryObject ro (registry-state/get-registered-entity-ro entity-id)]
          (when (.isPresent ro)
            (ModEntities/registerMobDefaultAttributes event (.get ro))
            (log/debug "Registered attributes for"
                      (edsl/get-entity-registry-name entity-id)))
          (log/warn "No registered entity type for scripted-mob"
                    {:entity-id entity-id}))))))

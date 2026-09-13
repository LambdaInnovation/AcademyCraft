(ns cn.li.neoforgebase.integration.events.entity-attributes
  "Shared scripted-mob attribute registration. Version loaders install ModEntities bridge."
  (:require [cn.li.neoforgebase.registry.state :as registry-state]
            [cn.li.mcmod.entity.dsl :as edsl]
            [cn.li.mcmod.util.log :as log])
  (:import [net.neoforged.neoforge.registries DeferredHolder]))

(defonce ^:private register-mob-attrs*
  (atom nil))

(defn install-register-mob-attrs!
  "Install (fn [event entity-type] ...) → ModEntities/registerMobDefaultAttributes."
  [f]
  (reset! register-mob-attrs* f)
  f)

(defn handle-entity-attribute-creation
  "Register PathfinderMob default attributes for every :scripted-mob entity type."
  [^net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent event]
  (let [register! @register-mob-attrs*]
    (when (nil? register!)
      (throw (IllegalStateException. "entity-attributes register-mob-attrs not installed")))
    (doseq [entity-id (edsl/list-entities)]
      (let [entity-spec (edsl/get-entity entity-id)]
        (when (= :scripted-mob (:entity-kind entity-spec))
          (if-let [^DeferredHolder ro (registry-state/get-registered-entity-ro entity-id)]
            (when (.isBound ro)
              (register! event (.get ro))
              (log/debug "Registered attributes for"
                        (edsl/get-entity-registry-name entity-id)))
            (log/warn "No registered entity type for scripted-mob"
                      {:entity-id entity-id})))))))

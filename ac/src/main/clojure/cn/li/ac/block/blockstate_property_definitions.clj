(ns cn.li.ac.block.blockstate-property-definitions
  "BlockState property definitions only (platform-neutral, no engine package imports).

  Provides definition/query API for block state properties. Forge/Fabric
  adapters use this to create actual IntegerProperty/BooleanProperty and
  register them. This namespace does not import game engine concrete types."
  (:require [cn.li.mcmod.protocol.metadata :as registry-metadata]))

(defn get-property-definition
  "Get the Clojure definition of a property for a block.

  Args:
    block-id: String - DSL block identifier
    property-key: keyword - property identifier

  Returns:
    Map with :type, :min/:max, :name, :default or nil"
  [block-id property-key]
  (let [block-spec (registry-metadata/get-block-spec block-id)
        properties (:block-state-properties block-spec)]
    (get properties property-key)))

(defn get-all-property-definitions
  "Get all property definitions for a block (map of property-key -> config).

  Args:
    block-id: String - DSL block identifier

  Returns:
    Map of keyword -> config, or empty map if none"
  [block-id]
  (let [block-spec (registry-metadata/get-block-spec block-id)]
    (or (:block-state-properties block-spec) {})))

(ns cn.li.mcbase.runtime.entity-query-core
  "Public aggregate API for shared entity query helpers.

  Lookup and iteration implementations live in focused modules, while this
  namespace remains the stable runtime-facing entrypoint for platform code."
  (:require [cn.li.mcbase.runtime.entity-iterators :as entity-iterators]
            [cn.li.mcbase.runtime.entity-lookup :as entity-lookup]))

(def get-player-by-uuid entity-lookup/get-player-by-uuid)
(def resolve-level entity-lookup/resolve-level)
(def resolve-level-strict entity-lookup/resolve-level-strict)
(def get-entity-by-uuid entity-lookup/get-entity-by-uuid)
(def entity-type-id-for-entity entity-iterators/entity-type-id-for-entity)
(def entity-type-id entity-iterators/entity-type-id)
(def create-entity-type-id-fn entity-iterators/create-entity-type-id-fn)

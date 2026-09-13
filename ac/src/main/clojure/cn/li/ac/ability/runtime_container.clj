(ns cn.li.ac.ability.runtime-container
  "Composition-root container for ability runtime components.

  This namespace centralizes runtime creation/installation so loader bridges
  can wire explicit runtimes from a single root."
  (:require [cn.li.ac.ability.registry.category :as category-registry]
            [cn.li.ac.ability.registry.event :as event-registry]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.spi-lifecycle :as lifecycle-registry]))

(defn default-ability-runtime-container
  []
  {:category-registry-runtime (category-registry/create-category-registry-runtime)
   :event-subscriber-runtime (event-registry/create-event-subscriber-runtime)
   :skill-registry-runtime (skill-registry/create-skill-registry-runtime)
    :lifecycle-registry-runtime (lifecycle-registry/create-lifecycle-registry-runtime)})

(defn create-ability-runtime-container
  ([]
   (default-ability-runtime-container))
  ([overrides]
   (merge (default-ability-runtime-container)
          (or overrides {}))))

(defn install-ability-runtime-container!
  [{:keys [category-registry-runtime
           event-subscriber-runtime
           skill-registry-runtime
           lifecycle-registry-runtime] :as container}]
  (category-registry/install-category-registry-runtime! category-registry-runtime)
  (event-registry/install-event-subscriber-runtime! event-subscriber-runtime)
  (skill-registry/install-skill-registry-runtime! skill-registry-runtime)
  (lifecycle-registry/install-lifecycle-registry-runtime! lifecycle-registry-runtime)
  container)

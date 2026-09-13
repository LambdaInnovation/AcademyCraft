(ns cn.li.fabric1211.runtime.adapters.registry
  "Declarative runtime adapter install registry for Fabric."
  (:require [cn.li.mc1211.runtime.raycast-ops-install]
            [cn.li.mc1211.runtime.ender-dragon-parts-install]
            [cn.li.fabric1211.runtime.damage-interception :as runtime-damage-interception]
            [cn.li.fabric1211.runtime.player-motion :as runtime-player-motion]
            [cn.li.fabric1211.runtime.entity-damage :as runtime-entity-damage]
            [cn.li.fabric1211.runtime.entity-motion :as runtime-entity-motion]
            [cn.li.fabric1211.runtime.entity-query :as runtime-entity-query]
            [cn.li.fabric1211.runtime.world-effects :as runtime-world-effects]
            [cn.li.fabric1211.runtime.block-manipulation :as runtime-block-manipulation]
            [cn.li.fabric1211.adapter.network :as runtime-network]
            [cn.li.fabric1211.adapter.server-context :as server-context]
            [cn.li.mcbase.runtime.adapter-registry :as adapter-registry]
            [cn.li.mc1211.runtime.interop-core :as interop-core]
            [cn.li.mcbase.runtime.raycast-core :as raycast-core]
            [cn.li.mc1211.runtime.teleportation-core :as teleportation-core]
            [cn.li.mc1211.runtime.named-position-store-core :as named-position-store-core]
            [cn.li.mc1211.runtime.potion-effects-core :as potion-effects-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.platform.neutral.hooks :as power-runtime]))

(defn runtime-install-steps
  "Build the callback vector after neutral facade IFns have been installed."
  []
  [(adapter-registry/step :damage-interception runtime-damage-interception/install-damage-interception!)
   (adapter-registry/step :player-motion
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :player-motion
                               (runtime-player-motion/fabric-player-motion))))
   (adapter-registry/step :entity-damage runtime-entity-damage/install-entity-damage!)
   (adapter-registry/step :entity-motion
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :entity-motion
                               (runtime-entity-motion/fabric-entity-motion))))
   (adapter-registry/step :entity-query runtime-entity-query/install-entity-query!)
   (adapter-registry/step :raycast
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :raycast
                               (raycast-core/create-raycast server-context/get-server))))
   (adapter-registry/step :world-effects runtime-world-effects/install-world-effects!)
   (adapter-registry/step :teleportation
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :teleportation
                               (teleportation-core/create-teleportation server-context/get-server))))
   (adapter-registry/step :named-position-store
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :named-position-store
                               (named-position-store-core/create-named-position-store server-context/get-server))))
   (adapter-registry/step :potion-effects
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :potion-effects
                               (potion-effects-core/create-potion-effects server-context/get-server))))
   (adapter-registry/step :runtime-interop
                          #(interop-core/install-runtime-interop! "Fabric" server-context/get-server))
   (adapter-registry/step :block-manipulation runtime-block-manipulation/install-block-manipulation!)
   (adapter-registry/step :network runtime-network/init!)
   (adapter-registry/step :damage-handlers power-runtime/init-damage-handlers!)])

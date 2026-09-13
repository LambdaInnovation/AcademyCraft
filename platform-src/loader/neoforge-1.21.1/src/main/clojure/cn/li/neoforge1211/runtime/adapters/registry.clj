(ns cn.li.neoforge1211.runtime.adapters.registry
  "Declarative Forge runtime adapter registry."
  (:require [cn.li.mc1211.runtime.raycast-ops-install]
            [cn.li.mc1211.runtime.ender-dragon-parts-install]
            [cn.li.neoforge1211.runtime.entity-damage :as entity-damage]
            [cn.li.neoforgebase.runtime.multipart-entity :as multipart-entity]
            [cn.li.neoforge1211.runtime.world-effects :as world-effects]
            [cn.li.neoforge1211.runtime.block-manipulation :as block-manipulation]
            [cn.li.neoforge1211.runtime.damage-interception :as damage-interception]
            [cn.li.neoforgebase.adapter.server-context :as server-context]
            [cn.li.mcbase.runtime.adapter-registry :as adapter-registry]
            [cn.li.mc1211.runtime.interop-core :as interop-core]
            [cn.li.mc1211.runtime.teleportation-core :as teleportation-core]
            [cn.li.mc1211.runtime.named-position-store-core :as position-store-core]
            [cn.li.mc1211.runtime.potion-effects-core :as potion-effects-core]
            [cn.li.mcbase.runtime.player-motion-core :as player-motion-core]
            [cn.li.mc1211.runtime.entity-motion-core :as entity-motion-core]
            [cn.li.mcbase.runtime.raycast-core :as raycast-core]
            [cn.li.mcbase.runtime.entity-query-core :as entity-query-core]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.entity :as pentity]))

(def runtime-install-steps
  [(adapter-registry/step :multipart-entity-parent-resolver
                          multipart-entity/register-parent-resolver!)
   (adapter-registry/step :entity-damage
                          entity-damage/install-entity-damage!)
   (adapter-registry/step :raycast
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :raycast
                               (raycast-core/create-raycast server-context/get-server))))
   (adapter-registry/step :interop
                          #(interop-core/install-runtime-interop! "Forge" server-context/get-server))
   (adapter-registry/step :world-effects
                          world-effects/install-world-effects!)
   (adapter-registry/step :potion-effects
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :potion-effects
                               (potion-effects-core/create-potion-effects server-context/get-server))))
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
                               (position-store-core/create-named-position-store server-context/get-server))))
   (adapter-registry/step :player-motion
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :player-motion
                               (player-motion-core/create-player-motion server-context/get-server))))
   (adapter-registry/step :entity-motion
                          #(when-let [fw-atom (fw/fw-atom)]
                             (platform/install-adapter!
                               fw-atom
                               :entity-motion
                               (entity-motion-core/create-entity-motion server-context/get-server))))
   (adapter-registry/step :entity-query
                          #(pentity/install-entity-type-id-fn!
                             (entity-query-core/create-entity-type-id-fn server-context/get-server)
                             "Forge entity query"))
   (adapter-registry/step :block-manipulation
                          block-manipulation/install-block-manipulation!)
   (adapter-registry/step :damage-interception
                          damage-interception/install-damage-interception!)])

(def adapter-installers runtime-install-steps)

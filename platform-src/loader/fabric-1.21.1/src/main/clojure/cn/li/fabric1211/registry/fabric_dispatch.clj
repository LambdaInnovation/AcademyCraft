(ns cn.li.fabric1211.registry.fabric-dispatch
  "Fabric 1.21.1 registry dispatch."
  (:require [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.fabric1211.shim FabricParticleHelper]
           [cn.li.mcver RegistryDispatch]
           [net.minecraft.world.item Item]
           [net.minecraft.world.level.block Block]
           [net.minecraft.world.level.material Fluid]))

(defn register-block
  [block-id ^Block block-instance]
  (log/debug "Registering block with Fabric BuiltInRegistries:" block-id)
  (RegistryDispatch/registerBlock modid/mod-id (str block-id) block-instance))

(defn register-item
  [item-id ^Item item-instance]
  (log/debug "Registering item with Fabric BuiltInRegistries:" item-id)
  (RegistryDispatch/registerItem modid/mod-id (str item-id) item-instance))

(defn register-fluid
  [fluid-id ^Fluid fluid-instance]
  (log/debug "Registering fluid with Fabric BuiltInRegistries:" fluid-id)
  (RegistryDispatch/registerFluid modid/mod-id (str fluid-id) fluid-instance))

(defn register-particle
  [particle-id always-show?]
  (log/debug "Registering particle with Fabric BuiltInRegistries:" particle-id)
  (FabricParticleHelper/registerParticle modid/mod-id (str particle-id) (boolean always-show?)))

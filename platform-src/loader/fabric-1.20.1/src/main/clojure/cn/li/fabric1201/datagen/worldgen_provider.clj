(ns cn.li.fabric1201.datagen.worldgen-provider
  "Fabric worldgen DataGen provider — thin adapter over shared shell."
  (:require [cn.li.mc1201.datagen.worldgen-provider-shell :as shell])
  (:import [net.minecraft.data PackOutput]))

(defn create-provider
  [^PackOutput output]
  (shell/create output :fabric))

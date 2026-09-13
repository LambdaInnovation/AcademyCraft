(ns cn.li.fabric1211.datagen.worldgen-provider
  "Fabric worldgen DataGen provider — thin adapter over shared shell."
  (:require [cn.li.mc1211.datagen.worldgen-provider-shell :as shell])
  (:import [net.minecraft.data PackOutput]))

(defn create-provider
  [^PackOutput output]
  (shell/create output :fabric))

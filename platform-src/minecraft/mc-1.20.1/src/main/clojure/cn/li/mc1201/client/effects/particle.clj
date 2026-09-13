(ns cn.li.mc1201.client.effects.particle
  "Thin re-export of cn.li.mcbase.client.effects.particle."
  (:require [cn.li.mcbase.client.effects.particle :as shared]))

(def spawn-particle-effect! shared/spawn-particle-effect!)
(def tick-particles! shared/tick-particles!)
(def init! shared/init!)

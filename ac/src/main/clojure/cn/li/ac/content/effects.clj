(ns cn.li.ac.content.effects
  "Custom status effect declarations."
  (:require [cn.li.mcmod.effect.dsl :as edsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn init-effects!
  []
  (install/framework-once! ::effects-initialized?
  (fn []
    (edsl/defeffect {:id "bleeding"
                     :category :harmful
                     :color 0xAA1111
                     :tick-interval 20
                     :damage-per-tick 1.0})
    (edsl/defeffect {:id "burning_core"
                     :category :harmful
                     :color 0xFF5500
                     :tick-interval 10
                     :damage-per-tick 1.0})
    (edsl/defeffect {:id "overload_recovery"
                     :category :beneficial
                     :color 0x33CCFF
                     :tick-interval 40
                     :damage-per-tick 0.0})
    (log/debug "Effect content initialized"))))

(ns cn.li.ac.item.windgen-fan
  "Wind generator fan item (required by wind-gen main slot)."
  (:require [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn init-windgen-fan!
  []
  (install/framework-once! ::installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "windgen_fan"
        {:max-stack-size 1
         :durability 100
         :creative-tab :misc
            :properties {:tooltip ["Wind generator rotor"
                    "Install into Wind Generator Main"]
                :model-texture "windgen_fan"}}))
          (log/debug "Wind generator fan item initialized"))))
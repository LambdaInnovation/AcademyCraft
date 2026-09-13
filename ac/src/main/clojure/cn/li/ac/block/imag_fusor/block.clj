(ns cn.li.ac.block.imag-fusor.block
  "Imaginary Fusor block registration and runtime wiring."
  (:require [cn.li.ac.block.imag-fusor.handlers :as fusor-handlers]
            [cn.li.ac.block.imag-fusor.logic :as fusor-logic]
            [cn.li.ac.block.imag-fusor.config :as fusor-config]
            [cn.li.ac.block.role-impls :as impls]
            [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.tile-dsl :as tdsl]
            [cn.li.mcmod.capability.registry :as cap-registry])
  (:import [cn.li.acapi.wireless IWirelessReceiver]))

(defn- fusor-max-energy [state]
  (or (:max-energy state) fusor-config/max-energy))

(defn- fusor-receiver-bandwidth [_state]
  fusor-config/receiver-bandwidth)

(defn- fusor-receiver-cap-factory [be _side]
  (impls/->WirelessReceiverImpl be fusor-max-energy fusor-receiver-bandwidth))

(defn init-imag-fusor!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Imaginary Fusor"
     :tiles [{:id "imag-fusor"
              :registry-name "imag_fusor"
              :blocks ["imag-fusor"]
              :tick-fn fusor-logic/fusor-tick-fn
              :read-nbt-fn fusor-logic/fusor-scripted-load-fn
              :write-nbt-fn fusor-logic/fusor-scripted-save-fn}]
     :tile-ids ["imag-fusor"]
     :containers {"imag-fusor" fusor-logic/fusor-container-fns}
     :capabilities [{:key :wireless-receiver
                     :interface IWirelessReceiver
                     :factory fusor-receiver-cap-factory}]
     :after #(when-not (cap-registry/get-capability-entry :wireless-receiver)
               (cap-registry/declare-capability!
                 :wireless-receiver IWirelessReceiver
                 fusor-receiver-cap-factory))
     :blocks [(bdsl/create-block-spec
                "imag-fusor"
                {:registry-name "imag_fusor"
                 :physical {:material :stone
                            :hardness 3.0
                            :resistance 15.0
                            :requires-tool true
                            :harvest-tool :pickaxe
                            :harvest-level 1
                            :sounds :stone}
                 ;; Original AcademyCraft emits light level 6 when working (getLightValue).
                 ;; Dynamic light emission (frame>0 → 6, idle → 0) requires forge-layer
                 ;; Block.getLightEmission override keyed on frame blockstate property.
                 :rendering {:model-parent "minecraft:block/cube_all"
                             :textures {:all (modid/asset-path "block" "imag_fusor")}
                             :flat-item-icon? true}
                 :block-state {:block-state-properties fusor-logic/fusor-block-state-properties}
                 :events {:on-right-click fusor-logic/open-fusor-gui!
                          :on-place fusor-logic/handle-fusor-place}})]
     :network-handler fusor-handlers/register-network-handlers!}))

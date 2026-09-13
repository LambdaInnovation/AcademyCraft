(ns cn.li.ac.block.ability-interferer.block
  "Ability Interferer block registration and runtime wiring."
  (:require [cn.li.ac.block.ability-interferer.handlers :as interferer-handlers]
            [cn.li.ac.block.ability-interferer.logic :as interferer-logic]
            [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessReceiver]))

(defn- interferer-receiver-cap-factory [be _side]
  (interferer-logic/create-interferer-wireless-receiver be))

(defn init-ability-interferer!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Ability Interferer"
     :tiles [{:id "ability-interferer"
              :registry-name "ability_interferer"
              :blocks ["ability-interferer"]
              :tick-fn interferer-logic/interferer-tick-fn
              :read-nbt-fn interferer-logic/interferer-scripted-load-fn
              :write-nbt-fn interferer-logic/interferer-scripted-save-fn}]
     :containers {"ability-interferer" interferer-logic/interferer-container-fns}
     :blocks [(bdsl/create-block-spec
                "ability-interferer"
                {:registry-name "ability_interferer"
                 ;; Upstream BlockAbilityInterferer is a bare ACBlockContainer
                 ;; (Material.ROCK) with no setHardness/setResistance — the 1.12
                 ;; Block defaults: hardness 2.0, resistance 10.0, pickaxe of
                 ;; ANY tier (ROCK material reports "pickaxe").
                 :physical {:material :stone
                            :hardness 2.0
                            :resistance 10.0
                            :requires-tool true
                            :harvest-tool :pickaxe
                            :harvest-level 0
                            :sounds :stone}
                 :rendering {:model-parent "minecraft:block/cube_all"
                             :textures {:all (modid/asset-path "block" "ability_interf_off")}
                             :flat-item-icon? true
                             :light-level 0}
                 :block-state {:block-state-properties interferer-logic/interferer-block-state-properties}
                 :events {:on-right-click interferer-logic/open-interferer-gui!
                          :on-place interferer-logic/on-interferer-placed!
                          :on-break interferer-logic/on-interferer-break!}})]
     :network-handler interferer-handlers/register-network-handlers!
     :capabilities [{:key :wireless-receiver
                     :interface IWirelessReceiver
                     :factory interferer-receiver-cap-factory}]})
   (interferer-logic/ensure-world-tick-cleanup!))

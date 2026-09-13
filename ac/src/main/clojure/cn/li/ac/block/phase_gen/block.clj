(ns cn.li.ac.block.phase-gen.block
  "Phase Generator block registration and runtime wiring."
  (:require [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.block.phase-gen.handlers :as phase-handlers]
            [cn.li.ac.block.phase-gen.logic :as phase-logic]
            [cn.li.ac.block.role-impls :as impls]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.tile-dsl :as tdsl]
            [cn.li.mcmod.capability.registry :as cap-registry])
  (:import [cn.li.acapi.wireless IWirelessGenerator]))

(defn init-phase-gen!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Phase Generator"
     :tiles [{:id "phase-gen"
              :registry-name "phase_gen"
              :blocks ["phase-gen"]
              :tick-fn phase-logic/phase-tick-fn
              :read-nbt-fn phase-logic/phase-scripted-load-fn
              :write-nbt-fn phase-logic/phase-scripted-save-fn}]
     :tile-ids ["phase-gen"]
     :containers {"phase-gen" phase-logic/phase-container-fns}
     :capabilities [{:key :wireless-generator
                     :interface IWirelessGenerator
                     :factory impls/wireless-generator-factory}]
     :after #(do
              (cap-registry/register-tile-fluid-spec! "phase-gen" modid/MOD-ID "imag_phase")
              (tdsl/register-tile-capability-keys! "phase-gen" :fluid-handler))
     :blocks [(bdsl/create-block-spec
                "phase-gen"
                {:registry-name "phase_gen"
                 :physical {:material :stone
                            :hardness 2.5
                            :resistance 12.5
                            :requires-tool true
                            :harvest-tool :pickaxe
                            :harvest-level 1
                            :sounds :stone}
                 :rendering {:model-parent "minecraft:block/block"
                             :textures {:all (modid/asset-path "block" "phase_gen")}
                             :flat-item-icon? true
                             :render-shape :invisible}
                 :events {:on-right-click phase-logic/open-phase-gen-gui!}})]
     :network-handler phase-handlers/register-network-handlers!
     :client-renderer 'cn.li.ac.block.phase-gen.render/init!}))

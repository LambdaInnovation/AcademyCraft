(ns cn.li.ac.block.solar-gen.block
  "Solar Generator block registration and runtime wiring."
  (:require [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.block.role-impls :as impls]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.block.solar-gen.handlers :as solar-handlers]
            [cn.li.ac.block.solar-gen.logic :as solar-logic]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessGenerator]))

(defn init-solar-gen!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Solar Generator"
     :tiles [{:id "solar-gen"
              :registry-name "solar_gen"
              :blocks ["solar-gen"]
              :tick-fn solar-logic/solar-tick-fn
              :read-nbt-fn solar-logic/solar-scripted-load-fn
              :write-nbt-fn solar-logic/solar-scripted-save-fn}]
     :tile-ids ["solar-gen"]
     :containers {"solar-gen" solar-logic/solar-container-fns}
     :capabilities [{:key :wireless-generator
                     :interface IWirelessGenerator
                     :factory impls/wireless-generator-factory}]
     :blocks [(bdsl/create-block-spec
                "solar-gen"
                {:registry-name "solar_gen"
                 :physical {:material :stone
                            :hardness 1.5
                            :resistance 7.5
                            :requires-tool true
                            :harvest-tool :pickaxe
                            :harvest-level 1
                            :sounds :stone}
                 :rendering {:model-parent "minecraft:block/cube_all"
                             :textures {:all (modid/asset-path "block" "solar_gen")}
                             :flat-item-icon? true
                             :render-shape :invisible}
                 :events {:on-right-click solar-logic/open-solar-gui!}})]
     :network-handler solar-handlers/register-network-handlers!
     :client-renderer 'cn.li.ac.block.solar-gen.render/init!}))

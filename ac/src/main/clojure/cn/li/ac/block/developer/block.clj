(ns cn.li.ac.block.developer.block
  (:require [cn.li.ac.block.developer.handlers :as dev-handlers]
            [cn.li.ac.block.developer.logic :as dev-logic]
            [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessReceiver]))

(defn- register-developer-multiblocks! []
  (bdsl/defmultiblock 'developer-normal
    :multi-block {:positions dev-logic/developer-multiblock-positions
                  :rotation-center [0.5 0.0 0.5]
                  :pivot-xz-override [0.0 0.0]
                  :tesr-use-raw-rotation-center? true
                  :tesr-y-deg-override 0.0}
    :common {:physical {:material :stone :hardness 4.0 :resistance 20.0
                       :requires-tool true :harvest-tool :pickaxe :harvest-level 2 :sounds :stone}}
    :controller {:registry-name "dev_normal"
                 :rendering {:light-level 1.0
                             :flat-item-icon? true
             :render-shape :invisible
                             :textures {:all (modid/asset-path "block" "dev_normal")}}
                 :events {:on-right-click (dev-logic/open-developer-gui-for "developer-normal")}}
    :part {:registry-name "dev_normal_part"
          :rendering {:model-parent "minecraft:block/cube_all"
            :render-shape :invisible
                       :textures {:all (modid/asset-path "block" "dev_normal")}}
           :events {:on-right-click (dev-logic/open-developer-gui-for "developer-normal")}})

  (bdsl/defmultiblock 'developer-advanced
    :multi-block {:positions dev-logic/developer-multiblock-positions
                  :rotation-center [0.5 0.0 0.5]
                  :pivot-xz-override [0.0 0.0]
                  :tesr-use-raw-rotation-center? true
                  :tesr-y-deg-override 0.0}
    :common {:physical {:material :stone :hardness 4.0 :resistance 20.0
                       :requires-tool true :harvest-tool :pickaxe :harvest-level 2 :sounds :stone}}
    :controller {:registry-name "dev_advanced"
                 :rendering {:light-level 2.0
                             :flat-item-icon? true
             :render-shape :invisible
                             :textures {:all (modid/asset-path "block" "dev_advanced")}}
                 :events {:on-right-click (dev-logic/open-developer-gui-for "developer-advanced")}}
    :part {:registry-name "dev_advanced_part"
          :rendering {:model-parent "minecraft:block/cube_all"
            :render-shape :invisible
                       :textures {:all (modid/asset-path "block" "dev_advanced")}}
           :events {:on-right-click (dev-logic/open-developer-gui-for "developer-advanced")}}))

(defn init-developer!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Developer blocks"
     :tiles [{:id "developer-normal"
              :registry-name "dev_normal"
              :blocks ["developer-normal" "developer-normal-part"]
              :tick-fn dev-logic/developer-tick-fn
              :read-nbt-fn dev-logic/dev-scripted-load-fn
              :write-nbt-fn dev-logic/dev-scripted-save-fn}
             {:id "developer-advanced"
              :registry-name "dev_advanced"
              :blocks ["developer-advanced" "developer-advanced-part"]
              :tick-fn dev-logic/developer-tick-fn
              :read-nbt-fn dev-logic/dev-scripted-load-fn
              :write-nbt-fn dev-logic/dev-scripted-save-fn}]
     :tile-ids ["developer-normal" "developer-advanced"]
     :containers {"developer-normal" dev-logic/dev-container-fns
                  "developer-advanced" dev-logic/dev-container-fns}
     :capabilities [{:key :wireless-receiver :interface IWirelessReceiver
                     :factory dev-logic/create-dev-receiver-cap}]
     :after register-developer-multiblocks!
     :network-handler dev-handlers/register-network-handlers!
     :client-renderer 'cn.li.ac.block.developer.render/init!}))

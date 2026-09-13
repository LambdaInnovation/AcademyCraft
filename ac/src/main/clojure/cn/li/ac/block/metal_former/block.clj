(ns cn.li.ac.block.metal-former.block
  "Metal Former block registration and runtime wiring."
  (:require [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.block.metal-former.config :as former-config]
            [cn.li.ac.block.metal-former.handlers :as former-handlers]
            [cn.li.ac.block.metal-former.logic :as former-logic]
            [cn.li.ac.block.role-impls :as impls]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessReceiver]))

(defn- former-max-energy [state]
  (or (:max-energy state) former-config/max-energy))

(defn- former-receiver-bandwidth [_state]
  former-config/receiver-bandwidth)

(defn- former-receiver-cap-factory [be _side]
  (impls/->WirelessReceiverImpl be former-max-energy former-receiver-bandwidth))

(defn init-metal-former!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Metal Former"
     :tiles [{:id "metal-former"
              :registry-name "metal_former"
              :blocks ["metal-former"]
              :tick-fn former-logic/former-tick-fn
              :read-nbt-fn former-logic/former-scripted-load-fn
              :write-nbt-fn former-logic/former-scripted-save-fn}]
     :tile-ids ["metal-former"]
     :containers {"metal-former" former-logic/former-container-fns}
     ;; Upstream TileMetalFormer extends TileReceiverBase implements
     ;; IWirelessReceiver (3000 IF, LATENCY_MK1 bandwidth) — it takes wireless
     ;; energy and is a valid CurrentCharging target. Without the capability
     ;; key on the tile spec, cn.li.ac.wireless.core.capability-lookup resolves
     ;; nothing here, so the skill's raytrace found "not an energy block":
     ;; no charging, and no surround arc on the block.
     :capabilities [{:key :wireless-receiver
                     :interface IWirelessReceiver
                     :factory former-receiver-cap-factory}]
     :blocks [(bdsl/create-block-spec
                "metal-former"
                {:registry-name "metal_former"
                 :physical {:material :stone
                            :hardness 3.0
                            :resistance 15.0
                            :requires-tool true
                            :harvest-tool :pickaxe
                            :harvest-level 1
                            :sounds :stone}
                 :rendering {:model-parent "minecraft:block/cube_all"
                             :textures {:all (modid/asset-path "block" "metal_former_front")}
                             :flat-item-icon? true}
                 :block-state {:block-state-properties former-logic/former-block-state-properties}
                 :events {:on-right-click former-logic/open-former-gui!
                          :on-place former-logic/handle-former-place}})]
     :network-handler former-handlers/register-network-handlers!}))

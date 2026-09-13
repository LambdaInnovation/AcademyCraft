(ns cn.li.ac.block.wireless-node.block
  "Wireless Node block registration and runtime wiring."
  (:require [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.block.wireless-node.capability :as node-capability]
            [cn.li.ac.block.wireless-node.handlers :as node-handlers]
            [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessNode]
           [cn.li.mcmod.energy IEnergyCapable]))

(defn get-all-wireless-nodes []
  [(bdsl/get-block-spec "wireless-node-basic")
   (bdsl/get-block-spec "wireless-node-standard")
   (bdsl/get-block-spec "wireless-node-advanced")])

(defn- node-block-spec [id registry-name node-type]
  (bdsl/create-block-spec
    id
    {:registry-name registry-name
     :physical {:material :stone
                :hardness 2.5
                :resistance 12.5
                :requires-tool true
                :harvest-tool :pickaxe
                :harvest-level 1
                :sounds :stone}
     :rendering {:model-parent "minecraft:block/cube_all"}
     :block-state {:block-state-properties node-logic/block-state-properties}
     :events {:on-right-click (node-logic/handle-node-right-click node-type)
              :on-place (node-logic/handle-node-place node-type)
              :on-break (node-logic/handle-node-break node-type)}}))

(defn init-wireless-nodes!
  []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Wireless Nodes"
     :before node-logic/ensure-node-slot-schema!
     :tile-kind {:tile-kind :wireless-node
                 :tick-fn node-logic/node-scripted-tick-fn
                 :read-nbt-fn node-logic/node-scripted-load-fn
                 :write-nbt-fn node-logic/node-scripted-save-fn}
     :tiles [{:id "wireless-node-basic" :registry-name "node_basic" :blocks ["wireless-node-basic"] :tile-kind :wireless-node}
             {:id "wireless-node-standard" :registry-name "node_standard" :blocks ["wireless-node-standard"] :tile-kind :wireless-node}
             {:id "wireless-node-advanced" :registry-name "node_advanced" :blocks ["wireless-node-advanced"] :tile-kind :wireless-node}]
     :tile-ids ["wireless-node-basic" "wireless-node-standard" "wireless-node-advanced"]
     :capabilities [{:key :wireless-node :interface IWirelessNode
                     :factory (fn [be _side] (node-capability/->WirelessNodeImpl be))}
                    {:key :wireless-energy :interface IEnergyCapable
                     :factory (fn [be _side] (node-capability/->ClojureEnergyImpl be))}]
     :containers {"wireless-node-basic" node-logic/node-container-fns
                  "wireless-node-standard" node-logic/node-container-fns
                  "wireless-node-advanced" node-logic/node-container-fns}
     :blocks [(node-block-spec "wireless-node-basic" "node_basic" :basic)
              (node-block-spec "wireless-node-standard" "node_standard" :standard)
              (node-block-spec "wireless-node-advanced" "node_advanced" :advanced)]
     :network-handler node-handlers/register-network-handlers!}))

(ns cn.li.ac.block.wireless-matrix.block
  "Wireless Matrix block registration and runtime wiring."
  (:require [cn.li.ac.block.machine.registration :as machine-reg]
            [cn.li.ac.block.wireless-matrix.handlers :as matrix-handlers]
            [cn.li.ac.block.wireless-matrix.capability :as matrix-capability]
            [cn.li.ac.block.wireless-matrix.logic :as matrix-logic]
            [cn.li.ac.block.wireless-matrix.stats :as matrix-stats]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.block.dsl :as bdsl])
  (:import [cn.li.acapi.wireless IWirelessMatrix]))

(defn- register-matrix-multiblock! []
  (bdsl/defmultiblock 'wireless-matrix
    :multi-block {:positions [[0 0 0] [0 0 1] [1 0 1] [1 0 0]
                              [0 1 0] [0 1 1] [1 1 1] [1 1 0]]
                  :rotation-center [1.0 0 1.0]}
    :common {:physical {:material :stone
                        :hardness 3.0
                        :resistance 15.0
                        :requires-tool false
                        :sounds :stone}
             :rendering {:light-level 1.0}}
    :controller {:registry-name "matrix"
           :rendering {:flat-item-icon? true
             :render-shape :invisible}
                 :events {:on-right-click matrix-logic/handle-matrix-right-click
                          :on-place (matrix-logic/handle-matrix-place)
                          :on-break (matrix-logic/handle-matrix-break)}}
    :part {:registry-name "matrix_part"
           :rendering {:model-parent "minecraft:block/block"
                       :render-shape :invisible
                       ;; Explicit texture: block/block declares no particle,
                       ;; and the datagen adds the particle from this texture —
                       ;; without it the break effect is the purple-black
                       ;; missing sprite (the model itself draws nothing).
                       :textures {:all (modid/asset-path "block" "matrix")}}
           :events {:on-right-click matrix-logic/handle-matrix-right-click}}))

(defn init-wireless-matrix! []
  (machine-reg/init-machine!
    {:guard ::init
     :log-label "Wireless Matrix"
     :before matrix-logic/ensure-matrix-slot-schema!
     :tile-kind {:tile-kind :wireless-matrix
                 :tick-fn matrix-logic/matrix-scripted-tick-fn
                 :read-nbt-fn matrix-stats/matrix-scripted-load-fn
                 :write-nbt-fn matrix-stats/matrix-scripted-save-fn}
     :tiles [{:id "wireless-matrix"
              :registry-name "matrix"
              :blocks ["wireless-matrix" "wireless-matrix-part"]
              :tile-kind :wireless-matrix}]
     :tile-ids ["wireless-matrix"]
     :capabilities [{:key :wireless-matrix
                     :interface IWirelessMatrix
                     :factory (fn [be _side] (matrix-capability/->WirelessMatrixImpl be))}]
     :containers {"wireless-matrix" matrix-logic/matrix-container-fns}
     :after register-matrix-multiblock!
     :network-handler matrix-handlers/register-network-handlers!
     :client-renderer 'cn.li.ac.block.wireless-matrix.render/init!}))

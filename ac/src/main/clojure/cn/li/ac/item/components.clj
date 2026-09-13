(ns cn.li.ac.item.components
  "Component items - building blocks for Wireless Matrix system

  These items are used to construct matrix cores, nodes, and other wireless components."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.ac.item.terminal-installer-handler :as terminal-installer-handler]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Silbarn - thrown marker entity (matches original ItemSilbarn behavior)
;; ============================================================================

(def ^:private silbarn-entity-id (modid/namespaced-path "entity_silbarn"))

(defn- play-silbarn-throw-sound!
  "Matches original ItemSilbarn#onItemRightClick: egg-throw sound, volume 0.5,
   pitch 0.4 / (rand[0,1) * 0.4 + 0.8) ~= [0.333, 0.5)."
  [player]
  (when (world-effects/available?)
    (when-let [world-id (world/dimension-id (entity/player-get-level player))]
      (world-effects/play-sound!
        world-id
        (entity/entity-get-x player) (entity/entity-get-y player) (entity/entity-get-z player)
        "minecraft:entity.egg.throw"
        :players
        0.5
        (/ 0.4 (+ (* (rand) 0.4) 0.8))))))

(defn- throw-silbarn!
  [{:keys [player side]}]
  (when (= side :server)
    (play-silbarn-throw-sound! player)
    (when (entity/player-spawn-entity-by-id! player silbarn-entity-id 1.0)
      (entity/player-consume-main-hand-item! player 1)))
  {:consume? true})

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-components! []
  (install/framework-once! ::components-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "wafer"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["基础计算晶圆"
                                "用于构建矩阵核心"]
                      :model-texture "wafer"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "terminal_installer"
        {:max-stack-size 1
         :creative-tab :tools
         :properties {:tooltip ["终端安装工具"
                                "用于在方块上安装无线通信终端"]
                      :model-texture "terminal_installer"}
         :on-right-click (fn [event-data]
                           (let [{:keys [player side]} event-data]
                             (or (when (= side :server)
                                   (terminal-installer-handler/handle-right-click player))
                                 {:consume? true})))}))
    (idsl/register-item!
      (idsl/create-item-spec
        "silbarn"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["银质材料"
                                "具有共鸣特性的稀有材料"]
                      :model-texture "silbarn"}
         :on-right-click throw-silbarn!}))
    (idsl/register-item!
      (idsl/create-item-spec
        "reso_crystal"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["共鸣晶体"
                                "用于放大无线信号的晶体"]
                      :model-texture "reso_crystal"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "resonance_component"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["共鸣组件"
                                "集成在矩阵中以增强功能"]
                      :model-texture "resonance_component"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "reinforced_iron_plate"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["强化铁板"
                                "用于构建装甲结构"]
                      :model-texture "reinforced_iron_plate"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "needle"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["针状组件"
                                "精密制造的微小部件"]
                      :model-texture "needle"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "coin"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["能力硬币"
                                "电磁炮 QTE 投掷触发物"]
                      :model-texture "coin_front"
                      ;; Upstream ItemCoin TEISR (ItemCoin#onModelBake):
                      ;; FP scale 0.5 + translate(0.2,0,-0.1); TP scale 0.2;
                      ;; ground scale(-0.3,-0.3,0.3) + translate(0,0.1,0).
                      ;; JSON translation is in 1/16-block units (*16 of matrix).
                      :item-model-display
                      {:firstperson_righthand {:scale [0.5 0.5 0.5]
                                               :translation [3.2 0.0 -1.6]}
                       :firstperson_lefthand {:scale [0.5 0.5 0.5]
                                              :translation [3.2 0.0 -1.6]}
                       :thirdperson_righthand {:scale [0.2 0.2 0.2]}
                       :thirdperson_lefthand {:scale [0.2 0.2 0.2]}
                       :ground {:scale [-0.3 -0.3 0.3]
                                :translation [0.0 1.6 0.0]}}}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "brain_component"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["脑波组件"
                                "用于高级能力设备"]
                      :model-texture "brain_component"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "energy_convert_component"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["能量转换组件"
                                "用于能量转换设备"]
                      :model-texture "energy_convert_component"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "info_component"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["信息组件"
                                "用于终端和信息处理设备"]
                      :model-texture "info_component"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "magnetic_coil"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["磁力线圈"
                                "用于超电磁炮技能"]
                      :model-texture "magnetic_coil"}}))
    (idsl/register-item!
      (idsl/create-item-spec
        "logo"
        {:max-stack-size 64
         :creative-tab nil
         :properties {:tooltip ["AcademyCraft"]
                      :model-texture "logo"}}))
    (log/debug "Component items initialized: wafer, terminal-installer, silbarn,"
              "reso-crystal, resonance-component, reinforced-iron-plate, needle, coin,"
              "brain-component, energy-convert-component, info-component, magnetic-coil, logo"))))

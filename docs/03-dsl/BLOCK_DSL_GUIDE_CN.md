# Block DSL 使用指南

## 概述

Block DSL 提供声明式方式定义方块元数据，**由 `cn.li.mcmod.protocol.metadata` 向平台层暴露查询接口**，平台注册循环不写死具体方块 id。

### 与项目架构的关系（必读）

1. **实现位置**：宏与 `BlockSpec` 定义在 **`cn.li.mcmod.block.dsl`**（简称下文 **`bdsl`**）；内容侧在 **`ac`** 的命名空间中 `(require [cn.li.mcmod.block.dsl :as bdsl])` 后调用 **`defblock`**。
2. **注册表**：每个 **`defblock`** 调用 **`register-block!`**，把校验后的 **`BlockSpec`** 存入 **`block-registry`**（atom）。**方块 id** 为 DSL 符号名的字符串形式（如 `wireless-node-basic` → `"wireless-node-basic"`）。
3. **BlockSpec 结构**（嵌套记录）：
   - **`:physical`** — `PhysicalProperties`：`:material`、`:hardness`、`:resistance`、采集工具等。
   - **`:rendering`** — `RenderingProperties`：`:model-parent`、`:textures`、`:model-textures`、`:has-item-form?`、`:light-level` 等。
   - **`:block-state`** — `BlockStateConfig`：动态 **`block-state-properties`**（供 `blockstate_properties` 生成 `Property`）。
  - **`:events`** — `EventHandlers`：`:on-right-click`、`:on-break`、`:on-place`、`:on-multi-block-break`。
   - **`:multi-block`** — `MultiBlockConfig`：多方块体积、不规则坐标、`controller-parts` 模式等。
4. **BlockEntity 元数据**：需要 Scripted BlockEntity 的方块必须在 **`cn.li.mcmod.block.tile-dsl`** 中用 **`deftile`** 显式绑定 block-id -> tile-id；Block DSL 不再承载 tile 生命周期钩子。
5. **进阶宏**：**`defmultiblock`** 一次生成 controller + part 两个 **`defblock`**，并在 `:multi-block` 中设置 controller-parts 元数据。
6. **事件进游戏**：平台侧通过 **`cn.li.mcmod.block.query`** + **`cn.li.mcmod.events.dispatcher`** 直接从 `BlockSpec :events` 分发。

完整启动链、与 `deftile`/Forge 注册的关系见 **`docs/02-architecture/Runtime_And_DSL_CN.md`**。

### AC 机器方块注册（`ac/block`）

业务方块不直接散落 `register-block!`，而是通过 **`cn.li.ac.block.machine.registration/init-machine!`** 一次性注册 tile、capability、container、block spec 与 hooks。块 spec 仍由 `bdsl/create-block-spec` 或 **`bdsl/defmultiblock`** 生成；`ac/block` 内多方块推荐 **仅使用 `defmultiblock`**（与 wind、matrix、developer 一致）。`create-block-spec` 的 `:multi-block` 保留为 mcmod DSL 通用能力，新内容优先 `defmultiblock`。

状态与 GUI 约定见 **`docs/02-architecture/AC_MODULE_LAYERING.md`** 中 Block machines 一节。

## 核心概念

### 1. Block DSL 核心 (`cn.li.mcmod.block.dsl`)

提供 **`defblock`**、**`defmultiblock`** 与运行时查询（**`get-block-spec`**、**`list-blocks`** 等）。

#### 基础语法

```clojure
(bdsl/defblock block-name
  :physical {:material :stone
             :hardness 1.5
             :resistance 6.0
             :requires-tool true
             :harvest-tool :pickaxe
             :harvest-level 0
             :sounds :stone}
  :rendering {:light-level 0}
  :events {:on-right-click fn
           :on-break fn
           :on-place fn})
```

### 模型策略官方字段（Datagen 推荐）

为了让模型生成不依赖回退规则，Block DSL 支持以下官方字段：

```clojure
:model-parent   "minecraft:block/cube_all"    ; 可选，模型 parent
:textures       {:all "academy:blocks/matrix"} ; 可选，基础纹理映射
:model-textures {"node_basic_energy_0" "academy:blocks/node_basic_energy_0"
                 "node_basic_energy_1" "academy:blocks/node_basic_energy_1"}
                                              ; 可选，按具体 model 名覆盖纹理
```

优先级（model provider）：

1. `:model-textures`（按 model 名精确匹配）
2. `:textures`（通用纹理）
3. `:properties` 下同名配置（仅用于 datagen 的扩展字段）
4. 自动回退到 `academy:blocks/<model-name>`

> 建议：新增方块优先声明 `:model-parent`/`:textures`/`:model-textures`，避免仅依赖回退推导。

### 2. 材质类型

```clojure
:stone   ; 石头
:wood    ; 木头
:metal   ; 金属
:glass   ; 玻璃
:dirt    ; 泥土
:sand    ; 沙子
:grass   ; 草
:leaves  ; 树叶
:water   ; 水
:lava    ; 岩浆
:air     ; 空气
```

### 3. 工具类型

```clojure
:pickaxe  ; 镐
:axe      ; 斧
:shovel   ; 铲
:hoe      ; 锄
:sword    ; 剑
```

### 4. 音效类型

```clojure
:stone   ; 石头音效
:wood    ; 木头音效
:metal   ; 金属音效
:glass   ; 玻璃音效
:grass   ; 草地音效
:sand    ; 沙子音效
:gravel  ; 沙砾音效
```

## 完整示例

### 示例 1：基础方块

```clojure
(ns cn.li.ac.content.example-blocks
  (:require [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.util.log :as log]))

;; 简单的石头方块
(bdsl/defblock custom-stone
  :material :stone
  :hardness 1.5
  :resistance 6.0
  :requires-tool true
  :harvest-tool :pickaxe
  :sounds :stone
  :model-parent "minecraft:block/cube_all"
  :textures {:all "academy:blocks/custom_stone"})
```

### 示例 1.1：显式多模型纹理（适合动态/分层模型）

```clojure
(bdsl/defblock wireless-node-basic
  :registry-name "node_basic"
  :material :metal
  :hardness 2.5
  :resistance 6.0
  :model-parent "minecraft:block/cube_all"
  :model-textures {"node_basic_base"      "academy:blocks/node_basic_base"
                   "node_basic_connected" "academy:blocks/node_basic_connected"
                   "node_basic_energy_0"  "academy:blocks/node_basic_energy_0"
                   "node_basic_energy_1"  "academy:blocks/node_basic_energy_1"
                   "node_basic_energy_2"  "academy:blocks/node_basic_energy_2"
                   "node_basic_energy_3"  "academy:blocks/node_basic_energy_3"
                   "node_basic_energy_4"  "academy:blocks/node_basic_energy_4"})
```

此写法可让 datagen 直接读取 DSL 声明，减少隐式命名约定带来的风险。

### 示例 2：带交互的方块

```clojure
;; 可点击的方块
(bdsl/defblock clickable-block
  :material :metal
  :hardness 3.0
  :resistance 10.0
  :light-level 5
  :on-right-click (fn [event-data]
                    (log/info "Block clicked!")
                    (let [{:keys [player world pos]} event-data]
                      ;; 打开 GUI 或执行其他逻辑
                      )))
```

### 示例 3：发光方块

```clojure
;; 发光方块
(bdsl/defblock lamp
  :material :glass
  :hardness 1.0
  :resistance 1.0
  :light-level 15        ; 最大亮度
  :sounds :glass)
```

### 示例 4：矿石

```clojure
;; 铜矿石
(bdsl/defblock copper-ore
  :material :stone
  :hardness 3.0
  :resistance 3.0
  :requires-tool true
  :harvest-tool :pickaxe
  :harvest-level 1       ; 石镐可挖
  :sounds :stone
  :on-break (fn [event-data]
              (log/info "Copper ore mined!")
              ;; 可以添加掉落物逻辑
              ))
```

### 示例 5：滑溜方块

```clojure
;; 冰块（滑溜）
(bdsl/defblock ice
  :material :glass
  :hardness 0.5
  :resistance 0.5
  :friction 0.98         ; 高摩擦力 = 滑溜
  :slip-factor 0.98
  :sounds :glass)
```

## 预设系统

DSL 提供了预设函数来快速创建常见类型的方块。

### 矿石预设

```clojure
(def my-ore-preset (bdsl/ore-preset 2))
;; => {:material :stone
;;     :hardness 3.0
;;     :resistance 3.0
;;     :requires-tool true
;;     :harvest-tool :pickaxe
;;     :harvest-level 2
;;     :sounds :stone}
```

### 木头预设

```clojure
(def my-wood-preset (bdsl/wood-preset))
;; => {:material :wood
;;     :hardness 2.0
;;     :resistance 3.0
;;     :requires-tool false
;;     :harvest-tool :axe
;;     :harvest-level 0
;;     :sounds :wood}
```

### 金属预设

```clojure
(def my-metal-preset (bdsl/metal-preset 3))
;; => {:material :metal
;;     :hardness 5.0
;;     :resistance 6.0
;;     :requires-tool true
;;     :harvest-tool :pickaxe
;;     :harvest-level 3
;;     :sounds :metal}
```

### 玻璃预设

```clojure
(def my-glass-preset (bdsl/glass-preset))
;; => {:material :glass
;;     :hardness 0.3
;;     :resistance 0.3
;;     :requires-tool false
;;     :sounds :glass}
```

### 发光方块预设

```clojure
(def my-light-preset (bdsl/light-block-preset 12))
;; => {:material :glass
;;     :hardness 1.0
;;     :resistance 1.0
;;     :light-level 12
;;     :sounds :glass}
```

### 多方格方块预设

```clojure
(def my-multiblock-preset (bdsl/multi-block-preset {:width 3 :height 4 :depth 3}))
;; => {:multi-block? true
;;     :multi-block-size {:width 3 :height 4 :depth 3}
;;     :multi-block-origin {:x 0 :y 0 :z 0}
;;     :material :metal
;;     :hardness 5.0
;;     :resistance 10.0
;;     :requires-tool true
;;     :harvest-tool :pickaxe}
```

## 预设合并

可以组合多个预设和自定义选项：

```clojure
(def magical-ore
  (bdsl/merge-presets
    (bdsl/ore-preset 2)           ; 基础矿石属性
    {:light-level 8               ; 添加发光
     :on-break (fn [event-data]   ; 添加破坏处理器
                 (log/info "Magical ore broken!"))}))

;; 使用合并后的预设
(bdsl/defblock magical-diamond-ore
  :material (:material magical-ore)
  :hardness (:hardness magical-ore)
  :light-level (:light-level magical-ore)
  :on-break (:on-break magical-ore))
```

## 交互处理器

### 右键点击

```clojure
(bdsl/defblock interactive-block
  :material :metal
  :hardness 3.0
  :on-right-click (fn [event-data]
                    (let [{:keys [player world pos]} event-data]
                      (log/info "Player" player "clicked at" pos)
                      ;; 打开 GUI
                      (container/open-gui-container player my-gui world pos))))
```

### 方块破坏

```clojure
(bdsl/defblock explosive-block
  :material :sand
  :hardness 0.5
  :on-break (fn [event-data]
              (let [{:keys [world pos]} event-data]
                (log/info "Boom at" pos)
                ;; 触发爆炸效果
                )))
```

### 方块放置

```clojure
(bdsl/defblock special-block
  :material :stone
  :hardness 2.0
  :on-place (fn [event-data]
              (let [{:keys [world pos player]} event-data]
                (log/info "Block placed by" player "at" pos)
                ;; 放置时的特殊逻辑
                )))
```

## 多方格方块（Multi-Block Structures）

多方格方块允许创建占据多个方格的大型结构，如大型熔炉、反应堆核心、风力涡轮机等。

### 基本语法

```clojure
(bdsl/defblock large-furnace
  :multi-block? true                           ; 启用多方格
  :multi-block-size {:width 2 :height 3 :depth 2}  ; 尺寸（宽x高x深）
  :material :metal
  :hardness 5.0
  :resistance 10.0
  :on-right-click (fn [event-data]
                    (log/info "Large furnace clicked!"))
  :on-place (fn [event-data]
              (log/info "Placing large furnace structure..."))
  :on-multi-block-break (fn [event-data]
                          (log/info "Breaking entire structure!")))
```

### 多方格尺寸说明

#### 规则形状（长方体）

- `:width` - 宽度（X 轴方向，方块数）
- `:height` - 高度（Y 轴方向，方块数）
- `:depth` - 深度（Z 轴方向，方块数）

例如：`{:width 3 :height 4 :depth 3}` 创建一个 3x4x3 的结构（共 36 个方块）

#### 不规则形状（自定义位置）

对于不规则形状，使用 `:multi-block-positions` 指定每个方块的相对位置：

```clojure
:multi-block-positions [{:x 0 :y 0 :z 0}   ; 原点（必须包含）
                        {:x 1 :y 0 :z 0}   ; 相对于原点的位置
                        {:x 0 :y 1 :z 0}
                        ...]
```

**注意**：
- 必须包含原点 `{:x 0 :y 0 :z 0}`
- 所有坐标必须是整数
- 坐标是相对于原点的偏移量

### 规则多方格示例

#### 示例 1：工业储罐（3x4x3）

```clojure
(bdsl/defblock industrial-tank
  :multi-block? true
  :multi-block-size {:width 3 :height 4 :depth 3}
  :material :metal
  :hardness 5.0
  :resistance 15.0
  :requires-tool true
  :harvest-tool :pickaxe
  :sounds :metal
  :on-right-click (fn [event-data]
                    (log/info "Opening tank GUI...")
                    ;; 打开液体管理界面
                    ))
```

#### 示例 2：反应堆核心（3x3x3）

```clojure
(bdsl/defblock reactor-core
  :multi-block? true
  :multi-block-size {:width 3 :height 3 :depth 3}
  :material :metal
  :hardness 10.0
  :resistance 1200.0
  :requires-tool true
  :harvest-tool :pickaxe
  :harvest-level 3
  :sounds :metal
  :light-level 15
  :on-right-click (fn [event-data]
                    (log/info "Reactor core accessed!")
                    (log/info "WARNING: High radiation!"))
  :on-multi-block-break (fn [event-data]
                          (log/info "CRITICAL: Reactor core destroyed!")
                          ;; 触发爆炸效果
                          ))
```

#### 示例 3：望远镜（2x5x2 - 高塔结构）

```clojure
(bdsl/defblock telescope
  :multi-block? true
  :multi-block-size {:width 2 :height 5 :depth 2}
  :material :metal
  :hardness 3.0
  :resistance 5.0
  :requires-tool true
  :harvest-tool :pickaxe
  :sounds :metal
  :on-right-click (fn [event-data]
                    (log/info "Looking through telescope...")
                    ;; 显示天空观察界面
                    ))
```

#### 示例 4：风力涡轮机（5x7x5 - 大型结构）

```clojure
(bdsl/defblock wind-turbine
  :multi-block? true
  :multi-block-size {:width 5 :height 7 :depth 5}
  :material :metal
  :hardness 4.0
  :resistance 8.0
  :requires-tool true
  :harvest-tool :pickaxe
  :sounds :metal
  :on-place (fn [event-data]
              (log/info "Constructing wind turbine...")
              (log/info "Size: 5x7x5 blocks (175 blocks total)"))
  :on-right-click (fn [event-data]
                    (log/info "Wind turbine energy output: 100 FE/t")))
```

### 使用多方格预设

```clojure
;; 使用预设快速创建
(def custom-machine
  (bdsl/merge-presets
    (bdsl/multi-block-preset {:width 3 :height 2 :depth 3})
    {:light-level 10
     :on-right-click (fn [_] (log/info "Custom machine activated!"))}))

;; 应用预设创建方块
(bdsl/defblock my-custom-machine
  :multi-block? (:multi-block? custom-machine)
  :multi-block-size (:multi-block-size custom-machine)
  :material (:material custom-machine)
  :hardness (:hardness custom-machine)
  :light-level (:light-level custom-machine)
  :on-right-click (:on-right-click custom-machine))
```

### 多方格辅助函数

#### 计算所有方块位置

```clojure
;; 规则多方格
(bdsl/calculate-multi-block-positions 
  {:width 2 :height 3 :depth 2}  ; 尺寸
  {:x 0 :y 0 :z 0})               ; 原点
;; => [{:x 0 :y 0 :z 0 :relative-x 0 :relative-y 0 :relative-z 0 :is-origin? true}
;;     {:x 1 :y 0 :z 0 :relative-x 1 :relative-y 0 :relative-z 0 :is-origin? false}
;;     ...]

;; 不规则多方格
(bdsl/calculate-multi-block-positions 
  [{:x 0 :y 0 :z 0} {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0}]  ; 自定义位置
  {:x 0 :y 0 :z 0})                                      ; 原点
```

#### 规范化位置

将绝对坐标转换为相对坐标，确保原点在 (0,0,0)：

```clojure
(bdsl/normalize-positions 
  [{:x 5 :y 10 :z 3}
   {:x 6 :y 10 :z 3}
   {:x 5 :y 11 :z 3}])
;; => [{:x 0 :y 0 :z 0}
;;     {:x 1 :y 0 :z 0}
;;     {:x 0 :y 1 :z 0}]
```

#### 获取主方块位置

```clojure
(bdsl/get-multi-block-master-pos 
  {:x 5 :y 10 :z 3}                 ; 部分方块的位置
  {:relative-x 1 :relative-y 2 :relative-z 1})  ; 相对位置
;; => {:x 4 :y 8 :z 2}  ; 主方块（原点）位置
```

## 不规则多方格结构

### 基本用法

```clojure
(bdsl/defblock cross-altar
  :multi-block? true
  :multi-block-positions [{:x 0 :y 0 :z 0}   ; 中心
                          {:x 1 :y 0 :z 0}   ; 东
                          {:x -1 :y 0 :z 0}  ; 西
                          {:x 0 :y 0 :z 1}   ; 南
                          {:x 0 :y 0 :z -1}] ; 北
  :material :stone
  :hardness 3.0
  :light-level 10
  :on-right-click (fn [event-data]
                    (log/info "Cross altar activated!")))
```

### 形状辅助函数

Block DSL 提供了多个辅助函数来生成常见的不规则形状：

#### 1. 十字形（Cross Shape）

```clojure
(bdsl/create-cross-shape 3)
;; 创建一个十字形，每条臂长度为 3
;; 结果：中心 + 4个方向各3个方块

(bdsl/defblock cross-platform
  :multi-block? true
  :multi-block-positions (flatten (bdsl/create-cross-shape 2))
  :material :metal
  :hardness 4.0)
```

#### 2. L形（L Shape）

```clojure
(bdsl/create-l-shape 3 3)
;; 创建 L 形：宽度3，高度3

(bdsl/defblock l-workbench
  :multi-block? true
  :multi-block-positions (bdsl/create-l-shape 4 4)
  :material :wood
  :hardness 2.5)
```

#### 3. T形（T Shape）

```clojure
(bdsl/create-t-shape 5 3)
;; 创建 T 形：顶部横条宽度5，竖条高度3

(bdsl/defblock t-beacon
  :multi-block? true
  :multi-block-positions (bdsl/create-t-shape 3 4)
  :material :metal
  :light-level 15)
```

#### 4. 金字塔（Pyramid）

```clojure
(bdsl/create-pyramid-shape 5 4)
;; 创建金字塔：底座5x5，高度4层

(bdsl/defblock pyramid-shrine
  :multi-block? true
  :multi-block-positions (bdsl/create-pyramid-shape 5 4)
  :material :stone
  :hardness 4.0
  :light-level 12
  :on-right-click (fn [_] (log/info "Ancient power activated!")))
```

#### 5. 空心立方体（Hollow Cube）

```clojure
(bdsl/create-hollow-cube 5)
;; 创建 5x5x5 的空心立方体（只有外壳）

(bdsl/defblock energy-chamber
  :multi-block? true
  :multi-block-positions (bdsl/create-hollow-cube 5)
  :material :metal
  :hardness 8.0
  :light-level 15
  :on-right-click (fn [_] (log/info "Energy chamber accessed!")))
```

### 不规则多方格示例

#### 示例 1：星形传送门

```clojure
(bdsl/defblock star-portal
  :multi-block? true
  :multi-block-positions [{:x 0 :y 0 :z 0}    ; 中心
                          ;; 四个主方向
                          {:x 1 :y 0 :z 0}
                          {:x 2 :y 0 :z 0}
                          {:x -1 :y 0 :z 0}
                          {:x -2 :y 0 :z 0}
                          {:x 0 :y 0 :z 1}
                          {:x 0 :y 0 :z 2}
                          {:x 0 :y 0 :z -1}
                          {:x 0 :y 0 :z -2}
                          ;; 四个对角
                          {:x 1 :y 0 :z 1}
                          {:x 1 :y 0 :z -1}
                          {:x -1 :y 0 :z 1}
                          {:x -1 :y 0 :z -1}]
  :material :metal
  :hardness 10.0
  :resistance 100.0
  :light-level 15
  :on-right-click (fn [event-data]
                    (log/info "Dimensional gateway opening!")))
```

#### 示例 2：螺旋楼梯

```clojure
(bdsl/defblock spiral-staircase
  :multi-block? true
  :multi-block-positions [{:x 0 :y 0 :z 0}
                          {:x 1 :y 1 :z 0}
                          {:x 1 :y 2 :z 1}
                          {:x 0 :y 3 :z 1}
                          {:x -1 :y 4 :z 1}
                          {:x -1 :y 5 :z 0}
                          {:x -1 :y 6 :z -1}
                          {:x 0 :y 7 :z -1}]
  :material :stone
  :hardness 3.0
  :on-place (fn [_]
              (log/info "Building spiral staircase...")
              (log/info "Height: 8 blocks")))
```

#### 示例 3：使用预设和形状函数

```clojure
;; 使用不规则多方格预设
(def cross-platform
  (bdsl/merge-presets
    (bdsl/irregular-multi-block-preset 
      (flatten (bdsl/create-cross-shape 3)))
    {:id "cross-platform"
     :material :metal
     :light-level 8
     :on-right-click (fn [_] (log/info "Platform activated!"))}))

;; 组合多个形状
(def complex-structure-positions
  (concat
    (bdsl/create-cross-shape 2)
    (map #(update % :y inc) (bdsl/create-cross-shape 1))))

(bdsl/defblock complex-structure
  :multi-block? true
  :multi-block-positions (flatten complex-structure-positions)
  :material :metal
  :hardness 5.0)
```

### 多方格交互处理器

多方格方块支持特殊的 `:on-multi-block-break` 处理器，当结构的任何部分被破坏时触发：

```clojure
(bdsl/defblock fragile-structure
  :multi-block? true
  :multi-block-size {:width 3 :height 3 :depth 3}
  :material :glass
  :hardness 0.5
  :on-multi-block-break (fn [event-data]
                          (log/info "Entire structure collapsing!")
                          ;; 平台适配器会自动破坏所有部分
                          ;; 这里可以添加额外的效果
                          ))
```

### 多方格最佳实践

#### 规则多方格
1. **合理的尺寸**：不要创建过大的结构（建议不超过 7x7x7）
2. **对称设计**：规则形状适合对称结构（熔炉、储罐、反应堆）

#### 不规则多方格
1. **明确的形状**：使用有意义的形状（十字、L形、星形）
2. **位置验证**：确保包含原点 `{:x 0 :y 0 :z 0}`
3. **使用辅助函数**：利用 `create-*-shape` 函数快速生成形状
4. **位置规范化**：使用 `normalize-positions` 处理绝对坐标

#### 通用最佳实践
1. **清晰的视觉反馈**：使用不同纹理标识结构的各个部分
2. **一致的交互**：无论点击哪个部分，都应该有相同的响应
3. **完整性检查**：在交互前检查结构是否完整
4. **优雅降级**：结构破坏时给予适当的反馈
5. **性能考虑**：避免创建过于复杂的不规则形状（建议不超过50个方块）

## 高级用法

### 方块破坏

```clojure
(bdsl/defblock explosive-demo
  :material :stone
  :hardness 0.5
  :on-break (fn [event-data]
              (let [{:keys [world pos]} event-data]
                (log/info "Block broken at" pos)
                ;; 在此添加额外逻辑（爆炸、掉落等）
                )))
```

### 方块放置

```clojure
(bdsl/defblock tracked-block
  :material :stone
  :hardness 2.0
  :on-place (fn [event-data]
              (let [{:keys [player pos]} event-data]
                (log/info "Player" player "placed block at" pos)
                ;; 记录放置位置
                )))
```

## 完整功能示例

### 示例：右键打开 GUI（思路说明）

实际项目中，右键逻辑通常在 **`ac`** 各块 `gui.clj`（`cn.li.mcmod.gui.spec/register-block-gui!`）与 Forge/`mc1201` 菜单桥接中完成。下面仅保留 **`defblock`** 侧占位：

```clojure
(ns cn.li.ac.content.example-gui-block
  (:require [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.util.log :as log]))

(bdsl/defblock crafting-station
  :material :wood
  :hardness 2.5
  :resistance 2.5
  :requires-tool false
  :harvest-tool :axe
  :sounds :wood
  :on-right-click (fn [event-data]
                    (log/info "Open GUI for crafting-station" (keys event-data))
                    ;; 真实打开路径：dispatcher -> gui-adapter / MenuType（见 Runtime_And_DSL_CN.md）
                    nil))
```

### 示例：自定义熔炉（占位）

```clojure
(bdsl/defblock custom-furnace
  :material :stone
  :hardness 3.5
  :resistance 3.5
  :light-level 13
  :requires-tool true
  :harvest-tool :pickaxe
  :sounds :stone
  :on-right-click (fn [event-data]
                    ;; 与上例相同，需与 GUI 元数据、MenuType 注册一致
                    nil))
```

### 示例：传送方块

```clojure
(bdsl/defblock teleporter
  :material :metal
  :hardness 3.0
  :resistance 10.0
  :light-level 5
  :requires-tool true
  :on-right-click (fn [event-data]
                    (let [{:keys [player world]} event-data]
                      (log/info "Teleporting player...")
                      ;; 执行传送逻辑
                      )))
```

## 获取方块信息

```clojure
;; 获取已注册的方块
(bdsl/get-block-spec "demo-block")
;; => #BlockSpec{:id "demo-block" :material :stone ...}

;; 列出所有方块
(bdsl/list-blocks)
;; => ("demo-block" "copper-ore" "glowing-stone" ...)

;; 获取方块属性
(bdsl/get-block-properties my-block-spec)
;; => {:material :stone :hardness 1.5 :resistance 6.0 ...}
```

## 与 Forge 注册的关系（当前实现）

**不要**在适配层手写「遍历 `bdsl/get-block-spec` + 手动 `.register`」。Forge 1.20.1 统一走 **`cn.li.forge1201.registry.content-registration/register-core-content!`**：

- 对 **`cn.li.mcmod.protocol.metadata/get-all-block-ids`** 返回的每个 id，读取 spec；
- 按是否需要 **动态 BlockState**、**Scripted BlockEntity**、**多方块** 等，调用 Java 侧 **`invoke-bootstrap-helper`** 工厂（如 `createDynamicStateBlock`、`createCarrierScriptedBlock`）；
- BlockItem、BlockEntityType 等同理走 metadata。

因此：**内容作者只负责在 `ac` 里 `defblock`（并确保命名空间被 `content-namespaces/load-all!` 加载）**；注册表代码由 selected target 的 Loader component 通过 `mcmod` metadata 驱动。

Fabric target 同样应对齐 **`protocol.metadata`** 查询模式，不复制注册逻辑。

## API 参考

### 宏

- `(defblock name & options)` - 定义方块

### 函数

- `(create-block-spec id options)` - 创建方块规范
- `(register-block! spec)` - 注册方块
- `(get-block-spec id)` - 获取方块规范
- `(list-blocks)` - 列出所有方块
- `(get-block-properties spec)` - 获取方块属性
- `(handle-right-click spec data)` - 处理右键点击
- `(handle-break spec data)` - 处理方块破坏
- `(handle-place spec data)` - 处理方块放置

### 预设函数

- `(ore-preset harvest-level)` - 矿石预设
- `(wood-preset)` - 木头预设
- `(metal-preset harvest-level)` - 金属预设
- `(glass-preset)` - 玻璃预设
- `(light-block-preset light-level)` - 发光方块预设
- `(multi-block-preset size & options)` - 多方格方块预设
- `(merge-presets & presets)` - 合并预设

### 多方格函数

- `(calculate-multi-block-positions size origin)` - 计算多方格所有位置
- `(get-multi-block-master-pos part-pos relative-pos)` - 获取主方块位置
- `(is-multi-block-complete? world master-pos size)` - 检查结构完整性
- `(handle-multi-block-break spec data)` - 处理多方格结构破坏

## 优势

### ✅ 声明式语法
- 清晰直观的方块定义
- 易于阅读和维护

### ✅ 代码复用
- 预设系统减少重复代码
- 组合式设计

### ✅ 类型安全
- 编译时验证
- 明确的错误提示

### ✅ 跨版本支持
- 版本无关的定义
- 平台特定实现分离

### ✅ 功能完整
- 支持所有方块属性
- 交互处理器
- GUI 集成
- **规则多方格结构**（长方体）
- **不规则多方格结构**（自定义形状）
- **形状辅助函数**（十字、L形、T形、金字塔、空心立方体）

## 代码对比

### 传统 Java 方式

```java
// 每个方块需要 50+ 行代码
public class DemoBlock extends Block {
    public DemoBlock() {
        super(Properties.of(Material.STONE)
            .strength(1.5f, 6.0f)
            .requiresCorrectToolForDrops());
    }
    
    @Override
    public InteractionResult use(BlockState state, Level level, 
                                  BlockPos pos, Player player, ...) {
        // 处理逻辑
        return InteractionResult.SUCCESS;
    }
}

// 注册代码
public static final RegistryObject<Block> DEMO_BLOCK = 
    BLOCKS.register("demo_block", DemoBlock::new);
```

### DSL 方式

```clojure
;; 只需 10 行代码
(bdsl/defblock demo-block
  :material :stone
  :hardness 1.5
  :resistance 6.0
  :requires-tool true
  :on-right-click (fn [data]
                    ;; 处理逻辑
                    ))
```

**代码减少 80%**

## 总结

Block DSL 提供了：

1. **简洁的语法** - 10 行代替 50+ 行 Java
2. **预设系统** - 快速创建常见方块类型
3. **交互处理** - 内置右键、破坏、放置处理器
4. **规则多方格** - 长方体结构（如 3x4x3 熔炉）
5. **不规则多方格** - 自定义形状（十字、L形、星形、金字塔等）
6. **形状辅助函数** - 快速生成常见几何形状
7. **跨平台** - 统一定义，多版本支持
8. **类型安全** - 编译时验证，运行时检查

使用 Block DSL，你可以专注于方块的功能，而不是繁琐的样板代码！

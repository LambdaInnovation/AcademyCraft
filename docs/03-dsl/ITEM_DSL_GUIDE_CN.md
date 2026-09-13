# Item DSL 使用指南

## 概述

Item DSL 提供声明式方式定义 **`ItemSpec`** 并写入 **`item-registry`**（atom），与 Block DSL 对称；Forge/Fabric 通过 **`cn.li.mcmod.protocol.metadata/get-all-item-ids`** 等在平台注册循环中注册。

- **宏与 API**：**`cn.li.mcmod.item.dsl`**（**`idsl`**）。
- **内容加载**：在 **`ac`** 中定义（例如 **`cn.li.ac.content.items.all`**），并列入 **`cn.li.ac.registry.content-namespaces`** 的 **`item-namespaces`**，由 **`load-all!`** 触发。
- **架构总览**：见 **`docs/02-architecture/Runtime_And_DSL_CN.md`**。

## 核心概念

### 1. Item DSL 核心 (`cn.li.mcmod.item.dsl`)

提供 **`defitem`**、**`item-registry`** 及 **`get-item`** / **`list-items`**。

#### 基础语法

```clojure
(idsl/defitem item-name
  :max-stack-size 64          ; 最大堆叠数量
  :durability 100             ; 耐久度
  :creative-tab :misc         ; 创造模式标签页
  :rarity :common             ; 稀有度
  :enchantability 10          ; 附魔能力
  :food-properties {...}      ; 食物属性
  :tool-properties {...}      ; 工具属性
  :armor-properties {...}     ; 护甲属性
  :on-use fn                  ; 使用处理器
  :on-right-click fn          ; 右键点击方块处理器
  :on-finish-using fn)        ; 完成使用处理器
```

### 2. 创造模式标签页

```clojure
:building-blocks  ; 建筑方块
:decorations      ; 装饰
:redstone         ; 红石
:transportation   ; 交通
:misc             ; 杂项
:food             ; 食物
:tools            ; 工具
:combat           ; 战斗
:brewing          ; 酿造
:materials        ; 材料
:search           ; 搜索
```

### 3. 稀有度等级

```clojure
:common    ; 普通（白色）
:uncommon  ; 不常见（黄色）
:rare      ; 稀有（青色）
:epic      ; 史诗（洋红色）
```

### 4. 工具等级

```clojure
:wood      ; 木制
:stone     ; 石制
:iron      ; 铁制
:diamond   ; 钻石
:gold      ; 金制
:netherite ; 下界合金
```

### 5. 护甲材质

```clojure
:leather   ; 皮革
:chainmail ; 锁链
:iron      ; 铁
:gold      ; 金
:diamond   ; 钻石
:netherite ; 下界合金
```

## 预设系统

Item DSL 提供了多种预设，用于快速创建常见类型的物品。

### 基础物品预设

```clojure
(idsl/basic-item-preset max-stack-size creative-tab)

;; 示例：创建一个可堆叠 64 个的杂项物品
(def my-item-spec
  (idsl/basic-item-preset 64 :misc))
```

### 工具预设

```clojure
(idsl/tool-preset tier durability attack-damage attack-speed)

;; 示例：创建一个钻石镐
(def diamond-pickaxe-spec
  (idsl/tool-preset :diamond 1561 5.0 -2.8))
```

工具预设会自动根据等级设置附魔能力：
- 木制: 15
- 石制: 5
- 铁制: 14
- 钻石: 10
- 金制: 22
- 下界合金: 15

### 食物预设

```clojure
(idsl/food-preset nutrition saturation-modifier)

;; 示例：创建恢复 4 点饥饿值的食物
(def bread-spec
  (idsl/food-preset 4 0.6))
```

### 稀有物品预设

```clojure
(idsl/rare-item-preset rarity)

;; 示例：创建一个史诗级物品
(def legendary-item-spec
  (idsl/rare-item-preset :epic))
```

### 组合预设

使用 `merge-presets` 可以组合多个预设：

```clojure
(def enchanted-sword-spec
  (idsl/merge-presets
    (idsl/tool-preset :diamond 1561 7.0 -2.4)
    (idsl/rare-item-preset :epic)
    {:on-use (fn [data] (println "Divine Strike!"))}))
```

## 属性辅助函数

### 食物属性

```clojure
(idsl/food-properties
  :nutrition 4              ; 饥饿值恢复
  :saturation-modifier 0.6  ; 饱和度
  :meat false               ; 是否是肉类
  :fast-to-eat false        ; 快速食用
  :always-edible false)     ; 总是可食用
```

### 工具属性

```clojure
(idsl/tool-properties
  :tier :diamond            ; 工具等级
  :attack-damage 7.0        ; 攻击伤害
  :attack-speed -2.4)       ; 攻击速度
```

### 护甲属性

```clojure
(idsl/armor-properties
  :material :diamond        ; 护甲材质
  :slot :chest              ; 装备槽位 (:head/:chest/:legs/:feet)
  :protection 8             ; 护甲值
  :toughness 2.0)          ; 韧性
```

## 完整示例

### 示例 1：基础材料物品

```clojure
(ns cn.li.my-items
  (:require [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.util.log :as log]))

;; 铜锭
(idsl/defitem copper-ingot
  :max-stack-size 64
  :creative-tab :materials)

;; 钢锭（不常见）
(idsl/defitem steel-ingot
  :max-stack-size 64
  :creative-tab :materials
  :rarity :uncommon)

;; 秘银碎片（稀有，限制堆叠）
(idsl/defitem mythril-shard
  :max-stack-size 16
  :creative-tab :materials
  :rarity :rare)

;; 龙鳞（史诗，不可堆叠）
(idsl/defitem dragon-scale
  :max-stack-size 1
  :creative-tab :materials
  :rarity :epic)
```

### 示例 2：工具

```clojure
;; 铜镐
(idsl/defitem copper-pickaxe
  :max-stack-size 1
  :durability 200
  :creative-tab :tools
  :tool-properties (idsl/tool-properties
                     :tier :stone
                     :attack-damage 3.0
                     :attack-speed -2.8)
  :enchantability 12)

;; 钢剑
(idsl/defitem steel-sword
  :max-stack-size 1
  :durability 1500
  :creative-tab :combat
  :tool-properties (idsl/tool-properties
                     :tier :diamond
                     :attack-damage 7.0
                     :attack-speed -2.4)
  :rarity :uncommon)

;; 钻石锤（3x3 挖掘）
(idsl/defitem diamond-hammer
  :max-stack-size 1
  :durability 1561
  :creative-tab :tools
  :tool-properties (idsl/tool-properties
                     :tier :diamond
                     :attack-damage 9.0
                     :attack-speed -3.0)
  :on-use (fn [data]
            (log/info "Mining 3x3 area...")
            ;; 实现 3x3 范围挖掘逻辑
            ))
```

### 示例 3：食物

```clojure
;; 自定义金苹果
(idsl/defitem golden-apple-custom
  :max-stack-size 64
  :creative-tab :food
  :rarity :rare
  :food-properties (idsl/food-properties
                     :nutrition 4
                     :saturation-modifier 9.6
                     :always-edible true)
  :on-finish-using (fn [data]
                     (log/info "Eating golden apple...")
                     ;; 添加药水效果
                     ))

;; 魔法面包
(idsl/defitem magic-bread
  :max-stack-size 16
  :creative-tab :food
  :food-properties (idsl/food-properties
                     :nutrition 5
                     :saturation-modifier 0.6
                     :fast-to-eat true)
  :on-finish-using (fn [data]
                     (log/info "Restoring mana...")
                     ;; 恢复魔法值
                     ))

;; 治疗药水
(idsl/defitem healing-potion
  :max-stack-size 16
  :creative-tab :brewing
  :rarity :uncommon
  :on-use (fn [data]
            (log/info "Instant heal!")
            ;; 瞬间治疗效果
            ))
```

### 示例 4：特殊功能物品

```clojure
;; 传送法杖
(idsl/defitem teleport-staff
  :max-stack-size 1
  :durability 100
  :creative-tab :tools
  :rarity :epic
  :enchantability 20
  :on-use (fn [data]
            (let [{:keys [player world]} data]
              (log/info "Teleporting player...")
              ;; 实现传送逻辑
              )))

;; 火焰魔杖
(idsl/defitem wand-of-fire
  :max-stack-size 1
  :durability 500
  :creative-tab :combat
  :rarity :rare
  :enchantability 20
  :on-use (fn [data]
            (log/info "Casting fireball...")
            ;; 发射火球
            ))

;; 调试棒
(idsl/defitem debug-stick
  :max-stack-size 1
  :creative-tab :tools
  :rarity :epic
  :on-right-click (fn [data]
                    (let [{:keys [block-state]} data]
                      (log/info "Cycling block state...")
                      ;; 循环方块状态
                      )))
```

### 示例 5：货币和任务物品

```clojure
;; 金币
(idsl/defitem gold-coin
  :max-stack-size 64
  :creative-tab :misc)

;; 铂金币
(idsl/defitem platinum-coin
  :max-stack-size 64
  :creative-tab :misc
  :rarity :rare)

;; 神秘遗物
(idsl/defitem mysterious-artifact
  :max-stack-size 1
  :creative-tab :misc
  :rarity :epic
  :on-use (fn [data]
            (log/info "Artifact activated! Quest triggered.")
            ;; 触发任务事件
            ))
```

### 示例 6：饰品（Baubles）

```clojure
;; 速度指环
(idsl/defitem speed-ring
  :max-stack-size 1
  :creative-tab :misc
  :rarity :rare
  :on-use (fn [data]
            (log/info "Speed boost activated!")
            ;; 添加速度效果
            ))

;; 再生护符
(idsl/defitem regeneration-amulet
  :max-stack-size 1
  :creative-tab :misc
  :rarity :epic
  :on-use (fn [data]
            (log/info "Regeneration activated!")
            ;; 添加再生效果
            ))
```

## 使用预设的完整示例

```clojure
;; 使用基础预设
(def copper-nugget-spec
  (idsl/merge-presets
    (idsl/basic-item-preset 64 :materials)))

;; 使用工具预设 + 自定义
(def magic-sword-spec
  (idsl/merge-presets
    (idsl/tool-preset :diamond 2000 8.0 -2.4)
    (idsl/rare-item-preset :epic)
    {:enchantability 25
     :on-use (fn [data]
               (println "Magic sword special attack!"))}))

;; 使用食物预设 + 额外属性
(def enchanted-bread-spec
  (idsl/merge-presets
    (idsl/food-preset 6 0.8)
    {:rarity :uncommon
     :on-finish-using (fn [data]
                        (println "Granting temporary buff!"))}))
```

## 交互处理器

Item DSL 支持三种主要的交互处理器：

### 1. on-use（右键使用）

当玩家在空中右键使用物品时触发。

```clojure
:on-use (fn [data]
          (let [{:keys [player world hand item-stack]} data]
            ;; 处理使用逻辑
            (log/info "Item used!")
            ;; 返回 ActionResult (可选)
            ))
```

**data 包含的键：**
- `:player` - 使用物品的玩家
- `:world` - 世界对象
- `:hand` - 使用的手（主手/副手）
- `:item-stack` - 物品堆栈

### 2. on-right-click（右键点击方块）

当玩家右键点击方块时触发。

```clojure
:on-right-click (fn [data]
                  (let [{:keys [player world pos hand block-state]} data]
                    ;; 处理右键方块逻辑
                    (log/info "Clicked block at" pos)
                    ;; 返回 ActionResult (可选)
                    ))
```

**data 包含的键：**
- `:player` - 使用物品的玩家
- `:world` - 世界对象
- `:pos` - 方块坐标
- `:hand` - 使用的手
- `:block-state` - 方块状态
- `:face` - 点击的面
- `:hit-vec` - 点击位置向量

### 3. on-finish-using（完成使用）

当玩家完成使用物品（如吃完食物）时触发。

```clojure
:on-finish-using (fn [data]
                   (let [{:keys [player world item-stack]} data]
                     ;; 处理完成使用逻辑
                     (log/info "Finished using item!")
                     ;; 返回新的 ItemStack (可选)
                     ))
```

**data 包含的键：**
- `:player` - 使用物品的玩家
- `:world` - 世界对象
- `:item-stack` - 物品堆栈

## API 函数

### 获取物品规格

```clojure
;; 获取已注册的物品规格
(idsl/get-item "demo-item")
;; => ItemSpec 记录

;; 获取物品属性映射
(idsl/get-item-properties "demo-item")
;; => {:max-stack-size 64, :creative-tab :misc, ...}
```

### 获取所有已注册物品

```clojure
(idsl/get-all-items)
;; => {"demo-item" #ItemSpec{...}, "copper-ingot" #ItemSpec{...}, ...}
```

### 创建平台特定物品

```clojure
;; 由平台适配器实现
(idsl/create-platform-item item-spec)
;; => 返回对应版本的 Item 实例
```

## 初始化物品

**当前项目不需要**单独的 `init-demo-items!`：只要把物品定义放在 **`ac`** 的命名空间中，并在 **`cn.li.ac.registry.content-namespaces`** 的 **`item-namespaces`** 里声明，**`cn.li.ac.core/init`** → **`content-ns/load-all!`** 会在适当时机加载；平台侧 **`register-all-items!`** 读取 **`protocol.metadata`**。

```clojure
;; 仅作概念示例：物品命名空间被 require 后 defitem 即生效
(ns cn.li.ac.content.items.all
  (:require [cn.li.mcmod.item.dsl :as idsl]))

(idsl/defitem example-ingot
  :id "example_ingot"
  :max-stack-size 64)
```

## 最佳实践

### 1. 组织物品定义

按类型拆分为多个 **`ac`** 下命名空间（示例路径）：

```clojure
;; ac/.../content/items/materials.clj
(ns cn.li.ac.content.items.materials
  (:require [cn.li.mcmod.item.dsl :as idsl]))

(idsl/defitem copper-ingot ...)
(idsl/defitem steel-ingot ...)

;; ac/.../content/items/tools.clj
(ns cn.li.ac.content.items.tools
  (:require [cn.li.mcmod.item.dsl :as idsl]))

(idsl/defitem copper-pickaxe ...)
(idsl/defitem steel-sword ...)

;; ac/.../content/items/food.clj
(ns cn.li.ac.content.items.food
  (:require [cn.li.mcmod.item.dsl :as idsl]))

(idsl/defitem magic-bread ...)
(idsl/defitem healing-potion ...)
```

并在 **`content-namespaces.clj`** 的 **`item-namespaces`** 中列出上述命名空间。

### 2. 使用预设减少重复

```clojure
;; 定义自己的预设
(defn my-tool-preset [tier durability]
  (idsl/merge-presets
    (idsl/tool-preset tier durability 5.0 -2.4)
    {:creative-tab :tools
     :enchantability 15}))

;; 使用自定义预设
(def my-pickaxe-spec
  (my-tool-preset :diamond 1561))
```

### 3. 共享处理器函数

```clojure
;; 定义可重用的处理器
(defn healing-effect [amount]
  (fn [data]
    (let [{:keys [player]} data]
      (log/info "Healing player for" amount)
      ;; 治疗逻辑
      )))

;; 在多个物品中使用
(idsl/defitem minor-healing-potion
  :on-use (healing-effect 5))

(idsl/defitem major-healing-potion
  :on-use (healing-effect 20))
```

### 4. 验证输入

Item DSL 会自动验证：
- 物品 ID 必须是非空字符串
- 如果设置了 durability，max-stack-size 必须为 1

```clojure
;; 错误：耐久物品不能堆叠
(idsl/defitem bad-item
  :durability 100
  :max-stack-size 64)  ; 会抛出异常！

;; 正确：耐久物品堆叠为 1
(idsl/defitem good-item
  :durability 100
  :max-stack-size 1)
```

## 与传统方式对比

### 传统 Java 方式（~50-100 行）

```java
public class CustomItem extends Item {
    public CustomItem() {
        super(new Item.Properties()
            .stacksTo(64)
            .tab(CreativeModeTab.TAB_MISC)
            .rarity(Rarity.RARE)
            .durability(500));
    }
    
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (!world.isClientSide) {
            // 使用逻辑
        }
        return InteractionResult.SUCCESS;
    }
    
    // 更多覆盖方法...
}

// 注册代码
public static final RegistryObject<Item> CUSTOM_ITEM = 
    ITEMS.register("custom_item", CustomItem::new);
```

### Item DSL 方式（~10 行）

```clojure
(idsl/defitem custom-item
  :max-stack-size 64
  :creative-tab :misc
  :rarity :rare
  :durability 500
  :on-use (fn [data]
            (let [{:keys [player world]} data]
              ;; 使用逻辑
              )))
```

**代码减少：80%+**

## 高级技巧

### 1. 动态物品生成

```clojure
(defn create-metal-set [metal-name tier durability]
  (let [base-props {:creative-tab :tools
                    :rarity :uncommon}]
    [(idsl/merge-presets
       (idsl/tool-preset tier durability 3.0 -2.8)
       base-props
       {:id (str metal-name "-pickaxe")})
     (idsl/merge-presets
       (idsl/tool-preset tier durability 7.0 -2.4)
       base-props
       {:id (str metal-name "-sword")})]))

;; 生成整套工具
(def copper-tools (create-metal-set "copper" :stone 200))
(def steel-tools (create-metal-set "steel" :diamond 1500))
```

### 2. 条件属性

```clojure
(defn conditional-item [id base-props should-glow?]
  (idsl/merge-presets
    base-props
    (when should-glow?
      {:rarity :rare
       :enchantability 20})))
```

### 3. 物品系列

```clojure
(defn create-coin-series []
  (doseq [[name rarity] [["copper-coin" :common]
                         ["silver-coin" :uncommon]
                         ["gold-coin" :rare]
                         ["platinum-coin" :epic]]]
    (idsl/defitem name
      :max-stack-size 64
      :creative-tab :misc
      :rarity rarity)))

(create-coin-series)
```

## 调试技巧

### 1. 打印物品规格

```clojure
(require '[clojure.pprint :refer [pprint]])

;; 查看物品定义
(pprint (idsl/get-item "demo-item"))

;; 查看所有物品
(pprint (idsl/get-all-items))
```

### 2. 验证物品属性

```clojure
(defn validate-item [item-id]
  (if-let [spec (idsl/get-item item-id)]
    (do
      (println "✓ Item found:" item-id)
      (pprint (idsl/get-item-properties item-id)))
    (println "✗ Item not found:" item-id)))

(validate-item "demo-item")
```

### 3. 测试处理器

```clojure
(defn test-handler [item-id event-type test-data]
  (let [spec (idsl/get-item item-id)
        handler (get spec event-type)]
    (if handler
      (do
        (println "Testing" event-type "for" item-id)
        (handler test-data))
      (println "No handler for" event-type))))

(test-handler "teleport-staff" :on-use {:player "test-player" :world "test-world"})
```

## 常见问题

### Q: 如何设置物品的纹理？

A: 纹理由资源包定义，不在 DSL 中。创建 `assets/mymod/models/item/item_id.json` 和对应的纹理文件。

### Q: 能否动态修改已注册的物品？

A: 不建议。物品在注册后应该是不可变的。如果需要修改，应在注册前完成。

### Q: 如何创建工具可以挖掘的方块列表？

A: 这通常在方块标签（tags）中定义，不在物品 DSL 中。

### Q: 支持自定义渲染吗？

A: Item DSL 关注逻辑定义。自定义渲染需要通过 Minecraft 的渲染系统单独实现。

### Q: 如何添加附魔？

A: 设置 `:enchantability` 属性即可。具体的附魔应用由游戏系统处理。

## 总结

Item DSL 提供了：

- ✅ **简洁的语法**：用 10 行代码替代 100 行 Java
- ✅ **类型安全**：Clojure 记录 + 验证
- ✅ **预设系统**：快速创建常见物品类型
- ✅ **灵活组合**：merge-presets 支持多预设组合
- ✅ **交互处理**：on-use、on-right-click、on-finish-using
- ✅ **跨版本**：multimethod 分发支持多版本
- ✅ **REPL 友好**：支持交互式开发和测试

开始使用 Item DSL，让物品定义更简单、更优雅！

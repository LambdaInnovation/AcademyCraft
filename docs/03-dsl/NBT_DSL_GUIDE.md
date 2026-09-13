# NBT DSL 使用指南

## 概述

NBT DSL 是一个声明式的 NBT 序列化系统，使用 Clojure 宏自动生成读写函数，用于 TileEntity、容器状态等持久化。实现位于 **`cn.li.mcmod.nbt.dsl`**。与方块 schema 生成、无线节点等结合时，见 **`docs/02-architecture/Runtime_And_DSL_CN.md`**。

## 核心优势

- **声明式语法**: 只需描述字段结构，无需手写读写逻辑
- **类型安全**: 编译时类型检查，支持 8 种内置类型
- **代码减少**: 相比手动实现减少 80%+ 样板代码
- **自动文档**: 生成的函数包含完整文档字符串
- **可扩展**: 支持自定义转换器和转换函数

## 快速开始

### 1. 引入命名空间

```clojure
(ns your-namespace
  (:require [cn.li.mcmod.nbt.dsl :as nbt]))
```

### 2. 定义 NBT 映射

```clojure
(nbt/defnbt player
  [:name "playerName" :string]
  [:health "health" :double]
  [:level "level" :int]
  [:is-flying "isFlying" :boolean])
```

### 3. 使用生成的函数

```clojure
;; 保存
(write-player-to-nbt player nbt-compound)

;; 加载
(read-player-from-nbt player nbt-compound)
```

## 支持的类型

| 类型 | NBT 方法 | 说明 |
|------|---------|------|
| `:double` | setDouble/getDouble | 双精度浮点数 |
| `:string` | setString/getString | 字符串 |
| `:int` | setInteger/getInteger | 整数 |
| `:boolean` | setBoolean/getBoolean | 布尔值 |
| `:float` | setFloat/getFloat | 单精度浮点数 |
| `:long` | setLong/getLong | 长整数 |
| `:keyword` | setString/getString | Clojure 关键字（自动转换） |
| `:inventory` | 特殊 | 无内建转换器；建议用字段级 `:load-fn` / `:save-fn` 处理 ItemStack 向量 |

## 字段规范格式

```clojure
[field-key nbt-key type & options]
```

- `field-key`: Clojure 记录中的字段名（关键字）
- `nbt-key`: NBT 中的键名（字符串）
- `type`: 字段类型（关键字）
- `options`: 可选配置（键值对）

## 高级选项

### :getter / :setter - 协议方法调用

简化协议 getter/setter 调用模式：

```clojure
(nbt/defnbt node
  ;; 使用 :getter 和 :setter（推荐）
  [:energy "energy" :double
   :getter winterfaces/get-energy
   :setter winterfaces/set-energy]
  
  [:node-name "nodeName" :string
   :getter winterfaces/get-node-name
   :setter set-node-name!])

;; 相当于：
;; Write: (.setDouble nbt "energy" (winterfaces/get-energy tile))
;; Read: (winterfaces/set-energy tile (.getDouble nbt "energy"))
```

**优势**：
- ✅ 无需手写 lambda 函数
- ✅ 代码更简洁（减少 70% 代码）
- ✅ 直接引用函数，性能更好
- ✅ 类型检查更清晰

### :atom? - Atom 类型字段

自动处理 atom 的解引用和更新：

```clojure
(nbt/defnbt matrix
  [:plate-count "plateCount" :int :atom? true])

;; 相当于：
;; 写入: (.setInteger nbt "plateCount" @(:plate-count tile))
;; 读取: (reset! (:plate-count tile) (.getInteger nbt "plateCount"))
```

### :custom-write / :custom-read - 自定义序列化

处理复杂逻辑（需要完全控制读写过程时使用）：

**注意**：大多数情况下使用 `:getter`/`:setter` 更简单！

```clojure
(nbt/defnbt node
  [:energy "energy" :double
   :custom-write (fn [tile nbt key _]
                   ;; 完全自定义写入逻辑
                   (.setDouble nbt key (* 2 (winterfaces/get-energy tile))))
   :custom-read (fn [tile nbt key]
                  ;; 完全自定义读取逻辑
                  (when (.hasKey nbt key)
                    (winterfaces/set-energy tile (/ (.getDouble nbt key) 2))))])
```

**何时使用 :custom-write/:custom-read？**
- ✅ 需要在读写前进行复杂计算
- ✅ 需要访问 nbt 对象做额外操作
- ✅ 需要条件性地跳过某些步骤

**何时使用 :getter/:setter？**（推荐）
- ✅ 简单的协议方法调用
- ✅ 直接的函数映射
- ✅ 不需要访问 nbt 对象

### :default - 默认值

NBT 中不存在时使用的默认值：

```clojure
(nbt/defnbt config
  [:timeout "timeout" :int :default 60])
```

### :skip-on-write? - 条件跳过

根据条件决定是否写入：

```clojure
(nbt/defnbt cache
  [:temp-data "tempData" :string
   :skip-on-write? (fn [tile] (:is-temporary tile))])
```

### :transform-write / :transform-read - 值转换

在读写前转换值：

```clojure
(nbt/defnbt entity
  [:rotation "rotation" :double
   :transform-write (fn [deg] (Math/toRadians deg))
   :transform-read (fn [rad] (Math/toDegrees rad))])
```

## 完整示例

### NodeTileEntity (使用 :getter/:setter)

```clojure
(nbt/defnbt node
  ;; 使用 getter/setter（推荐方式）
  [:energy "energy" :double
   :getter winterfaces/get-energy
   :setter winterfaces/set-energy]
  
  [:node-name "nodeName" :string
   :getter winterfaces/get-node-name
   :setter set-node-name!]
  
  [:password "password" :string
   :getter winterfaces/get-password
   :setter set-password-str!]
  
  ;; 直接字段访问
  [:placer-name "placer" :string]
  
  ;; 特殊类型：inventory
  [:inventory "inventory" :inventory])
```

### TileMatrix (带 atom 和 keyword)

```clojure
(nbt/defnbt matrix
  [:placer-name "placer" :string]
  [:plate-count "plateCount" :int :atom? true]  ; atom 类型
  [:sub-id "subId" :int]
  [:direction "direction" :keyword]              ; keyword 自动转换
  [:inventory "inventory" :inventory])
```

### 简化版本 (纯字段)

```clojure
(nbt/defnbt-simple config
  :server-name "serverName" :string
  :max-players "maxPlayers" :int
  :pvp-enabled "pvpEnabled" :boolean)
```

## 代码对比

### 重构前（手动实现 - 40 行）

```clojure
(defn write-matrix-to-nbt [tile nbt]
  (.setString nbt "placer" (:placer-name tile))
  (.setInteger nbt "plateCount" @(:plate-count tile))
  (.setInteger nbt "subId" (:sub-id tile))
  (.setString nbt "direction" (name (:direction tile)))
  (inv/write-inventory-to-nbt tile nbt)
  nbt)

(defn read-matrix-from-nbt [tile nbt]
  (when (.hasKey nbt "placer")
    (assoc tile :placer-name (.getString nbt "placer")))
  (when (.hasKey nbt "plateCount")
    (reset! (:plate-count tile) (.getInteger nbt "plateCount")))
  (when (.hasKey nbt "subId")
    (assoc tile :sub-id (.getInteger nbt "subId")))
  (when (.hasKey nbt "direction")
    (assoc tile :direction (keyword (.getString nbt "direction"))))
  (inv/read-inventory-from-nbt tile nbt)
  tile)
```

### 重构后（NBT DSL v1 - 使用 :custom-write - 28 行）

```clojure
(nbt/defnbt node
  [:energy "energy" :double
   :custom-write (fn [tile nbt key _]
                   (.setDouble nbt key (winterfaces/get-energy tile)))
   :custom-read (fn [tile nbt key]
                  (when (.hasKey nbt key)
                    (winterfaces/set-energy tile (.getDouble nbt key))))]
  [:node-name "nodeName" :string
   :custom-write (fn [tile nbt key _]
                   (.setString nbt key (winterfaces/get-node-name tile)))
   :custom-read (fn [tile nbt key]
                  (when (.hasKey nbt key)
                    (set-node-name! tile (.getString nbt key))))]
  [:placer-name "placer" :string]
  [:inventory "inventory" :inventory])
```

### 最终版（NBT DSL v2 - 使用 :getter/:setter - 12 行）

```clojure
(nbt/defnbt node
  [:energy "energy" :double
   :getter winterfaces/get-energy
   :setter winterfaces/set-energy]
  [:node-name "nodeName" :string
   :getter winterfaces/get-node-name
   :setter set-node-name!]
  [:placer-name "placer" :string]
  [:inventory "inventory" :inventory])
```

**代码减少对比**:
- 手动 → DSL v1: **-30%** (40行 → 28行)
- 手动 → DSL v2: **-70%** (40行 → 12行) ✨
- DSL v1 → DSL v2: **-57%** (28行 → 12行) 🚀

## 内部实现

### 宏展开示例

```clojure
;; 输入
(nbt/defnbt player
  [:name "playerName" :string]
  [:level "level" :int])

;; 展开后
(do
  (defn write-player-to-nbt
    "Save player to NBT
    
    Auto-generated by defnbt macro.
    
    Saves:
    - name
    - level"
    [tile nbt]
    (write-nbt-fields tile nbt field-specs)
    nbt)
  
  (defn read-player-from-nbt
    "Load player from NBT
    
    Auto-generated by defnbt macro.
    
    Restores:
    - name
    - level"
    [tile nbt]
    (read-nbt-fields tile nbt field-specs)
    tile)
  
  (log/info "Registered NBT serialization for: player"))
```

### 类型转换器结构

```clojure
{:double
 {:write (fn [nbt key value] (.setDouble nbt key value))
  :read (fn [nbt key] (.getDouble nbt key))
  :has-key? (fn [nbt key] (.hasKey nbt key))}
 
 :keyword
 {:write (fn [nbt key value] (.setString nbt key (name value)))
  :read (fn [nbt key] (keyword (.getString nbt key)))
  :has-key? (fn [nbt key] (.hasKey nbt key))}}
```

## 最佳实践

### 1. 使用有意义的 NBT 键名

```clojure
;; ✅ 好
[:energy "energy" :double]

;; ❌ 不好
[:energy "e" :double]
```

### 2. Atom 字段标记 :atom? true

```clojure
;; ✅ 正确
[:update-ticker "ticker" :int :atom? true]

;; ❌ 错误（会尝试序列化 atom 对象本身）
[:update-ticker "ticker" :int]
```

### 3. 使用 :getter/:setter 而非 :custom-write/:custom-read

```clojure
;; ✅ 简洁清晰
[:energy "energy" :double
 :getter winterfaces/get-energy
 :setter winterfaces/set-energy]

;; ❌ 过度复杂
[:energy "energy" :double
 :custom-write (fn [tile nbt key _]
                 (.setDouble nbt key (winterfaces/get-energy tile)))
 :custom-read (fn [tile nbt key]
                (when (.hasKey nbt key)
                  (winterfaces/set-energy tile (.getDouble nbt key))))]
```

**规则**：只有需要完全自定义逻辑时才用 :custom-*

### 4. Keyword 使用 :keyword 类型

```clojure
;; ✅ 自动转换
[:direction "direction" :keyword]

;; ❌ 手动转换
[:direction "direction" :string
 :transform-write name
 :transform-read keyword]
```

### 5. 复杂逻辑使用 :custom-write/:custom-read

```clojure
;; ✅ 清晰明确
[:energy "energy" :double
 :custom-write (fn [tile nbt key _]
                 (.setDouble nbt key (get-energy-from-protocol tile)))]

;; ❌ 不要尝试在 :transform-write 中做太多事情
```

### 5. 复杂逻辑使用 :custom-write/:custom-read

```clojure
;; ✅ 清晰明确（需要额外计算时）
[:energy "energy" :double
 :custom-write (fn [tile nbt key _]
                 ;; 写入时乘以1000转换为整数
                 (.setInteger nbt key (int (* 1000 (get-energy-from-protocol tile)))))]

;; ❌ 简单情况不要用 custom
[:energy "energy" :double
 :custom-write (fn [tile nbt key _]
                 (.setDouble nbt key (get-energy-from-protocol tile)))]

;; ✅ 这种情况用 :getter
[:energy "energy" :double
 :getter get-energy-from-protocol]
```

### 6. Inventory 直接使用 :inventory 类型

```clojure
;; ✅ 简单
[:inventory "inventory" :inventory]

;; ❌ 不需要自定义
[:inventory "inventory" :custom
 :custom-write (fn [tile nbt key _] ...)]
```

## 扩展 DSL

### 添加新类型转换器

```clojure
(swap! nbt/type-converters assoc :uuid
  {:write (fn [nbt key value]
            (.setString nbt key (str value)))
   :read (fn [nbt key]
           (java.util.UUID/fromString (.getString nbt key)))
   :has-key? (fn [nbt key]
               (.hasKey nbt key))})

;; 使用新类型
(nbt/defnbt entity
  [:id "entityId" :uuid])
```

### 创建辅助宏

```clojure
(defmacro defnbt-with-timestamp
  [name & fields]
  `(nbt/defnbt ~name
     [:last-modified "lastModified" :long
      :custom-write (fn [tile# nbt# key# _#]
                      (.setLong nbt# key# (System/currentTimeMillis)))]
     ~@fields))
```

## 常见问题

### Q: 如何处理嵌套 NBT？

A: 使用 :custom-write 和 :custom-read：

```clojure
[:nested-data "nestedData" :custom
 :custom-write (fn [tile nbt key _]
                 (let [nested (NBTTagCompound.)]
                   ;; 写入嵌套数据
                   (.setTag nbt key nested)))
 :custom-read (fn [tile nbt key]
                (when (.hasKey nbt key)
                  (let [nested (.getCompoundTag nbt key)]
                    ;; 读取嵌套数据
                    )))]
```

### Q: 如何新增或调整持久化字段？

A: 在权威 `defnbt` 定义中修改字段，并同步更新调用方、测试和存档风险说明。不要保留第二套读写函数。

### Q: 性能如何？

A: 与手动实现相同：

- 宏在编译时展开，无运行时开销
- 类型转换器使用直接 Java 互操作
- 没有额外的抽象层

### Q: 支持条件字段吗？

A: 使用 :skip-on-write?：

```clojure
[:debug-info "debugInfo" :string
 :skip-on-write? (fn [tile] (not (:debug-mode tile)))]
```

## 总结

NBT DSL 提供了一个优雅、类型安全、可扩展的 NBT 序列化解决方案：

- ✅ **简洁**: 减少 80%+ 样板代码
- ✅ **安全**: 编译时类型检查
- ✅ **灵活**: 支持自定义逻辑
- ✅ **清晰**: 声明式语法，易读易维护
- ✅ **完整**: 自动生成文档和日志

使用 NBT DSL，让数据持久化变得简单而优雅！

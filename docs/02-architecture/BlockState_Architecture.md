# BlockState 架构：属性定义与 DataProvider

## 概述

BlockState 采用**定义与实现分离**：属性与结构在 **`mcmod`（通用定义）** 与 **`ac`（具体方块数据）** 中声明；Forge 运行时与 DataProvider 使用这些定义，新增属性以 Clojure 为主，避免在 Java 中硬编码属性表。

---

## 问题与目标

- **原问题**：属性曾硬编码在 Java（如专用 DynamicBlock），新增属性需改 Java，平台与业务耦合。
- **目标**：属性与 BlockState 结构在 Clojure 元数据中声明；Forge 仅做注入与 JSON 生成。

---

## 当前方案

### 1. 业务与内容（ac + mcmod）

**属性数据（示例：无线节点）**：放在内容层，例如  
`ac/src/main/clojure/cn/li/ac/block/wireless_node/blockstate.clj`（或与该方块同域的 schema/block 文件）。

```clojure
(def block-state-properties
  {:energy   {:name "energy"   :type :integer :min 0 :max 4 :default 0}
   :connected {:name "connected" :type :boolean :default false}})
```

在 block DSL 中通过 `(bdsl/defblock ... :block-state-properties block-state-properties ...)` 等声明。

机器方块优先从 **`cn.li.ac.block.machine.runtime/schema-runtime`** 导出 `:block-state-properties` 与 `:blockstate-updater`，在 `make-tick-fn` 与 `commit-state!` 提交时投影，避免在 `logic.clj` 重复手写 updater。

### 机器 runtime 回调签名（扁平参数）

`make-tick-fn` / `commit-*` / GUI 打开 handler 使用位置参数，不再传递 per-tick ctx map：

| 键 | 签名 |
|----|------|
| `:tick-state` | `(fn [state level pos block-state be] → state')` |
| `:initial-state` | `(fn [be level pos block-state] → state)` |
| `:after-commit!` | `(fn [be level pos old-state new-state] → nil)` |
| `:can-open?` | `(fn [player world pos sneaking item-stack] → bool)` |
| `:resolve-open-pos` | `(fn [player world pos] → pos)` |
| `:server-before-open!` | `(fn [player world open-pos] → truthy)` |

方块事件（经 `mcmod/events/dispatcher` 解构后）：`:on-right-click` `[player world pos block-id & opts]`、`:on-place` `[player world pos block-id]`、`:on-break` `[world pos block-id]`。

**BlockState 结构定义（通用）**：`ac/.../block/blockstate_definition.clj`（查询聚合）+ `ac/.../block/blockstate_datagen.clj`（机器 multipart）

- `BlockStatePart`、`BlockStateDefinition` 等数据结构在 `blockstate-definition`。
- **机器方块**（ability-interferer、imag-fusor、metal-former）：multipart `properties` / `parts` 由 **`blockstate-datagen`** 从各 `schema` 的 `:block-state` 字段自动生成（与 `schema-runtime` 同源）；纹理与模型命名 helper 亦集中于此。
- **无线节点**：`wireless-node/blockstate.clj` 从 node schema 生成（同上模式）。
- 查询接口：`get-block-state-definition`、`get-all-definitions`、`is-multipart-block?`、`get-model-texture-config` 等。

### 2. 属性生成与注册（mcmod）

**位置**：`mcmod/src/main/clojure/cn/li/mcmod/block/blockstate_properties.clj`

- 根据 block 的 `block-state-properties` 生成 Minecraft `Property` 对象。
- 存入 registry，供运行时与 DataProvider 共用：`get-property`、`get-all-properties` 等。

### 3. 平台集成（Forge）

**运行时**：在 `cn.li.forge1201.mod` 等注册逻辑中，对需要动态属性的方块：

1. 通过 `bsp/get-all-properties`（或当前 API）按 block id 取 `Property`。
2. 使用 Forge 侧动态方块类注入属性。
3. 不硬编码属性名或取值范围。

**Data 生成**：`platform-src/minecraft/mc-1.20.1/src/main/clojure/cn/li/mc1201/datagen/blockstate_provider_shell.clj` 提供共享创建壳；Forge/Fabric 分别通过 `datagen/provider_factory.clj` 接入 Loader Provider API。

- 依赖 `cn.li.mcmod.block.blockstate-definition` 等，生成 `assets/<mod_id>/blockstates/*.json`。

---

## 定义层与实现层关系

```
mcmod/block/
  blockstate_definition.clj   ← BlockState 结构、parts、model 映射
  blockstate_properties.clj   ← Property 对象生成与 registry
ac/block/
  blockstate_datagen.clj        ← 机器 schema → multipart datagen 定义
  wireless_node/blockstate.clj  ← 节点 schema → multipart datagen 定义
  <machine>/schema.clj          ← :block-state 字段（runtime + datagen 同源）
        ↑
        │ 查询
platform-src/minecraft/mc-1.20.1/.../datagen/
  blockstate_provider_shell.clj ← 共享 blockstates/*.json provider 创建壳
platform-src/loader/forge/.../datagen/
  provider_factory.clj          ← Forge Provider API 适配
  setup.clj / event_handler.clj
platform-src/loader/fabric/.../datagen/
  provider_factory.clj          ← Fabric Provider API 适配
  setup.clj
```

---

## 添加新属性（仅 Clojure）

示例：为某方块增加 `powered` 属性。

1. 在该方块的 `block-state-properties` 中增加：
   ```clojure
   :powered {:name "powered" :type :boolean :default false}
   ```
2. 若需对应 model 变体，在 `blockstate_definition` 中为该方块的 parts 增加条件与 model。
3. 尽量不改 Java；DataProvider 从定义层读取并生成 JSON。

---

## 优势

- 单一真实来源：属性与结构在 Clojure 元数据中。
- 平台无游戏名硬编码：通过 metadata/registry 发现。
- Forge 侧 DataProvider 与运行时共用同一套属性定义。

---

## 参考文件（命名空间）

- 定义与属性：`cn.li.mcmod.block.blockstate-definition`、`cn.li.mcmod.block.blockstate-properties`
- 内容示例：`cn.li.ac.block.wireless-node.blockstate`（及同目录 block 定义）
- Forge 注册：`cn.li.forge1201.mod`
- 共享 DataProvider shell：`cn.li.mc1201.datagen.blockstate-provider-shell`
- 平台 Factory 适配：`cn.li.forge1201.datagen.provider-factory`、`cn.li.fabric1201.datagen.provider-factory`

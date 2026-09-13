# 无线系统接入指南（新方块 / 第三方 mod）

> 状态标签：**现行**（2026-07 重构后）

## 一、本 mod 内新方块接入（generator / receiver / node / matrix）

三步：

### 1. tile spec 声明 capability key

在方块的 `init-machine!` / tile DSL spec 中声明 `:capability-keys`，取值来自
`WirelessCapabilityKeys`（keyword 形式）：

```clojure
:capability-keys #{:wireless-generator}   ; 或 :wireless-receiver / :wireless-node / :wireless-matrix
```

capability 解析链：`capability-lookup/tile-capability` → tile spec 的
`:capability-keys` 判定 → `mcmod.capability.registry/get-handler-factory` 取
factory → `(factory tile nil)` 返回 `IWireless*` 实例。

### 2. 提供 capability factory

复用 `cn.li.ac.block.role-impls` 的通用 factory（`wireless-generator-factory`
/ `wireless-receiver-factory`，基于 state map 的 `:energy` 字段），或参照
`cn.li.ac.block.energy-converter.wireless-impl` 用 `reify` 实现
`IWirelessGenerator` / `IWirelessReceiver`（`get-state-fn`/`set-state-fn` 注入，
支持自定义带宽与容量）。factory 经 `mcmod.capability.registry` 注册表按 key 注册。

### 3. 经 api 挂接与清理

- 挂接：`wireless-api/link-generator-to-node!` / `link-receiver-to-node!`
  （password + need-auth）；节点发现用 `wireless-api/get-nodes-in-range`。
- 破坏清理：generator/receiver 无需显式清理（连接校验按冷却移除 stale 设备），
  但主动破坏时调用 `unlink-generator-from-node!` / `unlink-receiver-from-node!`
  可立即移除；node 方块用 `cleanup-node-at!`，matrix 用 `destroy-network-at!`。

### 扩展点清单（新增节点类型 / 角色时要动的位置）

| 需求 | 位置 |
|------|------|
| 新节点档位（容量/带宽/距离） | `wireless.config/descriptors`（`node.<tier>.*`）+ `node-types` |
| 新 `block-type` 角色 | `foundation.vblock` 与 `core.vblock` 的类型注释、`core.vblock-resolver/vblock-get` 的 case、`core.capability-resolver/resolve-capability` 的 case |
| 新拓扑操作 | `domain.topology` 纯变换 + `data.store` 原语 + `service.commands` 准入 + `api` 暴露 |
| 新配置项 | `wireless.config/descriptors` |
| NBT 字段 | `data.persistence`（必要时提升 `schema-version` 并声明版本边界） |

## 二、第三方 mod 接入

### 观察拓扑事件（IMC，Forge）

在 `InterModEnqueueEvent` 期间发送 IMC 消息（常量见
`cn.li.acapi.wireless.WirelessImc`）：

```java
InterModComms.sendTo(WirelessImc.MOD_ID, WirelessImc.REGISTER_NETWORK_HANDLER,
    () -> (java.util.function.Consumer<java.util.Map<?, ?>>) event -> {
        // event map 键为 Clojure keyword，契约见 WirelessImc javadoc
    });
```

- `register_wireless_network_handler`：`:kind :topology/network`，`:action`
  ∈ `:created`/`:destroyed`，携带 `:ssid`、`:matrix`（`IWirelessMatrix`）。
- `register_wireless_node_handler`：`:kind :topology/node`，`:action` ∈
  `:connected`/`:disconnected`/`:generator-linked`/`:generator-unlinked`/
  `:receiver-linked`/`:receiver-unlinked`，携带 `:node` 及相应 `:matrix`/
  `:generator`/`:receiver` capability。
- Handler 在 server 线程回调；抛异常会被记 WARN 并移除。

### 能量桥接（推荐路径）

第三方能量系统（FE 等）通过 **energy converter** 方块与无线网络互通：converter
同时实现平台能量 capability 与 `IWirelessGenerator`/`IWirelessReceiver`（见
`cn.li.ac.block.energy-converter.wireless-impl`），玩家把 converter 挂到节点即可。
第三方 mod 无需直接触碰无线内部状态。

### 尚未提供

设备级第三方注册（外部 mod 直接把自己的方块注册为 wireless 设备类型）需要
Java-facing registrar 桥接 `mcmod.capability.registry` 注册表——属新特性，
单独立项；当前第三方路径 = IMC 观察事件 + energy converter 能量桥。

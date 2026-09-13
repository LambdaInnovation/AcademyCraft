# 无线系统维护手册

> 状态标签：**现行**（2026-07 全量重构后）

无线系统负责：矩阵网络（SSID/密码/容量/距离）、无线节点入网、NodeConn（发电机/接收器挂接）、网络内能量均衡、节点连接传输、世界持久化与 GUI 查询。

**当前实现为单一函数式运行时**：位置键 map 存储 + 纯计划（`domain.transfer` / `domain.topology`）+ 单一提交点（`data.store`）+ 窄副作用边界（`runtime.effects` + capability）。

## 存储模型（2026-07 重构核心）

每世界拓扑状态存于 Framework atom `[:service :wireless-worlds <world-key> :world-state]`：

```clojure
{:networks       {}    ; {matrix-pos -> WirelessNet}   pos = [x y z]
 :connections    {}    ; {node-pos -> NodeConn}
 :net-by-ssid    {}    ; {ssid -> matrix-pos}
 :node-to-net    {}    ; {node-pos -> matrix-pos}
 :device-to-node {}    ; {generator/receiver-pos -> node-pos}
 :spatial-index  {}}   ; {chunk-key -> #{[x y z]}} 仅已放置的 node 方块
```

- **实体身份 = 不可变世界位置**（matrix-pos / node-pos，`vb/pos-of`）。实体提交 = O(1) `assoc`（`entity-commit/commit-network!`，仅在已注册时生效）。
- **lookup 表只存位置引用**，实体值只有一个家（`:networks`/`:connections`）——不存在引用失同步。
- **空间索引只服务节点发现**（`find-available-nodes-at`）：放置时加入、破坏时移除；残留条目由 capability 解析自然过滤。`range-search-networks` 是 O(n) 全扫（n<100）且过滤 disposed。
- **瞬态状态**（stale 设备时间戳）存 `[:service :wireless-transient <world-key>]`：不落盘、不触发 SavedData dirty，随世界卸载/会话清理一并清除。

## Tick 调度

每 tick `world-runtime/tick-world-data!` 构造一次 ctx：`{:game-time :cfg :cap-cache}`（cfg 为配置快照；cap-cache 为 tick 级 `java.util.HashMap` capability 缓存，单线程使用、tick 末即弃）。

- **零状态错峰**：`scheduling/due?` = `gameTime + hash(pos)` 取模 interval。无 per-entity 计数器、无提交、无持久化字段。
- 网络：非到期 tick 只做一次 `active?` 检查；到期 tick 先 `network-validation/validate!` 再 `network-energy-balance/balance-energy!`（比例填充 + buffer + 矩阵带宽预算；空闲网络零提交零 setDirty）。间隔 `:network-update-interval-ticks`（默认 40）。
- 连接：传输每 tick（`runtime/node-transfer`，节点带宽预算，`transfer/rotated` 轮转公平）；完整性校验按 `:validate-interval-ticks`（默认 20）错峰。stale 设备按 `:stale-device-cooldown-ticks` 的 gameTime 时间差移除。
- 周期清扫：`:sweep-interval-ticks`（默认 600）把 disposed 实体真正注销（不再只等存档）。
- `setDirty` 只在真实数据变更（拓扑变更、均衡写 buffer/能量）时发生。

## 模块边界

分层方向：`api → service → {data, domain} → core → foundation`；`data` 可用 `domain` 纯变换与 `runtime.effects`，**禁止** `data → service`。

| 命名空间 | 职责 |
|----------|------|
| `wireless.api` | **唯一**对外入口：查询、拓扑命令、拓扑事件、按位置清理（`destroy-network-at!`/`cleanup-node-at!`） |
| `service.commands` | 准入规则（ssid 唯一、密码、容量、距离）+ 委托 `data.store` |
| `service.queries` | 只读查询（tile → vblock → lookup；节点发现走空间索引） |
| `domain.topology` | world-state map 上的纯注册/注销/链接变换 |
| `domain.transfer` | `balance-plan`、传输步骤纯计划、`rotated` |
| `domain.model` | 容量等纯校验 |
| `data.store` | **唯一原语提交点**：每个操作一次原子 swap + 日志 |
| `data.world-registry` | Framework 路径、world-state/transient 读写 |
| `data.entity-commit` | O(1) 实体 commit/resolve（按位置） |
| `data.network-lookup` | 按 matrix/node/ssid/device 位置查询 |
| `data.spatial-lookup` | 空间索引读写（位置元组） |
| `data.network-state` / `data.node-conn` | 实体 record、纯 accessor、设备管理、瞬态 stale 助手 |
| `data.network-validation` / `data.node-conn-validation` | capability 校验与 dispose |
| `data.network-runtime` / `data.world-runtime` | tick 编排（ctx、due?、清扫） |
| `data.network-energy-balance` | 均衡编排（计划 + effects） |
| `data.persistence` | 当前 NBT schema（列表序列化，载入后重建各 map） |
| `runtime.node-transfer` | 连接 tick 入口 + generator/receiver 传输循环 |
| `runtime.effects` | `.setEnergy` / generator pull / receiver inject |
| `core.vblock`(+resolver/codec 拆分) | 位置引用；`pos-of` 为所有 map key 来源 |
| `core.capability-lookup/-resolver` | capability 解析（含 tick 级缓存 `resolve-cap-cached`） |
| `core.scheduling` | `due?` 错峰 |
| `core.spatial-index` | chunk 桶（`bit-shift-right 4`，与 `nearby-chunk-keys` 同源） |
| `config` | descriptors、typed getter、`cfg` 快照 |
| `gui.message.bootstrap` | 无线 GUI 消息统一注册入口 |

### 已删除（勿再引用）

`data.network-mutation`、`core.interfaces`、`shared.message-registry`（移至 `gui.message.bootstrap`）、`entity-commit` 的 `replace-*-in-state!`/`with-committed-*`/`network-in-world?`、`network-state` 的 update-counter 系列、`node-conn` 的 `:stale-counters`/`print-conn-info`、`WirelessNetworkEvent.java`、`vblock.clj` 的 NBT 门面。

## 生命周期

- **matrix 破坏** → `handle-matrix-break` → `api/destroy-network-at!`（按位置，立即注销 + 事件）。
- **node 破坏** → `api/cleanup-node-at!`（按位置：空间索引 → 网络 unlink → 连接销毁；不依赖 BlockEntity 存活）。
- **设备失效**（chunk 加载但 capability 消失）→ 连接校验按冷却移除；设备恢复则时间戳清零。
- **disposed** 标记仅作为 chunk 未加载时的宽限；周期清扫与存档前校验会真正注销。

## 调用约定（新增代码）

```text
改拓扑 / 入网 / 挂接           → wireless.api（或 service.commands）
只读查询                       → wireless.api 或 service.queries
数据层内部需要提交原语          → data.store（勿 require service）
改实体字段                     → network-state / node-conn 纯函数 + entity-commit
capability 充放电              → runtime.effects
tick 内 capability 解析        → resolver/resolve-cap-cached + ctx 的 :cap-cache
新周期性工作                   → scheduling/due? + ctx，勿加计数器字段
```

## 状态一致性规则

1. 多索引更新在一次 `world-registry/update-state!` swap 内完成（store 原语已如此）。
2. map key 一律位置元组（`vb/pos-of`）；禁止以 vblock record 或 ssid 作实体 map key。
3. GUI 使用 `api/network-snapshot` 或查询结果，不持有可变实体引用；加载后实体为新实例，按位置/ssid 重新 lookup。
4. 瞬态数据（不需要持久化、不应 setDirty 的）走 `world-registry/update-transient-value!`。

## 校验命令

```powershell
cmd /c .\gradlew.bat :ac:compileClojure
cmd /c .\gradlew.bat runAcUnitTests "-Dac.test.only=cn.li.ac.wireless.data.network-test,cn.li.ac.wireless.data.node-conn-test,cn.li.ac.wireless.data.world-test,cn.li.ac.wireless.service.commands-test,cn.li.ac.wireless.service.queries-test,cn.li.ac.contract.energy-wireless-contract-test"
cmd /c .\gradlew.bat verifyArchitectureBoundaries
```

## 相关文档

- 三方与新方块接入：[WIRELESS_INTEGRATION.md](WIRELESS_INTEGRATION.md)

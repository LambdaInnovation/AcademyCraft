# Wireless Runtime Contracts

本文档描述当前无线能源运行时的唯一架构。

## Architecture

```text
block / GUI / terminal / energy.operations
        │
        ▼
cn.li.ac.wireless.api
        │
        ├── service.queries
        └── service.commands
                │
                ├── domain.topology
                ├── domain.transfer
                └── domain.model

runtime / persistence
        │
        ├── data.world-registry
        ├── data.entity-commit
        ├── data.network-state
        ├── data.node-conn
        ├── data.network-runtime
        ├── data.world-runtime
        ├── data.world
        ├── data.persistence
        └── runtime.effects
```

## Public API

外部调用方只使用 `cn.li.ac.wireless.api`：

- `create-network!` / `destroy-network!`
- `link-node-to-network!` / `unlink-node-from-network!`
- `link-generator-to-node!` / `unlink-generator-from-node!`
- `link-receiver-to-node!` / `unlink-receiver-from-node!`
- `get-wireless-net-by-*` / `get-node-conn-by-*`
- `register-node-spatial!` / `unregister-node-spatial!`
- `network-snapshot`

Block、GUI、Terminal、Energy 模块不得直接 patch world-state 索引。

## State contract

- 每个世界一份 `WiWorldData`，由 `data.world-registry` 管理。
- 状态 map 包含 `:net-lookup`、`:node-lookup`、`:spatial-index`、`:networks`、`:connections`。
- 多索引变更必须通过 `world-registry/transact!` 或封装它的 service command。
- `WirelessNet` / `NodeConn` 是不可变 record；变更产生新值并经 `entity-commit` 写回。

## Side-effect boundary

| Layer | Allowed | Not allowed |
|-------|---------|-------------|
| `domain.*` | Pure transformations | world mutation, capability IO |
| `service.commands` | transaction orchestration | direct energy IO |
| `runtime.effects` | capability energy IO | topology mutation |
| `network-runtime` | tick orchestration | hand-written index patch |

Energy balance flow:

```text
network-runtime/tick
  -> network-energy-balance/balance-energy!
  -> domain.transfer/balance-plan
  -> runtime.effects/apply-node-energy-plan!
```

## Persistence

- Authoritative codec: `cn.li.ac.wireless.data.persistence`
- Save: serialize active networks and connections.
- Load: create `WiWorldData`, then rebuild indexes through service commands.
- `data.world` owns lifecycle hooks only.

## Related docs

- [../04-systems/WIRELESS_SYSTEM_MAINTENANCE.md](../04-systems/WIRELESS_SYSTEM_MAINTENANCE.md)
- [../02-architecture/AC_MODULE_LAYERING.md](../02-architecture/AC_MODULE_LAYERING.md)

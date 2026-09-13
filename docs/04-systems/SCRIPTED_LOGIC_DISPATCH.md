# Scripted Logic Dispatch

Scripted block and mob logic is compiled into explicit Java interface bundles in the Minecraft 1.20.1 version component.

## Ownership

| Layer | Location | Responsibility |
|-------|----------|----------------|
| Declaration | `mcmod` DSL | Tile/entity metadata and script intent. |
| Business | `ac` | Script implementations and content declarations. |
| Minecraft API | `platform-src/minecraft/mc-1.20.1/` | Java interfaces, bundle compile/install pipeline, hot-path dispatch. |
| Loader | `platform-src/loader/<loader>/` | Entity/block registration lifecycle glue. |

## Tile contracts

| Interface | Method | Empty behavior |
|-----------|--------|----------------|
| `ITileTickLogic` | `serverTick(level, pos, state, be)` | skip tick |
| `ITileNbtLogic` | `readNbt` / `writeNbt` | skip NBT hook |
| `ITileContainerLogic` | WorldlyContainer-aligned methods | safe defaults |
| `ITileCapabilityLogic` | `resolve(be, capKey, side)` | no capability |

`TileLogicBundle` is the block entity logic carrier.

## Mob contracts

| Interface | Method | Empty behavior |
|-----------|--------|----------------|
| `IMobTickLogic` | `aiStep(mob)` | no-op |
| `IMobHurtLogic` | `onIncomingDamage` | vanilla damage |
| `IMobDeathLogic` | `onDie` | vanilla death flow |
| `IMobLootLogic` | `dropLoot` | vanilla loot |

`ScriptedEntityLogicRegistry` is the mob bundle anchor for Minecraft runtime dispatch.

## Pipeline

```text
content init
  -> compile tile/entity bundles
  -> install bundles during block/entity registration
  -> runtime Java interface dispatch
```

## Boundaries

| Layer | Minecraft types | Loader API |
|-------|-----------------|------------|
| `mcmod` | no | no |
| `ac` | no | no |
| `platform-src/minecraft/mc-1.20.1` | yes | no |
| `platform-src/loader/<loader>` | yes | yes |

See also [PROJECT_LAYOUT.md](../01-overview/PROJECT_LAYOUT.md) and [TILE_DSL_GUIDE_CN.md](../03-dsl/TILE_DSL_GUIDE_CN.md).

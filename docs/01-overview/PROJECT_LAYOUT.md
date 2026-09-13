# 项目布局

当前项目使用核心工程 + 单一目标化平台工程的布局：

```text
api/
mcmod/
ac/

platform-src/
  minecraft/
    base/                 # catalog: minecraft-base (cn.li.mcbase)
    mc-1.20.1/            # catalog: minecraft-1.20.1 (cn.li.mc1201 + cn.li.mcver)
    mc-1.21.1/            # catalog: minecraft-1.21.1 (cn.li.mc1211 + cn.li.mcver)
    mc-26.2/              # catalog: minecraft-26.2 (cn.li.mc262 + cn.li.mcver)
  loader/
    forge-1.20.1/         # catalog: forge-1.20.1 (cn.li.forge1201)
    fabric-1.20.1/        # catalog: fabric-1.20.1 (cn.li.fabric1201)
    neoforge-shared/      # catalog: neoforge-shared (cn.li.neoforgebase)
    neoforge-1.21.1/      # catalog: neoforge-1.21.1 (cn.li.neoforge1211)
    neoforge-26.2/        # catalog: neoforge-26.2 (cn.li.neoforge262)
  test-support/

platform/
platform-catalog.json
build-logic/
platform-builds/
  mdg-gradle-9.2/         # isolated Gradle 9.2 wrapper for MDG targets
```

## Gradle 工程

| 工程 | 职责 |
|------|------|
| `:api` | 对外 Java API 与互操作接口 |
| `:mcmod` | DSL、协议、生命周期、平台抽象和不依赖 Minecraft 类的运行契约 |
| `:ac` | AcademyCraft 内容与领域逻辑 |
| `:platform` | 唯一平台工程；通过 `scripts/target-gradle.ps1 <target-id>` 选择具体目标 |

## 平台源码组件

| 目录 | 职责 |
|------|------|
| `platform-src/minecraft/base/` | 跨版本共享 Minecraft glue（`cn.li.mcbase`）；不含 Loader，也不引用 `mc1201`/`mc1211`/`mc262` |
| `platform-src/minecraft/mc-1.20.1/` | Minecraft 1.20.1 运行时适配 + `cn.li.mcver` 版本缝（相对 26.2 的降级实现） |
| `platform-src/minecraft/mc-1.21.1/` | Minecraft 1.21.1 运行时适配 + `cn.li.mcver` 版本缝（相对 26.2 的降级实现） |
| `platform-src/minecraft/mc-26.2/` | Minecraft 26.2 运行时适配 + `cn.li.mcver` 版本缝（契约塑形端） |
| `platform-src/loader/forge-1.20.1/` | Forge lifecycle、entrypoint、`mods.toml`、注册、client/datagen glue |
| `platform-src/loader/fabric-1.20.1/` | Fabric lifecycle、entrypoint、`fabric.mod.json`、client/datagen glue |
| `platform-src/loader/neoforge-shared/` | NeoForge 跨版本共享 glue（`cn.li.neoforgebase`）；禁止引用版本命名空间 |
| `platform-src/loader/neoforge-1.21.1/` | NeoForge 1.21.1 lifecycle、entrypoint、注册、client/datagen glue |
| `platform-src/loader/neoforge-26.2/` | NeoForge 26.2 lifecycle、entrypoint、注册、client/datagen glue |
| `platform-src/test-support/` | 平台目标测试辅助代码 |

版本缝说明见 [MC_VERSION_SEAM.md](../dev/MC_VERSION_SEAM.md)。`minecraft-base` 上提状态见 [MCBASE_PROMOTION_STATUS.md](../dev/MCBASE_PROMOTION_STATUS.md)。

## 目标声明

`platform-catalog.json` 是唯一目标目录。每个 target 显式声明 loader、Minecraft version、Java version、source components、test components、capabilities、dependencies、artifact 信息与 datagen parity group。

构建逻辑不得从 target id 字符串推导行为，也不得自动生成 Loader × Minecraft 版本的笛卡尔组合。

构建剖面（`buildProfiles`）按 `toolchain` 分组：

- `loom` — Architectury Loom + Gradle 8.8（`forge-1.20.1` / `fabric-1.20.1` / `neoforge-1.21.1`）
- `mdg` — ModDevGradle + Gradle 9.2 + Java 25（`neoforge-26.2`），隔离 wrapper 在 `platform-builds/gradle-9.2/`

当前生产目标（摘要）：

| target id | Loader | Minecraft | source components |
|-----------|--------|-----------|-------------------|
| `forge-1.20.1` | Forge | 1.20.1 | `minecraft-base`, `minecraft-1.20.1`, `forge-1.20.1` |
| `fabric-1.20.1` | Fabric | 1.20.1 | `minecraft-base`, `minecraft-1.20.1`, `fabric-1.20.1` |
| `neoforge-1.21.1` | NeoForge | 1.21.1 | `minecraft-base`, `minecraft-1.21.1`, `neoforge-shared`, `neoforge-1.21.1` |
| `neoforge-26.2` | NeoForge | 26.2 | `minecraft-base`, `minecraft-26.2`, `neoforge-shared`, `neoforge-26.2` |

## 依赖边界

- `mcmod` 与 `ac` 不引用 `net.minecraft.*`、Forge、Fabric 或 NeoForge API
- `platform-src/minecraft/*` 可以引用 Minecraft API，但不能枚举 Loader
- `platform-src/loader/*` 只承载对应 Loader 的生命周期、注册和入口 glue
- `neoforge-shared` 不得引用 `cn.li.mc1211` / `cn.li.mc262` / `cn.li.neoforge1211` / `cn.li.neoforge262`
- Loader Java entrypoint、client/datagen entrypoint、metadata 是外部框架要求，允许保留；内部转发 namespace、单调用封装和双轨实现不保留
- 不存在根目录 `forge-1.20.1/` / `fabric-1.20.1/` 等多模块平台工程；平台只通过 `:platform` + catalog 组装

## 常用命令

```powershell
.\gradlew.bat verifyCurrentPlatforms
.\scripts\target-gradle.ps1 forge-1.20.1
.\scripts\target-gradle.ps1 fabric-1.20.1
.\scripts\target-gradle.ps1 neoforge-1.21.1
.\scripts\target-gradle.ps1 neoforge-26.2
```

# 文档索引

本目录只保留当前项目的维护文档。文档描述的是现行架构：核心工程 `api` / `mcmod` / `ac`，以及单一 Gradle 平台工程 `:platform`；具体 Loader 与 Minecraft 版本由 `platform-catalog.json` 选择，并通过 `scripts/target-gradle.ps1`、`scripts/target-gradle.cmd` 或 `scripts/target-gradle.sh` 统一构建。

## 当前构建

- 根工程：`api`、`mcmod`、`ac`、`:platform`。
- 平台目标：`forge-1.20.1`、`fabric-1.20.1`、`neoforge-1.21.1`，均在 `platform-catalog.json` 显式声明。
- 源码组件：
  - 跨版本公用层：`platform-src/minecraft/base/`（`cn.li.mcbase.*`）
  - Minecraft 版本层：`platform-src/minecraft/mc-1.20.1/`（`cn.li.mc1201.*` + `cn.li.mcver.*`）、`platform-src/minecraft/mc-1.21.1/`（`cn.li.mc1211.*` + `cn.li.mcver.*`）
  - Loader 层：`platform-src/loader/forge-1.20.1/`、`platform-src/loader/fabric-1.20.1/`、`platform-src/loader/neoforge-1.21.1/`
- 构建输出：`build/targets/<target-id>/`（以及平台工程下的 run/generated 目录）。

## 目录

| 目录 | 用途 | 入口文档 |
|------|------|----------|
| `01-overview` | 项目总览、布局、上手命令 | [GETTING_STARTED.md](01-overview/GETTING_STARTED.md)、[PROJECT_LAYOUT.md](01-overview/PROJECT_LAYOUT.md)、[Project_Summary_CN.md](01-overview/Project_Summary_CN.md) |
| `02-architecture` | 架构、边界、平台目标模型 | [Runtime_And_DSL_CN.md](02-architecture/Runtime_And_DSL_CN.md)、[AC_MODULE_LAYERING.md](02-architecture/AC_MODULE_LAYERING.md)、[Platform_And_Fabric.md](02-architecture/Platform_And_Fabric.md)、[platform-expansion/README.md](02-architecture/platform-expansion/README.md) |
| `03-dsl` | Block / Item / NBT / Tile DSL | [BLOCK_DSL_GUIDE_CN.md](03-dsl/BLOCK_DSL_GUIDE_CN.md)、[ITEM_DSL_GUIDE_CN.md](03-dsl/ITEM_DSL_GUIDE_CN.md)、[NBT_DSL_GUIDE.md](03-dsl/NBT_DSL_GUIDE.md)、[TILE_DSL_GUIDE_CN.md](03-dsl/TILE_DSL_GUIDE_CN.md) |
| `04-datagen` | DataGen 运行、输出、parity manifest | [DataGenerator.md](04-datagen/DataGenerator.md) |
| `04-systems` | 核心系统维护手册 | [SYSTEMS_MAINTENANCE_INDEX.md](04-systems/SYSTEMS_MAINTENANCE_INDEX.md) |
| `05-wireless` | 无线系统与无线 GUI | [WIRELESS_REFACTOR_CONTRACTS.md](05-wireless/WIRELESS_REFACTOR_CONTRACTS.md)、[Node_GUI.md](05-wireless/Node_GUI.md)、[Matrix_GUI.md](05-wireless/Matrix_GUI.md) |
| `06-gui` | GUI DSL 与分层 | [GUI_DSL.md](06-gui/GUI_DSL.md)、[GUI_Architecture_Refactoring.md](06-gui/GUI_Architecture_Refactoring.md) |
| `07-ability` | 能力系统规范 | [ABILITY_CORE_SPEC_V2.md](07-ability/ABILITY_CORE_SPEC_V2.md) |
| `testing` | 当前测试与验证边界 | [IMPLEMENTATION_SCOPE.md](testing/IMPLEMENTATION_SCOPE.md)、[MULTI_LOADER_VERIFICATION.md](testing/MULTI_LOADER_VERIFICATION.md) |
| `dev` | 开发、Agent、构建、治理文档 | [BUILD_AND_VERIFY_PLAYBOOK.md](dev/BUILD_AND_VERIFY_PLAYBOOK.md)、[ADD_NEW_LOADER_OR_VERSION.md](dev/ADD_NEW_LOADER_OR_VERSION.md)、[AGENT_AND_TOOLING.md](dev/AGENT_AND_TOOLING.md)、[MC_VERSION_SEAM.md](dev/MC_VERSION_SEAM.md)、[loader-hook-support.properties](dev/loader-hook-support.properties)、[CLIENT_INPUT_PROTOCOL_PLAYBOOK.md](dev/CLIENT_INPUT_PROTOCOL_PLAYBOOK.md) |

## 常用入口

- 上手：[GETTING_STARTED.md](01-overview/GETTING_STARTED.md)
- 布局：[PROJECT_LAYOUT.md](01-overview/PROJECT_LAYOUT.md)
- 版本缝：[MC_VERSION_SEAM.md](dev/MC_VERSION_SEAM.md)
- Agent 约定：[AGENT_AND_TOOLING.md](dev/AGENT_AND_TOOLING.md)
- 多 loader 验证：[MULTI_LOADER_VERIFICATION.md](testing/MULTI_LOADER_VERIFICATION.md)

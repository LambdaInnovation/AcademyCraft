# 平台目标与 Fabric / NeoForge 维护级别

当前平台架构不再用 Gradle 子工程表达 Loader / Minecraft 版本。所有平台构建都通过单一 `:platform` 工程执行，目标由 `platform-catalog.json` 声明。

## 当前目标

| target id | Loader | Minecraft | 维护级别 |
|-----------|--------|-----------|----------|
| `forge-1.20.1` | Forge | 1.20.1 | 主线支持 |
| `fabric-1.20.1` | Fabric | 1.20.1 | 维护支持 |
| `neoforge-1.21.1` | NeoForge | 1.21.1 | 对等支持目标 |

### Fabric 维护支持

- 保持 catalog、sourceSet、compile、metadata、AOT、datagen entrypoint 链路可维护
- 不承诺功能与 Forge 完全同步
- 不通过复制 Forge 代码补齐差异
- 差异必须落在 `platform-src/loader/fabric-1.20.1/` 或 catalog capabilities 中

### NeoForge 对等支持

- `neoforge-1.21.1` 是正式 catalog 目标（与 Forge/Fabric 并列），不是“政策暂缓”项
- 源码在 `platform-src/loader/neoforge-1.21.1/`；Minecraft 运行时在 `platform-src/minecraft/mc-1.21.1/`
- 元数据使用 `META-INF/neoforge.mods.toml`（不是 Forge 的 `mods.toml`）
- 与 1.20.1 目标共享 `minecraft-base`；跨版本 API 差异走 `cn.li.mcver` 版本缝（见 [MC_VERSION_SEAM.md](../dev/MC_VERSION_SEAM.md)）
- 仍通过 `scripts/target-gradle.ps1 neoforge-1.21.1` 选择；不要新增根目录 NeoForge 子工程

合成 fixture（如 catalog fixture 中的未来版本探测）不等于生产支持，不得写入本表。

## 组件职责

- 跨版本共享 Minecraft glue：`platform-src/minecraft/base/`（`cn.li.mcbase`）
- Minecraft 版本差异：`platform-src/minecraft/mc-1.20.1/`、`platform-src/minecraft/mc-1.21.1/`
- Loader 差异：`platform-src/loader/forge-1.20.1/`、`fabric-1.20.1/`、`neoforge-1.21.1/`
- 构建目标输出：`platform/build/targets/<target-id>/platform/`

## 禁止

- 不新增 `forge-1.20.1/`、`fabric-1.20.1/`、`neoforge-1.21.1/`、`mc-1.20.1/` 这类**仓库根目录**多模块平台工程
- 不使用平台 SPI、ServiceLoader bootstrap、task alias 或 pass-through namespace
- 不用 target id 字符串解析 loader/version；行为只能来自 `platform-catalog.json` 的显式字段
- 不把未在 catalog 中声明、未通过 compile/test/datagen/AOT 验证的 Loader/版本写成“已支持”

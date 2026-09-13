# 运行时架构与 DSL 总览

本文描述当前模块边界、启动顺序，以及 DSL 数据如何进入具体平台目标�?
## 模块分工

| 模块 / 组件 | 职责 | 边界 |
|-------------|------|------|
| `api` | 对外 Java API 与互操作接口�?| 不放业务实现�?|
| `mcmod` | DSL、协议、metadata、生命周期、平台抽象�?| 不引�?Minecraft / Loader API�?|
| `ac` | AcademyCraft 内容、无线、能量、能力、GUI 业务�?| 不引�?Minecraft / Loader API�?|
| `platform-src/minecraft/*` | Minecraft API 适配与版本差异�?| 不枚�?Loader�?|
| `platform-src/loader/<loader>/` | Loader entrypoint、注册、事件、client/datagen glue�?| 不复制业务逻辑�?|
| `:platform` | �?`platform-catalog.json` 组合上述 source components�?| 不按 Gradle 子工程表达版本�?|

## DSL �?metadata

- `cn.li.mcmod.block.dsl`：`defblock` / `defmultiblock`，写�?block registry�?- `cn.li.mcmod.item.dsl`：`defitem`，写�?item registry�?- `cn.li.mcmod.block.tile-dsl`：`deftile` / `deftile-kind`，声�?tile �?BlockEntityType 需求�?- `cn.li.mcmod.protocol.metadata`：平台侧唯一聚合查询入口�?
Loader 组件只消�?metadata，不扫描 `ac` 源码，也不硬编码内容 id�?
## 内容加载

`ac` 通过 `cn.li.ac.core/init` 安装内容与业�?hook�?
1. 设置 mod id、GUI 平台实现、slot validator、resource resolver 等运行依赖�?2. 初始化世界数据、无�?能量 Java API bridge�?3. 加载 `cn.li.ac.registry.content-namespaces` 中声明的内容命名空间，触�?`defblock` / `defitem` / `deftile`�?4. 注册 GUI factory、network handler、client renderer hook�?
## 平台启动

Loader Java entrypoint 是外部框架要求保留的入口。它只进入对�?Loader Clojure entry namespace，然后调�?`cn.li.platform.bootstrap` 的统一启动流程�?
启动流程读取 `META-INF/academy-target.edn`，校验当�?target �?capabilities，并确保每个 capability 只有一个实�?owner�?
## 注册与事�?
- Loader 组件�?metadata 注册 block、item、menu、entity、network、client renderer、datagen provider�?- Minecraft 版本组件承载 blockstate、datagen shell、Minecraft API 差异�?- 业务事件分发通过 `mcmod` 协议�?`ac` hook 完成，不通过 Loader 层复制业务逻辑�?
## DataGen

DataGen 由目标显式选择�?
```powershell
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```

输出进入 `platform/build/targets/<target-id>/platform/generated/datagen/<target-id>/`。hash manifest �?parity 比较�?[../04-datagen/DataGenerator.md](../04-datagen/DataGenerator.md)�?
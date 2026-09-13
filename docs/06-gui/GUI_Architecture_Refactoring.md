# GUI 架构

GUI 分为纯协议、业务定义、Minecraft 版本适配和 Loader glue 四层。

## Layering

| Layer | Location | Responsibility |
|-------|----------|----------------|
| Protocol | `mcmod/src/main/clojure/cn/li/mcmod/gui/` | GUI spec、registry、slot schema、handler protocol。 |
| Business | `ac/src/main/clojure/cn/li/ac/**/gui.clj` | 具体 screen/container 组装、业务 slot、quick move、presenter。 |
| Minecraft version | `platform-src/minecraft/mc-1.20.1/gui/` | Menu proxy、CGui runtime、Minecraft 1.20.1 API 适配。 |
| Loader | `platform-src/loader/forge/`、`platform-src/loader/fabric/` | MenuType / ScreenHandlerType 注册、client entrypoint、network glue。 |

## Rules

- `mcmod` 与 `ac` 不引用 Minecraft / Loader API。
- Loader 层不复制业务 GUI 规则，只注册 metadata 暴露的 GUI。
- Terminal 是 simple screen，不进入 block menu 生命周期。
- Slot validator 与 quick move 配置由 `mcmod` schema + `ac` 业务 GUI 提供，平台只执行。

## Key namespaces

- `cn.li.mcmod.gui.registry`
- `cn.li.mcmod.gui.spec`
- `cn.li.mcmod.gui.slot-schema`
- `cn.li.ac.gui.platform-adapter`
- `cn.li.ac.gui.tech-ui-common`
- `cn.li.ac.block.wireless-node.gui`
- `cn.li.ac.block.wireless-matrix.gui`
- `cn.li.mc1201.gui.menu.proxy`
- `cn.li.mc1201.gui.provider.dispatcher`

## Verification

```powershell
.\gradlew.bat verifyCurrentPlatforms
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```

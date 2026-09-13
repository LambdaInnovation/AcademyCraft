# GUI DSL

当前 GUI 声明以 `mcmod` 的纯数据 spec 为中心，业务 GUI 由 `ac` 注册，Loader 组件只负责 Minecraft 菜单、screen 与网络 glue。

## mcmod entrypoints

| Namespace | Responsibility |
|-----------|----------------|
| `cn.li.mcmod.gui.spec` | GUI spec 构造与 `register-block-gui!`。 |
| `cn.li.mcmod.gui.registry` | GUI metadata、screen factory 与 handler registry。 |
| `cn.li.mcmod.gui.handler` | 平台 GUI handler 协议。 |
| `cn.li.mcmod.gui.slot-schema` | Slot layout、quick move 与 validator 描述。 |
| `cn.li.mcmod.gui.xml-parser` | Runtime XML widget 读取。 |

`mcmod` 不引用 Minecraft / Loader API。

## ac ownership

Wireless、TechUI、Terminal 等业务 GUI 定义在 `ac` 对应 namespace 中。`ac` 通过 `cn.li.ac.gui.platform-adapter/install-into-mcmod!` 向 `mcmod` 注入容器回调与 screen factory。

## Platform ownership

- Minecraft API 适配：`platform-src/minecraft/mc-1.20.1/gui/`
- Forge glue：`platform-src/loader/forge/`
- Fabric glue：`platform-src/loader/fabric/`

Loader 组件不得复制业务 GUI 规则。

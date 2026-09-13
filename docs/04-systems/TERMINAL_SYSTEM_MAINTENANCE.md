# 数据终端（Terminal）系统维护手册

> 状态标签：**现行**（2026-06 函数式重构后）

无线矩阵「数据终端」由 `ac/src/main/clojure/cn/li/ac/terminal/` 实现：服务端会话状态 + 不可变应用目录 + 客户端 shell/launcher，**无**动态 SPI、manifest 注册表、`:gui-fn` 符号启动或专用 Forge terminal screen bridge。

---

## 系统职责

- 管理玩家是否已安装终端、已安装哪些终端应用（独立 NBT 键 `academy_terminal`，不经 ability runtime-store）。
- 通过固定线协议（`terminal.messages`）处理安装终端、安装/卸载应用、查询状态。
- 在客户端用 CGui XML（`assets/academy/guis/terminal.xml`）展示应用网格，按 catalog 元数据分页；已安装应用通过 launcher 表打开各 app GUI。
- 与无线频率发射器等玩法通过 catalog 中的 `:freq-transmitter` 等条目关联，不在服务端持有 GUI 函数。

## 模块边界

### 服务端（可进 dedicated server classpath）

| 命名空间 | 职责 |
|----------|------|
| `cn.li.ac.terminal.model` | 纯函数：`:terminal-data` 形状、`normalize-state`、`install-terminal` / `install-app` 等 |
| `cn.li.ac.terminal.catalog` | 不可变 `apps` 向量 + 查询（`app-by-id`、`ordered-apps`、`app-count` 等） |
| `cn.li.ac.terminal.player` | 会话 API：`state`、`install-terminal!`、`install-app!`、`terminal-installed?` |
| `cn.li.ac.terminal.network` | 注册 4 个网络 handler，校验 catalog 后写 player |
| `cn.li.ac.terminal.messages` | 线消息 ID 1000–1003（`:install-terminal` 等） |
| `cn.li.ac.terminal.init` | 生命周期：仅 `network/register-handlers!` |

**已删除、不得回流：** `registry.clj`（catalog 薄包装）、`player_data.clj`、`app_spi.clj`、`app_manifest.clj`、`app_registry.clj`、`platform_bridge.clj`、根目录 `terminal_gui.clj`、`apps/freq_transmitter_handlers.clj` 等。

### 客户端（仅 client 加载）

| 命名空间 | 职责 |
|----------|------|
| `cn.li.ac.terminal.client.shell` | 主终端 GUI、RPC、`create-terminal-gui`、`install-ui-hooks!`（`:ac/terminal-gui` widget 工厂） |
| `cn.li.ac.terminal.client.runtime` | 按玩家 owner-key 的终端 UI 状态（分页、loading、已安装集合、媒体播放事件） |
| `cn.li.ac.terminal.client.apps` | `launchers` 映射：catalog `:id` → `open!` |
| `cn.li.ac.terminal.client.apps.*` | 各应用 GUI（static_pages、media、skill_tree、freq） |
| `cn.li.ac.terminal.client.actions` | **唯一** `requiring-resolve` 侧载入口（items + `ac.core` client init） |

**已删除、不得回流：** `client/bridge.clj`（widget 注册已并入 `shell`）。

### 平台与 mcmod

- Widget 注册：`mcmod.client.ui.registry/register-widget-factory!`，键 `:ac/terminal-gui`。
- 打开屏幕：`mcmod.client.platform-bridge/open-screen!` `:ac/terminal`（非已删的 `mcmod.platform.terminal-ui`）。
- Forge **无** `terminal_screen_bridge`；简单 CGui 屏幕走通用 `cgui_screen_bridge`。
- Gradle：`verifyTerminalBridgeInitOrchestration`、`verifyTerminalMessageIdsStatic` 约束上述边界。

### 侧载边界（有意保留）

共享代码（`item/*`、`ac.core`）不得 `require` 客户端命名空间，只能通过 `requiring-resolve` 调用 `client.actions` 中的函数：

- `install-ui-hooks!` — `core` client init
- `open-terminal!`、`open-tutorial!`、`open-skill-tree!` — 物品右键等

## 运行时流程

### 打开终端（客户端）

1. Item 或快捷键触发 `client.actions/open-terminal!` → `shell/open-terminal`。
2. RPC `get-state`；若未安装则 `install-terminal`，成功后 `open-screen!` `:ac/terminal`。
3. 平台根据 `:ac/terminal-gui` 工厂调用 `shell/create-terminal-gui`。
4. Shell 用 `client.runtime` 持有分页/安装集；网格数据来自服务端 RPC + `catalog/ordered-apps` 排序。

### 安装应用（服务端物品）

1. `item/app_installers` 在 server 侧调用 `player/install-app!`。
2. 先检查 `terminal-installed?` 与 `catalog/app-exists?`。

### 启动已安装应用（客户端）

1. 网格点击已安装槽 → `client.apps/launch!`。
2. 按 `:id` 查 `launchers`，调用对应 `open!`（可能再走 `platform-bridge` 或 `open-simple-gui!`）。

## 扩展点

### 新增终端应用

1. 在 `catalog.clj` 的 `apps` 向量增加元数据（`:id`、`:name`、`:icon`、`:description`、`:category`），**不要**放 GUI 函数。
2. 在 `client/apps/` 实现 `open!`，并在 `client/apps.clj` 的 `launchers` 注册同一 `:id`。
3. 如需安装器物品，在 `item/app_installers.clj` 增加条目。
4. 单测：`catalog_test`、`client/apps_test`（launcher 键集与 catalog id 一致）。

### 修改线协议

- 在 `messages.clj` 增加 action 与 ID；同步 `network.clj` handler 与 `shell` RPC。
- 跑 `verifyTerminalMessageIdsStatic`；客户端禁止动态 resolve `terminal.network/msg-id`。

## 排障手册

| 现象 | 检查 |
|------|------|
| 终端屏幕空白 | `install-ui-hooks!` 是否在 client init 执行；Forge 是否仍误用已删 bridge |
| 应用格为空 / 404 | 服务端 `get-state`、session-id；网络 handler 是否注册（`terminal.init`） |
| 点击应用无反应 | `launchers` 是否含该 `:id`；是否已 `install-app` |
| 物品打不开教程/技能树 | `requiring-resolve` 是否指向 `client.actions/*`（非已删别名） |
| Dedicated server 加载 client ns | 物品 handler 是否在 `:server` 分支才写状态；client 代码仅 `requiring-resolve` |

## 变更风险

- 修改 `catalog` 中 `:id` 会改变 `:installed-apps` 与安装器物品语义；这类变更必须声明版本边界。
- 在服务端 `require` `client.apps` 会导致 classpath / 侧分离违规。
- 恢复 `registry` 或 `client.bridge` 会与 Gradle 门禁冲突。

## 兼容性约束

- 线消息 ID（1000–1003）视为稳定协议；变更需同步客户端 shell 与所有消费者。
- `:ac/terminal`、`:ac/terminal-gui` 屏幕/工厂键由 AC 拥有；`mcmod` 不得硬编码。
- 测试：`cn.li.ac.terminal.*-test`、`cn.li.ac.terminal.client.*-test`；过滤示例见 [BUILD_AND_VERIFY_PLAYBOOK.md](../dev/BUILD_AND_VERIFY_PLAYBOOK.md)。

## 关联文档

- GUI 总览：[../06-gui/GUI_Architecture_Refactoring.md](../06-gui/GUI_Architecture_Refactoring.md)
- 无线系统：[WIRELESS_SYSTEM_MAINTENANCE.md](WIRELESS_SYSTEM_MAINTENANCE.md)
- 清理门禁：[../dev/CLEANUP_RESIDUE_GUARDS.md](../dev/CLEANUP_RESIDUE_GUARDS.md)

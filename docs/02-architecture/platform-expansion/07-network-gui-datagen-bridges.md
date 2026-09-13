# Network / GUI / Datagen 平台桥接

## Purpose

对多平台扩展中最容易分化、也最容易失控的三类子系统进行专项约束：网络、GUI、数据生成。

## Network

### 共享层职责

- 定义消息 ID、共享协议和消息语义。
- 定义平台分发点（例如发送请求、注册 handler）。

### 平台模块职责

- 实现具体 Loader 的消息注册与发送。
- 处理消息序列化接线、通道创建与生命周期注册。
- 不改变共享层定义的消息语义。

### 检查项

- 新平台是否覆盖所有关键消息发送入口。
- 平台消息注册是否发生在正确生命周期阶段。
- 消息 ID 是否与共享层目录保持一致。

## GUI

### 共享层职责

- DSL、元数据、GUI 业务逻辑。
- 菜单/屏幕所依赖的共享数据与动作语义。

### 平台模块职责

- 注册 MenuType / ScreenHandlerType。
- 实现 open screen / menu provider / screen factory。
- 在 client init 中注册客户端 screen。

### 检查项

- common/server init 与 client init 是否清晰分离。
- GUI 平台桥接是否只在平台模块中出现。
- 新平台是否同时补齐服务端菜单与客户端 screen 两侧逻辑。

## Datagen

### 共享层职责

- 提供元数据、DSL 和 provider 可复用逻辑。
- 保持生成规则与业务定义一致。

### 平台模块职责

- 提供 datagen 入口。
- 把共享层 provider 接入该 Loader 的 datagen 生命周期。
- 确保 datagen 运行时所需的平台注册状态已准备好。

### 检查项

- datagen 是否有单独入口。
- datagen 是否依赖 main init 的隐式副作用。
- 不同 Loader 的 datagen 输出路径与参数是否在文档中明确。

## 推荐实践

1. 三类桥接都保留“共享抽象 + 平台接线”分工。
2. 每个新增平台模块都为 network / GUI / datagen 提供明确文件落点。
3. 代码评审时单独检查这三类子系统，避免只验证模块能编译却遗漏运行桥接。

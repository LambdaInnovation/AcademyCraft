# Client / Server 分离规则

本文描述当前代码的 side 边界。

## 规则

- `ac`、`mcmod`、`api` 不直接引用 `net.minecraft.client.*`、Forge client API 或 Fabric client API。
- Minecraft client API 只允许出现在平台组件中：
  - `platform-src/minecraft/*` 的 client 专属代码；
  - `platform-src/loader/<loader>/` 的 client entrypoint、renderer、screen glue。
- Dedicated server 启动路径不能静态加载 client-only namespace 或 Java class。
- Client-only Clojure namespace 通过 side check 后 `requiring-resolve`。
- Java client entrypoint 或 screen/helper class 必须使用对应 Loader 支持的 client-only 注解或入口约束。

## 依赖链

`ac` 与 Loader 组件不互相直接依赖。两者通过 `mcmod` 的生命周期、metadata、协议和 platform abstraction 连接。

```text
api   mcmod
 \     /  \
  ac  /    platform-src/minecraft/*
        \  platform-src/loader/<loader>
             -> :platform target
```

## 验证

```powershell
.\gradlew.bat verifyCurrentPlatforms
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
```

服务器验证重点：

- dedicated server 不出现 client class loading error；
- block/item/menu/network 注册正常；
- client setup 只在 client target lifecycle 执行。

# 构建、验证与排障速查

在包含根目录 `settings.gradle` 的 `minecraftmod` 目录执行命令。当前根工程为 `api`、`mcmod`、`ac`、`:platform`；平台目标由 `platform-catalog.json` 声明，并通过 `scripts/target-gradle.ps1`、`.cmd` 或 `.sh` 统一选择。

## 环境

- 编译环境 JDK 21；目标字节码 Java 17
- Gradle Wrapper：`gradlew.bat` / `gradlew`

## 常用任务（Windows）

| 目标 | 命令 |
|------|------|
| 运行默认 Forge 客户端 | `.\gradlew.bat :platform:runClient` |
| 运行默认 Forge 服务端 | `.\gradlew.bat :platform:runServer` |
| 运行默认 Forge DataGen | `.\gradlew.bat :platform:runData` |
| 构建 Fabric target | `.\scripts\target-gradle.ps1 fabric-1.20.1` |
| 快速编译 core | `.\gradlew.bat :ac:compileClojure :mcmod:compileClojure` |
| 快速编译 Forge target | `.`\\scripts\\target-gradle.ps1 forge-1.20.1`` |
| 快速编译 Fabric target | `.`\\scripts\\target-gradle.ps1 fabric-1.20.1`` |
| 生成 Forge 发布 jar | `.`\\scripts\\target-gradle.ps1 forge-1.20.1`` |
| 生成 Fabric 发布 jar | `.`\\scripts\\target-gradle.ps1 fabric-1.20.1`` |
| 架构门禁 | `.\gradlew.bat verifyCurrentPlatforms` |

## 推荐验证流程

1. 先跑 `verifyCurrentPlatforms`，确认架构门禁、manifest drift、target 硬编码和生成残留没有回归。
2. 对当前修改涉及的 loader 运行对应的 `scripts/target-gradle.* <target-id>`。
3. 需要跨 loader 对照时，用两次独立 Gradle invocation 或 CI matrix 分别跑 Forge/Fabric target。

## 输出

- 平台产物位于 `platform/build/targets/<target-id>/platform/`。
- 每次只构建一个 target；目标脚本会根据 catalog 选择对应 Gradle wrapper、Gradle 版本和 Java 版本。
- 最终可分发文件只取 `platform/build/targets/<target-id>/platform/libs/` 中的非 `shadow` jar；不要发布 `platform/build/targets/<target-id>/platform/devlibs/*.jar`、`*-shadow.jar`、`*-shadow-stripped.jar` 或 `:platform:jar` 的直接输出。
- target metadata 生成到 `platform/build/targets/<target-id>/platform/generated/target-metadata/META-INF/academy-target.edn`。
- DataGen 输出位于 `platform/build/targets/<target-id>/platform/generated/datagen/<target-id>/`，不写回源码目录。

# DataGenerator

DataGen 随 `:platform` target 运行。目标由 `scripts/target-gradle.ps1 <target-id>` 显式选择，配置来自 `platform-catalog.json`。

## Source ownership

- 共享 Minecraft DataGen 逻辑：`platform-src/minecraft/mc-1.20.1/src/main/clojure/cn/li/mc1201/datagen/`
- Forge provider glue：`platform-src/loader/forge/src/main/clojure/cn/li/forge1201/datagen/`
- Fabric provider glue：`platform-src/loader/fabric/src/main/clojure/cn/li/fabric1201/datagen/`
- Loader Java entrypoints：对应 `platform-src/loader/<loader>/src/main/java/`

## Commands

- Forge：`.\scripts\target-gradle.ps1 forge-1.20.1`
- Fabric：`.\scripts\target-gradle.ps1 fabric-1.20.1`
- Hash manifest only：`由对应 target 脚本构建后生成`
- Parity compare：`cmd /c .\gradlew.bat compareDatagenParityManifests`

## Output

生成内容写入 `platform/build/targets/<target-id>/platform/generated/datagen/<target-id>/`。每个 target 的 hash manifest 位于 `platform/build/targets/<target-id>/platform/generated/datagen/<target-id>/META-INF/academy-datagen-hashes.json`。

同一 `datagenParityGroup` 的比较由 CI matrix 先分别生成各 target 输出，再运行 `compareDatagenParityManifests` 读取 manifest。DataGen 结果不写回 `platform-src`，也不提交生成残留。

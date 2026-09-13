# Testing Scope

本文描述当前测试边界与推荐执行方式�?
## Scope

- `ac`：业务与领域规则测试，包括无线、能量、能力、GUI presenter 等平台无关逻辑�?- `mcmod`：DSL、metadata、协议、NBT、GUI spec、事件分发等平台无关基础契约�?- `api`：公开 Java API 与互操作接口的编�?契约检查�?- `:platform`：按 `platform-catalog.json` 选择单个 target，验�?Loader glue、metadata、AOT、client/datagen entrypoint �?Minecraft API 适配�?
## Boundaries

- `ac` / `mcmod` 测试不得直接依赖 Minecraft、Forge、Fabric 类�?- Loader 目标测试只验证平台接线与 runtime bootstrap，不复制业务规则�?- �?Loader 对照通过多次�?target Gradle invocation �?CI matrix 完成�?
## Current verification commands

```powershell
.\gradlew.bat verifyCurrentPlatforms
.\gradlew.bat :ac:test
.\gradlew.bat :mcmod:test
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```

DataGen parity:

```powershell
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
.\gradlew.bat compareDatagenParityManifests
```

## Failure triage

- `ac` failure：优先按业务规则回归处理�?- `mcmod` failure：优先检�?DSL / metadata / protocol 契约�?- `:platform` compile failure：先确认 selected target �?source components �?dependencies，再定位 Loader �?Minecraft 版本层�?- `verifyCurrentPlatforms` failure：按任务名修复架构残留、重�?capability owner、AOT manifest drift、target 硬编码或生成残留�?
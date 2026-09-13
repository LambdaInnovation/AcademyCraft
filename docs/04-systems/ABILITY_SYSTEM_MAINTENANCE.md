# Ability System Maintenance

能力系统负责能力定义、触发、冷却、上下文状态和平台事件接入。

## Ownership

| Layer | Location | Responsibility |
|-------|----------|----------------|
| Protocol | `mcmod` | ability metadata、event hooks、platform-neutral contracts。 |
| Business | `ac/src/main/clojure/cn/li/ac/ability/` and `ac/src/main/clojure/cn/li/ac/content/ability/` | skill definition、state reducer、effects orchestration。 |
| Platform | `platform-src/loader/<loader>/` | key/event/network/client glue only。 |

## Runtime model

能力状态采用 reducer-only 写路径：

```text
command-runtime
  -> reducer
  -> runtime-store
  -> effects.interpreter
```

Context 读取通过 `context-dispatcher`、`context-skill-state`、`context-projection` 等当前 service namespace 完成。业务技能不得绕过 reducer 直接 patch runtime store。

## Key namespaces

- `cn.li.ac.ability.service.command-runtime`
- `cn.li.ac.ability.service.reducer`
- `cn.li.ac.ability.service.runtime-store`
- `cn.li.ac.ability.service.context-dispatcher`
- `cn.li.ac.ability.service.context-manager`
- `cn.li.ac.ability.service.context-skill-state`
- `cn.li.ac.ability.effects.interpreter`
- `cn.li.ac.content.ability.*`

## Platform boundary

- Loader 组件捕获 Minecraft / Loader 事件并转入 `mcmod` / `ac` contract。
- Loader 组件不保存能力业务状态。
- Client key binding、renderer、packet glue 必须留在 platform 或 Minecraft version component。

## Verification

```powershell
.\gradlew.bat :ac:test
.\gradlew.bat :mcmod:test
.\gradlew.bat verifyCurrentPlatforms
```

能力改动涉及平台事件时，再按目标编译：

```powershell
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```

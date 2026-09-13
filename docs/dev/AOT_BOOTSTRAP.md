# AOT Bootstrap Safety

This document explains why top-level registry/bootstrap access during Clojure AOT causes `IllegalArgumentException: Not bootstrapped`, and how to fix it.

## Core rule

Never touch Minecraft registry/bootstrap-dependent state from top-level forms that execute during namespace load.

Use:
- `cn.li.mcmod.aot/ensure-runtime!`
- `cn.li.mcmod.runtime.deferred/deferred`

## Static verification

Run root task:
- `verifyAotBootstrapSafety`

The linter script lives in `tools/aot-linter/aot_safety.clj` and the allowlist in `docs/dev/aot-linter-allowlist.edn`.

## 开发注意事项（必读）

1. **顶层初始化只放纯数据或惰性包装**
	- 允许：常量、纯函数、`delay`、`deferred`。
	- 禁止：顶层直接访问 `Blocks/*`、`Items/*`、`BuiltInRegistries/*`、`ForgeRegistries/*`、`DeferredRegister/*`、`create-*-register` 等。

2. **注册/属性创建统一延迟到运行期**
	- 统一使用 `cn.li.mcmod.runtime.deferred/deferred` 包装注册器、属性构造和桥接对象。
	- 触发时由 `cn.li.mcmod.aot/ensure-runtime!` 护栏兜底，编译期误触会直接报出可定位错误。

3. **不要吞异常（尤其是 Not bootstrapped）**
	- 禁止 `catch` 后仅打印或静默返回默认值。
	- 允许在边界层补充上下文后再抛出（文件/命名空间/修复建议）。

4. **改完先跑静态门禁，再跑平台编译**
	- 最小建议顺序：
	  - `verifyAotBootstrapSafety`
	  - `:platform:compileClojure -x test`
	  - `:platform:compileClojure -x test`

5. **禁止通过绕过 compileJava“修复” compileClojure**
	- `:platform:compileClojure -x compileJava` 只用于诊断。
	- 正常构建必须让 `compileJava` 先产出 bridge/accessor classes。

## 出问题时怎么查（SOP）

### 1) 先判定是哪一类问题

- **AOT 静态门禁失败**：`verifyAotBootstrapSafety` 报 `file:line symbol=...`
- **编译期触发 runtime**：`compileClojure` 报 `Not bootstrapped` 或 `compile-phase-violation`
- **平台类缺失**：`ClassNotFoundException`（常见于跳过 `compileJava` 或上游 Java 编译失败）

### 2) 按顺序执行排查命令

1. `cmd /c .\gradlew.bat verifyAotBootstrapSafety`
	- 若失败：先按输出中的 `file:line` 改顶层初始化。
2. `cmd /c .\gradlew.bat :platform:compileClojure -x test`
3. `cmd /c .\gradlew.bat :platform:compileClojure -x test`
4. 若 Fabric 报类缺失，再看：
	- `cmd /c .\gradlew.bat :platform:compileJava`

### 3) 典型现象 -> 根因 -> 处理

- **现象**：`IllegalArgumentException: Not bootstrapped`
  - 根因：命名空间顶层触发了 registry/bootstrap 访问。
  - 处理：把顶层逻辑搬到 `deferred` / `delay` / 运行期函数；必要处调用 `ensure-runtime!`。

- **现象**：`ClassNotFoundException`（`MinecraftClientAccess` / `AtomBackedDataSlot` / `ScriptedEntitySpecAccess` 等）
  - 根因：Java 输出目录未就绪（跳过了 `compileJava` 或 Java 本身编译失败）。
  - 处理：不要跳过 `compileJava`；先修 Java 错误再跑 `compileClojure`。

- **现象**：`No matching ctor found for ... Pack$Info`
  - 处理：字体现由 MSDF shadow `FontSet` + STB 在 `MsdfFontManager` 初始化；勿恢复 Clojure 反射构造 `Pack$Info`。

### 4) 如何验证“防呆机制”确实生效

- 正向：`cmd /c .\gradlew.bat verifyAotBootstrapSafety` 应通过。
- 负向：`cmd /c .\gradlew.bat verifyAotBootstrapSafetyNegativeFixture` 应通过（表示“故意违规样例被正确拦截”）。

### 5) 排查时的日志与产物位置

- AOT linter 脚本：`tools/aot-linter/aot_safety.clj`
- allowlist：`docs/dev/aot-linter-allowlist.edn`
- 负向样例：`tools/aot-linter/fixtures/bad_top_level_registry.clj`

建议在提交前把失败输出中的 `file:line + symbol + hint` 一并贴入 PR 描述，便于 reviewer 快速复现与验收。

# Agent and Tooling Rules

This repository uses a single `:platform` Gradle project. Loader/version behavior is selected only by `platform-catalog.json` plus `scripts/target-gradle.ps1 <target-id>`.

## Current layout

- `api`: Java API and external contracts.
- `mcmod`: loader-neutral runtime framework.
- `ac`: content layer.
- `platform-src/minecraft/base`: version-agnostic Minecraft glue (`cn.li.mcbase.*`).
- `platform-src/minecraft/mc-1.20.1`: Minecraft 1.20.1 adaptation (`cn.li.mc1201.*`) plus version seam (`cn.li.mcver.*`).
- `platform-src/minecraft/mc-1.21.1`: Minecraft 1.21.1 adaptation (`cn.li.mc1211.*`) plus version seam (`cn.li.mcver.*`).
- `platform-src/minecraft/mc-26.2`: Minecraft 26.2 adaptation (`cn.li.mc262.*`) plus version seam (`cn.li.mcver.*`; contract shape).
- `platform-src/loader/forge-1.20.1`: Forge loader entrypoints, metadata, events, and loader bindings.
- `platform-src/loader/fabric-1.20.1`: Fabric loader entrypoints, metadata, events, and loader bindings.
- `platform-src/loader/neoforge-shared`: NeoForge shared glue (`cn.li.neoforgebase.*`) for 1.21.1 + 26.2.
- `platform-src/loader/neoforge-1.21.1`: NeoForge 1.21.1 loader entrypoints, metadata, events, and loader bindings.
- `platform-src/loader/neoforge-26.2`: NeoForge 26.2 loader entrypoints (MDG / Java 25 / Gradle 9.2).
- `platform`: the single Gradle platform project.
- `platform-builds/mdg-gradle-9.2/`: isolated Gradle wrapper used by the `mc262-mdg-gradle-9.2` build profile.

Use only the current target catalog architecture. Do not add root platform modules, platform SPI, task aliases, pass-through namespaces, or dual-track implementations.

Version-seam rules: [MC_VERSION_SEAM.md](MC_VERSION_SEAM.md). Loader hook capability matrix: [loader-hook-support.properties](loader-hook-support.properties).

## Common commands

- Architecture gate: `cmd /c .\gradlew.bat verifyCurrentPlatforms`
- Install clj-kondo binary: `cmd /c .\gradlew.bat downloadCljKondo`
- Clojure lint gate (must run **per target** so that target's source roots are scanned): `.\scripts\target-gradle.ps1 <target-id> lintClojureNative`
- Neutral-layer API gate: `cmd /c .\gradlew.bat verifyNeutralClojureNoMinecraftApis` (blocks `net.minecraft.*` / Forge / Fabric / NeoForge refs in `ac`/`mcmod`; clj-kondo hook also errors on `:import` there). Note: `:import` is **not** a Clojure reflection warning — Reflection Guard only catches untyped interop.
- Reactive-UI id check (**on demand, not a gate**): `cmd /c .\gradlew.bat verifyUiXmlIds` — reports node ids that Clojure looks up but no `guis/**/*.xml` or `build-child!` spec declares. A wrong id costs nothing until its branch actually runs, so it ships green and fails in front of a player; the check is textual, cannot see ids assembled at runtime, and therefore advises rather than blocks. Deliberately outside `verifyCurrentPlatforms` and `lintClojureNative`.
- LVT strip (packaging): always on (`stripAotLvt` / `stripPlatformOutputLvt` / `stripShadowJarLvt`) for Loom 1.13 tiny-remapper. AOT also sets `-Dclojure.compile.elide-meta=[:doc]` (keeps `:file`/`:line`). MDG targets do not remap; LVT strip still runs for packaging hygiene where configured.
- Forge compile: `.\scripts\target-gradle.ps1 forge-1.20.1 :platform:compileClojure`
- Fabric compile: `.\scripts\target-gradle.ps1 fabric-1.20.1 :platform:compileClojure`
- NeoForge 1.21.1 compile: `.\scripts\target-gradle.ps1 neoforge-1.21.1 :platform:compileClojure`
- NeoForge 26.2 build: `.\scripts\target-gradle.ps1 neoforge-26.2 :platform:build` (requires JDK 25 / `MC_JAVA_HOME_25`)
- Target tests: `.\scripts\target-gradle.ps1 <target-id> :platform:runPlatformClojureTests`
- NeoForge 26.2 datagen: `.\scripts\target-gradle.ps1 neoforge-26.2 :platform:runData`
- Forge / Fabric / NeoForge lint:
  - `.\scripts\target-gradle.ps1 forge-1.20.1 lintClojureNative`
  - `.\scripts\target-gradle.ps1 fabric-1.20.1 lintClojureNative`
  - `.\scripts\target-gradle.ps1 neoforge-1.21.1 lintClojureNative`
  - `.\scripts\target-gradle.ps1 neoforge-26.2 lintClojureNative`

Use `scripts/target-gradle.ps1`, `scripts/target-gradle.cmd`, or `scripts/target-gradle.sh`; these are the only platform build entry points and select the target runtime from the catalog. Loom / MDG / clojurephant / shadow plugin versions come from the target's `buildProfile` in `platform-catalog.json` via `settings.gradle` `pluginManagement` (`toolchain`: `loom` or `mdg`).

## Architecture rules

- Never infer loader or Minecraft version by parsing the target id string; read the catalog model.
- Do not auto-generate a loader/version cartesian product. Every supported target must be explicitly declared.
- Minecraft components must not enumerate Forge/Fabric/NeoForge. Loader lifecycle belongs only to loader components.
- `ac` and `mcmod` must not depend on Minecraft, Forge, Fabric, NeoForge, or other loader APIs.
- Java entrypoints, client/datagen entrypoints, and loader metadata are allowed only because external frameworks require them. Internal pass-through namespaces are not allowed.
- Datagen output belongs under `build/targets/<target-id>/platform/generated/datagen/<target-id>/`; do not write generated output back to source directories.
- Do not add a real new-loader target, dependency, source tree, documentation promise, or release artifact unless the project explicitly decides to support it.

## Required gate

`verifyCurrentPlatforms` aggregates:

- `verifyBuildProfiles`
- `verifyNoLegacyArchitecture`
- `verifyNoThinForwarders`
- `verifyNoDuplicateCapabilities`
- `verifyNoTargetHardcoding`
- `verifyRepositoryHygiene`
- `verifyVersionSeamParity`
- `verifyNeutralClojureNoMinecraftApis` (and related catalog/entrypoint checks as configured)

## Logging conventions (mandatory)

All Clojure logging goes through `cn.li.mcmod.util.log` (`log/info|warn|error|debug|stacktrace`; single SLF4J logger named by mod id). The production jar ships with the loader's default log config (INFO visible), so every `info|warn|error` call site is production-visible and pays eager string construction — level choice is a performance decision, not a style preference.

| Level | Allowed for |
|---|---|
| `info` | One-time init/registration per subsystem, world save/load, admin command outcomes, player-visible state changes (node/network created/destroyed, terminal install) — max ~1 line per action |
| `debug` | Everything per-action/per-keypress/per-frame/per-tick: GUI open steps, packet send/receive, sync traces, validation sweeps, expected-failure catches (raycast miss, node at capacity, out of range, no handler registered), hot-path failure catches. The `debug` macro is lazy — zero cost when disabled |
| `warn` | Recoverable-but-notable conditions: config fallbacks, schema migration, integration failures (JEI/CraftTweaker), timeout, missing resources. Failure-only catches in hot paths belong at `debug`, not `warn` |
| `error` / `stacktrace` | Real failures. Prefer `(log/stacktrace "context" e)` over `(log/error "msg:" (ex-message e))` — never log an exception without its stack; never log the same exception twice (no error+stacktrace pairs) |

Rules:

- **No step-by-step traces at `info`**: a function that logs "called → doing → success" at info is a bug; keep at most one outcome line.
- **No `[X-TRACE]` labels at `info`** — trace labels imply debug.
- **Per-frame/per-tick failure catches log at `debug`** (a broken renderer/ability must not flood the log 20x/s); use the rate-limited pattern in `mcmod/.../client/content_actions.clj` if a condition must stay visible.
- **Never `(log/error "msg:" (ex-message e))`** — use `log/stacktrace`. When a failure is an expected game outcome (capacity, range, no-target), log at `debug` or don't log.
- The logger has no `trace` level; diagnostics go to `debug`.

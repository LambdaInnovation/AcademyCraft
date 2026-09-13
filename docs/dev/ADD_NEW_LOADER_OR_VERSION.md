# Add a Loader or Minecraft Version

This guide describes the current target-catalog architecture.

Adding a real supported target is a product decision. Prove architecture
expansion with focused catalog/source-set/capability unit tests against the
single production catalog instead of inventing release artifacts.

## Principles

- Use `platform-catalog.json` as the only target catalog.
- Add source components explicitly; do not infer behavior from a target id string.
- Do not generate loader/version combinations automatically.
- Put Minecraft API differences under `platform-src/minecraft/*`.
- Put loader lifecycle, metadata, and event bindings under `platform-src/loader/*`.
- Keep `api`, `mcmod`, and `ac` free of Minecraft/loader APIs.
- Do not add platform SPI, ServiceLoader platform bootstrap files, pass-through namespaces, or task aliases.
- Declare `buildProfile.toolchain` as `loom` or `mdg`. Minecraft 26.x+ NeoForge targets use ModDevGradle (`mdg`) with an isolated Gradle wrapper (see `platform-builds/mdg-gradle-9.2/`).

## Adding a Minecraft version component

1. Create a new version component under `platform-src/minecraft/mc-<version>/` (e.g. `mc-26.2`).
2. Move only version-specific Minecraft API differences into that component (`cn.li.mc*`).
3. Keep version-agnostic shared Minecraft glue in `platform-src/minecraft/base/` (`cn.li.mcbase`); do not put version forks there.
4. Implement `cn.li.mcver.*` under the new component so it matches the existing seam set (contracts shaped like the newest supported MC version; older versions downgrade). See `docs/dev/MC_VERSION_SEAM.md` and run `verifyVersionSeamParity`.
5. Declare `javaNamespace` on the catalog component (used by seam purity gates).
6. Add the component to the target catalog only for targets that are intentionally supported.
7. Add the component source and target tests; AOT namespaces are derived automatically from its Clojure sources.
8. Run the relevant single-target compile/test command and `verifyCurrentPlatforms`.

## Adding a loader component

1. Create a new loader component under `platform-src/loader/<loader>-<mc-version>/` (versioned directory, matching catalog ids such as `forge-1.20.1` / `neoforge-1.21.1` / `neoforge-26.2`).
2. Keep only framework-required entrypoints, metadata, client/datagen entrypoints, and loader event bindings there.
3. Route lifecycle to the shared platform bootstrap directly; do not add a ServiceLoader bootstrap SPI.
4. Declare dependencies and capabilities through the target catalog/build logic.
5. Add tests for capability ownership and source component selection before adding a real target.
6. For NeoForge multi-version support, put stable cross-version glue in `platform-src/loader/neoforge-shared/` (`cn.li.neoforgebase.*`, catalog `kind: loader-shared`) and keep version loaders thin.

### NeoForge note

NeoForge is a first-class peer loader for supported targets (today: `neoforge-1.21.1`, `neoforge-26.2`).

- Put NeoForge sources under `platform-src/loader/neoforge-<mc-version>/`.
- Shared NeoForge utilities live under `platform-src/loader/neoforge-shared/` (`cn.li.neoforgebase`); that component must not reference version namespaces.
- Use `META-INF/neoforge.mods.toml` (not Forge `mods.toml`).
- Pair with the matching `minecraft-<version>` + `minecraft-base` (+ `neoforge-shared` when applicable) source components in `platform-catalog.json`.
- Do not treat NeoForge as a Forge subdirectory.
- Minecraft 26.2+ uses the `mdg` toolchain profile (Java 25). Older NeoForge targets stay on Loom.
- MDG exposes `clientData()` / `serverData()`, but sharing one `--output` across
  both runs lets each HashCache delete the other run's files. For NeoForge 26.2,
  register **all** datagen providers on `GatherDataEvent.Client` and use
  `clientData()` only via `:platform:runData`. The hash manifest must still
  contain both `assets/` and `data/` outputs.

## Adding a real target

1. Add an explicit entry in `platform-catalog.json`.
2. Declare:
   - `loader`
   - `minecraftVersion`
   - `buildProfile` (and therefore `toolchain`)
   - Java version fields (`gradleJvmVersion` / `compileJavaVersion` / `runtimeJavaVersion`)
   - source components
   - test components
   - capabilities
   - capability owners
   - dependencies
   - artifact metadata
   - `datagenParityGroup` when datagen applies
3. Do not rely on parsing the target id.
4. Do not add publishing or docs commitments until compile, tests, datagen, AOT, and artifact checks pass for that target.

## Verification

For each real supported target:

1. `.\scripts\target-gradle.ps1 <target-id> :platform:build`
2. `.\scripts\target-gradle.ps1 <target-id> :platform:runPlatformClojureTests`
3. Datagen (`:platform:runData`) for that target, when applicable.
4. `cmd /c .\gradlew.bat verifyCurrentPlatforms`

Do not use old per-loader module tasks; those modules must not exist.
Do not run `runClient` / `runServer` as part of automated gates unless explicitly requested.

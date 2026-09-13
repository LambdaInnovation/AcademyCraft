# Multi-loader verification

Current supported targets are declared in `platform-catalog.json`:

- `forge-1.20.1` (Loom / Gradle 8.8)
- `fabric-1.20.1` (Loom / Gradle 8.8)
- `neoforge-1.21.1` (Loom / Gradle 8.8)
- `neoforge-26.2` (ModDevGradle / Gradle 9.2 / Java 25; isolated wrapper under `platform-builds/mdg-gradle-9.2/`)

Required local gate:

```text
cmd /c .\gradlew.bat verifyCurrentPlatforms
```

For target-specific checks, run a single selected target (including per-target Clojure lint, which only scans that target's source roots):

```text
.\scripts\target-gradle.ps1 forge-1.20.1 lintClojureNative
.\scripts\target-gradle.ps1 fabric-1.20.1 lintClojureNative
.\scripts\target-gradle.ps1 neoforge-1.21.1 lintClojureNative
.\scripts\target-gradle.ps1 neoforge-26.2 lintClojureNative
```

Compile / build smoke examples:

```text
.\scripts\target-gradle.ps1 forge-1.20.1 :platform:compileJava
.\scripts\target-gradle.ps1 neoforge-1.21.1 :platform:build
.\scripts\target-gradle.ps1 neoforge-26.2 :platform:build
.\scripts\target-gradle.ps1 neoforge-26.2 :platform:runData
```

Run the target-local Clojure test suite for every production target:

```text
.\scripts\target-gradle.ps1 forge-1.20.1 :platform:runPlatformClojureTests
.\scripts\target-gradle.ps1 fabric-1.20.1 :platform:runPlatformClojureTests
.\scripts\target-gradle.ps1 neoforge-1.21.1 :platform:runPlatformClojureTests
.\scripts\target-gradle.ps1 neoforge-26.2 :platform:runPlatformClojureTests
```

Do not use old root module commands. Do not add a new real target only to test architecture expansion without an explicit product decision.

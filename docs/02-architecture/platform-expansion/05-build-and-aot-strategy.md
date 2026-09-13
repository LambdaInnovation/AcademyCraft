# Build and AOT strategy

All platform builds use the single `:platform` project and a selected target.

Examples:

- `.`\\scripts\\target-gradle.ps1 forge-1.20.1``
- `.`\\scripts\\target-gradle.ps1 fabric-1.20.1``
- `cmd /c .\gradlew.bat verifyCurrentPlatforms`

AOT is exhaustive for each selected source component. The build derives the namespace set directly from its Clojure source roots, so moves and renames need no separate list update.

Datagen output goes under `platform/build/targets/<target-id>/platform/generated/datagen/<target-id>/` with `META-INF/academy-datagen-hashes.json`. `runData` / `runDatagen` finalize by writing the manifest, and `compareDatagenParityManifests` compares targets in the same `datagenParityGroup` after matrix jobs collect or preserve those outputs. Parity never belongs in duplicated source directories.

## Neutral-project AOT policy

`ac` and `mcmod` are neutral runtime projects. They do not directly reference Minecraft, Forge, NeoForge, or Fabric APIs, so their main Clojure sources are not independently AOT-compiled by default. This keeps the neutral projects reusable across loader/version targets while allowing the selected platform code to AOT whatever it actually requires transitively.

The switch is controlled by the root Gradle property:

```powershell
# Default: source-first neutral projects; platform AOT remains enabled
.\gradlew.bat :platform:jar

# Explicitly restore the historical full-AOT build
.\gradlew.bat :platform:jar -PfullAot=true
# Bare -PfullAot is equivalent to true
```

`-PfullAot=false` is accepted explicitly. Any other value fails during Gradle configuration. The default is equivalent to `false`.

### Mixed Jar representation

#### Non-negotiable packaging contract

The build uses a strict one-of-two representation for every neutral
namespace. This is a correctness rule, not an optimization switch:

- If platform AOT transitively compiles a namespace, the final Jar keeps all
  of that namespace's generated `.class` files and must not copy the matching
  `.clj`/`.cljc` resource.
- If platform AOT does not compile a neutral namespace, the final Jar carries
  only its `.clj`/`.cljc` resource.
- The build must never reduce the AOT boundary by preloading neutral source,
  deleting transitively generated classes, or modifying the AOT output after
  compilation.
- AOT-boundary reduction is permitted only by refactoring platform source to
  remove its static dependencies on neutral namespaces.

`verifyNeutralClojurePackaging` enforces the first two rules. Any proposed
build change that violates this contract is rejected, even if it lowers the
reported AOT count.

In source-first mode, the platform compile writes AOT classes to its target-local Clojure output. `prepareNeutralClojureRuntimeSources` scans that output for `__init.class` files and compares the class paths with the neutral `ac`/`mcmod` source paths. The final Jar/Shadow Jar contains exactly one representation per namespace:

- a namespace with a platform AOT `__init.class` is packaged as classes only;
- a neutral namespace not present in the platform AOT output is packaged as `.clj`/`.cljc` source;
- platform source namespaces are never copied as raw source resources.

The comparison uses resource-relative paths rather than namespace-string normalization, so hyphen/underscore naming cannot create a false match. `verifyNeutralClojurePackaging` fails if the generated source set does not equal `all-neutral-sources - current-platform-aot-namespaces`.

The filtered source directory is staged into the standard `resources/main` output after `processResources` completes. Jar/Shadow Jar and loader run tasks therefore see the same resources. It is intentionally not added as a `processResources` input: Clojurephant's `compileClojure` consumes the SourceSet resources, and doing so would create a `compileClojure -> processResources -> prepare -> compileClojure` task cycle.

When switching from full AOT back to source-first mode, the build removes old neutral AOT classes mirrored into the platform output and clears stale `ac`/`mcmod` Clojure output. This makes switching `-PfullAot=true` and the default mode safe without a manual clean.

### Recommended verification

```powershell
# Architecture and target gates
.\gradlew.bat verifyCurrentPlatforms

# Default mixed representation
.\gradlew.bat :platform:verifyNeutralClojurePackaging

# Full-AOT compatibility path
.\gradlew.bat :platform:verifyNeutralClojurePackaging -PfullAot=true
```

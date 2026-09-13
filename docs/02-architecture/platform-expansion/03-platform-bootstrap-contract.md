# Platform bootstrap contract

The old platform bootstrap SPI is removed.

Loader-required Java entrypoints are allowed only as framework entrypoints. They should require the loader Clojure entry namespace and call an explicit `start-<loader>-mod!`, `start-<loader>-client!`, or datagen function.

The loader entry function then calls the shared platform bootstrap in `cn.li.platform.bootstrap`. There is no platform SPI or ServiceLoader platform bootstrap file.

## Invariants

- Exactly one target is selected for a Gradle invocation.
- Target capabilities come from `META-INF/academy-target.edn` generated from `platform-catalog.json`.
- Runtime validates that target capabilities have exactly one implementation owner.
- Loader code may depend on Minecraft/loader APIs; core projects may not.

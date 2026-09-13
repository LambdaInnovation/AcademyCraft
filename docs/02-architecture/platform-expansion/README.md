# Platform expansion docs

This directory documents the current target-catalog platform architecture.

Use these rules for all loader/version work:

- One Gradle project: `:platform`.
- One target catalog: `platform-catalog.json`.
- Explicit source components; no target-id string parsing.
- No generated cartesian product of loader/version combinations.
- No platform bootstrap SPI or ServiceLoader platform bootstrap files.
- No real new-loader source tree, dependency, release artifact, or support promise unless the project intentionally adds that supported target.
- Use focused catalog/component-selection unit tests for architecture-only
  expansion checks; do not maintain a synthetic target catalog.

Files:

1. `01-target-architecture.md` — target catalog model and source component layout.
2. `02-module-boundaries.md` — ownership boundaries.
3. `03-platform-bootstrap-contract.md` — direct bootstrap contract.
4. `04-loader-lifecycle-map.md` — Forge/Fabric lifecycle ownership.
5. `05-build-and-aot-strategy.md` — single-target build and AOT strategy.
6. `06-versioning-strategy.md` — adding versions/loaders without duplication.
7. `07-network-gui-datagen-bridges.md` — bridge ownership notes.
8. `08-client-server-boundary-rules.md` — physical side rules.

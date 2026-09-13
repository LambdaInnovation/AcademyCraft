# Platform component template

This repository no longer has per-platform Gradle modules. A platform extension is a set of source components plus a catalog entry.

## Loader component

```text
platform-src/loader/<loader>-<mc-version>/
  src/main/java/...          # framework-required loader entrypoints only
  src/main/clojure/...       # loader lifecycle and bindings
  src/main/resources/...     # loader metadata only
  src/client/...             # client entry/bindings when needed
  src/datagen/...            # datagen entry/bindings when needed
```

Rules:

- Keep Java entrypoints limited to framework-required calls.
- Do not create internal pass-through namespaces.
- Do not create platform bootstrap SPI classes or ServiceLoader files.
- Call the shared bootstrap directly from the loader entry namespace.

## Minecraft version component

```text
platform-src/minecraft/mc-<version>/
  src/main/clojure/...
  src/main/java/...
```

Rules:

- Put only version-specific Minecraft API differences here.
- Move shared code to `platform-src/minecraft/base`.
- Do not mention loader families from Minecraft components.

## Catalog entry

Add a target only when it is intentionally supported. Validate catalog and
component-selection rules with focused unit tests; do not add a fake production
target or a second catalog.

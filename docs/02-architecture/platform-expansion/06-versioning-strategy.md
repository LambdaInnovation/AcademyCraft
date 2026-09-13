# Versioning strategy

Adding a Minecraft version or loader is explicit and incremental.

## Minecraft version

- Add a version component under `platform-src/minecraft/mc-<version>`.
- Keep shared Minecraft glue in `platform-src/minecraft/base`.
- Add the version component only to explicitly supported targets.

## Loader

- Add a versioned loader component under `platform-src/loader/<loader>-<version>`.
- Keep loader lifecycle and metadata there.
- Do not add a platform bootstrap SPI.

## Target

- Add a `platform-catalog.json` entry only when the combination is intentionally supported.
- Use focused catalog/component-selection unit tests for architecture-only
  validation; do not add a synthetic target catalog.

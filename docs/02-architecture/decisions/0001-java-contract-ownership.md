# ADR 0001: Java contract ownership

## Status

Accepted.

## Decision

Java contracts have one canonical owner:

- Public external API belongs in `api`.
- Runtime framework contracts belong in `mcmod`.
- Minecraft-version Java adapters belong in `platform-src/minecraft/mc-<version>`.
- Loader Java entrypoints and metadata-required classes belong in `platform-src/loader/<loader>`.

Do not duplicate the same contract in multiple packages. If a contract moves, update all callers in the same change and remove the previous package path.

## Rules

- `ac` implements business behavior and may consume `api` / `mcmod` contracts.
- `mcmod` must not depend on `ac` or Loader APIs.
- Loader components must not depend directly on `ac`; they reach content through metadata, lifecycle and platform contracts.
- Java entrypoints required by Forge/Fabric are allowed, but must stay limited to framework handoff.

## Validation

```powershell
.\gradlew.bat verifyCurrentPlatforms
.\gradlew.bat :ac:compileClojure :mcmod:compileClojure
.`\\scripts\\target-gradle.ps1 forge-1.20.1`
.`\\scripts\\target-gradle.ps1 fabric-1.20.1`
```

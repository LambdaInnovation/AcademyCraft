# Architecture Guard Tasks

`verifyCurrentPlatforms` aggregates repository hygiene and platform architecture checks. This document describes the current intent of those checks.

## Aggregated checks

| Task | Intent |
|------|--------|
| `verifyNoLegacyArchitecture` | Root layout must remain `api` / `mcmod` / `ac` / `:platform` with target catalog selection. |
| `verifyNoThinForwarders` | Internal pass-through namespaces are not allowed. |
| `verifyNoDuplicateCapabilities` | Each target capability has exactly one owner. |
| `verifyNoTargetHardcoding` | Build/source logic must read target data from `platform-catalog.json`. |
| `verifyRepositoryHygiene` | Generated output and transient logs stay out of source directories. |

## Current architecture seams

These boundaries are intentional and should remain explicit:

- `mcmod` protocols, DSL, metadata and lifecycle contracts.
- `platform-src/minecraft/*` Minecraft API adapters.
- `platform-src/loader/<loader>` Loader lifecycle, entrypoint and metadata glue.
- `ac` business APIs such as wireless, energy, ability and GUI presenters.

## Required command

```powershell
.\gradlew.bat verifyCurrentPlatforms
```

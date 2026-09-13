# Framework State Architecture

Framework-level mutable state is allowed only at explicit runtime boundaries.

## Allowed state owners

| Area | Owner |
|------|-------|
| Content metadata | `mcmod` registries loaded during content init. |
| World runtime state | `ac` data/runtime namespaces for each system. |
| Platform target metadata | generated `META-INF/academy-target.edn`. |
| Loader lifecycle state | selected Loader component under `platform-src/loader/<loader>/`. |

## Rules

- Prefer pure data and explicit parameters inside domain code.
- Keep mutable vars private to their owning namespace unless documented as a public runtime boundary.
- Do not use dynamic vars for platform implementation lookup when target metadata or explicit installation can provide the dependency.
- Clear world/player/session state through the owning service, not by reaching into another namespace's internals.

## Verification

Use `verifyCurrentPlatforms` plus targeted unit tests for the owning system.

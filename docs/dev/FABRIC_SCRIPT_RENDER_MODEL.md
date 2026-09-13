# Fabric ScriptRender Model (v1)

## Status

- Current model: **kind-level routing**.
- Scripted entity kinds are registered as shared Fabric entity types:
  - `scripted-effect`
  - `scripted-marker`
  - `scripted-ray`
  - `scripted-block-body`

Because one Fabric entity type may back many AC entity definitions, renderer/profile routing is resolved by shared profile-key contract and kind-level fallback behavior.

## Profile key contract

For scripted entities, profile key resolution is:

1. `:profile-key` (if present)
2. `:renderer-id`
3. kind fallback default (`effect-billboard`, `marker-billboard`, `ray-composite`, `block-body`)

This precedence is implemented in shared helper:
- `cn.li.mcmod.entity.dsl/resolve-render-profile-key`

## v1 limitation

- Fabric cannot guarantee per-entry type split for all scripted entities in v1.
- Multiple AC entries under one kind may share kind-level fallback behavior when no explicit key path is available.

## Safety guarantees

- Missing profile key does **not** crash client registration.
- Scripted path can be disabled globally/per-id and falls back to native dispatch.
- Shared dispatcher scripted-first routing is applied for effect, marker, and ray renderer-id selection.
- Runtime failures are degraded to native behavior with warning logs.

## Planned v2 direction

- Optional per-entry entity-type split for scripted kinds, or
- standardized payload profile-key routing for the current content model.

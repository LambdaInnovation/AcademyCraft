# ScriptRender Performance Certification (V2)

This document defines the minimum evidence required before treating a migrated renderer-id as production-ready beyond dark-launch/hybrid mode.

## Required metrics

For each migrated renderer-id (`ray-composite`, `md-ball` hybrid, etc.) collect:

1. Frame-time delta (scripted/hybrid vs native baseline)
2. Render-thread allocation delta
3. Stability under toggle/fallback (`script-render-enabled?` + per-id disable)

## Suggested scene matrix

- Low density: 1-5 entities
- Medium density: 20-40 entities
- Stress density: 80+ entities

Run each scene in:
- scripted/hybrid enabled
- global scripted disabled
- per-renderer-id disabled

## Pass criteria template

Set project-specific thresholds before sign-off, for example:

- frame-time regression ≤ X%
- allocation regression ≤ Y KB/frame
- no crash / no persistent render-state corruption

(Keep X/Y in team release notes for each milestone.)

## Evidence package

Each certification should include:

- commit SHA
- tested platform(s): Forge/Fabric
- scene description and entity counts
- measured numbers (mean + p95)
- fallback behavior confirmation

Without this package, migration remains in dark-launch/hybrid mode.

# Top-level Mutable State Governance

This document governs Clojure top-level mutable vars such as `defonce` atoms, `atom-registry` instances and lazy singleton holders.

## Classification

- **S0 pure**: constants, protocols, records, functions and immutable data.
- **S1 process-static registry / init guard / cache**: allowed only with a clear owner, duplicate policy and test reset hook when mutation is expected.
- **S2 runtime state**: must be owned by side, server session, world, player, container, screen, effect instance or request.

No alias or re-export category exists. If a mutable top-level is not S1 or S2, remove it or move ownership to the correct runtime object.

## Owner key rule

Runtime state must declare the narrowest available owner. Use a composite owner rather than a namespace singleton when these dimensions can differ:

- `:logical-side`
- `:server-session-id` / `:client-session-id`
- `:world-id`
- `:player-uuid`
- `:container-id`
- `:screen-id`
- `:effect-instance-id`
- `:request-id`

## Audit command

```powershell
cmd /c .\gradlew.bat auditTopLevelMutableState
cmd /c .\gradlew.bat verifyMultiplayerOwnerGuards
```

The report is written to `build/reports/top-level-state/audit.md` and cross-referenced with `docs/dev/top-level-mutable-state-whitelist.tsv`.

## Whitelist format

```text
relative-path<TAB>symbol<TAB>classification<TAB>owner<TAB>notes
```

Rules:

1. Each mutable top-level var is listed by exact relative path and symbol.
2. Classification must be S1 or S2.
3. Owner must explain cleanup scope or process-static justification.
4. Do not whitelist by namespace prefix, glob or vague symbol names.

## Platform state

Platform implementation state must be installed through explicit target bootstrap and target metadata. Do not use dynamic vars as a hidden platform implementation lookup mechanism when an explicit install path is available.

## Exactly-once primitives

`cn.li.mcmod.runtime.install` provides:

- `framework-once!`
- `process-once!`
- `install-root!`

New init guards should use these primitives instead of adding ad hoc locks or boolean flags.

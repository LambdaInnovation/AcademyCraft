# Dynamic Var Binding Audit

Audit target: `mcmod`, `ac`, `platform-src/minecraft + platform-src/loader`, `platform-src/minecraft/*`, and `platform-src/loader/*` main sources.

## Rule

A `^:dynamic` var is allowed only when callers intentionally bind it at runtime. If a value is installed once during bootstrap, prefer an explicit registry, target metadata, constructor argument or service function.

## Review steps

1. Search for `^:dynamic`.
2. Check whether any caller uses `binding`.
3. If there is no runtime binding, replace the var with an explicit dependency path.
4. Re-run `verifyCurrentPlatforms` and the target compile for affected components.

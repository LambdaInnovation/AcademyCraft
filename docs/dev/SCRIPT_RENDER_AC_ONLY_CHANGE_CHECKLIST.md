# ScriptRender AC-only Change Checklist

Use this checklist when adding/changing visual behavior without touching `platform-src/minecraft/mc-1.20.1` runtime.

## Preconditions

- [ ] Target effect/marker/ray/block-body is already covered by current ScriptRender ABI kind envelope.
- [ ] Required fields validate via `mcmod.client.render.script-render-abi`.
- [ ] Profile id/key selection follows shared contract:
  1. `:profile-key`
  2. `:renderer-id`
  3. kind fallback default

## AC-only steps

- [ ] Add/update profile in AC profile namespace (for example `ac.content.render-profiles.effect-profiles`).
- [ ] If needed, update AC entity spec payload for new key/params.
- [ ] Keep semantics in AC (do not hardcode business values in runtime layer).
- [ ] Ensure defaults are present for backward-compatible behavior.

## Verification

- [ ] `:ac:compileClojure` passes.
- [ ] `:mcmod:compileClojure` passes (schema/contract compatibility).
- [ ] Platform compile checks pass for impacted loader(s).
- [ ] Scripted path disabled => native fallback still renders.

## Escalate to ABI extension when

- You need a new primitive kind.
- You need new render-state operations outside whitelist.
- You need runtime texture/shader path changes not represented in current ABI.
- You need extra per-frame context not currently exposed by runtime.

If any escalation condition is true, stop and file an ABI extension request.

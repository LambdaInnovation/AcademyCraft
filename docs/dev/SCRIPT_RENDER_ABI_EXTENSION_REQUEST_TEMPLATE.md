# ScriptRender ABI Extension Request Template

Use this when a feature cannot be delivered as AC-only profile/content changes.

## 1) Request summary

- Feature name:
- Requested by module/team:
- Target release window:

## 2) Why current ABI is insufficient

- Current kind/profile path used:
- Missing capability:
- Why workarounds are unacceptable:

## 3) Proposed ABI change

- New/changed fields:
- Validation rules (range/type/default):
- Backward compatibility behavior for v1/v2 profiles:
- Failure behavior (must degrade safely to native path):

## 4) Runtime impact (`platform-src/minecraft/mc-1.20.1`)

- Compiler changes:
- Runtime/executor changes:
- Dispatcher/platform bridge changes:
- Expected hot-path overhead (qualitative + estimated bounds):

## 5) Platform parity

- Forge semantics:
- Fabric semantics:
- Cross-platform key-resolution impact:

## 6) Risks and rollback

- Primary technical risks:
- Fallback/kill-switch strategy:
- Rollback plan (how to disable quickly):

## 7) Verification plan

- Compile/boundary gates:
- Fallback fault-injection tests:
- Performance scenes + pass/fail thresholds:
- Owner for benchmark result sign-off:

## 8) Decision record

- Decision:
- Approvers:
- Date:
- Follow-up tasks:

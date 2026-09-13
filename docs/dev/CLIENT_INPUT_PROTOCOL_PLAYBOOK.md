# Client Input Protocol Playbook

## Purpose

This document is the maintenance handbook for the client-side input contract used by AcademyCraft (AC) across platform layers, the universal keyboard-input protocol, and the AC business layer.

It is intended for:
- AC feature developers,
- platform maintainers,
- new Loader contributors,
- reviewers validating input changes.

The goal is to keep the responsibility split explicit:
- platform code decides how a raw key gesture is interpreted,
- the input protocol dispatches the resulting event,
- AC business logic consumes the already-filtered event and performs the action.

## Core Rule

For gesture timing such as short press vs. long hold, the decision belongs to the platform/input-protocol boundary, not to the AC business layer.

In practice:
- platform polling / Forge event handlers decide whether an input is a short press, a long hold, or a regular press/release transition;
- the universal keyboard-input protocol dispatches the filtered event;
- AC handlers such as keybinds and input IDs only execute the business action.

## Input Chain

1. Platform input source
   - GLFW polling for hardcoded keys such as V, C, F4, Left Alt.
   - Forge key events for KeyMapping-based inputs.

2. Timing gate
   - For V-key behavior, the platform layer tracks the key state across press/release.
   - Only a short release emits the toggle input.
   - A long hold is suppressed so the AC layer never receives a toggle request for that gesture.

3. Universal protocol dispatch
   - The protocol layer receives the already-decided event and forwards it to the registered AC handler.
   - This boundary also preserves client-context values such as player UUID and client session ID.

4. AC business execution
   - AC handlers translate the input into business actions such as toggling activation, switching preset, opening a screen, or aborting active contexts.

## Responsibility Map

| Layer | Responsibility |
|---|---|
| Platform (GLFW / Forge) | Observe raw keyboard state and make timing decisions |
| mcmod protocol | Dispatch the filtered input event to the registered handler |
| AC keybinds / input IDs | Execute business logic for the received event |

## Concrete Example: V Key

### Intended behavior
- Short V press: toggle primary state.
- Held/active V: abort current action or keep the current state depending on the active delegate flow.
- Long V hold: do not emit a toggle request.

### Where the decision happens
- The decision is made in the platform-side input transition logic, currently represented by the shared button-state helper in the AC input-state-machine namespace and the platform glue in GLFW polling / Forge key handling.

### What AC sees
- AC only sees a filtered input event such as `:content/toggle-primary-state` when the timing gate has already accepted the gesture.

## Implementation Notes

### Platform side
- Keep timing policy in the platform/input boundary.
- Preserve raw key state long enough to distinguish short release from long hold.
- Avoid emitting toggle semantics from the AC business layer.

### Protocol side
- Keep the protocol thin: dispatch the event and bind client context.
- Do not implement gameplay-specific timing rules here.

### AC business side
- Keep handlers narrow and semantic.
- `trigger-mode-switch!` and related handlers should respond to the already-filtered input, not re-interpret raw key timing.

## Guidance for New Inputs

When introducing a new input with timing semantics:
1. Decide whether the input should be treated as a short press, press, release, or long hold.
2. Implement the timing decision in the platform/input boundary.
3. Emit a filtered event only when the gesture is accepted.
4. Keep AC handlers focused on business behavior.

## Platform Integration Rules

Any new platform integration must follow these rules:

### Required contract
- The platform layer must provide a stable input context containing at least:
  - `:player-uuid`
  - `:client-session-id`
  - `:logical-side`
- The platform layer must emit the universal input event through `cn.li.mcmod.protocol.keyboard-input`.

### Timing rule
- If the input has gesture semantics such as short press / long hold, the timing decision must be taken before dispatch.
- Long-hold gestures must not bubble into AC as a toggle-style input unless the platform explicitly decides that the input should be accepted.

### Suppression rule
- If a screen or GUI is open, the platform must suppress gameplay input that would otherwise leak into business logic.
- The suppression behavior must be consistent with the existing screen-open semantics.

## New Loader / New Platform Checklist

Use this checklist when adding a new Loader or adapting a new platform target.

### 1. Input source wiring
- [ ] Identify whether the platform provides raw polling, KeyMapping events, or both.
- [ ] Decide which inputs are platform-fixed and which are configurable.
- [ ] Ensure the platform layer can emit the universal input protocol events.

### 2. Context propagation
- [ ] Ensure player UUID and client session ID are available in the event context.
- [ ] Ensure the protocol dispatch preserves the same context shape across loaders.

### 3. Timing semantics
- [ ] Implement short-press / long-hold policy in the platform boundary.
- [ ] Verify that long-hold gestures do not accidentally trigger toggle semantics.
- [ ] Confirm that the AC business layer sees only the filtered event.

### 4. Regression coverage
- [ ] Add or update focused tests for the platform-side timing decision.
- [ ] Add or update AC-side regression tests for the business behavior.
- [ ] Verify the relevant Gradle test task still passes.

### 5. Review checklist
- [ ] No gameplay timing logic lives in AC handlers.
- [ ] No platform-specific business logic leaks into the protocol layer.
- [ ] The new loader follows the same input context contract as existing platforms.

## Reviewer Acceptance Criteria

A change is ready for merge when all of the following are true:
- the event chain from platform → protocol → AC is still intact,
- timing decisions happen in the correct layer,
- the relevant regression tests pass,
- any new Loader integration follows the same contract as existing platforms.

## Related Modules

- AC keybinds: `cn.li.ac.ability.client.keybinds`
- AC input state machine: `cn.li.ac.ability.client.input-state-machine`
- AC input IDs: `cn.li.ac.input-ids`
- Universal keyboard protocol: `cn.li.mcmod.protocol.keyboard-input`
- Platform GLFW polling: `cn.li.mc1201.glfw-polling-core`
- Forge key handling: `cn.li.forge1201.client.keyboard-event-handler`

# Ability Core Spec

本文是当前能力系统的核心规范。

## Goals

- 能力定义在 `ac`，平台事件 glue 在 selected `:platform` target。
- 状态写入只经过 reducer pipeline。
- 网络、按键、客户端视觉效果通过明确 hook 接入，不把 Loader API 带入 `ac`。

## Runtime flow

```text
input / event / packet
  -> platform glue
  -> mcmod contract
  -> ac command-runtime
  -> reducer
  -> runtime-store
  -> effects.interpreter
```

## State

- Player ability state lives in runtime store.
- Context lifecycle is explicit: activate, keepalive, abort, clear by owner/session.
- Skill code reads context through service APIs and returns commands/effects; it does not mutate transport snapshots directly.

## Wire contracts

- Message ids and payload shapes are part of the multiplayer contract.
- Breaking wire changes require a deliberate version boundary.
- Internal namespace layout may change as long as public gameplay and wire contracts stay explicit.

## Tests

Ability changes should include platform-neutral tests in `ac` where possible. Platform compile is required when key/event/network glue changes.

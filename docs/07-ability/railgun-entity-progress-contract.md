# Railgun Entity Progress Contract

This document freezes the railgun behavior contract for the entity-progress migration.

## Scope

- Skill: railgun
- Source of QTE timing: nearby entity_coin_throwing progress from runtime entity query
- Non-goal: old runtime coin-window timestamp state as primary source

## Progress Contract

- Progress range is normalized to [0.0, 1.0].
- Active threshold: 0.6 (configured by qte.coin-active-threshold).
- Perform threshold: 0.7 (configured by qte.coin-perform-threshold).
- If no valid coin entity is found, QTE window is absent.
- Coin perform path discards the coin entity before main shot resolve.

## Input Semantics

- Key down:
  - If progress >= perform threshold, perform immediately.
  - Else if coin exists but progress below perform threshold, mark coin-qte-miss and do not fire.
  - Else fallback to item-charge when accepted iron item is in hand.
- Key tick:
  - Only drives item-charge countdown and auto-fire.
- Key up and abort:
  - Only cancel unfinished item-charge.

## Item Action Pipeline

- Coin item plan order is fixed:
  1. consume-item
  2. domain-action (railgun-coin-throw)
  3. spawn-scripted-effect (entity_coin_throwing)

## Side Effects

- Successful perform must trigger beam FX topic and railgun sound.
- Reflect path must consume vec-reflection CP before reflected shot.
- Cooldown is written only on successful perform.
- Exp gain is based on hit/reflection-hit semantic path.

## Visual State Contract

- charge coin visual-state returns:
  - active?
  - coin-active?
  - charge-ratio
  - charge-ticks
  - coin-progress

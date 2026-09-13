# Groundshock Verification Notes

Groundshock now follows the stricter Groundshock plan: no fixed position fallback, configurable horizontal look fallback, living-only entity hits, and a single coarse entity query per propagation step through the world-effects AABB protocol path.

## Behavior changes

- Position lookup comes only from teleportation runtime state.
- Horizontal look can fall back to `+Z` only when the internal toggle is enabled.
- Entity damage, velocity, and EXP gain apply only to living entities.
- Propagation uses a coarse AABB query once per iteration and then filters by the shock box for each delta.

## Verification

- `.\gradlew.bat :ac:runAcClojureTests "-Dac.test.only=cn.li.ac.content.ability.vecmanip.groundshock-test,cn.li.ac.content.ability.vecmanip.groundshock-fx-test"`
- `.\gradlew.bat :ac:runAcClojureTests "-Dac.test.only=cn.li.ac.content.ability.vecmanip.*"`
- `.\gradlew.bat :ac:compileClojure`
- `.\gradlew.bat verifyArchitectureBoundaries`
- `.\gradlew.bat verifyLocalPrGate`
- `.\gradlew.bat runForgeGameTests validateForgeGameTestLog`

## Notes

- The Forge smoke suite now includes a Groundshock registry/callability smoke in the shared `ac_smoke` batch.
- The behavior-level Groundshock tests live in `ac/src/test/clojure/cn/li/ac/content/ability/vecmanip/groundshock_test.clj`.

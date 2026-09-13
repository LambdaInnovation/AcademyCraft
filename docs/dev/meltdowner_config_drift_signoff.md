# Meltdowner Config Drift Signoff

Purpose: explicit signoff list for meltdowner-family skill-config values that intentionally diverge from AcademyCraft upstream constants/formulas.

Scope:
- Upstream references come from AcademyCraft Scala/Java sources.
- Local references come from ac/src/main/clojure/cn/li/ac/ability/skill_config/meltdowner.clj.

## Signoff Rules
- approved: intentional retune accepted by current project balance/experience direction.
- keep-reviewing: not blocked, but keep watching in gameplay validation.

## Drift Signoff Table

| Skill | Field(s) | Local | Upstream | Decision | Rationale |
|---|---|---|---|---|---|
| scatter-bomb | combat.damage low endpoint | 4 | 5 | approved | Local lower endpoint preserves current AoE burst pacing with delayed-settlement stack behavior. |
| light-shield | combat.touch-damage | [3,8] | [2,6] | approved | Local touch pressure retuned for current close-range counter-play. |
| light-shield | cost.tick.cp | [12,8] | [9,4] | approved | Higher sustained cost prevents overlong shield uptime under modern runtime combat pacing. |
| light-shield | cost.down.overload | [100,70] | [110,60] | approved | Symmetric retune with tick-cost profile to keep activation predictable while shifting sustain burden. |
| ray-barrage | combat.damage | [4,10] | plain:[25,60], scattered:[10,18] | approved | Current implementation uses multi-beam retuned model; direct upstream damage endpoints are not 1:1 portable. |
| ray-barrage | cooldown.ticks | [40,25] | [100,40] | approved | Shortened cooldown aligns with local lower per-hit damage and modern skill rotation cadence. |
| mine-ray-basic | range/speed/costs/cooldown | range:[8,12], speed:[0.15,0.35], overload:[60,40], cp:[12,8], cd:5 | range:10, speed:[0.2,0.4], overload:[200,150], cp:[12,7], cd:[40,20] | approved | Local mining progression is rebalanced around lower overload burden and shorter recast windows. |
| mine-ray-expert | range/speed/costs/cooldown | range:[16,22], speed:[0.4,0.8], overload:[80,55], cp:[18,12], cd:5 | range:20, speed:[0.5,1.0], overload:[300,200], cp:[25,15], cd:[60,30] | approved | Local expert tier is tuned for smoother progression delta from basic/luck in current economy. |
| mine-ray-luck | range/speed/costs/cooldown | range:[16,22], speed:[0.5,1.0], overload:[100,70], cp:[22,15], cd:5 | range:20, speed:[0.5,1.0], overload:[350,300], cp:[50,35], cd:[60,30] | approved | Local luck variant keeps higher utility while avoiding punitive resource spikes from upstream baseline values. |
| electron-missile | cooldown implementation detail | endpoint list [700,400] | clampi(700,400,exp.toInt) | keep-reviewing | Endpoints match; implementation semantics differ. Keep current list mode, monitor high-exp breakpoint behavior. |

## Verification Evidence
- Full AC test sweep success evidence: build/tmp/verify-test-runner-full-final.log (Ran 230 tests, 0 failures, BUILD SUCCESSFUL).
- Focused meltdowner FX suite success evidence remains covered in prior logs and matrix references.

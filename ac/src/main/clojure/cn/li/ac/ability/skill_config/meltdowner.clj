(ns cn.li.ac.ability.skill-config.meltdowner
  "Meltdowner skill action tunables for player-facing skill config.")

(def skill-tunable-definitions
  [{:skill-id :rad-intensify :id :combat.damage-rate :path "combat.damage-rate" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [1.4 1.8] :comment "RadIntensify damage amplification rate endpoints."}
   {:skill-id :rad-intensify :id :effect.mark-duration-ticks :path "effect.mark-duration-ticks" :section-suffix "effect" :type :int :min 1 :default 60 :comment "RadIntensify target mark duration in ticks."}

   {:skill-id :electron-bomb :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [6.0 12.0] :comment "ElectronBomb beam damage endpoints."}
   {:skill-id :electron-bomb :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [20.0 10.0] :comment "ElectronBomb cooldown ticks."}
   {:skill-id :electron-bomb :id :progression.exp-hit :path "progression.exp-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "ElectronBomb exp gained per cast (unconditional — matches original's ctx.addSkillExp(.005f) firing regardless of whether the delayed ray hits)."}
   {:skill-id :electron-bomb :id :charge.settle-ticks :path "charge.settle-ticks" :section-suffix "charge" :type :int :min 1 :default 20 :comment "ElectronBomb ball settle time (ticks) before the ray fires, below the improved-exp threshold."}
   {:skill-id :electron-bomb :id :charge.settle-ticks-improved :path "charge.settle-ticks-improved" :section-suffix "charge" :type :int :min 1 :default 5 :comment "ElectronBomb ball settle time (ticks) once skill exp exceeds charge.improved-exp-threshold."}
   {:skill-id :electron-bomb :id :charge.improved-exp-threshold :path "charge.improved-exp-threshold" :section-suffix "charge" :type :double :min 0.0 :max 1.0 :default 0.8 :comment "ElectronBomb skill exp above which the ball settles faster (charge.settle-ticks-improved)."}

   {:skill-id :electron-missile :id :targeting.seek-range :path "targeting.seek-range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [5.0 13.0] :comment "ElectronMissile target seek range."}
   {:skill-id :electron-missile :id :charge.max-hold-ticks :path "charge.max-hold-ticks" :section-suffix "charge" :type :double-list :min 1.0 :list-count 2 :default [80.0 200.0] :comment "ElectronMissile max hold ticks."}
  {:skill-id :electron-missile :id :projectile.max-hold-balls :path "projectile.max-hold-balls" :section-suffix "projectile" :type :int :min 1 :default 5 :comment "ElectronMissile max active stored balls."}
  {:skill-id :electron-missile :id :timing.spawn-interval-ticks :path "timing.spawn-interval-ticks" :section-suffix "timing" :type :int :min 1 :default 10 :comment "ElectronMissile ticks between ball spawns."}
   {:skill-id :electron-missile :id :timing.fire-interval-ticks :path "timing.fire-interval-ticks" :section-suffix "timing" :type :int :min 1 :default 8 :comment "ElectronMissile ticks between ball launches."}
   {:skill-id :electron-missile :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [10.0 18.0] :comment "ElectronMissile damage per ball."}
   {:skill-id :electron-missile :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double :min 0.0 :default 200.0 :comment "ElectronMissile activation overload floor/cost."}
  {:skill-id :electron-missile :id :cost.attack.cp :path "cost.attack.cp" :section-suffix "cost.attack" :type :double-list :min 0.0 :list-count 2 :default [60.0 25.0] :comment "ElectronMissile extra CP cost per fired ball."}
  {:skill-id :electron-missile :id :cost.attack.overload :path "cost.attack.overload" :section-suffix "cost.attack" :type :double-list :min 0.0 :list-count 2 :default [9.0 4.0] :comment "ElectronMissile extra overload cost per fired ball."}
   {:skill-id :electron-missile :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [12.0 5.0] :comment "ElectronMissile channel CP cost."}
   {:skill-id :electron-missile :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :int :min 0 :default 700 :comment "ElectronMissile cooldown. Original clampi(700,400,exp.toInt) always evaluates to 700."}
  {:skill-id :electron-missile :id :progression.exp-hit :path "progression.exp-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.001 :comment "ElectronMissile exp gained per hit."}

  {:skill-id :jet-engine :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [7.0 20.0] :comment "JetEngine trigger-hit damage endpoints."}
  {:skill-id :jet-engine :id :cost.hold.required-cp :path "cost.hold.required-cp" :section-suffix "cost.hold" :type :double-list :min 0.0 :list-count 2 :default [170.0 140.0] :comment "JetEngine CP required to keep marking (original consumption variable)."}
  {:skill-id :jet-engine :id :cost.release.cp :path "cost.release.cp" :section-suffix "cost.release" :type :double-list :min 0.0 :list-count 2 :default [60.0 50.0] :comment "JetEngine release CP cost (original overload variable passed as consume's CP argument)."}
  {:skill-id :jet-engine :id :cost.release.overload :path "cost.release.overload" :section-suffix "cost.release" :type :double-list :min 0.0 :list-count 2 :default [170.0 140.0] :comment "JetEngine release overload cost (original consumption variable passed as consume's overload argument)."}
  {:skill-id :jet-engine :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [60.0 30.0] :comment "JetEngine cooldown ticks after successful release."}
  {:skill-id :jet-engine :id :progression.exp-use :path "progression.exp-use" :section-suffix "progression" :type :double :min 0.0 :default 0.004 :comment "JetEngine exp gained on successful release."}

   {:skill-id :light-shield :id :combat.touch-damage :path "combat.touch-damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [2.0 6.0] :comment "LightShield contact damage."}
   {:skill-id :light-shield :id :combat.touch-radius :path "combat.touch-radius" :section-suffix "combat" :type :double :min 0.0 :default 3.0 :comment "LightShield touch damage radius."}
  {:skill-id :light-shield :id :combat.absorb-damage :path "combat.absorb-damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [15.0 50.0] :comment "LightShield max absorbed damage per hit."}
  {:skill-id :light-shield :id :combat.absorb-interval-ticks :path "combat.absorb-interval-ticks" :section-suffix "combat" :type :int :min 1 :default 18 :comment "LightShield absorb window interval ticks."}
   {:skill-id :light-shield :id :combat.front-cone-degrees :path "combat.front-cone-degrees" :section-suffix "combat" :type :double :min 0.0 :max 180.0 :default 60.0 :comment "LightShield front-cone half-angle in degrees (horizontal yaw only, matching original)."}
  {:skill-id :light-shield :id :timing.max-active-ticks :path "timing.max-active-ticks" :section-suffix "timing" :type :double-list :min 1.0 :list-count 2 :default [120.0 180.0] :comment "LightShield max active duration ticks."}
   {:skill-id :light-shield :id :effect.slowness-duration-ticks :path "effect.slowness-duration-ticks" :section-suffix "effect" :type :int :min 0 :default 100 :comment "LightShield slowness duration on deactivate or abort (unified, matching original's single s_onEnd)."}
   {:skill-id :light-shield :id :effect.slowness-amplifier :path "effect.slowness-amplifier" :section-suffix "effect" :type :int :min 0 :default 1 :comment "LightShield slowness amplifier."}
   {:skill-id :light-shield :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [110.0 60.0] :comment "LightShield activation overload cost."}
   {:skill-id :light-shield :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [9.0 4.0] :comment "LightShield tick CP cost."}
  {:skill-id :light-shield :id :cost.absorb.cp :path "cost.absorb.cp" :section-suffix "cost.absorb" :type :double-list :min 0.0 :list-count 2 :default [50.0 30.0] :comment "LightShield contact CP cost; original defensive absorption accidentally uses this value as overload."}
  {:skill-id :light-shield :id :cost.absorb.overload :path "cost.absorb.overload" :section-suffix "cost.absorb" :type :double-list :min 0.0 :list-count 2 :default [5.0 3.0] :comment "LightShield contact overload cost; original defensive absorption accidentally uses this value as CP."}
  {:skill-id :light-shield :id :progression.exp-tick :path "progression.exp-tick" :section-suffix "progression" :type :double :min 0.0 :default 0.000001 :comment "LightShield exp gained per active server tick."}
  {:skill-id :light-shield :id :progression.exp-touch :path "progression.exp-touch" :section-suffix "progression" :type :double :min 0.0 :default 0.001 :comment "LightShield exp gained per successful contact attack."}
  {:skill-id :light-shield :id :progression.exp-attacked :path "progression.exp-attacked" :section-suffix "progression" :type :double :min 0.0 :default 0.001 :comment "LightShield exp gained per eligible incoming-damage event."}

   {:skill-id :meltdowner :id :charge.min-ticks :path "charge.min-ticks" :section-suffix "charge" :type :int :min 1 :default 20 :comment "Meltdowner minimum charge ticks."}
   {:skill-id :meltdowner :id :charge.max-ticks :path "charge.max-ticks" :section-suffix "charge" :type :int :min 1 :default 40 :comment "Meltdowner optimal max charge ticks."}
   {:skill-id :meltdowner :id :charge.max-tolerant-ticks :path "charge.max-tolerant-ticks" :section-suffix "charge" :type :int :min 1 :default 100 :comment "Meltdowner tolerant max hold ticks."}
   {:skill-id :meltdowner :id :charge.time-rate :path "charge.time-rate" :section-suffix "charge" :type :double-list :min 0.0 :list-count 2 :default [0.8 1.2] :comment "Meltdowner time-rate endpoints between min and max charge."}
   {:skill-id :meltdowner :id :beam.radius :path "beam.radius" :section-suffix "beam" :type :double-list :min 0.0 :list-count 2 :default [2.0 3.0] :comment "Meltdowner beam radius endpoints."}
   {:skill-id :meltdowner :id :beam.query-radius :path "beam.query-radius" :section-suffix "beam" :type :double :min 0.0 :default 55.0 :comment "Meltdowner beam query radius (must cover beam.max-distance, matching original's maxIncrement=50 reach)."}
   {:skill-id :meltdowner :id :beam.step :path "beam.step" :section-suffix "beam" :type :double :min 0.001 :default 0.9 :comment "Meltdowner beam trace step."}
   {:skill-id :meltdowner :id :beam.max-distance :path "beam.max-distance" :section-suffix "beam" :type :double :min 0.0 :default 50.0 :comment "Meltdowner beam max distance."}
   {:skill-id :meltdowner :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [18.0 50.0] :comment "Meltdowner beam damage endpoints."}
   {:skill-id :meltdowner :id :beam.block-energy :path "beam.block-energy" :section-suffix "beam" :type :double-list :min 0.0 :list-count 2 :default [300.0 700.0] :comment "Meltdowner block break energy endpoints."}
   {:skill-id :meltdowner :id :reflection.shot-distance :path "reflection.shot-distance" :section-suffix "reflection" :type :double :min 0.0 :default 10.0 :comment "Meltdowner reflected shot distance."}
   {:skill-id :meltdowner :id :reflection.damage-multiplier :path "reflection.damage-multiplier" :section-suffix "reflection" :type :double :min 0.0 :default 0.5 :comment "Meltdowner reflected shot damage multiplier."}
   {:skill-id :meltdowner :id :reflection.base-damage :path "reflection.base-damage" :section-suffix "reflection" :type :double-list :min 0.0 :list-count 2 :default [20.0 50.0] :comment "Meltdowner reflected shot base damage endpoints (before damage-multiplier), matching original's lerpf(20,50,exp)."}
   {:skill-id :meltdowner :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [200.0 170.0] :comment "Meltdowner down-stage overload cost."}
   {:skill-id :meltdowner :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [10.0 15.0] :comment "Meltdowner tick CP cost."}
   {:skill-id :meltdowner :id :cooldown.base-multiplier :path "cooldown.base-multiplier" :section-suffix "cooldown" :type :double :min 0.0 :default 20.0 :comment "Meltdowner cooldown base multiplier."}
   {:skill-id :meltdowner :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [15.0 7.0] :comment "Meltdowner cooldown endpoints multiplied by time-rate/base."}
   {:skill-id :meltdowner :id :progression.exp-use :path "progression.exp-use" :section-suffix "progression" :type :double :min 0.0 :default 0.002 :comment "Meltdowner exp gained per performed beam scaled by time-rate."}

   {:skill-id :mine-ray-basic :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 10.0 :comment "MineRayBasic range (flat, matching original — not exp-scaled)."}
   {:skill-id :mine-ray-basic :id :mining.break-speed :path "mining.break-speed" :section-suffix "mining" :type :double-list :min 0.0 :list-count 2 :default [0.2 0.4] :comment "MineRayBasic break speed."}
   {:skill-id :mine-ray-basic :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [200.0 150.0] :comment "MineRayBasic down-stage overload cost."}
   {:skill-id :mine-ray-basic :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [12.0 7.0] :comment "MineRayBasic tick CP cost."}
   {:skill-id :mine-ray-basic :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [40.0 20.0] :comment "MineRayBasic cooldown ticks."}
   {:skill-id :mine-ray-basic :id :progression.exp-block :path "progression.exp-block" :section-suffix "progression" :type :double :min 0.0 :default 0.0005 :comment "MineRayBasic exp per block broken."}

   {:skill-id :mine-ray-expert :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 20.0 :comment "MineRayExpert range (flat, matching original — not exp-scaled)."}
   {:skill-id :mine-ray-expert :id :mining.break-speed :path "mining.break-speed" :section-suffix "mining" :type :double-list :min 0.0 :list-count 2 :default [0.5 1.0] :comment "MineRayExpert break speed."}
   {:skill-id :mine-ray-expert :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [300.0 200.0] :comment "MineRayExpert down-stage overload cost."}
   {:skill-id :mine-ray-expert :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [25.0 15.0] :comment "MineRayExpert tick CP cost."}
   {:skill-id :mine-ray-expert :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [60.0 30.0] :comment "MineRayExpert cooldown ticks."}
   {:skill-id :mine-ray-expert :id :progression.exp-block :path "progression.exp-block" :section-suffix "progression" :type :double :min 0.0 :default 0.0003 :comment "MineRayExpert exp per block broken."}

   {:skill-id :mine-ray-luck :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 20.0 :comment "MineRayLuck range (flat, matching original — not exp-scaled)."}
   {:skill-id :mine-ray-luck :id :mining.break-speed :path "mining.break-speed" :section-suffix "mining" :type :double-list :min 0.0 :list-count 2 :default [0.5 1.0] :comment "MineRayLuck break speed."}
   {:skill-id :mine-ray-luck :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [350.0 300.0] :comment "MineRayLuck down-stage overload cost."}
   {:skill-id :mine-ray-luck :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [50.0 35.0] :comment "MineRayLuck tick CP cost."}
   {:skill-id :mine-ray-luck :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [60.0 30.0] :comment "MineRayLuck cooldown ticks."}
   {:skill-id :mine-ray-luck :id :progression.exp-block :path "progression.exp-block" :section-suffix "progression" :type :double :min 0.0 :default 0.0003 :comment "MineRayLuck exp per block broken."}

  {:skill-id :ray-barrage :id :combat.damage.plain :path "combat.damage.plain" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [25.0 60.0] :comment "RayBarrage plain/direct branch damage endpoints."}
  {:skill-id :ray-barrage :id :combat.damage.scattered :path "combat.damage.scattered" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [10.0 18.0] :comment "RayBarrage scattered branch (cone AOE) damage endpoints, per entity hit."}
  {:skill-id :ray-barrage :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 20.0 :comment "RayBarrage forward detect range / plain-branch hitscan range / scattered-branch cone reach (all RAY_DIST=20 in original)."}
  {:skill-id :ray-barrage :id :scatter.cone-angle-degrees :path "scatter.cone-angle-degrees" :section-suffix "scatter" :type :double :min 0.0 :max 360.0 :default 55.0 :comment "RayBarrage scattered-branch cone angle: yaw spans this value total (half each side), pitch spans this value on EACH side (double total) — matches original's single `range=55f` reused both ways."}
   {:skill-id :ray-barrage :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [450.0 380.0] :comment "RayBarrage down-stage CP cost."}
   {:skill-id :ray-barrage :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [300.0 140.0] :comment "RayBarrage down-stage overload cost."}
  {:skill-id :ray-barrage :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [100.0 40.0] :comment "RayBarrage cooldown ticks."}
   {:skill-id :ray-barrage :id :progression.exp-hit :path "progression.exp-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "RayBarrage exp gained on every successful execution (unconditional — matches original's ctx.addSkillExp(.005f) firing regardless of whether anything was hit)."}

   {:skill-id :scatter-bomb :id :projectile.max-balls :path "projectile.max-balls" :section-suffix "projectile" :type :int :min 1 :default 7 :comment "ScatterBomb maximum accumulated balls (ticks 20,30,...,80 step 10 = 7 spawns)."}
  {:skill-id :scatter-bomb :id :projectile.max-hold-ticks :path "projectile.max-hold-ticks" :section-suffix "projectile" :type :int :min 1 :default 80 :comment "ScatterBomb hold window tick cap for ball accumulation."}
   {:skill-id :scatter-bomb :id :projectile.spawn-interval-ticks :path "projectile.spawn-interval-ticks" :section-suffix "projectile" :type :int :min 1 :default 10 :comment "ScatterBomb ball spawn interval."}
   {:skill-id :scatter-bomb :id :projectile.spawn-start-tick :path "projectile.spawn-start-tick" :section-suffix "projectile" :type :int :min 0 :default 20 :comment "ScatterBomb first ball spawn tick."}
   {:skill-id :scatter-bomb :id :projectile.scatter-range :path "projectile.scatter-range" :section-suffix "projectile" :type :double :min 0.0 :default 15.0 :comment "ScatterBomb per-ball destination range (RAY_RANGE), used twice: base point along exact look, plus a randomly pitch/yaw-rotated offset."}
   {:skill-id :scatter-bomb :id :projectile.scatter-angle-degrees :path "projectile.scatter-angle-degrees" :section-suffix "projectile" :type :double :min 0.0 :default 25.0 :comment "ScatterBomb per-ball random pitch/yaw rotation span (each axis independently randomized within +/- half this value)."}
   {:skill-id :scatter-bomb :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [5.0 9.0] :comment "ScatterBomb per-ball damage."}
   {:skill-id :scatter-bomb :id :targeting.auto-aim-exp-threshold :path "targeting.auto-aim-exp-threshold" :section-suffix "targeting" :type :double :min 0.0 :max 1.0 :default 0.5 :comment "ScatterBomb: above this skill exp, some balls auto-aim at nearby living entities."}
   {:skill-id :scatter-bomb :id :targeting.auto-aim-radius :path "targeting.auto-aim-radius" :section-suffix "targeting" :type :double :min 0.0 :default 5.0 :comment "ScatterBomb auto-aim target search radius around the player."}
   {:skill-id :scatter-bomb :id :effect.anti-afk-tick :path "effect.anti-afk-tick" :section-suffix "effect" :type :int :min 1 :default 200 :comment "ScatterBomb anti-AFK self-damage tick."}
   {:skill-id :scatter-bomb :id :effect.anti-afk-damage :path "effect.anti-afk-damage" :section-suffix "effect" :type :double :min 0.0 :default 6.0 :comment "ScatterBomb anti-AFK self-damage amount."}
   {:skill-id :scatter-bomb :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [80.0 60.0] :comment "ScatterBomb down-stage overload cost."}
   {:skill-id :scatter-bomb :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [3.0 6.0] :comment "ScatterBomb tick CP cost (increases with exp in original)."}
   {:skill-id :scatter-bomb :id :progression.exp-per-ball :path "progression.exp-per-ball" :section-suffix "progression" :type :double :min 0.0 :default 0.001 :comment "ScatterBomb exp gained per fired ball."}])

(def internal-tunable-definitions
  [{:skill-id :meltdowner :id :beam.visual-distance :path "beam.visual-distance" :section-suffix "beam" :type :double :min 0.0 :default 30.0 :comment "Internal FX beam visual distance (original MDContext.s_perform: length = 30, shortened only by a reflector)."}])

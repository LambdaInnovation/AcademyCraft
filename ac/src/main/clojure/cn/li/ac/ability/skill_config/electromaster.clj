(ns cn.li.ac.ability.skill-config.electromaster
  "Electromaster skill action tunables for player-facing skill config.")

(def skill-tunable-definitions
  [{:skill-id :railgun :id :qte.coin-window-ms :path "qte.coin-window-ms" :section-suffix "qte" :type :int :min 1 :default 1600 :comment "Railgun coin QTE window duration in milliseconds. Must cover the coin's full flight (~1530 ms at init-vel 0.92 / gravity 0.06); the perform phase (flight progress > 0.7) starts at ~1250 ms."}
   {:skill-id :railgun :id :qte.coin-active-threshold :path "qte.coin-active-threshold" :section-suffix "qte" :type :double :min 0.0 :max 1.0 :default 0.6 :comment "Railgun coin QTE progress threshold at which the window becomes active."}
   {:skill-id :railgun :id :qte.coin-perform-threshold :path "qte.coin-perform-threshold" :section-suffix "qte" :type :double :min 0.0 :max 1.0 :default 0.7 :comment "Railgun coin QTE progress value that must be strictly exceeded to fire immediately."}
   {:skill-id :railgun :id :charge.item-charge-ticks :path "charge.item-charge-ticks" :section-suffix "charge" :type :int :min 1 :default 20 :comment "Ticks required for the iron-item charge fallback path."}
   {:skill-id :railgun :id :beam.radius :path "beam.radius" :section-suffix "beam" :type :double :min 0.0 :default 2.0 :comment "Railgun beam collision radius."}
   {:skill-id :railgun :id :beam.query-radius :path "beam.query-radius" :section-suffix "beam" :type :double :min 0.0 :default 50.0 :comment "Entity query radius used by the railgun beam operation. Must be >= beam.max-distance or straight-line-distant entities on-axis are never fetched, silently truncating the beam's effective range."}
   {:skill-id :railgun :id :beam.step :path "beam.step" :section-suffix "beam" :type :double :min 0.001 :default 0.9 :comment "Step size used while tracing the railgun beam."}
   {:skill-id :railgun :id :beam.max-distance :path "beam.max-distance" :section-suffix "beam" :type :double :min 0.0 :default 50.0 :comment "Maximum railgun beam hit distance."}
   {:skill-id :railgun :id :beam.damage :path "beam.damage" :section-suffix "beam" :type :double-list :min 0.0 :list-count 2 :default [60.0 110.0] :comment "Railgun beam damage lerp endpoints for skill exp 0.0 and 1.0."}
   {:skill-id :railgun :id :beam.block-energy :path "beam.block-energy" :section-suffix "beam" :type :double-list :min 0.0 :list-count 2 :default [900.0 2000.0] :comment "Railgun block-breaking energy lerp endpoints for skill exp 0.0 and 1.0."}
   {:skill-id :railgun :id :reflection.distance :path "reflection.distance" :section-suffix "reflection" :type :double :min 0.0 :default 15.0 :comment "Maximum distance of the secondary vec-reflection railgun shot."}
   {:skill-id :railgun :id :reflection.damage :path "reflection.damage" :section-suffix "reflection" :type :double :min 0.0 :default 14.0 :comment "Damage dealt by the secondary vec-reflection railgun shot."}
   {:skill-id :railgun :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [200.0 450.0] :comment "Railgun coin-QTE down-stage CP cost lerp endpoints."}
   {:skill-id :railgun :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [180.0 120.0] :comment "Railgun coin-QTE down-stage overload cost lerp endpoints."}
   {:skill-id :railgun :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [200.0 450.0] :comment "Railgun item-charge tick-stage CP cost lerp endpoints."}
   {:skill-id :railgun :id :cost.tick.overload :path "cost.tick.overload" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [180.0 120.0] :comment "Railgun item-charge tick-stage overload cost lerp endpoints."}
   {:skill-id :railgun :id :cooldown.manual-ticks :path "cooldown.manual-ticks" :section-suffix "cooldown" :type :double-list :min 1.0 :list-count 2 :default [300.0 160.0] :comment "Railgun manual cooldown lerp endpoints applied after a successful shot."}
   {:skill-id :railgun :id :progression.exp-hit :path "progression.exp-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "Railgun skill exp gained unless the reflected secondary shot hits an entity."}
   {:skill-id :railgun :id :progression.exp-reflection-hit :path "progression.exp-reflection-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.01 :comment "Railgun skill exp gained only when the reflected secondary shot hits an entity."}

   {:skill-id :thunder-clap :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 40.0 :comment "ThunderClap aim raycast range."}
   {:skill-id :thunder-clap :id :charge.min-ticks :path "charge.min-ticks" :section-suffix "charge" :type :int :min 1 :default 40 :comment "Minimum held ticks required for ThunderClap to perform."}
   {:skill-id :thunder-clap :id :charge.max-ticks :path "charge.max-ticks" :section-suffix "charge" :type :int :min 1 :default 60 :comment "Maximum ThunderClap charge ticks before the strike auto-releases."}
   {:skill-id :thunder-clap :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [390.0 252.0] :comment "ThunderClap down-stage overload cost lerp endpoints."}
   {:skill-id :thunder-clap :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [18.0 25.0] :comment "ThunderClap tick-stage CP cost lerp endpoints while the charge is within min-ticks."}
   {:skill-id :thunder-clap :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [36.0 72.0] :comment "ThunderClap base damage lerp endpoints."}
   {:skill-id :thunder-clap :id :combat.overcharge-multiplier :path "combat.overcharge-multiplier" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [1.0 1.2] :comment "ThunderClap damage multiplier lerp endpoints for charge ticks above min-ticks; default mirrors the original partial overcharge formula."}
   {:skill-id :thunder-clap :id :combat.aoe-radius :path "combat.aoe-radius" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [15.0 30.0] :comment "ThunderClap AOE radius lerp endpoints."}
   {:skill-id :thunder-clap :id :cooldown.ticks-per-hold :path "cooldown.ticks-per-hold" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [10.0 6.0] :comment "ThunderClap cooldown multiplier per held tick, lerped by skill exp."}
   {:skill-id :thunder-clap :id :progression.exp-use :path "progression.exp-use" :section-suffix "progression" :type :double :min 0.0 :default 0.003 :comment "ThunderClap skill exp gained for a successful use."}

   {:skill-id :arc-gen :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [5.0 9.0] :comment "Arc Gen direct damage lerp endpoints."}
   {:skill-id :arc-gen :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [6.0 15.0] :comment "Arc Gen raycast range lerp endpoints."}
   {:skill-id :arc-gen :id :effect.ignite-probability :path "effect.ignite-probability" :section-suffix "effect" :type :double-list :min 0.0 :max 1.0 :list-count 2 :default [0.0 0.6] :comment "Arc Gen block ignite probability lerp endpoints."}
   {:skill-id :arc-gen :id :effect.fishing-probability :path "effect.fishing-probability" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.1 :comment "Arc Gen fishing trigger probability when exp threshold is met."}
   {:skill-id :arc-gen :id :effect.fishing-exp-threshold :path "effect.fishing-exp-threshold" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.5 :comment "Arc Gen exp threshold required for fishing behavior."}
   {:skill-id :arc-gen :id :effect.creeper-charge-chance :path "effect.creeper-charge-chance" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.3 :comment "Arc Gen chance to power a creeper after attacking it."}
   {:skill-id :arc-gen :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [30.0 70.0] :comment "Arc Gen down-stage CP cost lerp endpoints."}
   {:skill-id :arc-gen :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [18.0 11.0] :comment "Arc Gen down-stage overload cost lerp endpoints."}
   {:skill-id :arc-gen :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [15.0 5.0] :comment "Arc Gen cooldown lerp endpoints."}
   {:skill-id :arc-gen :id :progression.exp-entity :path "progression.exp-entity" :section-suffix "progression" :type :double-list :min 0.0 :list-count 2 :default [0.0048 0.0024] :comment "Arc Gen entity-hit exp formula [base, exp-scale]."}
   {:skill-id :arc-gen :id :progression.exp-block :path "progression.exp-block" :section-suffix "progression" :type :double-list :min 0.0 :list-count 2 :default [0.0018 0.0009] :comment "Arc Gen block-hit exp formula [base, exp-scale]."}

   {:skill-id :body-intensify :id :charge.min-ticks :path "charge.min-ticks" :section-suffix "charge" :type :int :min 1 :default 10 :comment "BodyIntensify minimum charge ticks required to perform."}
   {:skill-id :body-intensify :id :charge.max-ticks :path "charge.max-ticks" :section-suffix "charge" :type :int :min 1 :default 40 :comment "BodyIntensify maximum effective charge ticks."}
   {:skill-id :body-intensify :id :charge.max-tolerant-ticks :path "charge.max-tolerant-ticks" :section-suffix "charge" :type :int :min 1 :default 100 :comment "BodyIntensify maximum tolerated charge ticks before context termination."}
   {:skill-id :body-intensify :id :effect.probability-offset-ticks :path "effect.probability-offset-ticks" :section-suffix "effect" :type :double :min 0.0 :default 10.0 :comment "BodyIntensify charge ticks subtracted before random buff probability calculation."}
   {:skill-id :body-intensify :id :effect.probability-divisor :path "effect.probability-divisor" :section-suffix "effect" :type :double :min 0.001 :default 18.0 :comment "BodyIntensify probability divisor for random buff picks."}
   {:skill-id :body-intensify :id :effect.duration-multiplier :path "effect.duration-multiplier" :section-suffix "effect" :type :double-list :min 0.0 :list-count 2 :default [1.5 2.5] :comment "BodyIntensify buff duration multiplier lerp endpoints."}
   {:skill-id :body-intensify :id :effect.hunger-multiplier :path "effect.hunger-multiplier" :section-suffix "effect" :type :double :min 0.0 :default 1.25 :comment "BodyIntensify hunger duration multiplier."}
   {:skill-id :body-intensify :id :effect.hunger-amplifier :path "effect.hunger-amplifier" :section-suffix "effect" :type :int :min 0 :default 2 :comment "BodyIntensify hunger amplifier."}
   {:skill-id :body-intensify :id :effect.available-effects :path "effect.available-effects" :section-suffix "effect" :type :string-list :default ["speed:3" "jump-boost:1" "regeneration:1" "strength:1" "resistance:1"] :comment "BodyIntensify random buff pool entries as effect-id:max-amplifier."}
   {:skill-id :body-intensify :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [200.0 120.0] :comment "BodyIntensify down-stage overload cost lerp endpoints."}
   {:skill-id :body-intensify :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [20.0 15.0] :comment "BodyIntensify tick-stage CP cost lerp endpoints while charging."}
   {:skill-id :body-intensify :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 1.0 :list-count 2 :default [900.0 600.0] :comment "BodyIntensify cooldown lerp endpoints."}
   {:skill-id :body-intensify :id :progression.exp-use :path "progression.exp-use" :section-suffix "progression" :type :double :min 0.0 :default 0.01 :comment "BodyIntensify exp gained on successful release."}

   {:skill-id :current-charging :id :charge.visual-max-ticks :path "charge.visual-max-ticks" :section-suffix "charge" :type :int :min 1 :default 40 :comment "CurrentCharging max ticks used for client-side charge bar normalization."}
   {:skill-id :current-charging :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 15.0 :comment "CurrentCharging block raycast range."}
   {:skill-id :current-charging :id :effect.charge-amount :path "effect.charge-amount" :section-suffix "effect" :type :double-list :min 0.0 :list-count 2 :default [15.0 35.0] :comment "CurrentCharging energy transfer amount lerp endpoints."}
   {:skill-id :current-charging :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [65.0 48.0] :comment "CurrentCharging down-stage overload cost lerp endpoints."}
   {:skill-id :current-charging :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [3.0 7.0] :comment "CurrentCharging tick-stage CP cost lerp endpoints."}
   {:skill-id :current-charging :id :progression.exp-effective :path "progression.exp-effective" :section-suffix "progression" :type :double :min 0.0 :default 0.0001 :comment "CurrentCharging exp gained per effective tick."}
   {:skill-id :current-charging :id :progression.exp-ineffective :path "progression.exp-ineffective" :section-suffix "progression" :type :double :min 0.0 :default 0.00003 :comment "CurrentCharging exp gained per ineffective tick."}

   {:skill-id :mag-manip :id :targeting.grab-range :path "targeting.grab-range" :section-suffix "targeting" :type :double :min 0.0 :default 10.0 :comment "MagManip maximum block grab range."}
   {:skill-id :mag-manip :id :targeting.throw-range :path "targeting.throw-range" :section-suffix "targeting" :type :double :min 0.0 :default 20.0 :comment "MagManip maximum throw range."}
   {:skill-id :mag-manip :id :targeting.max-hold-distance :path "targeting.max-hold-distance" :section-suffix "targeting" :type :double :min 0.0 :default 5.0 :comment "MagManip maximum distance between player and held focus before release fails."}
   {:skill-id :mag-manip :id :movement.hold-distance :path "movement.hold-distance" :section-suffix "movement" :type :double :min 0.0 :default 2.0 :comment "MagManip held block focus distance."}
   {:skill-id :mag-manip :id :movement.hold-head-y-offset :path "movement.hold-head-y-offset" :section-suffix "movement" :type :double :min 0.0 :default 0.1 :comment "MagManip held block head Y offset."}
   {:skill-id :mag-manip :id :movement.throw-speed :path "movement.throw-speed" :section-suffix "movement" :type :double-list :min 0.0 :list-count 2 :default [0.5 1.0] :comment "MagManip thrown-entity speed lerp endpoints (blocks/tick)."}
   {:skill-id :mag-manip :id :cost.up.cp :path "cost.up.cp" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [140.0 270.0] :comment "MagManip up-stage CP cost lerp endpoints."}
   {:skill-id :mag-manip :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [35.0 20.0] :comment "MagManip up-stage overload cost lerp endpoints."}
   {:skill-id :mag-manip :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 1.0 :list-count 2 :default [60.0 40.0] :comment "MagManip manual cooldown lerp endpoints."}
   {:skill-id :mag-manip :id :progression.exp-throw :path "progression.exp-throw" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "MagManip exp gained on successful throw."}

   {:skill-id :mag-movement :id :movement.acceleration :path "movement.acceleration" :section-suffix "movement" :type :double :min 0.0 :default 0.08 :comment "MagMovement smooth acceleration step."}
   {:skill-id :mag-movement :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 25.0 :comment "MagMovement target raycast range."}
   {:skill-id :mag-movement :id :targeting.weak-metal-exp-threshold :path "targeting.weak-metal-exp-threshold" :section-suffix "targeting" :type :double :min 0.0 :max 1.0 :default 0.6 :comment "Legacy MagMovement threshold constant; original effective metal check accepts weak metal at every exp level."}
   {:skill-id :mag-movement :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [60.0 30.0] :comment "MagMovement down-stage overload cost lerp endpoints."}
   {:skill-id :mag-movement :id :cost.tick.cp :path "cost.tick.cp" :section-suffix "cost.tick" :type :double-list :min 0.0 :list-count 2 :default [15.0 8.0] :comment "MagMovement tick-stage CP cost lerp endpoints."}
   {:skill-id :mag-movement :id :progression.exp-min :path "progression.exp-min" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "MagMovement minimum exp gained on completion."}
   {:skill-id :mag-movement :id :progression.exp-distance-scale :path "progression.exp-distance-scale" :section-suffix "progression" :type :double :min 0.0 :default 0.0011 :comment "MagMovement exp gained per traveled block."}

   {:skill-id :mine-detect :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [15.0 30.0] :comment "MineDetect scan range lerp endpoints."}
   {:skill-id :mine-detect :id :effect.blindness-duration-ticks :path "effect.blindness-duration-ticks" :section-suffix "effect" :type :int :min 0 :default 100 :comment "MineDetect blindness duration in ticks."}
   {:skill-id :mine-detect :id :effect.blindness-amplifier :path "effect.blindness-amplifier" :section-suffix "effect" :type :int :min 0 :default 0 :comment "MineDetect blindness amplifier."}
  {:skill-id :mine-detect :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [1500.0 1000.0] :comment "MineDetect down-stage CP cost lerp endpoints."}
  {:skill-id :mine-detect :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [200.0 180.0] :comment "MineDetect down-stage overload cost lerp endpoints."}
  {:skill-id :mine-detect :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 1.0 :list-count 2 :default [900.0 400.0] :comment "MineDetect cooldown lerp endpoints."}
  {:skill-id :mine-detect :id :progression.exp-cast :path "progression.exp-cast" :section-suffix "progression" :type :double :min 0.0 :default 0.008 :comment "MineDetect exp gained per cast."}

   {:skill-id :thunder-bolt :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double :min 0.0 :default 20.0 :comment "ThunderBolt target raycast range."}
   {:skill-id :thunder-bolt :id :combat.direct-damage :path "combat.direct-damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [10.0 25.0] :comment "ThunderBolt direct damage lerp endpoints."}
   {:skill-id :thunder-bolt :id :combat.aoe-radius :path "combat.aoe-radius" :section-suffix "combat" :type :double :min 0.0 :default 8.0 :comment "ThunderBolt AOE damage radius."}
   {:skill-id :thunder-bolt :id :combat.aoe-damage :path "combat.aoe-damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [6.0 15.0] :comment "ThunderBolt AOE damage lerp endpoints."}
   {:skill-id :thunder-bolt :id :effect.slowness-chance :path "effect.slowness-chance" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.8 :comment "ThunderBolt slowness roll chance."}
   {:skill-id :thunder-bolt :id :effect.slowness-exp-threshold :path "effect.slowness-exp-threshold" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.2 :comment "ThunderBolt minimum skill exp required before slowness can trigger."}
   {:skill-id :thunder-bolt :id :effect.slowness-duration-ticks :path "effect.slowness-duration-ticks" :section-suffix "effect" :type :int :min 0 :default 40 :comment "ThunderBolt slowness duration in ticks."}
   {:skill-id :thunder-bolt :id :effect.slowness-aoe-retry-duration-ticks :path "effect.slowness-aoe-retry-duration-ticks" :section-suffix "effect" :type :int :min 0 :default 20 :comment "ThunderBolt slowness duration for each original AOE retry roll applied to the direct target."}
   {:skill-id :thunder-bolt :id :effect.slowness-amplifier :path "effect.slowness-amplifier" :section-suffix "effect" :type :int :min 0 :default 3 :comment "ThunderBolt slowness amplifier."}
   {:skill-id :thunder-bolt :id :effect.creeper-charge-chance :path "effect.creeper-charge-chance" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.3 :comment "ThunderBolt chance to charge a creeper actually hit by the strike."}
   {:skill-id :thunder-bolt :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [280.0 420.0] :comment "ThunderBolt down-stage CP cost lerp endpoints."}
   {:skill-id :thunder-bolt :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [50.0 27.0] :comment "ThunderBolt down-stage overload cost lerp endpoints."}
   {:skill-id :thunder-bolt :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 1.0 :list-count 2 :default [120.0 50.0] :comment "ThunderBolt cooldown lerp endpoints."}
   {:skill-id :thunder-bolt :id :progression.exp-effective :path "progression.exp-effective" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "ThunderBolt effective exp gain."}
   {:skill-id :thunder-bolt :id :progression.exp-ineffective :path "progression.exp-ineffective" :section-suffix "progression" :type :double :min 0.0 :default 0.003 :comment "ThunderBolt ineffective exp gain."}])

(def internal-tunable-definitions
  [{:skill-id :railgun :id :beam.visual-distance :path "beam.visual-distance" :section-suffix "beam" :type :double :min 0.0 :default 45.0 :comment "Internal FX beam visual distance."}])

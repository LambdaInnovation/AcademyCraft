(ns cn.li.ac.ability.skill-config.teleporter
  "Teleporter skill action tunables for player-facing skill config.")

(def skill-tunable-definitions
  [{:skill-id :dim-folding-theorem :id :critical.damage-multipliers :path "critical.damage-multipliers" :section-suffix "critical" :type :double-list :min 0.0 :list-count 3 :default [1.3 1.6 2.6] :comment "Teleporter critical damage multipliers for crit levels 0, 1, and 2."}
   {:skill-id :dim-folding-theorem :id :critical.level0-probability :path "critical.level0-probability" :section-suffix "critical" :type :double-list :min 0.0 :max 1.0 :list-count 2 :default [0.1 0.2] :comment "DimFolding passive contribution to level-0 critical probability."}
   {:skill-id :dim-folding-theorem :id :progression.exp-per-crit-level :path "progression.exp-per-crit-level" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "DimFolding exp gained per critical level plus one."}

   {:skill-id :space-fluct :id :critical.level0-probability :path "critical.level0-probability" :section-suffix "critical" :type :double-list :min 0.0 :max 1.0 :list-count 2 :default [0.18 0.25] :comment "SpaceFluct passive contribution to level-0 critical probability."}
   {:skill-id :space-fluct :id :critical.level1-probability :path "critical.level1-probability" :section-suffix "critical" :type :double-list :min 0.0 :max 1.0 :list-count 2 :default [0.10 0.15] :comment "SpaceFluct passive contribution to level-1 critical probability."}
   {:skill-id :space-fluct :id :critical.level2-probability :path "critical.level2-probability" :section-suffix "critical" :type :double-list :min 0.0 :max 1.0 :list-count 2 :default [0.01 0.03] :comment "SpaceFluct passive contribution to level-2 critical probability."}
   {:skill-id :space-fluct :id :progression.exp-critical :path "progression.exp-critical" :section-suffix "progression" :type :double :min 0.0 :default 0.0001 :comment "SpaceFluct exp gained when a teleporter critical attack occurs."}

  {:skill-id :flashing :id :movement.blink-distance :path "movement.blink-distance" :section-suffix "movement" :type :double-list :min 0.0 :list-count 2 :default [12.0 18.0] :comment "Flashing blink distance per execute."}
   {:skill-id :flashing :id :timing.blink-interval-ticks :path "timing.blink-interval-ticks" :section-suffix "timing" :type :double-list :min 1.0 :list-count 2 :default [6.0 3.0] :comment "Flashing blink interval in ticks."}
  {:skill-id :flashing :id :timing.max-active-ticks :path "timing.max-active-ticks" :section-suffix "timing" :type :double-list :min 1.0 :list-count 2 :default [60.0 150.0] :comment "Flashing max active duration before auto-terminate."}
  {:skill-id :flashing :id :timing.post-blink-fall-protect-ticks :path "timing.post-blink-fall-protect-ticks" :section-suffix "timing" :type :double-list :min 0.0 :list-count 2 :default [40.0 40.0] :comment "Flashing post-blink fall-protection ticks."}
  {:skill-id :flashing :id :cost.down.overload :path "cost.down.overload" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [250.0 180.0] :comment "Flashing activation overload cost."}
  {:skill-id :flashing :id :cost.down.cp :path "cost.down.cp" :section-suffix "cost.down" :type :double-list :min 0.0 :list-count 2 :default [80.0 60.0] :comment "Flashing activation CP cost."}
  {:skill-id :flashing :id :cost.blink.cp :path "cost.blink.cp" :section-suffix "cost.blink" :type :double-list :min 0.0 :list-count 2 :default [13.0 6.0] :comment "Flashing manual CP cost per blink."}
  {:skill-id :flashing :id :cost.blink.overload :path "cost.blink.overload" :section-suffix "cost.blink" :type :double-list :min 0.0 :list-count 2 :default [0.0 0.0] :comment "Flashing manual overload cost per blink."}
  {:skill-id :flashing :id :cooldown.deactivate-ticks :path "cooldown.deactivate-ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [900.0 400.0] :comment "Flashing cooldown applied on deactivate/abort."}
   {:skill-id :flashing :id :progression.exp-blink :path "progression.exp-blink" :section-suffix "progression" :type :double :min 0.0 :default 0.002 :comment "Flashing exp gained per successful blink."}

  {:skill-id :flesh-ripping :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [6.0 14.0] :comment "FleshRipping entity raycast range."}
  {:skill-id :flesh-ripping :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [5.0 12.0] :comment "FleshRipping magic damage."}
  {:skill-id :flesh-ripping :id :effect.nausea-chance :path "effect.nausea-chance" :section-suffix "effect" :type :double :min 0.0 :max 1.0 :default 0.05 :comment "FleshRipping nausea application probability."}
  {:skill-id :flesh-ripping :id :effect.nausea-duration-ticks :path "effect.nausea-duration-ticks" :section-suffix "effect" :type :int :min 0 :default 100 :comment "FleshRipping nausea duration in ticks."}
   {:skill-id :flesh-ripping :id :effect.nausea-amplifier :path "effect.nausea-amplifier" :section-suffix "effect" :type :int :min 0 :default 0 :comment "FleshRipping nausea amplifier."}
  {:skill-id :flesh-ripping :id :cost.up.cp :path "cost.up.cp" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [130.0 270.0] :comment "FleshRipping up-stage CP cost."}
  {:skill-id :flesh-ripping :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [60.0 50.0] :comment "FleshRipping up-stage overload cost."}
  {:skill-id :flesh-ripping :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [90.0 40.0] :comment "FleshRipping cooldown ticks."}
  {:skill-id :flesh-ripping :id :progression.exp-hit :path "progression.exp-hit" :section-suffix "progression" :type :double :min 0.0 :default 0.005 :comment "FleshRipping exp gained per hit."}

   {:skill-id :mark-teleport :id :targeting.min-distance :path "targeting.min-distance" :section-suffix "targeting" :type :double :min 0.0 :default 3.0 :comment "MarkTeleport minimum valid teleport distance."}
   {:skill-id :mark-teleport :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [25.0 60.0] :comment "MarkTeleport maximum range."}
   {:skill-id :mark-teleport :id :targeting.range-per-hold-tick :path "targeting.range-per-hold-tick" :section-suffix "targeting" :type :double :min 0.0 :default 2.0 :comment "MarkTeleport range growth per held tick."}
   {:skill-id :mark-teleport :id :cost.up.cp-per-block :path "cost.up.cp-per-block" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [12.0 4.0] :comment "MarkTeleport CP cost per block."}
   {:skill-id :mark-teleport :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [40.0 20.0] :comment "MarkTeleport overload cost."}
   {:skill-id :mark-teleport :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [30.0 0.0] :comment "MarkTeleport manual cooldown ticks."}
   {:skill-id :mark-teleport :id :progression.exp-per-distance :path "progression.exp-per-distance" :section-suffix "progression" :type :double :min 0.0 :default 0.00018 :comment "MarkTeleport exp gained per teleported block."}

   {:skill-id :location-teleport :id :targeting.cross-dimension-exp-threshold :path "targeting.cross-dimension-exp-threshold" :section-suffix "targeting" :type :double :min 0.0 :max 1.0 :default 0.8 :comment "LocationTeleport exp threshold for cross-dimension travel."}
   {:skill-id :location-teleport :id :targeting.teleport-radius :path "targeting.teleport-radius" :section-suffix "targeting" :type :double :min 0.0 :default 5.0 :comment "LocationTeleport nearby entity teleport radius."}
   {:skill-id :location-teleport :id :ui.max-location-name-length :path "ui.max-location-name-length" :section-suffix "ui" :type :int :min 1 :default 16 :comment "LocationTeleport maximum saved location name length."}
   {:skill-id :location-teleport :id :cost.perform.cp-base :path "cost.perform.cp-base" :section-suffix "cost.perform" :type :double-list :min 0.0 :list-count 2 :default [200.0 150.0] :comment "LocationTeleport base CP cost before distance multiplier."}
   {:skill-id :location-teleport :id :cost.perform.overload :path "cost.perform.overload" :section-suffix "cost.perform" :type :double :min 0.0 :default 240.0 :comment "LocationTeleport fixed overload cost."}
   {:skill-id :location-teleport :id :cost.perform.cross-dimension-multiplier :path "cost.perform.cross-dimension-multiplier" :section-suffix "cost.perform" :type :double :min 0.0 :default 2.0 :comment "LocationTeleport CP multiplier for cross-dimension travel."}
   {:skill-id :location-teleport :id :cost.perform.min-distance-multiplier :path "cost.perform.min-distance-multiplier" :section-suffix "cost.perform" :type :double :min 0.0 :default 8.0 :comment "LocationTeleport minimum distance multiplier."}
   {:skill-id :location-teleport :id :cost.perform.distance-cap :path "cost.perform.distance-cap" :section-suffix "cost.perform" :type :double :min 0.0 :default 800.0 :comment "LocationTeleport distance cap before square root multiplier."}
   {:skill-id :location-teleport :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [30.0 20.0] :comment "LocationTeleport cooldown ticks."}
   {:skill-id :location-teleport :id :progression.long-distance-threshold :path "progression.long-distance-threshold" :section-suffix "progression" :type :double :min 0.0 :default 200.0 :comment "LocationTeleport distance threshold for long-distance exp."}
   {:skill-id :location-teleport :id :progression.exp-short :path "progression.exp-short" :section-suffix "progression" :type :double :min 0.0 :default 0.015 :comment "LocationTeleport exp for short-distance travel."}
   {:skill-id :location-teleport :id :progression.exp-long :path "progression.exp-long" :section-suffix "progression" :type :double :min 0.0 :default 0.03 :comment "LocationTeleport exp for long-distance travel."}

  {:skill-id :penetrate-teleport :id :targeting.max-distance :path "targeting.max-distance" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [10.0 35.0] :comment "PenetrateTeleport max requested distance."}
  {:skill-id :penetrate-teleport :id :cost.up.cp-per-block :path "cost.up.cp-per-block" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [14.0 9.0] :comment "PenetrateTeleport CP cost per teleported block."}
  {:skill-id :penetrate-teleport :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [80.0 50.0] :comment "PenetrateTeleport up-stage overload cost."}
  {:skill-id :penetrate-teleport :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [50.0 30.0] :comment "PenetrateTeleport cooldown ticks."}
  {:skill-id :penetrate-teleport :id :progression.exp-per-distance :path "progression.exp-per-distance" :section-suffix "progression" :type :double :min 0.0 :default 0.00014 :comment "PenetrateTeleport exp gained per teleported block."}

  {:skill-id :shift-teleport :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [25.0 35.0] :comment "ShiftTeleport block target range."}
  {:skill-id :shift-teleport :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [15.0 35.0] :comment "ShiftTeleport line-hit magic damage."}
  {:skill-id :shift-teleport :id :cost.up.cp :path "cost.up.cp" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [260.0 320.0] :comment "ShiftTeleport up-stage CP cost."}
  {:skill-id :shift-teleport :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [40.0 30.0] :comment "ShiftTeleport up-stage overload cost."}
  {:skill-id :shift-teleport :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [100.0 60.0] :comment "ShiftTeleport cooldown ticks."}
  {:skill-id :shift-teleport :id :progression.exp-base :path "progression.exp-base" :section-suffix "progression" :type :double :min 0.0 :default 0.002 :comment "ShiftTeleport base exp coefficient for (1 + hit-count)."}

  {:skill-id :threatening-teleport :id :targeting.range :path "targeting.range" :section-suffix "targeting" :type :double-list :min 0.0 :list-count 2 :default [8.0 15.0] :comment "ThreateningTeleport raycast range."}
  {:skill-id :threatening-teleport :id :combat.damage :path "combat.damage" :section-suffix "combat" :type :double-list :min 0.0 :list-count 2 :default [3.0 6.0] :comment "ThreateningTeleport base magic damage."}
  {:skill-id :threatening-teleport :id :combat.needle-damage-multiplier :path "combat.needle-damage-multiplier" :section-suffix "combat" :type :double :min 0.0 :default 1.5 :comment "ThreateningTeleport damage multiplier while the held item is a needle."}
  {:skill-id :threatening-teleport :id :cost.up.cp :path "cost.up.cp" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [35.0 100.0] :comment "ThreateningTeleport up-stage CP cost."}
  {:skill-id :threatening-teleport :id :cost.up.overload :path "cost.up.overload" :section-suffix "cost.up" :type :double-list :min 0.0 :list-count 2 :default [18.0 10.0] :comment "ThreateningTeleport up-stage overload cost."}
  {:skill-id :threatening-teleport :id :cooldown.ticks :path "cooldown.ticks" :section-suffix "cooldown" :type :double-list :min 0.0 :list-count 2 :default [30.0 15.0] :comment "ThreateningTeleport cooldown ticks."}
  {:skill-id :threatening-teleport :id :progression.exp-base :path "progression.exp-base" :section-suffix "progression" :type :double :min 0.0 :default 0.003 :comment "ThreateningTeleport base exp per execute."}
  {:skill-id :threatening-teleport :id :progression.exp-hit-factor :path "progression.exp-hit-factor" :section-suffix "progression" :type :double :min 0.0 :default 1.0 :comment "ThreateningTeleport exp multiplier for hit execute."}
  {:skill-id :threatening-teleport :id :progression.exp-miss-factor :path "progression.exp-miss-factor" :section-suffix "progression" :type :double :min 0.0 :default 0.2 :comment "ThreateningTeleport exp multiplier for miss execute."}
  {:skill-id :threatening-teleport :id :interaction.drop-prob.hit :path "interaction.drop-prob.hit" :section-suffix "interaction" :type :double :min 0.0 :max 1.0 :default 0.3 :comment "ThreateningTeleport drop probability when target is hit."}
  {:skill-id :threatening-teleport :id :interaction.drop-prob.miss :path "interaction.drop-prob.miss" :section-suffix "interaction" :type :double :min 0.0 :max 1.0 :default 1.0 :comment "ThreateningTeleport drop probability when no target is hit."}])

(def internal-tunable-definitions
  [{:skill-id :mark-teleport :id :targeting.eye-height :path "targeting.eye-height" :section-suffix "targeting" :type :double :min 0.0 :default 1.6 :comment "Internal raycast origin eye height."}
  {:skill-id :penetrate-teleport :id :targeting.scan-step :path "targeting.scan-step" :section-suffix "targeting" :type :double :min 0.001 :default 0.8 :comment "Internal wall scan step in blocks."}
   {:skill-id :shift-teleport :id :targeting.eye-height :path "targeting.eye-height" :section-suffix "targeting" :type :double :min 0.0 :default 1.6 :comment "Internal raycast eye height."}])

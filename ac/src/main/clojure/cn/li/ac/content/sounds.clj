(ns cn.li.ac.content.sounds
	"Sound event declarations for AC content."
	(:require [cn.li.mcmod.sound.dsl :as sdsl]
						[cn.li.mcmod.runtime.install :as install]
						[cn.li.mcmod.util.log :as log]))

(def all-sound-ids
	["ability.deny"
	 "ability.preset_switch"
	 "ability.preset_confirm"
	 "em.arc_weak"
	 "em.arc_strong"
	 "em.minedetect"
	 "em.railgun"
	 "em.move_loop"
	 "em.charge_loop"
	 "em.intensify_activate"
	 "em.intensify_loop"
	 "em.lf_loop"
	 "em.lf"
	 "em.mag_manip"
	 "md.ballshoot"
	 "md.ray_small"
	 "md.shield_startup"
	 "md.shield_loop"
	 "md.meltdowner"
	 "md.mine_loop"
	 "md.mine_basic_startup"
	 "md.mine_luck_startup"
	 "md.mine_expert_startup"
	 "md.simple_charge"
	 "md.md_charge"
	 "md.eb_spawn"
	 "md.eb_explode"
	 "md.em_start"
	 "md.em_fire"
	 "md.jet_engine"
	 "md.jet_charge"
	 "md.jet_max"
	 "md.mine_ray_start"
	 "md.ray_barrage"
	 "md.shield_on"
	 "md.shield_off"
	 "md.sb_charge"
	 "entity.flipcoin"
	 "entity.silbarn_heavy"
	 "entity.silbarn_light"
	 "terminal.select"
	 "terminal.confirm"
	 "tp.tp"
	 "tp.tp_pre"
	 "tp.guts"
	 "tp.tp_shift"
	 "tp.tp_flashing"
	 "tp.flash"
	 "tp.flesh_ripping"
	 "tp.threatening_tp"
	 "tp.penetrate_tp"
	 "vecmanip.blood_retro"
	 "vecmanip.directed_shock"
	 "vecmanip.groundshock"
	 "vecmanip.directed_blast"
	 "vecmanip.vec_accel"
	 "vecmanip.plasma_cannon"
	 "vecmanip.storm_wing"
	 "vecmanip.vec_deviation"
	 "vecmanip.vec_reflection"
	 "vecmanip.plasma_cannon_t"
	 "machine.imag_fusor_work"
	 "machine.machine_work"])

(defn init-sounds!
	[]
	(install/framework-once! ::sounds-initialized?
  (fn []
    (doseq [sound-id all-sound-ids]
			(sdsl/register-sound! (sdsl/create-sound-spec sound-id {})))

		(log/debug "Sound content initialized"))))
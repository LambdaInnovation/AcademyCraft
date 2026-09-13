(ns cn.li.ac.ability.rules.progression
	"Pure progression and learning-cost formulas for the ability system."
	(:require [cn.li.ac.ability.config :as cfg]))

(defn learning-cost
	"Return the stim cost to learn a skill at `level`."
	[level]
	(+ (cfg/skill-learning-cost-base)
		 (* level level (cfg/skill-learning-cost-level-square-factor))))

(defn skill-learning-stims
	"Stim count to learn a skill. Truncated, matching upstream
	 Skill.getLearningStims: (int)(3 + level^2 * 0.5)."
	[skill-level]
	(int (learning-cost skill-level)))

(defn level-up-stims
	"Stim count to level up from current-level."
	[current-level]
	(* (cfg/level-up-stim-base) (inc current-level)))

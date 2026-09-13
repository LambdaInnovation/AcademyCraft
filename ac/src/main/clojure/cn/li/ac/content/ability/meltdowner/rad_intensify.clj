(ns cn.li.ac.content.ability.meltdowner.rad-intensify
  "Meltdowner passive: radiation intensify.
  Custom exp is derived from max-cp ratio; damage rate lerps from 1.4 to 1.8."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.util.balance :as bal]))

(def-skill-config-ops :rad-intensify)
(defn skill-exp
  "Matches original's overridden getSkillExp: clamp(0,1, maxCP / getInitCP(5)).
  getInitCP(5) reads only the init_cp config list at level 5 — NOT
  init_cp + add_cp. That sum is a different concept: the ceiling a player can
  reach once :add-max-cp has fully accumulated through skill use."
  [player-id]
  (let [max-cp (double (or (skill-effects/player-path player-id [:resource-data :max-cp] 0.0) 0.0))
        level5-cp (double (cfg/get-init-cp 5))]
    (bal/clamp01
      (if (pos? level5-cp)
        (/ max-cp level5-cp)
        0.0))))

(defn rate
  [player-id]
  (cfg-lerp :combat.damage-rate (skill-exp player-id)))

(defn mark-duration-ticks []
  (max 1 (long (cfg-int :effect.mark-duration-ticks))))

(defskill rad-intensify
  :id :rad-intensify
  :category-id :meltdowner
  :name-key "ability.skill.meltdowner.rad_intensify"
  :description-key "ability.skill.meltdowner.rad_intensify.desc"
  :icon "textures/abilities/meltdowner/skills/rad_intensify.png"
  :ui-position [35 75]
  :ctrl-id :rad-intensify
  :pattern :passive
  :prerequisites [{:skill-id :electron-bomb :min-exp 0.5}])

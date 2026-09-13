(ns cn.li.ac.content.ability.teleporter.space-fluct
  "Teleporter passive: Space Fluctuation.

  Contributes crit probability at all levels via passive-hooks."
  (:require [cn.li.ac.ability.dsl :refer [defskill]]))

(defskill space-fluct
  :id :space-fluct
  :category-id :teleporter
  :name-key "ability.skill.teleporter.space_fluct"
  :description-key "ability.skill.teleporter.space_fluct.desc"
  :icon "textures/abilities/teleporter/skills/space_fluct.png"
  :ui-position [160 80]
  :ctrl-id :space-fluct
  :pattern :passive
  :prerequisites [{:skill-id :shift-teleport :min-exp 0.0}])

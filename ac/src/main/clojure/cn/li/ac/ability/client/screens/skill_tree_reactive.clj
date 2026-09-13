(ns cn.li.ac.ability.client.screens.skill-tree-reactive
  "Skill-tree popup overlay runtimes for the developer panel.

  The former full-screen 420×260 reactive tree that lived here was replaced
  by the classic-layout skill-tree viewer (upstream SkillTreeAppUI):
  cn.li.ac.block.developer.panel-reactive/create-viewer-runtime, wired to
  the :ac/skill-tree widget factory."
  (:require [cn.li.ac.ability.client.screens.skill-tree-view :as view]
            [cn.li.mcmod.ui.runtime :as rt]))

(defn create-detail-overlay-runtime [node]
  (let [r (rt/create-runtime)]
    (view/refresh-detail-overlay! r node)
    r))

(defn create-levelup-overlay-runtime [target-level dev-state & [est-consumption]]
  (let [r (rt/create-runtime)]
    (view/refresh-levelup-overlay! r target-level dev-state est-consumption)
    r))

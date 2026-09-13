(ns cn.li.ac.content.loot
  "Data-driven loot injection declarations."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.loot.dsl :as ldsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn init-loot!
  []
  (install/framework-once! ::loot-initialized?
  (fn []
    ;; Example: inject custom item into common dungeon chests.
    ;; Missing target item IDs are skipped safely at runtime in Forge helper.
    (ldsl/defloot {:id "dungeon_skill_book"
                   :target-table "minecraft:chests/simple_dungeon"
                   :item-id (modid/namespaced-path "electromaster_brain_course")
                   :weight 3
                   :quality 0
                   :min-count 1.0
                   :max-count 1.0})
    (log/debug "Loot content initialized"))))

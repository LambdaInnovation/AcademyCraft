(ns cn.li.ac.content.items.all
  "Content entrypoint for AC item declarations."
  (:require [cn.li.ac.item.components :as components]
            [cn.li.ac.tutorial.item :as tutorial-item]
            [cn.li.ac.item.app-installers :as app-installers]
            [cn.li.ac.item.constraint-plate :as constraint-plate]
            [cn.li.ac.item.energy-items :as energy-items]
            [cn.li.ac.item.materials :as materials]
            [cn.li.ac.item.mat-core :as mat-core]
            [cn.li.ac.item.media :as media]
            [cn.li.ac.item.special-items :as special-items]
            [cn.li.ac.item.windgen-fan :as windgen-fan]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.integration.runtime-hooks :as integration-hooks]))

(defn init-items!
  []
  (install/framework-once! ::items-installed?
  (fn []
    (materials/init-materials!)
    (components/init-components!)
    (tutorial-item/init-tutorial-item!)
    (app-installers/init-app-installers!)
    (constraint-plate/init-constraint-plate!)
    (energy-items/init-energy-items!)
    (mat-core/init-mat-cores!)
    (media/init-media!)
    (special-items/init-special-items!)
    (windgen-fan/init-windgen-fan!)
    (integration-hooks/register-jei-nbt-subtype-item-ids!
      ["energy_unit" "developer_portable" "matter_unit"]))))

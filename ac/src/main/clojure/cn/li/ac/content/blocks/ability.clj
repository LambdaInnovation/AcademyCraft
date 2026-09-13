(ns cn.li.ac.content.blocks.ability
  "Content entrypoint for ability system blocks"
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private ability-block-spec
  {:label :ability
   :namespaces '[cn.li.ac.block.developer.block
                 cn.li.ac.block.developer.gui-reactive
                 cn.li.ac.block.ability-interferer.block
                 cn.li.ac.block.ability-interferer.gui-reactive]
   :init-entries '[cn.li.ac.block.developer.block/init-developer!
                   cn.li.ac.block.developer.gui-reactive/init-developer-reactive!
                   cn.li.ac.block.ability-interferer.block/init-ability-interferer!
                   cn.li.ac.block.ability-interferer.gui-reactive/init-ability-interferer-reactive!]})

(defn init-ability-blocks!
  []
  (install/framework-once! ::ability-blocks-installed?
  (fn []
    (block-loader/load-block-category! ability-block-spec))))

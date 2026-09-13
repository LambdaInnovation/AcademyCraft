(ns cn.li.ac.content.blocks.generators
  "Content entrypoint for energy generator blocks"
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private generator-block-spec
  {:label :generators
   :namespaces '[cn.li.ac.block.solar-gen.block
                 cn.li.ac.block.solar-gen.gui-reactive
                 cn.li.ac.block.wind-gen.block
                 cn.li.ac.block.wind-gen.gui-reactive
                 cn.li.ac.block.phase-gen.block
                 cn.li.ac.block.phase-gen.gui-reactive
                 cn.li.ac.block.cat-engine.block]
   :init-entries '[cn.li.ac.block.solar-gen.block/init-solar-gen!
                   cn.li.ac.block.solar-gen.gui-reactive/init-solar-reactive!
                   cn.li.ac.block.wind-gen.block/init-wind-gen!
                   cn.li.ac.block.wind-gen.gui-reactive/init-wind-gen-reactive!
                   cn.li.ac.block.phase-gen.block/init-phase-gen!
                   cn.li.ac.block.phase-gen.gui-reactive/init-phase-gen-reactive!
                   cn.li.ac.block.cat-engine.block/init-cat-engine!]})

(defn init-generator-blocks!
  []
  (install/framework-once! ::generator-blocks-installed?
  (fn []
    (block-loader/load-block-category! generator-block-spec))))

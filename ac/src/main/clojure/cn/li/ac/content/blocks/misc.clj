(ns cn.li.ac.content.blocks.misc
  "Content entrypoint for miscellaneous blocks"
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private misc-block-spec
  {:label :misc
   :namespaces '[cn.li.ac.block.ores
                 cn.li.ac.block.imag-phase.block]
   :init-entries '[cn.li.ac.block.ores/init-ores!
                   cn.li.ac.block.imag-phase.block/init-imag-phase!]})

(defn init-misc-blocks!
  []
  (install/framework-once! ::misc-blocks-installed?
  (fn []
    (block-loader/load-block-category! misc-block-spec))))

(ns cn.li.ac.content.blocks.crafting
  "Content entrypoint for crafting/processing blocks"
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private crafting-block-spec
  {:label :crafting
   ;; Reactive UI migration — see generators.clj comment for rationale.
   :namespaces '[cn.li.ac.block.imag-fusor.gui-reactive
                 cn.li.ac.block.metal-former.gui-reactive]
   :init-entries '[cn.li.ac.block.imag-fusor.block/init-imag-fusor!
                   cn.li.ac.block.metal-former.block/init-metal-former!
                   cn.li.ac.block.imag-fusor.gui-reactive/init-imag-fusor-reactive!
                   cn.li.ac.block.metal-former.gui-reactive/init-metal-former-reactive!]})

(defn init-crafting-blocks!
  []
  (install/framework-once! ::crafting-blocks-installed?
  (fn []
    (block-loader/load-block-category! crafting-block-spec))))

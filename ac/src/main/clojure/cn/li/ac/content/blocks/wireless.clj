(ns cn.li.ac.content.blocks.wireless
  "Content entrypoint for wireless blocks (matrix + node).

  Each block namespace registers via init-machine! during category init.
  The wireless system (cn.li.ac.wireless.*) has no compile-time dependency on
  these namespaces; blocks interact through acapi Java interfaces."
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.ac.wireless.gui.message.bootstrap :as msg-reg]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private wireless-block-spec
  {:label :wireless
   :namespaces '[cn.li.ac.block.wireless-matrix.block
                cn.li.ac.block.wireless-matrix.gui-reactive
                cn.li.ac.block.wireless-node.block
                cn.li.ac.block.wireless-node.gui-reactive]
   :init-entries '[cn.li.ac.block.wireless-matrix.block/init-wireless-matrix!
                   cn.li.ac.block.wireless-matrix.gui-reactive/init-wireless-matrix-reactive!
                   cn.li.ac.block.wireless-node.block/init-wireless-nodes!
                   cn.li.ac.block.wireless-node.gui-reactive/init-wireless-node-reactive!]
   :post-init-entries [msg-reg/register-all!]})

(defn init-wireless-blocks!
  []
  (install/framework-once! ::wireless-blocks-installed?
  (fn []
    (block-loader/load-block-category! wireless-block-spec))))

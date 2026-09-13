(ns cn.li.mc1201.gui.reactive.kinds-renderer-parity-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mc1201.gui.reactive.render :as ui-render]))

(deftest node-kinds-match-kind-renderers
  (let [node-kinds (set (keys node/kinds))
        renderer-kinds (set (keys ui-render/kind-renderers))]
    (is (= node-kinds renderer-kinds)
        (str "kind mismatch. only-in-node: "
             (seq (clojure.set/difference node-kinds renderer-kinds))
             " only-in-renderers: "
             (seq (clojure.set/difference renderer-kinds node-kinds))))))

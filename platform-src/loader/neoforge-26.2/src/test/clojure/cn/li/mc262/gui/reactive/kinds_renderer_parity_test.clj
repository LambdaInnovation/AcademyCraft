(ns cn.li.mc262.gui.reactive.kinds-renderer-parity-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.set :as set]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mc262.gui.reactive.render :as ui-render]))

(deftest node-kinds-match-kind-renderers
  (let [node-kinds (set (keys node/kinds))
        renderer-kinds (set (keys ui-render/kind-renderers))]
    (is (= node-kinds renderer-kinds)
        (str "kind mismatch. only-in-node: "
             (seq (set/difference node-kinds renderer-kinds))
             " only-in-renderers: "
             (seq (set/difference renderer-kinds node-kinds))))))

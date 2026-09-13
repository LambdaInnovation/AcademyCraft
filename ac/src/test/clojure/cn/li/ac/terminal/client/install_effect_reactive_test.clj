(ns cn.li.ac.terminal.client.install-effect-reactive-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.terminal.client.install-effect-reactive :as install-effect]
            [cn.li.mcmod.ui.runtime :as rt]))

(deftest install-effect-builds-original-layer-types-test
  (let [runtime (install-effect/create-runtime nil)]
    (try
      (is (= :box (.getKind (rt/node-by-id runtime :main))))
      (is (= :box (.getKind (rt/node-by-id runtime :outline))))
      (is (= :box (.getKind (rt/node-by-id runtime :cover))))
      (is (= :box (.getKind (rt/node-by-id runtime :tag))))
      (is (= :text (.getKind (rt/node-by-id runtime :tag-text))))
      (is (= :box (.getKind (rt/node-by-id runtime :bar-fill))))
      (finally
        (rt/dispose! runtime)))))

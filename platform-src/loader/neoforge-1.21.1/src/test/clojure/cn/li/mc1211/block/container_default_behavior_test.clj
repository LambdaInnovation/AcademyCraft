(ns cn.li.mc1211.block.container-default-behavior-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.block.logic-compile :as lc])
  (:import [net.minecraft.core Direction]))

(deftest worldly-container-non-item-fallback-contract-test
  (let [c (.-container (lc/compile-tile-logic {:container {}}))]
    (is (= 0 (.getSize c nil)))
    (is (true? (.stillValid c nil nil)))
    (is (= 0 (alength (.getSlotsForFace c nil Direction/UP))))
    (is (false? (.canPlaceItemThroughFace c nil 0 nil Direction/DOWN)))
    (is (true? (.canTakeItemThroughFace c nil 0 nil Direction/DOWN)))))

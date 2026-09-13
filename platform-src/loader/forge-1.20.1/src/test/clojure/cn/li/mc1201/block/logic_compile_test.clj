(ns cn.li.mc1201.block.logic-compile-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.block.logic-compile :as lc])
  (:import [cn.li.mcbase.block.logic TileLogicBundle ITileTickLogic ITileContainerLogic]))

(deftest compile-empty-cfg-test
  (let [bundle (lc/compile-tile-logic {})]
    (is (instance? TileLogicBundle bundle))
    (is (nil? (.-tick bundle)))
    (is (nil? (.-nbt bundle)))
    (is (nil? (.-container bundle)))
    (is (nil? (.-capability bundle)))))

(deftest compile-tick-only-test
  (let [bundle (lc/compile-tile-logic {:tick-fn (fn [_ _ _ _] :ok)})]
    (is (instance? ITileTickLogic (.-tick bundle)))))

(deftest compile-container-fallbacks-test
  (let [bundle (lc/compile-tile-logic {:container {}})
        ^ITileContainerLogic c (.-container bundle)]
    (is (instance? ITileContainerLogic c))
    (is (= 0 (.getSize c nil)))
    (is (true? (.stillValid c nil nil)))
    (is (false? (.canPlaceItemThroughFace c nil 0 nil nil)))
    (is (true? (.canTakeItemThroughFace c nil 0 nil nil)))))

(deftest compile-tick-produces-tick-logic-test
  (let [cfg {:tick-fn (fn [_ _ _ _] nil)}
        a (lc/compile-tile-logic cfg)
        b (lc/compile-tile-logic cfg)]
    (is (instance? ITileTickLogic (.-tick a)))
    (is (instance? ITileTickLogic (.-tick b)))))

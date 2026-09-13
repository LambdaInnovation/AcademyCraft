(ns cn.li.mc1211.block.tick-microbench-test
  "Lightweight dispatch overhead sample (nanoTime). Not a CI gate."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.block.logic-compile :as lc])
  (:import [cn.li.mcbase.block.logic ITileTickLogic]))

(deftest bundle-tick-dispatch-overhead-smoke-test
  (let [volatile! (atom 0)
        bundle (lc/compile-tile-logic {:tick-fn (fn [_ _ _ _] (swap! volatile! inc))})
        ^ITileTickLogic tick (.-tick bundle)
        n 500
        t0 (System/nanoTime)]
    (dotimes [_ n] (.serverTick tick nil nil nil nil))
    (let [elapsed-ms (/ (- (System/nanoTime) t0) 1000000.0)]
      (is (= n @volatile!))
      (is (< elapsed-ms 5000.0) (str "tick dispatch took " elapsed-ms "ms for " n " calls")))))

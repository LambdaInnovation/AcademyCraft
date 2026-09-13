(ns cn.li.mcmod.aot-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.aot :as aot]))

(deftest compiling-detects-aot-binding
  (testing "*compile-files* marks compile context"
    (binding [*compile-files* true]
      (is (true? (aot/compiling?))))))

(deftest ensure-runtime-guards-compile-phase
  (testing "ensure-runtime! throws precise ex-info when compiling"
    (with-redefs [aot/compiling? (constantly true)
                  aot/compile-context (constantly {:compiling? true :aot true})]
      (let [ex (is (thrown? clojure.lang.ExceptionInfo
                            (aot/ensure-runtime! "unit-test")))]
        (is (= ::aot/compile-phase-violation (:type (ex-data ex))))
        (is (= "unit-test" (:who (ex-data ex))))))))

(deftest ensure-runtime-noop-at-runtime
  (testing "ensure-runtime! is no-op when not compiling"
    (with-redefs [aot/compiling? (constantly false)]
      (is (nil? (aot/ensure-runtime! "runtime-test"))))))

(deftest when-runtime-macro-behavior
  (testing "when-runtime only evaluates body when not compiling"
    (let [counter (atom 0)]
      (with-redefs [aot/compiling? (constantly true)]
        (aot/when-runtime (swap! counter inc))
        (is (= 0 @counter)))
      (with-redefs [aot/compiling? (constantly false)]
        (aot/when-runtime (swap! counter inc))
        (is (= 1 @counter))))))

(ns cn.li.mcmod.platform.world-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.platform.world :as world]))

(deftest world-op-nil-result-passthrough-test
  (testing "when world op exists and returns nil, wrapper returns nil as-is"
    (world/install-world-ops! {:world-get-tile-entity (fn [_ _] nil)} "test")
    (is (nil? (world/get-tile-entity (Object.) :pos)))))

(deftest world-op-false-result-passthrough-test
  (testing "when world op exists and returns false, wrapper returns false as-is"
    (world/install-world-ops! {:world-is-raining (fn [_] false)} "test")
    (is (false? (world/raining? (Object.))))))

(deftest missing-world-op-returns-nil-test
  (testing "wrapper returns nil (no throw) when the op key was not installed"
    (world/install-world-ops! {:world-is-raining (fn [_] false)} "test")
    (is (nil? (world/get-tile-entity (Object.) :pos)))))

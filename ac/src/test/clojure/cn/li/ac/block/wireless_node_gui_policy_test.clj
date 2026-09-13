(ns cn.li.ac.block.wireless-node-gui-policy-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-node.node-info-reactive :as node-info]))

(deftest node-info-area-policy-owner-matrix-test
  (testing "owner can edit both node-name and password"
    (is (= {:editable-node-name? true
            :editable-password? true}
           (#'node-info/node-info-area-policy true))))

  (testing "non-owner cannot edit protected fields"
    (is (= {:editable-node-name? false
            :editable-password? false}
           (#'node-info/node-info-area-policy false)))))

(deftest node-info-area-policy-safe-coercion-test
  (testing "policy fails closed for non-boolean truthy/falsey values"
    (is (= {:editable-node-name? true
            :editable-password? true}
           (#'node-info/node-info-area-policy :owner)))
    (is (= {:editable-node-name? false
            :editable-password? false}
           (#'node-info/node-info-area-policy nil)))))

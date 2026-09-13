(ns cn.li.ac.block.wireless-matrix-gui-policy-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-matrix.matrix-info-reactive :as matrix-info]))

(deftest matrix-info-area-policy-four-state-test
  (testing "initialized + owner"
    (is (= {:show-init? false
            :show-noinit? false
            :editable-ssid? true
            :editable-password? true}
           (matrix-info/matrix-info-area-policy true true))))

  (testing "initialized + non-owner"
    (is (= {:show-init? false
            :show-noinit? false
            :editable-ssid? false
            :editable-password? false}
           (matrix-info/matrix-info-area-policy true false))))

  (testing "uninitialized + owner"
    (is (= {:show-init? true
            :show-noinit? false
            :editable-ssid? false
            :editable-password? false}
           (matrix-info/matrix-info-area-policy false true))))

  (testing "uninitialized + non-owner"
    (is (= {:show-init? false
            :show-noinit? true
            :editable-ssid? false
            :editable-password? false}
           (matrix-info/matrix-info-area-policy false false)))))

(deftest matrix-info-area-policy-safe-defaults-test
  (testing "non-boolean inputs still fail closed"
    (is (= {:show-init? false
            :show-noinit? true
            :editable-ssid? false
            :editable-password? false}
           (matrix-info/matrix-info-area-policy nil nil)))
    (is (= {:show-init? false
            :show-noinit? false
            :editable-ssid? false
            :editable-password? false}
           (matrix-info/matrix-info-area-policy :initialized? nil)))))

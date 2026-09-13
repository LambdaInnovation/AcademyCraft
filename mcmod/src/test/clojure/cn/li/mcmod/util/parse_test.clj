(ns cn.li.mcmod.util.parse-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.util.parse :as parse]))

(deftest parse-core-test
  (testing "core parsing behavior for valid inputs"
    (is (= 42 (parse/parse-int "42")))
    (is (= 7 (parse/parse-int " 7 ")))
    (is (= 3.5 (parse/parse-float "3.5")))
    (is (= true (parse/parse-bool "TRUE")))
    (is (= false (parse/parse-bool "false")))
    (is (= 255 (parse/parse-color "#FF")))
    (is (= 16777215 (parse/parse-color "FFFFFF")))))

(deftest parse-edge-cases-test
  (testing "nil and invalid inputs use fallback semantics"
    (is (= nil (parse/parse-int nil)))
    (is (= 9 (parse/parse-int nil 9)))
    (is (= -1 (parse/parse-int "abc" -1)))
    (is (= 1.25 (parse/parse-float nil 1.25)))
    (is (= 2.5 (parse/parse-float "bad" 2.5)))
    (is (= nil (parse/parse-bool nil)))
    (is (= false (parse/parse-bool "anything-else")))
    (is (= 16777215 (parse/parse-color "bad-hex")))
    (is (= nil (parse/parse-color nil)))))

(deftest parse-contract-table-driven-test
  (testing "public parse contracts on representative data"
    (doseq [{:keys [f input expected]}
            [{:f parse/parse-int :input "0012" :expected 12}
             {:f parse/parse-int :input " 12 " :expected 12}
             {:f parse/parse-float :input "0.125" :expected 0.125}
             {:f parse/parse-bool :input " TrUe " :expected true}
             {:f parse/parse-color :input "#00ff00" :expected 65280}
             {:f parse/parse-color :input "00ff00" :expected 65280}]]
      (is (= expected (f input))))))

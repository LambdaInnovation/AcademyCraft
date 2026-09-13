(ns cn.li.ac.terminal.freq-transmitter-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.terminal.catalog :as catalog]))

(deftest freq-transmitter-in-catalog-test
  (let [app (catalog/app-by-id :freq-transmitter)]
    (is (= :freq-transmitter (:id app)))
    (is (= "Frequency Transmitter" (:name app)))
    (is (= :wireless (:category app)))))

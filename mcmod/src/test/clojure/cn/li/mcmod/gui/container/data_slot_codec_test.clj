(ns cn.li.mcmod.gui.container.data-slot-codec-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.gui.container.data-slot-codec :as codec]))

(deftest int-codec-roundtrip-test
  (let [c (codec/int-codec)]
    (is (= 42 (codec/decode-value c (codec/encode-value c 42))))))

(deftest boolean-codec-test
  (let [c (codec/boolean-codec)]
    (is (= true (codec/decode-value c 1)))
    (is (= false (codec/decode-value c 0)))))

(deftest scaled-double-codec-test
  (let [c (codec/scaled-double-codec {:scale 100})]
    (is (= 1.23 (codec/decode-value c (codec/encode-value c 1.23))))))

(deftest status-codec-test
  (let [c (codec/string-status-codec ["STOPPED" "WEAK" "STRONG"])]
    (is (= "STRONG" (codec/decode-value c (codec/encode-value c "STRONG"))))
    (is (= "WEAK" (codec/decode-value c (codec/encode-value c :WEAK))))))

(deftest codec-for-field-status-codes-test
  (let [field {:key :status
               :gui-data-slot? true
               :gui-data-slot-status-codes ["STOPPED" "WEAK" "STRONG"]}
        c (codec/codec-for-gui-field field)]
    (is (some? c))
    (is (= "WEAK" (codec/decode-value c (codec/encode-value c "WEAK"))))))

(deftest keyword-enum-codec-test
  (let [c (codec/keyword-enum-codec [:basic :advanced])]
    (is (= :advanced (codec/decode-value c (codec/encode-value c :advanced))))))

(ns cn.li.mcmod.network.binary-codec-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.network.binary-codec :as codec])
  (:import [cn.li.mcmod.math V3]))

(defn- roundtrip [v]
  (codec/decode (codec/encode v)))

(deftest scalar-roundtrip-test
  (testing "nil/bool"
    (is (nil? (roundtrip nil)))
    (is (= false (roundtrip false)))
    (is (= true (roundtrip true))))
  (testing "integers"
    (is (= 0 (roundtrip 0)))
    (is (= 42 (roundtrip 42)))
    (is (= -1 (roundtrip -1)))
    (is (= Long/MAX_VALUE (roundtrip Long/MAX_VALUE)))
    (is (= Long/MIN_VALUE (roundtrip Long/MIN_VALUE))))
  (testing "floating point"
    (is (= 1.5 (roundtrip 1.5)))
    (is (= 0.0 (roundtrip 0.0)))
    (is (= -3.25 (roundtrip (float -3.25)))))
  (testing "strings incl. unicode and empty"
    (is (= "" (roundtrip "")))
    (is (= "hello" (roundtrip "hello")))
    (is (= "你好，世界" (roundtrip "你好，世界"))))
  (testing "keywords incl. namespaced"
    (is (= :foo (roundtrip :foo)))
    (is (= :my-mod/skill-id (roundtrip :my-mod/skill-id)))))

(deftest string-over-64kb-roundtrip-test
  (let [big (apply str (repeat 100000 "a"))]
    (is (> (count big) 65536))
    (is (= big (roundtrip big)))))

(deftest collection-roundtrip-test
  (testing "vector"
    (is (= [1 "a" :b nil true] (roundtrip [1 "a" :b nil true]))))
  (testing "set"
    (is (= #{1 2 3} (roundtrip #{1 2 3}))))
  (testing "map with keyword keys"
    (is (= {:a 1 :b {:c [1 2 3]}} (roundtrip {:a 1 :b {:c [1 2 3]}}))))
  (testing "nested ability-shaped payload"
    (let [payload {:msg-id :runtime-sync
                   :payload {:uuid "abc-123"
                             :domains #{:cooldowns :resources}
                             :cooldowns {:skill-a 20 :skill-b 0}
                             :resources {:mana 45.5 :stamina 100.0}
                             :flags [true false nil]}}]
      (is (= payload (roundtrip payload))))))

(deftest v3-roundtrip-test
  (let [v (V3. 1.0 -2.5 3.75)
        decoded (roundtrip v)]
    (is (instance? V3 decoded))
    (is (= v decoded))))

(deftest unsupported-type-throws-test
  (is (thrown? clojure.lang.ExceptionInfo (codec/encode (Object.)))))

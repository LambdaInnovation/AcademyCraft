(ns cn.li.mc1201.gui.network.packet-bytes-codec-test
  "Wire-level roundtrip coverage for the binary-codec-backed payload boundary.
  Kept separate from packet_test.clj, which depends on a client-runtime test
  fixture unrelated to codec correctness."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.gui.network.packet :as packet]))

(deftest encode-decode-payload-bytes-roundtrip-test
  (let [payload {:msg-id "runtime-sync" :payload {:uuid "abc" :cooldowns {:a 1 :b 2} :flags #{:x :y}}}
        bs (packet/encode-payload-bytes payload)]
    (is (bytes? bs))
    (is (= payload (packet/decode-payload-bytes bs nil)))))

(deftest decode-payload-bytes-corrupt-input-calls-on-error-and-falls-back-to-empty-map-test
  (let [errors (atom [])
        bs (byte-array [99])] ;; unknown wire tag
    (is (= {} (packet/decode-payload-bytes bs #(swap! errors conj %))))
    (is (= 1 (count @errors)))))

(deftest decode-payload-bytes-nil-input-falls-back-to-empty-map-test
  (is (= {} (packet/decode-payload-bytes nil nil))))

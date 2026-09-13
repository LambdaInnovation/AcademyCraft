(ns cn.li.mc1211.runtime.sync-codec-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.runtime.sync-codec :as codec]))

(deftest full-frame-roundtrip-test
  (let [payload {:version 2
                 :opcode 1
                 :uuid "123e4567-e89b-12d3-a456-426614174000"
                 :revision 42
                 :dirty-mask 0x1f
                 :ability-data {:level 3 :learned #{:railgun}}
                 :resource-data {:cp 12.5 :overload 2.0}
                 :cooldown-data {:railgun 18}
                 :preset-data {:selected 2 :slots [1 2 3]}
                 :develop-data {:exp 99}}
        encoded (codec/encode-bytes payload)]
    (is (codec/runtime-sync-bytes? encoded))
    (is (= payload (codec/decode-bytes encoded)))))

(deftest delta-frame-omits-clean-domains-test
  (let [decoded (codec/decode-bytes
                  (codec/encode-bytes
                    {:version 2 :opcode 2 :uuid "player" :revision 7
                     :dirty-mask 0x02 :resource-data {:cp 1.0}}))]
    (is (= {:version 2 :opcode 2 :uuid "player" :revision 7
            :dirty-mask 0x02 :resource-data {:cp 1.0}}
           decoded))
    (is (not (contains? decoded :ability-data)))))

(deftest invalid-header-rejected-test
  (testing "unknown dirty bits cannot desynchronize field ordering"
    (is (thrown? clojure.lang.ExceptionInfo
                 (codec/encode-bytes
                   {:version 2 :opcode 2 :uuid "player" :revision 0
                    :dirty-mask 0x20}))))
  (testing "dirty bit without domain value is rejected instead of encoding {}"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Runtime sync domain missing for dirty bit"
                          (codec/encode-bytes
                            {:version 2 :opcode 1 :uuid "player" :revision 0
                             :dirty-mask 0x1f
                             :ability-data {}
                             :resource-data {}
                             :cooldown-data {}
                             :preset-data {}})))))

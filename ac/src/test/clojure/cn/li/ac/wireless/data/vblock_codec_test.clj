(ns cn.li.ac.wireless.data.vblock-codec-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.foundation.vblock :as foundation-vb]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.test.support.nbt :as test-nbt]
            [cn.li.ac.wireless.core.vblock :as runtime-vb]
            [cn.li.ac.wireless.data.vblock-codec :as codec]))

(use-fixtures :each support-fw/with-fresh-framework)

(deftest vblock-compound-roundtrip-test
  (test-nbt/install-test-structured-data-ops!)
  (testing "data codec round-trips pure foundation vblocks"
    (let [source (foundation-vb/vblock 10 20 30 :receiver true)
          decoded (codec/vblock-from-nbt (codec/vblock-to-nbt source))]
      (is (= source decoded)))))

(deftest vblock-list-roundtrip-test
  (test-nbt/install-test-structured-data-ops!)
  (let [source [(runtime-vb/create-vnode 1 2 3)
                (runtime-vb/create-vmatrix 4 5 6)]
        decoded (codec/nbt-list->vblocks
                  (codec/vblocks-to-nbt-list source)
                  :node
                  false
                  runtime-vb/from-foundation)]
    (is (= source decoded))))

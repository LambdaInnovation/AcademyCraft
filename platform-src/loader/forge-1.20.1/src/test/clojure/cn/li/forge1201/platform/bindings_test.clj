(ns cn.li.forge1201.platform.bindings-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.forge1201.platform.bindings :as bindings]
            [cn.li.mcmod.protocol.metadata :as registry-metadata]))

(deftest block-id-candidates-test
  (testing "keeps the DSL id and appends the registry name when available"
    (with-redefs [registry-metadata/get-block-registry-name (constantly "cn.li:test_block")]
      (is (= #{"example:block" "cn.li:test_block"}
             (set (#'bindings/block-id-candidates "example:block"))))))
  (testing "falls back to the DSL id when no registry name exists"
    (with-redefs [registry-metadata/get-block-registry-name (constantly nil)]
      (is (= ["example:block"]
             (vec (#'bindings/block-id-candidates "example:block")))))))

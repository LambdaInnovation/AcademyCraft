(ns cn.li.mcmod.block.blockstate-definition-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.block.blockstate-definition :as sut]
            [cn.li.mcmod.config :as config]
            [cn.li.mcmod.protocol.metadata :as metadata]))

(deftest default-definition-generation-test
  (with-redefs [metadata/get-all-block-ids (fn [] ["demo-block"])
                metadata/get-block-registry-name (fn [_] "demo_block")
                config/mod-id "academy"]
    (let [all-defs (#'sut/basic-get-all-definitions)
          definition (get all-defs :demo-block)]
      (testing "basic definitions are generated from metadata"
        (is (= "demo_block" (:registry-name definition)))
        (is (= {} (:properties definition)))
        (is (= ["academy:block/demo_block"]
               (-> definition :parts first :models))))
      (testing "lookup supports keyword and stringy keys"
        (is (= definition (#'sut/basic-get-block-state-definition :demo-block)))
        (is (= definition (#'sut/basic-get-block-state-definition "demo-block")))))))

(deftest multipart-and-item-model-helpers-test
  (testing "multipart helper checks part count"
    (is (false? (#'sut/basic-is-multipart-block? {:parts [{}]})))
    (is (true? (#'sut/basic-is-multipart-block? {:parts [{} {}]}))))
  (testing "item model id helper keeps naming contract"
    (is (= "ac:block/wireless_node"
           (#'sut/basic-get-item-model-id "ac" "wireless_node")))))

(deftest hooks-dispatch-test
  (let [original-hooks (sut/blockstate-hooks-snapshot)]
    (try
      (sut/register-blockstate-hooks!
        {:get-all-definitions (fn [] {:k :v})
         :get-block-state-definition (fn [k] {:block k})
         :is-multipart-block? (fn [_] :mp)
         :get-model-cube-texture-config (fn [m] [:cube m])
         :get-model-texture-config (fn [m] [:tex m])
         :get-item-model-id (fn [mod-id registry-name] (str mod-id "/" registry-name))})
      (testing "public APIs dispatch through registered hooks"
        (is (= {:k :v} (sut/get-all-definitions)))
        (is (= {:block :b} (sut/get-block-state-definition :b)))
        (is (= :mp (sut/is-multipart-block? {})))
        (is (= [:cube "a"] (sut/get-model-cube-texture-config "a")))
        (is (= [:tex "b"] (sut/get-model-texture-config "b")))
        (is (= "m/r" (sut/get-item-model-id "m" "r"))))
      (finally
        (sut/restore-blockstate-hooks! original-hooks)))))


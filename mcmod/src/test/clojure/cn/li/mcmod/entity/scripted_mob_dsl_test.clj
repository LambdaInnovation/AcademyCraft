(ns cn.li.mcmod.entity.scripted-mob-dsl-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcmod.entity.dsl :as edsl]))

(deftest scripted-mob-requires-mob-props-test
  (is (thrown-with-msg? Exception #":properties"
        (edsl/register-entity!
          (edsl/create-entity-spec "test-mob-missing"
            {:entity-kind :scripted-mob
             :properties {}})))))

(deftest scripted-mob-spec-valid-test
  (let [spec (edsl/create-entity-spec "test-mob-valid"
               {:entity-kind :scripted-mob
                :properties {:mob {:mob-tick-fn (fn [_] nil)}}})]
    (is (= :scripted-mob (:entity-kind spec)))
    (is (fn? (get-in spec [:properties :mob :mob-tick-fn])))))

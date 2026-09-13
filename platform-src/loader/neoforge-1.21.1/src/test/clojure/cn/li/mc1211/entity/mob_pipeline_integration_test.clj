(ns cn.li.mc1211.entity.mob-pipeline-integration-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.entity.mob-logic-pipeline :as mob-pipeline]
            [cn.li.mcmod.entity.dsl :as edsl]))

(deftest compile-all-mob-bundles-includes-scripted-mob-test
  (edsl/register-entity!
    (edsl/create-entity-spec "mob-pipeline-probe"
      {:entity-kind :scripted-mob
       :properties {:mob {:mob-tick-fn (fn [_] :tick)}}}))
  (let [bundles (mob-pipeline/compile-all-mob-bundles)]
    (is (contains? bundles "mob-pipeline-probe"))
    (is (some? (.-tick (get bundles "mob-pipeline-probe"))))))

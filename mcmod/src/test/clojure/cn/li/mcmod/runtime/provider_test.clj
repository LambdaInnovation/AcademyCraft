(ns cn.li.mcmod.runtime.provider-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.runtime.provider :as provider]))

(defn test-factory [context]
  {:run! (fn [value] [(:side context) value])})

(def ^:private descriptor
  {:id "test-provider"
   :side "client"
   :namespace "cn.li.mcmod.runtime.provider-test"
   :function "test-factory"
   :provides ["run!"]})

(deftest provider-loads-concrete-functions-test
  (let [fw-atom (atom {:service {} :platform {}})]
    (is (nil? (provider/load-provider! fw-atom descriptor {:target {:id "test" :loader "none" :minecraft-version "test"}})))
    (is (= [:client :payload]
           ((provider/provider-op! fw-atom :test-provider :run!) :payload)))))

(deftest provider-rejects-missing-host-port-test
  (let [fw-atom (atom {:service {} :platform {}})
        required (assoc descriptor :id "requires-host"
                        :required-host-ports [["world" "get!"]])]
    (is (thrown? clojure.lang.ExceptionInfo
                 (provider/load-provider! fw-atom required {:target {:id "test" :loader "none" :minecraft-version "test"}})))))

(deftest provider-operation-is-not-a-hot-dispatch-test
  (testing "callers receive the cached IFn once and invoke it directly"
    (let [fw-atom (atom {:service {} :platform {}})]
      (provider/load-provider! fw-atom descriptor {:target {:id "test" :loader "none" :minecraft-version "test"}})
      (let [run! (provider/provider-op! fw-atom "test-provider" "run!")]
        (is (= [:client 42] (run! 42)))))))

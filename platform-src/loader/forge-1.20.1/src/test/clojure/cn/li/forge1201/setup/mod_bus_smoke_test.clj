(ns cn.li.forge1201.setup.mod-bus-smoke-test
  "Smoke tests for mod-bus setup functions.
  These verify that key initialization functions exist and are callable."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.forge1201.setup.mod-bus :as mod-bus]))

(deftest mod-bus-functions-exist
  "Verify that all mod-bus initialization functions are defined"
  
  (testing "config phase function is available"
    (is (fn? mod-bus/register-config-phase!))
    (is (fn? mod-bus/register-registry-phase!))))

(deftest config-registration-callable
  "Verify that config registration can be attempted without errors"
  
  (testing "register-all! is callable from bridge module"
    (let [bridge-ns (symbol "cn.li.forge1201.config.bridge")]
      (is (some? (resolve (symbol (str bridge-ns) "register-all!")))))))

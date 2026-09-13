(ns cn.li.neoforge262.setup.lifecycle-listeners-smoke-test
  "Smoke tests for lifecycle listener setup."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.neoforge262.setup.lifecycle-listeners :as listeners]))

(deftest lifecycle-listener-functions-exist
  (testing "client hooks registration exists"
    (is (fn? listeners/register-client-hooks!))))

(deftest listener-registration-callability
  (testing "client hooks can be registered without errors"
    (is (nil? (listeners/register-client-hooks!))
        "register-client-hooks! should complete without error")))

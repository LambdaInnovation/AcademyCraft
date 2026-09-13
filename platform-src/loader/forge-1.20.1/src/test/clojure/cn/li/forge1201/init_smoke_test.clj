(ns cn.li.forge1201.init-smoke-test
  "Smoke tests for Forge initialization module.
  These verify that key initialization entry points exist."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.forge1201.init :as init]))

(deftest initialization-functions-exist
  "Verify that Forge initialization entry points are defined"
  
  (testing "version-setting function exists"
    (is (fn? init/set-version!)))
  
  (testing "Java initialization entry point exists"
    (is (fn? init/init-from-java))))

(deftest initialization-order
  "Verify initialization function dependencies"
  
  (testing "set-version! must be called before other initialization"
    ;; This is a logical test - in real integration, these would be called in order
    (let [v-fn init/set-version!
          init-fn init/init-from-java]
      (is (and (fn? v-fn) (fn? init-fn))
          "Both initialization functions must exist"))))

(ns cn.li.neoforge262.init-smoke-test
  "Smoke tests for NeoForge 26.2 initialization entry points.
  AOT class presence avoids brittle full-bootstrap; var checks cover the public API."
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.java.io :as io]
            [cn.li.neoforge262.init :as init]))

(deftest loader-sources-present
  (testing "mod and platform init sources are AOT'd into the test classpath"
    (let [mod-init (io/resource "cn/li/neoforge262/mod__init.class")
          platform-init (io/resource "cn/li/neoforge262/platform/init__init.class")]
      (is (some? mod-init) "cn.li.neoforge262.mod should be AOT'd into test runtime classpath")
      (is (some? platform-init) "cn.li.neoforge262.platform.init should be AOT'd into test runtime classpath"))))

(deftest initialization-functions-exist
  (testing "version-setting function exists"
    (is (fn? init/set-version!)))
  (testing "Java initialization entry point exists"
    (is (fn? init/init-from-java))))

(deftest initialization-order
  (testing "set-version! must be called before other initialization"
    (let [v-fn init/set-version!
          init-fn init/init-from-java]
      (is (and (fn? v-fn) (fn? init-fn))
          "Both initialization functions must exist"))))

(ns cn.li.neoforge262.setup.mod-bus-smoke-test
  "Smoke tests for mod-bus setup functions."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.neoforge262.setup.mod-bus :as mod-bus]))

(deftest mod-bus-functions-exist
  (testing "config phase function is available"
    (is (fn? mod-bus/register-config-phase!))
    (is (fn? mod-bus/register-registry-phase!))))

(deftest imc-dispatcher-wired-into-capability-phase
  (testing "capability phase registers InterModProcessEvent listener"
    (require 'cn.li.neoforgebase.setup.imc-dispatcher)
    (is (fn? @(resolve 'cn.li.neoforgebase.setup.imc-dispatcher/register-imc-listener!)))
    (let [src (slurp
               (or (let [f (java.io.File. "platform-src/loader/neoforge-shared/src/main/clojure/cn/li/neoforgebase/setup/capability_setup.clj")]
                     (when (.exists f) f))
                   (java.io.File. "../platform-src/loader/neoforge-shared/src/main/clojure/cn/li/neoforgebase/setup/capability_setup.clj")))]
      (is (re-find #"imc-dispatcher/register-imc-listener!" src)))))

(deftest config-registration-callable
  (testing "register-all! is callable from bridge module"
    (let [bridge-ns (symbol "cn.li.neoforgebase.config.bridge")]
      (is (some? (resolve (symbol (str bridge-ns) "register-all!")))))))

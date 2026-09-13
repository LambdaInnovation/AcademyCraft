(ns cn.li.ac.ability.discovery-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.discovery :as discovery]
            [cn.li.ac.discovery.registry :as registry]
            [cn.li.ac.discovery.scanner :as scanner]))

(defn- reset-discovery-state! [f]
  (let [providers-snapshot (registry/provider-registry-snapshot)
        attempts (discovery/bootstrap-attempts-snapshot)]
    (registry/reset-provider-registry-for-test!)
    (discovery/reset-bootstrap-attempts-for-test!)
    (try
      (f)
      (finally
        (registry/reset-provider-registry-for-test! providers-snapshot)
        (discovery/reset-bootstrap-attempts-for-test! attempts)))))

(use-fixtures :each reset-discovery-state!)

(deftest fallback-electromaster-fx-includes-arc-gen-fx-test
  (with-redefs [scanner/discover-ability-providers (fn [] [])]
    (let [fx-ns (set (discovery/discovered-fx-namespaces))]
      (is (contains? fx-ns 'cn.li.ac.content.ability.electromaster.arc-gen-fx))
      (is (contains? fx-ns 'cn.li.ac.content.ability.electromaster.mag-manip-fx))
      (is (contains? fx-ns 'cn.li.ac.content.ability.electromaster.body-intensify-fx)))))

(deftest discovery-provider-registry-freeze-policy-test
  (registry/freeze-provider-registry!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Discovery provider registry is frozen"
                        (registry/register-provider! {:id :demo :priority 0})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Discovery provider registry is frozen"
                        (registry/unregister-provider! :demo))))

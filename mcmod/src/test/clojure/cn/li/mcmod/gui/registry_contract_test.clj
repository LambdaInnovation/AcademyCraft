(ns cn.li.mcmod.gui.registry-contract-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.network.server :as net-server]))

(deftest handler-contract-validation-test
  (testing "default server GUI contract"
    (is (= :server (:owner-spec (registry-contract/validate-handler-contract!)))))
  (testing "client owner-spec fails for server handlers"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Server GUI handler contract requires :owner-spec :server"
                          (registry-contract/validate-handler-contract!
                           {:owner-spec :client})))))

(deftest screen-contract-validation-test
  (testing "client screen contract passes"
    (is (= :client
           (:owner-spec (registry-contract/validate-screen-contract!
                         {:owner-spec :client})))))
  (testing "server screen contract fails"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"requires :owner-spec :client"
                          (registry-contract/validate-screen-contract!
                           {:owner-spec :server})))))

(deftest sync-routing-contract-test
  (testing "container-id routing passes"
    (is (registry-contract/require-sync-routing {:container-id 17})))
  (testing "pos-only routing fails"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"sync-routing contract violation"
                          (registry-contract/require-sync-routing {:pos-x 1 :pos-y 2 :pos-z 3}))))
  (testing "empty routing fails"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"sync-routing contract violation"
                          (registry-contract/require-sync-routing {})))))

(deftest verify-catalog-handlers-test
  (let [catalog {:specs [{:domain :node :action :change-name :msg-id "wireless_node_change_name"}]}
        contracts {:node {:owner-spec :server :payload-routing :sync-routing}}]
    (try
      (net-server/reset-handlers-for-test!)
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"No network handler registered"
                            (registry-contract/verify-catalog-handlers! {} catalog contracts)))
      (net-server/register-handler "wireless_node_change_name" (fn [_ _] {}) contracts)
      (is (nil? (registry-contract/verify-catalog-handlers!
                 (:handlers (net-server/handlers-snapshot))
                 catalog
                 contracts)))
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Handler contract does not match"
                            (do
                              (net-server/reset-handlers-for-test!)
                              (net-server/register-handler "wireless_node_change_name"
                                                             (fn [_ _] {})
                                                             {:owner-spec :server
                                                              :payload-routing :none})
                              (registry-contract/verify-catalog-handlers!
                               (:handlers (net-server/handlers-snapshot))
                               catalog
                               contracts))))
      (finally
        (net-server/reset-handlers-for-test!)))))

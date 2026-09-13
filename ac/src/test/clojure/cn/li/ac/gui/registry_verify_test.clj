(ns cn.li.ac.gui.registry-verify-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.registry-verify :as registry-verify]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.gui.message.bootstrap :as shared-registry]
            [cn.li.mcmod.network.server :as net-server]))

(defn- reset-registries! [f]
  (let [msg-snapshot (msg-registry/registry-snapshot)]
    (net-server/reset-handlers-for-test!)
    (msg-registry/reset-registry-for-test!)
    (try
      (f)
      (finally
        (net-server/reset-handlers-for-test!)
        (msg-registry/reset-registry-for-test! msg-snapshot)))))

(use-fixtures :each (fn [f] (support-fw/with-fresh-framework #(reset-registries! f))))

(deftest message-domain-contract-manifest-test
  (testing "every declared domain has a registry contract"
    (is (= (set (keys gui-manifest/message-domain-actions))
           (set (keys gui-manifest/message-domain-contracts))))))

(deftest verify-wireless-handler-registration-test
  (let [contract (gui-manifest/message-domain-contract :node)]
    (msg-registry/register-block-messages! :node [:change-name] contract)
    (net-server/register-handler "wireless_node_change_name" (fn [_ _] {}) contract)
    (is (nil? (registry-verify/verify-wireless-message-handler-registration!)))))

(deftest finalize-freezes-registries-test
  (let [contract (gui-manifest/message-domain-contract :developer)]
    (msg-registry/register-block-messages! :developer [:start-development] contract)
    (net-server/register-handler "wireless_developer_start_development" (fn [_ _] {}) contract)
    (registry-verify/finalize-gui-network-registration!)
    (is (true? (:frozen? (net-server/handlers-snapshot))))
    (is (true? (:frozen? (msg-registry/registry-snapshot))))))

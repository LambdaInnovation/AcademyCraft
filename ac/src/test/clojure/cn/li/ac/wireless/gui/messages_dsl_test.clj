(ns cn.li.ac.wireless.gui.messages-dsl-test
  "Tests for wireless GUI message DSL and catalog integration."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.gui.message.dsl :as msg-dsl]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.test.support.framework :as support-fw]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.wireless.gui.message.api :as wireless-msgs]
            [cn.li.ac.wireless.gui.message.bootstrap :as shared-registry]))

(use-fixtures :each support-fw/with-fresh-framework)

(def expected-node-actions
  #{:change-name :change-password :list-networks :connect :disconnect})

(def expected-matrix-actions
  #{:gather-info :init :change-ssid :change-password})

(def ^:private wireless-prefix "wireless")

(deftest message-id-format-test
  (is (= "wireless_node_change_name" (msg-dsl/message-id wireless-prefix :node :change-name)))
  (is (= "wireless_matrix_gather_info" (msg-dsl/message-id wireless-prefix :matrix :gather-info))))

(deftest domain-spec-validation-test
  (testing "build-domain-spec constructs expected map"
    (let [spec (msg-dsl/build-domain-spec wireless-prefix :demo [:alpha :beta]
                                          {:owner-spec :server :payload-routing :sync-routing})]
      (is (= :demo (:domain spec)))
      (is (= :server (get-in spec [:contract :owner-spec])))
      (is (= "wireless_demo_alpha" (get-in spec [:messages :alpha])))
      (is (= 2 (count (:specs spec))))))
  (testing "duplicate actions are rejected"
    (is (thrown-with-msg?
          clojure.lang.ExceptionInfo
          #"Duplicate actions"
          (msg-dsl/build-domain-spec wireless-prefix :dup [:x :x])))))

(deftest catalog-validation-test
  (testing "cross-domain duplicate message ids are rejected"
    (let [s1 {:domain :x
              :messages {:a "wireless_collision_id"}
              :specs [{:domain :x :action :a :msg-id "wireless_collision_id"}]}
          s2 {:domain :y
              :messages {:b "wireless_collision_id"}
              :specs [{:domain :y :action :b :msg-id "wireless_collision_id"}]}]
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Duplicate message ids"
            (msg-dsl/build-catalog [s1 s2])))))
  (testing "naming format is enforced"
    (let [bad {:domain :z
               :messages {:a "UPPERCASE_ID"}
               :specs [{:domain :z :action :a :msg-id "UPPERCASE_ID"}]}]
      (is (thrown-with-msg?
            clojure.lang.ExceptionInfo
            #"Invalid message ids"
            (msg-dsl/build-catalog [bad]))))))

(deftest wireless-message-integration-test
  (shared-registry/register-all!)
  (let [catalog (wireless-msgs/catalog)
        node-spec (msg-registry/get-domain-spec :node)
        matrix-spec (msg-registry/get-domain-spec :matrix)]
    (testing "required domains are registered"
      (is (some? node-spec))
      (is (some? matrix-spec))
      (is (contains? (:domains catalog) :node))
      (is (contains? (:domains catalog) :matrix))
      (is (empty? (gui-manifest/missing-message-domains [:node :matrix :generator
                                                         :metal-former
                                                         :developer :ability-interferer]))))

    (testing "registered actions match expected sets"
      (is (= expected-node-actions (set (keys (:messages node-spec)))))
      (is (= expected-matrix-actions (set (keys (:messages matrix-spec)))))
      (is (= (set (gui-manifest/message-actions :metal-former))
             (set (keys (:messages (msg-registry/get-domain-spec :metal-former)))))))

    (testing "catalog lookups are bidirectional"
      (is (= "wireless_node_change_name" (wireless-msgs/msg :node :change-name)))
      (is (= "wireless_matrix_init" (wireless-msgs/msg :matrix :init)))
      (is (= {:domain :node :action :change-name}
             (wireless-msgs/find-by-msg-id "wireless_node_change_name")))
    )
  )
)

(deftest wireless-message-registry-duplicate-and-freeze-policy-test
  (let [snapshot (msg-registry/registry-snapshot)]
    (try
      (msg-registry/reset-registry-for-test!)
      (let [spec (msg-registry/register-block-messages! :demo [:alpha])]
        (is (= spec (msg-registry/register-block-messages! :demo [:alpha])))
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Conflicting wireless GUI message domain"
                              (msg-registry/register-block-messages! :demo [:beta])))
        (msg-registry/freeze-registry!)
        (is (thrown-with-msg? clojure.lang.ExceptionInfo
                              #"Wireless GUI message registry is frozen"
                              (msg-registry/register-block-messages! :new-domain [:x]))))
      (finally
        (msg-registry/reset-registry-for-test! snapshot)))))

(ns cn.li.neoforge262.config.bridge-smoke-test
  "Basic smoke tests for config bridge module functions.
  These tests verify that config functions can be called without errors."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.config :as ability]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.gameplay :as gameplay]))

(deftest gameplay-descriptors-are-ac-owned
  (testing "AC gameplay descriptors include platform-supported UI primitive types"
    (is (seq gameplay/descriptors))
    (is (some #(= :boolean (:type %)) gameplay/descriptors))
    (is (some #(= :string (:type %)) gameplay/descriptors))))

(deftest ability-descriptors-are-ac-owned
  (testing "AC ability descriptors include platform-supported primitive and list types"
    (is (seq ability/descriptors))
    (is (some #(= :boolean (:type %)) ability/descriptors))
    (is (some #(= :string-list (:type %)) ability/descriptors))
    (is (some #(= :double-list (:type %)) ability/descriptors))))

(deftest ability-domain-builds-forge-external-file-contract
  (testing "ability domain maps to the player-facing Forge TOML file"
    (let [domain->file-name @(ns-resolve 'cn.li.neoforgebase.config.bridge 'domain->file-name)
          build-domain-spec @(ns-resolve 'cn.li.neoforgebase.config.bridge 'build-domain-spec)
          domain-info (build-domain-spec config-common/ability-domain ability/descriptors)]
      (is (= "academy/cn.li.ac-ability.toml"
             (domain->file-name config-common/ability-domain ".toml")))
      (is (= config-common/ability-domain (:domain domain-info)))
      (is (= "academy/cn.li.ac-ability.toml" (:file-name domain-info)))
      (is (= (set (keys ability/default-values))
             (set (keys (:entries domain-info))))))))

(deftest split-player-config-domains-build-expected-forge-file-names
  (testing "wireless/ability split domains map to stable player-facing TOML files"
    (let [domain->file-name @(ns-resolve 'cn.li.neoforgebase.config.bridge 'domain->file-name)
          expected {config-common/wireless-domain "academy/cn.li.ac-wireless.toml"
                    config-common/wireless-devices-domain "academy/cn.li.ac-wireless-devices.toml"
                    config-common/gameplay-domain "academy/cn.li.ac-gameplay.toml"
                    config-common/ability-domain "academy/cn.li.ac-ability.toml"
                    config-common/ability-devices-domain "academy/cn.li.ac-ability-devices.toml"}]
      (doseq [[domain file-name] expected]
        (is (= file-name (domain->file-name domain ".toml"))))
      (doseq [category-id skill-config/category-ids]
        (is (= (str "academy/cn.li.ac-ability-skills-" (name category-id) ".toml")
               (domain->file-name (config-common/ability-skill-category-domain category-id)
                                  ".toml")))))))

(deftest config-storage-is-initialized
  (testing "private registered-config storage defaults to a map"
    (let [registered-configs-var (ns-resolve 'cn.li.neoforgebase.config.bridge 'registered-configs)]
      (is (some? registered-configs-var))
      (is (map? @(var-get registered-configs-var))))))

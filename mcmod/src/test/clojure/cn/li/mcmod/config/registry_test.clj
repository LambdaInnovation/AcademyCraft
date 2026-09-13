(ns cn.li.mcmod.config.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.config.registry :as registry]))

(defn- reset-registries! []
  (registry/set-descriptor-registry! {})
  (registry/set-value-registry! {}))

(deftest register-descriptors-validation-test
  (reset-registries!)
  (testing "domain must be keyword"
    (is (thrown? clojure.lang.ExceptionInfo
                 (registry/register-config-descriptors! "bad" []))))
  (testing "descriptor keys must be keywords and unique"
    (is (thrown? clojure.lang.ExceptionInfo
                 (registry/register-config-descriptors! :d [{:key "x"}])))
    (is (thrown? clojure.lang.ExceptionInfo
                 (registry/register-config-descriptors! :d [{:key :x} {:key :x}])))))

(deftest defaults-and-values-merge-test
  (reset-registries!)
  (registry/register-config-descriptors!
    :gameplay
    [{:key :limit :default 5}
     {:key :enabled :default true}])
  (testing "domain descriptors and default values are discoverable"
    (is (= [:gameplay] (vec (registry/get-all-config-domains))))
    (is (= {:limit 5 :enabled true}
           (registry/descriptor-default-values :gameplay))))
  (testing "ensure-default-values! only seeds missing values"
    (registry/ensure-default-values! :gameplay {:limit 8 :enabled false})
    (registry/ensure-default-values! :gameplay {:limit 9 :enabled true})
    (is (= {:limit 8 :enabled false}
           (get (registry/get-value-registry) :gameplay))))
  (testing "set-config-values! preserves defaults for missing keys"
    (registry/set-config-values! :gameplay {:limit 12})
    (is (= {:limit 12 :enabled true}
           (registry/get-config-values :gameplay)))
    (is (= 12 (registry/get-config-value :gameplay :limit)))
    (is (= :fallback (registry/get-config-value :gameplay :missing :fallback)))))

(deftest cross-domain-and-unregistered-domain-test
  (reset-registries!)
  (registry/register-config-descriptors!
    :domain-a
    [{:key :a1 :default 1}
     {:key :shared :default :a}])
  (registry/register-config-descriptors!
    :domain-b
    [{:key :b1 :default 2}
     {:key :shared :default :b}])
  (testing "empty descriptor domain is allowed and returns empty defaults"
    (registry/register-config-descriptors! :empty [])
    (is (= [] (registry/get-config-descriptors :empty)))
    (is (= {} (registry/descriptor-default-values :empty))))
  (testing "unregistered domain returns empty values/default fallback"
    (is (= [] (registry/get-config-descriptors :missing)))
    (is (= {} (registry/get-config-values :missing)))
    (is (nil? (registry/get-config-value :missing :any)))
    (is (= :d (registry/get-config-value :missing :any :d))))
  (testing "cross-domain values remain isolated"
    (registry/set-config-values! :domain-a {:a1 10})
    (registry/set-config-values! :domain-b {:b1 20})
    (is (= {:a1 10 :shared :a} (registry/get-config-values :domain-a)))
    (is (= {:b1 20 :shared :b} (registry/get-config-values :domain-b)))))

(deftest value-precedence-order-test
  (reset-registries!)
  (registry/register-config-descriptors!
    :order
    [{:key :x :default 1}
     {:key :y :default 2}])
  (testing "ensure-default-values! establishes initial runtime values"
    (registry/ensure-default-values! :order {:x 8 :y 9})
    (is (= {:x 8 :y 9} (get (registry/get-value-registry) :order))))
  (testing "set-config-values! overrides runtime and fills missing from descriptor defaults"
    (registry/set-config-values! :order {:x 100})
    (is (= {:x 100 :y 2} (registry/get-config-values :order))))
  (testing "ensure-default-values! does not overwrite existing runtime values after set"
    (registry/ensure-default-values! :order {:x 7 :y 7})
    (is (= {:x 100 :y 2} (registry/get-config-values :order)))))


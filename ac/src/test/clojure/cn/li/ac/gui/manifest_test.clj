(ns cn.li.ac.gui.manifest-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.gui.manifest :as manifest]))

(def expected-gui-keys
  #{:wireless-node
    :wireless-matrix
    :solar-gen
    :wind-gen-main
    :wind-gen-base
    :imag-fusor
    :metal-former
    :phase-gen
    :developer
    :energy-converter
    :ability-interferer})

(def required-registration-keys
  #{:gui-id
    :display-name
    :gui-type
    :registry-name
    :screen-factory-fn-kw
    :slot-schema-id})

(defn- duplicates
  [xs]
  (->> xs
       frequencies
       (keep (fn [[x n]] (when (> n 1) x)))
       set))

(deftest gui-definition-catalog-test
  (testing "manifest declares every AC block GUI key"
    (is (= expected-gui-keys (set (keys manifest/gui-definitions)))))

  (testing "every GUI definition includes the stable registration metadata"
    (doseq [[gui-key definition] manifest/gui-definitions]
      (is (= required-registration-keys
             (set (keys (manifest/gui-registration gui-key))))
          (str "registration shape for " gui-key))
      (is (= (:gui-id definition) (:gui-id (manifest/gui-registration gui-key))))
      (is (= (:gui-name definition) (manifest/gui-name gui-key)))
      (is (integer? (:gui-id definition)))
      (is (keyword? (:gui-type definition)))
      (is (string? (:registry-name definition)))
      (is (keyword? (:screen-factory-fn-kw definition)))
      (is (keyword? (:slot-schema-id definition)))))

  (testing "stable platform-visible identifiers are unique"
    (let [defs (vals manifest/gui-definitions)]
      (is (empty? (duplicates (map :gui-id defs))))
      (is (empty? (duplicates (map :gui-type defs))))
      (is (empty? (duplicates (map :registry-name defs)))))))

(deftest gui-type-and-message-domain-lookup-test
  (testing "GUI type lookups are manifest-backed"
    (is (= 0 (manifest/gui-id-for-type :node)))
    (is (= 2 (manifest/gui-id-for-type :solar)))
    (is (= 14 (manifest/gui-id-for-type :energy-converter)))
    (is (nil? (manifest/gui-id-for-type :unknown-gui-type))))

  (testing "message domains referenced by GUI definitions are manifest declared"
    (let [referenced-domains (->> (vals manifest/gui-definitions)
                                  (keep :message-domain)
                                  set)]
      (is (empty? (manifest/missing-message-domains referenced-domains)))
      (is (= [:alternate] (manifest/message-actions :metal-former))))))

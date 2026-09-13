(ns cn.li.ac.config.registry-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.skill-config :as ability-skill-config]
            [cn.li.ac.block.ability-interferer.config :as interferer-config]
            [cn.li.ac.block.cat-engine.config :as cat-config]
            [cn.li.ac.block.developer.config :as developer-config]
            [cn.li.ac.block.phase-gen.config :as phase-config]
            [cn.li.ac.block.solar-gen.config :as solar-config]
            [cn.li.ac.block.wind-gen.config :as wind-config]
            [cn.li.ac.config.common :as config-common]
            [cn.li.ac.config.gameplay :as gameplay-config]
            [cn.li.ac.config.registry :as registry]
            [cn.li.ac.config.worldgen :as worldgen-config]
            [cn.li.ac.integration.block.energy-converter.config :as energy-converter-config]
            [cn.li.ac.tutorial.config :as tutorial-config]
            [cn.li.ac.wireless.config :as wireless-config]
            [cn.li.mcmod.config.registry :as config-reg]))

(def ^:private supported-descriptor-types
  #{:int :double :boolean :string :string-list :int-list :double-list})

(defn- registered-descriptor-sets []
  (concat
    [{:label :wireless :descriptors registry/wireless-descriptors}
     {:label :wireless-devices :descriptors registry/wireless-devices-descriptors}
     {:label :gameplay :descriptors gameplay-config/descriptors}
     {:label :ability :descriptors ability-config/descriptors}
     {:label :ability-devices :descriptors registry/ability-devices-descriptors}]
    (map (fn [category-id]
           {:label category-id
            :descriptors (get ability-skill-config/descriptors-by-category category-id)})
         ability-skill-config/category-ids)))

(deftest descriptor-shape-guardrail-test
  (testing "all registered descriptors use bridge-supported types and dotted paths"
    (doseq [{:keys [label descriptors]} (registered-descriptor-sets)
            {:keys [key path type section] :as descriptor} descriptors]
      (is (keyword? key) [label descriptor])
      (is (contains? supported-descriptor-types type) [label descriptor])
      (is (and (string? path)
               (re-matches #"[A-Za-z0-9_-]+(\.[A-Za-z0-9_-]+)*" path))
          [label descriptor])
      (is (keyword? section) [label descriptor]))))

(deftest wireless-core-descriptor-aggregation-test
  (testing "wireless core domain only contains wireless network descriptors"
    (is (= wireless-config/descriptors registry/wireless-descriptors))
    (is (= (count registry/wireless-descriptors)
           (count (set (map :key registry/wireless-descriptors)))))))

(deftest wireless-device-descriptor-aggregation-test
  (testing "wireless device domain contains generator/converter descriptors"
    (let [expected-count (+ (count solar-config/descriptors)
                            (count wind-config/descriptors)
                            (count phase-config/descriptors)
                            (count cat-config/descriptors)
                            (count energy-converter-config/descriptors))]
      (is (= expected-count (count registry/wireless-devices-descriptors)))
      (is (= expected-count
             (count (set (map :key registry/wireless-devices-descriptors))))))))

(deftest ability-device-descriptor-aggregation-test
  (testing "ability device domain contains developer/interferer descriptors"
    (let [expected-count (+ (count developer-config/descriptors)
                            (count interferer-config/descriptors))]
      (is (= expected-count (count registry/ability-devices-descriptors)))
      (is (= expected-count
             (count (set (map :key registry/ability-devices-descriptors))))))))

(deftest wireless-default-values-merge-test
  (testing "wireless core defaults stay isolated from device defaults"
    (is (= wireless-config/default-values registry/wireless-default-values))))

(deftest wireless-device-default-values-merge-test
  (testing "wireless device defaults merge every device source module"
    (let [expected (merge solar-config/default-values
                          wind-config/default-values
                          phase-config/default-values
                          cat-config/default-values
                          energy-converter-config/default-values)]
      (is (= expected registry/wireless-devices-default-values)))))

(deftest ability-device-default-values-merge-test
  (testing "ability device defaults merge every device source module"
    (let [expected (merge developer-config/default-values
                          interferer-config/default-values)]
      (is (= expected registry/ability-devices-default-values)))))

(deftest init-configs-registers-descriptors-and-defaults-test
  (testing "init-configs! registers descriptors then defaults for AC config domains"
    (let [calls (atom [])]
      (with-redefs [config-reg/register-config-descriptors!
                    (fn [domain descriptors]
                      (swap! calls conj [:register domain descriptors]))
                    config-reg/ensure-default-values!
                    (fn [domain defaults]
                      (swap! calls conj [:defaults domain defaults]))]
        (is (nil? (registry/init-configs!)))
        (is (= (vec
                 (concat
                   [[:register config-common/wireless-domain registry/wireless-descriptors]
                    [:defaults config-common/wireless-domain registry/wireless-default-values]
                    [:register config-common/wireless-devices-domain registry/wireless-devices-descriptors]
                    [:defaults config-common/wireless-devices-domain registry/wireless-devices-default-values]
                    [:register config-common/gameplay-domain gameplay-config/descriptors]
                    [:defaults config-common/gameplay-domain gameplay-config/default-values]
                    [:register config-common/worldgen-domain worldgen-config/descriptors]
                    [:defaults config-common/worldgen-domain worldgen-config/default-values]
                    [:register config-common/ability-domain ability-config/descriptors]
                    [:defaults config-common/ability-domain ability-config/default-values]
                    [:register config-common/ability-devices-domain registry/ability-devices-descriptors]
                    [:defaults config-common/ability-devices-domain registry/ability-devices-default-values]
                    [:register config-common/tutorial-domain tutorial-config/descriptors]
                    [:defaults config-common/tutorial-domain tutorial-config/default-values]]
                   (mapcat (fn [category-id]
                             (let [domain (ability-skill-config/category-domain category-id)]
                               [[:register domain (get ability-skill-config/descriptors-by-category category-id)]
                                [:defaults domain (get ability-skill-config/default-values-by-category category-id)]]))
                           ability-skill-config/category-ids)))
               @calls))))))

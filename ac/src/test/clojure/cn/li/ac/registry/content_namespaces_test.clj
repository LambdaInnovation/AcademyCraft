(ns cn.li.ac.registry.content-namespaces-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.registry.content-namespaces :as content-ns]
            [cn.li.ac.registry.discovery :as discovery]
            [cn.li.ac.test.support.framework :as support-fw]))

(defn- clean-discovery-fixture [f]
  (discovery/reset-provider-registry-for-test!)
  (try
    (f)
    (finally
      (discovery/reset-provider-registry-for-test!))))

(use-fixtures :each support-fw/with-fresh-framework)
(use-fixtures :each clean-discovery-fixture)

(deftest current-content-load-plan-includes-core-phases-test
  (let [plan (content-ns/current-content-load-plan)
        phases (set (map :phase plan))]
    (is (= #{:block :item :entity :ability :achievement :system} phases))
    (let [ability-phase (first (filter #(= :ability (:phase %)) plan))]
      (is (some #(= 'cn.li.ac.content.ability %) (:namespaces ability-phase)))
      (is (some #(= 'cn.li.ac.content.ability/init-ability-content! %) (:init-fns ability-phase)))
      (is (some #(= 'cn.li.ac.content.loot/init-loot! %) (:init-fns ability-phase))))
    (let [system-phase (first (filter #(= :system (:phase %)) plan))]
      (is (= '[cn.li.ac.terminal.init/init-terminal!
               cn.li.ac.tutorial.init/init-tutorial!]
             (:init-fns system-phase))))))

(deftest register-content-phase-plugin-appends-after-defaults-test
  (content-ns/register-content-phase-plugin!
    {:phase :demo-extension
     :namespaces '[cn.li.ac.registry.content-namespaces-test]
     :init-fns []})
  (let [plan (content-ns/current-content-load-plan)]
    (is (= :demo-extension (:phase (last plan))))))

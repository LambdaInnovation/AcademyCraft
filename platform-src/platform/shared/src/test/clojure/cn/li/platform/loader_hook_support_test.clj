(ns cn.li.platform.loader-hook-support-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.platform.loader-hook-support :as hooks]))

(def ^:private fabric-target (str "fabric-" "1.20.1"))

(deftest supported-impl-key-matrix-test
  (testing "fabric allowlist accepts known effect keys"
    (is (hooks/supported-impl-key? fabric-target :effect :tiered-arcs))
    (is (hooks/supported-impl-key? fabric-target :effect :owner-orbit)))
  (testing "fabric allowlist rejects unknown effect keys"
    (is (not (hooks/supported-impl-key? fabric-target :effect :not-a-real-hook))))
  (testing "ray allowlist rejects effect-only keys"
    (is (not (hooks/supported-impl-key? fabric-target :ray :owner-orbit))))
  (testing "nil target allows all (unit / non-platform contexts)"
    (is (hooks/supported-impl-key? nil :effect :anything))))

(deftest filter-impl-key->hook-class-test
  (let [m {:tiered-arcs "A" :mystery "B"}
        filtered (hooks/filter-impl-key->hook-class fabric-target :effect m)]
    (is (= {:tiered-arcs "A"} filtered))))

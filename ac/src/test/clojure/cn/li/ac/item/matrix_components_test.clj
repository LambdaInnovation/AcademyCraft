(ns cn.li.ac.item.matrix-components-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.item.constraint-plate :as plate]
            [cn.li.ac.item.mat-core :as core]
            [cn.li.mcmod.platform.item :as item]))

(defn- with-fake-item-ids [f]
  (with-redefs [item/object identity
                item/registry-name (fn [o] (:reg o))
                item/description-id (fn [o] (:desc o))]
    (f)))

(deftest matrix-component-predicates-use-platform-item-identity
  (with-fake-item-ids
    (fn []
      (testing "runtime stacks are matched through platform item ids"
        (is (true? (core/is-mat-core? {:reg "mat_core_0"})))
        (is (true? (core/is-mat-core? {:desc "item.ac.mat_core_1"})))
        (is (true? (plate/is-constraint-plate? {:reg "constraint_plate"})))
        (is (true? (plate/is-constraint-plate? {:desc "item.ac.constraint_plate"}))))

      (testing "DSL specs are not accepted as runtime item stacks"
        (is (false? (core/is-mat-core? {:id "mat_core_0"})))
        (is (false? (plate/is-constraint-plate? {:id "constraint_plate"})))))))

(ns cn.li.ac.gui.slot-validators-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.gui.slot-validators :as v]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.item.constraint-plate :as plate]
            [cn.li.ac.item.mat-core :as core]))

(deftest energy-item-validator-delegates
  (with-redefs [energy/is-energy-item-supported? (constantly true)]
    (is (true? (v/energy-item-validator :s))))
  (with-redefs [energy/is-energy-item-supported? (constantly false)]
    (is (false? (v/energy-item-validator :s)))))

(deftest constraint-and-matrix-validators-delegate
  (with-redefs [plate/is-constraint-plate? (constantly true)]
    (is (true? (v/constraint-plate-validator :x))))
  (with-redefs [core/is-mat-core? (constantly true)]
    (is (true? (v/matrix-core-validator :y)))))

(deftest output-slot-validator-blocks
  (is (false? (v/output-slot-validator :anything))))

(ns cn.li.ac.integration.block.energy-converter.base-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.integration.block.energy-converter.base :as base]))

(deftest energy-capability-direction-test
  (let [be :be
        in-cap (base/make-energy-capability be "rf-input")
        out-cap (base/make-energy-capability be "rf-output")
        unknown-cap (base/make-energy-capability be "unknown")]
    (is (true? (.canReceive in-cap)))
    (is (false? (.canExtract in-cap)))
    (is (true? (.canExtract out-cap)))
    (is (false? (.canReceive out-cap)))
    (is (false? (.canExtract unknown-cap)))
    (is (false? (.canReceive unknown-cap)))))

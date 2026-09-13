(ns cn.li.mc1211.entity.mob-logic-compile-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.entity.mob-logic-compile :as mlc])
  (:import [cn.li.mcbase.entity.logic MobLogicBundle IMobHurtLogic]))

(deftest compile-empty-mob-props-test
  (let [bundle (mlc/compile-mob-logic {})]
    (is (instance? MobLogicBundle bundle))
    (is (nil? (.-tick bundle)))
    (is (nil? (.-hurt bundle)))))

(deftest compile-hurt-nan-cancels-test
  (let [bundle (mlc/compile-mob-logic {:mob-hurt-fn (constantly Float/NaN)})
        ^IMobHurtLogic hurt (.-hurt bundle)]
    (is (Float/isNaN (.onIncomingDamage hurt nil nil 1.0)))))

(ns cn.li.mc1201.entity.scripted-mob-dispatch-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mcbase.entity.mob-logic-compile :as mlc])
  (:import [cn.li.mcbase.entity ScriptedEntityLogicRegistry]
           [cn.li.mcbase.entity.logic MobLogicBundle IMobHurtLogic IMobLootLogic]))

(deftest hurt-nan-cancels-via-compile-test
  (let [bundle (mlc/compile-mob-logic {:mob-hurt-fn (constantly Float/NaN)})
        ^IMobHurtLogic hurt (.-hurt bundle)]
    (is (Float/isNaN (.onIncomingDamage hurt nil nil 5.0)))))

(deftest loot-false-delegates-to-vanilla-semantics-test
  (let [bundle (mlc/compile-mob-logic {:mob-loot-fn (constantly false)})
        ^IMobLootLogic loot (.-loot bundle)]
    (is (false? (.dropLoot loot nil nil true)))))

(deftest install-and-read-mob-bundle-test
  (is (identical? MobLogicBundle/EMPTY (ScriptedEntityLogicRegistry/getMobLogic nil))))

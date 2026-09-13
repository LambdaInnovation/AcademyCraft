(ns cn.li.ac.block.developer.logic-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.model.develop :as dev-model]
            [cn.li.ac.block.developer.logic :as dev-logic]
            [cn.li.ac.block.developer.session :as session]
            [cn.li.ac.block.machine.runtime :as machine-runtime]))

(deftest developer-tick-state-consumes-energy-while-developing-test
  (let [dd (dev-model/start-develop (dev-model/new-develop-data) :normal :level-up {:target-level 2} 100)
        state {:is-developing true
               :development-data dd
               :energy 100000.0
               :tier "normal"
               :structure-valid true
               :update-ticker 0}
        be :be
        runtime-state (machine-runtime/ensure-machine-state state dev-logic/dev-default-state)
        old-energy (:energy runtime-state)
        next (dev-logic/developer-tick-state runtime-state :lvl :pos nil be)]
    (is (< (:energy next) old-energy))
    (is (>= (:development-progress next) 0.0))
    (is (<= (:development-progress next) 1.0))))

(deftest developer-after-commit-applies-completion-once-test
  (let [completed (atom 0)
        cleared (atom 0)
        state {:development-complete? true
               :development-action :level-up
               :development-payload {}
               :user-uuid "player-1"}]
    (with-redefs [session/apply-completion! (fn [_] (swap! completed inc))
                  machine-runtime/commit-transform! (fn [& _] (swap! cleared inc))]
      (#'dev-logic/developer-after-commit! :tile nil nil {} state)
      (is (= 1 @completed))
      (is (= 1 @cleared)))))

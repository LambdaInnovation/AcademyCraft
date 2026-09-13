(ns cn.li.ac.block.machine-container-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.machine.container :as container]
            [cn.li.ac.block.machine.runtime :as runtime]))

(deftest inventory-container-fns-test
  (testing "generated container fns read/write inventory slots"
    (let [default {:inventory [nil nil]}
          fns (container/make-inventory-container-fns
                {:default-state default
                 :slot-count 2
                 :can-place? (fn [_ s _ _] (= s 0))
                 :can-take? (fn [_ _ _ _] true)})
          be (atom default)]
      (with-redefs [runtime/state-or-default (fn [_ _] @be)
                    cn.li.mcmod.platform.be/set-custom-state!
                    (fn [_ state] (reset! be state))]
        (is (= 2 ((:get-size fns) be)))
        ((:set-item! fns) be 0 :item-a)
        (is (= :item-a (get-in @be [:inventory 0])))
        (is (= :item-a ((:get-item fns) be 0)))))))

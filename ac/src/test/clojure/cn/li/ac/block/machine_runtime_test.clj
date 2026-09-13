(ns cn.li.ac.block.machine-runtime-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.machine.runtime :as runtime]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.world :as world]))

(def test-schema
  [{:key :energy :type :double :default 0.0 :persist? true}
   {:key :update-ticker :type :int :default 0 :persist? false}])

(def test-runtime (runtime/schema-runtime test-schema :server-only? true))

(deftest schema-runtime-test
  (testing "schema-runtime builds lifecycle fns and mutable field layout"
    (is (= 0.0 (get (:default-state test-runtime) :energy)))
    (is (fn? (:load-fn test-runtime)))
    (is (fn? (:save-fn test-runtime)))))

(deftest make-tick-fn-test
  (testing "tick wrapper mutates one MachineState and marks persisted fields"
    (let [stored (atom nil)
          changed (atom 0)
          tick-fn (runtime/make-tick-fn
                    {:default-state (:default-state test-runtime)
                     :tick-state (fn [state _level _pos _block-state _be]
                                   (assoc state :energy 1.0))})]
      (with-redefs [world/client-side? (fn [_] false)
                    platform-be/get-custom-state (fn [_] @stored)
                    platform-be/set-custom-state! (fn [_ state] (reset! stored state))
                    platform-be/set-changed! (fn [_] (swap! changed inc))]
        (tick-fn :level :pos :bs :be)
        (is (runtime/machine-state? @stored))
        (is (= 1.0 (:energy @stored)))
        (is (= 1 @changed))))))

(deftest make-tick-fn-initial-state-and-after-commit-test
  (testing "after-commit receives a zero-copy previous-value view"
    (let [stored (atom nil)
          after (atom nil)
          tick-fn (runtime/make-tick-fn
                    {:default-state (:default-state test-runtime)
                     :initial-state (fn [_be _level _pos _block-state] {:energy 0.5})
                     :tick-state (fn [state _level _pos _block-state _be]
                                   (assoc state :energy 1.0))
                     :after-commit! (fn [_be _level _pos old new]
                                      (reset! after {:old-energy (:energy old)
                                                     :new-energy (:energy new)}))})]
      (with-redefs [world/client-side? (fn [_] false)
                    platform-be/set-custom-state! (fn [_ state] (reset! stored state))
                    platform-be/set-changed! (fn [_] nil)]
        (tick-fn :level :pos :bs :be)
        (is (= 0.5 (:old-energy @after)))
        (is (= 1.0 (:new-energy @after)))))))

(deftest commit-transform-test
  (testing "interaction transforms join the same MachineState commit boundary"
    (let [stored (atom nil)]
      (with-redefs [platform-be/get-custom-state (fn [_] @stored)
                    platform-be/set-custom-state! (fn [_ state] (reset! stored state))
                    platform-be/set-changed! (fn [_] nil)]
        (runtime/commit-transform! :tile (:default-state test-runtime)
                                   #(assoc % :energy 5.0))
        (is (= 5.0 (:energy @stored)))))))

(deftest make-open-gui-handler-test
  (testing "open handler requires player world pos and respects sneaking"
    (let [handler (runtime/make-open-gui-handler :solar)
          opened (atom false)]
      (with-redefs [world/client-side? (fn [_] true)
                    cn.li.ac.gui.open/open-gui-by-type (fn [& _] (reset! opened true))]
        (handler :p :w :pos :solar :sneaking true)
        (is (false? @opened))
        (handler :p :w :pos :solar)
        (is (true? @opened))))))

(deftest make-open-gui-handler-predicate-test
  (testing "make-open-gui-handler-with-predicate respects can-open? predicate"
    (let [handler (runtime/make-open-gui-handler-with-predicate :wind-gen-main (fn [_ _ _ _ _] false))
          opened (atom false)]
      (with-redefs [world/client-side? (fn [_] true)
                    cn.li.ac.gui.open/open-gui-by-type (fn [& _] (reset! opened true))]
        (handler :p :w :pos :wind-gen-main)
        (is (false? @opened))))))

(deftest make-open-gui-handler-multiblock-options-test
  (testing "resolve-open-pos and server-before-open! hooks"
    (let [opened (atom nil)
          handler (runtime/make-open-gui-handler-with-predicate
                    :developer
                    (constantly true)
                    :resolve-open-pos (fn [_ _ _] :controller-pos)
                    :server-before-open! (fn [_ _ _] true))]
      (with-redefs [world/client-side? (fn [_] false)
                    cn.li.ac.gui.open/open-gui-by-type
                    (fn [player gui-type world pos]
                      (reset! opened {:player player :gui-type gui-type :world world :pos pos}))]
        (handler :p :w :part-pos :developer)
        (is (= {:player :p :gui-type :developer :world :w :pos :controller-pos}
               @opened))))))

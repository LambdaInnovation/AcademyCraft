(ns cn.li.ac.ability.item-actions-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.item-actions :as ia]))



(defn- reset-registries! [f]
  (ia/reset-item-action-registries-for-test!)
  (try
    (f)
    (finally
      (ia/reset-item-action-registries-for-test!))))

(use-fixtures :each reset-registries!)

(deftest register-item-action-roundtrip-test
  (is (nil? (ia/register-item-action! "academy:coin" :coin-toss)))
  (is (= :coin-toss (ia/resolve-item-action "academy:coin")))
  (is (nil? (ia/resolve-item-action "other:item"))))

(deftest on-item-action-dispatch-test
  (let [seen (atom [])]
    (ia/register-action-handler! :my-action
                               (fn [uuid payload]
                                 (swap! seen conj [uuid payload])
                                 :ok))
    (is (= :ok (ia/on-item-action! :my-action "p1" {:x 1})))
    (is (= [["p1" {:x 1}]] @seen)))
  (is (nil? (ia/on-item-action! :unregistered "p1" nil))))

(deftest on-item-action-handler-exception-test
  (ia/register-action-handler! :throws (fn [_ _] (throw (ex-info "boom" {}))))
  (is (nil? (ia/on-item-action! :throws "p1" nil))))

(deftest item-entity-spawn-registry-test
  (is (nil? (ia/register-item-entity-spawn! "academy:ball" {:entity-id "academy:sfx_ball" :speed 1.5})))
  (is (= {:entity-id "academy:sfx_ball" :speed 1.5}
         (ia/get-item-entity-spawn "academy:ball")))
  (is (nil? (ia/get-item-entity-spawn "nope"))))

(deftest item-action-registry-duplicate-and-freeze-policy-test
  (ia/register-item-action! "academy:coin" :coin-toss)
  (is (nil? (ia/register-item-action! "academy:coin" :coin-toss)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Conflicting item action id"
                        (ia/register-item-action! "academy:coin" :other-action)))
  (let [seen (atom [])]
    (ia/register-action-handler! :dup (fn [_ _] (swap! seen conj :first)))
    (ia/register-action-handler! :dup (fn [_ _] (swap! seen conj :second)))
    (ia/on-item-action! :dup "p" {})
    (is (= [:first] @seen)))
  (ia/register-item-entity-spawn! "academy:ball" {:entity-id "academy:sfx_ball" :speed 1.5})
  (is (nil? (ia/register-item-entity-spawn! "academy:ball" {:entity-id "academy:sfx_ball" :speed 1.5})))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Conflicting item entity spawn id"
                        (ia/register-item-entity-spawn! "academy:ball" {:entity-id "other"})))
  (ia/freeze-item-action-registries!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Item action registries are frozen"
                        (ia/register-item-action! "academy:new" :new-action)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Item action registries are frozen"
                        (ia/register-action-handler! :new-action (fn [_ _] nil))))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Item action registries are frozen"
                        (ia/register-item-entity-spawn! "academy:new" {:entity-id "e"}))))

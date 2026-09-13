(ns cn.li.forge1201.runtime.lifecycle-event-binding-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.forge1201.runtime.lifecycle-event-binding :as binding])
  (:import [net.minecraftforge.event.entity.player PlayerEvent$PlayerLoggedInEvent
                                                 PlayerEvent$PlayerLoggedOutEvent
                                                 PlayerEvent$Clone
                                                 PlayerEvent$PlayerChangedDimensionEvent]
           [net.minecraftforge.event.entity.living LivingDeathEvent]
           [net.minecraftforge.event TickEvent$ServerTickEvent]))

(defn- reset-registration!
  [f]
  (binding/reset-lifecycle-listeners-registration-for-test!)
  (try
    (f)
    (finally
      (binding/reset-lifecycle-listeners-registration-for-test!))))

(use-fixtures :each reset-registration!)

(defn- test-handlers
  []
  {:on-player-login identity
   :on-player-logout identity
   :on-player-clone identity
   :on-player-death identity
   :on-player-dimension-change identity
   :on-server-tick identity})

(deftest register-lifecycle-listeners-is-idempotent-test
  (let [registrations (atom [])]
    (with-redefs [binding/add-listener! (fn [event-class handler]
                                          (swap! registrations conj [event-class handler]))]
      (binding/register-lifecycle-listeners! (test-handlers))
      (binding/register-lifecycle-listeners! (test-handlers)))
    (is (= [[PlayerEvent$PlayerLoggedInEvent identity]
            [PlayerEvent$PlayerLoggedOutEvent identity]
            [PlayerEvent$Clone identity]
            [LivingDeathEvent identity]
            [PlayerEvent$PlayerChangedDimensionEvent identity]
            [TickEvent$ServerTickEvent identity]]
           @registrations))))

(deftest reset-allows-re-registration-test
  (let [registrations (atom [])]
    (with-redefs [binding/add-listener! (fn [event-class _handler]
                                          (swap! registrations conj event-class))]
      (binding/register-lifecycle-listeners! (test-handlers))
      (binding/reset-lifecycle-listeners-registration-for-test!)
      (binding/register-lifecycle-listeners! (test-handlers)))
    (is (= 12 (count @registrations)))))

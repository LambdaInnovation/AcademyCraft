(ns cn.li.mc1201.runtime.spi.server-context-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.runtime.spi.server-context :as server-context]))

(defn- reset-server-context!
  [f]
  (server-context/reset-server-context-spi-for-test!)
  (try
    (f)
    (finally
      (server-context/reset-server-context-spi-for-test!))))

(use-fixtures :each reset-server-context!)

(deftest available-callback-registration-is-idempotent-test
  (let [calls (atom [])]
    (server-context/register-server-context-impl!
      {:get-current-server (fn [] :server-a)})
    (server-context/on-server-available! :cb/available
      (fn [server]
        (swap! calls conj [:available server])))
    (server-context/on-server-available! :cb/available
      (fn [server]
        (swap! calls conj [:duplicate server])))
    (server-context/notify-server-available! :server-b)
    (is (= [[:available :server-a]
            [:available :server-b]]
           @calls))))

(deftest unavailable-callback-registration-dedupes-by-callback-identity-test
  (let [calls (atom [])
        callback (fn [server]
                   (swap! calls conj [:unavailable server]))]
    (server-context/on-server-unavailable! callback)
    (server-context/on-server-unavailable! callback)
    (server-context/notify-server-unavailable! :server-x)
    (is (= [[:unavailable :server-x]] @calls))))

(deftest reset-clears-registered-impl-and-callbacks-test
  (let [calls (atom [])]
    (server-context/register-server-context-impl!
      {:get-current-server (fn [] :server-a)})
    (server-context/on-server-available! :cb/reset
      (fn [server]
        (swap! calls conj server)))
    (server-context/reset-server-context-spi-for-test!)
    (server-context/notify-server-available! :server-b)
    (is (= [:server-a] @calls))
    (is (= :unregistered (server-context/server-state)))))

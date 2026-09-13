(ns cn.li.forge1201.integration.imc-dispatch-test
  "IMC registration keys must match the published WirelessImc constants, and
  dispatched events must reach both Consumer and IFn handlers with the full
  payload (including device capabilities)."
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.forge1201.integration.imc-dispatch :as imc-dispatch])
  (:import [cn.li.acapi.wireless WirelessImc]
           [java.util.function Consumer]))

(use-fixtures
  :each
  (fn [f]
    (imc-dispatch/reset-handlers-for-test!)
    (try
      (f)
      (finally
        (imc-dispatch/reset-handlers-for-test!)))))

(deftest register-by-wireless-imc-keys-test
  (let [network-events (atom [])
        node-events (atom [])]
    (imc-dispatch/register-by-method-key!
      WirelessImc/REGISTER_NETWORK_HANDLER
      (reify Consumer
        (accept [_ payload] (swap! network-events conj payload))))
    (imc-dispatch/register-by-method-key!
      WirelessImc/REGISTER_NODE_HANDLER
      (fn [payload] (swap! node-events conj payload)))
    (imc-dispatch/dispatch-event!
      {:kind :topology/network :action :created :ssid "s" :matrix :matrix-cap})
    (imc-dispatch/dispatch-event!
      {:kind :topology/node :action :generator-linked :node :node-cap :generator :gen-cap})
    (testing "network handler registered via WirelessImc key receives the event map"
      (is (= 1 (count @network-events)))
      (is (= :created (:action (first @network-events))))
      (is (= "s" (:ssid (first @network-events))))
      (is (= :matrix-cap (:matrix (first @network-events)))))
    (testing "node handler receives the full payload including the device cap"
      (is (= 1 (count @node-events)))
      (is (= :generator-linked (:action (first @node-events))))
      (is (= :node-cap (:node (first @node-events))))
      (is (= :gen-cap (:generator (first @node-events)))))))

(deftest unknown-method-key-is-ignored-test
  (imc-dispatch/register-by-method-key! "bogus_key" (fn [_]))
  ;; nothing registered — dispatch must be a quiet no-op
  (imc-dispatch/dispatch-event! {:kind :topology/network :action :created})
  (is true))

(deftest throwing-handler-is-removed-test
  (let [calls (atom 0)]
    (imc-dispatch/register-by-method-key!
      WirelessImc/REGISTER_NETWORK_HANDLER
      (fn [_]
        (swap! calls inc)
        (throw (RuntimeException. "boom"))))
    (imc-dispatch/dispatch-event! {:kind :topology/network :action :created})
    (imc-dispatch/dispatch-event! {:kind :topology/network :action :created})
    (is (= 1 @calls) "handler that throws is removed after first dispatch")))

(ns cn.li.mcmod.lifecycle-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.lifecycle :as lifecycle]))

(defn- reset-state! []
  (lifecycle/reset-lifecycle-state-for-test!))

(deftest content-init-registration-test
  (reset-state!)
  (let [called (atom 0)]
    (testing "run-content-init! is no-op without registration"
      (is (nil? (lifecycle/run-content-init!))))
    (testing "registered init runs once per invocation"
      (is (nil? (lifecycle/register-content-init! #(swap! called inc))))
      (lifecycle/run-content-init!)
      (lifecycle/run-content-init!)
      (is (= 2 @called)))))

(deftest runtime-activation-registration-test
  (reset-state!)
  (let [called (atom [])]
    (is (nil? (lifecycle/register-runtime-content-activation!
                #(swap! called conj :activated))))
    (lifecycle/run-runtime-content-activation!)
    (is (= [:activated] @called))))

(deftest client-init-registration-order-test
  (reset-state!)
  (let [called (atom [])]
    (is (nil? (lifecycle/register-client-init! #(swap! called conj :a))))
    (is (nil? (lifecycle/register-client-init! #(swap! called conj :b))))
    (lifecycle/run-client-init!)
    (is (= [:a :b] @called))))

(deftest client-init-runs-once-per-session-test
  (reset-state!)
  (let [called (atom 0)]
    (lifecycle/register-client-init! #(swap! called inc))
    (testing "redundant invocations are no-ops (RegisterRenderers fires from multiple sites)"
      (lifecycle/run-client-init!)
      (lifecycle/run-client-init!)
      (lifecycle/run-client-init!)
      (is (= 1 @called)))
    (testing "reset re-arms client init for the next session"
      (reset-state!)
      (lifecycle/register-client-init! #(swap! called inc))
      (lifecycle/run-client-init!)
      (is (= 2 @called)))))

(deftest multiple-content-modules-are-retained-test
  (reset-state!)
  (let [called (atom [])]
    (lifecycle/register-content-init! #(swap! called conj :first))
    (lifecycle/register-content-init! #(swap! called conj :second))
    (lifecycle/run-content-init!)
    (is (= [:first :second] @called)))
  (let [called (atom [])]
    (lifecycle/register-runtime-content-activation! #(swap! called conj :first))
    (lifecycle/register-runtime-content-activation! #(swap! called conj :second))
    (lifecycle/run-runtime-content-activation!)
    (is (= [:first :second] @called))))

(deftest world-tick-registration-test
  (reset-state!)
  (let [called (atom [])]
    (lifecycle/register-world-tick! #(swap! called conj [:ac %]))
    (lifecycle/register-world-tick! #(swap! called conj [:bc %]))
    (lifecycle/run-world-tick! :world)
    (is (= [[:ac :world] [:bc :world]] @called))))

(deftest content-and-runtime-exception-propagation-test
  (reset-state!)
  (testing "run-content-init! propagates exception from registered function"
    (lifecycle/register-content-init! #(throw (ex-info "content-fail" {})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"content-fail"
                          (lifecycle/run-content-init!))))
  (testing "run-runtime-content-activation! propagates exception from registered function"
    (lifecycle/register-runtime-content-activation! #(throw (ex-info "runtime-fail" {})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"runtime-fail"
                          (lifecycle/run-runtime-content-activation!)))))

(deftest client-init-exception-short-circuit-test
  (reset-state!)
  (let [calls (atom [])]
    (lifecycle/register-client-init! #(swap! calls conj :first))
    (lifecycle/register-client-init! #(throw (ex-info "client-fail" {})))
    (lifecycle/register-client-init! #(swap! calls conj :third))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo #"client-fail"
                          (lifecycle/run-client-init!)))
    (is (= [:first] @calls))))

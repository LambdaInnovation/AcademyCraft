(ns cn.li.mc262.gui.network.packet-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.gui.network.packet :as packet]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as client]))

(defn- with-fresh-client-runtime
  [f]
  (client/call-with-client-runtime (client/create-client-runtime)
    (fn []
      (try
        (f)
        (finally
          (client/reset-client-state-for-test!))))))

(use-fixtures :each with-fresh-client-runtime)

(deftest dispatch-client-response-respects-explicit-owner-test
  (let [session (client/create-client-network-session)
        owner {:logical-side :client
               :client-session-id :session-a
               :player-uuid "player-a"
               :screen-id :screen-a
               :client-network-session session}
        calls (atom [])]
    (with-redefs [client/send-request (fn [& _] nil)]
      (client/send-to-server owner "req" {} (fn [resp] (swap! calls conj [:response resp])))
      (client/register-push-handler! owner "push/state" (fn [payload] (swap! calls conj [:push payload])))
      (packet/dispatch-client-response! owner 1 {:ok true})
      (packet/dispatch-client-response! owner -1 {:msg-id "push/state" :payload {:x 1}}))
    (is (= [[:response {:ok true}]
            [:push {:x 1}]]
           @calls))))

(deftest response-dispatch-stays-bound-to-current-client-session-test
  (let [calls (atom [])]
    (with-redefs [client/send-request (fn [& _] nil)]
      (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "req" {} (fn [resp] (swap! calls conj resp)))))
      (runtime-hooks/with-client-ctx-fn {:session-id :session-b} (fn [] (packet/dispatch-client-response! 1 {:ok :stale})))
      (is (empty? @calls))
      (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (packet/dispatch-client-response! 1 {:ok :live}))))
    (is (= [{:ok :live}] @calls))))

(deftest dispatch-client-response-without-owner-uses-global-push-handlers-test
  (let [calls (atom [])]
    (client/register-push-handler! "push/global" (fn [payload] (swap! calls conj payload)))
    (packet/dispatch-client-response! -1 {:msg-id "push/global" :payload {:value 42}})
    (is (= [{:value 42}] @calls))))

(deftest dispatch-client-response-owner-session-falls-back-to-global-push-handlers-test
  (let [calls (atom [])
        owner {:logical-side :client
               :client-session-id :session-a
               :player-uuid "player-a"}]
    (client/register-push-handler! "push/global" (fn [payload] (swap! calls conj payload)))
    (packet/dispatch-client-response! owner -1 {:msg-id "push/global" :payload {:value 7}})
    (is (= [{:value 7}] @calls))))

(ns cn.li.mcmod.network.client-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as client]
            [cn.li.mcmod.platform.dispatch :as dispatch]))

(defn- with-fresh-client-runtime [f]
  (client/call-with-client-runtime (client/create-client-runtime)
    (fn []
      (try
        (f)
        (finally
          (client/reset-client-state-for-test!))))))

(use-fixtures :each with-fresh-client-runtime)

(deftest send-to-server-request-id-contract-test
  (let [calls (atom [])]
    (with-redefs [client/send-request (fn [msg payload request-id]
                                        (swap! calls conj [msg payload request-id]))]
      (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "one-way" {:a 1})
        (client/send-to-server "req" {:b 2} identity)))
      (is (= [["one-way" {:a 1} -1]
              ["req" {:b 2} 1]]
             @calls))
      (is (= #{[(runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/client-owner-key nil))) 1]}
             (set (keys (:pending-requests (client/client-state-snapshot {:client-session-id :session-a})))))))))

(deftest ownerless-send-requires-client-session-test
  (with-redefs [client/send-request (fn [& _] nil)]
    (runtime-hooks/with-client-ctx-fn {:session-id nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"Client network owner requires :client-session-id"
                            (client/send-to-server "req" {} identity)))))))

(deftest screen-id-does-not-scope-pending-requests-test
  (testing "Responses rebuild owner without :screen-id; pending keys must still match"
    (let [responses (atom [])
          owner {:logical-side :client
                 :client-session-id :session-a
                 :player-uuid "player-a"
                 :screen-id :terminal}]
      (with-redefs [client/send-request (fn [& _] nil)]
        (client/send-to-server owner "terminal/get-state" {}
                               (fn [resp] (swap! responses conj resp))))
      ;; Response path: owner has session but no screen-id (with-client-response-owner).
      (client/handle-response {:logical-side :client
                               :client-session-id :session-a
                               :player-uuid "player-a"}
                              1
                              {:terminal-installed? true})
      (is (= [{:terminal-installed? true}] @responses))
      (is (empty? (:pending-requests
                   (client/client-state-snapshot {:client-session-id :session-a})))))))

(deftest handle-response-lifecycle-test
  (let [responses (atom [])]
    (with-redefs [client/send-request (fn [& _] nil)]
      (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "req" {} (fn [resp] (swap! responses conj resp))))))
    (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/handle-response 1 {:ok true})))
    (is (= [{:ok true}] @responses))
    (is (empty? (:pending-requests (client/client-state-snapshot {:client-session-id :session-a})))))
  (testing "unknown request-id is ignored without throw"
    (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (is (= nil (client/handle-response 999 {:ok false}))))))
  (testing "callback exception is swallowed"
    (with-redefs [client/send-request (fn [& _] nil)]
      (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "req" {} (fn [_] (throw (ex-info "cb-fail" {})))))))
    (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (is (= nil (client/handle-response 2 {:x 1})))))
    (is (empty? (:pending-requests (client/client-state-snapshot {:client-session-id :session-a}))))))

(deftest owner-scoped-pending-requests-test
  (let [session-a (client/create-client-network-session)
        session-b (client/create-client-network-session)
        owner-a {:logical-side :client
                 :client-session-id :session-a
                 :player-uuid "player-a"
                 :client-network-session session-a}
        owner-b {:logical-side :client
                 :client-session-id :session-b
                 :player-uuid "player-b"
                 :client-network-session session-b}
        responses (atom [])]
    (with-redefs [client/send-request (fn [& _] nil)]
      (client/send-to-server owner-a "req" {} (fn [resp] (swap! responses conj [:a resp])))
      (client/send-to-server owner-b "req" {} (fn [resp] (swap! responses conj [:b resp]))))
    (is (= #{[(client/client-owner-key owner-a) 1]
             [(client/client-owner-key owner-b) 1]}
           (set (concat (keys (:pending-requests (client/client-state-snapshot session-a)))
                        (keys (:pending-requests (client/client-state-snapshot session-b)))))))
    (client/handle-response owner-b 1 {:ok :b})
    (is (= [[:b {:ok :b}]] @responses))
    (is (= #{[(client/client-owner-key owner-a) 1]}
           (set (keys (:pending-requests (client/client-state-snapshot session-a))))))
    (client/clear-owner-state! owner-a)
    (is (empty? (:pending-requests (client/client-state-snapshot session-a))))))

(deftest pending-requests-expire-by-timeout-test
  (let [session (client/create-client-network-session)
        owner {:logical-side :client
               :client-session-id :session-a
               :player-uuid "player-a"
               :timeout-ms 1
               :client-network-session session}]
    (with-redefs [client/send-request (fn [& _] nil)]
      (client/send-to-server owner "req" {} identity))
    (is (= [[(client/client-owner-key owner) 1]]
           (client/expire-pending-requests! session (+ (System/currentTimeMillis) 10000))))
    (is (empty? (:pending-requests (client/client-state-snapshot session))))))

(deftest push-handler-contract-test
  (let [calls (atom [])]
    (client/register-push-handler! "push/a" (fn [payload] (swap! calls conj payload)))
    (client/handle-push "push/a" {:x 1})
    (is (= [{:x 1}] @calls))
    (is (= nil (client/handle-push "missing" {}))))
  (testing "push handler exception is swallowed"
    (client/register-push-handler! "push/boom" (fn [_] (throw (ex-info "boom" {}))))
    (is (= nil (client/handle-push "push/boom" {:a 1})))))

(deftest owner-scoped-push-handler-test
  (let [session-a (client/create-client-network-session)
        session-b (client/create-client-network-session)
        owner-a {:logical-side :client
                 :client-session-id :session-a
                 :player-uuid "player-a"
                 :channel-id :screen-a
                 :client-network-session session-a}
        owner-b {:logical-side :client
                 :client-session-id :session-a
                 :player-uuid "player-b"
                 :channel-id :screen-b
                 :client-network-session session-b}
        calls (atom [])]
    (client/register-push-handler! "push/state" (fn [payload] (swap! calls conj [:static payload])))
    (client/register-push-handler! owner-a "push/state" (fn [payload] (swap! calls conj [:a payload])))
    (client/handle-push owner-a "push/state" {:x 1})
    (client/handle-push owner-b "push/state" {:x 2})
    (is (= [[:a {:x 1}]] @calls))
    (client/clear-owner-state! owner-a)
    (client/handle-push owner-a "push/state" {:x 3})
    (is (= [[:a {:x 1}]] @calls))
    (is (empty? (:push-handlers (client/client-state-snapshot session-a))))
    (is (empty? (:push-handlers (client/client-state-snapshot session-b))))))

(deftest client-runtime-isolation-test
  (let [runtime-a (client/create-client-runtime)
        runtime-b (client/create-client-runtime)
        calls (atom [])]
    (with-redefs [client/send-request (fn [& _] nil)]
      (client/call-with-client-runtime runtime-a
        (fn []
          (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "req" {} (fn [resp] (swap! calls conj [:a-response resp])))
            (client/register-push-handler! "push/runtime" (fn [payload] (swap! calls conj [:a-push payload])))))))
      (client/call-with-client-runtime runtime-b
        (fn []
          (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/send-to-server "req" {} (fn [resp] (swap! calls conj [:b-response resp])))))))
      (client/call-with-client-runtime runtime-b
        (fn []
          (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/handle-push "push/runtime" {:x :ignored})
            (client/handle-response 1 {:ok :b})))))
      (is (= [[:b-response {:ok :b}]] @calls))
      (client/call-with-client-runtime runtime-a
        (fn []
          (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (client/handle-push "push/runtime" {:x :a})
            (client/handle-response 1 {:ok :a})))))
      (is (= [[:b-response {:ok :b}]
              [:a-push {:x :a}]
              [:a-response {:ok :a}]]
             @calls)))))

(deftest default-send-request-contract-test
  (dispatch/install-platform-version! :unknown "test")
  (try
    (client/send-request "m" {} 0)
    (is false)
    (catch clojure.lang.ExceptionInfo e
      (is (= :unknown (:version (ex-data e)))))))

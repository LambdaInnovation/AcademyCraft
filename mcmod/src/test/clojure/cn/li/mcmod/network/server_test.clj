(ns cn.li.mcmod.network.server-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.network.server :as server]))

(defn- reset-handlers! [f]
  (server/reset-handlers-for-test!)
  (try
    (f)
    (finally
      (server/reset-handlers-for-test!))))

(use-fixtures :each reset-handlers!)

(deftest handle-request-core-test
  (let [responses (atom [])]
    (server/register-handler "msg/a" (fn [payload _] {:ok (:x payload)}))
    (server/handle-request "msg/a" 10 {:x 7 :container-id 1} :player
                           (fn [req-id resp] (swap! responses conj [req-id resp])))
    (is (= [[10 {:ok 7}]] @responses)))
  (testing "nil handler response is normalized to empty map"
    (let [responses (atom [])]
      (server/register-handler "msg/b" (fn [_ _] nil))
      (server/handle-request "msg/b" 1 {:container-id 1} :player
                             (fn [req-id resp] (swap! responses conj [req-id resp])))
      (is (= [[1 {}]] @responses)))))

(deftest handle-request-edge-cases-test
  (testing "one-way request-id does not trigger respond-fn"
    (let [called (atom false)]
      (server/register-handler "one-way" (fn [_ _] {:ok true}))
      (server/handle-request "one-way" -1 {} :player (fn [_ _] (reset! called true)))
      (is (= false @called))))
  (testing "missing handler returns no-handler response"
    (let [responses (atom [])]
      (server/handle-request "missing" 2 {} :player
                             (fn [req-id resp] (swap! responses conj [req-id resp])))
      (is (= [[2 {:success false :error "no-handler"}]] @responses)))))

(deftest handle-request-exception-contract-test
  (testing "handler exception returns structured error response"
    (let [responses (atom [])]
      (server/register-handler "boom" (fn [_ _] (throw (ex-info "broken" {}))))
      (server/handle-request "boom" 6 {:container-id 1} :player
                             (fn [req-id resp] (swap! responses conj [req-id resp])))
      (is (= 1 (count @responses)))
      (is (= 6 (ffirst @responses)))
      (is (= false (get-in @responses [0 1 :success])))
      (is (= "broken" (get-in @responses [0 1 :error]))))))

(deftest handler-registration-policy-test
  (let [handler (fn [payload _player] payload)]
    (server/register-handler "same" handler)
    (server/register-handler "same" handler)
    (is (= #{"same"} (set (keys (:handlers (server/handlers-snapshot))))))
    (is (= :server
           (get-in (server/handlers-snapshot) [:handlers "same" :contract :owner-spec])))
    (is (thrown-with-msg?
         clojure.lang.ExceptionInfo
         #"Conflicting network handler id"
         (server/register-handler "same" (fn [_ _] {:other true}))))))

(deftest register-handler-rejects-invalid-owner-spec-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Server GUI handler contract requires :owner-spec :server"
                        (server/register-handler "bad" (fn [_ _] nil)
                                                 {:owner-spec :client}))))

(deftest frozen-handlers-reject-registration-test
  (server/freeze-handlers!)
  (is (true? (:frozen? (server/handlers-snapshot))))
  (is (thrown-with-msg?
       clojure.lang.ExceptionInfo
       #"Network server handlers are frozen"
       (server/register-handler "late" (fn [_ _] nil)))))

(deftest network-server-runtime-isolation-test
  (let [runtime-a (server/create-network-server-runtime)
        runtime-b (server/create-network-server-runtime)
        responses* (atom [])]
    (server/call-with-network-server-runtime
      runtime-a
      (fn []
        (server/register-handler "a" (fn [payload _] {:ok (:v payload)}))
        (server/handle-request "a" 1 {:v 11 :container-id 1} :player
                               (fn [req-id resp] (swap! responses* conj [:a req-id resp])))
        (is (= [[:a 1 {:ok 11}]] @responses*))))
    (server/call-with-network-server-runtime
      runtime-b
      (fn []
        (is (empty? (:handlers (server/handlers-snapshot))))
        (server/register-handler "b" (fn [payload _] {:ok (:v payload)}))
        (server/handle-request "b" 2 {:v 22 :container-id 2} :player
                               (fn [req-id resp] (swap! responses* conj [:b req-id resp])))
        (is (= [[:a 1 {:ok 11}] [:b 2 {:ok 22}]] @responses*))))
    (server/call-with-network-server-runtime
      runtime-a
      (fn []
        (server/handle-request "a" 3 {:v 33 :container-id 3} :player
                               (fn [req-id resp] (swap! responses* conj [:a req-id resp])))
        (is (= [[:a 1 {:ok 11}] [:b 2 {:ok 22}] [:a 3 {:ok 33}]]
               @responses*))))))

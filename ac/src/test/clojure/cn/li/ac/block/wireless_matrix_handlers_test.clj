(ns cn.li.ac.block.wireless-matrix-handlers-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.block.wireless-matrix.handlers :as handlers]))

(defn- handler-failure?
  [result]
  (and (map? result) (false? (:success result))))

(deftest init-network-requires-owner-test
  (testing "non-owner init is rejected"
    (with-redefs [handlers/owner-controller (fn [_ _] nil)
                  handlers/create-network! (fn [& _] (throw (ex-info "should-not-call" {})))]
      (is (handler-failure?
           (handlers/handle-init-network {:ssid "s" :password "p"} :player)))))

  (testing "owner init calls create-network"
    (let [called (atom nil)]
      (with-redefs [handlers/owner-controller (fn [_ _] {:ctrl :tile})
                    handlers/create-network! (fn [tile ssid password]
                                              (reset! called [tile ssid password])
                                              true)]
        (let [result (handlers/handle-init-network {:ssid "abc" :password "pw"} :player)]
          (is (true? (:success result)))
          (is (= [:tile "abc" "pw"] @called))))))

  (testing "owner init failure is normalized"
    (with-redefs [handlers/owner-controller (fn [_ _] {:ctrl :tile})
                  handlers/create-network! (fn [& _] (throw (RuntimeException. "boom")))]
      (let [result (handlers/handle-init-network {:ssid "abc" :password "pw"} :player)]
        (is (handler-failure? result))
        (is (= :exception (:reason result)))))))

(deftest change-operations-require-owner-test
  (testing "non-owner change requests are rejected"
    (with-redefs [handlers/owner-controller (fn [_ _] nil)
                  handlers/wireless-network (fn [& _] (throw (ex-info "should-not-call" {})))
                  handlers/change-ssid! (fn [& _] (throw (ex-info "should-not-call" {})))
                  handlers/change-password! (fn [& _] (throw (ex-info "should-not-call" {})))]
      (is (handler-failure?
           (handlers/handle-change-ssid {:new-ssid "new"} :player)))
      (is (handler-failure?
           (handlers/handle-change-password {:new-password "newpw"} :player)))))

  (testing "owner change-ssid success path"
    (let [called (atom nil)]
      (with-redefs [handlers/owner-controller (fn [_ _] {:ctrl :tile})
                    handlers/wireless-network (fn [_] :net)
                    handlers/change-ssid! (fn [net ssid]
                                            (reset! called [net ssid])
                                            true)]
        (let [result (handlers/handle-change-ssid {:new-ssid "new"} :player)]
          (is (true? (:success result)))
          (is (= [:net "new"] @called))))))

  (testing "owner change-password success path"
    (let [called (atom nil)]
      (with-redefs [handlers/owner-controller (fn [_ _] {:ctrl :tile})
                    handlers/wireless-network (fn [_] :net)
                    handlers/change-password! (fn [net password]
                                                (reset! called [net password])
                                                true)]
        (let [result (handlers/handle-change-password {:new-password "newpw"} :player)]
          (is (true? (:success result)))
          (is (= [:net "newpw"] @called))))))

  (testing "owner change-ssid failure is normalized"
    (with-redefs [handlers/owner-controller (fn [_ _] {:ctrl :tile})
                  handlers/wireless-network (fn [_] :net)
                  handlers/change-ssid! (fn [& _] (throw (RuntimeException. "boom")))]
      (let [result (handlers/handle-change-ssid {:new-ssid "new"} :player)]
        (is (handler-failure? result))
        (is (= :exception (:reason result)))))))

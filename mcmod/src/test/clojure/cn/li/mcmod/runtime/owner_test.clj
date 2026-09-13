(ns cn.li.mcmod.runtime.owner-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.runtime.owner :as owner]))

(def ^:private valid-client-owner
  {:logical-side :client
   :client-session-id :session-a
   :player-uuid "player-a"})

(def ^:private valid-server-owner
  {:logical-side :server
   :server-session-id [:server 1]
   :player-uuid "player-a"})

(deftest client-owner-validation-test
  (testing "valid client owner passes"
    (is (owner/valid-client-owner? valid-client-owner))
    (is (= valid-client-owner
           (owner/require-client-owner valid-client-owner))))
  (testing "missing client-session-id fails"
    (is (false? (owner/valid-client-owner?
                 {:logical-side :client :player-uuid "player-a"})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"client-owner contract violation"
                          (owner/require-client-owner
                           {:logical-side :client :player-uuid "player-a"}))))
  (testing "wrong logical-side fails"
    (is (false? (owner/valid-client-owner?
                 (assoc valid-client-owner :logical-side :server)))))
  (testing "missing player-uuid fails require-client-owner"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"client-owner requires :player-uuid"
                          (owner/require-client-owner
                           {:logical-side :client :client-session-id :session-a})))))

(deftest server-owner-validation-test
  (testing "valid server owner passes"
    (is (owner/valid-server-owner? valid-server-owner))
    (is (= valid-server-owner
           (owner/require-server-owner valid-server-owner))))
  (testing "missing server-session-id fails"
    (is (false? (owner/valid-server-owner?
                 {:logical-side :server :player-uuid "player-a"})))))

(deftest owner-validation-test
  (testing "accepts client or server owner"
    (is (owner/valid-owner? valid-client-owner))
    (is (owner/valid-owner? valid-server-owner)))
  (testing "mixed side keys fail"
    (is (false? (owner/valid-owner?
                 {:logical-side :client
                  :server-session-id [:server 1]
                  :client-session-id :session-a
                  :player-uuid "player-a"})))))

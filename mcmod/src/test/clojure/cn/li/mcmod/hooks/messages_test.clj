(ns cn.li.mcmod.hooks.messages-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.hooks.messages :as messages]))

(defn- clean-messages-fixture
  [f]
  (messages/clear-messages!)
  (f)
  (messages/clear-messages!))

(use-fixtures :each clean-messages-fixture)

(deftest register-and-resolve-message-test
  (messages/register-message! :test/message "test:message")
  (is (= "test:message" (messages/maybe-msg-id :test/message)))
  (is (= "test:message" (messages/msg-id :test/message)))
  (is (messages/valid-msg-id? "test:message"))
  (is (not (messages/valid-msg-id? "missing:message"))))

(deftest register-messages-bulk-test
  (messages/register-messages! {:one "test:one"
                                :two "test:two"})
  (is (= "test:one" (messages/msg-id :one)))
  (is (= "test:two" (messages/msg-id :two))))

(deftest validate-registration-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"keyword"
                        (messages/register-message! "not-a-keyword" "test:id")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"non-empty string"
                        (messages/register-message! :bad-id "")))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"not registered"
                        (messages/msg-id :missing))))

(ns cn.li.mcmod.command.context-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.command.context :as ctx]))

(deftest create-context-defaults-test
  (testing "create-context fills optional maps with defaults"
    (let [context (ctx/create-context {:player :p :world :w :source :s})]
      (is (= :p (ctx/get-player context)))
      (is (= :w (ctx/get-world context)))
      (is (= :s (ctx/get-source context)))
      (is (= {} (ctx/get-arguments context)))
      (is (= {} (:metadata context)))
      (is (nil? (ctx/get-target-player context))))))

(deftest arguments-and-metadata-access-test
  (let [context (ctx/create-context {:arguments {:a 1}
                                     :metadata {:trace-id "t-1"}})]
    (testing "argument lookup supports keyword and string names"
      (is (= 1 (ctx/get-argument context :a)))
      (is (= 1 (ctx/get-argument context "a")))
      (is (nil? (ctx/get-argument context :missing))))
    (testing "metadata lookup returns nil when absent"
      (is (= "t-1" (ctx/get-metadata context :trace-id)))
      (is (nil? (ctx/get-metadata context :missing))))))

(deftest mutation-helpers-test
  (let [base (ctx/create-context {})
        updated (-> base
                    (ctx/with-argument "power" 9)
                    (ctx/with-target-player :target)
                    (ctx/with-metadata :origin :console))]
    (is (= 9 (ctx/get-argument updated :power)))
    (is (= :target (ctx/get-target-player updated)))
    (is (= :console (ctx/get-metadata updated :origin)))))

(deftest extreme-argument-keys-test
  (let [context (ctx/create-context {:arguments {:kw 1 "str" 2 nil 3}})]
    (testing "keyword lookup and string lookup both map to keyword key"
      (is (= 1 (ctx/get-argument context :kw)))
      (is (= 1 (ctx/get-argument context "kw"))))
    (testing "string-only key is not found because accessor normalizes to keyword"
      (is (nil? (ctx/get-argument context :str)))
      (is (nil? (ctx/get-argument context "str"))))
    (testing "nil argument name can address nil key"
      (is (= 3 (ctx/get-argument context nil)))))
  (testing "with-argument nil key stores value under nil key"
    (let [updated (ctx/with-argument (ctx/create-context {}) nil :v)]
      (is (= :v (get (ctx/get-arguments updated) nil)))
      (is (= :v (ctx/get-argument updated nil))))))

(deftest metadata-key-mix-test
  (let [context (-> (ctx/create-context {:metadata {}})
                    (ctx/with-metadata :k1 1)
                    (ctx/with-metadata "k2" 2))]
    (testing "metadata key type is preserved; keyword/string keys do not coerce"
      (is (= 1 (ctx/get-metadata context :k1)))
      (is (= 2 (ctx/get-metadata context "k2")))
      (is (nil? (ctx/get-metadata context :k2)))
      (is (nil? (ctx/get-metadata context "k1"))))))


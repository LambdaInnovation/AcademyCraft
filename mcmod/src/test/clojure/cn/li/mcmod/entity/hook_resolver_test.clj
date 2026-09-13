(ns cn.li.mcmod.entity.hook-resolver-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.entity.hook-resolver :as hook-resolver]))

(defn- clean-resolvers-fixture
  [f]
  (hook-resolver/clear-resolvers!)
  (f)
  (hook-resolver/clear-resolvers!))

(use-fixtures :each clean-resolvers-fixture)

(deftest register-and-resolve-test
  (hook-resolver/register-resolver! :effect #({"foo" :bar} %))
  (is (fn? (hook-resolver/get-resolver :effect)))
  (is (= :bar (hook-resolver/resolve-impl-key :effect "foo")))
  (is (nil? (hook-resolver/resolve-impl-key :effect "missing")))
  (is (nil? (hook-resolver/resolve-impl-key :ray "foo"))))

(deftest validate-registration-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"keyword"
                        (hook-resolver/register-resolver! "effect" identity)))
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"function"
                        (hook-resolver/register-resolver! :effect :not-a-fn))))
(ns cn.li.mcmod.nbt.dsl-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcmod.nbt.dsl :as dsl]))

(deftest validate-nbt-field-spec-test
  (testing "known type spec is accepted"
    (is (true?
         (dsl/validate-nbt-field-spec
          {:field-key :energy
           :nbt-key "energy"
           :type :double}))))
  (testing "unknown type without custom converters fails"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown NBT type"
                          (dsl/validate-nbt-field-spec
                           {:field-key :payload
                            :nbt-key "payload"
                            :type :unsupported}))))
  (testing "unknown type with custom converter hooks is accepted"
    (is (true?
         (dsl/validate-nbt-field-spec
          {:field-key :payload
           :nbt-key "payload"
           :type :unsupported
           :custom-write (fn [& _] nil)
           :custom-read (fn [& _] nil)})))))

(deftest validate-world-list-spec-test
  (testing "valid list spec is accepted"
    (is (true?
         (dsl/validate-world-list-spec
          {:tag "entries"
           :atom :entries
           :to-nbt (fn [_] nil)
           :from-nbt (fn [_ _] nil)}))))
  (testing "missing required keys fail fast"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"world-nbt-list-spec contract violation"
                          (dsl/validate-world-list-spec
                           {:tag "entries"
                            :to-nbt (fn [_] nil)})))))


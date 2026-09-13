(ns cn.li.ac.command.dsl-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.command.dsl :as dsl]))

(defn- reset-command-registry! [f]
  (dsl/reset-command-registry-for-test!)
  (try
    (f)
    (finally
      (dsl/reset-command-registry-for-test!))))

(use-fixtures :each reset-command-registry!)

(deftest command-spec-core-test
  (let [spec (dsl/create-command-spec
              "aim"
              {:permission-level 2
               :subcommands
               {:cat {:arguments [{:name "category" :type :string}]
                      :executor-fn (fn [_] :ok)
                      :description "switch"}
                :learn {:arguments [{:name "skill" :type :word}]
                        :executor-fn (fn [_] :learn)}}})]
    (is (= "aim" (:id spec)))
    (is (= 2 (:permission-level spec)))
    (is (map? (:subcommands spec)))
    (is (= "cat" (:name (dsl/find-subcommand spec ["cat"]))))
    (is (fn? (dsl/get-executor spec ["learn"])))
    (is (= 2 (dsl/get-permission-level spec ["learn"]))))
  (let [leaf (dsl/create-command-spec
              "ac"
              {:arguments [{:name "player" :type :player}]
               :executor-fn (fn [_] :ok)})]
    (dsl/register-command! leaf)
    (is (= leaf (dsl/get-command "ac")))
    (is (= #{"ac"} (set (dsl/list-commands))))))

(deftest command-edge-cases-test
  (is (thrown? clojure.lang.ExceptionInfo
               (dsl/create-argument-spec "x" {:type :not-exists})))
  (is (thrown? clojure.lang.ExceptionInfo
               (dsl/create-command-spec :bad {:executor-fn identity})))
  (is (thrown? clojure.lang.ExceptionInfo
               (dsl/create-command-spec "x" {:executor-fn identity
                                             :subcommands {:a {:executor-fn identity}}})))
  (is (thrown? clojure.lang.ExceptionInfo
               (dsl/create-command-spec "x" {:description "none"}))))

(deftest command-contract-test
  (let [spec (dsl/create-command-spec
              "root"
              {:permission-level 1
               :subcommands
               {:admin {:permission-level 4 :executor-fn (fn [_] :admin)}
                :user {:executor-fn (fn [_] :user)}}})]
    (is (= 4 (dsl/get-permission-level spec ["admin"])))
    (is (= 1 (dsl/get-permission-level spec ["user"])))
    (is (= nil (dsl/find-subcommand spec ["not-found"])))
    (is (= nil (dsl/get-executor spec ["not-found"])))
    (is (= nil (dsl/get-arguments spec ["not-found"])))))

(deftest nested-subcommand-path-test
  (let [spec (dsl/create-command-spec
              "root"
              {:permission-level 0
               :subcommands
               {:admin {:permission-level 2
                       :description "admin branch"
                       :subcommands
                       {:grant {:permission-level 5
                                :executor-fn (fn [_] :granted)
                                :description "grant op"}}}}})
        leaf (dsl/find-subcommand spec ["admin" "grant"])]
    (is (= "grant" (:name leaf)))
    (is (= :granted ((dsl/get-executor spec ["admin" "grant"]) {})))
    (is (= 5 (dsl/get-permission-level spec ["admin" "grant"])))
    (is (= nil (dsl/find-subcommand spec ["admin" "missing"])))
    (is (= nil (dsl/get-executor spec ["admin" "missing"])))))

(deftest command-registry-duplicate-and-freeze-policy-test
  (let [first-spec (dsl/create-command-spec "dup" {:executor-fn (fn [_] :first)})
        second-spec (dsl/create-command-spec "dup" {:executor-fn (fn [_] :second)})]
    (is (= first-spec (dsl/register-command! first-spec)))
    (is (= first-spec (dsl/register-command! second-spec)))
    (dsl/freeze-command-registry!)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Command registry is frozen"
                          (dsl/register-command!
                           (dsl/create-command-spec "new" {:executor-fn identity}))))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Command registry is frozen"
                          (dsl/unregister-command! "dup")))))



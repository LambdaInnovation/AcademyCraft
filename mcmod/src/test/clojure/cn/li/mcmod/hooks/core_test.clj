(ns cn.li.mcmod.hooks.core-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.hooks.core :as hooks]))

(defn- with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst)))
      (f)
      (finally
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(defn- clean-hooks-fixture
  [f]
  ;; reset by installing no-op overrides for deterministic tests
  (hooks/register-power-runtime-hooks! {:on-player-login! (fn [_] nil)
                                        :on-player-logout! (fn [_] nil)})
  (f)
  (hooks/register-power-runtime-hooks! {:on-player-login! (fn [_] nil)
                                        :on-player-logout! (fn [_] nil)}))

(use-fixtures :each clean-hooks-fixture)

(deftest register-runtime-hooks-validation-test
  (testing "valid known hook keys and fn values are accepted"
    (is (nil? (hooks/register-power-runtime-hooks!
               {:on-player-login! (fn [_] :ok)
                :on-player-logout! (fn [_] :ok)}))))
  (testing "unknown keys fail fast"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"Unknown runtime hook keys"
                          (hooks/register-power-runtime-hooks!
                           {:unknown/hook (fn [& _] nil)}))))
  (testing "non-fn values fail contract"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"runtime-hooks contract violation"
                          (hooks/register-power-runtime-hooks!
                           {:on-player-login! :not-a-fn})))))

(deftest player-state-dirty-defaults-to-true-test
  (testing "unregistered player-state-dirty? hook conservatively reports dirty"
    (is (true? (hooks/player-state-dirty? "any-uuid"))))
  (testing "content-registered hook is honored"
    (with-framework
      #(do
         (hooks/register-power-runtime-hooks! {:player-state-dirty? (fn [uuid] (= uuid "dirty-one"))})
         (is (true? (hooks/player-state-dirty? "dirty-one")))
         (is (false? (hooks/player-state-dirty? "clean-one")))))))

(deftest client-tick-hooks-publish-on-registration-test
  (with-framework
    #(do
       (hooks/register-power-runtime-hooks!
        {:client-tick! (fn [] :client-ticked)
         :client-on-slot-key-down! (fn [player-uuid key-idx]
                                      [player-uuid key-idx])})
       (is (= :client-ticked (hooks/client-tick!)))
       (is (= ["player" 2] (hooks/client-on-slot-key-down! "player" 2))))))

(ns cn.li.mcmod.command.runtime-hooks-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.command.runtime-hooks :as sut]))

(defn- with-framework [f]
  (let [prev-fw fw/framework]
    (try
      (when-let [fw-inst (fw/create-framework)]
        (alter-var-root #'fw/framework (constantly fw-inst))
        (sut/reset-command-hooks-for-test!)
        (f))
      (finally
        (sut/reset-command-hooks-for-test!)
        (alter-var-root #'fw/framework (constantly prev-fw))))))

(use-fixtures :each with-framework)

(deftest content-can-claim-unset-command-hook-test
  (let [calls (atom 0)
        init! (fn [] (swap! calls inc))]
    (is (nil? (sut/register-command-hooks! {:init-commands! init!})))
    (sut/init-commands!)
    (is (= 1 @calls))))

(deftest duplicate-content-registration-conflicts-test
  (sut/register-command-hooks! {:init-commands! (fn [] :first)})
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"Conflicting command hook"
        (sut/register-command-hooks! {:init-commands! (fn [] :second)}))))

(deftest init-commands-noop-without-registration-test
  (is (nil? (sut/init-commands!))))

(ns cn.li.ac.ability.server.handlers.common-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.server.handlers.common :as handlers-common]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-handler-common-runtime! [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (try
        (f)
        (finally
          (store/reset-store!))))))

(use-fixtures :each reset-handler-common-runtime!)

(deftest get-state-uses-bound-owner-session-test
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id :handler-session
                                                 :player-uuid "p1"}} (fn [] (let [state (handlers-common/get-state "p1")]
      (is (map? state))
      (is (some? (store/get-player-state :handler-session "p1")))))))

(deftest get-state-session-resolution-still-fail-fast-test
  (runtime-hooks/with-client-ctx-fn {:player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"requires bound :server-session-id"
                          (handlers-common/get-state "p2"))))))

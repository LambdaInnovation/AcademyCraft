(ns cn.li.mc1201.gui.menu.proxy-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.mcbase.gui.menu.proxy :as menu-proxy]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest owner-map-for-player-context-requires-client-session-test
  (testing "client menu owner fails without bound client session"
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"GUI menu owner requires session id"
                          (#'menu-proxy/owner-map-for-player-context
                           {:player-uuid "player-a"
                            :client-session-id nil})))))

(deftest owner-map-for-player-context-uses-canonical-client-session-test
  (let [session-id [:client-session 1 2]]
    (testing "canonical client owner when session and uuid are present"
      (is (= {:logical-side :client
              :client-session-id session-id
              :player-uuid "00000000-0000-0000-0000-000000000001"}
             (dissoc (#'menu-proxy/owner-map-for-player-context
                      {:player :stub-player
                       :player-uuid "00000000-0000-0000-0000-000000000001"
                       :client-session-id session-id})
                     :player))))))

(deftest owner-for-player-uses-bound-client-session-test
  (let [session-id [:client-session 9 9]]
    (runtime-hooks/with-client-ctx-fn {:session-id session-id} (fn [] (is (= session-id
             (:client-session-id
              (#'menu-proxy/owner-map-for-player-context
               {:player-uuid "player-a"
                :client-session-id (runtime-hooks/client-session-id)}))))))))

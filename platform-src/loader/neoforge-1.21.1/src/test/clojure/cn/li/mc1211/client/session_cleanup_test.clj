(ns cn.li.mc1211.client.session-cleanup-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.mcbase.client.session-cleanup-core :as cleanup-core]
            [cn.li.mc1211.client.session-cleanup :as cleanup]))

(def ^:private owner-a {:client-session-id [:client :session-a]
                        :player-uuid "player-a"})
(def ^:private owner-b {:client-session-id [:client :session-b]
                        :player-uuid "player-b"})

(use-fixtures :each
  (fn [f]
    (cleanup/call-with-session-cleanup-runtime
      (cleanup/create-session-cleanup-runtime)
      (fn []
        (f)))))

(deftest tick-connection-change-clears-previous-owner-on-transition-test
  (let [connection-key* (atom :conn-a)
        owner* (atom owner-a)
        cleared (atom [])]
    ;; Redef the core var, not the version module's re-export. `(def
    ;; clear-owner-state! shared/clear-owner-state!)` copies the *value* into a
    ;; second var, so rebinding that copy leaves the one `tick-connection-change!`
    ;; actually resolves untouched — the real cleanup would run instead, throw
    ;; on the headless Minecraft/getInstance, get swallowed by the caller's
    ;; try/catch, and the assertion below would see an empty log.
    (with-redefs [client-session/connection-key (fn [] @connection-key*)
                  client-session/current-local-player-owner (fn [] @owner*)
                  cleanup-core/clear-owner-state! (fn [owner opts]
                                                    (swap! cleared conj [owner opts])
                                                    nil)]
      (cleanup/tick-connection-change!)
      (is (= {:connection-key :conn-a
              :owner owner-a}
             (cleanup/cleanup-state-snapshot)))
      (reset! connection-key* :conn-b)
      (reset! owner* owner-b)
      (cleanup/tick-connection-change! {:clear-owner-input-state! :marker})
      (is (= [[owner-a {:clear-owner-input-state! :marker}]]
             @cleared))
      (is (= {:connection-key :conn-b
              :owner owner-b}
             (cleanup/cleanup-state-snapshot))))))

(deftest session-cleanup-runtime-isolation-test
  (let [runtime-b (cleanup/create-session-cleanup-runtime)]
    (cleanup/reset-cleanup-state-for-test! {:connection-key :conn-a
                                            :owner owner-a})
    (cleanup/call-with-session-cleanup-runtime
      runtime-b
      (fn []
        (cleanup/reset-cleanup-state-for-test! {:connection-key :conn-b
                                                :owner owner-b})
        (is (= {:connection-key :conn-b
                :owner owner-b}
               (cleanup/cleanup-state-snapshot)))))
    (is (= {:connection-key :conn-a
            :owner owner-a}
           (cleanup/cleanup-state-snapshot)))))

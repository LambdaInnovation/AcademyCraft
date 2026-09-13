(ns cn.li.ac.terminal.client.runtime-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.terminal.client.runtime :as runtime]
            [cn.li.ac.test.support.framework :refer [with-fresh-framework]]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.runtime.owner :as owner]))

(defn- reset-fixture [f]
  ;; Terminal runtime state lives in the Framework atom
  ;; ([:service :terminal-runtime]) — a fresh framework isolates it.
  (with-fresh-framework
    (fn []
      (runtime/reset-states-for-test!)
      (try
        (f)
        (finally
          (runtime/reset-states-for-test!))))))

(use-fixtures :each reset-fixture)

(deftest player-owner-invokes-client-session-id-test
  ;; Regression: player-owner used to store the client-session-id *function*
  ;; itself (truthy under `or`), which then failed :client-owner validation
  ;; when Left-Alt toggled the terminal.
  (runtime-hooks/with-client-ctx-fn {:session-id [:client-session :test]}
    (fn []
      (let [po (runtime/player-owner "380df991-f603-344c-a090-369bad2a924a")]
        (is (= [:client-session :test] (:client-session-id po)))
        (is (false? (fn? (:client-session-id po))))
        (is (owner/valid-client-owner? po)))))
  (let [po (runtime/player-owner "no-bound-session")]
    (is (= [:terminal-client "no-bound-session"] (:client-session-id po)))
    (is (owner/valid-client-owner? po))))

(deftest query-state-isolated-by-owner-test
  (let [owner-a {:client-session-id :session-a :screen-id :terminal :player-uuid "a"}
        owner-b {:client-session-id :session-a :screen-id :terminal :player-uuid "b"}]
    (with-redefs [net-client/send-to-server
                  (fn [_owner _msg-id _payload callback]
                    (callback {:terminal-installed? true
                               :installed-apps ["media-player"]
                               :available-apps ["media-player"]}))]
      (runtime/dispatch-event! owner-a :terminal/query-response
                               {:terminal-installed? true
                                :installed-apps ["media-player"]
                                :available-apps ["media-player"]})
      (runtime/dispatch-event! owner-b :terminal/query-response
                               {:terminal-installed? false
                                :installed-apps []
                                :available-apps ["map"]}))
    (is (= true (:terminal-installed? (runtime/state-snapshot owner-a))))
    (is (= #{:media-player} (:installed-apps (runtime/state-snapshot owner-a))))
    (is (= false (:terminal-installed? (runtime/state-snapshot owner-b))))
    (is (= #{} (:installed-apps (runtime/state-snapshot owner-b))))))

(deftest install-and-uninstall-app-update-only-owner-test
  (let [owner-a {:client-session-id :session-a :screen-id :terminal :player-uuid "a"}
        owner-b {:client-session-id :session-a :screen-id :terminal :player-uuid "b"}]
    (runtime/dispatch-event! owner-a :terminal/install-app-result
                             {:success true :app-id (keyword "media-player")})
    (runtime/dispatch-event! owner-b :terminal/install-app-result {:success true :app-id :map})
    (runtime/dispatch-event! owner-a :terminal/uninstall-app-result
                             {:success true :app-id (keyword "media-player")})
    (is (= #{} (:installed-apps (runtime/state-snapshot owner-a))))
    (is (= #{:map} (:installed-apps (runtime/state-snapshot owner-b))))))

(deftest cleared-owner-ignores-stale-generation-test
  (let [owner {:client-session-id :session-a :screen-id :terminal :player-uuid "a"}
        generation (runtime/ensure-owner! owner)]
    (runtime/dispatch-event! owner :terminal/install-app-start nil)
    (is (= true (:loading? (runtime/state-snapshot owner))))
    (runtime/clear-state! owner)
    (is (= false (runtime/owner-active? owner generation)))))

(deftest ui-open-flag-test
  (is (false? (runtime/ui-open?)))
  (runtime/mark-ui-open! true)
  (is (true? (runtime/ui-open?)))
  (runtime/mark-ui-open! false)
  (is (false? (runtime/ui-open?))))
(deftest owner-key-requires-player-uuid-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Terminal owner requires :player-uuid"
                        (runtime/state-snapshot {:client-session-id :session-a
                                                 :screen-id :terminal}))))

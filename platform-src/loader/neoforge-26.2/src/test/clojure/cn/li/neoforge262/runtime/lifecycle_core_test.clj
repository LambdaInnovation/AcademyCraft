(ns cn.li.neoforge262.runtime.lifecycle-core-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.mc262.runtime.lifecycle-core :as lifecycle-core]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def ^:private test-owner {:server-session-id :test-session})

(defn- sync-payload
  [uuid]
  {:uuid uuid :ability-data {:x 1}})

(deftest login-immediate-sync-clears-dirty-test
  (let [called (atom [])]
    (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [_] "p-login")
                     #'runtime-hooks/on-player-login! (fn [_] nil)
                     #'runtime-hooks/build-sync-payload sync-payload}
      (fn []
        (lifecycle-core/on-player-login!
          :player
          (merge test-owner
                 {:mark-player-dirty! (fn [owner uuid]
                                        (swap! called conj [:dirty owner uuid]))
                  :send-sync-now! (fn [uuid payload]
                                    (swap! called conj [:sync uuid payload]))
                  :clear-player-dirty! (fn [owner uuid]
                                         (swap! called conj [:clear owner uuid]))}))))
    (is (= [[:dirty test-owner "p-login"]
            [:sync "p-login" (sync-payload "p-login")]
            [:clear test-owner "p-login"]]
           @called))))

(deftest dimension-change-prefers-immediate-sync-over-tick-sync-test
  (let [called (atom [])]
    (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [_] "p-dim")
                     #'runtime-hooks/on-player-dimension-change! (fn [_ _ _] nil)
                     #'runtime-hooks/build-sync-payload sync-payload}
      (fn []
        (lifecycle-core/on-player-dimension-change!
          :player
          "minecraft:overworld"
          "minecraft:the_nether"
          (merge test-owner
                 {:mark-player-dirty! (fn [owner uuid]
                                        (swap! called conj [:dirty owner uuid]))
                  :send-sync-now! (fn [uuid payload]
                                    (swap! called conj [:sync uuid payload]))
                  :clear-player-dirty! (fn [owner uuid]
                                         (swap! called conj [:clear owner uuid]))
                  :tick-sync! (fn [_send-sync-fn owner]
                                (swap! called conj [:tick owner]))
                  :send-sync-fn (fn [_uuid _payload] nil)}))))
    (is (= [[:dirty test-owner "p-dim"]
            [:sync "p-dim" (sync-payload "p-dim")]
            [:clear test-owner "p-dim"]]
           @called))))

(deftest clone-immediate-sync-targets-new-player-test
  (let [called (atom [])]
    (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [p] (case p :old "old-uuid" :new "new-uuid"))
                     #'runtime-hooks/on-player-clone! (fn [_old _new] nil)
                     #'runtime-hooks/build-sync-payload sync-payload}
      (fn []
        (lifecycle-core/on-player-clone!
          :old
          :new
          true
          (merge test-owner
                 {:mark-player-dirty! (fn [owner uuid]
                                        (swap! called conj [:dirty owner uuid]))
                  :send-sync-now! (fn [uuid payload]
                                    (swap! called conj [:sync uuid payload]))
                  :clear-player-dirty! (fn [owner uuid]
                                         (swap! called conj [:clear owner uuid]))}))))
    (is (= [[:dirty test-owner "new-uuid"]
            [:sync "new-uuid" (sync-payload "new-uuid")]
            [:clear test-owner "new-uuid"]]
           @called))))

(deftest login-immediate-sync-requires-two-arity-sender-test
  (let [called (atom [])]
    (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [_] "p-login")
                     #'runtime-hooks/on-player-login! (fn [_] nil)
                     #'runtime-hooks/build-sync-payload sync-payload}
      (fn []
        (is (thrown? clojure.lang.ArityException
                     (lifecycle-core/on-player-login!
                       :player
                       (merge test-owner
                              {:mark-player-dirty! (fn [owner uuid]
                                                     (swap! called conj [:dirty owner uuid]))
                               :send-sync-now! (fn [uuid]
                                                 (swap! called conj [:sync uuid]))
                               :clear-player-dirty! (fn [owner uuid]
                                                      (swap! called conj [:clear owner uuid]))}))))))
    (is (= [[:dirty test-owner "p-login"]]
           @called))))

(deftest lifecycle-binds-player-state-owner-test
  (let [seen (atom nil)]
    (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [_] "p-login")
                     #'runtime-hooks/on-player-login! (fn [_]
                                                        (reset! seen (runtime-hooks/player-state-owner)))}
      (fn []
        (lifecycle-core/on-player-login! :player test-owner)))
    (is (= test-owner @seen))))

(deftest server-stop-cleans-session-with-bound-owner-test
  (let [server (Object.)
        expected-session-id [:server (System/identityHashCode server)]
        seen-owner (atom nil)
        called (atom [])]
    (with-redefs-fn {#'runtime-hooks/on-server-stop! (fn [session-id]
                                                       (reset! seen-owner (runtime-hooks/player-state-owner))
                                                       (swap! called conj [:hook session-id]))}
      (fn []
        (lifecycle-core/on-server-stop!
          server
          {:cleanup-session! (fn [session-id]
                               (swap! called conj [:cleanup session-id]))})))
    (is (= {:server-session-id expected-session-id} @seen-owner))
    (is (= [[:hook expected-session-id]
            [:cleanup expected-session-id]]
           @called))))

(deftest sync-owner-is-required-test
  (with-redefs-fn {#'cn.li.mcbase.runtime.lifecycle-core/player-uuid (fn [_] "p-login")
                   #'runtime-hooks/on-player-login! (fn [_] nil)}
    (fn []
      (is (thrown-with-msg? clojure.lang.ExceptionInfo
                            #"requires :server-session-id"
                            (lifecycle-core/on-player-login!
                              :player
                              {:mark-player-dirty! (fn [_owner _uuid])}))))))

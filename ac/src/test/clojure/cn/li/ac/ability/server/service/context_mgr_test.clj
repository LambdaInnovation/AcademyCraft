(ns cn.li.ac.ability.server.service.context-mgr-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.test.support.owner :as owner-support]
            [cn.li.ac.ability.service.context-manager :as cm]
            [cn.li.ac.ability.service.context-state :as ctx-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.messages :as catalog]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner]))

(defn- reset-fixture [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture
     (fn []
       (skill-registry/install-skill-registry-runtime!
        (skill-registry/create-skill-registry-runtime))
       (evt/install-event-subscriber-runtime!
        (evt/create-event-subscriber-runtime))
       (cm/register-send-fns! {:to-client nil :to-server nil})
       (try
         (f)
         (finally
           (skill-registry/install-skill-registry-runtime!
            (skill-registry/create-skill-registry-runtime))
           (evt/install-event-subscriber-runtime!
            (evt/create-event-subscriber-runtime))
           (cm/register-send-fns! {:to-client nil :to-server nil})))))))

(use-fixtures :each reset-fixture)

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})
(def ^:private test-server-session-id :test-session)

(defn- server-owner
  [player-uuid]
  {:logical-side :server
   :server-session-id test-server-session-id
   :player-uuid (str player-uuid)})

(defn- with-server-player-owner
  [player-uuid f]
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id test-server-session-id
                                                 :player-uuid player-uuid}} (fn [] (f))))

(defn- seed-player! [uuid skill-kw]
  (when-not (skill-registry/get-skill skill-kw)
    (skill-registry/register-skill! {:id skill-kw
                                     :category-id :electromaster
                                     :level 1
                                     :pattern :passive}))
  (let [ability-data (-> (ad/new-ability-data)
                         (ad/learn-skill skill-kw))
        resource-data (assoc (rd/new-resource-data) :activated true)]
    (store/set-player-state! test-player/test-session-id
                  uuid
                  {:ability-data ability-data
                   :resource-data resource-data})))

(deftest activate-context-sends-begin-link-test
  (let [out (atom [])
        client-owner (owner-support/client-owner :test-session "player-1")]
    (cm/register-send-fns! {:to-server (fn [msg-id payload]
                                          (swap! out conj [msg-id payload]))
                            :to-client nil})
    (let [c (runtime-hooks/with-client-ctx-fn {:player-owner {:client-session-id :test-session
                                                             :player-uuid "player-1"}} (fn [] (ctx/with-context-owner client-owner
                (cm/activate-context! "player-1" :arc-gen))))]
      (is (= 1 (count @out)))
      (is (= catalog/MSG-CTX-BEGIN-LINK (first (first @out))))
      (is (= :arc-gen (:skill-id (second (first @out)))))
      (is (= (:id c) (:ctx-id (second (first @out))))))))

(deftest establish-context-success-test
  (let [out (atom [])]
    (seed-player! "p2" :arc-gen)
    (cm/register-send-fns! {:to-client (fn [uuid msg-id payload]
                                         (swap! out conj [uuid msg-id payload]))
                            :to-server nil})
    (let [res (ctx/with-context-owner (test-context-owner "p2")
          (cm/establish-context! "p2" "cid-1" :arc-gen))]
      (is (some? res))
      (is (= 1 (count (filter #(= catalog/MSG-CTX-ESTABLISH (second %)) @out)))))))

(deftest establish-context-rejects-unlearned-test
  (let [out (atom [])]
    (seed-player! "p3" :arc-gen)
    (cm/register-send-fns! {:to-client (fn [uuid msg-id payload]
                                         (swap! out conj [uuid msg-id payload]))
                            :to-server nil})
    (ctx/with-context-owner (test-context-owner "p3")
      (cm/establish-context! "p3" "cid-x" :other-skill))
    (is (some #(= catalog/MSG-CTX-TERMINATE (second %)) @out))))

(deftest push-channel-forwards-to-client-test
  (let [out (atom [])]
    (cm/register-send-fns! {:to-client (fn [uuid msg-id payload]
                                         (swap! out conj [uuid msg-id payload]))
                            :to-server nil})
    (cm/push-channel-to-player! "pu" "ctx-9" :fx {:k 1})
    (is (= 1 (count @out)))
    (let [[u mid p] (first @out)]
      (is (= "pu" u))
      (is (= catalog/MSG-CTX-CHANNEL mid))
      (is (= "ctx-9" (:ctx-id p)))
      (is (= :fx (:channel p)))
      (is (= {:k 1} (:payload p))))))

(deftest tick-context-manager-purges-old-terminated-contexts-test
  (let [old-ts (- (System/currentTimeMillis) 5000)
        fresh-ts (System/currentTimeMillis)
        old-ctx-id "ctx-old-terminated"
        fresh-ctx-id "ctx-fresh-terminated"]
    (ctx/register-context! (assoc (ctx/new-server-context "p-old" :arc-gen old-ctx-id (test-context-owner "p-old"))
                                  :status ctx/STATUS-TERMINATED
                                  :terminated-at-ms old-ts))
    (ctx/register-context! (assoc (ctx/new-server-context "p-fresh" :arc-gen fresh-ctx-id (test-context-owner "p-fresh"))
                                  :status ctx/STATUS-TERMINATED
                                  :terminated-at-ms fresh-ts))
    (cm/tick-context-manager!)
    (is (nil? (ctx/get-context (test-context-owner "p-old") old-ctx-id)))
    (is (some? (ctx/get-context (test-context-owner "p-fresh") fresh-ctx-id)))))

(deftest tick-context-manager-terminates-expired-alive-context-test
  (let [ctx-id "ctx-timeout"
        terminated (atom [])]
    (ctx/register-context! (assoc (ctx/new-server-context "p-timeout" :arc-gen ctx-id (test-context-owner "p-timeout"))
                                  :last-keepalive-ms (- (System/currentTimeMillis) 5000)))
    (cm/register-send-fns! {:to-client (fn [_uuid msg-id payload]
                                         (when (= catalog/MSG-CTX-TERMINATE msg-id)
                                           (swap! terminated conj (:ctx-id payload))))
                            :to-server nil})
    (cm/tick-context-manager!)
    (is (= [ctx-id] @terminated))
    (ctx/with-context-owner (test-context-owner "p-timeout")
      (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context ctx-id)))))))

(deftest tick-player-contexts-drives-only-owned-active-server-contexts-test
  (let [calls (atom [])]
    (ctx/register-context! (assoc (ctx/new-server-context "p1" :arc-gen "cid-1" (server-owner "p1"))
                                  :input-state ctx-rt/INPUT-ACTIVE))
    (ctx/register-context! (assoc (ctx/new-server-context "p1" :vec-accel "cid-2" (server-owner "p1"))
                                  :input-state ctx-rt/INPUT-IDLE))
    (ctx/register-context! (assoc (ctx/new-server-context "p2" :meltdowner "cid-1" (server-owner "p2"))
                                  :input-state ctx-rt/INPUT-ACTIVE))
    (with-redefs [ctx-rt/handle-key-tick! (fn [owner ctx-id payload terminate-fn]
                                            (swap! calls conj {:ctx-id ctx-id
                                                               :payload payload
                                                               :owner owner
                                                               :terminate-fn (some? terminate-fn)})
                                            true)]
      (with-server-player-owner "p1"
        #(cm/tick-player-contexts! "p1"))
      (is (= [{:ctx-id "cid-1"
               :payload {:ctx-id "cid-1" :skill-id :arc-gen :player nil}
               :owner (server-owner "p1")
               :terminate-fn true}]
             @calls)))))

(deftest abort-player-contexts-only-terminates-owned-server-contexts-test
  (let [terminated (atom [])
        client-ctx (assoc (ctx/new-context "p1" :arc-gen {:logical-side :client
                                                         :client-session-id :session-a
                                                         :player-uuid "p1"})
                          :status ctx/STATUS-ALIVE)]
    (ctx/register-context! (assoc (ctx/new-server-context "p1" :arc-gen "server-ctx" (server-owner "p1"))
                                  :status ctx/STATUS-ALIVE))
    (ctx/register-context! client-ctx)
    (cm/register-send-fns! {:to-client (fn [_uuid msg-id payload]
                                         (when (= catalog/MSG-CTX-TERMINATE msg-id)
                                           (swap! terminated conj (:ctx-id payload))))
                            :to-server nil})
    (with-server-player-owner "p1"
      #(cm/abort-player-contexts! "p1"))
    (is (= ["server-ctx"] @terminated))
    (is (= ctx/STATUS-TERMINATED
           (:status (ctx/with-context-owner (server-owner "p1")
                      (ctx/get-context "server-ctx")))))
    (is (= ctx/STATUS-ALIVE
           (:status (ctx/get-context {:logical-side :client
                                     :client-session-id :session-a
                                     :player-uuid "p1"}
                                     (:id client-ctx)))))))

(deftest dispatcher-timing-is-frozen-for-runtime-test
  (is (= 1500 (cm/keepalive-timeout-ms)))
  (is (= 1000 (cm/terminated-context-grace-ms))))

(deftest establish-context-uses-bound-server-session-test
  (let [out (atom [])
        player-id "p-alt"
        skill-id :arc-gen
        alt-session :alt-server-session
        ability-data (-> (ad/new-ability-data)
                         (ad/learn-skill skill-id))
        resource-data (assoc (rd/new-resource-data) :activated true)]
    (when-not (skill-registry/get-skill skill-id)
      (skill-registry/register-skill! {:id skill-id
                                       :category-id :electromaster
                                       :level 1
                                       :pattern :passive}))
    (store/set-player-state! alt-session
                              player-id
                              {:ability-data ability-data
                               :resource-data resource-data})
    (cm/register-send-fns! {:to-client (fn [uuid msg-id payload]
                                         (swap! out conj [uuid msg-id payload]))
                            :to-server nil})
    (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id alt-session
                                                   :player-uuid player-id}} (fn [] (let [res (cm/establish-context! player-id "cid-alt" skill-id)]
        (is (some? res))
        (is (= alt-session (owner/transport-store-session-id res)))
        (is (= player-id (:player-uuid res)))
        (is (= 1 (count (filter #(= catalog/MSG-CTX-ESTABLISH (second %)) @out)))))))))

(deftest context-manager-session-resolution-still-fail-fast-test
  (runtime-hooks/with-client-ctx-fn {:player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"requires bound"
                          (cm/abort-player-contexts! "p-fail"))))))

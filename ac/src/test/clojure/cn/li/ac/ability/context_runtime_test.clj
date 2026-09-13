(ns cn.li.ac.ability.context-runtime-test
  (:require 
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.content.ability]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.registry.skill :as skill-reg]
            [cn.li.ac.ability.registry.event :as evt]            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.model.cooldown :as cd]
            [cn.li.ac.ability.service.context-state :as rt]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-test-state! [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture
     (fn []
       (skill-reg/install-skill-registry-runtime!
         (skill-reg/create-skill-registry-runtime))
       (evt/install-event-subscriber-runtime!
         (evt/create-event-subscriber-runtime))
       (try
         (f)
         (finally
           (skill-reg/install-skill-registry-runtime!
             (skill-reg/create-skill-registry-runtime))
           (evt/install-event-subscriber-runtime!
             (evt/create-event-subscriber-runtime))))))))

(use-fixtures :each reset-test-state!)

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(defn- seed-player-state!
  [uuid]
  (let [ability-data (-> (ad/new-ability-data)
                         (assoc :category-id :electromaster)
                         (update :learned-skills conj :arc-gen))
        resource-data (assoc (rd/new-resource-data) :activated true :cur-cp 100.0 :cur-overload 0.0)
        cooldown-data (cd/new-cooldown-data)]
      (store/set-player-state! test-player/test-session-id
                    uuid
                    {:ability-data ability-data
                     :resource-data resource-data
                     :cooldown-data cooldown-data
                     :preset-data {:active-preset 0 :slots {}}
                     :dirty-domains #{}})))

(deftest key-down-blocked-by-cooldown-test
  (let [uuid "test-player-cooldown"
        _ (seed-player-state! uuid)
  _ (command-rt/run-command-in-session! test-player/test-session-id
                uuid
                {:command :set-cooldown
                 :ctrl-id :arc-gen
                 :ticks 10
                 :sub-id :main})
        c (ctx/new-server-context uuid :arc-gen "ctx-cd" (test-context-owner uuid))]
    (ctx/register-context! c)
  (ctx/with-context-owner (test-context-owner uuid)
    (is (false? (rt/handle-key-down! "ctx-cd" {:ctx-id "ctx-cd" :skill-id :arc-gen}))
      "key-down should be rejected while main cooldown is active")
    (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context "ctx-cd")))
      "rejected key-down should terminate context"))))

(deftest key-tick-dispatches-while-active-test
  (let [uuid "test-player-resource"
        _ (seed-player-state! uuid)
        c (-> (ctx/new-server-context uuid :arc-gen "ctx-res" (test-context-owner uuid))
              (assoc :input-state :active))]
    (ctx/register-context! c)
  (ctx/with-context-owner (test-context-owner uuid)
    (is (true? (rt/handle-key-tick! "ctx-res" {:ctx-id "ctx-res" :skill-id :arc-gen}))
      "active context should accept key-tick")
    (is (= ctx/STATUS-ALIVE (:status (ctx/get-context "ctx-res")))
      "key-tick keeps context alive"))))

(deftest cooldown-policy-helper-test
  (testing "manual cooldown mode disables auto main cooldown"
    (is (false? (rt/should-apply-main-cooldown? {:cooldown {:mode :manual}}))))
  (testing "missing/other mode uses default auto cooldown"
    (is (true? (rt/should-apply-main-cooldown? {:cooldown {:mode :default}})))
    (is (true? (rt/should-apply-main-cooldown? {})))))

(deftest key-up-termination-policy-helper-test
  (testing "default policy terminates context on key-up"
    (is (true? (rt/should-terminate-context-on-key-up? {}))))
  (testing "skill can opt-out via input-policy"
    (is (false? (rt/should-terminate-context-on-key-up?
                  {:input-policy {:terminate-on-key-up? false}})))
    (is (true? (rt/should-terminate-context-on-key-up?
                 {:input-policy {:terminate-on-key-up? true}})))))

(deftest key-up-keep-active-policy-helper-test
  (testing "default policy does not keep active"
    (is (false? (rt/should-keep-active-on-key-up? {}))))
  (testing "explicit policy can keep active"
    (is (true? (rt/should-keep-active-on-key-up?
                 {:input-policy {:keep-active-on-key-up? true}})))
    (is (false? (rt/should-keep-active-on-key-up?
                  {:input-policy {:keep-active-on-key-up? false}})))))

(deftest key-up-can-keep-context-alive-when-policy-disables-termination-test
  (let [uuid "test-player-sticky"
        _ (seed-player-state! uuid)
        c (-> (ctx/new-server-context uuid :arc-gen "ctx-sticky" (test-context-owner uuid))
              (assoc :input-state :active))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :ctrl-id :arc-gen
                                         :cooldown {:mode :manual}
                                         :input-policy {:terminate-on-key-up? false}})]
      (ctx/with-context-owner (test-context-owner uuid)
      (is (true? (rt/handle-key-up! "ctx-sticky" {:ctx-id "ctx-sticky" :skill-id :arc-gen}))
        "key-up should be handled")
      (let [updated (ctx/get-context "ctx-sticky")]
        (is (= ctx/STATUS-ALIVE (:status updated))
          "context remains alive when termination policy is disabled")
        (is (= :idle (:input-state updated))
          "input state resets to :idle so subsequent key-down can reactivate"))))))

(deftest key-up-can-keep-context-active-when-policy-demands-active-follow-through-test
  (let [uuid "test-player-keep-active"
        _ (seed-player-state! uuid)
        c (-> (ctx/new-server-context uuid :arc-gen "ctx-keep-active" (test-context-owner uuid))
              (assoc :input-state :active))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :ctrl-id :arc-gen
                                         :cooldown {:mode :manual}
                                         :input-policy {:terminate-on-key-up? false
                                                        :keep-active-on-key-up? true}})]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-up! "ctx-keep-active" {:ctx-id "ctx-keep-active" :skill-id :arc-gen}))
            "key-up should be handled")
        (let [updated (ctx/get-context "ctx-keep-active")]
          (is (= ctx/STATUS-ALIVE (:status updated))
              "context remains alive when keep-active policy is enabled")
          (is (= :active (:input-state updated))
              "input state should remain active for post-release server tick follow-through"))))))

(deftest pattern-handled-key-up-skips-generic-perform-and-cooldown-test
  (let [uuid "test-player-pattern-key-up"
        _ (seed-player-state! uuid)
        ctx-id "ctx-pattern-key-up"
        c (-> (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid))
              (assoc :input-state :active))
        events* (atom [])]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :ctrl-id :arc-gen
                                         :pattern :release-cast
                                         :actions {:up! (fn [& _] nil)}
                                         :cooldown {:mode :default}
                                         :cooldown-ticks 9})
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (ctx/with-context-owner (test-context-owner uuid)
      (is (true? (rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
      (is (= 0 (cd/get-remaining (:cooldown-data (store/get-player-state test-player/test-session-id uuid)) :arc-gen :main))
        "generic key-up should not apply cooldown when pattern runtime owns settlement")
      (is (some #(= evt/EVT-CONTEXT-KEY-UP %) (map :event/type @events*))
          "key-up should emit context key-up event")
      (is (not-any? #(= evt/EVT-SKILL-PERFORM (:event/type %)) @events*))))))

(deftest key-input-lifecycle-dispatches-distinct-callback-keys-test
  (let [uuid "test-player-callbacks"
        _ (seed-player-state! uuid)
        ctx-id "ctx-callbacks"
        c (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid))
        callback-keys (atom [])
        terminated (atom [])]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :pattern :hold-channel
                         :actions {:down!  (fn [& _] (swap! callback-keys conj :on-key-down))
                               :tick!  (fn [& _] (swap! callback-keys conj :on-key-tick))
                               :up!    (fn [& _] (swap! callback-keys conj :on-key-up))
                               :abort! (fn [& _] (swap! callback-keys conj :on-key-abort))}
                                         :cooldown {:mode :manual}
                                         :input-policy {:terminate-on-key-up? false}})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :arc-gen} #(swap! terminated conj %))))
        (is (true? (rt/handle-key-tick! ctx-id {:ctx-id ctx-id :skill-id :arc-gen} #(swap! terminated conj %))))
        (is (true? (rt/handle-key-up! ctx-id {:ctx-id ctx-id :skill-id :arc-gen} #(swap! terminated conj %))))
        (is (true? (rt/handle-key-abort! ctx-id {:ctx-id ctx-id :skill-id :arc-gen} #(swap! terminated conj %))))
        (is (= [:on-key-down :on-key-tick :on-key-up :on-key-abort] @callback-keys))
        (is (= [ctx-id] @terminated))
        (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context ctx-id))))))))

(deftest context-state-uses-bound-owner-session-for-implicit-path-test
  (let [uuid "test-player-implicit-session"
        ctx-id "ctx-implicit-session"
        alt-session :ctx-state-alt]
    (store/set-player-state! alt-session
                              uuid
                              {:ability-data (-> (ad/new-ability-data)
                                                 (assoc :category-id :electromaster)
                                                 (update :learned-skills conj :arc-gen))
                               :resource-data (assoc (rd/new-resource-data) :activated true)
                               :cooldown-data (-> (cd/new-cooldown-data)
                                                  (cd/set-cooldown :arc-gen :main 5))
                               :preset-data {:active-preset 0 :slots {}}
                               :dirty-domains #{}})
    (ctx/register-context! (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid)))
    (ctx/with-context-owner (test-context-owner uuid)
      (runtime-hooks/with-player-state-owner-fn
        {:server-session-id alt-session
         :player-uuid uuid}
        (fn []
          (is (false? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :arc-gen})))
          (is (= ctx/STATUS-TERMINATED (:status (ctx/get-context ctx-id)))))))))

(deftest context-state-uses-context-owned-session-without-thread-player-owner-test
  (let [uuid "test-player-context-owner"
        ctx-id "ctx-context-owner"]
    (ctx/register-context! (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid)))
    (ctx/with-context-owner (test-context-owner uuid)
      (runtime-hooks/with-player-state-owner-fn
        nil
        (fn []
          (is (true? (rt/handle-key-down! ctx-id {:ctx-id ctx-id :skill-id :arc-gen}))))))))

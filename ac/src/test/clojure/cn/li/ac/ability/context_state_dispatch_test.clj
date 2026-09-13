(ns cn.li.ac.ability.context-state-dispatch-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-state :as rt]
            [cn.li.ac.ability.registry.skill :as skill-reg]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.model.cooldown :as cd]
            [cn.li.ac.ability.service.skill-callback :as skill-cb]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.content.ability]))

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

(defn- test-context-owner [uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str uuid)})

(defn- seed-player-state! [uuid cp]
  (store/set-player-state! test-player/test-session-id
                            uuid
                            {:ability-data (-> (ad/new-ability-data)
                                               (assoc :category-id :electromaster)
                                               (update :learned-skills conj :arc-gen))
                             :resource-data (assoc (rd/new-resource-data)
                                                   :activated true
                                                   :cur-cp cp
                                                   :cur-overload 0.0)
                             :cooldown-data (cd/new-cooldown-data)
                             :preset-data {:active-preset 0 :slots {}}
                             :dirty-domains #{}}))

(deftest resolve-action-key-mappings-test
  (testing ":instant key-down -> :perform!"
    (is (= :perform! (skill-cb/resolve-action-key :instant :on-key-down))))
  (testing ":toggle key-down/up -> activate/deactivate"
    (is (= :activate! (skill-cb/resolve-action-key :toggle :on-key-down)))
    (is (= :deactivate! (skill-cb/resolve-action-key :toggle :on-key-up))))
  (testing ":release-cast key-down/up -> down/up"
    (is (= :down! (skill-cb/resolve-action-key :release-cast :on-key-down)))
    (is (= :up! (skill-cb/resolve-action-key :release-cast :on-key-up)))))

(deftest instant-key-down-invokes-perform-test
  (let [uuid "dispatch-instant"
        ctx-id "ctx-instant"
        invoked? (atom false)
        _ (seed-player-state! uuid 100.0)
        c (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :pattern :instant
                                         :cost {:down {:cp 1.0 :overload 0.0}}
                                         :actions {:perform! (fn [& _]
                                                               (reset! invoked? true))}})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {})))
        (is (true? @invoked?) ":instant key-down must invoke :perform!")))))

(deftest toggle-key-down-up-invokes-activate-deactivate-test
  (let [uuid "dispatch-toggle"
        ctx-id "ctx-toggle"
        actions (atom [])
        _ (seed-player-state! uuid 100.0)
        c (ctx/new-server-context uuid :vec-deviation ctx-id (test-context-owner uuid))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :vec-deviation
                                         :pattern :toggle
                                         :cost {:down {:cp 0.0} :up {:cp 0.0}}
                                         :input-policy {:terminate-on-key-up? false}
                                         :actions {:activate! (fn [& _] (swap! actions conj :activate!))
                                                   :deactivate! (fn [& _] (swap! actions conj :deactivate!))}})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {})))
        (is (true? (rt/handle-key-up! ctx-id {})))
        (is (= [:activate! :deactivate!] @actions))))))

(deftest cost-fail-invokes-cost-fail-action-test
  (let [uuid "dispatch-cost-fail"
        ctx-id "ctx-cost-fail"
        stages (atom [])
        _ (seed-player-state! uuid 0.0)
        c (ctx/new-server-context uuid :arc-gen ctx-id (test-context-owner uuid))]
    (ctx/register-context! c)
    (with-redefs [skill-reg/get-skill (fn [_]
                                        {:id :arc-gen
                                         :pattern :instant
                                         :cost {:down {:cp 50.0 :overload 0.0}}
                                         :actions {:perform! (fn [& _] nil)
                                                   ;; unified 8-arg action arity:
                                                   ;; [ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]
                                                   :cost-fail! (fn [_ _ _ _ _ _ stage _]
                                                                 (swap! stages conj stage))}})
                  evt/fire-ability-event! (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner uuid)
        (is (true? (rt/handle-key-down! ctx-id {})))
        (is (= [:down] @stages) "cost failure must invoke :cost-fail! with :down stage")))))

(ns cn.li.ac.ability.skill-control-gate-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.test.support.contexts :as test-contexts]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.messages :as catalog]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.model.resource :as rd]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.context-manager :as cm]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]))

(defn- reset-fixture
  [f]
  (test-contexts/clean-contexts-fixture
   #(test-player/clean-player-states-fixture
     (fn []
       (cm/register-send-fns! {:to-client nil :to-server nil})
       (f)
       (cm/register-send-fns! {:to-client nil :to-server nil})))))

(use-fixtures :each reset-fixture)

(def ^:private test-context-owner
  {:logical-side :server :server-session-id :test-session :player-uuid "p-gate"})

(defn- seed-player!
  [player-uuid skill-id resource-data]
  (store/set-player-state! test-player/test-session-id
                            player-uuid
                            {:ability-data (-> (ad/new-ability-data)
                                               (ad/learn-skill skill-id))
                             :resource-data resource-data}))

(defn- activated-resource-data []
  (assoc (rd/new-resource-data) :activated true))

(defn- establish-result!
  [skill-spec resource-data]
  (let [out (atom [])
        player-uuid "p-gate"
        skill-id :arc-gen
        ctx-id "ctx-gate"]
    (seed-player! player-uuid skill-id resource-data)
    (cm/register-send-fns! {:to-client (fn [uuid msg-id payload]
                                         (swap! out conj [uuid msg-id payload]))
                            :to-server nil})
    (with-redefs [skill-registry/get-skill (fn [_] skill-spec)]
      (let [ctx-map (ctx/with-context-owner test-context-owner
              (cm/establish-context! player-uuid ctx-id skill-id))]
        {:ctx ctx-map
         :messages @out}))))

(deftest enabled-controllable-learned-skill-can-establish-context-test
  (let [{:keys [ctx messages]} (establish-result! {:id :arc-gen
                                                   :category-id :electromaster
                                                   :enabled true
                                                   :controllable? true}
                                                  (activated-resource-data))]
    (is (some? ctx))
    (is (= [catalog/MSG-CTX-ESTABLISH]
           (mapv second messages)))))

(deftest disabled-or-uncontrollable-skill-is-rejected-at-server-establish-test
  (testing "disabled learned skill"
    (let [{:keys [ctx messages]} (establish-result! {:id :arc-gen
                                                     :category-id :electromaster
                                                     :enabled false
                                                     :controllable? true}
                                                    (activated-resource-data))]
      (is (nil? ctx))
      (is (= [catalog/MSG-CTX-TERMINATE]
             (mapv second messages)))))
  (testing "non-controllable learned skill"
    (let [{:keys [ctx messages]} (establish-result! {:id :arc-gen
                                                     :category-id :electromaster
                                                     :enabled true
                                                     :controllable? false}
                                                    (activated-resource-data))]
      (is (nil? ctx))
      (is (= [catalog/MSG-CTX-TERMINATE]
             (mapv second messages))))))

(deftest unusable-resource-state-is-rejected-at-server-establish-test
  (let [{:keys [ctx messages]} (establish-result! {:id :arc-gen
                                                   :category-id :electromaster
                                                   :enabled true
                                                   :controllable? true}
                                                  (assoc (activated-resource-data)
                                                         :activated false))]
    (is (nil? ctx))
    (is (= [catalog/MSG-CTX-TERMINATE]
           (mapv second messages)))))

(deftest unknown-skill-is-rejected-even-if-player-state-claims-it-learned-test
  (let [{:keys [ctx messages]} (establish-result! nil (activated-resource-data))]
    (is (nil? ctx))
    (is (= [catalog/MSG-CTX-TERMINATE]
           (mapv second messages)))))



(ns cn.li.ac.ability.server.handlers.activation-handler-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.server.handlers.activation-handler :as activation-handler]            [cn.li.ac.ability.util.uuid :as uuid]))

(use-fixtures :each test-player/clean-player-states-fixture)

(defn- seed-player!
  [player-uuid ability-data resource-data]
  (store/set-player-state! test-player/test-session-id
                            player-uuid
                            {:ability-data ability-data
                             :resource-data resource-data}))

(deftest activation-request-without-category-is-ignored-test
  (let [events* (atom [])
        uuid "p1"]
    (seed-player! uuid (adata/new-ability-data) (rdata/new-resource-data))
    (with-redefs [uuid/player-uuid identity
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (activation-handler/handle-set-activated-request {:activated true} uuid)
      (is (false? (get-in (store/get-player-state test-player/test-session-id uuid) [:resource-data :activated])))
      (is (empty? @events*)))))

(deftest activation-request-with-category-activates-and-fires-event-test
  (let [events* (atom [])
        uuid "p1"]
    (seed-player! uuid
                  (assoc (adata/new-ability-data) :category-id :electromaster)
                  (rdata/new-resource-data))
    (with-redefs [uuid/player-uuid identity
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (activation-handler/handle-set-activated-request {:activated true} uuid)
      (is (true? (get-in (store/get-player-state test-player/test-session-id uuid) [:resource-data :activated])))
      (is (= [{:event/type evt/EVT-ABILITY-ACTIVATE
               :event/side :server
               :uuid uuid}]
             @events*)))))

(deftest deactivate-request-is-allowed-without-category-test
  (let [events* (atom [])
        uuid "p1"]
    (seed-player! uuid
                  (adata/new-ability-data)
                  (assoc (rdata/new-resource-data) :activated true))
    (with-redefs [uuid/player-uuid identity
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (activation-handler/handle-set-activated-request {:activated false} uuid)
      (is (false? (get-in (store/get-player-state test-player/test-session-id uuid) [:resource-data :activated])))
      (is (= [{:event/type evt/EVT-ABILITY-DEACTIVATE
               :event/side :server
               :uuid uuid}]
             @events*)))))

(deftest overload-or-interference-does-not-block-activation-test
  (let [uuid "p1"]
    (seed-player! uuid
                  (assoc (adata/new-ability-data) :category-id :electromaster)
                  (assoc (rdata/new-resource-data)
                         :overload-fine false
                         :interferences #{:jammed}))
    (with-redefs [uuid/player-uuid identity
                  evt/fire-ability-event! (fn [_] nil)]
      (activation-handler/handle-set-activated-request {:activated true} uuid)
      (is (true? (get-in (store/get-player-state test-player/test-session-id uuid) [:resource-data :activated]))))))


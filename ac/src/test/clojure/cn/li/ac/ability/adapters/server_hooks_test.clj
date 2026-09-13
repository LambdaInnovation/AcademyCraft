(ns cn.li.ac.ability.adapters.server-hooks-test
  (:require
            [cn.li.ac.ability.service.state-tick :as ps-tick]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.runtime-container :as runtime-container]
            [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.adapters.server-hooks :as server-hooks]
            [cn.li.ac.ability.item-actions :as item-actions]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.service.player-runtime-commands :as player-runtime-cmd]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.server.network :as network]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as md-damage]
            [cn.li.ac.ability.service.platform-hooks :as platform-hooks]            [cn.li.ac.block.developer.logic :as developer-logic]
            [cn.li.ac.wireless.data.world-registry :as world-registry]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-registries! [f]
  (let [item-actions-snapshot (item-actions/item-action-registries-snapshot)]
    (try
      (item-actions/reset-item-action-registries-for-test!)
      (f)
      (finally
        (item-actions/reset-item-action-registries-for-test! item-actions-snapshot)))))

(defn- reset-lifecycle-subs! [f]
  (evt/install-event-subscriber-runtime!
    (evt/create-event-subscriber-runtime))
  (evt/reset-ability-event-subscribers-for-test!)
  (server-hooks/reset-lifecycle-subscriptions-registered-for-test!)
  (try
    (f)
    (finally
      (evt/reset-ability-event-subscribers-for-test!)
      (evt/install-event-subscriber-runtime!
        (evt/create-event-subscriber-runtime))
      (server-hooks/reset-lifecycle-subscriptions-registered-for-test!))))

(use-fixtures :each reset-registries!)
(use-fixtures :each reset-lifecycle-subs!)
(use-fixtures :each
  (fn [f]
    (ps-fix/with-test-player-state-owner
      (fn []
        (store/reset-store!)
        (try
          (f)
          (finally
            (store/reset-store!)))))))
(use-fixtures :each
  (fn [f]
    (ctx/reset-contexts-for-test!)
    (try
      (f)
      (finally
        (ctx/reset-contexts-for-test!)))))
(use-fixtures :each
  (fn [f]
    (platform-hooks/reset-platform-fns!)
    (try
      (f)
      (finally
        (platform-hooks/reset-platform-fns!)))))
(use-fixtures :each
  (fn [f]
    (runtime-container/install-ability-runtime-container!
      (runtime-container/create-ability-runtime-container))
    (f)))
(use-fixtures :each
  (fn [f]
    ;; Must be a complete owner — :player-uuid is required by the :owner contract
    ;; that context routing validates.
    (runtime-hooks/with-client-ctx-fn {:player-owner ps-fix/test-player-state-owner} (fn [] (f)))))

(deftest build-item-use-plan-order-test
  (testing "coin use plans consume, dispatch domain action, then spawn scripted effect"
    (item-actions/register-item-action! "ac:coin" :railgun-coin-throw)
    (item-actions/register-item-entity-spawn! "ac:coin" {:entity-id "entity_coin_throwing" :speed 0.0})
    (let [build-plan (:build-item-use-plan (server-hooks/runtime-server-hooks))
          plan (build-plan "p1" "ac:coin" true :server)]
          (is (= {:kind :consume-item :count 1 :unless-instabuild? true}
            (first (:server-actions plan))))
          (is (= :domain-action (:kind (second (:server-actions plan)))))
          (is (= :railgun-coin-throw (:action (second (:server-actions plan)))))
          (is (number? (get-in plan [:server-actions 1 :payload :timestamp-ms])))
          (is (= {:kind :spawn-scripted-effect :entity-id "entity_coin_throwing"
                  :unique-per-owner? false :speed 0.0}
            (nth (:server-actions plan) 2)))
      (is (= [{:kind :notify-local-effect
               :event-key :ac/charge-coin-throw}]
             (:client-actions plan)))
      (is (true? (:consume? plan))))))

(deftest build-item-use-plan-coin-throw-unconditional-test
  (testing "coin use plans dispatch consume + domain + spawn + QTE notify regardless of activation (upstream ItemCoin)"
    (item-actions/register-item-action! "ac:coin" :railgun-coin-throw)
    (item-actions/register-item-entity-spawn! "ac:coin" {:entity-id "entity_coin_throwing" :speed 0.0})
    (let [build-plan (:build-item-use-plan (server-hooks/runtime-server-hooks))
          plan (build-plan "p1" "ac:coin" false :server)]
      (is (= [:consume-item :domain-action :spawn-scripted-effect]
             (mapv :kind (:server-actions plan))))
      (is (= [{:kind :notify-local-effect
               :event-key :ac/charge-coin-throw}]
             (:client-actions plan)))
      (is (true? (:consume? plan))))))

(deftest level-change-event-uses-service-recalc-path-test
  (let [commands* (atom [])
        calc-calls (atom [])]
    (with-redefs [evt/fire-calc-event! (fn [event-key value extra]
                                         (swap! calc-calls conj [event-key value extra])
                                         value)
        store/get-player-state (fn [_session-id _uuid]
              {:resource-data {:cur-cp 100.0
                     :cur-overload 10.0
                     :max-cp 200.0
                     :max-overload 150.0
                     :add-max-cp 5.0
                     :add-max-overload 7.0}})
        command-rt/run-command-in-session! (fn [session-id uuid command]
                    (swap! commands* conj [session-id uuid command])
                    nil)]
      (evt/install-event-subscriber-runtime!
        (evt/create-event-subscriber-runtime))
              (server-hooks/reset-lifecycle-subscriptions-registered-for-test!)
      (server-hooks/register-lifecycle-subscriptions!)
      (evt/fire-ability-event! (evt/make-level-change-event "u-level" 2 3))
                                        (is (= [evt/CALC-MAX-CP evt/CALC-MAX-OVERLOAD]
                                         (mapv first @calc-calls)))
                                        (is (every? #(= {:uuid "u-level"} (nth % 2)) @calc-calls))
                (is (= [[:test-session "u-level"]]
                  (mapv (fn [[sid uuid _]] [sid uuid]) @commands*)))
                (is (= :hydrate-player-state
                  (get-in (first @commands*) [2 :command])))
                (is (= {:add-max-cp 0.0
                   :add-max-overload 0.0}
                  (select-keys (get-in (first @commands*) [2 :resource-data])
                     [:add-max-cp :add-max-overload]))))))

(deftest overload-event-aborts-player-contexts-test
  (let [aborted (atom [])]
    (with-redefs [ctx-mgr/abort-player-contexts!
                  (fn [uuid]
                    (swap! aborted conj uuid)
                    nil)]
      (evt/install-event-subscriber-runtime!
        (evt/create-event-subscriber-runtime))
      (server-hooks/reset-lifecycle-subscriptions-registered-for-test!)
      (server-hooks/register-lifecycle-subscriptions!)
      (evt/fire-ability-event! (evt/make-overload-event "u-overload"))
      (is (some #{"u-overload"} @aborted)))))

(deftest player-logout-clears-delayed-projectiles-test
  (let [called (atom [])
        logout! (:on-player-logout! (server-hooks/runtime-server-hooks))]
    (with-redefs [ctx-mgr/abort-player-contexts! (fn [uuid]
                                                   (swap! called conj [:abort uuid])
                                                   nil)
                  delayed-projectiles/clear-player-tasks! (fn [uuid]
                                                            (swap! called conj [:projectiles uuid])
                                                            nil)
                  md-damage/clear-target-mark! (fn [_uuid] nil)
                  md-damage/clear-source-marks! (fn [_uuid] nil)
          store/remove-player-state! (fn [session-id uuid]
                     (swap! called conj [:remove-state session-id uuid])
                     nil)]
      (logout! "player-1"))
        (is (= [[:abort "player-1"]
            [:projectiles "player-1"]
          [:remove-state :test-session "player-1"]]
           @called))))

;; The manager sweep and mark tick are global work: they run once in
;; :on-server-tick-start!, ahead of the per-player phase.
(deftest server-tick-start-sweeps-before-player-tick-test
  (let [calls (atom [])
        hooks (server-hooks/runtime-server-hooks)
        start! (:on-server-tick-start! hooks)
        tick! (:on-player-tick! hooks)]
    (with-redefs [store/get-or-create-player-state! (fn [session-id uuid]
                                                      (swap! calls conj [:ensure-state session-id uuid])
                                                      nil)
                  ps-tick/server-tick-player-in-session! (fn [session-id uuid payload]
                                                           (swap! calls conj [:player-state-tick session-id uuid payload])
                                                           nil)
                  ctx-mgr/tick-player-contexts! (fn [uuid _player]
                                                 (swap! calls conj [:context-tick uuid])
                                                 nil)
                  md-damage/tick-marks! (fn [] (swap! calls conj [:marks]) nil)
                  delayed-projectiles/tick-player! (fn [uuid]
                                                     (swap! calls conj [:projectiles uuid])
                                                     nil)
                  ctx-mgr/tick-context-manager! (fn []
                                                 (swap! calls conj [:context-manager])
                                                 nil)]
      (start! 1)
      (tick! "p1" (Object.))
      (is (= [[:marks]
              [:context-manager]
              [:ensure-state :test-session "p1"]
              [:player-state-tick :test-session "p1" nil]
              [:context-tick "p1"]
              [:projectiles "p1"]]
             @calls)))))

(deftest get-context-player-uuid-requires-owner-or-unique-match-test
  (let [get-player-uuid (:get-context-player-uuid (server-hooks/runtime-server-hooks))
        owner-a {:logical-side :server :server-session-id :server-a :player-uuid "player-a"}
        owner-b {:logical-side :server :server-session-id :server-b :player-uuid "player-b"}]
    (ctx/register-context! (ctx/new-server-context "player-a" :sk "dup-ctx" owner-a))
    (ctx/register-context! (ctx/new-server-context "player-a" :sk "unique-ctx" owner-a))
    (ctx/register-context! (ctx/new-server-context "player-b" :sk "dup-ctx" owner-b))
    (is (= "player-a"
           (ctx/with-context-owner owner-a
             (get-player-uuid "dup-ctx"))))
    (is (= "player-a" (get-player-uuid "unique-ctx")))
    (is (nil? (get-player-uuid "dup-ctx")))))

(deftest server-stop-clears-session-state-test
  (let [called (atom [])
        stop! (:on-server-stop! (server-hooks/runtime-server-hooks))]
    (platform-hooks/register-platform-fn! :ability/reset-server-runtimes!
                                          (fn []
                                            (swap! called conj [:reset-runtimes])
                                            nil))
    (with-redefs [ctx/clear-store-session-contexts! (fn [session-id]
                                                      (swap! called conj [:contexts session-id])
                                                      nil)
                  store/remove-session! (fn [session-id]
                                          (swap! called conj [:player-states session-id])
                                          nil)
                  world-registry/clear-session-world-data! (fn [session-id]
                                                             (swap! called conj [:wireless session-id])
                                                             nil)
                  md-damage/on-server-stop! (fn [session-id]
                                              (swap! called conj [:marks session-id])
                                              nil)
                  delayed-projectiles/clear-all-tasks! (fn []
                                                         (swap! called conj [:projectiles])
                                                         nil)]
      (stop! :server-session))
    (is (= [[:contexts :server-session]
            [:player-states :server-session]
            [:wireless :server-session]
            [:reset-runtimes]
            [:projectiles]
            [:marks :server-session]]
           @called))))

(deftest register-platform-functions-registers-network-reset-and-energy-pull-test
  (let [energy-calls (atom [])]
    (with-redefs [player-runtime-cmd/reset-all-content-runtimes! (fn [] :reset-ok)
                  network/register-handlers! (fn [] :network-ok)
                  developer-logic/try-pull-energy! (fn [tile amount]
                                                     (swap! energy-calls conj [tile amount])
                                                     true)]
      (server-hooks/register-platform-functions!)
      (is (= :reset-ok ((platform-hooks/get-platform-fn :ability/reset-server-runtimes!))))
      (is (= :network-ok ((platform-hooks/get-platform-fn :ability/register-network-handlers!))))
      (is (true? ((platform-hooks/get-platform-fn :ability/try-pull-developer-energy!) :tile 12.5)))
      (is (= [[:tile 12.5]] @energy-calls)))))



      (deftest category-change-event-aborts-deactivates-and-recalculates-test
        (let [aborted (atom [])
          commands* (atom [])
         calc-calls (atom [])]
          (with-redefs [ctx-mgr/abort-player-contexts!
              (fn [uuid]
                (swap! aborted conj uuid)
                nil)
              store/get-player-state (fn [_session-id _]
                {:ability-data {:level 4}
                 :resource-data {:activated true
                  :cur-cp 10.0
                  :cur-overload 3.0
                  :max-cp 20.0
                  :max-overload 40.0}})
          command-rt/run-command-in-session! (fn [session-id uuid command]
                      (swap! commands* conj [session-id uuid command])
                      nil)
              evt/fire-calc-event! (fn [event-key value extra]
                      (swap! calc-calls conj [event-key value extra])
                      value)]
            (evt/install-event-subscriber-runtime!
              (evt/create-event-subscriber-runtime))
            (server-hooks/reset-lifecycle-subscriptions-registered-for-test!)
            (server-hooks/register-lifecycle-subscriptions!)
            (evt/fire-ability-event! (evt/make-category-change-event "u-category" :old :new))
            (is (some #{"u-category"} @aborted))
             (is (some (fn [[sid uuid cmd]]
               (and (= sid :test-session)
               (= uuid "u-category")
               (= :clear-all-cooldowns (:command cmd))))
             @commands*))
             (is (some (fn [[sid uuid cmd]]
               (and (= sid :test-session)
               (= uuid "u-category")
               (= :set-activated (:command cmd))
               (false? (:activated cmd))))
             @commands*))
            (is (every? (set (mapv first @calc-calls))
                        [evt/CALC-MAX-CP evt/CALC-MAX-OVERLOAD]))
            (is (every? #(= {:uuid "u-category"} (nth % 2)) @calc-calls))
              (is (some (fn [[sid uuid cmd]]
                    (and (= sid :test-session)
                      (= uuid "u-category")
                      (= :hydrate-player-state (:command cmd))
                      (map? (:resource-data cmd))))
                  @commands*)))))

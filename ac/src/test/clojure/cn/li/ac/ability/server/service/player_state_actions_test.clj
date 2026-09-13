(ns cn.li.ac.ability.server.service.player-state-actions-test
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.adapters.server-hooks :as server-hooks]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.cooldown :as cdata]
            [cn.li.ac.ability.model.develop :as ddata]
            [cn.li.ac.ability.model.preset :as pdata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-lifecycle-subs! [f]
  (ps-fix/with-test-player-state-owner
   (fn []
     (evt/install-event-subscriber-runtime!
      (evt/create-event-subscriber-runtime))
     (evt/reset-ability-event-subscribers-for-test!)
     (server-hooks/reset-lifecycle-subscriptions-registered-for-test!)
     (try
       (f)
       (finally
         (evt/install-event-subscriber-runtime!
          (evt/create-event-subscriber-runtime))
         (evt/reset-ability-event-subscribers-for-test!)
         (server-hooks/reset-lifecycle-subscriptions-registered-for-test!))))))

(use-fixtures :each ps-fix/clean-player-states-fixture)
(use-fixtures :each reset-lifecycle-subs!)

(deftest set-level-fires-recalc-with-reset-add-max-test
  (let [recalc-calls (atom [])]
    (store/set-player-state! ps-fix/test-session-id "p1"
                          (-> (store/fresh-player-state)
                              (assoc-in [:ability-data :level] 2)
                              (assoc-in [:resource-data :add-max-cp] 5.0)
                              (assoc-in [:resource-data :add-max-overload] 7.0)
                              (assoc-in [:resource-data :cur-cp] 80.0)
                              (assoc-in [:resource-data :max-cp] 120.0)
                              (assoc-in [:resource-data :max-overload] 40.0)))
    (with-redefs [rdata/recalc-max-values (fn [rd level]
                                            (swap! recalc-calls conj [:base level rd])
                                            (assoc rd :max-cp 140.0 :max-overload 55.0))
                  evt/fire-calc-event! (fn [event-type base-value extra]
                                         (swap! recalc-calls conj [:calc event-type base-value extra])
                                         base-value)]
      (server-hooks/register-lifecycle-subscriptions!)
      (command-rt/run-command-in-session! ps-fix/test-session-id "p1"
                  {:command :set-level :level 3})
      (is (= 3 (get-in (store/get-player-state ps-fix/test-session-id "p1") [:ability-data :level])))
      (is (= [evt/CALC-MAX-CP evt/CALC-MAX-OVERLOAD]
             (->> @recalc-calls
                  (filter #(= :calc (first %)))
                  (mapv second))))
      ;; recalc-max-values is stubbed out above, so the refill asserted here is
      ;; the wrapper's own: upstream recalcMaxValue ends with
      ;; curCP = getMaxCP() / curOverload = 0, and the wrapper has to redo it
      ;; against the post-CALC maximum, not the pre-CALC one.
      (is (= {:cur-cp 140.0
              :cur-overload 0.0
              :max-cp 140.0
              :max-overload 55.0
              :add-max-cp 0.0
              :add-max-overload 0.0}
             (select-keys (get-in (store/get-player-state ps-fix/test-session-id "p1") [:resource-data])
                          [:cur-cp :cur-overload :max-cp :max-overload
                           :add-max-cp :add-max-overload]))))))

(deftest recover-all-restores-cp-overload-and-recovery-timers-test
  (store/set-player-state! ps-fix/test-session-id "p1"
                        (-> (store/fresh-player-state)
                            (assoc :resource-data {:cur-cp 5.0
                                                   :max-cp 42.0
                                                   :cur-overload 17.0
                                                   :max-overload 80.0
                                                   :add-max-cp 1.0
                                                   :add-max-overload 2.0
                                                   :activated true
                                                   :overload-fine false
                                                   :until-recover 9
                                                   :until-overload-recover 11
                                                   :interferences #{:jam}})))
  (command-rt/run-command-in-session! ps-fix/test-session-id "p1"
                                      {:command :recover-all})
  (is (= {:cur-cp 42.0
          :max-cp 42.0
          :cur-overload 0.0
          :max-overload 80.0
          :add-max-cp 1.0
          :add-max-overload 2.0
          :activated true
          :overload-fine true
          :until-recover 0
          :until-overload-recover 0
          :interferences #{:jam}}
         (:resource-data (store/get-player-state ps-fix/test-session-id "p1")))))

(deftest unlearn-skill-clears-skill-exp-and-preset-slot-test
  (store/set-player-state! ps-fix/test-session-id "p1"
                        (-> (store/fresh-player-state)
                            (assoc-in [:ability-data :learned-skills] #{:railgun :arc-gen})
                            (assoc-in [:ability-data :skill-exps] {:railgun 0.8 :arc-gen 0.2})
                            (assoc-in [:preset-data :slots] {[0 0] [:electromaster :railgun]
                                                             [0 1] [:electromaster :arc-gen]})))
  (with-redefs [skill-query/controllable-key (fn [skill-id]
                                               (case skill-id
                                                 :railgun [:electromaster :railgun]
                                                 :arc-gen [:electromaster :arc-gen]
                                                 nil))]
    (command-rt/run-command-in-session! ps-fix/test-session-id "p1"
                                        {:command :unlearn-skill :skill-id :railgun}))
  (let [state (store/get-player-state ps-fix/test-session-id "p1")]
    (is (= #{:arc-gen} (get-in state [:ability-data :learned-skills])))
    (is (= {:arc-gen 0.2} (get-in state [:ability-data :skill-exps])))
    (is (nil? (get-in state [:preset-data :slots [0 0]])))
    (is (= [:electromaster :arc-gen] (get-in state [:preset-data :slots [0 1]])))))

(deftest reset-abilities-resets-runtime-slices-and-fires-category-side-effects-test
  (let [aborted (atom [])]
    (store/set-player-state! ps-fix/test-session-id "p1"
                          (-> (store/fresh-player-state)
                              (assoc :cheats-enabled? true)
                              (assoc-in [:ability-data :category-id] :electromaster)
                              (assoc-in [:ability-data :level] 4)
                              (assoc :cooldown-data {[:railgun :main] {:ticks 20 :max 20}})
                              (assoc-in [:preset-data :slots] {[0 0] [:electromaster :railgun]})
                              (assoc-in [:resource-data :activated] true)
                              (assoc-in [:develop-data :state] :developing)))
    (with-redefs [ctx-mgr/abort-player-contexts! (fn [uuid]
                                                   (swap! aborted conj uuid)
                                                   nil)]
      (server-hooks/register-lifecycle-subscriptions!)
      (command-rt/run-command-in-session! ps-fix/test-session-id "p1"
                  {:command :reset-abilities})
      (let [state (store/get-player-state ps-fix/test-session-id "p1")]
        (is (= ["p1"] @aborted))
        (is (= (adata/new-ability-data) (:ability-data state)))
        (is (= (cdata/new-cooldown-data) (:cooldown-data state)))
        (is (= (pdata/new-preset-data) (:preset-data state)))
        (is (= (ddata/new-develop-data) (:develop-data state)))
        (is (= true (:cheats-enabled? state)))
        (is (= (rdata/new-resource-data) (:resource-data state)))))))

(deftest state-actions-uses-bound-owner-session-test
  (store/set-player-state! :actions-session "p2"
                        (-> (store/fresh-player-state)
                            (assoc-in [:ability-data :level] 1)))
  (runtime-hooks/with-client-ctx-fn {:player-owner {:server-session-id :actions-session}} (fn [] (command-rt/run-command-in-session! nil "p2"
                                        {:command :set-level :level 4})
    (is (= 4 (get-in (store/get-player-state :actions-session "p2") [:ability-data :level]))))))

(deftest state-actions-session-resolution-still-fail-fast-test
  (runtime-hooks/with-client-ctx-fn {:player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"session-id"
                          (command-rt/run-command-in-session! nil "p3"
                                                              {:command :recover-all}))))))

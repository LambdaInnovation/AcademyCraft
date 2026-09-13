(ns cn.li.ac.ability.service.reducer-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.model.develop :as ddata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.service.reducer :as reducer]
            [cn.li.ac.ability.service.runtime-store :as runtime-store]))

(defn- base-state []
  (runtime-store/fresh-player-state))

(deftest set-activated-command-emits-event-test
  (let [player-state (base-state)
        result (reducer/apply-command player-state
                                      {:command :set-activated
                                       :player-uuid "p-1"
                                       :activated true})]
    (is (true? (get-in result [:state :resource-data :activated])))
    (is (= [evt/EVT-ABILITY-ACTIVATE]
           (mapv :event/type (:events result))))
    (is (empty? (:effects result)))))

(deftest consume-resource-command-updates-cp-when-activated-test
  (let [player-state (-> (base-state)
                         (assoc :resource-data (rdata/set-activated (:resource-data (base-state)) true)))
        result (reducer/apply-command player-state
                                      {:command :consume-resource
                                       :player-uuid "p-2"
                                       :cp 10.0
                                       :overload 3.0
                                       :creative? false})]
    (is (true? (:success? result)))
    (is (< (get-in result [:state :resource-data :cur-cp])
           (get-in player-state [:resource-data :cur-cp])))))

(deftest overload-cap-emits-an-event-the-abort-subscriber-can-read-test
  ;; server-hooks answers EVT-OVERLOAD with (abort-player-contexts! uuid),
  ;; mirroring upstream ContextManager.__onOverload -> disposePlayer. It reads
  ;; :uuid, the key every constructor in registry.event produces. An overload
  ;; event carrying the player under any other key is silently a no-op: the
  ;; player's contexts survive an overload they should have been killed by.
  (let [player-state (-> (base-state)
                         (update :resource-data rdata/set-activated true)
                         (assoc-in [:resource-data :max-overload] 100.0)
                         (assoc-in [:resource-data :cur-overload] 0.0))
        result (reducer/apply-command player-state
                                      {:command :consume-resource
                                       :player-uuid "p-overload"
                                       :cp 0.0
                                       :overload 500.0
                                       :creative? false})
        overload-event (first (filter #(= evt/EVT-OVERLOAD (:event/type %))
                                      (:events result)))]
    (is (some? overload-event) "hitting the cap must emit EVT-OVERLOAD")
    (is (= (evt/make-overload-event "p-overload") overload-event)
        "and must match the constructor the subscriber destructures")
    (is (= "p-overload" (:uuid overload-event)))))

(deftest set-skill-exp-is-refused-for-an-unlearned-skill-test
  ;; Upstream AbilityData.setSkillExp is guarded by isSkillLearned; exp on an
  ;; unlearned skill is meaningless and would surface the moment it is
  ;; learned.
  (let [unlearned (reducer/apply-command (base-state)
                                         {:command :set-skill-exp
                                          :player-uuid "p-exp"
                                          :skill-id :railgun
                                          :amount 1.0})]
    (is (= :skill-not-learned (:rejected-reason unlearned)))
    (is (nil? (get-in unlearned [:state :ability-data :skill-exps :railgun]))))
  (let [learned (-> (base-state)
                    (update-in [:ability-data :learned-skills] conj :railgun))
        result (reducer/apply-command learned
                                      {:command :set-skill-exp
                                       :player-uuid "p-exp"
                                       :skill-id :railgun
                                       :amount 1.0})]
    (is (= 1.0 (get-in result [:state :ability-data :skill-exps :railgun])))))

(deftest maxout-only-fills-the-current-level-progress-test
  ;; Upstream maxOutLevelProgress is `expAddedThisLevel = 100` — it grants no
  ;; level, no skills and no exp.
  (let [before (-> (base-state)
                   (assoc-in [:ability-data :category-id] :electromaster)
                   (assoc-in [:ability-data :level] 2))
        result (reducer/apply-command before {:command :maxout-level-progress
                                              :player-uuid "p-maxout"})
        after (:state result)]
    (is (nil? (:rejected-reason result)))
    (is (= 2 (get-in after [:ability-data :level])) "no level is granted")
    (is (empty? (get-in after [:ability-data :learned-skills])) "no skill is learned")
    (is (empty? (get-in after [:ability-data :skill-exps])) "no exp is granted")
    (is (>= (get-in after [:ability-data :level-progress]) 0.0))))

(deftest apply-commands-accumulates-events-and-effects-test
  (let [player-state (base-state)
        result (reducer/apply-commands player-state
                                       [{:command :set-activated
                                         :player-uuid "p-3"
                                         :activated true}
                                        {:command :switch-preset
                                         :player-uuid "p-3"
                                         :preset-idx 2}])]
    (is (= 2 (count (:events result))))
    (is (= 1 (count (:effects result))))
    (is (= :network-send (-> result :effects first :effect/type)))
    (is (= 2 (get-in result [:state :preset-data :active-preset])))))

;; ============================================================================
;; server-tick-noop? equivalence — must mirror cmd-server-tick branch-for-branch.
;; Every "true" case below is asserted against the real :server-tick command
;; to guard against drift if the recovery/cooldown/develop formulas change.
;; ============================================================================

(defn- tick-command
  [player-uuid]
  {:command :server-tick :player-uuid player-uuid :cp-speed 5.0 :ol-speed 5.0})

(deftest server-tick-noop-true-on-fully-idle-state-is-a-real-noop-test
  (let [player-state (base-state)]
    (is (true? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-idle"))]
      (is (identical? player-state (:state result)))
      (is (empty? (:events result))))))

(deftest server-tick-noop-false-when-cp-not-full-test
  (let [player-state (update-in (base-state) [:resource-data]
                                #(assoc % :cur-cp (- (:max-cp %) 10.0)))]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-cp"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-until-recover-pending-test
  (let [player-state (assoc-in (base-state) [:resource-data :until-recover] 5)]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-until-recover"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-until-overload-recover-pending-test
  (let [player-state (assoc-in (base-state) [:resource-data :until-overload-recover] 5)]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-until-overload"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-overloaded-test
  (let [player-state (update-in (base-state) [:resource-data]
                                #(assoc % :overload-fine false
                                          :cur-overload (:max-overload %)))]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-overloaded"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-residual-overload-test
  (let [player-state (update-in (base-state) [:resource-data]
                                #(assoc % :cur-overload (* 0.5 (:max-overload %))))]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-residual-overload"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-cooldown-present-test
  (let [player-state (assoc (base-state) :cooldown-data {[:railgun :main] {:ticks 5 :max 5}})]
    (is (false? (reducer/server-tick-noop? player-state)))
    (let [result (reducer/apply-command player-state (tick-command "p-cooldown"))]
      (is (not= player-state (:state result))))))

(deftest server-tick-noop-false-when-developing-test
  (let [player-state (assoc (base-state) :develop-data
                            (assoc (ddata/new-develop-data) :state :developing))]
    (is (false? (reducer/server-tick-noop? player-state)))))

;; ============================================================================
;; Radiation-mark commands emit :radiation-index-sync effects — the derived
;; index (service/radiation-mark-index.clj) is kept in sync purely through
;; these effects, so every command that changes radiation-marks must emit one.
;; ============================================================================

(deftest mark-radiation-target-emits-index-sync-effect-test
  (let [player-state (base-state)
        mark {:source-player-id "atk-1" :target-id "victim-1" :ticks-left 100 :rate 1.5}
        result (reducer/apply-command player-state
                                      {:command :mark-radiation-target
                                       :player-uuid "atk-1"
                                       :target-id "victim-1"
                                       :mark mark})]
    (is (= mark (get-in result [:state :runtime :meltdowner :radiation-marks "victim-1"])))
    (is (= [{:effect/type :radiation-index-sync
             :player-uuid "atk-1"
             :marks {"victim-1" mark}}]
           (:effects result)))))

(deftest mark-radiation-target-rejects-missing-fields-test
  (let [player-state (base-state)
        result (reducer/apply-command player-state
                                      {:command :mark-radiation-target
                                       :player-uuid "atk-1"})]
    (is (= :invalid-radiation-mark (:rejected-reason result)))
    (is (empty? (:effects result)))))

(deftest clear-radiation-marks-noop-emits-no-effect-test
  (let [player-state (base-state)
        result (reducer/apply-command player-state
                                      {:command :clear-radiation-marks
                                       :player-uuid "atk-1"
                                       :target-id "nobody-marked"})]
    (is (identical? player-state (:state result)))
    (is (empty? (:effects result)))))

(deftest clear-radiation-marks-emits-index-sync-effect-when-changed-test
  (let [mark {:source-player-id "atk-1" :target-id "victim-1" :ticks-left 100 :rate 1.5}
        player-state (assoc-in (base-state)
                               [:runtime :meltdowner :radiation-marks "victim-1"]
                               mark)
        result (reducer/apply-command player-state
                                      {:command :clear-radiation-marks
                                       :player-uuid "atk-1"
                                       :target-id "victim-1"})]
    (is (= {} (get-in result [:state :runtime :meltdowner :radiation-marks])))
    (is (= [{:effect/type :radiation-index-sync :player-uuid "atk-1" :marks {}}]
           (:effects result)))))

(deftest tick-radiation-marks-unconditionally-emits-index-sync-effect-test
  (testing "even with zero marks — this is the index's self-healing channel"
    (let [player-state (base-state)
          result (reducer/apply-command player-state
                                        {:command :tick-radiation-marks
                                         :player-uuid "atk-1"})]
      (is (= [{:effect/type :radiation-index-sync :player-uuid "atk-1" :marks {}}]
             (:effects result))))))

(deftest tick-radiation-marks-decrements-and-expires-test
  (let [expiring {:source-player-id "atk-1" :target-id "v1" :ticks-left 1 :rate 1.5}
        surviving {:source-player-id "atk-1" :target-id "v2" :ticks-left 5 :rate 1.5}
        player-state (assoc-in (base-state)
                               [:runtime :meltdowner :radiation-marks]
                               {"v1" expiring "v2" surviving})
        result (reducer/apply-command player-state
                                      {:command :tick-radiation-marks
                                       :player-uuid "atk-1"})
        next-marks (get-in result [:state :runtime :meltdowner :radiation-marks])]
    (is (nil? (get next-marks "v1")))
    (is (= 4 (get-in next-marks ["v2" :ticks-left])))
    (is (= [{:effect/type :radiation-index-sync :player-uuid "atk-1" :marks next-marks}]
           (:effects result)))))

(deftest hydrate-player-state-with-runtime-data-emits-index-sync-effect-test
  (let [player-state (base-state)
        runtime-data {:meltdowner {:radiation-marks
                                    {"victim-1" {:source-player-id "atk-1"
                                                :target-id "victim-1"
                                                :ticks-left 50
                                                :rate 2.0}}}}
        result (reducer/apply-command player-state
                                      {:command :hydrate-player-state
                                       :player-uuid "atk-1"
                                       :runtime-data runtime-data})]
    (is (= runtime-data (get-in result [:state :runtime])))
    (is (= [{:effect/type :radiation-index-sync
             :player-uuid "atk-1"
             :marks (get-in runtime-data [:meltdowner :radiation-marks])}]
           (:effects result)))))

(deftest hydrate-player-state-without-runtime-data-emits-no-radiation-effect-test
  (let [player-state (base-state)
        result (reducer/apply-command player-state
                                      {:command :hydrate-player-state
                                       :player-uuid "atk-1"
                                       :ability-data (:ability-data player-state)})]
    (is (empty? (:effects result)))))

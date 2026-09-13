(ns cn.li.ac.ability.registry.skill-schema
  "Skill spec validation extracted from skill.clj.

  All functions here are pure (no atoms, no side-effects).  They throw
  clojure.lang.ExceptionInfo on invalid input so callers can either let
  errors propagate or catch and report them independently.")

;; ============================================================================
;; Constants
;; ============================================================================

(def supported-patterns
  #{:instant :hold-charge-release :hold-channel
    :toggle :release-cast :charge-window :passive})

(def allowed-action-keys
  #{:perform! :down! :tick! :up! :abort! :cost-fail! :activate! :deactivate!})

(def allowed-fx-keys
  #{:start :update :perform :end})

(def allowed-cost-stages
  #{:down :tick :up})

(def ^:private required-action-keys
  {:instant             #{:perform!}
   :hold-charge-release #{:perform!}
   :hold-channel        #{}
   :toggle              #{:activate! :deactivate!}
   :release-cast        #{}
   :charge-window       #{}
   :passive             #{}})

;; ============================================================================
;; Individual validators (each throws ExceptionInfo on failure)
;; ============================================================================

(defn- assert! [condition data]
  (when-not condition
    (throw (ex-info (:message data) (dissoc data :message)))))

(defn validate-no-legacy-callbacks!
  [{:keys [id] :as spec}]
  (doseq [k [:on-key-down :on-key-tick :on-key-up :on-key-abort]]
    (assert! (not (contains? spec k))
             {:message "Legacy callback keys are no longer supported for skills"
              :skill-id id :legacy-key k})))

(defn validate-no-dead-op-vectors!
  [{:keys [id] :as spec}]
  (doseq [k [:on-down :on-tick :on-up :ops]]
    (assert! (not (contains? spec k))
             {:message "Declarative :ops / :on-* vectors are not executed; use :actions"
              :skill-id id :dead-key k})))

(defn validate-pattern!
  [{:keys [id pattern] :as _spec}]
  (assert! (keyword? pattern)
           {:message "Skill :pattern is required and must be a keyword" :skill-id id})
  (assert! (contains? supported-patterns pattern)
           {:message "Unknown skill :pattern"
            :skill-id id :pattern pattern :valid-patterns supported-patterns}))

(defn validate-actions!
  [{:keys [id pattern actions perform] :as _spec}]
  (when (some? actions)
    (assert! (map? actions)
             {:message "Skill :actions must be a map" :skill-id id}))
  (doseq [[k _] actions]
    (assert! (contains? allowed-action-keys k)
             {:message "Unsupported action key in :actions"
              :skill-id id :action-key k}))
  ;; Check required action keys for the given pattern
  (let [required     (get required-action-keys pattern #{})
        has-perform? (fn [action-key]
                       (and (= action-key :perform!) (seq perform)))
        missing      (seq (remove #(or (contains? (or actions {}) %)
                                       (has-perform? %))
                                  required))]
    (when missing
      (throw (ex-info "Missing required actions for pattern"
                      {:skill-id id :pattern pattern :missing missing})))))

(defn validate-cooldown!
  [{:keys [id cooldown] :as _spec}]
  (when (some? cooldown)
    (assert! (map? cooldown)
             {:message "Skill :cooldown must be a map" :skill-id id :value cooldown})
    (assert! (contains? #{:default :manual} (get cooldown :mode :default))
             {:message "Skill :cooldown :mode must be :default or :manual"
              :skill-id id :cooldown cooldown})))

(defn validate-cost!
  [{:keys [id cost] :as _spec}]
  (doseq [[stage stage-spec] cost]
    (assert! (contains? allowed-cost-stages stage)
             {:message "Unsupported cost stage" :skill-id id :stage stage})
    (assert! (map? stage-spec)
             {:message "Cost stage spec must be a map"
              :skill-id id :stage stage :value stage-spec})
    (assert! (contains? #{nil :runtime-speed} (:mode stage-spec))
             {:message "Cost stage :mode must be :runtime-speed or omitted"
              :skill-id id :stage stage :value stage-spec})))

(defn validate-fx!
  [{:keys [id fx] :as _spec}]
  (doseq [[fx-k fx-v] fx]
    (assert! (contains? allowed-fx-keys fx-k)
             {:message "Unsupported fx key" :skill-id id :fx-key fx-k})
    (assert! (map? fx-v)
             {:message "FX entry must be a map" :skill-id id :fx-key fx-k :value fx-v})
    (assert! (keyword? (:topic fx-v))
             {:message "FX entry requires keyword :topic" :skill-id id :fx-key fx-k :value fx-v})
    (when (contains? fx-v :mode)
      (assert! (keyword? (:mode fx-v))
               {:message "FX entry :mode must be a keyword" :skill-id id :fx-key fx-k}))
    (when (contains? fx-v :to)
      (assert! (contains? #{:client :self :except-local} (:to fx-v))
               {:message "FX entry :to must be :client, :self, or :except-local"
                :skill-id id :fx-key fx-k :value (:to fx-v)}))
    (when (contains? fx-v :payload)
      (assert! (fn? (:payload fx-v))
               {:message "FX entry :payload must be a function" :skill-id id :fx-key fx-k}))))

(defn validate-map-fields!
  [{:keys [id perform ops aim exp] :as spec}]
  (assert! (or (nil? perform) (vector? perform))
           {:message "Skill :perform must be a vector or nil" :skill-id id :value perform})
  (assert! (or (nil? ops) (map? ops))
           {:message "Skill :ops must be a map or nil" :skill-id id :value ops})
  (assert! (or (nil? aim) (map? aim))
           {:message "Skill :aim must be a map or nil" :skill-id id :value aim})
  (assert! (or (nil? exp) (map? exp))
           {:message "Skill :exp must be a map or nil" :skill-id id :value exp})
  (doseq [k [:targeting :transitions :exp-policy :cooldown-policy :input-policy :state]]
    (when (contains? spec k)
      (assert! (map? (get spec k))
               {:message "Skill policy fields must be maps"
                :skill-id id :field k :value (get spec k)}))))

;; ============================================================================
;; Composite validator
;; ============================================================================

(defn validate!
  "Run all validators against `spec`.  Returns spec unchanged on success.
  Throws ExceptionInfo on the first failure."
  [spec]
  (validate-no-legacy-callbacks! spec)
  (validate-no-dead-op-vectors! spec)
  (validate-pattern! spec)
  (validate-actions! spec)
  (validate-cooldown! spec)
  (validate-cost! spec)
  (validate-fx! spec)
  (validate-map-fields! spec)
  spec)

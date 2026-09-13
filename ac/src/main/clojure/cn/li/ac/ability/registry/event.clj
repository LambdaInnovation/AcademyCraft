(ns cn.li.ac.ability.registry.event
  "Ability domain events.

  Events are plain maps dispatched through the platform event bus.
  Listeners subscribe in game content namespaces (no Minecraft imports here).

  Subscriber registry stored in Framework [:service :ability-events]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Event Subscriber Registry — Framework [:service :ability-events]

(def ^:private event-path [:service :ability-events])

(defn- event-subscriber-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom event-path {:subscribers {} :frozen? false})
    {:subscribers {} :frozen? false}))

(defn- update-event-subscriber-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in event-path
           (fn [current] (apply f (or current {:subscribers {} :frozen? false}) args))))
  nil)

(defn- assert-subscribers-open! []
  (when (:frozen? (event-subscriber-state-snapshot))
    (throw (ex-info "Ability event subscriber registry is frozen" {}))))

;; Runtime container API.
(defn create-event-subscriber-runtime
  ([]
   {::event-subscriber-runtime true
    :state* (atom {:subscribers {} :frozen? false})})
  ([{:keys [state*] :or {state* (atom {:subscribers {} :frozen? false})}}]
   {::event-subscriber-runtime true :state* state*}))

(defn install-event-subscriber-runtime! [runtime]
  (when-let [fw-atom (fw/fw-atom)]
    (when-let [state* (:state* runtime)]
      (swap! fw-atom assoc-in event-path @state*)))
  runtime)

(defn subscriber-registry-snapshot []
  (:subscribers (event-subscriber-state-snapshot)))

(defn reset-ability-event-subscribers-for-test!
  ([]
   (reset-ability-event-subscribers-for-test! {}))
  ([snapshot]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in event-path {:subscribers (or snapshot {}) :frozen? false}))
   nil))

(defn freeze-ability-event-subscribers! []
  (update-event-subscriber-state! assoc :frozen? true)
  nil)

(defn subscribe-ability-event!
  [event-type handler-fn]
  (assert-subscribers-open!)
  (update-event-subscriber-state! update-in [:subscribers event-type] (fnil conj []) handler-fn)
  nil)

(defn fire-ability-event!
  [event]
  (let [event-type (:event/type event)]
    (doseq [h (get (:subscribers (event-subscriber-state-snapshot)) event-type [])]
      (try (h event)
           (catch Exception e
             (log/warn "Ability event subscriber threw" event-type (ex-message e)))))
    event))

(defn fire-calc-event!
  "Chain base-value through all handlers subscribed to event-type, each seeing
  the running :value from the previous handler. Zero-allocation fast path when
  no handler is subscribed (the common case on the per-player tick path)."
  [event-type base-value extra]
  (let [handlers (get (:subscribers (event-subscriber-state-snapshot)) event-type)]
    (if-not (seq handlers)
      base-value
      (let [event (merge extra {:event/type event-type})]
        (reduce (fn [value h]
                  (let [result (try (h (assoc event :value value))
                                    (catch Exception e
                                      (log/warn "Calc event subscriber threw" event-type (ex-message e))
                                      nil))]
                    (if (number? result) result value)))
                base-value
                handlers)))))

;; Event Type Constants
(def EVT-ABILITY-ACTIVATE    :ability/activate)
(def EVT-ABILITY-DEACTIVATE  :ability/deactivate)
(def EVT-SKILL-LEARN         :ability/skill-learn)
(def EVT-SKILL-PERFORM       :ability/skill-perform)
(def EVT-SKILL-EXP-ADDED     :ability/skill-exp-added)
(def EVT-SKILL-EXP-CHANGED   :ability/skill-exp-changed)
(def EVT-LEVEL-CHANGE        :ability/level-change)
(def EVT-CATEGORY-CHANGE     :ability/category-change)
(def EVT-ACHIEVEMENT-TRIGGER :ability/achievement-trigger)
(def EVT-OVERLOAD            :ability/overload)
(def EVT-PRESET-UPDATE       :ability/preset-update)
(def EVT-PRESET-SWITCH       :ability/preset-switch)
(def EVT-CONTEXT-KEY-DOWN    :ability/context-key-down)
(def EVT-CONTEXT-KEY-TICK    :ability/context-key-tick)
(def EVT-CONTEXT-KEY-UP      :ability/context-key-up)
(def EVT-CONTEXT-KEY-ABORT   :ability/context-key-abort)
(def EVT-CONTEXT-REGISTERED  :ability/context-registered)
(def EVT-CONTEXT-STATUS-CHANGED :ability/context-status-changed)
(def EVT-CONTEXT-PURGED      :ability/context-purged)
(def CALC-SKILL-ATTACK       :calc/skill-attack)
(def CALC-SKILL-PERFORM      :calc/skill-perform)
(def CALC-MAX-CP             :calc/max-cp)
(def CALC-MAX-OVERLOAD       :calc/max-overload)
(def CALC-CP-RECOVER-SPEED   :calc/cp-recover-speed)
(def CALC-OVERLOAD-RECOVER-SPEED :calc/overload-recover-speed)

;; Helper Constructors
(defn make-skill-learn-event [uuid skill-id]
  {:event/type EVT-SKILL-LEARN :event/side :both :uuid uuid :skill-id skill-id})
(defn make-level-change-event [uuid old-level new-level]
  {:event/type EVT-LEVEL-CHANGE :event/side :server :uuid uuid :old-level old-level :new-level new-level})
(defn make-category-change-event [uuid old-cat new-cat]
  {:event/type EVT-CATEGORY-CHANGE :event/side :both :uuid uuid :old-cat old-cat :new-cat new-cat})
(defn make-overload-event [uuid]
  {:event/type EVT-OVERLOAD :event/side :server :uuid uuid})
(defn make-activate-event [uuid]
  {:event/type EVT-ABILITY-ACTIVATE :event/side :server :uuid uuid})
(defn make-deactivate-event [uuid]
  {:event/type EVT-ABILITY-DEACTIVATE :event/side :server :uuid uuid})
(defn make-preset-switch-event [uuid old-idx new-idx]
  {:event/type EVT-PRESET-SWITCH :event/side :both :uuid uuid :old-preset old-idx :new-preset new-idx})
(defn make-preset-update-event [uuid preset-idx key-idx slot]
  {:event/type EVT-PRESET-UPDATE :event/side :both :uuid uuid :preset-idx preset-idx :key-idx key-idx :slot slot})
(defn make-skill-perform-event [uuid skill-id]
  {:event/type EVT-SKILL-PERFORM :event/side :server :uuid uuid :skill-id skill-id})
(defn make-context-registered-event [uuid ctx-id skill-id status]
  {:event/type EVT-CONTEXT-REGISTERED :event/side :server :uuid uuid :ctx-id ctx-id :skill-id skill-id :status status})
(defn make-context-status-changed-event [uuid ctx-id old-status new-status reason]
  {:event/type EVT-CONTEXT-STATUS-CHANGED :event/side :server :uuid uuid :ctx-id ctx-id :old-status old-status :new-status new-status :reason reason})
(defn make-context-purged-event [uuid removed-count]
  {:event/type EVT-CONTEXT-PURGED :event/side :server :uuid uuid :removed-count removed-count})
(defn make-achievement-trigger-event [uuid achievement-id]
  {:event/type EVT-ACHIEVEMENT-TRIGGER :event/side :server :uuid uuid :achievement-id achievement-id})

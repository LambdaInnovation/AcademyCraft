(ns cn.li.ac.ability.client.hand-effects
  "Registry-based hand effect infrastructure for first-person hand animations.

  Skills register their hand-effect handlers via `register-hand-effect!` at
  load time.  The infrastructure dispatches enqueue / tick / transform calls
  without any skill-specific knowledge."
  (:require [cn.li.ac.ability.client.effects.queue-infra :as queue-infra]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util ArrayDeque HashMap ArrayList]))

;; ---------------------------------------------------------------------------
;; Camera pitch delta accumulator (shared across all hand effects)
;; ---------------------------------------------------------------------------

(defonce ^:private fallback-camera-pitch-queue (ArrayDeque. 1024))

(defn- camera-pitch-deltas-queue ^ArrayDeque
  []
  fallback-camera-pitch-queue)

(defn- normalize-session-id
  [owner-or-session]
  (queue-infra/normalize-session-id "hand" owner-or-session))

(defn current-effect-owner
  []
  (queue-infra/current-effect-owner "hand"))

(defn add-camera-pitch-delta!
  "Queue a camera pitch delta (float degrees) to be consumed by the platform
  renderer on the next frame."
  ([delta]
   (add-camera-pitch-delta! nil delta))
  ([owner-or-session delta]
   (queue-infra/queue-effect! (camera-pitch-deltas-queue) "hand" owner-or-session (float delta))
   nil))

(defn add-current-camera-pitch-delta!
  [delta]
  (add-camera-pitch-delta! (current-effect-owner) delta))

(defn drain-camera-pitch-deltas!
  "Atomically drain and return all queued camera pitch deltas."
  ([]
   (drain-camera-pitch-deltas! nil))
  ([owner-or-session]
   (queue-infra/poll-effects! (camera-pitch-deltas-queue) "hand" owner-or-session)))

(defn clear-session-camera-pitch-deltas!
  "Remove all queued camera pitch deltas for a specific session from the
  lock-free queue."
  ([]
   (clear-session-camera-pitch-deltas! nil))
  ([owner-or-session]
   (let [session-id (normalize-session-id owner-or-session)
         ^ArrayDeque q (camera-pitch-deltas-queue)]
     (.removeIf q (reify java.util.function.Predicate
                    (test [_ entry]
                      (= session-id (first entry))))))
   nil))

(defn clear-owner-camera-pitch-deltas!
  [owner]
  (clear-session-camera-pitch-deltas! owner))

;; ---------------------------------------------------------------------------
;; Utility functions (shared by skill hand-effect impls)
;; ---------------------------------------------------------------------------

(defn smoothstep
  "Hermite interpolation: 0 at edge0, 1 at edge1."
  ^double [^double edge0 ^double edge1 ^double x]
  (let [t (max 0.0 (min 1.0 (/ (- x edge0) (- edge1 edge0))))]
    (* t t (- 3.0 (* 2.0 t)))))

(defn sample-curve
  "Piecewise-linear interpolation on a sorted seq of [x y] control points."
  ^double [curve ^double t]
  (cond
    (<= t (ffirst curve)) (second (first curve))
    (>= t (first (last curve))) (second (last curve))
    :else
    (let [[a b] (first (filter (fn [[[x0 _] [x1 _]]]
                                 (and (<= x0 t) (< t x1)))
                               (partition 2 1 curve)))]
      (if (and a b)
        (let [[x0 y0] a
              [x1 y1] b
              frac (/ (- t x0) (- x1 x0))]
          (+ y0 (* frac (- y1 y0))))
        (second (last curve))))))

;; ---------------------------------------------------------------------------
;; Registry
;; ---------------------------------------------------------------------------

(defn default-hand-effect-runtime-state
  []
  {:registry {}
   :order []
   :effect-states {}
   :frozen? false})

(defonce ^:private ^HashMap hand-effect-registry (HashMap.))
(defonce ^:private ^ArrayList hand-effect-order (ArrayList.))
(defonce ^:private ^HashMap hand-effect-states (HashMap.))
(defonce ^:private hand-effect-frozen (boolean-array 1))

(defn- put-effect-state! [effect-id effect-state]
  (if (nil? effect-state)
    (.remove hand-effect-states effect-id)
    (.put hand-effect-states effect-id effect-state))
  effect-state)

(defn- assert-registry-open!
  []
  (when (aget ^booleans hand-effect-frozen 0)
    (throw (ex-info "Hand effect registry is frozen" {}))))

(defn register-hand-effect!
  "Register a hand effect handler.  `effect-id` is a keyword.

  `handler-map` keys:
    :enqueue-state-fn (fn [state ctx-id channel owner-key payload] -> state)
    :tick-state-fn    (fn [state] -> state)
    :initial-state    any (optional)
    :transform-fn     (fn [] -> transform-map or nil) (optional)"
  [effect-id handler-map]
  (when-not (and (keyword? effect-id) (map? handler-map)
                 (fn? (:enqueue-state-fn handler-map))
                 (fn? (:tick-state-fn handler-map)))
    (throw (IllegalArgumentException. "register-hand-effect!: invalid effect-id or handler-map")))
  (assert-registry-open!)
  (when-not (.containsKey hand-effect-registry effect-id)
    (.add hand-effect-order effect-id)
    (.put hand-effect-registry effect-id handler-map)
    (let [init (:initial-state handler-map)]
      (put-effect-state! effect-id (if (fn? init) (init) init))))
  (log/debug "Registered hand effect:" effect-id)
  nil)

(defn freeze-hand-effect-registry!
  []
  (aset-boolean ^booleans hand-effect-frozen 0 true)
  nil)

(defn hand-effect-registry-snapshot
  []
  {:registry (into {} hand-effect-registry)
   :order (vec hand-effect-order)
   :effect-states (into {} hand-effect-states)
   :frozen? (aget ^booleans hand-effect-frozen 0)
   :camera-pitch-deltas (vec (.toArray (camera-pitch-deltas-queue)))})

(defn effect-state-snapshot
  [effect-id]
  (.get hand-effect-states effect-id))

(defn reset-hand-effect-state-for-test!
  [effect-id state]
  (put-effect-state! effect-id state)
  nil)

(defn update-effect-state!
  [effect-id f & args]
  (put-effect-state! effect-id (apply f (.get hand-effect-states effect-id) args))
  nil)

(defn reset-hand-effect-registry-for-test!
  []
  (.clear (camera-pitch-deltas-queue))
  (.clear hand-effect-registry)
  (.clear hand-effect-order)
  (.clear hand-effect-states)
  (aset-boolean ^booleans hand-effect-frozen 0 false)
  nil)

;; ---------------------------------------------------------------------------
;; Dispatch
;; ---------------------------------------------------------------------------

(defn- default-owner-key [effect-id payload ctx-id _channel]
  (cond (and (map? payload) (:effect-instance-id payload)) [:effect-instance (:effect-instance-id payload)]
        ctx-id [:ctx ctx-id]
        (and (map? payload) (:source-player-id payload)) [:source-player (:source-player-id payload)]
        :else [:ctx ctx-id]))

(defn enqueue-hand-effect!
  "Dispatch an incoming FX payload to the registered hand-effect handler."
  [effect-id ctx-id channel payload & {:keys [owner-key]}]
  (if-let [{:keys [enqueue-state-fn]} (.get hand-effect-registry effect-id)]
    (let [owner-key* (or owner-key (default-owner-key effect-id payload ctx-id channel))]
      (put-effect-state! effect-id
                         (enqueue-state-fn (.get hand-effect-states effect-id)
                                           ctx-id channel owner-key* payload)))
    (log/warn "No hand effect registered for" effect-id)))

(defn tick-hand-effects!
  "Tick all registered hand effects."
  []
  (doseq [eid hand-effect-order]
    (when-let [{:keys [tick-state-fn]} (.get hand-effect-registry eid)]
      (put-effect-state! eid (tick-state-fn (.get hand-effect-states eid))))))

(defn current-hand-transform
  "Merge transforms from all registered hand effects.
  Returns the first non-nil transform (priority = registration order)."
  []
  (some (fn [eid]
          (when-let [{:keys [transform-fn]} (.get hand-effect-registry eid)]
            (when transform-fn (transform-fn))))
        hand-effect-order))

(defn registered-effects
  "Return the set of currently-registered hand effect ids."
  []
  (set (.keySet hand-effect-registry)))

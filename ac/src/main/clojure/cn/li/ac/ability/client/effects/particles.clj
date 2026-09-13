(ns cn.li.ac.ability.client.effects.particles
  "Particle effect commands for ability system (AC layer - no Minecraft imports).

  Queue uses a client-thread-confined bounded ArrayDeque. It drops the oldest
  entry at 8,192 commands and never performs an O(n) size scan."
  (:require [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.client.effects.queue-infra :as queue-infra])
  (:import [java.util ArrayDeque]))

;; ============================================================================
;; Particle effect command factories (pure data constructors)
;; ============================================================================

(defn make-skill-activation-particles
  [player-pos skill-category]
  (let [particle-type (case skill-category
                        :electromaster :electric-spark
                        :teleporter :portal
                        :meltdowner :flame
                        :vecmanip :end-rod
                        :generic)]
    {:type :particle :particle-type particle-type
     :x (:x player-pos) :y (+ (:y player-pos) 1.0) :z (:z player-pos)
     :count 10 :speed 0.1 :offset-x 0.5 :offset-y 0.5 :offset-z 0.5}))

(defn make-cp-consumption-particles
  [player-pos amount]
  (when (> amount 10)
    {:type :particle :particle-type :enchant
     :x (:x player-pos) :y (+ (:y player-pos) 1.5) :z (:z player-pos)
     :count (int (/ amount 10)) :speed 0.05 :offset-x 0.3 :offset-y 0.3 :offset-z 0.3}))

(defn make-overload-particles
  [player-pos]
  {:type :particle :particle-type :angry-villager
   :x (:x player-pos) :y (+ (:y player-pos) 2.0) :z (:z player-pos)
   :count 5 :speed 0.0 :offset-x 0.5 :offset-y 0.5 :offset-z 0.5})

(defn make-level-up-particles
  [player-pos]
  {:type :particle :particle-type :totem-of-undying
   :x (:x player-pos) :y (+ (:y player-pos) 1.0) :z (:z player-pos)
   :count 30 :speed 0.2 :offset-x 1.0 :offset-y 1.0 :offset-z 1.0})

;; ============================================================================
;; Lock-free queue singleton (lazy init via delay)
;;
;; Fixed-capacity client queue.
;; ============================================================================

(def ^:private particle-queue-delay
  (delay (ArrayDeque. 8192)))

(defn- particle-queue
  "Lazy singleton bounded deque."
  []
  @particle-queue-delay)

;; ============================================================================
;; Queue operations (backward-compatible arities)
;; ============================================================================

(defn queue-particle-effect!
  "Enqueue a particle effect on the client thread.
   (queue-particle-effect! cmd)           — singleton queue, current owner
   (queue-particle-effect! owner cmd)     — singleton queue, explicit owner"
  ([particle-cmd]
   (queue-particle-effect! nil particle-cmd))
  ([owner-or-session particle-cmd]
   (queue-infra/queue-effect! (particle-queue) "particle" owner-or-session particle-cmd)
   nil))

(defn current-effect-owner
  "Return the current effect owner for particle queue operations."
  []
  (queue-infra/current-effect-owner "particle"))

(defn queue-current-particle-effect!
  "Enqueue a particle effect for the current effect owner."
  [particle-cmd]
  (queue-particle-effect! (current-effect-owner) particle-cmd)
  nil)

(defn poll-particle-effects!
  "Drain all queued particle effects for a session. Safe from render thread."
  ([]
   (poll-particle-effects! nil))
  ([owner-or-session]
   (queue-infra/poll-effects! (particle-queue) "particle" owner-or-session)))

(defn particle-queue-snapshot
  "Return a snapshot of the singleton queue for diagnostics."
  ([]
   (vec (.toArray ^ArrayDeque (particle-queue))))
  ([owner-or-session]
   (let [session-id (queue-infra/normalize-session-id "particle" owner-or-session)
         results (java.util.ArrayList.)]
     (doseq [^clojure.lang.PersistentVector entry (.toArray ^ArrayDeque (particle-queue))]
       (let [[entry-sid cmd] entry]
         (when (= session-id entry-sid)
           (.add results cmd))))
     (vec results))))


(defn clear-session-particle-effects!
  ([]
   (clear-session-particle-effects! nil))
  ([owner-or-session]
   (let [session-id (queue-infra/normalize-session-id "particle" owner-or-session)
         ^ArrayDeque q (particle-queue)
         keep (java.util.ArrayList.)]
     (doseq [^clojure.lang.PersistentVector entry (.toArray q)]
       (let [[entry-sid _] entry]
         (when-not (= session-id entry-sid)
           (.add keep entry))))
     (.clear q)
     (doseq [entry keep]
       (.addLast ^ArrayDeque q entry)))
   nil))

(defn clear-owner-particle-effects!
  [owner]
  (clear-session-particle-effects! owner))

(defn reset-particle-queue-for-test!
  ([]
   (.clear ^ArrayDeque (particle-queue))
   nil)
  ([_queues]
   (reset-particle-queue-for-test!)))

;; ============================================================================
;; Event listeners (automatic particle spawning on ability events)
;; ============================================================================

(defn on-skill-activation
  [event]
  (when-let [player-pos (:player-pos event)]
    (when-let [category (:category event)]
      (queue-particle-effect! (make-skill-activation-particles player-pos category)))))

(defn on-overload
  [event]
  (when-let [player-pos (:player-pos event)]
    (queue-particle-effect! (make-overload-particles player-pos))))

(defn on-level-up
  [event]
  (when-let [player-pos (:player-pos event)]
    (queue-particle-effect! (make-level-up-particles player-pos))))

(defn init!
  "Initialize particle effect system. Wire event listeners."
  []
  (evt/subscribe-ability-event!
   evt/EVT-ABILITY-ACTIVATE
   (fn [event] (on-skill-activation event)))
  (evt/subscribe-ability-event!
   evt/EVT-LEVEL-CHANGE
   (fn [event] (on-level-up event)))
  nil)

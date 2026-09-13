(ns cn.li.ac.ability.service.reflection-damage
  "Server-thread-confined VecReflection damage queue.

  Damage interception only records UUID-based tasks. Tasks are drained after
  the current server tick, outside the native hurt/event call stack, so thorns
  and third-party reflection handlers cannot grow a synchronous recursion."
  (:require [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util Iterator LinkedHashMap Map$Entry]))

(def ^:private max-pending-tasks 256)

(defonce ^:private ^LinkedHashMap pending-by-pair (LinkedHashMap.))

(defn- task-key
  [{:keys [world-id caster-id target-id]}]
  [(str (or world-id ""))
   (str (or caster-id ""))
   (str (or target-id ""))])

(defn- valid-task?
  [{:keys [world-id caster-id target-id damage]}]
  (let [damage* (double (or damage 0.0))]
    (and (some? world-id)
         (some? caster-id)
         (some? target-id)
         (not= (str caster-id) (str target-id))
         (Double/isFinite damage*)
         (pos? damage*))))

(defn enqueue!
  "Queue reflected damage for server tick end. Repeated part hits from the
  same caster/target pair in one tick collapse to the largest damage value."
  [task]
  (if-not (valid-task? task)
    false
    (let [task* (-> task
                    (update :world-id str)
                    (update :caster-id str)
                    (update :target-id str)
                    (update :damage double))
          key (task-key task*)
          existing (.get pending-by-pair key)]
      (cond
        existing
        (do
          (when (> (double (:damage task*)) (double (:damage existing)))
            (.put pending-by-pair key task*))
          true)

        (>= (.size pending-by-pair) max-pending-tasks)
        (do
          (log/warn "VecReflection damage queue is full; dropping task for" (:target-id task*))
          false)

        :else
        (do
          (.put pending-by-pair key task*)
          true)))))

(defn pending-tasks-snapshot
  []
  (vec (.values pending-by-pair)))

(defn clear-player-tasks!
  [player-id]
  (let [player-id* (str player-id)
        ^Iterator iterator (.iterator (.entrySet pending-by-pair))]
    (while (.hasNext iterator)
      (let [^Map$Entry entry (.next iterator)
            task (.getValue entry)]
        (when (or (= player-id* (:caster-id task))
                  (= player-id* (:target-id task)))
          (.remove iterator)))))
  nil)

(defn clear-all-tasks!
  []
  (.clear pending-by-pair)
  nil)

(defn drain!
  "Clear the active batch before applying it. Any damage scheduled by nested
  hooks therefore belongs to the next tick even if a foreign mod reacts to
  the dedicated reflection source."
  []
  (let [tasks (pending-tasks-snapshot)]
    (.clear pending-by-pair)
    (mapv
     (fn [{:keys [world-id caster-id target-id damage] :as task}]
       (try
         (assoc task
                :applied?
                (boolean
                 (and (entity-damage/available?)
                      (entity-damage/apply-direct-damage!
                       world-id
                       target-id
                       damage
                       :vec-reflection
                       {:attacker-uuid caster-id}))))
         (catch Exception e
           (log/warn "Deferred VecReflection damage failed:" (ex-message e))
           (assoc task :applied? false))))
     tasks)))

(defn reset-for-test!
  []
  (clear-all-tasks!))

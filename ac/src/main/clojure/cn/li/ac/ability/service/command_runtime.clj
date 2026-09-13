(ns cn.li.ac.ability.service.command-runtime
  "Imperative shell bridge for reducer command execution.

  This namespace is the runtime entry for command-based state transitions:
  1) load current player state
  2) apply reducer command(s)
  3) persist resulting state
  4) execute emitted events/effects"
  (:require
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.ac.ability.effects.interpreter :as interpreter]
            [cn.li.ac.ability.service.reducer :as reducer]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner])
  (:import [java.util HashMap Map$Entry]))

(def ^:private default-command-trace-ttl-ms 60000)
(def ^:private default-max-command-traces 2048)

;; Trace map and the auto-id sequence both live under Framework [:service ...] —
;; only commands with an explicit :command-id (idempotent network retries) are ever
;; traced or looked up, so this map stays small and is never touched by tick commands.
(defonce ^:private ^HashMap command-traces (HashMap.))

(defn- now-ms
  []
  (System/currentTimeMillis))

(defn- trace-key
  [session-id uuid command-id]
  [session-id uuid command-id])

(defn- prune-command-traces!
  [^long now ^long ttl-ms ^long max-size]
  (let [iterator (.iterator (.entrySet command-traces))]
    (while (.hasNext iterator)
      (let [^Map$Entry entry (.next iterator)
            value (.getValue entry)]
        (when (> (- now (long (:completed-at-ms value))) ttl-ms)
          (.remove iterator)))))
  (while (> (.size command-traces) max-size)
    (let [iterator (.iterator (.entrySet command-traces))]
      (when (.hasNext iterator)
        (.next iterator)
        (.remove iterator)))))

(defn- lookup-command-trace
  [session-id uuid command-id]
  (when command-id
    (.get command-traces (trace-key session-id uuid command-id))))

(defn- record-command-trace!
  "Only ever called for commands carrying an explicit :command-id — auto-generated
   tick command ids are never traced (they can never be replayed/looked up)."
  [session-id uuid command-id result]
  (let [at (now-ms)
        ttl-ms default-command-trace-ttl-ms
        max-size default-max-command-traces
        k (trace-key session-id uuid command-id)
        entry {:completed-at-ms at :result result}]
    (.put command-traces k entry)
    (when (> (.size command-traces) max-size)
      (prune-command-traces! at ttl-ms max-size)))
  nil)

(defn reset-command-traces-for-test!
  []
  (.clear command-traces)
  nil)

(defn command-traces-snapshot
  []
  (into {} command-traces))

(defn- ensure-command-owner-fields
  [session-id uuid command]
  (-> command
      (assoc :uuid uuid)
      (assoc :player-uuid uuid)
      (cond->
        session-id (assoc :session-id session-id))))

(defn- normalize-commands
  [session-id uuid commands]
  (let [command-count (count commands)]
    (loop [index 0
           result (transient [])]
      (if (< index command-count)
        (recur (unchecked-inc-int index)
               (conj! result
                      (ensure-command-owner-fields session-id uuid
                                                   (nth commands index))))
        (persistent! result)))))

(defn- resolve-session-id
  [session-id]
  (or session-id
      (runtime-hooks/player-state-session-id)
      (throw (ex-info "Command runtime requires explicit/bound session-id"
                      {:provided-session-id session-id
                       :player-state-owner (runtime-hooks/current-player-state-owner)}))))

(defn- run-command*
  [session-id uuid command {:keys [mark-dirty?]
                            :or {mark-dirty? true}
                            :as opts}]
  ;; Network boundaries validate before enqueueing. Internal commands are trusted
  ;; and pay no schema/assertion or generated-id cost.
  (let [explicit-command-id (:command-id command)
        session-id (resolve-session-id session-id)
        normalized (ensure-command-owner-fields session-id uuid command)
        cached (when (and explicit-command-id (:idempotent? opts))
                 (some-> (lookup-command-trace session-id uuid explicit-command-id)
                         :result))]
    (if cached
      (assoc cached :idempotent-replay? true)
      (let [owner (or (runtime-hooks/current-player-state-owner)
                      (when session-id
                        (if (runtime-hooks/player-state-server-session-id)
                          {:logical-side :server
                           :server-session-id session-id}
                          {:logical-side :client
                           :client-session-id session-id})))
            state (store/get-or-create-player-state! session-id uuid)
            result (reducer/apply-command state normalized)
            next-state (:state result)]
        (when (and next-state (not= next-state state))
          (let [mask (if mark-dirty?
                       (store/changed-sync-mask state next-state)
                       0)]
            (store/commit-player-state! session-id uuid next-state mask)))
        (if (= owner (runtime-hooks/current-player-state-owner))
          (interpreter/execute-reducer-result! result)
          (runtime-hooks/with-player-state-owner-fn
            owner
            (fn [] (interpreter/execute-reducer-result! result))))
        (when explicit-command-id
          (record-command-trace! session-id uuid explicit-command-id result))
        result))))

(defn run-command-in-session!
  "Execute one reducer command with an explicit runtime session id.

  This API is the migration target for removing implicit owner resolution."
  ([session-id uuid command]
   (run-command-in-session! session-id uuid command {}))
  ([session-id uuid command opts]
   (run-command* session-id uuid command opts)))

(defn- run-commands*
  [session-id uuid commands {:keys [mark-dirty?]
                             :or {mark-dirty? true}}]
  (let [session-id (resolve-session-id session-id)
      owner (or (runtime-hooks/current-player-state-owner)
                (when session-id
                  {:logical-side :server
                   :server-session-id session-id}))
        state (store/get-or-create-player-state! session-id uuid)
        normalized (normalize-commands session-id uuid commands)
        result (reducer/apply-commands state normalized)
        next-state (:state result)]
    (when (and next-state (not= next-state state))
      (let [mask (if mark-dirty?
                   (store/changed-sync-mask state next-state)
                   0)]
        (store/commit-player-state! session-id uuid next-state mask)))
    (if (= owner (runtime-hooks/current-player-state-owner))
      (interpreter/execute-reducer-result! result)
      (runtime-hooks/with-player-state-owner-fn
        owner
        (fn [] (interpreter/execute-reducer-result! result))))
    result))

(defn run-commands-in-session!
  "Execute multiple reducer commands with an explicit runtime session id."
  ([session-id uuid commands]
   (run-commands-in-session! session-id uuid commands {}))
  ([session-id uuid commands opts]
   (run-commands* session-id uuid commands opts)))



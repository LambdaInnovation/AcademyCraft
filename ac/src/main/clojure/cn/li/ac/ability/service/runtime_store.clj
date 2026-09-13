(ns cn.li.ac.ability.service.runtime-store
  "Thread-confined ability runtime state.

  A server session owns a UUID keyed HashMap of PlayerRuntime values. The
  Minecraft server thread is the only writer: there are no composite string
  keys, ConcurrentHashMaps, per-player atoms, or CAS retries. Player data is
  still represented as Clojure values at the domain boundary, while dirty
  tracking and revisions are primitive mutable fields."
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.model.preset :as pdata]
            [cn.li.ac.ability.model.cooldown :as cdata]
            [cn.li.ac.ability.model.develop :as ddata])
  (:import [java.util ArrayDeque HashMap]))

(def ability-data-mask 0x01)
(def resource-data-mask 0x02)
(def cooldown-data-mask 0x04)
(def preset-data-mask 0x08)
(def develop-data-mask 0x10)
(def all-sync-mask 0x1f)

(def sync-domains
  #{:ability-data :resource-data :cooldown-data :preset-data :develop-data})

(defn domain-mask
  ^long [domain]
  (case domain
    :ability-data ability-data-mask
    :resource-data resource-data-mask
    :cooldown-data cooldown-data-mask
    :preset-data preset-data-mask
    :develop-data develop-data-mask
    0))

(defn domains->mask
  ^long [domains]
  (if (number? domains)
    (long domains)
    (loop [remaining (seq domains)
           mask 0]
      (if remaining
        (recur (next remaining)
               (bit-or (long mask) (domain-mask (first remaining))))
        (long mask)))))

(defn mask->domains
  [^long mask]
  (cond-> #{}
    (not (zero? (bit-and mask ability-data-mask))) (conj :ability-data)
    (not (zero? (bit-and mask resource-data-mask))) (conj :resource-data)
    (not (zero? (bit-and mask cooldown-data-mask))) (conj :cooldown-data)
    (not (zero? (bit-and mask preset-data-mask))) (conj :preset-data)
    (not (zero? (bit-and mask develop-data-mask))) (conj :develop-data)))

(defn fresh-player-state
  "Initial player-state containing every sync domain with its constructor.

  Full sync sets all domain bits; missing domains previously became nil in the
  payload and were silently encoded as {} on the wire, which then crashed
  consumers that expect schema-complete maps (e.g. develop/progress)."
  []
  {:ability-data (adata/new-ability-data)
   :resource-data (rdata/new-resource-data)
   :cooldown-data (cdata/new-cooldown-data)
   :preset-data (pdata/new-preset-data)
   :develop-data (ddata/new-develop-data)
   ;; Upstream keeps the /aim cheat switch in PlayerDataTag, outside
   ;; AbilityData/CPData, and it survives relog. Its own persisted domain here
   ;; for the same reason: it gates commands, it is not ability progression.
   :cheats-data {:enabled? false}
   :context-registry {}})

(definterface IPlayerRuntime
  (^Object uuid [])
  (^Object state [])
  (^long dirtyMask [])
  (^long revision [])
  (^boolean queued [])
  (^Object replaceState [^Object next-state ^long dirty-mask])
  (^long markDirty [^long dirty-mask])
  (^void clearDirty [])
  (^void markQueued [])
  (^void clearQueued []))

(deftype PlayerRuntime
  [uuid
   ^:unsynchronized-mutable state-value
   ^:unsynchronized-mutable ^long dirty-mask
   ^:unsynchronized-mutable ^long revision-value
   ^:unsynchronized-mutable ^long queued-flag]
  IPlayerRuntime
  (uuid [_] uuid)
  (state [_] state-value)
  (dirtyMask [_] dirty-mask)
  (revision [_] revision-value)
  (queued [_] (not (zero? queued-flag)))
  (replaceState [_ next-state changed-mask]
    (set! state-value next-state)
    (when-not (zero? changed-mask)
      (set! dirty-mask (bit-or dirty-mask changed-mask))
      (set! revision-value (unchecked-inc revision-value)))
    state-value)
  (markDirty [_ changed-mask]
    (when-not (zero? changed-mask)
      (set! dirty-mask (bit-or dirty-mask changed-mask))
      (set! revision-value (unchecked-inc revision-value)))
    dirty-mask)
  (clearDirty [_]
    (set! dirty-mask 0))
  (markQueued [_]
    (set! queued-flag 1))
  (clearQueued [_]
    (set! queued-flag 0)))

(definterface ISessionRuntime
  (^Object id [])
  (^java.util.HashMap players [])
  (^java.util.ArrayDeque dirtyQueue []))

(deftype SessionRuntime [session-id ^HashMap player-runtimes ^ArrayDeque dirty-queue]
  ISessionRuntime
  (id [_] session-id)
  (players [_] player-runtimes)
  (dirtyQueue [_] dirty-queue))

(definterface IAbilityStore
  (^java.util.HashMap sessions []))

(deftype AbilityStore [^HashMap session-runtimes]
  IAbilityStore
  (sessions [_] session-runtimes))

(defn create-store
  ^IAbilityStore []
  (AbilityStore. (HashMap.)))

(defonce ^:private default-store (create-store))

(defn- ensure-store
  ^IAbilityStore []
  default-store)

(defn get-store
  ^IAbilityStore []
  (ensure-store))

(defn- session-runtime
  ^ISessionRuntime [^IAbilityStore store session-id]
  (when store
    (.get (.sessions store) session-id)))

(defn- ensure-session-runtime
  ^ISessionRuntime [^IAbilityStore store session-id]
  (when store
    (let [^HashMap sessions (.sessions store)]
      (or (.get sessions session-id)
          (let [runtime (SessionRuntime. session-id (HashMap.) (ArrayDeque.))]
            (.put sessions session-id runtime)
            runtime)))))

(defn player-runtime
  ^IPlayerRuntime [session-id player-uuid]
  (when-let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
    (.get (.players session) player-uuid)))

(defn- clean-state
  [state]
  (if (and (map? state) (contains? state :dirty-domains))
    (dissoc state :dirty-domains)
    state))

(defn- create-player-runtime!
  ^IPlayerRuntime [^ISessionRuntime session player-uuid state]
  (let [runtime (PlayerRuntime. player-uuid (clean-state state) 0 0 0)]
    (.put (.players session) player-uuid runtime)
    runtime))

(defn- ensure-player-runtime
  ^IPlayerRuntime [session-id player-uuid]
  (when-let [^ISessionRuntime session (ensure-session-runtime (ensure-store) session-id)]
    (or (.get (.players session) player-uuid)
        (create-player-runtime! session player-uuid (fresh-player-state)))))

(defn get-player-state
  [session-id player-uuid]
  (some-> (player-runtime session-id player-uuid) .state))

(defn get-or-create-player-state!
  [session-id player-uuid]
  (some-> (ensure-player-runtime session-id player-uuid) .state))

(defn set-player-state!
  [session-id player-uuid state]
  (let [^ISessionRuntime session (ensure-session-runtime (ensure-store) session-id)
        ^IPlayerRuntime runtime (or (.get (.players session) player-uuid)
                                    (create-player-runtime! session player-uuid state))]
    (.replaceState runtime (clean-state state) 0))
  nil)

(defn- update-player-state-core!
  [session-id player-uuid f]
  (let [^IPlayerRuntime runtime (ensure-player-runtime session-id player-uuid)
        next-state (clean-state (f (.state runtime)))]
    (.replaceState runtime next-state 0)))

(defn update-player-state!
  [session-id player-uuid f & args]
  (if (seq args)
    (update-player-state-core! session-id player-uuid
                               (fn [state] (apply f state args)))
    (update-player-state-core! session-id player-uuid f)))

(defn update-player-state-domain!
  [session-id player-uuid domain-key f]
  (let [^IPlayerRuntime runtime (ensure-player-runtime session-id player-uuid)
        current (.state runtime)
        next-state (assoc current domain-key (f (get current domain-key)))
        mask (domain-mask domain-key)]
    (.replaceState runtime next-state mask)))

(defn dirty-mask
  ^long [session-id player-uuid]
  (if-let [^IPlayerRuntime runtime (player-runtime session-id player-uuid)]
    (.dirtyMask runtime)
    0))

(defn player-revision
  ^long [session-id player-uuid]
  (if-let [^IPlayerRuntime runtime (player-runtime session-id player-uuid)]
    (.revision runtime)
    0))

(defn- enqueue-dirty-runtime!
  [session-id ^IPlayerRuntime runtime]
  (when-not (.queued runtime)
    (let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
      (.markQueued runtime)
      (.addLast (.dirtyQueue session) runtime))))

(defn commit-player-state!
  "Commit state and dirty mask in one thread-confined mutation."
  [session-id player-uuid state ^long changed-mask]
  (let [^IPlayerRuntime runtime (ensure-player-runtime session-id player-uuid)]
    (.replaceState runtime (clean-state state) changed-mask)
    (when-not (zero? changed-mask)
      (enqueue-dirty-runtime! session-id runtime))
    (.state runtime)))

(defn mark-dirty!
  ([session-id player-uuid]
   (mark-dirty! session-id player-uuid all-sync-mask))
  ([session-id player-uuid domains]
   (let [^IPlayerRuntime runtime (ensure-player-runtime session-id player-uuid)
         mask (domains->mask domains)]
     (.markDirty runtime mask)
     (when (and (not (zero? mask)) (not (.queued runtime)))
       (enqueue-dirty-runtime! session-id runtime)))
   nil))

(defn mark-player-dirty!
  ([session-id player-uuid]
   (mark-dirty! session-id player-uuid))
  ([session-id player-uuid domains]
   (mark-dirty! session-id player-uuid domains)))

(defn clear-dirty!
  [session-id player-uuid]
  (when-let [^IPlayerRuntime runtime (player-runtime session-id player-uuid)]
    (.clearDirty runtime)
    (.clearQueued runtime))
  nil)

(defn drain-dirty-players!
  [session-id]
  (if-let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
    (let [^ArrayDeque queue (.dirtyQueue session)
          result (transient [])]
      (loop []
        (if-let [^IPlayerRuntime runtime (.pollFirst queue)]
          (do
            (.clearQueued runtime)
            (when-not (zero? (.dirtyMask runtime))
              (conj! result (.uuid runtime)))
            (recur))
          (persistent! result))))
    []))

(defn get-dirty-players
  [session-id]
  (if-let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
    (let [result (transient [])]
      (doseq [^IPlayerRuntime runtime (.values (.players session))]
        (when-not (zero? (.dirtyMask runtime))
          (conj! result (.uuid runtime))))
      (persistent! result))
    []))

(defn create-session!
  [session-id]
  (ensure-session-runtime (ensure-store) session-id)
  nil)

(defn remove-player-state!
  [session-id player-uuid]
  (when-let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
    (.remove (.players session) player-uuid))
  nil)

(defn remove-session!
  [session-id]
  (when-let [^IAbilityStore store (ensure-store)]
    (.remove (.sessions store) session-id))
  nil)

(defn list-sessions
  []
  (if-let [^IAbilityStore store (ensure-store)]
    (vec (.keySet (.sessions store)))
    []))

(defn list-players
  [session-id]
  (if-let [^ISessionRuntime session (session-runtime (ensure-store) session-id)]
    (vec (.keySet (.players session)))
    []))

(defn reset-store!
  []
  (.clear ^HashMap (.sessions ^IAbilityStore default-store))
  nil)

(defn snapshot
  []
  (if-let [^IAbilityStore store (ensure-store)]
    (let [result (transient {})]
      (doseq [^ISessionRuntime session (.values (.sessions store))]
        (let [players (transient {})]
          (doseq [^IPlayerRuntime runtime (.values (.players session))]
            (assoc! players (.uuid runtime)
                    (assoc (.state runtime)
                           :dirty-domains (mask->domains (.dirtyMask runtime))
                           :revision (.revision runtime))))
          (assoc! result (.id session) (persistent! players))))
      {:sessions (persistent! result)})
    {:sessions {}}))

(defn changed-sync-mask
  ^long [old-state new-state]
  (let [m0 0
        m1 (if (not= (get old-state :ability-data) (get new-state :ability-data))
             (bit-or m0 ability-data-mask) m0)
        m2 (if (not= (get old-state :resource-data) (get new-state :resource-data))
             (bit-or m1 resource-data-mask) m1)
        m3 (if (not= (get old-state :cooldown-data) (get new-state :cooldown-data))
             (bit-or m2 cooldown-data-mask) m2)
        m4 (if (not= (get old-state :preset-data) (get new-state :preset-data))
             (bit-or m3 preset-data-mask) m3)]
    (long (if (not= (get old-state :develop-data) (get new-state :develop-data))
            (bit-or m4 develop-data-mask)
            m4))))

(defn changed-sync-domains
  [old-state new-state]
  (mask->domains (changed-sync-mask old-state new-state)))

(defn with-player-state
  [session-id player-uuid f]
  (f (get-player-state session-id player-uuid)))

(defn apply-reducer-result!
  [session-id player-uuid {:keys [state]}]
  (when state
    (let [^IPlayerRuntime runtime (ensure-player-runtime session-id player-uuid)
          current (.state runtime)
          mask (changed-sync-mask current state)]
      (when-not (identical? state current)
        (commit-player-state! session-id player-uuid state mask))))
  state)

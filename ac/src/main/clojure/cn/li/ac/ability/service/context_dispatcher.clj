(ns cn.li.ac.ability.service.context-dispatcher
  "Context transport router: listeners, routes, id counters, message buffering.

  Business context state (status, input-state, skill-state, keepalive) is
  authoritative in runtime-store and updated only via reducer commands."
  (:require [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-domain :as context-domain]
            [cn.li.ac.ability.service.context-projection :as ctx-proj]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap HashSet]
           [java.util.concurrent ConcurrentHashMap]))

(def STATUS-CONSTRUCTED context-domain/status-constructed)
(def STATUS-ALIVE context-domain/status-alive)
(def STATUS-TERMINATED context-domain/status-terminated)

(defn status-valid-transition? [from to]
  (context-domain/status-valid-transition? from to))

(def ^:private STATIC-ROUTE-OWNER [::static-routes ::process])

(defn context-owner
  "Read the explicitly scoped thread owner, or use the provided owner."
  ([]
   (runtime-hooks/current-context-owner))
  ([owner]
   (or owner (context-owner))))

(defn with-context-owner-fn
  "Run thunk with an explicitly scoped owner without touching Framework."
  [ctx-owner thunk]
  (runtime-hooks/with-client-ctx-fn {:context-owner ctx-owner} thunk))

(defmacro with-context-owner
  "Macro wrapper over with-context-owner-fn."
  [ctx-owner & body]
  `(with-context-owner-fn ~ctx-owner (fn [] ~@body)))

;; Context dispatcher — Framework [:service :context-dispatcher]

(definterface IDispatcherRuntime
  (^Object contexts [])
  (^Object playerIndex [])
  (^Object routeFns [])
  (^Object clientCounters [])
  (^Object serverCounters [])
  (^Object deadlineBuckets []))

;; ConcurrentHashMaps throughout: context transport state is mutated from
;; the network/server threads while client ticks snapshot it (scan_vm_contexts
;; -> get_all_contexts) — a plain HashMap threw ConcurrentModificationException
;; mid-iteration. The weakly-consistent iterators never fail on concurrent
;; writes, and in-place reads (get/put/remove) stay O(1) lock-free.
(deftype DispatcherRuntime
  [^ConcurrentHashMap transport-contexts ^ConcurrentHashMap player-index ^ConcurrentHashMap route-fns
   ^ConcurrentHashMap client-counters ^ConcurrentHashMap server-counters ^ConcurrentHashMap deadline-buckets]
  IDispatcherRuntime
  (contexts [_] transport-contexts)
  (playerIndex [_] player-index)
  (routeFns [_] route-fns)
  (clientCounters [_] client-counters)
  (serverCounters [_] server-counters)
  (deadlineBuckets [_] deadline-buckets))

(defn create-dispatcher-runtime
  ^IDispatcherRuntime []
  (DispatcherRuntime. (ConcurrentHashMap.) (ConcurrentHashMap.) (ConcurrentHashMap.)
                      (ConcurrentHashMap.) (ConcurrentHashMap.) (ConcurrentHashMap.)))

(defonce ^:private default-runtime (create-dispatcher-runtime))
(def ^:dynamic *dispatcher-runtime* nil)

(defn- current-runtime
  ^IDispatcherRuntime []
  (or *dispatcher-runtime* default-runtime))

(defn- dispatcher-state-snapshot []
  (let [^IDispatcherRuntime runtime (current-runtime)]
    {:transport-contexts (into {} (.contexts runtime))
     :player-index (into {} (.playerIndex runtime))
     :route-fns (into {} (.routeFns runtime))
     :client-id-counter (into {} (.clientCounters runtime))
     :server-id-counter (into {} (.serverCounters runtime))}))

(defn- transport-contexts-snapshot
  []
  (into {} (.contexts (current-runtime))))

(defn- context-store-session-id
  [ctx]
  (owner/transport-store-session-id ctx))

(defn- context-store-player-uuid
  [ctx]
  (some-> (:player-uuid ctx) str))

(defn- run-context-status-command!
  [ctx status reason]
  (let [session-id (context-store-session-id ctx)
        player-uuid (context-store-player-uuid ctx)]
    (when (and session-id player-uuid)
      (command-rt/run-command-in-session!
        session-id
        player-uuid
        {:command :update-context-status
         :ctx-id  (:id ctx)
         :status  status
         :reason  reason}))))

(defn- route-fns-snapshot
  []
  (.routeFns (current-runtime)))

(declare context-player-index-key unschedule-deadline!)

(defn- assoc-transport!
  [key ctx]
  (let [^IDispatcherRuntime runtime (current-runtime)
        ^ConcurrentHashMap registry (.contexts runtime)
        ^ConcurrentHashMap index (.playerIndex runtime)
        index-key (context-player-index-key ctx)
        ^HashSet keys (or (.get index index-key)
                          (let [created (HashSet.)]
                            (.put index index-key created)
                            created))]
    (.put registry key ctx)
    (.add keys key))
  ctx)

(defn- update-transport-if-present!
  [key f & args]
  (let [^ConcurrentHashMap registry (.contexts (current-runtime))]
    (when-let [current (.get registry key)]
      (.put registry key (apply f current args))))
  nil)

(defn- dissoc-transport!
  [key]
  (let [^IDispatcherRuntime runtime (current-runtime)
        ^ConcurrentHashMap registry (.contexts runtime)
        ^ConcurrentHashMap index (.playerIndex runtime)]
    (when-let [ctx (.remove registry key)]
      (unschedule-deadline! ctx)
      (let [index-key (context-player-index-key ctx)
            ^HashSet keys (.get index index-key)]
        (when keys
          (.remove keys key)
          (when (.isEmpty keys)
            (.remove index index-key))))))
  nil)

(defn- clear-route-fns!
  []
  (.clear ^ConcurrentHashMap (.routeFns (current-runtime)))
  nil)

(defn- register-route-fns-entry!
  [owner-key routes]
  (.put ^ConcurrentHashMap (.routeFns (current-runtime)) owner-key routes)
  nil)

(defn- context-logical-side
  [ctx]
  (case (:logical-side ctx)
    (:client "client" :logical-side/client) :client
    (:server "server" :logical-side/server) :server
    (:any "any") :any
    (or (when (:server-id ctx) :server) :client)))

(defn- context-registry-key
  [ctx]
  [(context-logical-side ctx) (owner/transport-route-key ctx) (:id ctx)])

(defn- context-player-index-key
  [ctx]
  [(context-logical-side ctx)
   (owner/transport-route-key ctx)
   (some-> (:player-uuid ctx) str)])

;; ============================================================================
;; Deadline buckets — O(due entries) keepalive-timeout/purge scheduling.
;;
;; deadline-buckets maps a coarse time bucket (wall-clock ms / bucket-width,
;; floored) to the set of registry-keys whose deadline falls in that window.
;; Only buckets that were actually scheduled exist as HashMap entries, so
;; draining checks a handful of near-term buckets, never the full context
;; population. Bucket granularity only affects *when* an entry is looked at;
;; the caller (context-manager) always re-validates the precise deadline
;; before acting, so coarsening never causes an early or missed decision —
;; a not-yet-due entry surfaced by bucket rounding is simply rescheduled.
;; ============================================================================

(def ^:private deadline-bucket-width-ms 250)

(defn- deadline-bucket-key
  ^long [^long ms]
  (quot ms deadline-bucket-width-ms))

(defn- unschedule-deadline!
  [ctx]
  (when-let [bucket (:deadline-bucket ctx)]
    (let [^ConcurrentHashMap buckets (.deadlineBuckets (current-runtime))
          ^HashSet keys (.get buckets (long bucket))]
      (when keys
        (.remove keys (context-registry-key ctx))
        (when (.isEmpty keys)
          (.remove buckets (long bucket))))))
  nil)

(defn- schedule-deadline!
  "Schedule ctx's registry key into the deadline bucket for deadline-ms,
   moving it out of any bucket it was previously scheduled in. Returns ctx
   with :deadline-bucket updated; caller writes the result back into the
   transport map."
  [ctx deadline-ms]
  (unschedule-deadline! ctx)
  (let [bucket (deadline-bucket-key (long deadline-ms))
        ^ConcurrentHashMap buckets (.deadlineBuckets (current-runtime))
        ^HashSet keys (or (.get buckets bucket)
                          (let [created (HashSet.)]
                            (.put buckets bucket created)
                            created))]
    (.add keys (context-registry-key ctx))
    (assoc ctx :deadline-bucket bucket)))

(defn reschedule-deadline-for-key!
  "Re-schedule the context stored at registry-key to deadline-ms. Used when a
   drained bucket entry turns out not to be precisely due yet (bucket
   rounding) — keeps it under deadline-bucket tracking instead of dropping it."
  [registry-key deadline-ms]
  (update-transport-if-present! registry-key
    (fn [ctx] (schedule-deadline! ctx deadline-ms)))
  nil)

(defn get-raw-transport-context
  "Look up a raw transport context map by its exact registry key (as
   returned by drain-due-deadline-keys!). Intended for internal lifecycle
   managers only — bypasses owner resolution and store projection."
  [registry-key]
  (get (transport-contexts-snapshot) registry-key))

(defn get-projected-transport-context
  "Like `get-raw-transport-context`, but with the authoritative runtime-store
   fields (:status, :input-state, :last-keepalive-ms, ...) overlaid.

   Lifecycle deadline decisions must use this, not the raw map: keepalive
   refreshes land in the store via the :touch-context-keepalive command and
   are never written back onto the transport map, whose :last-keepalive-ms
   stays frozen at the value new-server-context stamped at creation.

   Unlike `get-context`, the transport owner metadata is preserved, so the
   caller can still resolve the owning route from the result."
  [registry-key]
  (when-let [ctx (get-raw-transport-context registry-key)]
    (ctx-proj/merge-store-projection ctx)))

(defn drain-due-deadline-keys!
  "Return registry keys whose scheduled deadline bucket is at or before
   `now`, removing those buckets. O(number of currently populated buckets
   at-or-before now), independent of total registered context count."
  [now]
  (let [^ConcurrentHashMap buckets (.deadlineBuckets (current-runtime))
        current-bucket (deadline-bucket-key (long now))
        due-buckets (java.util.ArrayList.)]
    (doseq [b (.keySet buckets)]
      (when (<= (long b) current-bucket)
        (.add due-buckets b)))
    (let [result (java.util.ArrayList.)]
      (doseq [b due-buckets]
        (when-let [^HashSet keys (.remove buckets b)]
          (.addAll result keys)))
      result)))

(defn- route-owner-key
  [owner-map]
  (let [logical-side (or (:logical-side owner-map) (:side owner-map))
        store (owner/store-session-id owner-map)]
    (if (and (nil? logical-side) (nil? store))
      STATIC-ROUTE-OWNER
      [(case logical-side
         (:client "client" :logical-side/client) :client
         (:server "server" :logical-side/server) :server
         (:any "any") :any
         logical-side)
       (owner/route-key owner-map logical-side)])))

(defn- context-owner-counter-key
  [owner-map logical-side]
  (let [side (case logical-side
               (:client "client" :logical-side/client) :client
               (:server "server" :logical-side/server) :server
               (:any "any") :any
               logical-side)]
    [side (owner/route-key owner-map side)]))

(defn- next-context-id!
  [counter-key owner prefix]
  (let [^IDispatcherRuntime runtime (current-runtime)
        ^ConcurrentHashMap counters (if (= counter-key :client-id-counter)
                            (.clientCounters runtime)
                            (.serverCounters runtime))
        next-counter (unchecked-inc (long (or (.get counters owner) 0)))]
    (.put counters owner next-counter)
    (str prefix "-" next-counter)))

(defn- resolve-context-owner
  [owner ctx-id _preferred-side]
  (let [resolved-owner (or owner (context-owner))]
    (when-not resolved-owner
      (throw (ex-info "Opaque ctx-id resolution requires context-owner or an explicit owner"
                      {:ctx-id ctx-id})))
    resolved-owner))

(defn- preferred-context-entry
  ([ctx-id]
   (preferred-context-entry nil ctx-id nil))
  ([ctx-id preferred-side]
   (preferred-context-entry nil ctx-id preferred-side))
  ([owner ctx-id preferred-side]
   (if (vector? ctx-id)
     (when-let [ctx (get (transport-contexts-snapshot) ctx-id)]
       [ctx-id ctx])
     (let [resolved-owner (resolve-context-owner owner ctx-id preferred-side)
           lookup-side (or preferred-side (owner/logical-side resolved-owner))
           route-key (owner/route-key resolved-owner lookup-side)
           key [lookup-side route-key ctx-id]]
       (when-let [ctx (get (transport-contexts-snapshot) key)]
         [key ctx])))))

(defn- route-fns-for-context
  [ctx]
  (let [side-key [(context-logical-side ctx) (owner/transport-route-key ctx)]
        route-any-key [:any (owner/transport-route-key ctx)]]
    (or (get (route-fns-snapshot) side-key)
        (get (route-fns-snapshot) route-any-key)
        (get (route-fns-snapshot) STATIC-ROUTE-OWNER)
        {:to-server nil :to-client nil :to-except-local nil})))

(defn- as-public-context
  [ctx]
  (when ctx
    (owner/public-context (ctx-proj/merge-store-projection ctx))))

(declare register-context!)

(defn new-context
  ([player-uuid skill-id]
   (new-context player-uuid skill-id (context-owner)))
  ([player-uuid skill-id owner]
   (let [owner* (resolve-context-owner owner nil :client)
         counter-owner (context-owner-counter-key owner* :client)
         id (next-context-id! :client-id-counter counter-owner "cid")
         base {:id        id :server-id nil :player-uuid (str player-uuid) :skill-id skill-id
               :status    STATUS-CONSTRUCTED :input-state :idle :message-buffer []
               :listeners {} :last-keepalive-ms nil :terminated-at-ms nil}]
     (owner/attach-transport-owner-metadata! base owner* :client))))

(defn new-server-context
  ([player-uuid skill-id client-id]
   (new-server-context player-uuid skill-id client-id (context-owner)))
  ([player-uuid skill-id client-id owner]
   (let [owner* (resolve-context-owner owner nil :server)
         counter-owner (context-owner-counter-key owner* :server)
         sid (next-context-id! :server-id-counter counter-owner "sid")
         base {:id        client-id :server-id sid :player-uuid (str player-uuid) :skill-id skill-id
               :status    STATUS-ALIVE :input-state :idle :message-buffer []
               :listeners {} :last-keepalive-ms (System/currentTimeMillis) :terminated-at-ms nil}]
     (owner/attach-transport-owner-metadata! base owner* :server))))

(defn start-context!
  [player-uuid skill-id]
  (let [ctx (new-context player-uuid skill-id (context-owner))]
    (register-context! ctx)
    ctx))

(defn start-server-context!
  [player-uuid skill-id client-id]
  (let [ctx (new-server-context player-uuid skill-id client-id (context-owner))]
    (register-context! ctx)
    ctx))

(defn- registration-deadline-ms
  "Deadline to schedule at register-context! time, or nil when the incoming
   ctx needs no deadline tracking. Covers both the normal case (server-owned
   context created already-alive by establish-context!, bypassing
   transition-to-alive!) and directly registering an already-terminated ctx
   (used by test/replay paths, bypassing terminate-context!) — either way
   register-context! is the single point where a freshly-stored ctx's
   deadline is derived from whatever state it arrives with."
  [ctx]
  (cond
    (and (= :server (context-logical-side ctx))
         (= STATUS-ALIVE (:status ctx))
         (:last-keepalive-ms ctx))
    (+ (long (:last-keepalive-ms ctx)) (long context-domain/keepalive-timeout-ms))

    (and (= STATUS-TERMINATED (:status ctx)) (:terminated-at-ms ctx))
    (+ (long (:terminated-at-ms ctx)) (long context-domain/terminated-context-grace-ms))

    :else nil))

(defn register-context! [ctx]
  (let [session-id (context-store-session-id ctx)
        player-uuid (context-store-player-uuid ctx)]
    (when (and session-id player-uuid)
      (command-rt/run-command-in-session!
        session-id
        player-uuid
        {:command  :register-context
         :ctx-id   (:id ctx)
         :skill-id (:skill-id ctx)
         :status   (or (:status ctx) STATUS-CONSTRUCTED)}))
    (let [deadline-ms (registration-deadline-ms ctx)
          ctx* (if deadline-ms (schedule-deadline! ctx deadline-ms) ctx)]
      (assoc-transport! (context-registry-key ctx*) ctx*)
      ctx*)))

(defn get-context
  ([ctx-id]
   (as-public-context (second (preferred-context-entry nil ctx-id nil))))
  ([owner ctx-id]
   (if (vector? ctx-id)
     (as-public-context (get (transport-contexts-snapshot) ctx-id))
     (let [[side route-key] (route-owner-key owner)
           ctx (get (transport-contexts-snapshot) [side route-key ctx-id])]
       (as-public-context ctx)))))

(defn get-all-contexts
  []
  (into {}
        (map (fn [[k v]] [k (as-public-context v)]))
        (transport-contexts-snapshot)))

(defn contexts-version-token
  "Cheap identity-stable snapshot of the raw transport-contexts map, suitable
  for identical?-based dirty-checking. Returns the same object across calls
  when no context has been added, removed, or updated."
  []
  (transport-contexts-snapshot))

(defn remove-context!
  ([ctx-id]
   (remove-context! nil ctx-id))
  ([owner ctx-id]
   (if (vector? ctx-id)
     (dissoc-transport! ctx-id)
     (when-let [[key _ctx] (preferred-context-entry owner ctx-id nil)]
       (dissoc-transport! key)))))

(defn- owner-matches-context?
  [owner ctx]
  (let [[logical-side route-key] (route-owner-key owner)]
    (and (= logical-side (context-logical-side ctx))
         (= route-key (owner/transport-route-key ctx)))))

(defn transport-contexts-for-player
  "Return raw transport contexts through the player index.

  The explicit-owner arity is the server/client hot path and performs no
  global context scan."
  ([owner player-uuid]
   (let [[logical-side route-key] (route-owner-key owner)
         ^IDispatcherRuntime runtime (current-runtime)
         ^ConcurrentHashMap registry (.contexts runtime)
         ^HashSet keys (.get ^ConcurrentHashMap (.playerIndex runtime)
                             [logical-side route-key (str player-uuid)])]
     (loop [remaining (seq keys)
            result (transient [])]
       (if remaining
         (recur (next remaining) (conj! result (get registry (first remaining))))
         (persistent! result)))))
  ([player-uuid]
   (let [pid (str player-uuid)]
     (->> (vals (transport-contexts-snapshot))
          (filter #(= pid (:player-uuid %)))
          vec))))

(defn get-all-contexts-for-player
  ([player-uuid]
   (mapv as-public-context (transport-contexts-for-player player-uuid)))
  ([owner player-uuid]
   (mapv as-public-context (transport-contexts-for-player owner player-uuid))))

(defn snapshot-context-registry []
  (get-all-contexts))

(defn snapshot-transport-contexts
  "Return full transport context maps including private owner metadata.
  Intended for internal lifecycle managers only."
  []
  (vals (transport-contexts-snapshot)))

(defn- remove-owner-context-entry
  [owner-key result registry-key ctx-map]
  (if (= owner-key [(context-logical-side ctx-map)
                    (owner/transport-route-key ctx-map)])
    result
    (assoc result registry-key ctx-map)))

(defn- remove-session-context-entry
  [store-session-id result registry-key ctx-map]
  (if (= store-session-id (context-store-session-id ctx-map))
    result
    (assoc result registry-key ctx-map)))

(defn- counter-for-other-session
  [store-session-id result counter-key counter]
  (let [[_side route-key] counter-key
        counter-session (if (vector? route-key) (first route-key) route-key)]
    (if (= store-session-id counter-session)
      result
      (assoc result counter-key counter))))

(defn clear-owner-contexts!
  [owner]
  (let [owner-key (route-owner-key owner)
        ^IDispatcherRuntime runtime (current-runtime)
        doomed (java.util.ArrayList.)]
    (doseq [[registry-key ctx-map] (.entrySet ^ConcurrentHashMap (.contexts runtime))]
      (when (= owner-key [(context-logical-side ctx-map)
                          (owner/transport-route-key ctx-map)])
        (.add doomed registry-key)))
    (doseq [registry-key doomed]
      (dissoc-transport! registry-key))
    (.remove ^ConcurrentHashMap (.clientCounters runtime) owner-key)
    (.remove ^ConcurrentHashMap (.serverCounters runtime) owner-key))
  nil)

(defn clear-store-session-contexts!
  "Clear all registered contexts and counters for one store session token."
  [store-session-id]
  (let [^IDispatcherRuntime runtime (current-runtime)
        doomed (java.util.ArrayList.)]
    (doseq [[registry-key ctx-map] (.entrySet ^ConcurrentHashMap (.contexts runtime))]
      (when (= store-session-id (context-store-session-id ctx-map))
        (.add doomed registry-key)))
    (doseq [registry-key doomed]
      (dissoc-transport! registry-key))
    (doseq [^ConcurrentHashMap counters [(.clientCounters runtime) (.serverCounters runtime)]]
      (let [iterator (.iterator (.keySet counters))]
        (while (.hasNext iterator)
          (let [[_side route-key] (.next iterator)
                counter-session (if (vector? route-key) (first route-key) route-key)]
            (when (= store-session-id counter-session)
              (.remove iterator)))))))
  nil)

(defn reset-contexts-for-test!
  ([]
   (reset-contexts-for-test! {}))
  ([contexts]
   (let [^IDispatcherRuntime runtime (current-runtime)]
     (.clear ^ConcurrentHashMap (.contexts runtime))
     (.clear ^ConcurrentHashMap (.playerIndex runtime))
     (.clear ^ConcurrentHashMap (.clientCounters runtime))
     (.clear ^ConcurrentHashMap (.serverCounters runtime))
     (.clear ^ConcurrentHashMap (.deadlineBuckets runtime))
     (doseq [[registry-key ctx-map] contexts]
       (assoc-transport! registry-key ctx-map)))
   nil))

(defn reset-route-fns-for-test!
  []
  (clear-route-fns!)
  nil)

(defn context-owned-by?
  [ctx-id player-uuid]
  (when-let [ctx (get-context ctx-id)]
    (= (str player-uuid) (:player-uuid ctx))))

(defn transition-to-alive!
  ([ctx-id server-id flush-fn]
   (transition-to-alive! nil ctx-id server-id flush-fn))
  ([owner ctx-id server-id flush-fn]
   (when-let [[_key ctx] (preferred-context-entry owner ctx-id :client)]
     (let [merged (ctx-proj/merge-store-projection ctx)]
       (when (status-valid-transition? (:status merged) STATUS-ALIVE)
         (run-context-status-command! merged STATUS-ALIVE nil)
         (update-transport-if-present!
           _key
           (fn [c]
             (let [buffer (:message-buffer c)
                   alive-transport (context-domain/transition-to-alive
                                     c server-id (System/currentTimeMillis))]
               (when (and flush-fn (seq buffer))
                 (doseq [msg buffer] (flush-fn msg)))
               alive-transport))))
       (get-context owner ctx-id)))))

(defn terminate-context!
  ([ctx-id send-terminated-fn]
   (terminate-context! nil ctx-id send-terminated-fn))
  ([owner ctx-id send-terminated-fn]
   (let [resolved-owner (or owner (context-owner))
         entry (preferred-context-entry resolved-owner ctx-id nil)]
     (when-let [[_key transport] entry]
       (let [merged (ctx-proj/merge-store-projection transport)]
         (when (not= (:status merged) STATUS-TERMINATED)
           (run-context-status-command! merged STATUS-TERMINATED :dispatcher-terminated)
           (let [now (System/currentTimeMillis)]
             (update-transport-if-present!
               _key
               (fn [c]
                 (schedule-deadline!
                   (context-domain/transition-to-terminated c now)
                   (+ now (long context-domain/terminated-context-grace-ms))))))
           (when send-terminated-fn
             (when-let [ctx-owner (owner/canonical-owner-from-transport transport)]
               (with-context-owner-fn ctx-owner
                 #(send-terminated-fn (:id merged)))))
           (log/debug "Context terminated:" (:id merged))))))))

(defn abort-all-contexts-for-player!
  ([player-uuid send-terminated-fn]
   (doseq [ctx (get-all-contexts-for-player player-uuid)]
     (terminate-context! (:id ctx) send-terminated-fn)))
  ([owner player-uuid send-terminated-fn]
   (doseq [ctx (get-all-contexts-for-player owner player-uuid)]
     (terminate-context! owner (:id ctx) send-terminated-fn))))

(defn- require-bound-owner!
  [owner ctx-id op]
  (or owner (context-owner)
      (throw (ex-info (str op " requires explicit owner or bound context-owner")
                      {:ctx-id ctx-id}))))

(defn ctx-buffer-or-send!
  ([ctx-id msg send-fn]
   (ctx-buffer-or-send! nil ctx-id nil msg send-fn))
  ([ctx-id preferred-side msg send-fn]
   (ctx-buffer-or-send! nil ctx-id preferred-side msg send-fn))
  ([owner ctx-id preferred-side msg send-fn]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-buffer-or-send!")]
     (let [ctx (second (preferred-context-entry owner* ctx-id preferred-side))]
       (when ctx
         (let [merged (ctx-proj/merge-store-projection ctx)]
           (case (:status merged)
             :constructed (when-let [[key _] (preferred-context-entry owner* ctx-id preferred-side)]
                            (update-transport-if-present! key update :message-buffer conj msg))
             :alive (when send-fn (send-fn msg))
             nil)))))))

(defn ctx-send-to-local!
  ([ctx-id channel msg]
   (ctx-send-to-local! nil ctx-id channel msg))
  ([owner ctx-id channel msg]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-send-to-local!")]
     (when-let [transport (second (preferred-context-entry owner* ctx-id nil))]
       (doseq [h (get-in transport [:listeners channel] [])]
         (try (h msg) (catch Exception e (log/warn "Listener threw" (ex-message e)))))))))

(defn register-route-fns! [{:keys [to-server to-client to-except-local] :as routes}]
  (if (and (nil? (:logical-side routes))
           (nil? (:side routes))
           (nil? (owner/store-session-id routes))
           (nil? to-server)
           (nil? to-client)
           (nil? to-except-local))
    (clear-route-fns!)
    (register-route-fns-entry!
      (route-owner-key routes)
      {:to-server to-server :to-client to-client :to-except-local to-except-local}))
  nil)

(defn ctx-send-to-server!
  ([ctx-id channel msg]
   (ctx-send-to-server! nil ctx-id channel msg))
  ([owner ctx-id channel msg]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-send-to-server!")]
     (ctx-buffer-or-send! owner* ctx-id :client {:channel channel :payload msg}
                          (fn [m] (when-let [ctx (second (preferred-context-entry owner* ctx-id :client))]
                                    (when-let [f (:to-server (route-fns-for-context ctx))]
                                      (f ctx-id (:channel m) (:payload m) ctx))))))))

(defn ctx-send-to-client!
  ([ctx-id channel msg]
   (ctx-send-to-client! nil ctx-id channel msg))
  ([owner ctx-id channel msg]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-send-to-client!")]
     (ctx-buffer-or-send! owner* ctx-id :server {:channel channel :payload msg}
                          (fn [m] (when-let [ctx (second (preferred-context-entry owner* ctx-id :server))]
                                    (when-let [f (:to-client (route-fns-for-context ctx))]
                                      (f ctx-id (:channel m) (:payload m) ctx))))))))

(defn ctx-send-to-except-local!
  ([ctx-id channel msg]
   (ctx-send-to-except-local! nil ctx-id channel msg))
  ([owner ctx-id channel msg]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-send-to-except-local!")]
     (ctx-buffer-or-send! owner* ctx-id :server {:channel channel :payload msg}
                          (fn [m] (when-let [ctx (second (preferred-context-entry owner* ctx-id :server))]
                                    (when-let [f (:to-except-local (route-fns-for-context ctx))]
                                      (f ctx-id (:channel m) (:payload m) ctx))))))))

(defn ctx-send-to-self! [ctx-id channel msg] (ctx-send-to-local! ctx-id channel msg))

(defn ctx-on!
  ([ctx-id channel handler-fn]
   (ctx-on! nil ctx-id channel handler-fn))
  ([owner ctx-id channel handler-fn]
   (let [owner* (require-bound-owner! owner ctx-id "ctx-on!")]
     (when-let [[key _ctx] (preferred-context-entry owner* ctx-id nil)]
       (update-transport-if-present! key update-in [:listeners channel] (fnil conj []) handler-fn)))))

(defn active-context? [ctx]
  (context-domain/active-context? ctx))

(defn active-contexts
  ([]
   (->> (transport-contexts-snapshot)
        (map (fn [[_key ctx]] (as-public-context ctx)))
        (filter active-context?)
        (map (fn [ctx] [(:id ctx) ctx]))
        (into {})))
  ([player-uuid]
   (->> (get-all-contexts-for-player player-uuid)
        (filter active-context?))))

(defn send-context-message!
  ([ctx-id channel payload]
   (ctx-send-to-local! ctx-id channel payload))
  ([ctx-id direction channel payload]
   (case direction
     :to-server (ctx-send-to-server! ctx-id channel payload)
     :to-client (ctx-send-to-client! ctx-id channel payload)
     :to-except-local (ctx-send-to-except-local! ctx-id channel payload)
     :to-self (ctx-send-to-self! ctx-id channel payload)
     (throw (ex-info "Unsupported context message direction"
                     {:ctx-id    ctx-id
                      :direction direction
                      :allowed   [:to-server :to-client :to-except-local :to-self]})))))

(defn dispatch-skill-event!
  ([event]
   (evt/fire-ability-event! event))
  ([skill-id callback-key event]
   (evt/fire-ability-event!
     {:skill-id     skill-id
      :callback-key callback-key
      :event        event})))

(defn call-with-dispatcher-runtime
  "Run `f` with an isolated dispatcher runtime."
  [runtime f]
  (binding [*dispatcher-runtime* runtime]
    (f)))

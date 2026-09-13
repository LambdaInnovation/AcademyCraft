(ns cn.li.ac.ability.client.effects.queue-infra
  "Shared queue helpers for client-side effect pipelines.

  Uses a client-thread-confined bounded ArrayDeque. Network callbacks enqueue
  onto Minecraft's client executor before reaching this layer."
  (:require [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner])
  (:import [java.util ArrayDeque]))

;; ============================================================================
;; Session-id resolution
;; ============================================================================

(defn- require-owner-value
  [kind owner label value]
  (if (some? value)
    value
    (throw (ex-info (format "%s queue requires %s" kind label)
                    {:owner owner
                     :required label}))))

(defn normalize-session-id
  "Resolve store partition key from a canonical client owner map or bare session token."
  [kind owner-or-session]
  (cond
    (map? owner-or-session)
    (or (owner/store-session-id owner-or-session)
        (require-owner-value kind owner-or-session
                             ":client-session-id"
                             (or (:client-session-id owner-or-session)
                                 (runtime-hooks/player-state-client-session-id)
                                 (runtime-hooks/client-session-id))))

    (some? owner-or-session)
    owner-or-session

    :else
    (require-owner-value kind nil
                         ":client-session-id"
                         (or (runtime-hooks/player-state-client-session-id)
                             (runtime-hooks/client-session-id)))))

(defn current-effect-owner
  [kind]
  (or (runtime-hooks/current-player-state-owner)
      (when (runtime-hooks/client-session-id)
        {:logical-side :client
         :client-session-id (runtime-hooks/client-session-id)})
      (throw (ex-info (format "Current %s effect owner requires :client-session-id" kind)
                      {:required ":client-session-id"}))))

;; ============================================================================
;; Bounded O(1) queue operations
;; ============================================================================

(defn queue-effect!
  "Enqueue an effect command tagged with session-id.
   Oldest low-priority entries are discarded at the fixed capacity."
  [^ArrayDeque q kind owner-or-session effect-cmd]
  (let [session-id (normalize-session-id kind owner-or-session)]
    (when (>= (.size q) (if (= kind "particle") 8192 1024))
      (.pollFirst q))
    (.addLast q [session-id effect-cmd]))
  nil)

(defn poll-effects!
  "Drain all queued effects for a session-id.
   Called from the client thread."
  [^ArrayDeque q kind owner-or-session]
  (let [session-id (normalize-session-id kind owner-or-session)
        results (java.util.ArrayList.)]
    ;; Drain the queue, collecting only entries for the requested session.
    ;; Unmatched entries are re-enqueued at the tail.
    (let [snapshot (.toArray q)]
      (.clear q)
      (doseq [^clojure.lang.PersistentVector entry snapshot]
        (let [[entry-session-id cmd] entry]
          (if (= session-id entry-session-id)
            (.add results cmd)
            (.addLast q entry)))))
    (vec results)))

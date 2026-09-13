(ns cn.li.ac.ability.service.context-skill-state
  "Shared skill-state read/write helpers for skill implementations.

  All writes delegate to skill-state-commands (reducer path only)."
  (:require [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.skill-state-commands :as ctx-cmd]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn current-owner
  []
  (or (ctx/context-owner)
      (runtime-hooks/current-player-state-owner)))

(defn get-context
  ([ctx-id]
   (get-context (current-owner) ctx-id))
  ([owner ctx-id]
   (if owner
     (ctx/get-context owner ctx-id)
     (ctx/get-context ctx-id))))

(defn active-server-skill-context
  "The alive SERVER context carrying this player's live state for `skill-id`.

  Only for callbacks whose arity carries no ctx-id — the :cost fns, which
  receive [player-id skill-id exp]. Anything holding a ctx-id must use
  `get-context` instead of scanning.

  The :logical-side and :status filters are load-bearing. Client and server
  share one dispatcher runtime in single player, so the same ctx-id is
  registered twice, and context-projection overlays :skill-state onto the
  server copy only — the client twin reports an empty skill-state forever.
  A scan that matched it silently froze charge counters and starved
  cost/gating logic of the state it was reading."
  [player-id skill-id]
  (->> (ctx/get-all-contexts)
       vals
       (filter (fn [ctx-data]
                 (and (= (str (:player-uuid ctx-data)) (str player-id))
                      (= skill-id (:skill-id ctx-data))
                      (= :server (:logical-side ctx-data))
                      (= ctx/STATUS-ALIVE (:status ctx-data)))))
       ;; If a stale duplicate survives briefly, prefer the longest charge.
       (sort-by (fn [ctx-data]
                  (long (or (get-in ctx-data [:skill-state :hold-ticks]) 0)))
                >)
       first))

(defn hold-ticks
  "Charge ticks tracked in this context's :skill-state, 0 when absent."
  [ctx-id]
  (long (or (get-in (get-context ctx-id) [:skill-state :hold-ticks]) 0)))

(defn hold-ticks-for-player
  "Charge ticks for a player's live server context of `skill-id`.
  See `active-server-skill-context` for why this must not be used when a
  ctx-id is available."
  [player-id skill-id]
  (long (or (get-in (active-server-skill-context player-id skill-id)
                    [:skill-state :hold-ticks])
            0)))

(defn assoc-skill-state!
  [ctx-id k v]
  (ctx-cmd/assoc-skill-state! (current-owner) ctx-id k v))

(defn update-skill-state-root!
  [ctx-id f & args]
  (let [ctx-data (or (get-context ctx-id) {})
        current (or (:skill-state ctx-data) {})
        next-state (if (and (= f identity) (= 1 (count args)))
                     (first args)
                     (apply f current args))]
    (ctx-cmd/replace-skill-state-root! (current-owner) ctx-id next-state)))

(defn clear-skill-state!
  [ctx-id]
  (ctx-cmd/clear-skill-state! (current-owner) ctx-id))

(defn replace-skill-state!
  "Replace the entire :skill-state map for ctx-id."
  [ctx-id state-map]
  (update-skill-state-root! ctx-id identity state-map))

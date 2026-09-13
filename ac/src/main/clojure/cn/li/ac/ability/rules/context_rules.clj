(ns cn.li.ac.ability.rules.context-rules
  "Pure business logic for context lifecycle state transitions.
  
  These are pure functions—no atoms, no event firing, no side effects.
  Callers (reducer layer) combine these with event generation.
  
  Context states: :constructed → :alive → :terminated
  
  Reference: cn.li.ac.ability.model.context for the data schema."
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.domain.skill :as skill-domain]
            [cn.li.ac.ability.registry.skill :as skill-registry]))

;; ============================================================================
;; Context Validation Rules
;; ============================================================================

(defn can-establish-skill-context?
  "Check if player can activate a skill context.
  
  Returns: bool.
  
  Guard conditions:
    - ability-data is present
    - resource-data is present
    - skill is learned
    - resource is usable (not in overload recovery, not interfered)
    - skill is controllable
  
  Args:
    ability-data   – AbilityData map
    resource-data  – ResourceData map
    skill-id       – keyword"
  [ability-data resource-data skill-id]
  (boolean
   (and ability-data
        resource-data
        (adata/is-learned? ability-data skill-id)
        (rdata/can-use-ability? resource-data)
        (skill-domain/controllable? (skill-registry/get-skill skill-id)))))

(defn can-maintain-context?
  "Check if context can continue running (per-tick keepalive).
  
  Returns: bool.
  
  Guard conditions:
    - lifetime has not exceeded max
    - no timeout condition met
  
  Args:
    elapsed-ticks – int, ticks since context was created
    max-lifetime  – int, max ticks allowed for this context"
  [elapsed-ticks max-lifetime]
  (< elapsed-ticks max-lifetime))

;; ============================================================================
;; State Transition (Pure)
;; ============================================================================

(defn transition-to-alive
  "State transition: :constructed → :alive.
  
  Returns: {:data updated-context :events-needed [...]}.
  
  Events needed:
    [:context-alive] – context has entered alive state
  
  Args:
    context – context map in :constructed state"
  [context]
  {:data (assoc context :status :alive :activated-at (System/currentTimeMillis))
   :events-needed [:context-alive]})

(defn transition-to-terminated
  "State transition: :alive → :terminated (or :constructed → :terminated).
  
  Returns: {:data updated-context :events-needed [...] :cleanup-tasks [...]}.
  
  Events needed:
    [:context-terminated]
  
  Cleanup tasks: items to be executed (e.g., [[:cancel-pending-effects ctx-id]...])
  
  Args:
    context       – context map
    reason        – keyword describing termination (:manual :timeout :rejected :error)"
  [context reason]
  (let [cleanup-tasks (case reason
                        :manual []
                        :timeout []
                        :rejected []
                        :error []
                        [])]
    {:data (assoc context :status :terminated :terminated-reason reason)
     :events-needed [:context-terminated]
     :cleanup-tasks cleanup-tasks}))

;; ============================================================================
;; Queries (Pure)
;; ============================================================================

(defn is-alive?
  "Check if context is in :alive state."
  [context]
  (= (:status context) :alive))

(defn is-constructed?
  "Check if context is in :constructed state."
  [context]
  (= (:status context) :constructed))

(defn is-terminated?
  "Check if context is in :terminated state."
  [context]
  (= (:status context) :terminated))

(defn get-context-age-ticks
  "Get how many ticks this context has been active.
  
  Args:
    context               – context map
    current-server-ticks  – int, current tick count"
  [context current-server-ticks]
  (if-let [activated-at (:activated-at context)]
    (- current-server-ticks activated-at)
    0))

(defn get-context-status
  "Get current status keyword."
  [context]
  (:status context))

(defn get-context-skill
  "Get skill-id from context."
  [context]
  (:skill-id context))

(defn get-context-player
  "Get player-uuid from context."
  [context]
  (:player-uuid context))

;; ============================================================================
;; Bulk Query Helpers
;; ============================================================================

(defn filter-active-contexts
  "Filter contexts to only alive ones for a specific player.
  
  Args:
    contexts     – [context-maps]
    player-uuid  – UUID"
  [contexts player-uuid]
  (->> contexts
       (filter (fn [ctx]
                 (and (= player-uuid (:player-uuid ctx))
                      (is-alive? ctx))))
       vec))

(defn count-active-contexts
  "Count alive contexts for a player."
  [contexts player-uuid]
  (count (filter-active-contexts contexts player-uuid)))

(defn filter-contexts-by-skill
  "Filter contexts to only those for a specific skill.
  
  Args:
    contexts – [context-maps]
    skill-id – keyword"
  [contexts skill-id]
  (->> contexts
       (filter (fn [ctx]
                 (and (= skill-id (:skill-id ctx))
                      (is-alive? ctx))))
       vec))

;; ============================================================================
;; Lifecycle Rules Summary
;; ============================================================================

;; Context lifetime:
;;   Phase 1 (client):  BEGIN-LINK sent to server
;;                      client waits for ESTABLISH or TERMINATE
;;
;;   Phase 2 (server):  BEGIN-LINK received
;;                      if can-establish? → create context, send ESTABLISH
;;                      else → send TERMINATE immediately
;;
;;   Phase 3 (alive):   ESTABLISH received by client
;;                      server periodically checks can-maintain-context?
;;                      if timeout/error → transition-to-terminated
;;
;;   Phase 4 (end):     TERMINATE sent to client
;;                      both sides clean up context data

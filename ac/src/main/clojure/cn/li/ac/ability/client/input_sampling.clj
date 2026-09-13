(ns cn.li.ac.ability.client.input-sampling
  "Pure pre-check predicates for client input handling.

  All functions take raw DATA maps (not player-uuid, not atoms).
  No side effects, no state reads. Safe to unit-test in isolation.

  Consumers (input_state_machine.clj, input_processor.clj) call these
  after reading player-state from atoms, passing the extracted domain maps."
  (:require [cn.li.ac.ability.util.resource-check :as resource-check]
            [cn.li.ac.ability.model.cooldown :as cd]))

;; ============================================================================
;; Resource predicates
;; ============================================================================

(defn activated?
  "True when the player has activated ability mode."
  [resource-data]
  (boolean (:activated resource-data)))

(defn can-use?
  "True when activated && overload-fine && no interference."
  [resource-data]
  (resource-check/can-use-resource-data? resource-data))

;; ============================================================================
;; Cooldown predicates
;; ============================================================================

(defn on-cooldown?
  "True when ctrl-id is in cooldown (checks :main sub-id)."
  [cooldown-data ctrl-id]
  (boolean (cd/in-cooldown? cooldown-data ctrl-id :main)))

;; ============================================================================
;; Combined pre-check
;; ============================================================================

(defn should-abort?
  "True when a skill key press/tick should be aborted (not usable or on cooldown)."
  [resource-data cooldown-data ctrl-id]
  (or (not (can-use? resource-data))
      (on-cooldown? cooldown-data ctrl-id)))

;; ============================================================================
;; Key transition
;; ============================================================================

(defn key-transition
  "Given the previous and current boolean state of a key, return the transition type.

  Returns:
    :press   — key just went down
    :tick    — key held down
    :release — key just went up
    :noop    — key is still up (no event)"
  [was-down is-down]
  (cond
    (and (not was-down) is-down)  :press
    (and was-down       is-down)  :tick
    (and was-down (not  is-down)) :release
    :else                         :noop))

(ns cn.li.ac.ability.client.input-state-machine
  "Pure state transition logic for client key input.

  All functions are pure: given a key-state map and player-state map they
  return either a description of what action to take, or a new key-state map.

  No atoms, no side effects, no dynamic vars — safe for unit tests and
  property-based tests.

  Key-state shape:
    {:skill-keys    [bool bool bool bool]
     :movement-keys {:forward bool :back bool :left bool :right bool}
     :gui-keys      {:skill-tree bool :preset-editor bool}}

  Action shapes returned:
    nil                                    — nothing to do
    {:transition :press/:tick/:release     — skill key
     :delegate   delegate-map}
    {:transition :abort
     :delegate   delegate-map}
    {:transition :press/:tick/:release     — movement key
     :movement-key keyword}
    {:transition :press                    — GUI key
     :gui-type   keyword}"
  (:require [cn.li.ac.ability.client.input-sampling :as sampling]
            [cn.li.mcmod.client.input-buttons :as input-buttons]))

;; ============================================================================
;; Shared button-state machine for short/long press detection
;; ============================================================================

(def initial-button-state input-buttons/initial-button-state)
(def handle-button-state! input-buttons/handle-button-state!)

;; ============================================================================
;; Default key-state constant (exported for consumers)
;; ============================================================================

(def default-key-state
  {:skill-keys    [false false false false]
   :movement-keys {:forward false :back false :left false :right false}
   :gui-keys      {:skill-tree false :preset-editor false}})

;; ============================================================================
;; Skill key transitions
;; ============================================================================

(defn compute-skill-key-event
  "Pure: compute the action for a skill key event.

  Args:
    key-state    — current key-state map for this owner
    player-state — full player state map {:resource-data ... :cooldown-data ...}
    key-idx      — 0-3
    is-down      — boolean (current raw key state)
    delegate     — delegate map {:skill-id :on-key-down :on-key-tick :on-key-up :on-key-abort}
                   (may be nil if no delegate is bound for this slot)

  Returns a map describing what to do, or nil if nothing."
  [key-state player-state key-idx is-down delegate]
  (let [was-down    (get-in key-state [:skill-keys key-idx] false)
        transition  (sampling/key-transition was-down is-down)]
    (when (and delegate (not= :noop transition))
      (let [res-data (or (:resource-data player-state) {})
            cd-data  (or (:cooldown-data player-state) {})
            ctrl-id  (or (:ctrl-id delegate) (:skill-id delegate))
            activated? (sampling/activated? res-data)]
        (cond
          ;; If release: always forward the release (clean up even if not activated)
          (= :release transition)
          {:transition :release :delegate delegate}

          ;; Not activated at all: treat as abort (guards lingering holds)
          (not activated?)
          {:transition :abort :delegate delegate}

          ;; Activated: check abort conditions
          (sampling/should-abort? res-data cd-data ctrl-id)
          {:transition :abort :delegate delegate}

          :else
          {:transition transition :delegate delegate})))))

(defn next-skill-key-state
  "Return new key-state with the skill key at key-idx updated."
  [key-state key-idx is-down]
  (assoc-in key-state [:skill-keys key-idx] is-down))

;; ============================================================================
;; Movement key transitions
;; ============================================================================

(defn compute-movement-key-event
  "Pure: compute the action for a movement key event.

  Returns {:transition :press/:tick/:release :movement-key kw} or nil."
  [key-state movement-key is-down]
  (let [was-down   (get-in key-state [:movement-keys movement-key] false)
        transition (sampling/key-transition was-down is-down)]
    (when (not= :noop transition)
      {:transition transition :movement-key movement-key})))

(defn next-movement-key-state
  "Return new key-state with the movement key updated."
  [key-state movement-key is-down]
  (assoc-in key-state [:movement-keys movement-key] is-down))

;; ============================================================================
;; GUI key transitions
;; ============================================================================

(defn compute-gui-key-event
  "Pure: compute the action for a GUI key event.
  GUI keys fire only on press (rising edge), not on hold or release.

  Returns {:transition :press :gui-type kw} or nil."
  [key-state gui-type is-down]
  (let [was-down (get-in key-state [:gui-keys gui-type] false)]
    (when (and (not was-down) is-down)
      {:transition :press :gui-type gui-type})))

(defn next-gui-key-state
  "Return new key-state with the GUI key updated."
  [key-state gui-type is-down]
  (assoc-in key-state [:gui-keys gui-type] is-down))

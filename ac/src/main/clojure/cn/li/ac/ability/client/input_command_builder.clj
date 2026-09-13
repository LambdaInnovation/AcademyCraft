(ns cn.li.ac.ability.client.input-command-builder
  "Pure command builders for client-originated ability requests.

  These functions take raw data values and return command maps that
  describe the intended network request. They have no side effects.

  Consumers (input_processor.clj) execute the commands via the API layer."
  )

;; ============================================================================
;; Activate toggle
;; ============================================================================

(defn toggle-activated-command
  "Return a command map to toggle the activated state.

  Args:
    current-activated — boolean, the player's current activation state

  Returns: {:command :set-activated :activated bool}"
  [current-activated]
  {:command :set-activated :activated (not (boolean current-activated))})

;; ============================================================================
;; Preset switch
;; ============================================================================

(defn preset-switch-command
  "Return a command map to switch to the next preset cyclically.

  Args:
    current-preset — int, current preset index (0-based)
    preset-count   — int, total number of presets

  Returns: {:command :switch-preset :preset-idx int}"
  [current-preset preset-count]
  {:command :switch-preset
   :preset-idx (mod (inc (int current-preset)) (int preset-count))})

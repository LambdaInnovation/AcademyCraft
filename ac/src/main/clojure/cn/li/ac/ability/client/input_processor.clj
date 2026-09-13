(ns cn.li.ac.ability.client.input-processor
  "Effectful input processor — the Imperative Shell for client key input.

  This namespace is the ONLY place that:
    1. Reads key-state atoms
    2. Calls state machine (pure) to compute what action to take
    3. Executes that action (calls delegate fns / API layer)
    4. Writes back updated key state

  Callers (keybinds.clj) delegate on-skill-key-event, on-movement-key-event,
  on-gui-key-event here after providing the player-state and runtime context.

  No business logic lives here — only wiring."
  (:require [cn.li.ac.ability.client.input-state-machine :as sm]
            [cn.li.ac.ability.client.api :as api]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.runtime :as runtime]))

;; ============================================================================
;; Skill key execution
;; ============================================================================

(defn execute-skill-key-event!
  "Execute the action described by a skill key event map.

  event shape: {:transition :press/:tick/:release/:abort :delegate delegate-map}
  player-uuid: string"
  [event player-uuid]
  (when (and event player-uuid)
    (let [{:keys [transition delegate]} event]
      (case transition
        :press   (when-let [f (:on-key-down  delegate)] (f player-uuid))
        :tick    (when-let [f (:on-key-tick  delegate)] (f player-uuid))
        :release (when-let [f (:on-key-up    delegate)] (f player-uuid))
        :abort   (when-let [f (:on-key-abort delegate)] (f player-uuid))
        nil)))
  nil)

;; ============================================================================
;; Movement key execution
;; ============================================================================

(defn execute-movement-key-event!
  "Execute the action described by a movement key event map.

  event shape: {:transition :press/:tick/:release :movement-key keyword}
  player-uuid: string"
  [event player-uuid]
  (when (and event player-uuid)
    (let [{:keys [transition movement-key]} event]
      (case transition
        :press   (runtime/on-movement-key-down! player-uuid movement-key)
        :tick    (runtime/on-movement-key-tick! player-uuid movement-key)
        :release (runtime/on-movement-key-up!   player-uuid movement-key)
        nil)))
  nil)

;; ============================================================================
;; GUI key execution
;; ============================================================================

(defn execute-gui-key-event!
  "Execute the action described by a GUI key event map.

  event shape: {:transition :press :gui-type :skill-tree/:preset-editor}
  player-uuid: string"
  [event player-uuid]
  (when (and event player-uuid)
    (let [gui-type (:gui-type event)]
      (case gui-type
        :skill-tree    (client-bridge/open-screen! :ac/skill-tree    {:player-uuid player-uuid})
        :preset-editor (client-bridge/open-screen! :ac/preset-editor {:player-uuid player-uuid})
        nil)))
  nil)

;; ============================================================================
;; Input command execution (activate toggle / preset switch)
;; ============================================================================
;; Note on responsibility split:
;; - Platform polling / Forge event handlers decide whether an input is a
;;   short press, a long hold, or a regular press/release transition.
;; - This processor only consumes the already-decided event and forwards it to
;;   AC business logic (keybinds / runtime / API). No timing policy should be
;;   added here.

(defn execute-input-command!
  "Execute an input command map (from input_command_builder.clj).

  Supported commands:
    {:command :set-activated :activated bool}
    {:command :switch-preset :preset-idx int}"
  [owner {:keys [command] :as cmd}]
  (case command
    :set-activated (api/req-set-activated! owner (:activated cmd) nil)
    :switch-preset (api/req-switch-preset!  owner (:preset-idx cmd) nil)
    nil))

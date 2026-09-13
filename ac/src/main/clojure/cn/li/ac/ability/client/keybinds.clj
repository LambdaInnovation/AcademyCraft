(ns cn.li.ac.ability.client.keybinds
  "Key binding state tracking and event handling (AC layer - no Minecraft imports).

  Provides:
  - Activate handler registry (V key stack)
  - Key group system with delegates
  - Pre-check logic (cooldown, resource) before dispatching"
  (:require 
            [cn.li.ac.ability.client.read-model :as read-model]
[cn.li.ac.ability.client.runtime :as runtime]
            [cn.li.ac.ability.client.api :as api]
            [cn.li.ac.ability.client.input-sampling :as sampling]
            [cn.li.ac.ability.client.input-state-machine :as sm]
            [cn.li.ac.ability.client.input-command-builder :as cmd-builder]
            [cn.li.ac.ability.client.input-processor :as processor]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.model.preset :as preset-data]
            [cn.li.ac.ability.registry.skill-query :as skill]
            [cn.li.ac.terminal.client.apps.freq-transmitter-reactive :as freq-transmitter]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.spi.vanilla-input-control :as vanilla-input]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap TreeMap Map$Entry]))

;; Dynamic var for getting player UUID (set by forge layer)
(def ^:dynamic *get-player-uuid-fn* nil)

(def ^:dynamic *client-session-id* nil)

;; ============================================================================
;; Activate Handler Registry (V key stack)
;; ============================================================================
;; Each handler: {:id kw :priority int :handles-fn (fn [uuid]) :on-key-down-fn (fn [uuid]) :hint-fn (fn [uuid])}
;; Higher priority wins. First handler where handles-fn returns true is active.

(defonce ^:private ^TreeMap activate-handlers (TreeMap.))
(defonce ^:private ^HashMap activate-handler-keys (HashMap.))
(defonce ^:private ^HashMap key-groups (HashMap.))
(defonce ^:private registry-frozen (boolean-array 1))

;; Keybind registry — Framework [:service :keybind-registry]

(defn- keybind-registry-state-snapshot []
  {:activate-handlers (into (sorted-map) activate-handlers)
   :key-groups (into {}
                     (map (fn [^Map$Entry entry]
                            [(.getKey entry) (into {} ^HashMap (.getValue entry))]))
                     (.entrySet key-groups))
   :frozen? (aget ^booleans registry-frozen 0)})

(defn- assert-keybind-registries-open!
  []
  (when (aget ^booleans registry-frozen 0)
    (throw (ex-info "Keybind registries are frozen" {}))))

(defn- find-activate-handler-entry
  [id]
  (when-let [key (.get activate-handler-keys id)]
    (clojure.lang.MapEntry/create key (.get activate-handlers key))))

(defn add-activate-handler!
  "Register an activate handler. Higher priority = checked first."
  [{:keys [id priority] :as handler}]
  (assert-keybind-registries-open!)
  (when-not (keyword? id)
    (throw (ex-info "Activate handler id must be a keyword" {:id id})))
  (let [priority (int (or priority 0))]
    (if-let [[[existing-priority _] _existing-handler] (find-activate-handler-entry id)]
      (when (not= existing-priority priority)
        (throw (ex-info "Conflicting activate handler id"
                        {:id id
                         :existing-priority existing-priority
                         :new-priority priority})))
      (let [key [priority id]]
        (.put activate-handler-keys id key)
        (.put activate-handlers key (assoc handler :priority priority)))))
  nil)

(defn remove-activate-handler!
  "Remove an activate handler by id."
  [id]
  (assert-keybind-registries-open!)
  (when-let [key (.remove activate-handler-keys id)]
    (.remove activate-handlers key))
  nil)

(defn get-active-handler
  "Walk handlers from highest priority down. Return first where handles-fn returns true."
  [player-uuid]
  (some (fn [handler]
          (when ((:handles-fn handler) player-uuid) handler))
        (.values (.descendingMap activate-handlers))))

(defn get-activate-hint
  "Get the hint string from the active handler, or nil."
  [player-uuid]
  (when-let [h (get-active-handler player-uuid)]
    (when-let [hint-fn (:hint-fn h)]
      (hint-fn player-uuid))))

;; ============================================================================
;; Key Group System
;; ============================================================================
;; Groups: {:default {key-idx delegate-map}, :custom-group {key-idx delegate-map}}
;; delegate-map: {:skill-id kw :on-key-down fn :on-key-tick fn :on-key-up fn :on-key-abort fn}

(defn register-key-delegate!
  "Register a delegate for a key index in a named group.
   Called during runtime syncs (update-default-group!) — must work after freeze."
  [group key-idx delegate-map]
  (let [^HashMap delegates (or (.get key-groups group)
                               (let [created (HashMap.)]
                                 (.put key-groups group created)
                                 created))]
    (.put delegates key-idx delegate-map))
  nil)

(defn clear-key-group!
  "Remove all delegates in a group.
   Called during runtime syncs (not registration time) — must work after freeze."
  [group]
  (.remove key-groups group)
  nil)

(defn clear-all-key-groups!
  "Remove all delegates from all groups.
   Called during runtime syncs (not registration time) — must work after freeze."
  []
  (.clear key-groups)
  nil)

(defn freeze-keybind-registries!
  []
  (aset-boolean ^booleans registry-frozen 0 true)
  nil)

(defn keybind-registries-snapshot
  []
  (keybind-registry-state-snapshot))

(defn reset-keybind-registries-for-test!
  []
  (.clear activate-handlers)
  (.clear activate-handler-keys)
  (.clear key-groups)
  (aset-boolean ^booleans registry-frozen 0 false)
  nil)

(declare get-client-player-state)

(defn get-delegate-for-key
  "Get the effective delegate for a key index. Custom groups override :default."
  [key-idx]
  ;; Walk non-default groups first (any custom override), then default
  (or (some (fn [^Map$Entry entry]
              (when-not (= :default (.getKey entry))
                (.get ^HashMap (.getValue entry) key-idx)))
            (.entrySet key-groups))
      (when-let [^HashMap defaults (.get key-groups :default)]
        (.get defaults key-idx))))

(defn- get-skill-id-for-slot
  "Get the skill-id bound to a slot from the current preset."
  [player-uuid key-idx]
  (when-let [state (get-client-player-state player-uuid)]
    (let [pd (:preset-data state)
          slots (preset-data/get-active-slots pd)]
      (when-let [slot (nth slots key-idx nil)]
        (when (and (vector? slot) (= 2 (count slot)))
          (let [[cat-id ctrl-id] slot]
            (when-let [s (skill/get-skill-by-controllable cat-id ctrl-id)]
              s)))))))

(defn get-skill-id-for-slot-public
  "Public accessor for skill-id at a slot index."
  [player-uuid key-idx]
  (get-skill-id-for-slot player-uuid key-idx))

(defn update-default-group!
  "Rebuild default key group from current preset. Called on preset switch/activate."
  [player-uuid]
  (clear-key-group! :default)
  (doseq [idx (range 4)]
    (when-let [skill-id (get-skill-id-for-slot player-uuid idx)]
      (register-key-delegate!
       :default idx
       {:skill-id  skill-id
        :on-key-down  (fn [uuid] (runtime/on-slot-key-down! uuid idx))
        :on-key-tick  (fn [uuid] (runtime/on-slot-key-tick! uuid idx))
        :on-key-up    (fn [uuid] (runtime/on-slot-key-up! uuid idx))
        :on-key-abort (fn [uuid] (runtime/on-slot-key-abort! uuid idx))}))))

;; State tracking for key transitions
(def ^:private default-key-state
  {:skill-keys [false false false false]
   :movement-keys {:forward false
                   :back false
                   :left false
                   :right false}
   :gui-keys {:skill-tree false
              :preset-editor false}})

(def ^:private previous-screen-open
  "Client-global edge latch for the Screen-open window (single local player —
  no per-owner concern, like v-toggle-state in glfw-polling-core)."
  (atom false))

;; Client keybind runtime — Framework [:service :client-keybinds]

(definterface IClientKeybindRuntime
  (^java.util.HashMap keyStates [])
  (^java.util.HashMap presetSwitchStates []))

(deftype ClientKeybindRuntime [^HashMap keys ^HashMap presets]
  IClientKeybindRuntime
  (keyStates [_] keys)
  (presetSwitchStates [_] presets))

(defn create-client-keybind-runtime []
  (ClientKeybindRuntime. (HashMap.) (HashMap.)))

(defonce ^:private client-keybind-runtime-slot
  (object-array [(create-client-keybind-runtime)]))

(defn- client-keybind-runtime ^ClientKeybindRuntime []
  (aget ^objects client-keybind-runtime-slot 0))

(defn call-with-client-keybind-runtime
  [runtime f]
  (let [previous (client-keybind-runtime)]
    (aset ^objects client-keybind-runtime-slot 0 runtime)
    (try (f) (finally (aset ^objects client-keybind-runtime-slot 0 previous)))))

(def ^:private default-preset-switch-state
  {:current-preset 0 :previous-preset 0 :show-until-ms 0})

(def ^:private PRESET-COUNT 4)
(def ^:private PRESET-INDICATOR-DURATION-MS 2000)

(defn- current-client-session-id
  []
  (or *client-session-id* (runtime-hooks/client-session-id)))

(defn- require-client-owner-value
  [owner label value]
  (if (some? value)
    value
    (throw (ex-info (format "Client keybind owner requires %s" label)
                    {:owner owner
                     :required label}))))

(defn client-owner-key
  [owner]
  (let [owner-map (cond
                    (vector? owner) owner
                    (map? owner) owner
                    (some? owner) {:player-uuid owner}
                    :else {})]
    (if (vector? owner-map)
      owner-map
      [(require-client-owner-value owner ":client-session-id"
                                   (or (:client-session-id owner-map)
                                       (current-client-session-id)))
       (require-client-owner-value owner ":player-uuid"
                                   (some-> (or (:player-uuid owner-map)
                                               (:uuid owner-map))
                           str))])))

(defn- get-client-player-state
  [player-uuid]
  (let [session-id (require-client-owner-value {:player-uuid player-uuid}
                                               ":client-session-id"
                                               (current-client-session-id))
        player-uuid* (require-client-owner-value {:player-uuid player-uuid}
                                                 ":player-uuid"
                                                 (some-> player-uuid str))]
    (read-model/get-player-state [session-id :keybinds player-uuid*])))

(defn key-state-snapshot
  ([]
  (into {} (.keyStates (client-keybind-runtime))))
  ([owner]
  (or (.get (.keyStates (client-keybind-runtime)) (client-owner-key owner)) default-key-state)))

(defn preset-switch-state-snapshot
  ([]
  (into {} (.presetSwitchStates (client-keybind-runtime))))
  ([owner]
  (or (.get (.presetSwitchStates (client-keybind-runtime)) (client-owner-key owner))
      default-preset-switch-state)))

(defn- swap-key-state!
  [owner f & args]
  (let [owner-key (client-owner-key owner)
        ^HashMap states (.keyStates (client-keybind-runtime))]
    (.put states owner-key
          (apply f (or (.get states owner-key) default-key-state) args))))

(defn- swap-preset-switch-state!
  [owner f & args]
  (let [owner-key (client-owner-key owner)
        ^HashMap states (.presetSwitchStates (client-keybind-runtime))]
    (.put states owner-key
          (apply f (or (.get states owner-key) default-preset-switch-state) args))))

(defn clear-client-keybind-state!
  [owner]
  (let [owner-key (client-owner-key owner)]
    (.remove (.keyStates (client-keybind-runtime)) owner-key)
    (.remove (.presetSwitchStates (client-keybind-runtime)) owner-key))
  nil)

(defn reset-client-keybind-state-for-test!
  []
  (.clear (.keyStates (client-keybind-runtime)))
  (.clear (.presetSwitchStates (client-keybind-runtime)))
  (reset! previous-screen-open false)
  nil)

(def ^:private movement-keys
  [:forward :back :left :right])

(defn- activated?
  [player-uuid]
  (boolean (get-in (get-client-player-state player-uuid) [:resource-data :activated])))

(defn- has-category?
  "Check if player has learned a category (original AcademyCraft: aData.hasCategory())."
  [player-uuid]
  (some? (get-in (get-client-player-state player-uuid) [:ability-data :category-id])))

(defn- get-client-player-uuid
  "Get current client player UUID. Must be provided by forge layer."
  []
  (when *get-player-uuid-fn*
    (*get-player-uuid-fn*)))

(defn- current-client-owner
  [player-uuid]
  {:client-session-id (require-client-owner-value {:player-uuid player-uuid}
                                                  ":client-session-id"
                                                  (current-client-session-id))
   :player-uuid player-uuid})

(defn- has-active-delegates?
  "Check if any skill delegate key is currently held (upstream
  ClientRuntime.hasActiveDelegate semantics).
  V-key short-press aborts only while a delegate key is down; otherwise it
  toggles activation even if stale/alive contexts still exist."
  [player-uuid]
  (let [owner (current-client-owner player-uuid)
        skill-keys (:skill-keys (key-state-snapshot owner))]
    (boolean (some true? skill-keys))))

(defn on-skill-key-event
  "Handle skill key state change. Uses delegate system with pre-checks.

  Physical key state is tracked every tick, Screen open or not, so a press
  that happens while a Screen is open is absorbed when the Screen closes —
  clicking a pause-menu button (which closes the menu on mouse-down while the
  button is still held) never re-activates a skill. Mirrors upstream
  ClientRuntime, which polls KeyManager.getKeyDown unconditionally and gates
  only event dispatch on ClientUtils.isPlayerInGame().

  screen-open? — a Screen is open: press/tick/release dispatch is suppressed,
  but the physical state is still recorded (no false rising edge later).
  just-opened?  — first tick of the suppressed window: a held delegate is
  aborted once, matching upstream `state.state && shouldAbort → onKeyAbort`.
  just-closed?  — first tick after the window: a key that is physically down
  is absorbed. The mouse press that clicks a menu button and the menu close
  both happen in the same frame's GLFW event poll, BEFORE this tick samples —
  the press was under the Screen but never got a suppressed tick, so without
  this the closing frame would emit a false :press."
  ([key-idx is-down]
   (on-skill-key-event key-idx is-down false false false))
  ([key-idx is-down screen-open? just-opened?]
   (on-skill-key-event key-idx is-down screen-open? just-opened? false))
  ([key-idx is-down screen-open? just-opened? just-closed?]
   (when-let [player-uuid (get-client-player-uuid)]
     (let [owner        (current-client-owner player-uuid)
           key-state    (key-state-snapshot owner)
           player-state (get-client-player-state player-uuid)
           was-down     (boolean (get-in key-state [:skill-keys key-idx] false))
           delegate     (get-delegate-for-key key-idx)
           event        (sm/compute-skill-key-event key-state player-state key-idx is-down delegate)]
       (cond
         ;; Screen opened while this skill key was held: abort the held
         ;; context once; the rest of the hold is absorbed by the
         ;; physical-state tracking below, so it cannot restart on close.
         (and just-opened? was-down delegate)
         (processor/execute-skill-key-event!
           {:transition :abort :delegate delegate} player-uuid)

         ;; Screen just closed with the key still physically down: the press
         ;; happened under the Screen (same-frame button click) — absorb it.
         (and just-closed? is-down)
         nil

         (not screen-open?)
         (processor/execute-skill-key-event! event player-uuid))
       (when (not= was-down (boolean is-down))
         (swap-key-state! owner sm/next-skill-key-state key-idx is-down))))))


(defn on-gui-key-event
  "Handle GUI key state change. Opens screens or toggles mode on key press.
  Presses while a Screen is open are suppressed but still tracked, so a key
  held across a Screen close never looks like a fresh press (and a key that
  is down on the first tick after a Screen closes is absorbed — see
  on-skill-key-event)."
  ([gui-type is-down]
   (on-gui-key-event gui-type is-down false false))
  ([gui-type is-down screen-open? just-closed?]
   (when-let [player-uuid (get-client-player-uuid)]
     (let [owner     (current-client-owner player-uuid)
           key-state (key-state-snapshot owner)
           event     (sm/compute-gui-key-event key-state gui-type is-down)]
       (when (and (not screen-open?) (not just-closed?))
         (processor/execute-gui-key-event! event player-uuid))
       (when (not= (boolean (get-in key-state [:gui-keys gui-type])) (boolean is-down))
         (swap-key-state! owner sm/next-gui-key-state gui-type is-down))))))

(defn on-movement-key-event
  "Handle movement key state transitions and forward them to runtime bridge.
  Transitions while a Screen is open are suppressed but still tracked (see
  on-skill-key-event); keys down on the first tick after a Screen closes are
  absorbed too."
  ([movement-key is-down]
   (on-movement-key-event movement-key is-down false false))
  ([movement-key is-down screen-open? just-closed?]
   (when-let [player-uuid (get-client-player-uuid)]
     (let [owner     (current-client-owner player-uuid)
           key-state (key-state-snapshot owner)
           event     (sm/compute-movement-key-event key-state movement-key is-down)]
       (when (and (not screen-open?) (not just-closed?))
         (processor/execute-movement-key-event! event player-uuid))
       (when (not= (boolean (get-in key-state [:movement-keys movement-key])) (boolean is-down))
         (swap-key-state! owner sm/next-movement-key-state movement-key is-down))))))

(defn trigger-mode-switch!
  "V key short-press handler. Delegates to the active activate handler.
  If no handler matches, falls through to default toggle.

  Mirrors original AcademyCraft ClientHandler + ClientRuntime.getActivateHandler():
   1. hasCategory? guard — only toggle if player has learned a category
   2. If has active delegates → abort them (keep activation state unchanged)
   3. Otherwise → toggle activation state"
  ([]
   (trigger-mode-switch! nil))
  ([player-uuid]
   (when-let [player-uuid (or player-uuid (get-client-player-uuid))]
     ;; hasCategory check matching original: aData.hasCategory()
     (if-not (has-category? player-uuid)
       (log/debug "[V-TRACE][AC][CLIENT][NO-CATEGORY]"
                 {:uuid (str player-uuid)
                  :session-id (current-client-session-id)
                  :state-keys (some-> (get-client-player-state player-uuid) keys vec)})
      ;; Determine whether abort handler will match BEFORE running the stack.
      ;; When has-active-delegates? is true, the priority-10 abort-delegates
       ;; handler will fire → aborts contexts WITHOUT toggling activation.
       ;; When false, the priority-0 default-toggle handler fires → toggles.
      (let [will-abort? (boolean (has-active-delegates? player-uuid))]
         (if-let [handler (get-active-handler player-uuid)]
           (do
             (log/debug "[V-TRACE][AC][CLIENT][HANDLER]"
                       {:handler-id (:id handler) :uuid (str player-uuid)})
             ((:on-key-down-fn handler) player-uuid)
             ;; After handler runs, update client overlay state for
             ;; immediate HUD feedback (no server round-trip delay).
             ;; - Abort: activation unchanged → leave overlay state alone
             ;; - Toggle: activation toggled → update overlay immediately
             (when-not will-abort?
               (let [state (get-client-player-state player-uuid)
                     current (boolean (get-in state [:resource-data :activated]))]
                 (runtime-hooks/set-client-overlay-activated! player-uuid (not current)))))
           ;; Fallback: no handler matched → build and execute toggle command
           (let [state   (get-client-player-state player-uuid)
                 current (boolean (get-in state [:resource-data :activated]))]
             (log/debug "[V-TRACE][AC][CLIENT][TOGGLE]"
                       {:uuid (str player-uuid) :current current :next (not current)})
             (processor/execute-input-command! (current-client-owner player-uuid)
                                               (cmd-builder/toggle-activated-command current))
             (runtime-hooks/set-client-overlay-activated! player-uuid (not current)))))))))

(def ^:private flashing-movement-key-codes
  "Upstream Flashing KEY_GROUP: the WASD sub-keys registered in
  localMakeAlive while the context is alive. rebuildOverrides pushes every
  delegate keyID into ControlOverrider while the ability is activated, so
  vanilla movement is suppressed for the whole active window — the sub-keys
  drive the flash preview instead of walking."
  [87 65 83 68])

(defn- flashing-active?
  [player-uuid]
  (boolean
   (some (fn [[_ ctx-data]]
           (and (= :flashing (:skill-id ctx-data))
                (= (str player-uuid) (:player-uuid ctx-data))
                (ctx/active-context? ctx-data)))
         (ctx/get-all-contexts))))

(defn vanilla-override-key-codes
  "AC key ids that must suppress matching vanilla KeyMappings this frame.

  Mirrors upstream ClientRuntime.rebuildOverrides + ControlOverrider:
  - ability mode off → no overrides
  - Frequency Transmitter pass-on → LMB/RMB owned by the overlay
  - ability mode on → slots that currently have a skill delegate, plus the
    flashing WASD sub-keys while a flashing context is alive"
  ([]
   (vanilla-override-key-codes (get-client-player-uuid)))
  ([player-uuid]
   (cond
     (nil? player-uuid) []
     (freq-transmitter/overlay-active? player-uuid) [-100 -99]
     (activated? player-uuid)
     (into (into []
                 (keep (fn [idx]
                         (when (get-delegate-for-key idx)
                           ;; Live KeyMapping binding in AC convention (mouse
                           ;; buttons -100+value) — the config value is only
                           ;; the initial seed and goes stale on rebind.
                           (client-bridge/call-adapter
                             :keybind-get-key-code
                             (keyword (str "ability-key-" idx)))))
                       (range 4)))
           (when (flashing-active? player-uuid)
             flashing-movement-key-codes))
     :else [])))

(defn sync-vanilla-input-overrides!
  "Apply ControlOverrider-equivalent suppression for this client tick.

  Called both at the START of the client tick (before vanilla handleKeybinds
  reads the KeyMappings — the only point that can stop skill-owned movement
  keys, since KeyboardHandler re-reads them from GLFW every tick) and at the
  END via tick-keys! (consumes attack/use clicks)."
  ([]
   (sync-vanilla-input-overrides! (get-client-player-uuid)))
  ([player-uuid]
   (vanilla-input/suppress-vanilla-inputs! (vanilla-override-key-codes player-uuid))))

(defn- screen-window-edges
  "Screen-open window edge flags for this tick, from the previous tick's state.

  Returns [just-opened? just-closed?]:
  - just-opened? — first tick a Screen is open: held skill delegates are
    aborted (upstream `state.state && shouldAbort → onKeyAbort`).
  - just-closed?  — first tick after a Screen closed: keys physically down are
    absorbed. A menu button closes the Screen on mouse-down inside the same
    frame's GLFW event poll, so the press never got a suppressed tick; without
    the just-closed absorb the closing frame would emit a false :press."
  [screen-open?]
  (let [prev @previous-screen-open
        screen-open? (boolean screen-open?)]
    (reset! previous-screen-open screen-open?)
    [(and screen-open? (not prev))
     (and (not screen-open?) prev)]))

(defn tick-keys!
  "Main tick function called by forge layer. key-state-fn returns boolean for
  each key.

  screen-open? — a Screen (pause menu, chat, inventory…) is open. The raw
  physical state is still polled and tracked every tick, but gameplay events
  are suppressed: upstream gates every dispatch on ClientUtils.isPlayerInGame()
  while KeyManager keeps tracking the physical key state, so a press that
  happened under the Screen is absorbed when it closes — clicking 'Back to
  Game' with LMB bound to a skill must not fire that skill."
  ([key-state-fn]
   (tick-keys! key-state-fn false))
  ([key-state-fn screen-open?]
   ;; Frequency Transmitter's pass-on stage owns the mouse buttons just like
   ;; upstream ControlOverrider; ordinary ability-slot input is suppressed.
   (let [[just-opened? just-closed?] (screen-window-edges screen-open?)
         player-uuid (get-client-player-uuid)]
     ;; Suppress vanilla attack/use first so handleKeybinds on the *next* frame
     ;; does not keep swinging while a skill owns LMB/RMB (upstream ControlOverrider).
     (sync-vanilla-input-overrides! player-uuid)
     (if player-uuid
       (if (freq-transmitter/overlay-active? player-uuid)
         (freq-transmitter/tick-overlay-input!
           player-uuid
           (key-state-fn [:slot 1]))
         (doseq [idx (range 4)]
           (on-skill-key-event
             idx
             (key-state-fn [:slot idx])
             screen-open?
             just-opened?
             just-closed?)))
       (doseq [idx (range 4)]
         (on-skill-key-event
           idx
           (key-state-fn [:slot idx])
           screen-open?
           just-opened?
           just-closed?)))

     ;; Poll movement keys (W/A/S/D)
     (doseq [movement-key movement-keys]
       (on-movement-key-event movement-key (key-state-fn [:movement movement-key]) screen-open? just-closed?))

     ;; Poll GUI keys. :primary = N (edit-preset) — see screen-glfw-keys in the
     ;; platform key-state-fn; the CURRENT binding (Settings app / config)
     ;; resolves through the bound-key resolver. Upstream AcademyCraft:
     ;; KEY_EDIT_PRESET = N (ClientHandler.java) — preset-editor must be on
     ;; :primary/N to match. The skill-tree viewer is debug-only and has NO key
     ;; binding (upstream reaches it only via the terminal app).
     (on-gui-key-event :preset-editor (key-state-fn [:screen :primary]) screen-open? just-closed?))))

(defn reset-all-keys!
  "Reset all key states. Called on disconnect or dimension change."
  ([]
   (reset-client-keybind-state-for-test!)
   (clear-all-key-groups!))
  ([owner]
   (clear-client-keybind-state! owner)))

;; ============================================================================
;; Preset Switch
;; ============================================================================

(defn switch-preset!
  "Cycle to next preset. Called by platform key handler."
  ([]
   (when-let [player-uuid (get-client-player-uuid)]
     (switch-preset! player-uuid)))
  ([player-uuid]
   (when (activated? player-uuid)
     (let [owner         (current-client-owner player-uuid)
           current-state (preset-switch-state-snapshot owner)
           switch-cmd    (cmd-builder/preset-switch-command
                           (:current-preset current-state) PRESET-COUNT)]
       (swap-preset-switch-state! owner assoc
                                  :previous-preset (:current-preset current-state)
                                  :current-preset (:preset-idx switch-cmd)
                                  :show-until-ms (+ (client-bridge/game-time-ms)
                                                    PRESET-INDICATOR-DURATION-MS))
       (processor/execute-input-command! owner switch-cmd)
       (update-default-group! player-uuid)
       (log/debug "[PRESET-SWITCH]" {:preset (:preset-idx switch-cmd)})))))

(defn get-preset-switch-state
  "Get current preset switch state for HUD rendering."
  ([]
   (if-let [player-uuid (get-client-player-uuid)]
     (get-preset-switch-state player-uuid)
     default-preset-switch-state))
  ([owner]
   (preset-switch-state-snapshot owner)))

;; ============================================================================
;; Default Activate Handlers
;; ============================================================================

(defn install-default-handlers!
  "Register default activate handlers. Call once at init."
  []
  ;; Priority 0: Default toggle (lowest priority)
  (add-activate-handler!
   {:id              :default-toggle
    :priority        0
    :handles-fn      (fn [_uuid] true)
    :on-key-down-fn  (fn [uuid]
               (let [state   (get-client-player-state uuid)
                             current (boolean (get-in state [:resource-data :activated]))]
                         (api/req-set-activated! (current-client-owner uuid) (not current) nil)))
    :hint-fn         (fn [uuid]
                       (if (activated? uuid)
                         "ac.activate.hint.deactivate"
                         "ac.activate.hint.activate"))})

  ;; Priority 10: Abort active contexts (higher priority than toggle)
  (add-activate-handler!
   {:id              :abort-delegates
    :priority        10
    :handles-fn      (fn [uuid] (boolean (has-active-delegates? uuid)))
    :on-key-down-fn  (fn [uuid]
                       (runtime-hooks/client-abort-all!)
                       (log/debug "[V-TRACE] Aborted all contexts for" uuid))
    :hint-fn         (fn [_uuid] "ac.activate.hint.abort")}))

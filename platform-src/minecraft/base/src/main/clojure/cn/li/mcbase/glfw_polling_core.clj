(ns cn.li.mcbase.glfw-polling-core
  "Shared GLFW key polling implementation for all platforms.

   Eliminates 95% code duplication between Forge and Fabric.
   Both platforms use the same GLFW_KEY_* constants and polling.

   Usage by platform:
   - Forge: call poll-all-inputs! in client tick event (AFTER event parsing)
   - Fabric: call poll-all-inputs! in client tick event (primary input mechanism)"
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.keyboard-input :as kb-proto]
            [cn.li.mcmod.spi.key-scheme-provider :as key-provider]
            [cn.li.platform.neutral.client-render :as input-buttons])
  (:import [cn.li.mcver McClientAccess]
           [net.minecraft.client Minecraft]
           [org.lwjgl.glfw GLFW GLFWScrollCallback GLFWScrollCallbackI]))

;; GLFW key codes (shared constants)
(def GLFW_KEY_C 67)
(def GLFW_KEY_V 86)
(def GLFW_KEY_F4 293)
(def GLFW_KEY_LEFT_ALT 342)

(defn- settings-key-id
  "Translate the original Settings/KeyManager integer convention to the
   neutral provider's keyboard-or-mouse key identity."
  [key-code]
  (case (int key-code)
    -100 :mouse-left
    -99 :mouse-right
    key-code))

;; ============================================================================
;; Bound-key resolver — lets polling follow the CURRENT binding instead of a
;; hardcoded GLFW code, so Settings app rebinds (and vanilla Options > Controls
;; rebinds of AC KeyMappings) actually take effect on polling platforms (Fabric;
;; Forge slot/screen keys). Platform loaders install the resolver: [:bridge id]
;; reads the KeyMapping's current key, [:slot n] / [:screen kw] read the AC
;; gameplay config.
;; ============================================================================

(defonce ^:private bound-key-resolver (atom nil))

(defn install-bound-key-resolver!
  "Install (fn [input-ref] -> GLFW key-code | nil) resolving the current
   binding for [:bridge input-id] / [:slot idx] / [:screen kw] inputs."
  [f]
  (reset! bound-key-resolver f)
  nil)

(defn- resolve-bound-key-code
  "Current bound GLFW key-code for an input-ref, or nil when no resolver is
   installed or the binding is unknown/unbound."
  [input-ref]
  (when-let [f @bound-key-resolver]
    (try
      (f input-ref)
      (catch Throwable _
        nil))))

(defn ^:private is-key-pressed?
  "Query key state through installed KeySchemeProvider SPI."
  [scheme-name key-code]
  (boolean (key-provider/query-key-down? scheme-name key-code)))

(defn ^:private should-trigger?
  "Debounce logic: only trigger once per actual key press.

   Prevents rapid-fire by tracking transition from :released to :pressed."
  [_input-id was-pressed now-pressed]
  (and now-pressed (not was-pressed)))

(def ^:private last-poll-time
  "Client-process-global debounce state (single local player per client
   process — no multiplayer owner-key concern on this side)."
  (atom {}))

(def ^:private v-toggle-threshold-ns (* 300 1000 1000))

(def ^:private v-toggle-state
  (atom {:was-down false :down-at-ns nil}))

(defn handle-v-toggle-input!
  [state-atom is-down {:keys [player-uuid client-session-id suppress-triggers?
                               emit-fn now-ns short-press-threshold-ns]
                        :or {now-ns (System/nanoTime)
                             short-press-threshold-ns v-toggle-threshold-ns}}]
  (input-buttons/handle-button-state!
    state-atom is-down
    {:now-ns now-ns
     :short-press-threshold-ns short-press-threshold-ns
     :screen-open? (boolean suppress-triggers?)
     :on-short-up (fn []
                    (when emit-fn
                      (emit-fn :content/toggle-primary-state
                               {:player-uuid player-uuid
                                :client-session-id client-session-id
                                :logical-side :client})))}))

(defn poll-all-inputs!
  "Poll hardcoded key inputs and emit keyboard events.

   Called once per tick by Forge/Fabric platform layer.

   For Forge: supplements the KeyMapping event system (handles edge cases).
   For Fabric: the PRIMARY input mechanism (Fabric has no KeyMapping events).

   Polling targets:
   - :content/cycle-selection (C key — upstream KEY_SWITCH_PRESET)
   - :content/toggle-primary-state (V key — upstream KEY_ACTIVATE_ABILITY)
   - :content/toggle-debug-overlay (F4 key — upstream DebugConsole KEY_F4)

   Flow:
   1. Poll GLFW state for each input
   2. Detect press transition (released → pressed)
   3. Emit keyboard-input event via protocol

   Context provided to handlers:
   {:player-uuid 'current-player-uuid'
    :client-session-id 'session-id'
    :logical-side :client}

   opts:
   - :suppress-triggers? — true while a Screen (chat/GUI) is open. Physical key
     state is still recorded into the debounce map (so closing a screen with a
     key still held does NOT fire a stale released→pressed transition), but no
     input events are emitted — matching vanilla KeyMapping screen suppression."
  ([_minecraft-client player-uuid client-session-id]
   (poll-all-inputs! _minecraft-client player-uuid client-session-id nil))
  ([_minecraft-client player-uuid client-session-id {:keys [suppress-triggers?]}]
   (try
     ;; Cycle selection / switch preset (C key — upstream KEY_SWITCH_PRESET).
     ;; Poll the CURRENT KeyMapping binding, so Settings / Options > Controls
     ;; rebinds take effect here.
     (let [key-code (settings-key-id (or (resolve-bound-key-code [:bridge :content/cycle-selection])
                                             GLFW_KEY_C))
           is-pressed (is-key-pressed? :original key-code)
           was-pressed (get @last-poll-time :cycle-selection false)]
       (when (and (not suppress-triggers?)
                  (should-trigger? :cycle-selection was-pressed is-pressed))
         (kb-proto/emit-keyboard-input!
           :content/cycle-selection
           {:player-uuid player-uuid
            :client-session-id client-session-id
            :logical-side :client}))
       (swap! last-poll-time assoc :cycle-selection is-pressed))

     ;; Toggle primary state (V key — mode switch, upstream KEY_ACTIVATE_ABILITY)
     ;; This is the platform-side timing boundary for the client input chain.
     ;; Only a short release emits the toggle event; a long hold is suppressed
     ;; so the AC business layer never sees a toggle request for that gesture.
     (let [key-code (settings-key-id (or (resolve-bound-key-code [:bridge :content/toggle-primary-state])
                                             GLFW_KEY_V))
           is-pressed (is-key-pressed? :original key-code)]
       (handle-v-toggle-input! v-toggle-state is-pressed
         {:player-uuid player-uuid
          :client-session-id client-session-id
          :suppress-triggers? suppress-triggers?
          :emit-fn (fn [input-id context]
                     (kb-proto/emit-keyboard-input! input-id context))
          :now-ns (System/nanoTime)}))

     ;; Toggle debug overlay (F4 key — upstream DebugConsole KEY_F4, cycles
     ;; none -> normal -> show-exp -> none on key-down, fires once per press).
     (let [key-code (settings-key-id (or (resolve-bound-key-code [:bridge :content/toggle-debug-overlay])
                                             GLFW_KEY_F4))
           is-pressed (is-key-pressed? :original key-code)
           was-pressed (get @last-poll-time :toggle-debug-overlay false)]
       (when (and (not suppress-triggers?)
                  (should-trigger? :toggle-debug-overlay was-pressed is-pressed))
         (kb-proto/emit-keyboard-input!
           :content/toggle-debug-overlay
           {:player-uuid player-uuid
            :client-session-id client-session-id
            :logical-side :client}))
       (swap! last-poll-time assoc :toggle-debug-overlay is-pressed))

     ;; Toggle terminal (Left Alt) — Fabric's only dispatch path for this input
     ;; (Forge covers it via the :alternative-scheme KeyMapping event and does
     ;; not call poll-all-inputs!, so there is no double-fire).
     (let [key-code (settings-key-id (or (resolve-bound-key-code [:bridge :content/toggle-terminal])
                                             GLFW_KEY_LEFT_ALT))
           is-pressed (is-key-pressed? :original key-code)
           was-pressed (get @last-poll-time :toggle-terminal false)]
       (when (and (not suppress-triggers?)
                  (should-trigger? :toggle-terminal was-pressed is-pressed))
         (kb-proto/emit-keyboard-input!
           :content/toggle-terminal
           {:player-uuid player-uuid
            :client-session-id client-session-id
            :logical-side :client}))
       (swap! last-poll-time assoc :toggle-terminal is-pressed))

     nil
     (catch Exception e
       (log/warn e "Error polling GLFW inputs")))))

(defn reset-polling-state!
  "Reset debounce state (for testing or platform restart)"
  []
  (reset! last-poll-time {})
  (reset! v-toggle-state {:was-down false :down-at-ns nil})
  nil)

(defn install-scroll-callback!
  "Install a mouse-wheel scroll callback that CHAINS the previous GLFW
  callback — vanilla MouseHandler's onScroll (GUI/hotbar scrolling must keep
  working) — then hands the raw delta (yoffset, ~1.0 per notch, the same
  units as Forge/NeoForge InputEvent$MouseScrollingEvent.getDeltaY) to
  `on-scroll`.

  Used by Fabric loaders (Fabric API has no scroll event); Forge/NeoForge
  receive the same input from InputEvent$MouseScrollingEvent. The wheel
  feeds upstream PenetrateTeleport's distance control."
  [on-scroll]
  (let [window (try (.getWindow ^Minecraft (Minecraft/getInstance))
                    (catch Throwable _ nil))]
    (when window
      (let [handle (long (McClientAccess/windowHandle window))]
        (when-not (zero? handle)
          (let [prev (atom nil)]
            (reset! prev (GLFW/glfwSetScrollCallback
                          handle
                          (reify GLFWScrollCallbackI
                            (invoke [_ w xoffset yoffset]
                              ;; When the penetrate distance control consumed
                              ;; the wheel, skip vanilla's callback so the
                              ;; hotbar does not switch items.
                              (when-not (on-scroll (double yoffset))
                                (when-let [p @prev]
                                  (.invoke ^GLFWScrollCallback p w xoffset yoffset)))))))))))))

;; ============================================================================
;; Per-frame held-key state — shared by Forge (runtime_bridge.clj) and Fabric
;; (keyboard_init.clj) key-state-fn callbacks for keybinds/tick-keys!. Was
;; previously duplicated verbatim in both loader packages; extracted here so
;; a key remap (e.g. the ability-slot/preset-editor/skill-tree key alignment
;; pass) only needs to change one place.
;; ============================================================================

;; Upstream AcademyCraft ClientHandler.keyIDsInit default: MOUSE_LEFT,
;; MOUSE_RIGHT, R, F → ability slots 0-3.
(def slot-glfw-keys [:mouse-left :mouse-right 82 70])
(def movement-glfw-keys {:forward 87   ;; W
                         :back 83      ;; S
                         :left 65      ;; A
                         :right 68})   ;; D
;; :primary = N (78) — raw key identity only; which screen it opens is
;; decided in keybinds.clj/tick-keys!, matching upstream AcademyCraft's
;; KEY_EDIT_PRESET = N (ClientHandler.java). The skill-tree viewer is
;; debug-only with NO key binding (upstream reaches it only via the terminal
;; app).
(def screen-glfw-keys {:primary 78})

(defn glfw-key-state-fn
  "key-state-fn callback for keybinds/tick-keys!. Takes [:slot idx],
   [:movement kw], [:screen kw], or [:raw key-code] and returns boolean key/mouse-button state
   from GLFW (via the installed KeySchemeProvider SPI).

   Slot and screen keys resolve the CURRENT binding (Settings app / config),
   so a rebind takes effect immediately; :movement is fixed (WASD) and :raw
   queries the literal key-code."
  [[kind sub-key]]
  (let [key-code (case kind
                   ;; Config stores mouse buttons as -100/-99 (Settings app
                   ;; convention) — normalize through settings-key-id so the
                   ;; provider sees :mouse-left/:mouse-right keywords.
                   :slot (let [code (or (resolve-bound-key-code [:slot sub-key])
                                        (nth slot-glfw-keys sub-key nil))]
                           (if (number? code) (settings-key-id code) code))
                   :movement (get movement-glfw-keys sub-key)
                   :screen (let [code (or (resolve-bound-key-code [:screen sub-key])
                                          (get screen-glfw-keys sub-key))]
                             (if (number? code) (settings-key-id code) code))
                   :raw (settings-key-id sub-key)
                   nil)]
    (when key-code
      (try
        (key-provider/query-key-down? :original key-code)
        (catch Throwable _ false)))))

;; NOTE: while a Screen is open, glfw-key-state-fn still reports the RAW
;; physical state — the loaders pass screen-open? to client-tick-keys!, which
;; suppresses event dispatch (upstream KeyManager tracks physical state every
;; tick and ClientRuntime gates dispatch on ClientUtils.isPlayerInGame()).

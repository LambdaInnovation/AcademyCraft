(ns cn.li.neoforge1211.client.keyboard-event-handler
  "Forge InputEvent$Key handler - routes to AC keybinding system.

   Purpose: Forge-specific event handling that abstracts platform differences.
   Routes Forge keyboard events to the universal mcmod protocol."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.keyboard-input :as kb-proto]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.mcbase.glfw-polling-core :as glfw-polling]
            [cn.li.platform.neutral.client-render :as input-buttons]
            [cn.li.neoforge1211.client.key-mapping-adapter :as key-mapping-adapter])
  (:import [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post ClientTickEvent$Pre]
           [net.neoforged.neoforge.client.event InputEvent$Key]
           [net.minecraft.client Minecraft]
           [net.minecraft.client KeyMapping]))

(def ^:private v-toggle-state-atom nil)

;; ===== Forge Event Handler Registration =====

(defn- get-current-player-uuid
  "Get the current player's UUID from client session"
  []
  (try
    (if-let [player (.player (Minecraft/getInstance))]
      (str (.getUUID player))
      nil)
    (catch Exception _
      nil)))

(defn- get-client-session-id
  "Get the session ID for the current client"
  []
  (try
    (client-session/client-session-id)
    (catch Exception _
      (java.util.UUID/randomUUID))))

(defn ^:private on-key-input
  "Handle Forge InputEvent$Key - called for each key press/release.

   This is where platform-specific event flow meets universal protocol.
   Forge events only cover :alternative scheme (KeyMappings).
   For :original scheme (hardcoded keys), see glfw_polling_core.

   V key (toggle-primary-state) uses release-based timing: the toggle fires
   on RELEASE only when held < 300ms —matching upstream AcademyCraft
   ClientHandler. This is the platform-side timing gate for the client input
   chain; the AC business layer only sees a filtered input event after the
   timing decision has been made."
  [^InputEvent$Key event]
  (try
    (let [player-uuid (get-current-player-uuid)
          session-id (get-client-session-id)
          context {:player-uuid player-uuid
                   :client-session-id session-id
                   :logical-side :client}
          ;; Resolve the CURRENT bound key for toggle-primary-state from the
          ;; live KeyMapping rather than a hardcoded GLFW code —otherwise
          ;; rebinding this key (Settings app "keys" category, or vanilla
          ;; Options > Controls) would silently stop the toggle from firing
          ;; on either the old or the new key.
          ^KeyMapping v-mapping (key-mapping-adapter/get-key-mapping :content/toggle-primary-state)
          v-key-code (when v-mapping (.getValue (.getKey v-mapping)))]
      ;; Track V-key transitions and emit only on short release.
      (when (and v-key-code (= (int v-key-code) (.getKey event)))
        (glfw-polling/handle-v-toggle-input! v-toggle-state-atom
          (= 1 (.getAction event))
          {:player-uuid player-uuid
           :client-session-id session-id
           :suppress-triggers? false
           :emit-fn (fn [input-id ctx]
                      (kb-proto/emit-keyboard-input! input-id ctx))
           :now-ns (System/nanoTime)}))
      ;; Dispatch other consumed Forge KeyMappings from AC :alternative scheme
      ;; (skip toggle-primary-state —handled above with release-based timing).
      (doseq [[input-id ^KeyMapping key-mapping] (key-mapping-adapter/get-key-mappings-by-input-id)]
        (when (and (not= input-id :content/toggle-primary-state)
                   (.consumeClick ^KeyMapping key-mapping))
          (kb-proto/emit-keyboard-input! input-id context))))
    (catch Exception e
      (log/warn e "Error in Forge key input handler"))))

(defn ^:private on-client-tick
  "Handle Forge client tick end.
   NOTE: Forge KeyMapping events (InputEvent$Key) handle all :alternative scheme
   keys (Z, X, R, V, Left Alt). GLFW polling in this handler is reserved for
   future :original scheme keys (LMB/RMB) that have no KeyMapping event source.
   Currently, no :original scheme keys are registered, so poll-all-inputs! is skipped
   to avoid double-firing with KeyMapping events."
  [^ClientTickEvent$Post event]
  (try
    ;; Reserved for future :original scheme key polling via glfw-polling/poll-all-inputs!
    nil
    (catch Exception e
      (log/warn e "Error in Forge client tick keyboard polling"))))

(defn ^:private on-client-tick-pre
  "NeoForge ClientTickEvent$Pre fires before vanilla handleKeybinds reads the
   KeyMappings — the only point that can suppress skill-owned movement keys
   (flashing's WASD sub-keys); KeyboardHandler re-reads them from GLFW every
   tick, so the Post-phase setDown would be clobbered."
  [^ClientTickEvent$Pre _event]
  (try
    (client-session/with-current-client-session
      #(power-runtime/client-tick-start! get-current-player-uuid))
    (catch Exception e
      (log/warn e "Error in client tick pre keyboard suppression"))))

(defn install-forge-event-handler!
  "Register the Forge InputEvent$Key listener.
   
   Called during platform initialization after:
   1. SPI providers installed
   2. AC keybindings bootstrapped
   3. KeyMappings registered
   
   This connects Forge events to the universal protocol."
  []
  (try
    (install/process-once! ::event-handler-installed
      #(do
         ;; The client-render provider is installed before listener
         ;; registration, but not while this namespace is AOT-loaded.  Cache
         ;; its initial state now so the key-event hot path only uses this atom.
         (alter-var-root #'v-toggle-state-atom
                         (constantly (atom (input-buttons/initial-button-state))))
         (.addListener NeoForge/EVENT_BUS
                       EventPriority/NORMAL
                       false
                       InputEvent$Key
                       (reify java.util.function.Consumer
                         (accept [_ evt]
                           (on-key-input evt))))

         (.addListener NeoForge/EVENT_BUS
                       EventPriority/NORMAL
                       false
                       ClientTickEvent$Pre
                       (reify java.util.function.Consumer
                         (accept [_ evt]
                           (on-client-tick-pre evt))))

         (.addListener NeoForge/EVENT_BUS
                       EventPriority/NORMAL
                       false
                       ClientTickEvent$Post
                       (reify java.util.function.Consumer
                         (accept [_ evt]
                           (on-client-tick evt))))

         (log/debug "Forge keyboard event handler installed")))

    (catch Exception e
      (log/stacktrace "Failed to install Forge keyboard event handler" e))))

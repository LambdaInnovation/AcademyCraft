(ns cn.li.neoforge262.client.keyboard-event-handler
  "Forge InputEvent$Key handler - routes to AC keybinding system.

   Purpose: Forge-specific event handling that abstracts platform differences.
   Routes Forge keyboard events to the universal mcmod protocol."
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.platform.neutral.keyboard-input :as kb-proto]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.mcbase.glfw-polling-core :as glfw-polling]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.platform.neutral.client-render :as input-buttons]
            [cn.li.neoforge262.client.key-mapping-adapter :as key-mapping-adapter])
  (:import [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.bus.api EventPriority]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post ClientTickEvent$Pre]
           [net.neoforged.neoforge.client.event InputEvent$Key]
           [net.neoforged.neoforge.client.event InputEvent$MouseScrollingEvent]
           [net.minecraft.client Minecraft]
           [net.minecraft.client KeyMapping]))

(def ^:private v-toggle-state-atom nil)

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
  "Handle Forge InputEvent$Key - called for each key press/release."
  [^InputEvent$Key event]
  (try
    (let [player-uuid (get-current-player-uuid)
          session-id (get-client-session-id)
          context {:player-uuid player-uuid
                   :client-session-id session-id
                   :logical-side :client}
          ^KeyMapping v-mapping (key-mapping-adapter/get-key-mapping :content/toggle-primary-state)
          v-key-code (when v-mapping (.getValue (.getKey v-mapping)))]
      (when (and v-key-code (= (int v-key-code) (.getKey event)))
        (glfw-polling/handle-v-toggle-input! v-toggle-state-atom
          (= 1 (.getAction event))
          {:player-uuid player-uuid
           :client-session-id session-id
           :suppress-triggers? false
           :emit-fn (fn [input-id ctx]
                      (kb-proto/emit-keyboard-input! input-id ctx))
           :now-ns (System/nanoTime)}))
      (doseq [[input-id ^KeyMapping key-mapping] (key-mapping-adapter/get-key-mappings-by-input-id)]
        (when (and (not= input-id :content/toggle-primary-state)
                   (.consumeClick ^KeyMapping key-mapping))
          (kb-proto/emit-keyboard-input! input-id context))))
    (catch Exception e
      (log/warn e "Error in Forge key input handler"))))

(defn ^:private on-client-tick
  [^ClientTickEvent$Post event]
  (try
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

(defn ^:private on-mouse-scroll
  [^InputEvent$MouseScrollingEvent evt]
  ;; Upstream PenetrateTeleport onPlayerUseWheel: mouse wheel adjusts the
  ;; teleport distance (raw GLFW delta, ~1.0 per notch — NeoForge names it
  ;; getScrollDeltaY). The hook resolves the active penetrate context
  ;; itself; when it consumed the wheel, cancel the event so the vanilla
  ;; hotbar does not switch items.
  (try
    (when-let [player-uuid (get-current-player-uuid)]
      (client-session/with-current-client-session
        #(when (power-runtime/client-on-slot-wheel! player-uuid 0 (.getScrollDeltaY evt))
           (.setCanceled evt true))))
    (catch Exception e
      (log/warn e "Error in Forge mouse scroll handler"))))

(defn install-forge-event-handler!
  "Register the Forge InputEvent$Key listener."
  []
  (try
    (install/process-once! ::event-handler-installed
      #(do
         ;; Providers are installed before listener registration, not before
         ;; namespace construction. Cache the neutral initial state once so
         ;; key events remain on the atom-only path.
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

         (.addListener NeoForge/EVENT_BUS
                       EventPriority/NORMAL
                       false
                       InputEvent$MouseScrollingEvent
                       (reify java.util.function.Consumer
                         (accept [_ evt]
                           (on-mouse-scroll evt))))

         (log/debug "Forge keyboard event handler installed")))

    (catch Exception e
      (log/stacktrace "Failed to install Forge keyboard event handler" e))))

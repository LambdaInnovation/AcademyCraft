(ns cn.li.neoforge1211.client.runtime-bridge
  "CLIENT-ONLY Forge adapter for runtime hooks."
  (:require [cn.li.mc1211.client.effects.particle :as particle]
            [cn.li.mc1211.client.effects.sound :as sound]
            [cn.li.mc1211.client.session-cleanup :as session-cleanup]
            [cn.li.mcbase.client.session :as client-session]
            [cn.li.mcbase.client.overlay.state :as overlay-state]
            [cn.li.mcbase.glfw-polling-core :as glfw-polling]
            [cn.li.platform.neutral.hooks :as power-runtime]
             [cn.li.mcmod.runtime.install :as install]
             [cn.li.mcmod.util.log :as log]
             [cn.li.mc1211.client.player-state-core :as player-state]
             [cn.li.mc1211.client.font.msdf-tick :as msdf-tick])
  (:import [cn.li.mc1211.client.effect ScriptedEffectSpawner]
            [net.neoforged.neoforge.common NeoForge]
           [net.neoforged.neoforge.client.event ClientTickEvent$Post]
           [net.neoforged.neoforge.client.event InputEvent$MouseScrollingEvent]
           [net.neoforged.bus.api EventPriority]
           [net.minecraft.client Minecraft]))

(defn active-contexts []
  (power-runtime/client-active-contexts))

(defn latest-sync [player-uuid]
  (power-runtime/client-latest-sync player-uuid))

(defn local-player-item-id []
  (player-state/local-player-item-id))

(defn local-player-pos [] (player-state/local-player-pos))
(defn local-player-eye-pos [] (player-state/local-player-eye-pos))
(defn camera-position [] (player-state/camera-position))
(defn local-player-look-end [distance] (player-state/local-player-look-end distance))
(defn local-player-block-aim [distance] (player-state/local-player-block-aim distance))
(defn camera-raycast-visible? [from-x from-y from-z to-x to-y to-z]
  (player-state/camera-raycast-visible? from-x from-y from-z to-x to-y to-z))

(defn clear-client-activated-overlay! []
  (if-let [owner (client-session/current-local-player-owner)]
    (overlay-state/clear-client-activated! owner)
    (when-let [session-id (client-session/client-session-id)]
      (overlay-state/clear-client-overlay-session! session-id))))

(defn spawn-local-scripted-effect!
  "Returns the spawned entity's UUID string (nil on failure) so callers that
  own a despawn —e.g. JetEngine's diamond shield —can remove it later."
  [effect-id]
  (ScriptedEffectSpawner/spawnLocalWithUuid effect-id))

(defn spawn-local-scripted-effect-at! [effect-id x y z]
  (ScriptedEffectSpawner/spawnLocalAt effect-id x y z))

(defn spawn-scripted-effect-at-player!
  "Spawn a scripted effect anchored to `owner-uuid` (any currently-loaded
  player, not just the local one) —the string UUID of the spawned entity is
  returned so the caller can despawn it later via
  remove-local-scripted-effect!. Used for effects that must appear at a
  skill's caster for every nearby viewer, not just the caster's own screen."
  [effect-id owner-uuid]
  (ScriptedEffectSpawner/spawnAtPlayerWithUuid effect-id owner-uuid))

(defn move-local-scripted-effect!
  "Move a client-local scripted effect entity to an absolute position
  (upstream Flashing localTick: marking.setPosition(dest))."
  [entity-uuid x y z]
  (ScriptedEffectSpawner/moveLocalByUuid entity-uuid (double x) (double y) (double z)))

(defn remove-local-scripted-effect! [entity-uuid]
  (ScriptedEffectSpawner/removeLocalByUuid entity-uuid))

(defn slot-visual-state [player-uuid key-idx]
  (client-session/with-current-client-session #(power-runtime/client-slot-visual-state player-uuid key-idx)))

(defn on-slot-key-down! [player-uuid key-idx]
  (client-session/with-current-client-session #(power-runtime/client-on-slot-key-down! player-uuid key-idx)))

(defn on-slot-key-tick! [player-uuid key-idx]
  (client-session/with-current-client-session #(power-runtime/client-on-slot-key-tick! player-uuid key-idx)))

(defn on-slot-key-up! [player-uuid key-idx]
  (client-session/with-current-client-session #(power-runtime/client-on-slot-key-up! player-uuid key-idx)))

(defn on-slot-key-abort! [player-uuid key-idx]
  (client-session/with-current-client-session #(power-runtime/client-on-slot-key-abort! player-uuid key-idx)))

(defn on-movement-key-down! [player-uuid movement-key]
  (client-session/with-current-client-session #(power-runtime/client-on-movement-key-down! player-uuid movement-key)))

(defn on-movement-key-tick! [player-uuid movement-key]
  (client-session/with-current-client-session #(power-runtime/client-on-movement-key-tick! player-uuid movement-key)))

(defn on-movement-key-up! [player-uuid movement-key]
  (client-session/with-current-client-session #(power-runtime/client-on-movement-key-up! player-uuid movement-key)))

(defn abort-all! []
  (client-session/with-current-client-session #(power-runtime/client-abort-all!)))

;; ===== Key State Function for keybinds/tick-keys! =====
;; slot/movement/screen key maps + glfw-key-state-fn now live in
;; cn.li.mcbase.glfw-polling-core, shared with Fabric's keyboard_init.clj —
;; a key remap only needs to change one place.

(defn- screen-open? []
  (some? (.screen (Minecraft/getInstance))))

(defn- get-player-uuid-str
  "Get current player UUID as string for keybinds context."
  []
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [player (.player mc)]
        (str (.getUUID player))))
    (catch Throwable _ nil)))

(defn tick-client! []
  (session-cleanup/tick-connection-change! {})
  (particle/tick-particles!)
  (sound/tick-sounds!)
  (msdf-tick/client-tick!)
  ;; Per-frame key polling for skill slot keys (Z/X/C/V held) + movement keys + GUI keys
  ;; Needs client session ctx: keybinds owner resolution reads client-session-id.
  ;; Raw physical state is polled EVERY tick (Screen open or not) and
  ;; screen-open? gates event dispatch — upstream KeyManager keeps tracking
  ;; physical key state while a GUI is open and ClientRuntime gates dispatch
  ;; on ClientUtils.isPlayerInGame(), so the click that closes a Screen is
  ;; absorbed instead of re-firing a bound skill.
  (client-session/with-current-client-session
    #(power-runtime/client-tick-keys!
       glfw-polling/glfw-key-state-fn get-player-uuid-str (screen-open?)))
  (client-session/with-current-client-session #(power-runtime/client-tick!)))

(defn- on-client-tick [^ClientTickEvent$Post evt]
  (tick-client!))

(defn- on-mouse-scroll [^InputEvent$MouseScrollingEvent evt]
  ;; Upstream PenetrateTeleport onPlayerUseWheel: mouse wheel adjusts the
  ;; teleport distance (raw GLFW delta, ~1.0 per notch — NeoForge names it
  ;; getScrollDeltaY). The hook resolves the active penetrate context
  ;; itself; when it consumed the wheel, cancel the event so the vanilla
  ;; hotbar does not switch items.
  (client-session/with-current-client-session
    #(when (power-runtime/client-on-slot-wheel! (get-player-uuid-str) 0 (.getScrollDeltaY evt))
       (.setCanceled evt true))))

(defn init! []
  (power-runtime/client-register-push-handlers!)
  (install/process-once! ::tick-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false ClientTickEvent$Post
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-client-tick evt)))))
  (install/process-once! ::wheel-listener-registered
    #(.addListener (NeoForge/EVENT_BUS)
                   EventPriority/NORMAL false InputEvent$MouseScrollingEvent
                   (reify java.util.function.Consumer
                     (accept [_ evt] (on-mouse-scroll evt)))))
  (log/debug "Client runtime bridge initialized"))

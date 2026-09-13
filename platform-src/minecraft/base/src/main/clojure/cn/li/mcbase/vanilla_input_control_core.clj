(ns cn.li.mcbase.vanilla-input-control-core
  "ControlOverrider equivalent: clear vanilla KeyMapping pressed state
   for ability-slot key codes so Minecraft does not attack/use/swing while the
   skill system owns those keys via GLFW polling."
  (:require [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client KeyMapping Minecraft Options]
           [cn.li.mcbase.client KeyMappingAccess]))

(defn- get-minecraft
  []
  (Minecraft/getInstance))

(defn- get-options
  (^Options [] (get-options (get-minecraft)))
  (^Options [minecraft]
   (when minecraft
     (.options ^Minecraft minecraft))))

(defn ac-key-code
  "Map a KeyMapping's bound InputConstants.Key to the AC Settings integer
   convention (MOUSE_LEFT=-100, MOUSE_RIGHT=-99, else GLFW keysym)."
  [^KeyMapping mapping]
  (KeyMappingAccess/acKeyCode mapping))

(defn- drain-and-release!
  "Consume pending clicks and force the mapping not-down for this frame."
  [^KeyMapping mapping]
  (while (.consumeClick mapping))
  (.setDown mapping false))

(defn suppress-key-codes!
  "For each AC key-code in `key-codes`, force matching vanilla KeyMappings
   off for this frame: the interaction mappings (attack/use/pick) plus the
   movement mappings (up/down/left/right).

   Movement suppression mirrors upstream ControlOverrider owning the Flashing
   KEY_GROUP WASD sub-keys: while a flashing context is alive the movement
   keys drive the flash preview instead of walking.

   Matching is by the mapping's *current* bound key, so rebinding vanilla
   attack away from LMB (or ability slots onto other keys) still works.
   Empty `key-codes` is a no-op."
  [key-codes]
  (let [codes (into #{} (map int) key-codes)]
    (when (seq codes)
      (try
        (when-let [^Options opts (get-options)]
          (doseq [^KeyMapping mapping [(.keyAttack opts)
                                       (.keyUse opts)
                                       (.keyPickItem opts)
                                       (.keyUp opts)
                                       (.keyDown opts)
                                       (.keyLeft opts)
                                       (.keyRight opts)]]
            (when (and mapping (contains? codes (ac-key-code mapping)))
              (drain-and-release! mapping))))
        (catch Exception e
          (log/debug e "Failed to suppress vanilla KeyMappings" {:key-codes key-codes})))))
  nil)

(def ^:private impl
  {:suppress! (fn [key-codes] (suppress-key-codes! key-codes))})

(defn get-spi-implementation
  "SPI map installed by Forge/Fabric/NeoForge client init."
  []
  impl)

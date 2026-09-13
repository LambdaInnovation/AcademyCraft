(ns cn.li.mcbase.gui.reactive.clock
  "Clock driver -- set the three clock signals each frame."
  (:require [cn.li.platform.neutral.ui :as rt]
            [cn.li.platform.neutral.ui :as sig])
  (:import [cn.li.mcver McAccess McClientAccess]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [net.minecraft.client Minecraft]))

(defn- resolve-partial-ticks
  ^double
  [^Minecraft mc partial-ticks]
  (if (some? partial-ticks)
    (double partial-ticks)
    (McClientAccess/clientPartialTick mc)))

(defn- resolve-game-ticks
  ^long
  [^Minecraft mc]
  (or (when-let [server (.getSingleplayerServer mc)]
        (long (McAccess/serverTickCount server)))
      (when-let [level (.level mc)]
        (long (McAccess/gameTime level)))
      0))

(defn tick!
  "Update clock signals from Minecraft game state. Called once per frame.

   clock-ms uses System/currentTimeMillis (wall clock) so UI animations keep
   running even when the game is paused in a GUI screen.  game-ticks prefers
   integrated-server tick count; falls back to McAccess/gameTime on remote
   clients."
  [^UiRt runtime partial-ticks]
  (let [^Minecraft mc (Minecraft/getInstance)
        pt (resolve-partial-ticks mc partial-ticks)]
    (sig/sset-l! (rt/clock-ms-sig runtime) (System/currentTimeMillis))
    (sig/sset-d! (rt/partial-ticks-sig runtime) pt)
    (sig/sset-l! (rt/game-ticks-sig runtime) (resolve-game-ticks mc))))

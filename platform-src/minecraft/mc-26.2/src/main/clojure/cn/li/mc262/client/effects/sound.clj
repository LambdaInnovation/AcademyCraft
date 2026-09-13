(ns cn.li.mc262.client.effects.sound
  "CLIENT-ONLY shared sound effect bridge for Minecraft 26.2."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mc262.runtime.registry :as registry])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.sounds SoundManager]
           [net.minecraft.sounds SoundSource SoundEvent]
           [net.minecraft.resources Identifier]
           [cn.li.mcver ResourceLocations]
           [cn.li.mcbase.client.audio ExternalOggPlayer]
           [cn.li.mc262.client.audio LoopingSoundRegistry]))

(def ^:private sound-source-map
  {:ambient SoundSource/AMBIENT
   :blocks SoundSource/BLOCKS
   :hostile SoundSource/HOSTILE
   :master SoundSource/MASTER
   :music SoundSource/MUSIC
   :neutral SoundSource/NEUTRAL
   :players SoundSource/PLAYERS
   :records SoundSource/RECORDS
   :weather SoundSource/WEATHER})

(defn- resolve-sound-source
  [source]
  (get sound-source-map source SoundSource/PLAYERS))

(defn- play-sound-effect
  [sound-cmd]
  (try
    (when-let [^Minecraft mc (Minecraft/getInstance)]
      (when-let [player (.player mc)]
        (when-let [level (.level player)]
          (let [{:keys [sound-id source volume pitch x y z]} sound-cmd
                sound-loc (ResourceLocations/parse ^String sound-id)
                pos-x (or x (.getX player))
                pos-y (or y (.getY player))
                pos-z (or z (.getZ player))
                ^SoundEvent sound-event (.getValue (registry/builtin "SOUND_EVENT") sound-loc)]
            (when sound-event
              (.playLocalSound level pos-x pos-y pos-z
                               sound-event
                               (resolve-sound-source source)
                               (float volume)
                               (float pitch)
                               false))))))
    (catch Exception e
      (log/stacktrace "Error playing sound effect" e))))

(defn tick-sounds!
  []
  (try
    (when-let [owner (client-session/current-local-player-owner)]
      (doseq [sound-cmd (power-runtime/client-poll-sound-effects owner)]
        (play-sound-effect sound-cmd)))
    (catch Exception e
      (log/debug "Error in sound tick:" (ex-message e)))))

(defn- get-sound-manager []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (.getSoundManager mc)))

(defn stop-sound!
  "Stop a playing sound by its Identifier id (e.g. \"academy:em.arc_strong\")."
  [sound-id]
  (when-let [^SoundManager sm (get-sound-manager)]
    (let [^Identifier loc (if (string? sound-id)
                            (ResourceLocations/parse ^String sound-id)
                            (ResourceLocations/of (namespace sound-id) (name sound-id)))]
      ;; 26.2 requires an explicit SoundSource with Identifier stop.
      (.stop sm loc SoundSource/PLAYERS)
      (.stop sm loc SoundSource/AMBIENT)
      (.stop sm loc SoundSource/MASTER))))

(defn stop-all-media!
  "Stop external OGG media and tear down tracked looping sounds.
   26.2 SoundManager no longer exposes stop(SoundSource); ExternalOggPlayer
   is the primary media backend."
  []
  (ExternalOggPlayer/stop)
  (LoopingSoundRegistry/stopAll)
  nil)

;; -- True looping sounds (TickableSoundInstance-backed) --

(defn start-loop-sound!
  "Start (or restart) a native-looping sound at [x y z], tracked by `key`
  (any stringifiable value) so it can be repositioned or stopped later."
  [key sound-id volume pitch x y z]
  (LoopingSoundRegistry/start (str key) (str sound-id) (float volume) (float pitch)
                               (double x) (double y) (double z))
  nil)

(defn start-loop-sound-at-player!
  "Start (or restart) an ambient sound that follows a loaded player by UUID
  until explicitly stopped. `looping?` (default true) off plays the clip once
  and lets it end on its own, still stoppable by key."
  ([key sound-id volume pitch player-uuid]
   (start-loop-sound-at-player! key sound-id volume pitch player-uuid true))
  ([key sound-id volume pitch player-uuid looping?]
   (LoopingSoundRegistry/startFollowingPlayer
    (str key) (str sound-id) (float volume) (float pitch) (str player-uuid)
    (boolean looping?))
   nil))

(defn update-loop-sound-position!
  [key x y z]
  (LoopingSoundRegistry/updatePosition (str key) (double x) (double y) (double z))
  nil)

(defn stop-loop-sound!
  [key]
  (LoopingSoundRegistry/stop (str key))
  nil)

(defn init!
  []
  (log/debug "Shared sound effect bridge initialized"))

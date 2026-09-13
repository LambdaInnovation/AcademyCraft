(ns cn.li.mc1201.client.effects.sound
  "CLIENT-ONLY shared sound effect bridge for Minecraft 1.20.1."
  (:require [cn.li.mcbase.client.session :as client-session]
            [cn.li.platform.neutral.hooks :as power-runtime]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.sounds SoundManager]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.sounds SoundSource SoundEvent]
           [net.minecraft.resources ResourceLocation]
           [cn.li.mc1201.client.audio LoopingSoundRegistry]))

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
                sound-loc (ResourceLocation. ^String sound-id)
                pos-x (or x (.getX player))
                pos-y (or y (.getY player))
                pos-z (or z (.getZ player))
                ^SoundEvent sound-event (.get BuiltInRegistries/SOUND_EVENT sound-loc)]
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

;; -- SoundManager helpers (no :import — uses reflection to avoid
;;    compile-time class loading that triggers registry bootstrap) --

(defn- get-sound-manager []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (.getSoundManager mc)))

(defn stop-sound!
  "Stop a playing sound by its ResourceLocation id (e.g. \"academy:em.arc_strong\")."
  [sound-id]
  (when-let [^SoundManager sm (get-sound-manager)]
    (let [loc (ResourceLocation. (namespace sound-id) (name sound-id))]
      (.stop sm ^ResourceLocation loc))))

(defn stop-all-media!
  "Stop all sounds in the PLAYERS category (covers media playback).
  Matches upstream implementation MediaBackend stop behavior."
  []
  (when-let [^SoundManager sm (get-sound-manager)]
    (.stop sm SoundSource/PLAYERS)))

;; -- True looping sounds (TickableSoundInstance-backed) --
;; For skills that need a continuous, position-following, explicitly
;; stoppable loop (e.g. current-charging's held-arc sound) rather than the
;; fire-and-forget queue above, which can only ever play one-shot clips.

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

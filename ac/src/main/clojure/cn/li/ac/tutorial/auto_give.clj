(ns cn.li.ac.tutorial.auto-give
  "Tutorial item auto-give on first player login.

  Mirrors upstream AcademyCraft TutorialData: reads/writes a boolean
  directly in player persistent NBT (like @SerializeIncluded), completely
  independent of the runtime store lifecycle.  This guarantees reliable
  persistence because Minecraft handles player NBT save/load natively."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.tutorial.config :as tut-config]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  )

(def tutorial-item-id (modid/namespaced-path "tutorial"))
(def ^:private nbt-key "ac_tutorial_acquired_v2")

(defn- player-persistent-data
  [player]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :player-persistent-data :get! player)))

(defn- tutorial-acquired-in-nbt?
  [tag]
  (and (sd/has-key-safe? tag nbt-key) (sd/get-boolean tag nbt-key)))

(defn auto-give-on-login!
  "Check NBT flag directly (matching upstream @SerializeIncluded boolean).
  Called with just the ServerPlayer, resolved in the lifecycle hook."
  [player]
  (when (tut-config/give-cloud-terminal-enabled?)
    (let [tag (player-persistent-data player)]
      (when-not (tutorial-acquired-in-nbt? tag)
        (try
          (when-let [stack (pitem/stack-by-id tutorial-item-id 1)]
            (entity/player-give-item-stack! player stack)
            (sd/set-boolean! tag nbt-key true)
            (log/info "Tutorial item auto-given to player" {:uuid (uuid/player-uuid player)}))
          (catch Exception e
            (log/warn "Failed to auto-give tutorial item:" (ex-message e))))))))

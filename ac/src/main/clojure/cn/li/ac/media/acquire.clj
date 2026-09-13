(ns cn.li.ac.media.acquire
  "Server-side per-player acquired-internal-media tracking — persisted
  directly to player NBT. Mirrors upstream MediaAcquireData (a bitset of
  unlocked internal media ids); external tracks need no acquisition (matches
  upstream: 'media.external || bitset.get(...)')."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.media.catalog :as catalog]
            [cn.li.mcmod.platform.structured-data :as sd]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.ac.persistence.nbt-collections :as nbt-coll]
            [cn.li.mcmod.util.log :as log]))

(def ^:private nbt-key "ac_media_v1")

(defn- player-persistent-data
  [player]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :player-persistent-data :get! player)))

(defn- load-ids
  [tag]
  (when (sd/has-key-safe? tag nbt-key)
    (let [root (sd/get-structured tag nbt-key)]
      (nbt-coll/read-keyword-set root "acquired"))))

(defn- save-ids!
  [tag ids]
  (let [root (sd/create-structured)]
    (nbt-coll/write-keyword-set! root "acquired" ids)
    (sd/set-entry! tag nbt-key root)))

(defn acquired-ids
  "Set of acquired internal media ids (keywords) for `player`."
  [player]
  (or (load-ids (player-persistent-data player)) #{}))

(defn is-acquired?
  [player media-id]
  (let [media-id (keyword media-id)]
    (or (not (catalog/internal-media? media-id))
        (contains? (acquired-ids player) media-id)))
  )

(defn acquire!
  "Unlock an internal media id for `player`. No-op for unknown/external ids."
  [player media-id]
  (let [media-id (keyword media-id)]
    (when (catalog/internal-media? media-id)
      (let [tag (player-persistent-data player)
            current (or (load-ids tag) #{})]
        (when-not (contains? current media-id)
          (log/info "Acquired media" media-id "for player:" (str (uuid/player-uuid player)))
          (save-ids! tag (conj current media-id)))))
    nil))

(defn installed-medias
  "Internal tracks `player` has acquired.

  External tracks are deliberately excluded. They are scanned from the
  player's own disk and only exist client-side, so the server has nothing to
  say about them — it could only ever see them in single-player, where client
  and server share one JVM and therefore one catalog atom. That leak made the
  media list report every external track as \"granted\", and the app then
  listed it a second time from its own scan: every external row appeared
  twice in single-player and once on a dedicated server."
  [player]
  (let [acquired (acquired-ids player)]
    (filterv #(contains? acquired (:id %)) (catalog/internal-medias))))

(ns cn.li.ac.media.catalog
  "Media catalog for the terminal's Media Player app.

  Matches upstream MediaManager/Media: two kinds of track —
  - internal: bundled with the mod, gated by MediaAcquireData-style per-player
    unlock (see cn.li.ac.media.acquire). This port ships the catalog/unlock
    plumbing WITHOUT bundled audio (the originals are copyrighted anime
    songs) — :source is nil until real .ogg files are supplied.
  - external: user-supplied .ogg files scanned from a folder on disk at
    client startup (see cn.li.ac.media.external-scan), always available,
    with a client-local (per-machine) editable name/description.")

(def internal-media-ids
  "Stable ids, matching the existing media_0/1/2 items in cn.li.ac.item.media."
  [:media_0 :media_1 :media_2])

(def ^:private internal-catalog
  {:media_0 {:id :media_0 :external? false
             :name "Sisters' Noise" :desc "FripSide" :source nil :length-secs 0.0}
   :media_1 {:id :media_1 :external? false
             :name "Only My Railgun" :desc "FripSide" :source nil :length-secs 0.0}
   :media_2 {:id :media_2 :external? false
             :name "Level5 Judgelight" :desc "FripSide" :source nil :length-secs 0.0}})

(defonce ^:private external-catalog (atom {}))

(defn register-external-media!
  "Register (or replace) one externally-scanned track. `media` must include
  :id (keyword), :name, :desc, :source (absolute file path string),
  :length-secs, and optionally :cover (a registered texture location
  string, as produced by the loader's cover scan -- not a disk path)."
  [media]
  (swap! external-catalog assoc (:id media) (assoc media :external? true))
  nil)

(defn update-external-media!
  "Update mutable client-local metadata for one external track."
  [id changes]
  (let [id (keyword id)]
    (when (contains? @external-catalog id)
      (swap! external-catalog update id merge (select-keys changes [:name :desc]))))
  nil)

(defn reset-external-media!
  "Clear all registered external tracks (used before a rescan)."
  []
  (reset! external-catalog {})
  nil)

(defn internal-medias [] (mapv internal-catalog internal-media-ids))
(defn external-medias [] (vec (vals @external-catalog)))
(defn all-medias [] (into (internal-medias) (external-medias)))

(defn media-by-id
  [id]
  (let [id (keyword id)]
    (or (get internal-catalog id) (get @external-catalog id))))

(defn internal-media?
  [id]
  (contains? internal-catalog (keyword id)))

(defn display-length
  "mm:ss for a positive length, or \"--:--\" when unknown (no bundled audio)."
  [length-secs]
  (if (and length-secs (pos? length-secs))
    (let [total (long length-secs)
          m (quot total 60) s (rem total 60)]
      (format "%02d:%02d" m s))
    "--:--"))

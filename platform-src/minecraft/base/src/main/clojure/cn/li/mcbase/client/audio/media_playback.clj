(ns cn.li.mcbase.client.audio.media-playback
  "Bridges the shared ExternalOggPlayer (raw OpenAL playback of an external
  .ogg file path) and OggMetadata scanner into framework platform adapters.
  Shared by Forge and Fabric — both loaders install this identically."
  (:require [clojure.string :as str]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcbase.client.audio ExternalOggPlayer]
           [cn.li.mcbase.client.audio OggMetadata]
           [cn.li.mcver ResourceLocations DynamicTextureAccess]
           [com.mojang.blaze3d.platform NativeImage]
           [net.minecraft.client Minecraft]
           [java.io File FileInputStream]
           [java.nio.charset StandardCharsets]
           [java.nio.file Files]))

(defn play!
  [source-path volume]
  (when source-path
    (ExternalOggPlayer/play source-path (float volume))))

(defn stop!
  []
  (ExternalOggPlayer/stop))

(defn pause!
  []
  (ExternalOggPlayer/pause))

(defn resume!
  []
  (ExternalOggPlayer/resume))

(defn set-volume!
  [volume]
  (ExternalOggPlayer/setVolume (float volume)))

(defn playing?
  []
  (ExternalOggPlayer/isPlaying))

(defn seek!
  [seconds]
  (ExternalOggPlayer/seek (float seconds)))

(defn playback-state
  []
  {:status (keyword (ExternalOggPlayer/getPlaybackState))
   :elapsed-secs (double (ExternalOggPlayer/getElapsedSeconds))
   :volume (double (ExternalOggPlayer/getVolume))})

;; ============================================================================
;; External track discovery — <gameDir>/acmedia/source/*.ogg (same folder
;; layout as upstream) probed for duration only; no full decode, no cover art.
;; ============================================================================

(defn- media-root
  ^File []
  (File. (.gameDirectory ^Minecraft (Minecraft/getInstance)) "acmedia"))

(defn- source-folder ^File [] (File. (media-root) "source"))
(defn- cover-folder  ^File [] (File. (media-root) "cover"))

(def ^:private readme-text
  "Written into acmedia/ on load, as upstream MediaManager.loadClient copies
   its readme_template.txt there. Reworded for this port: it carries the
   catalog and unlock plumbing but ships no bundled audio, so `source` starts
   empty rather than pre-filled with the original tracks."
  (str "========================================================================
"
       "
"
       "AcademyCraft - custom media
"
       "
"
       "1. Put a music file into  acmedia/source/  (.ogg / Vorbis only).
"
       "2. Optionally put a cover of the SAME NAME into  acmedia/cover/
"
       "   (.png, square looks best).
"
       "3. Open the Media Player app. The folder is rescanned every time it
"
       "   opens, so there is no need to restart the game.
"
       "
"
       "Track name and description can be edited in the app; those edits last
"
       "for the session only.
"
       "
"
       "========================================================================
"
       "
"
       "1. 将音乐文件放入  acmedia/source/  （仅支持 .ogg / Vorbis）。
"
       "2. 可选：将同名封面图放入  acmedia/cover/  （.png，建议方形）。
"
       "3. 打开媒体播放器 App。每次打开都会重新扫描，无需重启游戏。
"
       "
"
       "曲目名称与说明可在 App 内直接编辑，仅在本次游戏会话内有效。
"
       "
"
       "========================================================================
"))

(defn ensure-media-folders!
  "Create acmedia/, acmedia/source/ and acmedia/cover/, and drop a README in.

   Upstream MediaManager.loadClient does the same while the mod loads --
   checkPath mkdirs each folder and the readme template is copied in -- so a
   player always has somewhere to put files and something telling them how.
   listOggFiles only returns empty for a missing directory, which left nothing
   to say the path existed or which run directory it lived under.

   Called from the bridge install, i.e. client setup: the earliest point where
   Minecraft is constructed and gameDirectory resolves. Hanging it off the scan
   meant the folders only appeared once the Media Player had been opened."
  []
  (try
    (let [^File root (media-root)
          ^File src (source-folder)
          ^File cov (cover-folder)]
      (doseq [^File d [root src cov]]
        (when-not (.isDirectory d) (.mkdirs d)))
      ;; Upstream copies with REPLACE_EXISTING every load; keep the file current
      ;; rather than preserving edits to what is a generated template.
      (Files/write (.toPath (File. root "README.txt"))
                   (.getBytes ^String readme-text StandardCharsets/UTF_8)
                   ^"[Ljava.nio.file.OpenOption;" (into-array java.nio.file.OpenOption []))
      (log/debug "External media folder:" (.getAbsolutePath src))
      src)
    (catch Throwable e
      (log/warn e "Could not prepare the external media folders")
      nil)))

(defn- meta-file ^File [] (File. (media-root) "tracks.properties"))

(defn- load-track-meta
  "Persisted per-track name/desc. Upstream keeps these in the mod config, as
   media.<id>_name / media.<id>_desc defaulting to the id, so a renamed track
   survives a restart. Our config domains are schema-declared and cannot take a
   key per file, so they live in acmedia/tracks.properties instead -- same
   effect, stored beside the media it describes."
  []
  (let [^File f (meta-file)]
    (if (.isFile f)
      (try
        (with-open [in (FileInputStream. f)]
          (let [props (java.util.Properties.)]
            (.load props in)
            (into {} (map (fn [k] [k (.getProperty props k)]) (.stringPropertyNames props)))))
        (catch Throwable e
          (log/warn e "Could not read acmedia/tracks.properties")
          {}))
      {})))

(defn save-track-meta!
  "Persist one track's edited name/desc."
  [id field value]
  (try
    (let [k (str id "_" (name field))
          cur (load-track-meta)
          props (java.util.Properties.)]
      (doseq [[pk pv] (assoc cur k (str value))]
        (.setProperty props (str pk) (str pv)))
      (ensure-media-folders!)
      (with-open [out (java.io.FileOutputStream. ^File (meta-file))]
        (.store props out "AcademyCraft external media names/descriptions"))
      true)
    (catch Throwable e
      (log/warn e "Could not persist media track metadata")
      false)))

(defn- cover-src
  "Register acmedia/cover/<id>.png as a texture and return its location string,
   or nil when there is no cover. Upstream reads the same <id>.png beside the
   track; a file on disk is not a resource-pack path, so it has to be uploaded
   as a DynamicTexture before any :image node can name it.

   Runs on the render thread: client setup and the Media Player's open-time
   rescan are both there."
  [id]
  (let [^File f (File. (cover-folder) (str id ".png"))]
    (when (.isFile f)
      (try
        (with-open [in (FileInputStream. f)]
          (let [safe (-> (str/lower-case (str id))
                         (str/replace #"[^a-z0-9_.-]" "_"))
                ;; Constructor shape changed in 26.2.  The version-specific
                ;; Java shim is platform AOT/remapped; this shared code sees
                ;; only its stable signature.
                tex (DynamicTextureAccess/create
                     (str "acmedia-cover/" safe) (NativeImage/read in))
                rl (ResourceLocations/of "academy" (str "acmedia_cover/" safe))]
            (.register (.getTextureManager (Minecraft/getInstance)) rl tex)
            (str rl)))
        (catch Throwable e
          (log/warn e (str "Could not load media cover: " (.getAbsolutePath f)))
          nil)))))

(defn- file->track [^File f meta]
  (let [fname (.getName f)
        id (subs fname 0 (- (count fname) 4)) ;; strip ".ogg"
        info (OggMetadata/probe (.getAbsolutePath f))]
    (when info
      {:id id
       :source (.getAbsolutePath f)
       :cover (cover-src id)
       ;; Upstream defaults both to the id (media.<id>_name / _desc).
       :name (get meta (str id "_name") id)
       :desc (get meta (str id "_desc") id)
       :length-secs (double (.-lengthSecs info))})))

(defn scan-external-tracks!
  []
  (try
    (ensure-media-folders!)
    (let [meta (load-track-meta)]
      (->> (OggMetadata/listOggFiles (source-folder))
           (keep #(file->track % meta))
           vec))
    (catch Throwable e
      (log/warn e "Failed to scan external media tracks")
      [])))

(defn install-media-playback-bridge!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter!
      fw-atom
      :media-playback
      {:play! play!
       :stop! stop!
       :pause! pause!
       :resume! resume!
       :set-volume! set-volume!
       :seek! seek!
       :playing? playing?
       :state playback-state})
    (platform/install-adapter!
      fw-atom
      :media-library
      {:scan-external-tracks! scan-external-tracks!
       :save-track-meta! save-track-meta!})
    ;; Client setup runs on the main thread with Minecraft constructed, so the
    ;; game directory is resolvable here — content init, where the scan is
    ;; kicked off, is far too early for that.
    (ensure-media-folders!))
  nil)

(ns cn.li.ac.terminal.catalog
  "Immutable terminal app catalog and pure queries (server-safe metadata only)."
  (:require [clojure.string :as str]
            [cn.li.ac.config.modid :as modid]))

(def apps
  [{:id :skill-tree
    :name "Skill Tree"
    :icon (modid/asset-path "textures/guis/apps" "skill_tree/icon.png")
    :description "View and manage your abilities"
    :category :abilities}
   {:id :settings
    :name "Settings"
    :icon (modid/asset-path "textures/guis/apps" "settings/icon.png")
    :description "Configure game settings"
    :category :system
    :pre-installed? true}
   {:id :tutorial
    :name "MisakaCloud"
    :icon (modid/asset-path "textures/guis/apps" "tutorial/icon_0.png")
    :icons [(modid/asset-path "textures/guis/apps" "tutorial/icon_0.png")
            (modid/asset-path "textures/guis/apps" "tutorial/icon_1.png")
            (modid/asset-path "textures/guis/apps" "tutorial/icon_2.png")]
    :description "Learn how to use your abilities"
    :category :help
    :pre-installed? true}
   {:id :freq-transmitter
    :name "Frequency Transmitter"
    :icon (modid/asset-path "textures/guis/apps" "freq_transmitter/icon.png")
    :description "Manage wireless frequencies"
    :category :wireless}
   {:id :media-player
    :name "Media Player"
    :icon (modid/asset-path "textures/guis/apps" "media_player/icon.png")
    :description "Browse AcademyCraft media tracks"
    :category :media}
   {:id :about
    :name "About"
    :icon (modid/asset-path "textures/guis/apps" "about/icon.png")
    :description "Credits and information"
    :category :help
    :pre-installed? true}])

(def ^:private apps-by-id-index
  (into {} (map #(vector (get % :id) %) apps)))

(defn- apps-by-id
  []
  apps-by-id-index)

(defn app-ids
  []
  (mapv #(get % :id) apps))

(defn app-by-id
  [app-id]
  (get (apps-by-id) app-id))

(defn app-exists?
  [app-id]
  (boolean (app-by-id app-id)))

(defn app-icon [app]
  (if-let [icons (seq (:icons app))]
    (let [r (rand)
          idx (cond (< r 0.2) 0
                    (< r 0.3) 1
                    :else 2)]
      (nth icons idx))
    (:icon app)))

(defn app-name-key
  "Return the original AcademyCraft-style localization key for an app."
  [app]
  (str "app." modid/MOD-ID "."
       (str/replace (name (:id app)) "-" "_")))

(defn installed-apps-in-display-order
  "Resolve installed ids to catalog entries without losing AppRegistry order.
   Player persistence intentionally uses a set, but TerminalUI always rendered
   the AppRegistry enumeration order rather than a hash-set iteration order."
  [installed-ids]
  (let [installed (into #{} (map keyword) installed-ids)]
    (->> apps
         (filter #(contains? installed (:id %)))
         vec)))

(defn app-count
  []
  (count apps))

(defn ordered-apps
  []
  ;; `apps` is the explicit AppRegistry/display order. Do not sort by category:
  ;; the terminal grid has no categories and upstream preserves registration
  ;; order for both placement and selection.
  (vec apps))

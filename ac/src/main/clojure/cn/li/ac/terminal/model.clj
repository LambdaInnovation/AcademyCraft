(ns cn.li.ac.terminal.model
  "Pure terminal player-state model and transitions.
  Pre-installed apps (matching original App.isPreInstalled() / AppRegistry)
  are always considered installed regardless of :installed-apps set.")

(def state-key
  :terminal-data)

(def ^:private pre-installed-app-ids
  "App ids that are always installed (matching original setPreInstalled)."
  #{:about :tutorial :settings})

(defn fresh-state
  []
  {:terminal-installed? false
   :installed-apps #{}})

(defn normalize-state
  [terminal-data]
  (let [data (or terminal-data {})]
    {:terminal-installed? (boolean (:terminal-installed? data))
     :installed-apps (into #{} (map keyword (or (:installed-apps data) #{})))}))

(defn terminal-installed?
  [terminal-data]
  (boolean (:terminal-installed? (normalize-state terminal-data))))

(defn app-installed?
  "Returns true if app is installed. Pre-installed apps are always considered
  installed (matching original TerminalData.isInstalled(App) which checks
  app.isPreInstalled() || installedNameHashes.contains(...))."
  [terminal-data app-id]
  (or (contains? pre-installed-app-ids (keyword app-id))
      (contains? (:installed-apps (normalize-state terminal-data)) (keyword app-id))))

(defn install-terminal
  [terminal-data]
  (let [data (normalize-state terminal-data)]
    (if (:terminal-installed? data)
      data
      (assoc data :terminal-installed? true))))

(defn uninstall-terminal
  [_terminal-data]
  (fresh-state))

(defn install-app
  [terminal-data app-id]
  (update (normalize-state terminal-data)
          :installed-apps
          (fnil conj #{}) app-id))

(defn uninstall-app
  [terminal-data app-id]
  (update (normalize-state terminal-data)
          :installed-apps
          disj app-id))

(defn install-apps
  [terminal-data app-ids]
  (update (normalize-state terminal-data)
          :installed-apps
          (fnil into #{}) app-ids))

(ns cn.li.platform.neutral.tabbed-gui
  "Installed tabbed-menu callbacks for platform AOT code.")

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Tabbed GUI provider is unavailable: " operation))))

(defn tabbed-container? [& _] (unavailable :tabbed-container?))
(defn slots-active? [& _] (unavailable :slots-active?))
(defn slots-active-for-menu? [& _] (unavailable :slots-active-for-menu?))
(defn detach-tab-sync! [& _] (unavailable :detach-tab-sync!))

(def ^:private operation-vars
  {:tabbed-container? #'tabbed-container?
   :slots-active? #'slots-active?
   :slots-active-for-menu? #'slots-active-for-menu?
   :detach-tab-sync! #'detach-tab-sync!})

(defn install!
  [operations]
  (let [expected (set (keys operation-vars))]
    (when (or (not= expected (set (keys operations)))
              (some (complement ifn?) (vals operations)))
      (throw (ex-info "Tabbed GUI provider contract mismatch"
                      {:expected (sort expected) :actual (sort (keys operations))})))
    (doseq [[operation target-var] operation-vars]
      (alter-var-root target-var (constantly (get operations operation)))))
  nil)

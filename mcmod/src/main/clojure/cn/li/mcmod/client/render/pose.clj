(ns cn.li.mcmod.client.render.pose
  "Required pose/rotation operations for client rendering."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def pose-ops-keys #{:push-pose :pop-pose :translate :scale
                      :y-rotation :x-rotation :z-rotation :axis-rotation :get-matrix})

(defn- pose-op [k]
  (let [ops (get-in @(fw/fw-atom) [:platform :pose-ops])
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required pose operation is not installed"
                      {:operation k :installed (keys ops)})))
    f))

(defn install-pose-ops!
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) pose-ops-keys))]
      (swap! fw-atom assoc-in [:platform :pose-ops] ops-map)
      (log/debug "Pose ops installed:" (pr-str (keys ops-map)))
      (when missing
        (log/error "Pose ops MISSING required keys:" (pr-str missing))))
    (log/error "Pose ops install FAILED: Framework atom nil")))

(defn pose-ops-available? []
  (boolean (get-in @(fw/fw-atom) [:platform :pose-ops])))

(defn- invoke [k args]
  (try
    (apply (pose-op k) args)
    (catch Exception e
      (log/stacktrace (str "Pose operation failed: " (name k)) e)
      (log/stacktrace (str "Pose operation failed: " (name k)) e)
      (throw e))))

(defn apply-y-rotation [pose-stack angle-degrees]
  (invoke :y-rotation [pose-stack angle-degrees]))

(defn apply-x-rotation [pose-stack angle-degrees]
  (invoke :x-rotation [pose-stack angle-degrees]))

(defn apply-z-rotation [pose-stack angle-degrees]
  (invoke :z-rotation [pose-stack angle-degrees]))

(defn apply-axis-rotation [pose-stack angle-degrees ax ay az]
  (invoke :axis-rotation [pose-stack angle-degrees ax ay az]))

(defn push-pose [pose-stack]
  (invoke :push-pose [pose-stack]))

(defn pop-pose [pose-stack]
  (invoke :pop-pose [pose-stack]))

(defn translate [pose-stack x y z]
  (invoke :translate [pose-stack x y z]))

(defn scale [pose-stack x y z]
  (invoke :scale [pose-stack x y z]))

(defn get-matrix [pose-stack]
  (invoke :get-matrix [pose-stack]))

(ns cn.li.mcmod.platform.position
  "Position operations via Framework function map — pure relay layer, no MC dependencies."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def position-keys #{:pos-x :pos-y :pos-z :create-block-pos :pos-above
                      :position-get-block-pos :position-get-pos})

(defn install-position-ops!
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) position-keys))]
      (swap! fw-atom assoc-in [:platform :position-ops] ops-map)
      (log/debug "Position ops installed:" (pr-str (keys ops-map)))
      (when missing
        (log/error "Position ops MISSING required keys:" (pr-str missing)
                   "- position ops will fail silently!")))
    (log/error "Position ops install FAILED: Framework atom nil")))

(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :position-ops])))
(defn current-ops            [] (get-in @(fw/fw-atom) [:platform :position-ops]))

(defn- call [k & args]
  (let [ops (current-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required position operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn pos-x       [p]    (call :pos-x p))
(defn pos-y       [p]    (call :pos-y p))
(defn pos-z       [p]    (call :pos-z p))
(defn pos-above   [p]    (call :pos-above p))

(defn create-block-pos [x y z]
  (if-let [f (get (current-ops) :create-block-pos)]
    (f x y z)
    (throw (ex-info "Position ops not installed" {:key :create-block-pos}))))

(defn factory-initialized? []
  (boolean (get (current-ops) :create-block-pos)))

(defn block-pos [this] (call :position-get-block-pos this))
(defn pos [this] (call :position-get-pos this))

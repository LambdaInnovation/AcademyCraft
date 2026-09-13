(ns cn.li.mcmod.util.render
  "Rendering utilities - OpenGL and texture helpers.

  State stored in Framework [:service :render-runtime]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn- default-state []
  {:texture-binder nil :texture-binder-warned false})

(def ^:private render-path [:service :render-runtime])

(defn- render-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom render-path (default-state))
    (default-state)))

(defn- update-render-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in render-path
           (fn [current] (apply f (or current (default-state)) args))))
  nil)

(defn reset-render-runtime-state-for-test!
  []
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in render-path (default-state)))
  nil)

(defn register-texture-binder! [binder-fn]
  (update-render-state! assoc :texture-binder binder-fn))

(defn get-render-time []
  (/ (double (System/currentTimeMillis)) 1000.0))

(defn bind-texture
  [texture]
  (if-let [binder (:texture-binder (render-state-snapshot))]
    (binder texture)
    (let [warned? (:texture-binder-warned (render-state-snapshot))]
      (when-not warned?
        (update-render-state! assoc :texture-binder-warned true)
        (log/warn "Texture binder not registered; skipping bind" texture)))))

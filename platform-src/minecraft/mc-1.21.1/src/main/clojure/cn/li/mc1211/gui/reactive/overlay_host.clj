(ns cn.li.mc1211.gui.reactive.overlay-host
  "Overlay host -- thin shell over mcbase core + versioned render."
  (:require [cn.li.mcbase.gui.reactive.overlay-host-core :as core]
            [cn.li.mc1211.gui.reactive.render :as render]))

(def register-overlay! core/register-overlay!)
(def get-overlay-runtime core/get-overlay-runtime)
(def dispose-overlay! core/dispose-overlay!)

(defn update-overlay!
  [gg client-session-id screen-w screen-h partial-ticks build-fn update-fn]
  (core/update-overlay! render/draw-tape! gg client-session-id screen-w screen-h
                        partial-ticks build-fn update-fn))

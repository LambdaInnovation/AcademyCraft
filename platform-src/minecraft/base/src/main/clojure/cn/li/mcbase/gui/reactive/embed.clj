(ns cn.li.mcbase.gui.reactive.embed
  "Shared embedded-runtime helpers for reactive hosts.

  Entries live under UiRt user-signal :embedded-runtimes as an atom of
  {:child-rt :x :y :w :h :visible?-fn :anchor-node?} maps."
  (:require [cn.li.platform.neutral.ui :as rt])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

(defn dispose-embedded-runtimes!
  [^UiRt rt]
  (when-let [entries (rt/user-signal rt :embedded-runtimes)]
    (doseq [{:keys [child-rt]} @entries]
      (rt/dispose! child-rt))))

(defn render-embedded-runtimes!
  "render-embedded-runtime!: (fn [gg child-rt x y w h pt] ...)."
  [render-embedded-runtime! ^UiRt rt gg left top pt]
  (when-let [entries (rt/user-signal rt :embedded-runtimes)]
    (doseq [{:keys [child-rt x y w h visible?-fn anchor-node]} @entries]
      (when (or (nil? visible?-fn) (visible?-fn))
        (let [ax (if anchor-node (.getAbsX ^INode anchor-node) (double x))
              ay (if anchor-node (.getAbsY ^INode anchor-node) (double y))]
          (render-embedded-runtime! gg child-rt (+ (double left) ax) (+ (double top) ay) w h pt))))))

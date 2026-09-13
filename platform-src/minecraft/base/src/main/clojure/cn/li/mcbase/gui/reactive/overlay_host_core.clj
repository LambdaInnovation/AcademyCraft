(ns cn.li.mcbase.gui.reactive.overlay-host-core
  "Overlay host core -- manages a persistent UiRt per client session.
   Version modules supply draw-tape! (render pipeline differs on 26.2)."
  (:require [cn.li.platform.neutral.ui :as rt]
            [cn.li.platform.neutral.ui :as layout]
            [cn.li.mcbase.gui.reactive.clock :as clock])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [java.util HashMap]))

(def ^:private ^HashMap overlay-runtimes (HashMap.))

(defn register-overlay!
  "Register a reactive overlay runtime for a client session.
   build-fn: (fn [screen-w screen-h] -> UiRt) creates the tree + bindings."
  [client-session-id build-fn screen-w screen-h]
  (let [rt (build-fn screen-w screen-h)]
    (rt/resize! rt (double screen-w) (double screen-h))
    (.put overlay-runtimes client-session-id
          {:runtime rt :build-fn build-fn :width screen-w :height screen-h})
    rt))

(defn get-overlay-runtime
  "Get or create the overlay runtime for a client session."
  [client-session-id screen-w screen-h]
  (if-let [entry (.get overlay-runtimes client-session-id)]
    (:runtime entry)
    nil))

(defn update-overlay!
  "Per-frame update. draw-tape!: (fn [gg rt left top] ...)."
  [draw-tape! gg client-session-id screen-w screen-h partial-ticks build-fn update-fn]
  (let [entry (.get overlay-runtimes client-session-id)
        resized? (and entry
                      (or (not= screen-w (:width entry))
                          (not= screen-h (:height entry))))
        _ (when resized?
            (rt/dispose! (:runtime entry))
            (.remove overlay-runtimes client-session-id))
        ^UiRt runtime (when (and (or (nil? entry) resized?) build-fn)
                        (when-let [new-runtime (build-fn screen-w screen-h)]
                          (rt/resize! new-runtime (double screen-w) (double screen-h))
                          (.put overlay-runtimes client-session-id
                                {:runtime new-runtime
                                 :width screen-w
                                 :height screen-h})
                          new-runtime))
        ^UiRt rt (or runtime (when-not resized? (:runtime entry)))]
    (when rt
      (clock/tick! rt partial-ticks)
      (when update-fn (update-fn rt))
      (rt/flush! rt)
      (layout/ensure-layout! rt)
      (layout/ensure-tape! rt)
      (draw-tape! gg rt 0 0))))

(defn dispose-overlay!
  "Clean up overlay runtime for a client session."
  [client-session-id]
  (when-let [entry (.remove overlay-runtimes client-session-id)]
    (rt/dispose! (:runtime entry))
    nil))

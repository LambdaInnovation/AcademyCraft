(ns cn.li.mcmod.events.world-state-notify
  "Platform-neutral notification when modular world-state mutates.

  Content modules call `notify-world-state-changed!` after persisting state.
  Loader adapters register a dirty/save callback via `set-on-world-state-changed-fn!`."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private notify-path [:service :world-state-notify :on-changed])

(defn set-on-world-state-changed-fn!
  "Register a (fn [world-key]) callback invoked after world-state mutations."
  [f]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in notify-path f))
  nil)

(defn notify-world-state-changed!
  "Invoke the registered world-state change callback, if any."
  [world-key]
  (when-let [fw-atom (fw/fw-atom)]
    (when-let [f (get-in @fw-atom notify-path)]
      (f world-key)))
  nil)

(defn reset-for-test!
  []
  (set-on-world-state-changed-fn! nil))

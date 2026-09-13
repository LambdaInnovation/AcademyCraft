(ns cn.li.ac.gui.reactive.register
  "Reactive GUI bridge — installs reactive handlers via client bridge merge.
   Individual block GUIs self-register via their own init-*-reactive! functions."
  (:require [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.ac.ability.adapters.reactive-overlay :as reactive-overlay]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]
            [cn.li.mcmod.util.log :as log]))

(defn- reactive-screen-handler
  [gui-key container menu player]
  (if-let [entry (when-let [fw-atom (fw/fw-atom)]
                   (registry/get-spec fw-atom :guis [::screen-creator gui-key]))]
    ((:create entry) container menu player)
    (do (log/warn "No reactive screen for:" gui-key) nil)))

(defn register-screen!
  "Called by each gui_reactive.clj's init-*-reactive! to register its screen creator."
  [gui-key create-fn title]
  (registry/register! (fw/fw-atom) :guis [::screen-creator gui-key] {:create create-fn :title title}))

(defn install-bridge!
  "Install reactive handlers into client bridge via merge:
   - :open-reactive-screen  — block GUI screen dispatch
   - :reactive-overlay-build — HUD overlay runtime construction
   - :reactive-overlay-update — HUD overlay per-frame signal update"
  []
  (bridge/merge-client-bridge!
    {:open-reactive-screen reactive-screen-handler
     :reactive-overlay-build reactive-overlay/build-overlay-runtime
     :reactive-overlay-update reactive-overlay/update-overlay-signals!
     :reactive-overlay-mode-switch! reactive-overlay/on-mode-switch-key-state!})
  (log/debug "Reactive bridges installed (screen + overlay)"))

(ns cn.li.mcmod.gui.handler
  "GUI handler protocol, platform handler registration, and handler runtime."
  (:require [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.gui.registry :as gui-registry]
            [cn.li.mcmod.platform.world :as pworld]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

;; Expected map keys for IGuiHandler:
;; - :get-server-container (fn [gui-id player world pos] -> container or nil)
;; - :get-client-gui (fn [gui-id player world pos] -> screen or nil)

;; Wrapper functions

(defn get-server-container
  [handler gui-id player world pos]
  ((:get-server-container handler) gui-id player world pos))

(defn get-client-gui
  [handler gui-id player world pos]
  ((:get-client-gui handler) gui-id player world pos))

(defn get-gui-config
  "Return true when gui-id is registered."
  [gui-id]
  (gui-registry/has-gui-id? gui-id))

(defn make-registry-gui-handler
  [container-runtime]
  {:container-runtime container-runtime
   :get-server-container (fn [gui-id player world pos]
                           (let [tile-entity (pworld/get-tile-entity world pos)
                                 cfg? (get-gui-config gui-id)
                                 container-fn (gui-registry/get-container-fn gui-id)]
                             (if (and tile-entity cfg? container-fn)
                               (do
                                 (log/debug "Creating container for player"
                                            (entity/player-get-name player)
                                            "gui" gui-id)
                                 (container-fn tile-entity player))
                               (do
                                 (when-not cfg?
                                   (log/warn "Unknown GUI ID:" gui-id))
                                 (when (and cfg? (nil? container-fn))
                                   (log/warn "Missing :container-fn for GUI ID:" gui-id))
                                 nil))))
   :get-client-gui (fn [gui-id player world pos]
                     (let [tile-entity (pworld/get-tile-entity world pos)
                           cfg? (get-gui-config gui-id)
                           container-fn (gui-registry/get-container-fn gui-id)
                           screen-fn (gui-registry/get-screen-fn gui-id)]
                       (if (and tile-entity cfg? container-fn screen-fn)
                         (do
                           (log/debug "Creating GUI for player"
                                      (entity/player-get-name player)
                                      "gui" gui-id)
                           (let [container (container-fn tile-entity player)]
                             (screen-fn container nil player)))
                         (do
                           (when-not cfg?
                             (log/warn "Unknown GUI ID:" gui-id))
                           (when (and cfg? (nil? container-fn))
                             (log/warn "Missing :container-fn for GUI ID:" gui-id))
                           (when (and cfg? (nil? screen-fn))
                             (log/warn "Missing :screen-fn for GUI ID:" gui-id))
                           nil))))})

;; GUI Handler — stored in Framework [:service :gui-handler]

(def ^:private handler-path [:service :gui-handler])

(defn- get-gui-handler-instance []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom handler-path)
        (let [h (make-registry-gui-handler (container-state/installed-runtime))]
          (swap! fw-atom assoc-in handler-path h)
          h))
    (make-registry-gui-handler (container-state/installed-runtime))))

(defn get-gui-handler
  []
  (get-gui-handler-instance))

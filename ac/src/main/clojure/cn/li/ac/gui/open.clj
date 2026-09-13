(ns cn.li.ac.gui.open
  "AC GUI open-result construction.

  This namespace is AC-owned business glue: it validates AC GUI metadata and
  returns the platform-neutral GUI open result consumed by shared/platform
  Minecraft GUI code."
  (:require [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.mcmod.gui.handler :as gui-handler]
            [cn.li.mcmod.gui.registry :as gui-registry]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as pworld]
            [cn.li.mcmod.util.log :as log]))

(def get-gui-config
  "Get GUI spec/config by gui-id."
  gui-handler/get-gui-config)

(def get-gui-handler
  "Get the global GUI handler instance."
  gui-handler/get-gui-handler)

(defn open-gui
  "Build a platform-neutral GUI open result for a concrete GUI id."
  [player gui-id world pos]
  (log/debug "Opening GUI" gui-id "for player" (entity/player-get-name player) "at" pos)

  (when-not (get-gui-config gui-id)
    (log/warn "Invalid GUI ID:" gui-id)
    (throw (ex-info "Invalid GUI ID" {:gui-id gui-id})))

  (let [tile-entity (pworld/get-tile-entity world pos)]
    (when-not tile-entity
      (log/warn "No tile entity at position:" pos)
      (throw (ex-info "No tile entity at position" {:pos pos}))))

  {:gui-id gui-id
   :handler (get-gui-handler)
   :player player
   :world world
   :pos pos})

(defn open-gui-by-type
  "Build a platform-neutral GUI open result for an AC GUI type keyword."
  [player gui-type world pos]
  (if-let [gui-id (or (gui-registry/get-gui-id-for-type gui-type)
                      (gui-manifest/gui-id-for-type gui-type))]
    (open-gui player gui-id world pos)
    (throw (ex-info "No GUI registered for GUI type"
                    {:gui-type gui-type}))))

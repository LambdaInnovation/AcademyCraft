(ns cn.li.ac.wireless.gui.container.common
  "Shared container utility functions for wireless GUI system."
  (:require [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.util.log :as log]))

(defn get-slot-item
  "Get item from slot by accessing tile entity inventory."
  [container slot-index]
  (let [tile (:tile-entity container)]
    (if (map? tile)
      (let [inventory-atom (:inventory tile)]
        (when inventory-atom
          (get @inventory-atom slot-index)))
      (try
        (get-in (platform-be/get-custom-state tile) [:inventory slot-index])
        (catch Exception _ nil)))))

(defn set-slot-item!
  "Set item in slot by updating tile entity inventory."
  [container slot-index item-stack]
  (let [tile (:tile-entity container)]
    (if (map? tile)
      (let [inventory-atom (:inventory tile)]
        (when inventory-atom
          (swap! inventory-atom assoc slot-index item-stack)))
      (try
        (let [default-state (or (platform-be/get-custom-state tile) {})]
          (machine-runtime/commit-transform! tile default-state
                                           #(assoc-in % [:inventory slot-index] item-stack)))
        (catch Exception _ nil)))))

(defn still-valid?
  "Check if container is still valid for player based on distance."
  [container player]
  (let [tile (:tile-entity container)
        raw-pos (try (pos/block-pos tile) (catch Exception _ nil))
        max-distance 8.0]
    (and (= player (:player container))
         raw-pos
         (let [bx (double (or (try (pos/pos-x raw-pos) (catch Exception _ nil))
                              (:x raw-pos)))
               by (double (or (try (pos/pos-y raw-pos) (catch Exception _ nil))
                              (:y raw-pos)))
               bz (double (or (try (pos/pos-z raw-pos) (catch Exception _ nil))
                              (:z raw-pos)))]
           (< (entity/entity-distance-to-sqr player (+ bx 0.5) by (+ bz 0.5))
              (* max-distance max-distance))))))

(defn reset-container-atoms!
  "Reset container atoms to defaults."
  [& atom-defaults]
  (doseq [[atom-ref default-val] atom-defaults]
    (reset! atom-ref default-val))
  nil)

(defn get-slot-item-be
  "Get item from a ScriptedBlockEntity slot via customState."
  [container slot-index]
  (let [tile (:tile-entity container)]
    (try
      (get-in (platform-be/get-custom-state tile) [:inventory slot-index])
      (catch Exception _ nil))))

(defn set-slot-item-be!
  "Set item in a ScriptedBlockEntity slot via customState.
  - default-state: map used when customState is nil
  - post-write:    (fn [state]) -> state', applied after assoc-in
  - options:       commit options for the scripted BE state"
  ([container slot-index item-stack default-state post-write]
   (set-slot-item-be! container slot-index item-stack default-state post-write {}))
  ([container slot-index item-stack default-state post-write {:keys [sync-client?]
                                                               :or {sync-client? false}}]
  (let [tile (:tile-entity container)]
    (try
      (machine-runtime/commit-transform! tile default-state
                                         (fn [state]
                                           (-> state
                                               (assoc-in [:inventory slot-index] item-stack)
                                               post-write))
                                         :sync-client? sync-client?)
      (catch Exception e
        (log/stacktrace "set-slot-item-be! failed" e))))))

(defn get-tile-state
  "Get the current Clojure state map from a tile entity.
  Returns the map itself for state-map fixtures, calls getCustomState for BEs.
  Returns nil if tile is nil."
  [tile]
  (when tile
    (if (map? tile)
      tile
      (try (platform-be/get-custom-state tile) (catch Exception _ {})))))

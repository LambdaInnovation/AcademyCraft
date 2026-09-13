(ns cn.li.ac.block.machine.registration
  "Unified scripted machine registration: tile, container, capabilities, block, hooks."
  (:require [cn.li.ac.registry.hooks :as hooks]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.block.dsl :as bdsl]
            [cn.li.mcmod.block.tile-dsl :as tdsl]
            [cn.li.mcmod.block.tile-kind :as tile-kind]
            [cn.li.mcmod.capability.registry :as cap-registry]
            [cn.li.mcmod.util.log :as log]))

(defn register-tile-spec!
  [{:keys [id registry-name blocks impl tile-kind tick-fn read-nbt-fn write-nbt-fn
           container capability-keys]}]
  (tdsl/register-tile!
    (tdsl/create-tile-spec
      id
      (cond-> {:registry-name registry-name
               :impl (or impl :scripted)
               :blocks blocks}
        tile-kind (assoc :tile-kind tile-kind)
        tick-fn (assoc :tick-fn tick-fn)
        read-nbt-fn (assoc :read-nbt-fn read-nbt-fn)
        write-nbt-fn (assoc :write-nbt-fn write-nbt-fn)
        container (assoc :container container)
        capability-keys (assoc :capability-keys capability-keys)))))

(defn register-tile-kind!
  [{:keys [tile-kind tick-fn read-nbt-fn write-nbt-fn]}]
  (tile-kind/register-tile-kind!
    tile-kind
    (cond-> {}
      tick-fn (assoc :tick-fn tick-fn)
      read-nbt-fn (assoc :read-nbt-fn read-nbt-fn)
      write-nbt-fn (assoc :write-nbt-fn write-nbt-fn))))

(defn declare-and-register-capabilities!
  "capabilities: [{:key :phase-generator :interface IWirelessGenerator :factory fn} ...]
  tile-ids: vector of tile id strings — returns capability key set for tile specs."
  [capabilities tile-ids]
  (doseq [{:keys [key interface factory]} capabilities]
    (when-not (cap-registry/get-capability-entry key)
      (cap-registry/declare-capability! key interface factory))
    (doseq [tile-id tile-ids]
      (tdsl/register-tile-capability-keys! tile-id key)))
  (set (map #(get % :key) capabilities)))

(defn register-blocks!
  [block-specs]
  (doseq [spec block-specs]
    (bdsl/register-block! spec)))

(defn register-machine-hooks!
  [{:keys [network-handler client-renderer]}]
  (when network-handler (hooks/register-network-handler! network-handler))
  (when client-renderer (hooks/register-client-renderer! client-renderer)))

(defn init-machine!
  "Run one-time machine init behind a Framework-scoped exactly-once guard.

  machine-spec keys:
  - :guard keyword — unique install-key for install/framework-once!
    (each caller passes its own ::init, ns-qualified so it's unique per file)
  - :log-label string
  - :before (fn []) optional
  - :tile-kind optional map for register-tile-kind!
  - :tiles vector of tile spec maps
  - :capabilities optional
  - :tile-ids for capability/container binding (defaults from :tiles :id)
  - :containers map tile-id -> container fns
  - :blocks vector of block specs (bdsl/create-block-spec results)
  - :network-handler fn
  - :client-renderer symbol"
  [{:keys [guard log-label before after tile-kind tiles capabilities tile-ids containers blocks
           network-handler client-renderer]}]
  (install/framework-once! guard
    (fn []
      (when before (before))
      (when tile-kind (register-tile-kind! tile-kind))
      (let [ids (or tile-ids (mapv #(get % :id) tiles))
            capability-tile-ids (set ids)
            cap-keys (when (seq capabilities)
                       (declare-and-register-capabilities! capabilities ids))]
        (doseq [tile tiles]
          (register-tile-spec!
            (assoc tile
                   :container (get containers (:id tile))
                   ;; :tile-ids selects which of a multi-tile machine's tiles
                   ;; actually carry the capability — the wind generator holds
                   ;; its energy in the base, not the rotor head or the pillar.
                   ;; declare-and-register-capabilities! already targets those
                   ;; ids, but register-tile! then overwrites the whole spec,
                   ;; so the keys have to be scoped here too or every tile of
                   ;; the machine ends up advertising the capability and the
                   ;; wireless network will happily resolve a vblock against a
                   ;; part that stores nothing.
                   :capability-keys (or (:capability-keys tile)
                                        (when (contains? capability-tile-ids (:id tile))
                                          cap-keys))))))
      (when blocks (register-blocks! blocks))
      (when after (after))
      (register-machine-hooks! {:network-handler network-handler
                                :client-renderer client-renderer})
      (log/debug (str "Initialized " (or log-label "machine")))))
  nil)

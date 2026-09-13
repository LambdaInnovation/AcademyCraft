(ns cn.li.ac.wireless.config
  "Canonical wireless configuration descriptors and typed getters."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]))

(def descriptors
  [{:key :network-update-interval-ticks
    :section :network
    :path "network.update-interval-ticks"
    :type :int
    :default 1
    :min 1
    :max 1200
    :comment "Ticks between wireless network energy balance passes. Upstream
      WirelessNet.tick() balances every server tick (WirelessSystem.onServerTick
      -> WiWorldData.tick -> net.tick); 40 made node-to-node transfer ~0.75 IF/t
      instead of upstream's ~30 IF/t — effectively invisible."}
   {:key :network-buffer-max
    :section :network
    :path "network.buffer-max"
    :type :double
    :default 2000.0
    :min 0.0
    :max 1000000.0
    :comment "Maximum transit buffer stored by a wireless network."}
   {:key :search-node-range
    :section :search
    :path "search.node-search-range"
    :type :double
    :default 20.0
    :min 0.0
    :max 4096.0
    :comment "Search radius for generators/receivers querying nearby nodes."}
   {:key :search-max-results
    :section :search
    :path "search.max-results"
    :type :int
    :default 100
    :min 1
    :max 10000
    :comment "Maximum results returned by wireless range searches."}
   {:key :node-basic-max-energy
    :section :node.basic
    :path "node.basic.max-energy"
    :type :int
    :default 15000
    :min 0
    :max 100000000
    :comment "Basic node max energy storage."}
   {:key :node-basic-bandwidth
    :section :node.basic
    :path "node.basic.bandwidth"
    :type :int
    :default 150
    :min 0
    :max 1000000
    :comment "Basic node transfer bandwidth."}
   {:key :node-basic-range
    :section :node.basic
    :path "node.basic.range"
    :type :double
    :default 9.0
    :min 0.0
    :max 4096.0
    :comment "Basic node wireless range in blocks."}
   {:key :node-basic-capacity
    :section :node.basic
    :path "node.basic.capacity"
    :type :int
    :default 5
    :min 0
    :max 1024
    :comment "Basic node connection capacity."}
   {:key :node-standard-max-energy
    :section :node.standard
    :path "node.standard.max-energy"
    :type :int
    :default 50000
    :min 0
    :max 100000000
    :comment "Standard node max energy storage."}
   {:key :node-standard-bandwidth
    :section :node.standard
    :path "node.standard.bandwidth"
    :type :int
    :default 300
    :min 0
    :max 1000000
    :comment "Standard node transfer bandwidth."}
   {:key :node-standard-range
    :section :node.standard
    :path "node.standard.range"
    :type :double
    :default 12.0
    :min 0.0
    :max 4096.0
    :comment "Standard node wireless range in blocks."}
   {:key :node-standard-capacity
    :section :node.standard
    :path "node.standard.capacity"
    :type :int
    :default 10
    :min 0
    :max 1024
    :comment "Standard node connection capacity."}
   {:key :node-advanced-max-energy
    :section :node.advanced
    :path "node.advanced.max-energy"
    :type :int
    :default 200000
    :min 0
    :max 100000000
    :comment "Advanced node max energy storage."}
   {:key :node-advanced-bandwidth
    :section :node.advanced
    :path "node.advanced.bandwidth"
    :type :int
    :default 900
    :min 0
    :max 1000000
    :comment "Advanced node transfer bandwidth."}
   {:key :node-advanced-range
    :section :node.advanced
    :path "node.advanced.range"
    :type :double
    :default 19.0
    :min 0.0
    :max 4096.0
    :comment "Advanced node wireless range in blocks."}
   {:key :node-advanced-capacity
    :section :node.advanced
    :path "node.advanced.capacity"
    :type :int
    :default 20
    :min 0
    :max 1024
    :comment "Advanced node connection capacity."}
   {:key :node-sync-interval
    :section :performance
    :path "performance.node-sync-interval"
    :type :int
    :default 20
    :min 1
    :max 1200
    :comment "Ticks between node network check and GUI sync passes."}
   {:key :matrix-capacity-per-core-level
    :section :matrix
    :path "matrix.capacity-per-core-level"
    :type :int
    :default 8
    :min 0
    :max 512
    :comment "Connected node capacity granted by each matrix core level."}
   {:key :matrix-bandwidth-factor
    :section :matrix
    :path "matrix.bandwidth-factor"
    :type :int
    :default 60
    :min 0
    :max 100000
    :comment "Matrix bandwidth uses coreLevel^2 * bandwidth-factor."}
   {:key :matrix-range-base
    :section :matrix
    :path "matrix.range-base"
    :type :double
    :default 24.0
    :min 0.0
    :max 4096.0
    :comment "Matrix range uses range-base * sqrt(coreLevel)."}
   {:key :matrix-gui-sync-interval
    :section :performance
    :path "performance.matrix-gui-sync-interval"
    :type :int
    :default 15
    :min 1
    :max 1200
    :comment "Ticks between matrix GUI sync broadcasts."}
   {:key :stale-device-cooldown-ticks
    :section :performance
    :path "performance.stale-device-cooldown-ticks"
    :type :int
    :default 20
    :min 1
    :max 200
    :comment "Ticks before a stale wireless device (capability gone) is removed from a connection."}
   {:key :validate-interval-ticks
    :section :performance
    :path "performance.validate-interval-ticks"
    :type :int
    :default 20
    :min 1
    :max 1200
    :comment "Ticks between node-connection integrity validation passes."}
   {:key :sweep-interval-ticks
    :section :performance
    :path "performance.sweep-interval-ticks"
    :type :int
    :default 600
    :min 20
    :max 24000
    :comment "Ticks between periodic sweeps unregistering disposed networks/connections."}
   {:key :world-load-rebuild-batch-size
    :section :performance
    :path "performance.world-load-rebuild-batch-size"
    :type :int
    :default 200
    :min 1
    :max 100000
    :comment "Max networks/connections rebuilt from NBT per tick after world load; the rest queue across subsequent ticks instead of rebuilding the whole topology in one tick."}])

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn cfg
  "Full config snapshot map. Tick paths call this once per tick and pass the
  snapshot down via ctx instead of re-merging per getter call."
  []
  (merge default-values
         (config-reg/get-config-values config-common/wireless-domain)))

(defn update-interval-ticks []
  (:network-update-interval-ticks (cfg)))

(defn buffer-max []
  (:network-buffer-max (cfg)))

(defn node-search-range []
  (:search-node-range (cfg)))

(defn max-results []
  (:search-max-results (cfg)))

(defn- tier-key
  [tier suffix]
  (keyword (str "node-" (name tier) "-" suffix)))

(defn max-energy [tier]
  (get (cfg) (tier-key tier "max-energy")))

(defn bandwidth [tier]
  (get (cfg) (tier-key tier "bandwidth")))

(defn range-blocks [tier]
  (get (cfg) (tier-key tier "range")))

(defn capacity [tier]
  (get (cfg) (tier-key tier "capacity")))

(defn node-config [tier]
  {:max-energy (max-energy tier)
   :bandwidth (bandwidth tier)
   :range (range-blocks tier)
   :capacity (capacity tier)})

(defn node-types []
  {:basic (node-config :basic)
   :standard (node-config :standard)
   :advanced (node-config :advanced)})

(defn sync-interval []
  (:node-sync-interval (cfg)))

(defn capacity-per-core-level []
  (:matrix-capacity-per-core-level (cfg)))

(defn bandwidth-factor []
  (:matrix-bandwidth-factor (cfg)))

(defn range-base []
  (:matrix-range-base (cfg)))

(defn gui-sync-interval []
  (:matrix-gui-sync-interval (cfg)))

(defn stale-device-cooldown-ticks []
  (:stale-device-cooldown-ticks (cfg)))

(defn validate-interval-ticks []
  (:validate-interval-ticks (cfg)))

(defn sweep-interval-ticks []
  (:sweep-interval-ticks (cfg)))

(defn world-load-rebuild-batch-size []
  (:world-load-rebuild-batch-size (cfg)))

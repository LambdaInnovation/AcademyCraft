(ns cn.li.ac.block.developer.config
  "Developer configuration — values aligned with AcademyCraft `DeveloperType`
  energy / stimulation pacing (gameplay constants, not 1:1 Java port)."
  (:require [cn.li.ac.ability.domain.developer :as developer]
            [cn.li.ac.config.common :as config-common]))

;; Structure footprint: `cn.li.ac.block.developer.block/developer-multiblock-positions`

(def default-validate-interval
  "Ticks between multi-block structure validation."
  100)

(def ^:private default-wireless-bandwidth-by-tier
  ;; Upstream TileReceiverBase: NORMAL latency 100, ADVANCED latency 300 IF/t.
  {:normal 100.0
   :advanced 300.0})

(def descriptors
  (vec
    (concat
      [{:key :developer-validate-interval
        :section :devices.developer.performance
        :path "devices.developer.performance.validate-interval"
        :type :int
        :default default-validate-interval
        :min 1
        :max 1200
        :comment "Ticks between Developer multiblock structure validation checks."}]
      (mapcat
        (fn [tier]
          (let [{:keys [energy cps tps]} (developer/developer-spec tier)
                prefix (str "devices.developer.tiers." (name tier))
                key-prefix (str "developer-" (name tier))]
            [{:key (keyword (str key-prefix "-max-energy"))
              :section (keyword (str "devices.developer.tiers." (name tier)))
              :path (str prefix ".max-energy")
              :type :double
              :default energy
              :min 0.0
              :max 100000000.0
              :comment (str "Developer " (name tier) " tier internal energy buffer in IF.")}
             {:key (keyword (str key-prefix "-energy-per-stimulation"))
              :section (keyword (str "devices.developer.tiers." (name tier)))
              :path (str prefix ".energy-per-stimulation")
              :type :double
              :default cps
              :min 0.0
              :max 1000000.0
              :comment (str "Developer " (name tier) " tier IF consumed per stimulation step.")}
             {:key (keyword (str key-prefix "-stimulation-interval-ticks"))
              :section (keyword (str "devices.developer.tiers." (name tier)))
              :path (str prefix ".stimulation-interval-ticks")
              :type :int
              :default tps
              :min 1
              :max 1200
              :comment (str "Developer " (name tier) " tier ticks between stimulation steps.")}
             {:key (keyword (str key-prefix "-wireless-bandwidth"))
              :section (keyword (str "devices.developer.tiers." (name tier)))
              :path (str prefix ".wireless-bandwidth")
              :type :double
              :default (get default-wireless-bandwidth-by-tier tier 100.0)
              :min 0.0
              :max 100000000.0
              :comment (str "Developer " (name tier) " tier wireless receiver bandwidth in IF/t.")}]))
        [:normal :advanced]))))

(def default-values
  (into {} (map #(vector (get % :key) (get % :default)) descriptors)))

(defn- cfg []
  (merge default-values
         (config-common/ability-devices-config)))

(defn validate-interval []
  (:developer-validate-interval (cfg)))

(defn- tier-spec->config
  [tier]
  (let [tier (keyword tier)
        values (cfg)
        key-prefix (str "developer-" (name tier))]
    {:max-energy (get values (keyword (str key-prefix "-max-energy")))
     :energy-per-stimulation (get values (keyword (str key-prefix "-energy-per-stimulation")))
     :stimulation-interval-ticks (get values (keyword (str key-prefix "-stimulation-interval-ticks")))
     :wireless-bandwidth (get values (keyword (str key-prefix "-wireless-bandwidth")))}))

(defn- tier-table
  "Per-tier limits (NORMAL / ADVANCED from classic AC).
  - :max-energy — buffer size (IF)
  - :energy-per-stimulation — IF consumed each stimulation when developing
  - :stimulation-interval-ticks — ticks between stimulations (classic `tps`)
  - :wireless-bandwidth — IF/tick cap for `IWirelessReceiver` inject path"
  []
  {:normal (tier-spec->config :normal)
   :advanced (tier-spec->config :advanced)})

(defn tier-config
  "Keyword tier `:normal` / `:advanced` → config map."
  [tier]
  (let [table (tier-table)]
    (get table (keyword tier) (:normal table))))

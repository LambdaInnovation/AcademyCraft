(ns cn.li.mcmod.spi.keybinding-registry
  "Neutral keybinding configuration registry.

  Problem: the Forge loader key mapping adapter was directly
  calling `cn.li.ac.input-ids/get-input-ids` — a static dependency on
  a business content module.

  Solution: Content modules register their keybinding configurations
  into this neutral registry. Platform key-mapping adapters read from
  the registry without knowing which content modules exist."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]
            [cn.li.mcmod.util.log :as log]))

(defn register-keybinding-configs!
  "Register keybinding configurations from a content module.

  configs must be a map of {input-id -> config-map} where each
  config-map has keys like :scheme, :input-id, :key-mapping.

  Called by content modules (e.g. AC) during bootstrap."
  [module-label configs]
  (assert (map? configs)
          (str "keybinding configs must be map?, got " (type configs)))
  (let [fw-atom (fw/fw-atom)]
    (doseq [[input-id config] configs]
      (registry/register! fw-atom :keybinds input-id config))
    (log/info "Keybinding configs registered from" module-label
              "(+" (count configs) "bindings, total:"
              (count (get-in @fw-atom [:registry :keybinds])) ")"))
  nil)

(defn get-all-keybinding-configs
  "Returns all registered keybinding configurations.
  Called by platform key-mapping adapters."
  []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom [:registry :keybinds])
    {}))

(defn registered-keybinding-count
  "Returns the total count of registered keybindings (for diagnostics)."
  []
  (count (get-all-keybinding-configs)))

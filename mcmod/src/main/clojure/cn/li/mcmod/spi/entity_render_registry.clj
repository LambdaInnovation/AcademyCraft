(ns cn.li.mcmod.spi.entity-render-registry
  "Neutral registry for content-owned entity render namespaces."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]
            [cn.li.mcmod.util.log :as log]))

(defn register-entity-render-ns!
  "Register a content render namespace for an entity render profile."
  [hook-id render-ns]
  (assert (string? render-ns)
          (str "render-ns must be a string, got " (type render-ns)))
  (registry/register! (fw/fw-atom) :hooks [::entity-render (str hook-id)] render-ns)
  (log/debug "Entity render namespace registered:" hook-id "->" render-ns)
  nil)

(defn get-entity-render-ns
  "Returns the registered render namespace for a render profile."
  [hook-id]
  (when-let [fw-atom (fw/fw-atom)]
    (registry/get-spec fw-atom :hooks [::entity-render (str hook-id)])))

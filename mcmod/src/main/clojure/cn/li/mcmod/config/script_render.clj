(ns cn.li.mcmod.config.script-render
  "Shared ScriptRender runtime config values.

  Platform layers may populate values through config.registry."
  (:require [cn.li.mcmod.config.registry :as cfg]))

(def domain :client/script-render)

(def ^:private descriptors
  [{:key :script-render-enabled?
    :type :boolean
    :default true
    :doc "Global ScriptRender enable switch."}
   {:key :disabled-renderer-ids
    :type :set
    :default #{}
    :doc "Renderer IDs forced to native fallback."}])

(defn init-descriptors!
  []
  (cfg/register-config-descriptors! domain descriptors)
  (cfg/ensure-default-values! domain (cfg/descriptor-default-values domain))
  nil)

(defn script-render-enabled?
  []
  (boolean (cfg/get-config-value domain :script-render-enabled? true)))

(defn disabled-renderer-ids
  []
  (let [value (cfg/get-config-value domain :disabled-renderer-ids #{})]
    (cond
      (set? value) value
      (sequential? value) (set value)
      :else #{})))

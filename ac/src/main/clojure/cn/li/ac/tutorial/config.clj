(ns cn.li.ac.tutorial.config
  "Tutorial system configuration descriptors and getters.

  Follows the same pattern as cn.li.ac.ability.config for registering
  config descriptors with the mcmod config registry."
  (:require [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.util.log :as log]))

(def default-values
  {:give-cloud-terminal true
   :heads-or-tails false})

(def descriptors
  [{:key :give-cloud-terminal
    :path "tutorial.give-cloud-terminal"
    :section :tutorial
    :type :boolean
    :default (:give-cloud-terminal default-values)
    :comment "Whether the tutorial item (MisakaCloud Terminal) is automatically given to new players on first login."}
   {:key :heads-or-tails
    :path "tutorial.heads-or-tails"
    :section :tutorial
    :type :boolean
    :default (:heads-or-tails default-values)
    :comment "Whether the Heads or Tails coin flip game is enabled."}])

(defn init-config!
  "Ensure tutorial defaults are present in the shared config registry."
  []
  (config-reg/register-config-descriptors! config-common/tutorial-domain descriptors)
  (config-reg/ensure-default-values! config-common/tutorial-domain default-values)
  (log/debug "Initialized tutorial config descriptors" {:domain config-common/tutorial-domain})
  nil)

(defn give-cloud-terminal-enabled?
  "Return true if the cloud terminal auto-give feature is enabled (default: true).
  Matches original AcademyCraft's `giveCloudTerminal` config flag."
  []
  (boolean (config-reg/get-config-value config-common/tutorial-domain :give-cloud-terminal true)))

(defn heads-or-tails-enabled?
  []
  (boolean (config-reg/get-config-value config-common/tutorial-domain :heads-or-tails false)))

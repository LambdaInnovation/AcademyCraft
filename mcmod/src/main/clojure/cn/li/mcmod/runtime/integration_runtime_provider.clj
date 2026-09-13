(ns cn.li.mcmod.runtime.integration-runtime-provider
  "Neutral SPI factory for integration and tutorial domain operations."
  (:require [cn.li.mcmod.hooks.messages :as messages]
            [cn.li.mcmod.hooks.tutorial-events :as tutorial-events]
            [cn.li.mcmod.integration.energy-conversion :as energy-conversion]
            [cn.li.mcmod.integration.energy-hooks :as energy-hooks]
            [cn.li.mcmod.integration.runtime-hooks :as integration-hooks]))

(defn runtime-provider [_]
  {:content-to-fe #'energy-conversion/content-to-fe
   :fe-to-content #'energy-conversion/fe-to-content
   :validate-conversion-rate #'energy-conversion/validate-conversion-rate
   :forge-energy-conversion-rate #'energy-hooks/forge-energy-conversion-rate
   :ic2-energy-conversion-rate #'energy-hooks/ic2-energy-conversion-rate
   :jei-get-all-categories #'integration-hooks/jei-get-all-categories
   :jei-get-recipes #'integration-hooks/jei-get-recipes
   :jei-format-recipe #'integration-hooks/jei-format-recipe
   :get-jei-nbt-subtype-item-ids #'integration-hooks/get-jei-nbt-subtype-item-ids
   :msg-id #'messages/msg-id
   :on-item-event! #'tutorial-events/on-item-event!
   :process-pending-activations! #'tutorial-events/process-pending-activations!
   :register-tutorial-activated-hook! #'tutorial-events/register-tutorial-activated-hook!})

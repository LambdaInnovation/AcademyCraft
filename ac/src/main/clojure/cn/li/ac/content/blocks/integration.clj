(ns cn.li.ac.content.blocks.integration
  "Integration blocks content loader - energy converters and external mod support"
  (:require [cn.li.ac.content.block-loader-core :as block-loader]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.integration.block.energy-converter.init :as ec-init]
            [cn.li.ac.integration.block.energy-converter.gui-reactive :as ec-gui-reactive]
            [cn.li.mcmod.runtime.install :as install]))

(def ^:private integration-block-spec
  {:label :integration
   :init-entries [(fn []
                    (log/debug "Loading integration blocks...")
                    (ec-init/load-converters!))
                  (fn []
                    (log/debug "Initializing integration block definitions...")
                    (ec-init/init-converters!))
                  ec-gui-reactive/register-converter-guis-reactive!]
   :post-init-entries [(fn []
                         (log/debug "Integration block definitions initialized"))
                       (fn []
                         (log/debug "Loaded integration blocks content (4 converters)"))]})

(defn init-integration-blocks!
  []
  (install/framework-once! ::integration-blocks-installed?
  (fn []
    (block-loader/load-block-category! integration-block-spec))))
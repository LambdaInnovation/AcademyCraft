(ns cn.li.ac.content.ability.electromaster.thunder-bolt-fx
  "Client FX for Thunder Bolt: zigzag electric arc effects."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :thunder-bolt-strike
     :sound-id (modid/namespaced-path "em.arc_strong")
     :sound-volume 0.6
     :arc-life 20
     :arc-pattern :strong
     :aoe-points? true
     :hand-origin? true
     :channels [{:topic :thunder-bolt/fx-perform :mode :perform}]}))

(arc-beam/def-arc-beam-fx :thunder-bolt-strike)

(ns cn.li.ac.content.ability.teleporter.teleporter-crit-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :teleporter-crit
     :initial-state (fn [] {})
     :channels {:crit-hit {:topic :teleporter/fx-crit-hit :mode :crit-hit}}}))

(arc-beam/def-arc-beam-fx :teleporter-crit)

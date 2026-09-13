(ns cn.li.ac.content.ability.teleporter.penetrate-teleport-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :penetrate-teleport
     :initial-state (fn [] {:fx-state {}})
     :channels {:start {:topic :penetrate-teleport/fx-start :mode :start}
                :update {:topic :penetrate-teleport/fx-update :mode :update}
                :perform {:topic :penetrate-teleport/fx-perform :mode :perform}
                :end {:topic :penetrate-teleport/fx-end :mode :end}}}))

(arc-beam/def-arc-beam-fx :penetrate-teleport)

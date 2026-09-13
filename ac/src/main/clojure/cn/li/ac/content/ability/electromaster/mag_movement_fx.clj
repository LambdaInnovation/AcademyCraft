(ns cn.li.ac.content.ability.electromaster.mag-movement-fx
  (:require [cn.li.ac.ability.client.effects.beam-ops :as beam-ops]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :mag-movement
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :mag-movement/fx-start :mode :start}
                :update {:topic :mag-movement/fx-update :mode :update}
                :end {:topic :mag-movement/fx-end :mode :end}}}))

(arc-beam/def-arc-beam-fx :mag-movement)
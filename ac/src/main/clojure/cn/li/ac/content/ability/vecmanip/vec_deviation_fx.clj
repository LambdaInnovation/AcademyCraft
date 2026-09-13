(ns cn.li.ac.content.ability.vecmanip.vec-deviation-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :vec-deviation
     :initial-state (fn [] {:effect-state {} :wave-effects {}})
     :channels {:start {:topic :vec-deviation/fx-start :mode :start}
                :end {:topic :vec-deviation/fx-end :mode :end}
                :stop-entity {:topic :vec-deviation/fx-stop-entity :mode :stop-entity
                              :level-payload (fn [_ _ p]
                                               {:x (double (or (:x p) 0.0))
                                                :y (double (or (:y p) 0.0))
                                                :z (double (or (:z p) 0.0))
                                                :marked? (boolean (:marked? p))
                                                :entity-uuid (:entity-uuid p)
                                                :yaw-rad (:yaw-rad p)
                                                :pitch-rad (:pitch-rad p)})}
                :play {:topic :vec-deviation/fx-play :mode :play
                       :level-payload (fn [_ _ p]
                                        {:x (double (or (:x p) 0.0))
                                         :y (double (or (:y p) 0.0))
                                         :z (double (or (:z p) 0.0))})}}}))

(arc-beam/def-arc-beam-fx :vec-deviation)
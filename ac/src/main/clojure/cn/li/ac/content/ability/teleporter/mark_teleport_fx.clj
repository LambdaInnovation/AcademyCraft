(ns cn.li.ac.content.ability.teleporter.mark-teleport-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :mark-teleport
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :mark-teleport/fx-start :mode :start}
                :update {:topic :mark-teleport/fx-update :mode :update
                         :level-payload (fn [_ _ p] {:target (:target p)
                                                     :dist (double (or (:dist p) 0.0))
                                                     :distance (double (or (:distance p) 0.0))})}
                :end {:topic :mark-teleport/fx-end :mode :end}
                :perform {:topic :mark-teleport/fx-perform :mode :perform
                          :level-payload (fn [_ _ p] {:target (:target p)
                                                     :dist (double (or (:dist p) 0.0))
                                                     :distance (double (or (:distance p) 0.0))})}}}))

(arc-beam/def-arc-beam-fx :mark-teleport)
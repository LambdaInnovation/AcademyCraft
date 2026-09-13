(ns cn.li.ac.content.ability.teleporter.threatening-teleport-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(defn- target-payload [_ctx-id _channel p]
  {:target-x (or (:target-x p) (:drop-x p))
   :target-y (or (:target-y p) (:drop-y p))
   :target-z (or (:target-z p) (:drop-z p))
   :start-x (:start-x p)
   :start-y (:start-y p)
   :start-z (:start-z p)
   :hit? (boolean (or (:hit? p) (:attacked? p)))
   :target-uuid (:target-uuid p)
   :target-width (double (or (:target-width p) 0.5))
   :target-height (double (or (:target-height p) 0.0))})

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :threatening-teleport
     :initial-state (fn [] {:fx-state {}})
     :channels {:start {:topic :threatening-teleport/fx-start :mode :start :level-payload target-payload}
                :update {:topic :threatening-teleport/fx-update :mode :update :level-payload target-payload}
                :perform {:topic :threatening-teleport/fx-perform :mode :perform :level-payload target-payload}
                :end {:topic :threatening-teleport/fx-end :mode :end}}}))

(arc-beam/def-arc-beam-fx :threatening-teleport)

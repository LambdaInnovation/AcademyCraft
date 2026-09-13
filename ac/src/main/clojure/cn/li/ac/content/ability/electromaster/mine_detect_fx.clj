(ns cn.li.ac.content.ability.electromaster.mine-detect-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :mine-detect
     :initial-state (fn [] {:effect-state {}})
     :channels {:perform {:topic :mine-detect/fx-perform :mode :perform
                         :level-payload (fn [_ _ p]
                                          {:range (:range p)
                                           :advanced? (:advanced? p)
                                           :life-ticks (:life-ticks p)
                                           :rescan-interval (:rescan-interval p)})}
                :end {:topic :mine-detect/fx-end :mode :end}}}))

(arc-beam/def-arc-beam-fx :mine-detect)
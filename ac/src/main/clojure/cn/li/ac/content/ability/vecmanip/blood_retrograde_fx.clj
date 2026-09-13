(ns cn.li.ac.content.ability.vecmanip.blood-retrograde-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :blood-retrograde
     :initial-state (fn [] {:effect-state {} :splashes {} :sprays {}})
     :channels {:start {:topic :blood-retrograde/fx-start :mode :start}
								:update {:topic :blood-retrograde/fx-update :mode :update
												 :level-payload (fn [_ _ p]
																				{:ticks (long (or (:ticks p) 0))
																				 :charge-ratio (double (or (:charge-ratio p) 0.0))})}
								:end {:topic :blood-retrograde/fx-end :mode :end
											:level-payload (fn [_ _ p]
																			 {:performed? (boolean (:performed? p))})}
								:perform {:topic :blood-retrograde/fx-perform :mode :perform
													:level-payload (fn [_ _ p]
																					 {:sound-pos (:sound-pos p)
																						:splashes (:splashes p)
																						:sprays (:sprays p)})}}}))

(arc-beam/def-arc-beam-fx :blood-retrograde)
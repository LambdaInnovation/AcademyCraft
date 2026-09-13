(ns cn.li.ac.content.ability.vecmanip.vec-accel-fx
  (:require [cn.li.ac.ability.client.effects.beam-ops :as beam-ops]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :vec-accel
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :vec-accel/fx-start :mode :start}
								:update {:topic :vec-accel/fx-update :mode :update
												 :level-payload (fn [_ _ p]
																				{:charge-ticks (long (or (:charge-ticks p) 0))
																				 :can-perform? (boolean (:can-perform? p))
																				 :look-dir (:look-dir p)
																				 :init-vel (:init-vel p)})}
								:perform {:topic :vec-accel/fx-perform :mode :perform}
								:end {:topic :vec-accel/fx-end :mode :end
											:level-payload (fn [_ _ p]
																			 {:performed? (boolean (:performed? p))})}}}))

(arc-beam/def-arc-beam-fx :vec-accel)
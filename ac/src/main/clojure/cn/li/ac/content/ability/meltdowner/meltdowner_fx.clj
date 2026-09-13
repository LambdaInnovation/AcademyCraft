(ns cn.li.ac.content.ability.meltdowner.meltdowner-fx
  (:require [cn.li.ac.ability.client.effects.beam-ops :as beam-ops]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.meltdowner :as meltdowner-impl]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :meltdowner
     :initial-state (fn [] {:effect-state {} :rays {}})
     ;; Charge camera zoom: per-frame query into the impl's eased offset.
     :fov-offset-fn (fn [player-uuid] (meltdowner-impl/current-fov-offset player-uuid))
     :channels {:start {:topic :meltdowner/fx-start :mode :start}
                :update {:topic :meltdowner/fx-update :mode :update
                         :level-payload (fn [_ _ p]
                                          {:ticks (long (or (:ticks p) 0))
                                           :charge-ratio (double (or (:charge-ratio p) 0.0))
                                           ;; The charge particles ring the caster.
                                           :caster-x (:caster-x p)
                                           :caster-y (:caster-y p)
                                           :caster-z (:caster-z p)})}
                :end {:topic :meltdowner/fx-end :mode :end
                      :level-payload (fn [_ _ p]
                                       {:performed? (boolean (:performed? p))})}
                :perform {:topic :meltdowner/fx-perform :mode :perform
                          :level-payload (fn [_ _ p]
                                           {:charge-ticks (int (or (:charge-ticks p) 20))
                                            :beam-length (double (or (:beam-length p) 30.0))
                                            :start (:start p)
                                            :end (:end p)})}
                :reflect {:topic :meltdowner/fx-reflect :mode :reflect
                          :level-payload (fn [_ _ p]
                                           {:start (:start p) :end (:end p)})}}}))

(arc-beam/def-arc-beam-fx :meltdowner)
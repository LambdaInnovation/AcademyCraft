(ns cn.li.ac.content.ability.electromaster.thunder-clap-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :thunder-clap
     :initial-state (fn [] {:effect-state {} :tails {} :impacts {}})
     :channels {:start {:topic :thunder-clap/fx-start :mode :start}
                :update {:topic :thunder-clap/fx-update :mode :update
                         :level-payload (fn [_ _ p]
                                          (cond-> {:ticks (long (or (:ticks p) 0))
                                                   :charge-ratio (double (or (:charge-ratio p) 0.0))
                                                   :target (get p :target)}
                                            (map? (:caster-pos p))
                                            (assoc :caster-pos (:caster-pos p))))}
                :perform {:topic :thunder-clap/fx-perform :mode :perform
                          :level-payload (fn [_ _ p]
                                           (cond-> {:performed? (boolean (:performed? p))
                                                    :ticks (long (or (:ticks p) (:charge-ticks p) 0))
                                                    :charge-ratio (double (or (:charge-ratio p) 0.0))
                                                    :target (get p :target)}
                                             (map? (:caster-pos p))
                                             (assoc :caster-pos (:caster-pos p))))}
                :end {:topic :thunder-clap/fx-end :mode :end
                      :level-payload (fn [_ _ p]
                                       (cond-> {:performed? (boolean (:performed? p))
                                                :ticks (long (or (:ticks p) (:charge-ticks p) 0))
                                                :charge-ratio (double (or (:charge-ratio p) 0.0))
                                                :target (get p :target)}
                                         (map? (:caster-pos p))
                                         (assoc :caster-pos (:caster-pos p))))}}}))

(arc-beam/def-arc-beam-fx :thunder-clap)

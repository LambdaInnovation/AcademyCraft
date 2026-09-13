(ns cn.li.ac.content.ability.electromaster.arc-gen-fx
  "Client FX for Arc-Gen: short electric arc beam and weak arc sound."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :arc-gen
     :sound-id (modid/namespaced-path "em.arc_weak")
     :sound-source :ambient
     :sound-volume 0.5
     :arc-life 10
     :arc-pattern :weak
     ;; Original spawns EntityArc at the caster's eye and then renders it
     ;; through ViewOptimize.fix, which shifts it into their hand.
     :hand-origin? true
     :channels [{:topic :arc-gen/fx-perform :mode :perform
                 :level-payload-fn (fn [_ _ p]
                                     {:start (:start p)
                                      :end (:end p)
                                      :hit-type (:hit-type p)
                                      :sound-pos (:sound-pos p)
                                      :source-player-id (:source-player-id p)})}]}))

(arc-beam/def-arc-beam-fx :arc-gen)

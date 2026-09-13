(ns cn.li.ac.content.ability.meltdowner.ray-barrage-fx
  "Client FX for ray-barrage: green tube rays rendered by the ray-barrage
  level effect (see fx-templates.arc-beam.impl.ray-barrage). The preray and
  barrage events carry the geometry (eye->target / silbarn position + aim)
  the original passed to its EntityBarrageRayPre / EntityMdRayBarrage."
  (:require [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(defn- preray-sound! [_ctx-id _channel _payload]
  (client-sounds/queue-current-sound-effect!
    {:type :sound :sound-id (modid/namespaced-path "md.ray_small") :volume 0.8 :pitch 1.0}))

(defn- barrage-sound! [_ctx-id _channel _payload]
  (client-sounds/queue-current-sound-effect!
    {:type :sound :sound-id (modid/namespaced-path "md.ray_small") :volume 0.5 :pitch 1.0}))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :ray-barrage
     :initial-state (fn [] {:beam-queue {}})
     :channels {:preray {:topic :ray-barrage/fx-preray
                         :mode :preray
                         :targets [:immediate :level]
                         :immediate-fn preray-sound!
                         :level-payload (fn [_ _ p]
                                          {:start (:start p)
                                           :end (:end p)
                                           :hit? (boolean (:hit? p))})}
                :barrage {:topic :ray-barrage/fx-barrage
                          :mode :barrage
                          :targets [:immediate :level]
                          :immediate-fn barrage-sound!
                          :level-payload (fn [_ _ p]
                                           {:silbarn (:silbarn p)
                                            :yaw (double (or (:yaw p) 0.0))
                                            :pitch (double (or (:pitch p) 0.0))})}}}))

(arc-beam/def-arc-beam-fx :ray-barrage)

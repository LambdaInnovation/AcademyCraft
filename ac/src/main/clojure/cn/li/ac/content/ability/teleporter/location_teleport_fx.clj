(ns cn.li.ac.content.ability.teleporter.location-teleport-fx
  "Client FX for Location Teleport success feedback."
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]))

(defn- on-perform-success! [_ctx-id _channel _payload]
  (client-sounds/queue-current-sound-effect!
    {:type :sound :sound-id (modid/namespaced-path "tp.tp") :volume 0.5 :pitch 1.0}))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :location-teleport
     :runtime :none
     :channels [{:topic :location-teleport/fx-perform-success
                 :targets [:immediate]
                 :immediate-fn on-perform-success!}]}))

(arc-beam/def-arc-beam-fx :location-teleport)

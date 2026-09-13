(ns cn.li.ac.content.ability.electromaster.body-intensify-fx
  "Client FX for body-intensify."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(def ^:private activate-sound-id (modid/namespaced-path "em.intensify_activate"))
(def ^:private loop-sound-id (modid/namespaced-path "em.intensify_loop"))
(def ^:private intensify-effect-id "intensify_effect")

(defn- loop-key [ctx-id]
  (str "body-intensify/" ctx-id))

(defn- on-fx-start
  [ctx-id _channel payload]
  ;; FollowEntitySound upstream is a real ambient loop attached to the caster.
  (client-bridge/run-client-effect!
   :mcmod/start-loop-sound-at-player
   {:key (loop-key ctx-id)
    :sound-id loop-sound-id
    :owner-uuid (:source-player-id payload)
    :volume 1.0
    :pitch 1.0}))

(defn- on-fx-end
  [ctx-id channel payload]
  ;; Stop any charging loop started by fx-start before handling release.
  (client-bridge/run-client-effect!
   :mcmod/stop-loop-sound
   {:key (loop-key ctx-id)})
  ;; Upstream c_endEffect: isLocal → hud.startBlend(performed). No-op for the
  ;; fanned-out copies about other players' releases.
  (reactive-hud/start-charging-blend! (:source-player-id payload)
                                      (boolean (:performed? payload)))
  (when (:performed? payload)
    (let [{:keys [x y z]} (:caster-pos payload)]
      (client-sounds/queue-current-sound-effect!
       (cond-> {:sound-id activate-sound-id
                :source :ambient
                :volume 0.5
                :pitch 1.0}
         (number? x) (assoc :x (double x))
         (number? y) (assoc :y (double y))
         (number? z) (assoc :z (double z)))))
    (client-bridge/run-client-effect!
     :mcmod/spawn-scripted-effect-at-player
     {:effect-id intensify-effect-id
      :owner-uuid (:source-player-id payload)
      :ctx-id ctx-id
      :channel channel})))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :body-intensify
     :runtime :none
     :channels [{:topic :body-intensify/fx-start
                 :targets [:immediate]
                 :immediate-fn on-fx-start}
                {:topic :body-intensify/fx-end
                 :targets [:immediate]
                 :immediate-fn on-fx-end}]}))

(arc-beam/def-arc-beam-fx :body-intensify)

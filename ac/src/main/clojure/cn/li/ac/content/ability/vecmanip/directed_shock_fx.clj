(ns cn.li.ac.content.ability.vecmanip.directed-shock-fx
  (:require [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.config.modid :as modid]))

(def ^:private punch-sound-id (modid/namespaced-path "vecmanip.directed_shock"))

;; Original's second MSG_GENERATE_EFFECT client listener plays this sound
;; unconditionally (outside the isLocal guard that gates only the hand-punch
;; animation) — every recipient of the fanned-out fx-perform event hears it
;; at the caster's position. It rides :immediate so the fan-out reaches the
;; sound without touching the hand runtime.
(defn- on-fx-perform-sound!
  [_ctx-id _channel payload]
  (when (and (:x payload) (:y payload) (:z payload))
    (client-sounds/queue-current-sound-effect!
     {:sound-id punch-sound-id
      :volume 0.5
      :pitch 1.0
      :x (double (:x payload))
      :y (double (:y payload))
      :z (double (:z payload))})))

;; :start / :punch are owner-only sends (:client), matching the original's
;; isLocal-gated hand animations (l_handEffectStart / l_effect); :perform
;; fans out to owner + nearby so every recipient hears the caster-positioned
;; punch sound.
(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :directed-shock
     :runtime :hand
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :directed-shock/fx-start :mode :start :targets [:hand]}
                :punch {:topic :directed-shock/fx-punch :mode :punch :targets [:hand]}
                :perform {:topic :directed-shock/fx-perform :mode :perform
                          :targets [:immediate]
                          :immediate-fn on-fx-perform-sound!}
                :end {:topic :directed-shock/fx-end :mode :end :targets [:hand]
                      :hand-payload (fn [_ _ p]
                                      {:performed? (boolean (:performed? p))})}}}))

(arc-beam/def-arc-beam-fx :directed-shock)
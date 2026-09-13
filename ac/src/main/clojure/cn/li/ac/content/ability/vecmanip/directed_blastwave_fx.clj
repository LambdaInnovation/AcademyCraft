(ns cn.li.ac.content.ability.vecmanip.directed-blastwave-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

;; Upstream channel map (BlastwaveContext/BlastwaveContextC):
;;   key-down   (MSG_MADEALIVE) -> createPrepareAnim hand raise, 0.15s  (isLocal)
;;   key-up     (MSG_PERFORM)   -> punch anim + WaveEffect + sound      (anim isLocal)
;;   air release                -> same as key-up, position = player + look*4
;; The :start / :punch channels are owner-only sends (:client), matching the
;; original's isLocal hand-anim gates; :perform fans out to owner + nearby
;; because every recipient renders the WaveEffect at the caster's position.
(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :directed-blastwave
     :runtime :both
     :level-initial-state (fn [] {:waves {}})
     :hand-initial-state (fn [] {:effect-state {}})
     :transform-fn #(arc-beam/effect-transform-fn :directed-blastwave)
     :channels {:start {:topic :directed-blastwave/fx-start :mode :start :targets [:hand]}
                :punch {:topic :directed-blastwave/fx-punch :mode :punch :targets [:hand]}
                :perform {:topic :directed-blastwave/fx-perform :mode :perform :targets [:level]
                          :level-payload (fn [_ _ p]
                                           {:pos (:pos p) :look-dir (:look-dir p)})}
                :end {:topic :directed-blastwave/fx-end :mode :end :targets [:hand]
                      :hand-payload (fn [_ _ p]
                                      {:performed? (boolean (:performed? p))})}}}))
(arc-beam/def-arc-beam-fx :directed-blastwave)

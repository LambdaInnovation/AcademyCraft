(ns cn.li.ac.content.ability.meltdowner.light-shield-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

;; c_end only kills the shield entity and stops the loop sample — the original
;; plays nothing on shutdown. The port fired md.shield_loop as a one-shot here,
;; which both invented an end cue and spent the loop sample on it.

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :light-shield
     :initial-state (fn [] {:effect-state {}})
     :channels {:start {:topic :light-shield/fx-start :mode :start}
                ;; Carries the caster's lookingPos(player, 1) each server tick;
                ;; the channel particles are world-space and have nowhere else
                ;; to get it from.
                :tick {:topic :light-shield/fx-tick :mode :tick
                       :level-payload (fn [_ _ p] {:pos (:pos p)})}
                :end {:topic :light-shield/fx-end :mode :end}}}))

(arc-beam/def-arc-beam-fx :light-shield)

(ns cn.li.ac.content.ability.meltdowner.rad-intensify-fx
  (:require [cn.li.ac.ability.client.fx-spec :as fx-spec]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]))

(def ^:private spec
  (arc-beam/build-spec
    {:effect-id :rad-intensify-mark
     :initial-state (fn [] {:marks {}})
     :channels {:mark {:topic :rad-intensify/fx-mark}}}))

(arc-beam/def-arc-beam-fx :rad-intensify-mark)
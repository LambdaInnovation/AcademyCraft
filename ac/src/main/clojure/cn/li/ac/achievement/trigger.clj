(ns cn.li.ac.achievement.trigger
  "Achievement trigger helper owned by AC."
  (:require [cn.li.ac.ability.registry.event :as evt]))

(defn trigger-achievement!
  [uuid achievement-id]
  (evt/fire-ability-event!
    (evt/make-achievement-trigger-event uuid achievement-id)))


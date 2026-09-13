(ns cn.li.ac.content.ability.generic.brain-course
  "Generic passive skill: Brain Course (+1000 max CP)."
  (:require [cn.li.ac.content.ability.generic.course-chain :as courses]))

(def skill-specs (delay (courses/build-skill-specs :brain-course)))

(defn init!
  []
  (courses/register-passive-hooks! :brain-course))

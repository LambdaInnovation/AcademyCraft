(ns cn.li.ac.content.ability.generic.mind-course
  "Generic passive skill: Mind Course (x1.2 CP recovery)."
  (:require [cn.li.ac.content.ability.generic.course-chain :as courses]))

(def skill-specs (delay (courses/build-skill-specs :mind-course)))

(defn init!
  []
  (courses/register-passive-hooks! :mind-course))

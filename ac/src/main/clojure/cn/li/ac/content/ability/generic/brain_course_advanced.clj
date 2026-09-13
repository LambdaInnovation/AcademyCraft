(ns cn.li.ac.content.ability.generic.brain-course-advanced
  "Generic passive skill: Advanced Brain Course (+1500 CP, +100 overload)."
  (:require [cn.li.ac.content.ability.generic.course-chain :as courses]))

(def skill-specs (delay (courses/build-skill-specs :brain-course-advanced)))

(defn init!
  []
  (courses/register-passive-hooks! :brain-course-advanced))

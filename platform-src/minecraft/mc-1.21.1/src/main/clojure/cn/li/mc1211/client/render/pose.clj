(ns cn.li.mc1211.client.render.pose
  "Thin re-export of cn.li.mcbase.client.render.pose."
  (:require [cn.li.mcbase.client.render.pose :as shared]))

(def rotate-y shared/rotate-y)
(def rotate-x shared/rotate-x)
(def rotate-z shared/rotate-z)
(def rotate-axis shared/rotate-axis)
(def push-pose shared/push-pose)
(def pop-pose shared/pop-pose)
(def translate shared/translate)
(def scale shared/scale)
(def get-pose-matrix shared/get-pose-matrix)
(def submit-vertex shared/submit-vertex)
(def submit-vertex-no-overlay shared/submit-vertex-no-overlay)

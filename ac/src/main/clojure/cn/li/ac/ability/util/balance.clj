(ns cn.li.ac.ability.util.balance
  (:require [cn.li.ac.ability.util.scaling :as scaling]))

(def lerp scaling/lerp)

(defn clamp01
  [x]
  (max 0.0 (min 1.0 (double x))))

(defn by-exp
  "Create an evt-aware scaling fn based on :exp in runtime event."
  [a b]
  (fn [evt]
    (let [exp (double (or (:exp evt) 0.0))]
      (lerp a b exp))))

(defn falloff-linear
  [distance radius]
  (max 0.0 (- 1.0 (/ (double distance) (max 1.0e-6 (double radius))))))

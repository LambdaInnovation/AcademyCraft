(ns cn.li.ac.ability.client.beam-render-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.effects.beam-render :as beam-render]
            [cn.li.ac.ability.client.effects.rv3 :as v3]))

(deftest fading-beam-ops-builds-standard-shape-test
  (let [start (v3/v3 0.0 0.0 0.0)
        end (v3/v3 0.0 0.0 1.0)
        cam-pos (v3/v3 1.0 0.0 0.0)
        beam {:start start :end end :ttl 5 :max-ttl 10 :mode :test}
        ops (beam-render/fading-beam-ops
              cam-pos
              beam
              {:width (fn [_ life] life)
               :core-ratio 0.5
               :outer-color (fn [_ life] {:r 1 :g 2 :b 3 :a (int (* 100 life))})
               :inner-color {:r 4 :g 5 :b 6 :a 7}
               :line-color (fn [b _] {:r 8 :g 9 :b 10 :a (if (= :test (:mode b)) 11 0)})})]
    (testing "delegates to standard outer/core/line beam primitive"
      (is (= 3 (count ops)))
      (is (= :quad (:kind (first ops))))
      (is (= :quad (:kind (second ops))))
      (is (= :line (:kind (nth ops 2)))))
    (testing "config callbacks receive life ratio and beam state"
      (is (= {:r 1 :g 2 :b 3 :a 50} (:color (first ops))))
      (is (= {:r 4 :g 5 :b 6 :a 7} (:color (second ops))))
      (is (= {:r 8 :g 9 :b 10 :a 11} (:color (nth ops 2)))))))

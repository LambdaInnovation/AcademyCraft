(ns cn.li.ac.ability.client.beam-ops-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.effects.beam-ops :as beam-ops]
            [cn.li.ac.ability.client.effects.rv3 :as v3]))

(deftest fading-beam-style-composes-rgb-alpha-test
  (let [start (v3/v3 0.0 0.0 0.0)
        end (v3/v3 0.0 0.0 1.0)
        cam-pos (v3/v3 1.0 0.0 0.0)
        beam {:start start :end end :ttl 5 :max-ttl 10 :is-reflect? true}
        ops (beam-ops/fading-beam-ops
              cam-pos
              beam
              {:width (fn [_ life] life)
               :core-ratio 0.5
               :outer-rgb {:r 1 :g 2 :b 3}
               :outer-alpha (fn [_ life] (+ 10 (* 100 life)))
               :inner-rgb {:r 4 :g 5 :b 6}
               :inner-alpha 99
               :line-rgb (fn [b _] (if (:is-reflect? b)
                                      {:r 7 :g 8 :b 9}
                                      {:r 0 :g 0 :b 0}))
               :line-alpha (fn [_ life] (* 200 life))})]
    (testing "uses the shared billboard primitive shape"
      (is (= 3 (count ops)))
      (is (= [:quad :quad :line] (mapv :kind ops))))
    (testing "resolves RGB + alpha constants and callbacks"
      (is (= {:r 1 :g 2 :b 3 :a 60} (:color (first ops))))
      (is (= {:r 4 :g 5 :b 6 :a 99} (:color (second ops))))
      (is (= {:r 7 :g 8 :b 9 :a 100} (:color (nth ops 2)))))))

(deftest direct-beam-style-resolves-static-colors-test
  (let [start (v3/v3 0.0 0.0 0.0)
        end (v3/v3 0.0 1.0 0.0)
        cam-pos (v3/v3 1.0 0.0 0.0)
        ops (beam-ops/beam-ops
              cam-pos
              start
              end
              {:width 0.25
               :core-width 0.1
               :outer-rgb {:r 10 :g 20 :b 30}
               :outer-alpha 40
               :inner-color {:r 50 :g 60 :b 70 :a 80}})]
    (is (= 2 (count ops)))
    (is (= {:r 10 :g 20 :b 30 :a 40} (:color (first ops))))
    (is (= {:r 50 :g 60 :b 70 :a 80} (:color (second ops))))))

(deftest fading-tube-beam-ops-emits-outer-and-inner-cylinders-test
  (let [start (v3/v3 0.0 0.0 0.0)
        end (v3/v3 0.0 0.0 1.0)
        beam {:start start :end end :ttl 5 :max-ttl 10 :is-reflect? false}
        ops (beam-ops/fading-tube-beam-ops
              beam
              {:width (fn [_ life] (* 0.1 (+ 0.5 life)))
               :core-ratio 0.5
               :outer-rgb {:r 106 :g 242 :b 106}
               :outer-alpha (fn [_ life] (+ 20 (* 40 life)))
               :inner-rgb {:r 216 :g 248 :b 216}
               :inner-alpha 200})]
    (is (= 32 (count ops)) "16-segment outer + 16-segment inner cylinder")
    (is (every? #(= :quad (:kind %)) ops))
    ;; life 0.5: outer alpha 40, radius 0.15; inner alpha 200, radius 0.075
    (is (= {:r 106 :g 242 :b 106 :a 40} (:color (first ops))))
    (is (= {:r 216 :g 248 :b 216 :a 200} (:color (nth ops 16))))))

(deftest fading-glow-board-ops-orients-by-view-test
  (let [start (v3/v3 0.0 0.0 0.0)
        end (v3/v3 0.0 0.0 10.0)
        beam {:start start :end end :ttl 5 :max-ttl 10}
        style {:outer-rgb {:r 106 :g 242 :b 106} :outer-alpha 80}]
    (testing "first person uses the fixed (0,1,-0.5) axis"
      (let [[op] (beam-ops/fading-glow-board-ops
                   {:x 0.0 :y 0.0 :z 0.0} beam style {:first-person? true})
            axis (v3/vnorm (v3/v3 0.0 1.0 -0.5))
            span (v3/v- (:p0 op) (:p1 op))]
        (is (= :quad (:kind op)))
        (is (= {:r 106 :g 242 :b 106 :a 80} (:color op)))
        (is (< (v3/vlen (v3/v- span (v3/v* axis 1.5))) 1.0e-6)
            "board lateral axis is the fixed up-and-back axis"))
    (testing "third person uses the view-perpendicular axis"
      (let [[op] (beam-ops/fading-glow-board-ops
                   {:x 5.0 :y 0.0 :z 0.0} beam style {})
            span (v3/v- (:p0 op) (:p1 op))]
        ;; cross(to-beam=(-5,0,0), dir=(0,0,1)) -> +y
        (is (< (v3/vlen (v3/v- span (v3/v* (v3/v3 0.0 1.0 0.0) 1.5))) 1.0e-6)))))))

(deftest trajectory-glow-helpers-match-legacy-shape-test
  (let [p0 {:x 0.0 :y 0.0 :z 0.0}
        p1 {:x 1.0 :y 0.0 :z 0.0}
        p2 {:x 1.0 :y 1.0 :z 0.0}
        p3 {:x 0.0 :y 1.0 :z 0.0}
        color (beam-ops/rgba {:r 255 :g 51 :b 51} (beam-ops/fade-alpha 0))
        op (beam-ops/glow-line-quad-op p0 p1 p2 p3 color)]
    (is (= 178 (:a color)))
    (is (= :quad (:kind op)))
    (is (= beam-ops/default-glow-line-texture (:texture op)))
    (is (= color (:color op)))))

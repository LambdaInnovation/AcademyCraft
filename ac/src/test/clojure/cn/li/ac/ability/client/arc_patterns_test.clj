(ns cn.li.ac.ability.client.arc-patterns-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.arc-patterns :as arc]
            [cn.li.ac.ability.client.effects.rv3 :as v3]))

(def ^:private start (v3/v3 0.0 0.0 0.0))
(def ^:private end (v3/v3 0.0 0.0 16.0))

(defn- path
  [opts]
  (arc/generate-zigzag-segments start end (merge {:segments 8 :amplitude 0.12 :seed 7} opts)))

(defn- deviation
  "Perpendicular distance of a vertex from the straight start→end axis
  (the beam runs along +Z, so that is just the XY magnitude)."
  [{:keys [pos]}]
  (Math/hypot (.-x pos) (.-y pos)))

(deftest zigzag-path-is-anchored-at-both-endpoints
  (testing "the bolt leaves start and lands on end exactly — a caster-anchored
            path is what keeps the near segments inside the field of view"
    (let [vs (path {})]
      (is (= start (:pos (first vs))))
      (is (= end (:pos (peek vs))))
      (is (= 0.0 (:u (first vs))))
      (is (= 1.0 (:u (peek vs)))))))

(deftest zigzag-path-subdivides-to-a-power-of-two
  (testing "segments is a target count, rounded up to the pass count"
    ;; 8 segments -> 3 passes -> 9 vertices; 24 -> 5 passes -> 33 vertices.
    (is (= 9 (count (path {:segments 8}))))
    (is (= 33 (count (path {:segments 24}))))))

(deftest zigzag-path-u-runs-monotonically-along-the-beam
  (let [us (mapv :u (path {:segments 24}))]
    (is (= us (vec (sort us))))
    (is (apply distinct? us))))

(deftest zigzag-path-is-deterministic-per-seed
  (testing "shape is fixed for the arc's lifetime, so it is precomputed once"
    (is (= (mapv :pos (path {:seed 3})) (mapv :pos (path {:seed 3}))))
    (is (not= (mapv :pos (path {:seed 3})) (mapv :pos (path {:seed 4}))))))

(deftest zigzag-deviation-concentrates-toward-the-middle
  (testing "midpoint displacement, not per-vertex noise: the ends stay tight
            and the swing builds up in the middle"
    (let [vs (path {:segments 24})
          n (count vs)
          near-start (apply max (map deviation (subvec vs 1 4)))
          mid (apply max (map deviation (subvec vs (- (quot n 2) 2) (+ (quot n 2) 2))))]
      (is (< near-start mid))
      ;; Pass-0 offset is amplitude * length; later passes halve, so the total
      ;; swing stays within twice that.
      (is (< mid (* 2.0 0.12 16.0))))))

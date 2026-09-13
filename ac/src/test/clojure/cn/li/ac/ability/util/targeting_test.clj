(ns cn.li.ac.ability.util.targeting-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.util.targeting :as tgt]))

(deftest distance-3d-test
  (is (= 0.0 (tgt/distance-3d 1 2 3 1 2 3)))
  (is (= 1.0 (tgt/distance-3d 0 0 0 1 0 0)))
  (let [d (tgt/distance-3d 0 0 0 1 1 1)]
    (is (< 1.73 d 1.74))))

(deftest calculate-aoe-falloff-test
  (is (= 10.0 (tgt/calculate-aoe-falloff 10.0 3.0 5.0 false)))
  (is (= 10.0 (tgt/calculate-aoe-falloff 10.0 0.0 5.0 true)))
  (is (= 0.0 (tgt/calculate-aoe-falloff 10.0 5.0 5.0 true)))
  (is (= 5.0 (tgt/calculate-aoe-falloff 10.0 2.5 5.0 true))))

(deftest find-entities-in-cone-test
  (let [entities [{:x 2 :y 0 :z 0 :id 1}
                  {:x -1 :y 0 :z 0 :id 2}
                  {:x 0 :y 5 :z 0 :id 3}]
        ;; origin 0,0,0, dir +x, 30° half-angle
        half (/ Math/PI 6)]
    (is (= 1 (count (tgt/find-entities-in-cone entities 0 0 0 1 0 0 10 half))))
    (is (= 1 (:id (first (tgt/find-entities-in-cone entities 0 0 0 1 0 0 10 half)))))))

(deftest find-nearest-entity-test
  (is (nil? (tgt/find-nearest-entity [] 0 0 0)))
  (let [e1 {:x 0 :y 0 :z 1}
        e2 {:x 0 :y 0 :z 3}]
    (is (= e1 (tgt/find-nearest-entity [e1 e2] 0 0 0)))))

(deftest filter-by-distance-test
  (let [es [{:x 0 :y 0 :z 0} {:x 10 :y 0 :z 0}]]
    (is (= 1 (count (tgt/filter-by-distance es 0 0 0 1.0))))
    (is (= 2 (count (tgt/filter-by-distance es 0 0 0 100.0))))))

(deftest sort-by-distance-test
  (let [a {:x 0 :y 0 :z 1}
        b {:x 0 :y 0 :z 5}
        c {:x 0 :y 0 :z 2}]
    (is (= [a c b] (vec (tgt/sort-by-distance [b c a] 0 0 0))))))

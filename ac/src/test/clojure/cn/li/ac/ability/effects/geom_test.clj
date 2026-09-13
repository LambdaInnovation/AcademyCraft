(ns cn.li.ac.ability.effects.geom-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.test.support.player-state :as ps-fix]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(defn- approx=
  [a b eps]
  (< (Math/abs (- (double a) (double b))) eps))

(deftest vector-ops-test
  (let [a {:x 1 :y 2 :z 3}
        b {:x 4 :y -1 :z 0}]
    (is (= {:x 5.0 :y 1.0 :z 3.0} (geom/v+ a b)))
    (is (= {:x -3.0 :y 3.0 :z 3.0} (geom/v- a b)))
    (is (= {:x 2.0 :y 4.0 :z 6.0} (geom/v* a 2.0)))
    (is (= 2.0 (geom/vdot {:x 1 :y 0 :z 0} {:x 2 :y 3 :z 4})))
    (is (approx= 1.0 (geom/vlen {:x 1 :y 0 :z 0}) 1e-9))
    (is (approx= 1.0 (geom/vlen (geom/vnorm {:x 3 :y 4 :z 0})) 1e-6))))

(deftest vcross-test
  (is (= {:x 0.0 :y 0.0 :z 1.0} (geom/vcross {:x 1 :y 0 :z 0} {:x 0 :y 1 :z 0}))))

(deftest orthonormal-basis-test
  (doseq [dir [{:x 1 :y 0 :z 0}
               {:x 0 :y 1 :z 0}
               {:x 0.3 :y 0.4 :z (Math/sqrt 0.75)}]]
    (let [[right up] (geom/orthonormal-basis dir)
          eps 1e-5]
      (is (approx= 1.0 (geom/vlen right) eps))
      (is (approx= 1.0 (geom/vlen up) eps))
      (is (approx= 0.0 (geom/vdot right dir) eps))
      (is (approx= 0.0 (geom/vdot up dir) eps))
      (is (approx= 0.0 (geom/vdot right up) eps)))))

(deftest vdist-and-floor-test
  (is (approx= 5.0 (geom/vdist {:x 0 :y 0 :z 0} {:x 3 :y 4 :z 0}) 1e-9))
  (is (= 25.0 (geom/vdist-sq {:x 0 :y 0 :z 0} {:x 3 :y 4 :z 0})))
  (is (= 3 (geom/floor-int 3.9)))
  (is (= -4 (geom/floor-int -3.1))))

(deftest rotate-around-axis-test
  (let [v {:x 1 :y 0 :z 0}
        axis {:x 0 :y 0 :z 1}
        r (geom/rotate-around-axis v axis 90.0)]
    (is (approx= 0.0 (:x r) 1e-5))
    (is (approx= 1.0 (:y r) 1e-5))
    (is (approx= 1.0 (geom/vlen r) 1e-5))))

(deftest world-id-and-eye-pos-test
  (testing "defaults when no player state"
    (is (= "minecraft:overworld" (geom/world-id-of "unknown")))
    (let [e (geom/eye-pos "unknown")]
      (is (= 0.0 (:x e)))
      (is (> 0.01 (Math/abs (- (:y e) 65.62))))
      (is (= 0.0 (:z e))))))

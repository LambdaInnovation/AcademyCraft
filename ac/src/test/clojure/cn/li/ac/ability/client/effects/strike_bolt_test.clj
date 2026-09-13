(ns cn.li.ac.ability.client.effects.strike-bolt-test
  "Geometry of the descending strike channel.

  Vanilla LightningBoltRenderer walks 8 fixed 16-block segments straight up
  from the entity, offsetting each joint by at most ±5 — over 128 blocks that
  is a shallow enough wander to read as a bright vertical column rather than
  a forked strike. These tests pin the properties that make the replacement
  read as lightning: it spans sky to impact, it visibly wanders off the
  vertical, and it forks."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.effects.arc-fx :as arc-fx]))

(def ^:private impact {:x 100.0 :y 64.0 :z -40.0})

(defn- points [segments]
  (mapcat (fn [s] [(:pos (:start s)) (:pos (:end s))]) segments))

(defn- horizontal-drift
  "Furthest any point strays from the vertical line through the impact."
  [segments]
  (->> (points segments)
       (map (fn [p]
              (let [dx (- (.-x p) (:x impact))
                    dz (- (.-z p) (:z impact))]
                (Math/sqrt (+ (* dx dx) (* dz dz))))))
       (reduce max 0.0)))

(deftest bolt-spans-from-the-sky-down-to-the-impact-test
  (let [segments (arc-fx/strike-bolt-segments impact {:height 64.0})
        ys (map #(.-y %) (points segments))]
    (is (seq segments))
    (is (< (- (apply min ys) (:y impact)) 1.0e-6)
        "the channel reaches the impact point")
    (is (> (apply max ys) (+ (:y impact) 60.0))
        "and starts high enough overhead to read as coming out of the sky")))

(deftest bolt-wanders-off-the-vertical-test
  ;; The whole point of the replacement: a straight column scores 0.
  ;;
  ;; Displacement is randomised, so the bounds are deliberately loose. The
  ;; per-run floor is what must never regress — a bolt that scores near zero
  ;; is the column we are replacing — while the mean pins that the channel
  ;; wanders on the scale of the offset budget rather than merely jittering.
  (let [drifts (vec (repeatedly 24 #(horizontal-drift
                                     (arc-fx/strike-bolt-segments impact {:height 64.0}))))
        mean (/ (reduce + drifts) (count drifts))]
    (is (every? pos? drifts)
        (str "no strike may come out perfectly straight; got " drifts))
    (is (every? #(> % 0.5) drifts)
        (str "every strike wanders measurably; got " drifts))
    (is (> mean 2.0)
        (str "and typically on the scale of the offset budget; mean " mean))))

(deftest bolt-forks-test
  ;; Branch segments start where they were spawned, so a forked bolt has more
  ;; segments than the 2^passes joints of a bare trunk.
  (let [trunk-only (arc-fx/strike-bolt-segments impact {:height 64.0
                                                        :passes 5
                                                        :branch-factor 0.0})
        forked (arc-fx/strike-bolt-segments impact {:height 64.0
                                                    :passes 5
                                                    :branch-factor 0.6})]
    (is (= 32 (count trunk-only)) "2^5 joints with no branching")
    (is (> (count forked) (count trunk-only)))))

(deftest alpha-scale-drives-the-flash-envelope-test
  (let [segments (arc-fx/strike-bolt-segments impact {:height 16.0 :passes 2})]
    (is (empty? (arc-fx/bolt-segments->ops segments 0.0))
        "a dark frame emits nothing at all")
    (let [bright (arc-fx/bolt-segments->ops segments 1.0)
          dim (arc-fx/bolt-segments->ops segments 0.25)]
      (is (= (count bright) (count dim)))
      (is (< (get-in (first dim) [:color :a])
             (get-in (first bright) [:color :a]))))))

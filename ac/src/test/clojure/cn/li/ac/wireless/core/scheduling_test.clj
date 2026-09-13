(ns cn.li.ac.wireless.core.scheduling-test
  "Phase-staggered scheduling: exactness and distribution."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.wireless.core.scheduling :as sched]))

(deftest due-fires-exactly-once-per-interval-test
  (let [interval 40]
    (doseq [seed [[0 64 0] [17 70 -3] [-160 12 8000] :sweep]]
      (let [fires (filterv #(sched/due? (long %) interval seed) (range (* 3 interval)))]
        (is (= 3 (count fires))
            (str "seed " seed " must fire exactly once per interval window"))
        (is (= [interval interval] (mapv - (rest fires) fires))
            (str "seed " seed " fires evenly spaced"))))))

(deftest due-staggers-different-seeds-test
  (testing "positions spread across the interval instead of spiking on one tick"
    (let [interval 40
          seeds (for [x (range 10) z (range 10)] [(* 16 x) 64 (* 16 z)])
          phase (fn [seed]
                  (first (filter #(sched/due? (long %) interval seed) (range interval))))
          phases (map phase seeds)]
      (is (> (count (distinct phases)) 1))
      (is (>= (count (distinct phases)) 10)
          "100 chunk-aligned positions should occupy many distinct phases"))))

(deftest due-degenerate-interval-test
  (is (true? (sched/due? 123 1 [0 0 0])))
  (is (true? (sched/due? 123 0 [0 0 0]))))

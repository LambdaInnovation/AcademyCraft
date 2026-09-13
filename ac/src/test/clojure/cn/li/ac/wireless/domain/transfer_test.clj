(ns cn.li.ac.wireless.domain.transfer-test
  "Regression tests for the pure balance planner and the rotation helper."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.wireless.domain.transfer :as transfer]))

(defn- entry [id energy max-energy]
  {:id id :energy energy :max-energy max-energy :bandwidth 1.0e6})

(deftest balance-plan-proportional-fill-test
  (testing "unlimited bandwidth converges every node to the same fill ratio"
    (let [entries [(entry :a 900.0 1000.0) (entry :b 100.0 1000.0)]
          {:keys [energies buffer]} (transfer/balance-plan entries 1.0e6 0.0 1.0e6)]
      (is (< (Math/abs (- (get energies :a) (get energies :b))) 1.0e-6))
      (is (< (Math/abs (- (+ (get energies :a) (get energies :b) buffer) 1000.0)) 1.0e-6))))

  (testing "different node capacities converge to equal percentage"
    (let [entries [(entry :small 0.0 100.0) (entry :large 1000.0 1000.0)]
          plan1 (transfer/balance-plan entries 1.0e6 0.0 1.0e6)
          entries2 [(assoc (entry :small 0.0 100.0) :energy (get (:energies plan1) :small))
                    (assoc (entry :large 0.0 1000.0) :energy (get (:energies plan1) :large))]
          {:keys [energies buffer]} (transfer/balance-plan entries2 1.0e6 (:buffer plan1) 1.0e6)
          percent (/ 1000.0 1100.0)]
      (is (< (Math/abs (- (get energies :small) (* 100.0 percent))) 1.0e-6))
      (is (< (Math/abs (- (get energies :large) (* 1000.0 percent))) 1.0e-6))
      (is (< (Math/abs (- (+ (get energies :small) (get energies :large) buffer) 1000.0)) 1.0e-6)))))

(deftest balance-plan-respects-matrix-bandwidth-test
  ;; Entries the budget never reached are absent from :energies — default to
  ;; their input energy, mirroring effects/apply-node-energy-plan!.
  (let [entries [(entry :a 1000.0 1000.0) (entry :b 0.0 1000.0)]
        {:keys [energies buffer]} (transfer/balance-plan entries 50.0 0.0 1.0e6)
        a' (double (get energies :a 1000.0))
        b' (double (get energies :b 0.0))
        moved (+ (Math/abs (- a' 1000.0)) (Math/abs (- b' 0.0)))]
    (is (<= moved (+ 50.0 1.0e-6)) "total movement bounded by matrix bandwidth")
    (is (< (Math/abs (- (+ a' b' buffer) 1000.0)) 1.0e-6)))
  (testing "energy passes through the buffer, clamped to buffer-max"
    (let [entries [(entry :a 1000.0 1000.0) (entry :b 0.0 1000.0)]
          {:keys [buffer]} (transfer/balance-plan entries 1.0e6 0.0 10.0)]
      (is (<= 0.0 buffer 10.0)))))

(deftest balance-plan-degenerate-inputs-test
  (is (= {:energies {} :buffer 5.0} (transfer/balance-plan [] 100.0 5.0 10.0)))
  (is (= {:energies {} :buffer 5.0}
         (transfer/balance-plan [(entry :dead 0.0 0.0)] 100.0 5.0 10.0))
      "zero-max entries are ignored")
  (is (= 10.0 (:buffer (transfer/balance-plan [] 100.0 99.0 10.0)))
      "incoming buffer is clamped to buffer-max"))

(deftest rotated-fairness-test
  (let [v [:a :b :c :d :e]
        n (count v)
        firsts (map #(first (transfer/rotated v (long %))) (range n))]
    (testing "over n consecutive ticks every element leads exactly once"
      (is (= (set v) (set firsts)))
      (is (= n (count (distinct firsts)))))
    (testing "rotation preserves elements and order cyclically"
      (doseq [t (range n)]
        (let [r (transfer/rotated v (long t))]
          (is (= (sort (map str v)) (sort (map str r))))
          (is (= (take n (drop t (cycle v))) (seq r))))))))

(deftest rotated-small-vectors-test
  (is (= [] (transfer/rotated [] 7)))
  (is (= [:only] (transfer/rotated [:only] 7)))
  (is (= [:a :b] (transfer/rotated [:a :b] 0)))
  (is (= [:b :a] (transfer/rotated [:a :b] 1))))

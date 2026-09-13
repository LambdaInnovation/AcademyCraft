(ns cn.li.ac.ability.client.effects.academy-arc-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.client.effects.academy-arc :as academy-arc]
            [cn.li.ac.ability.client.effects.rv3 :as v]))

(use-fixtures
 :each
 (fn [f]
   (academy-arc/reset-template-cache-for-test!)
   (try
     (f)
     (finally
       (academy-arc/reset-template-cache-for-test!)))))

(deftest upstream-template-banks-and-dimensions-test
  (let [charging (academy-arc/template-snapshot :charging)
        normal (academy-arc/template-snapshot :normal)
        thin (academy-arc/template-snapshot :thin)]
    (testing "ArcPatterns.chargingArc has 20 fixed 20-block templates"
      (is (= 20 (count charging)))
      (is (every? #(= 20.0 (:length %)) charging)))
    (testing "EntitySurroundArc generates ten mode-specific templates"
      (is (= 10 (count normal)))
      (is (= 10 (count thin)))
      (is (every? #(<= 3.0 (:length %) 4.0) normal))
      (is (every? #(<= 1.5 (:length %) 2.0) thin)))
    (testing "the high upstream branch factor produces recursive sub-lines"
      (is (every? #(> (:line-count %) 1) normal))
      (is (every? #(> (:segment-count %) 8) normal)))))

(deftest entity-arc-prefix-is-immediately-renderable-test
  (let [ops (academy-arc/entity-arc-ops
             (v/v3 0.0 1.6 0.0)
             (v/v3 0.0 1.6 0.0)
             (v/v3 0.0 1.6 15.0)
             0
             nil)]
    (is (seq ops))
    (is (every? #(= :current-charging/beam (:effect-part %)) ops))
    (is (every? #(integer? (:color %)) ops))
    (is (every? #(= 0x00FFFFFF
                    (bit-and 0x00FFFFFF (long (:color %))))
                ops))))

(deftest surround-subarcs-use-client-tick-state-test
  (let [salt 1000
        initial (academy-arc/initial-surround-state :normal salt)
        advanced (reduce
                  (fn [state tick]
                    (academy-arc/tick-surround-state
                     state :normal salt tick))
                  initial
                  (range 1 11))
        initial-ops (academy-arc/surround-arc-ops
                     (v/v3 0.0 2.0 -3.0)
                     {:x 0.5 :y 0.0 :z 0.5
                      :width 1.0 :height 1.0 :depth 1.0}
                     :normal
                     initial
                     salt)
        ops (academy-arc/surround-arc-ops
             (v/v3 0.0 2.0 -3.0)
             {:x 0.5 :y 0.0 :z 0.5
              :width 1.0 :height 1.0 :depth 1.0}
             :normal
             advanced
             salt)]
    (is (= 6 (count (:sparks initial))))
    (is (= [true false false false false false]
           (mapv :draw? (:sparks initial))))
    (is (seq initial-ops))
    (is (every? #(= :current-charging/surround (:effect-part %)) initial-ops))
    (is (= 6 (count (:sparks advanced))))
    (is (some :draw? (:sparks advanced)))
    (is (seq ops))
    (is (every? #(= :current-charging/surround (:effect-part %)) ops))))

(deftest template-ops-strip-stays-watertight-through-kinks-test
  ;; Upstream handleSegment carries lastDir so adjacent quads share one width
  ;; axis at the junction. Per-segment independent axes notch the outside of
  ;; each bend — the current-charging beam would otherwise read as separate
  ;; segments on its sharpest kinks.
  (let [kink (v/v3 1.0 0.5 0.0)
        line [{:start {:pos (v/v3 0.0 0.0 0.0) :width 0.2}
               :end {:pos kink :width 0.2}
               :alpha 1.0}
              {:start {:pos kink :width 0.2}
               :end {:pos (v/v3 2.0 0.0 0.0) :width 0.2}
               :alpha 1.0}]
        ops (#'academy-arc/template-ops
             (v/v3 5.0 0.0 5.0)
             {:lines [line]}
             identity
             1.0
             nil
             :test/part
             nil)]
    (is (= 2 (count ops)))
    (is (= (:p2 (nth ops 0)) (:p1 (nth ops 1)))
        "adjacent quads share the junction edge")
    (is (= (:p3 (nth ops 0)) (:p0 (nth ops 1))))
    ;; no quad may self-intersect (bowtie): anti-parallel width axes fold the
    ;; quad over itself and its GL fill tears a visible gap.
    (is (every?
         (fn [{:keys [p0 p1 p2 p3]}]
           (let [n (v/vnorm (v/vcross (v/v- p2 p0) (v/v- p3 p1)))
                 cross? (fn [a b c d]
                          (let [orient (fn [p q r]
                                         (v/vdot (v/vcross (v/v- q p) (v/v- r p)) n))]
                            (and (neg? (* (orient a b c) (orient a b d)))
                                 (neg? (* (orient c d a) (orient c d b))))))]
             (not (or (cross? p0 p1 p2 p3)
                      (cross? p1 p2 p3 p0)))))
         ops))))

(deftest surround-batch-never-collapses-to-zero-geometry-test
  (let [salt 20260803
        body {:x 0.5 :y 0.0 :z 0.5
              :width 1.0 :height 1.0 :depth 1.0}
        states (reductions
                (fn [state tick]
                  (academy-arc/tick-surround-state
                   state :normal salt tick))
                (academy-arc/initial-surround-state :normal salt)
                (range 1 201))]
    (is (every?
         (fn [state]
           (seq (academy-arc/surround-arc-ops
                 (v/v3 0.0 2.0 -3.0)
                 body
                 :normal
                 state
                 salt)))
         states))))

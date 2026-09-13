(ns cn.li.ac.tutorial.model-test
  (:require [clojure.test :refer :all]
            [cn.li.ac.tutorial.model :as model]
            [cn.li.ac.tutorial.registry :as registry]))

;; --- fresh-state ---

(deftest fresh-state-shape
  (let [s (model/fresh-state)]
    (is (set? (:activated-tuts s)))
    (is (nil? (:misaka-id s)))
    (is (true? (:first-open? s)))))

;; --- activate-tutorial / is-activated? ---

(deftest activate-and-check
  (let [s (model/fresh-state)
        s2 (model/activate-tutorial s :ores)]
    (is (model/is-activated? s2 :ores))
    (is (not (model/is-activated? s2 :welcome)))
    ;; idempotent
    (let [s3 (model/activate-tutorial s2 :ores)]
      (is (= (count (:activated-tuts s3))
             (count (:activated-tuts s2)))))))

;; --- ensure-misaka-id ---

(deftest misaka-id-assignment
  (let [s (model/ensure-misaka-id (model/fresh-state))]
    (is (integer? (:misaka-id s)))
    (is (>= (:misaka-id s) 1000))
    (is (< (:misaka-id s) 19000))
    ;; idempotent
    (let [s2 (model/ensure-misaka-id s)]
      (is (= (:misaka-id s) (:misaka-id s2))))))

;; --- first-open ---

(deftest first-open-flag
  (let [s (model/fresh-state)]
    (is (true? (model/first-open? s)))
    (let [s2 (model/mark-first-open-done! s)]
      (is (false? (model/first-open? s2))))))

;; --- registry ---

(deftest registry-count
  (is (= 13 (count (registry/all-tutorials)))))

(deftest registry-tutorial-by-id
  (is (some? (registry/tutorial-by-id :welcome)))
  (is (some? (registry/tutorial-by-id :ores)))
  (is (nil? (registry/tutorial-by-id :nonexistent))))

(deftest registry-default-installed
  (let [default-installed (filter :default-installed? (registry/all-tutorials))]
    (is (= 5 (count default-installed)))
    (is (every? #(empty? (:conditions %)) default-installed))))

(deftest registry-conditional
  (let [conditional (remove :default-installed? (registry/all-tutorials))]
    (is (= 8 (count conditional)))
    (is (every? #(seq (:conditions %)) conditional))))

;; --- group-by-activated ---

(deftest group-by-activated-fresh
  (let [state (model/fresh-state)
        {:keys [learned unlearned]} (registry/group-by-activated state)]
    ;; fresh state: only default-installed tutorials are "learned"
    (is (= 5 (count learned)))
    (is (= 8 (count unlearned)))
    (is (some #(= :welcome (:id %)) learned))
    (is (some #(= :ores (:id %)) unlearned))))

(deftest group-by-activated-after-unlock
  (let [state (-> (model/fresh-state)
                  (model/activate-tutorial :ores)
                  (model/activate-tutorial :terminal))
        {:keys [learned unlearned]} (registry/group-by-activated state)]
    (is (= 7 (count learned)))   ;; 5 default + 2 unlocked
    (is (= 6 (count unlearned))) ;; 8 conditional - 2 unlocked
    (is (some #(= :ores (:id %)) learned))
    (is (some #(= :terminal (:id %)) learned))))

;; --- energy_bridge not registered ---

(deftest energy-bridge-not-registered
  (is (nil? (registry/tutorial-by-id :energy_bridge))
      "energy_bridge.md exists but should NOT be registered (matches upstream AC)"))

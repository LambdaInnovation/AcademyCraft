(ns cn.li.ac.tutorial.conditions-test
  "Tests for condition index, matching, and tutorial activation logic."
  (:require [clojure.test :refer :all]
            [cn.li.ac.tutorial.conditions :as conds]
            [cn.li.ac.tutorial.registry :as registry]
            [cn.li.ac.tutorial.model :as model]))

(use-fixtures :each
  (fn [t]
    (conds/reset-condition-index!)
    (t)
    (conds/reset-condition-index!)))

;; --- build-condition-index ---

(deftest build-condition-index-deduplication
  (let [tutorials [{:id :a :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:foo"}
                                 {:type :item-obtained :item-id "academy:bar"}]}
                   {:id :b :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:foo"}]}]
        idx (conds/build-condition-index tutorials)]
    ;; "academy:foo" appears twice but should be deduplicated
    (is (= 2 (count idx)))
    (is (= {:type :item-obtained :item-id "academy:foo"} (first idx)))
    (is (= {:type :item-obtained :item-id "academy:bar"} (second idx)))))

(deftest build-condition-index-empty
  (is (= [] (conds/build-condition-index [])))
  (is (= [] (conds/build-condition-index
              [{:id :a :default-installed? true :conditions []}]))))

(deftest condition-index-stability
  (let [tutorials [{:id :a :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:a"}]}
                   {:id :b :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:b"}]}]
        idx (conds/build-condition-index tutorials)]
    (is (= 2 (count idx)))
    (is (= 0 (.indexOf idx {:type :item-obtained :item-id "academy:a"})))
    (is (= 1 (.indexOf idx {:type :item-obtained :item-id "academy:b"})))))

;; --- find-matching-conditions ---

(deftest find-matching-obtained-condition
  (conds/ensure-condition-index!
    [{:id :a :default-installed? false
      :conditions [{:type :item-obtained :item-id "academy:foo"}]}])
  ;; :item-obtained matches :item-crafted, :item-smelted, :item-pickup
  (is (= [0] (conds/find-matching-conditions "academy:foo" :item-crafted)))
  (is (= [0] (conds/find-matching-conditions "academy:foo" :item-smelted)))
  (is (= [0] (conds/find-matching-conditions "academy:foo" :item-pickup))))

(deftest find-matching-exact-type
  (conds/ensure-condition-index!
    [{:id :a :default-installed? false
      :conditions [{:type :item-crafted :item-id "academy:bar"}]}])
  (is (= [0] (conds/find-matching-conditions "academy:bar" :item-crafted)))
  ;; :item-crafted condition should NOT match :item-smelted
  (is (= [] (conds/find-matching-conditions "academy:bar" :item-smelted))))

(deftest find-matching-no-match
  (conds/ensure-condition-index!
    [{:id :a :default-installed? false
      :conditions [{:type :item-obtained :item-id "academy:a"}]}])
  ;; Different item-id should not match
  (is (= [] (conds/find-matching-conditions "academy:other" :item-crafted)))
  (is (= [] (conds/find-matching-conditions "academy:other" :item-smelted))))

;; --- check-new-activations ---

(deftest check-new-activations-single-unlock
  (conds/reset-condition-index!)
  (let [tutorials [{:id :ores :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:constrained_ore"}]}
                   {:id :welcome :default-installed? true :conditions []}]
        _ (conds/ensure-condition-index! tutorials)
        cond-map (conds/build-tutorial-condition-map tutorials)]
    ;; Fresh state — nothing activated
    (let [fresh (model/fresh-state)]
      (is (= #{} (conds/check-new-activations fresh cond-map))))
    ;; After marking condition 0 (constrained_ore) — ores should activate
    (let [s1 (model/mark-condition! (model/fresh-state) 0)]
      (is (= #{:ores} (conds/check-new-activations s1 cond-map))))
    ;; After activating ores — nothing new
    (let [s2 (model/activate-tutorial (model/mark-condition! (model/fresh-state) 0) :ores)]
      (is (= #{} (conds/check-new-activations s2 cond-map))))))

(deftest check-new-activations-multiple-conditions-or
  (conds/reset-condition-index!)
  (let [tutorials [{:id :ores :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:a"}
                                 {:type :item-obtained :item-id "academy:b"}]}]
        _ (conds/ensure-condition-index! tutorials)
        cond-map (conds/build-tutorial-condition-map tutorials)]
    ;; Condition 0 met → ores should activate
    (let [s (model/mark-condition! (model/fresh-state) 0)]
      (is (= #{:ores} (conds/check-new-activations s cond-map))))
    ;; Condition 1 met (alone) → ores should also activate
    (let [s (model/mark-condition! (model/fresh-state) 1)]
      (is (= #{:ores} (conds/check-new-activations s cond-map))))))

(deftest check-new-activations-empty-cond-map
  (is (= #{} (conds/check-new-activations (model/fresh-state) {}))))

;; --- build-tutorial-condition-map ---

(deftest build-tutorial-condition-map-excludes-default
  (conds/reset-condition-index!)
  (let [tutorials [{:id :welcome :default-installed? true :conditions []}
                   {:id :ores :default-installed? false
                    :conditions [{:type :item-obtained :item-id "academy:foo"}]}]
        _ (conds/ensure-condition-index! tutorials)
        cond-map (conds/build-tutorial-condition-map tutorials)]
    ;; Only ores should be in the condition map (welcome has no conditions)
    (is (= 1 (count cond-map)))
    (is (contains? cond-map :ores))
    (is (not (contains? cond-map :welcome)))))

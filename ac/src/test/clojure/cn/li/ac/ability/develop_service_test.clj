(ns cn.li.ac.ability.develop-service-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.model.develop :as dev]
            [cn.li.ac.ability.model.resource :as rdata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill :as skill]
            [cn.li.ac.ability.rules.develop-rules :as develop]))

(deftest start-skill-learning-guards-test
  (let [developing (assoc (dev/new-develop-data) :state :developing)]
    (is (= :already-developing (:error (develop/start-skill-learning developing :normal :s1)))))
  (with-redefs [skill/get-skill (fn [_] nil)]
    (is (= :unknown-skill
           (:error (develop/start-skill-learning (dev/new-develop-data) :normal :missing)))))
  (with-redefs [skill/get-skill (fn [_] {:id :s1 :level 2})]
    (let [{:keys [develop-data error]}
          (develop/start-skill-learning (dev/new-develop-data) :advanced :s1)]
      (is (nil? error))
      (is (= :developing (:state develop-data)))
      (is (= :learn-skill (:action-type develop-data)))
      (is (= {:skill-id :s1} (:action-data develop-data)))
      (is (= (dev/skill-learning-stims 2) (:max-stim develop-data))))))

(deftest start-level-up-guards-test
  (let [developing (assoc (dev/new-develop-data) :state :developing)]
    (is (= :already-developing (:error (develop/start-level-up developing :portable 1)))))
  (is (= :max-level (:error (develop/start-level-up (dev/new-develop-data) :portable 5))))
  (let [{:keys [develop-data error]}
        (develop/start-level-up (dev/new-develop-data) :normal 3)]
    (is (nil? error))
    (is (= :level-up (:action-type develop-data)))
    (is (= {:target-level 4} (:action-data develop-data)))
    (is (= (dev/level-up-stims 3) (:max-stim develop-data)))))

(deftest tick-develop-completed-and-incomplete-test
  (let [idle (dev/new-develop-data)]
    (is (= {:develop-data idle :completed? false}
           (develop/tick-develop idle))))
  (let [started (dev/start-develop (dev/new-develop-data) :advanced :learn-skill {:skill-id :s1} 1)
        ret (first (filter :completed?
                           (map develop/tick-develop
                                (take 1000 (rest (iterate dev/tick-develop started))))))]
    (is (some? ret))
    (is (true? (:completed? ret)))
    (is (= :learn-skill (:action-type ret)))
    (is (= {:skill-id :s1} (:action-data ret)))))

(deftest apply-completion-learn-skill-branch-test
  (let [develop-data (-> (dev/new-develop-data)
                         (assoc :state :done
                                :action-type :learn-skill
                                :action-data {:skill-id :s1}))
        ability-data (adata/new-ability-data)
        resource-data (rdata/new-resource-data)]
    (with-redefs [evt/make-skill-learn-event (fn [uuid sid]
                           (is (= "u1" uuid))
                           (is (= :s1 sid))
                           {:event/type :ability/skill-learn :uuid uuid :skill-id sid})
          rdata/recalc-max-values (fn [d level]
                (assoc d
                  :max-cp (+ (:max-cp d) level 10.0)
                  :max-overload (+ (:max-overload d) level 5.0)))
          evt/fire-calc-event! (fn [_event-key value _extra] value)]
      (let [{:keys [ability-data resource-data events develop-data]}
            (develop/apply-completion develop-data ability-data resource-data "u1")]
        (is (contains? (:learned-skills ability-data) :s1))
        (is (= 1 (count events)))
        (is (= :ability/skill-learn (:event/type (first events))))
     (is (= (+ (:max-cp (rdata/new-resource-data)) 11.0)
       (:max-cp resource-data)))
        (is (= :idle (:state develop-data)))))))

(deftest apply-completion-level-up-and-fallback-test
  (let [ability-data (assoc (adata/new-ability-data) :level 2)
        resource-data (assoc (rdata/new-resource-data) :add-max-cp 10.0 :add-max-overload 20.0)
        level-up-dev (-> (dev/new-develop-data)
                         (assoc :state :done
                                :action-type :level-up
                                :action-data {:target-level 3}))]
    (with-redefs [rdata/reset-add-max (fn [d] (assoc d :add-max-cp 0.0 :add-max-overload 0.0))
                  rdata/recalc-max-values (fn [d level] (assoc d :level-mark level))
                  evt/make-level-change-event (fn [uuid old-level new-level]
                                                {:event/type :ability/level-change
                                                 :uuid uuid
                                                 :old-level old-level
                                                 :new-level new-level})]
      (let [{:keys [ability-data resource-data events develop-data]}
            (develop/apply-completion level-up-dev ability-data resource-data "u2")]
        (is (= 3 (:level ability-data)))
        (is (= 0.0 (:add-max-cp resource-data)))
        (is (= 3 (:level-mark resource-data)))
        (is (= {:event/type :ability/level-change
                :uuid "u2"
                :old-level 2
                :new-level 3}
               (first events)))
        (is (= :idle (:state develop-data)))))
    (let [unknown-dev (-> (dev/new-develop-data)
                          (assoc :state :done :action-type :unknown-action))]
      (is (= [] (:events (develop/apply-completion unknown-dev ability-data resource-data "u2")))))))

(deftest tick-develop-public-boundary-test
  (let [started (develop/start-level-up (dev/new-develop-data) :normal 1)
        {:keys [develop-data completed?]} (develop/tick-develop (:develop-data started))]
    (is (false? completed?))
    (is (= :developing (:state develop-data)))))

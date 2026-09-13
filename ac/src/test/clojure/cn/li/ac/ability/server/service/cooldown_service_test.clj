(ns cn.li.ac.ability.server.service.cooldown-service-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.hud :as hud]
            [cn.li.ac.ability.model.cooldown :as model]
            [cn.li.ac.ability.rules.cooldown-rules :as svc]))

(deftest wrappers-delegate-to-model-test
  (let [d0 (model/new-cooldown-data)
    d1 (:data (svc/set-cooldown d0 :arc-gen 10 :main))
    d2 (:data (svc/set-cooldown d1 :arc-gen 3 :sub))
    d3 (:data (svc/server-tick d2))]
  (is (true? (svc/in-cooldown? d1 :arc-gen :main)))
  (is (= 10 (svc/get-remaining-ticks d2 :arc-gen :main)))
  (is (= 9 (svc/get-remaining-ticks d3 :arc-gen :main)))
  (is (= 2 (svc/get-remaining-ticks d3 :arc-gen :sub)))))

;; ============================================================================
;; Duration resolution — one rule for both apply paths and the HUD estimate
;; ============================================================================

(def ^:private ctx {:player-id "p1" :skill-id :arc-gen :exp 0.5})

(deftest policy-ticks-wins-over-cooldown-ticks-test
  ;; context-state/apply-main-cooldown! used to read :cooldown-ticks only, so a
  ;; skill declaring its duration under :cooldown-policy got one tick.
  (is (= 200 (svc/resolve-ticks {:cooldown-policy {:ticks 200} :cooldown-ticks 40} ctx)))
  (is (= 40 (svc/resolve-ticks {:cooldown-ticks 40} ctx)))
  (testing "an undeclared cooldown is effectively none"
    (is (= 1 (svc/resolve-ticks {} ctx)))))

(deftest fn-declarations-work-in-both-authored-shapes-test
  ;; Skills author both (fn [player-id skill-id exp]) and (fn [{:keys [exp]}]).
  ;; The old context-state path called (int spec) on these and would have thrown.
  (is (= 60 (svc/resolve-ticks {:cooldown-ticks (fn [_ _ exp] (* 120 (- 1.0 exp)))} ctx)))
  (is (= 60 (svc/resolve-ticks {:cooldown-ticks (fn [{:keys [exp]}] (* 120 (- 1.0 exp)))} ctx)))
  (testing "fractional ticks round rather than truncate"
    (is (= 61 (svc/resolve-ticks {:cooldown-ticks (fn [_ _ _] 60.6)} ctx)))))

(deftest hud-estimate-resolves-to-the-applied-duration-test
  ;; build-skill-slot-shape carries the declaration, patch-skill-slot-cooldown
  ;; resolves it — both through the same rule the apply paths use, so an idle
  ;; slot's estimate equals what a cast would actually set.
  (let [spec {:cooldown-policy {:ticks (fn [_ _ exp] (* 120 (- 1.0 exp)))}}]
    (is (= (svc/resolve-ticks spec ctx)
           (svc/resolve-ticks-value (svc/ticks-spec spec) ctx))))
  (testing "and the HUD keeps that same value on the slot"
    (let [spec {:cooldown-ticks 240}
          slot (first (hud/patch-skill-slot-cooldown
                       [{:skill-id :arc-gen :cooldown-total-spec (svc/ticks-spec spec)}]
                       {}
                       {:player-id "p1" :skill-exps {}}))]
      (is (= 240 (:cooldown-total slot))))))

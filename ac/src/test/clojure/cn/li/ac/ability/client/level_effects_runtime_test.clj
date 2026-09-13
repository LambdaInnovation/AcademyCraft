(ns cn.li.ac.ability.client.level-effects-runtime-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.level-effects :as level-effects]))

(defn- reset-fixture [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (try
    (f)
    (finally
      (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- handler
  [ops]
  {:initial-state {:x [1]}
   :enqueue-state-fn (fn [state _ _ _ _] state)
   :tick-state-fn (fn [state] state)
   :build-plan-fn (fn [_ _ _ _] {:ops ops})})

(deftest level-effect-plan-and-freeze-behavior-test
  (level-effects/register-level-effect! :a (handler [{:op :a}]))
  (level-effects/register-level-effect! :a (handler [{:op :ignored-duplicate}]))
  (level-effects/register-level-effect! :b (handler [{:op :b}]))
  (is (= [:a :b] (:order (level-effects/level-effect-registry-snapshot))))
  (is (= {:ops [{:op :a} {:op :b}]
          :local-walk-speed nil}
         (level-effects/build-level-effect-plan nil nil 0 nil)))
  (level-effects/freeze-level-effect-registry!)
  (is (thrown-with-msg?
        clojure.lang.ExceptionInfo
        #"frozen"
        (level-effects/register-level-effect! :c (handler [{:op :c}])))))

(deftest idle-effect-never-invokes-build-plan-fn-test
  (let [calls* (atom 0)]
    (level-effects/register-level-effect!
      :idle {:enqueue-state-fn (fn [state _ _ _ _] state)
             :tick-state-fn (fn [state] state)
             :build-plan-fn (fn [_ _ _ _] (swap! calls* inc) {:ops [{:op :idle}]})})
    (is (false? (level-effects/any-level-effect-active?)))
    (is (nil? (level-effects/build-level-effect-plan nil nil 0 nil)))
    (is (zero? @calls*))))

(deftest arc-state-auto-empties-and-goes-idle-test
  (level-effects/register-level-effect!
    :ttl-one {:initial-state {:items {}}
              :enqueue-state-fn (fn [state _ _ _ payload]
                                   (assoc-in state [:items :o] [payload]))
              :tick-state-fn (fn [state] (update state :items empty))
              :build-plan-fn (fn [_ _ _ _] {:ops [{:op :live}]})})
    (level-effects/enqueue-level-effect! :ttl-one "ctx" :ch {:v 1} :owner-key :o)
    (is (true? (level-effects/any-level-effect-active?)))
    (level-effects/tick-level-effects!)
    (is (nil? (level-effects/effect-state-snapshot :ttl-one)))
    (is (false? (level-effects/any-level-effect-active?))))

(deftest tick-state-fn-not-called-while-idle-test
  (let [ticks* (atom 0)]
    (level-effects/register-level-effect!
      :idle-tick {:enqueue-state-fn (fn [state _ _ _ _] state)
                  :tick-state-fn (fn [state] (swap! ticks* inc) state)
                  :build-plan-fn (fn [_ _ _ _] nil)})
    (level-effects/tick-level-effects!)
    (level-effects/tick-level-effects!)
    (is (zero? @ticks*))))

(deftest custom-empty-state-predicate-is-honored-test
  (level-effects/register-level-effect!
    :scalar-state {:initial-state {:count 0}
                   :empty-state? (fn [state] (zero? (:count state)))
                   :enqueue-state-fn (fn [state _ _ _ _] (update (or state {:count 0}) :count inc))
                   :tick-state-fn (fn [state] state)
                   :build-plan-fn (fn [_ _ _ _] {:ops [{:op :scalar}]})})
  (is (false? (level-effects/any-level-effect-active?)))
  (level-effects/enqueue-level-effect! :scalar-state "ctx" :ch {} :owner-key :o)
  (is (true? (level-effects/any-level-effect-active?)))
  (is (= {:ops [{:op :scalar}] :local-walk-speed nil}
         (level-effects/build-level-effect-plan nil nil 0 nil))))

(deftest current-fov-offset-aggregates-registered-contributors-test
  (level-effects/register-level-effect!
    :no-fov {:initial-state {:x [1]}
             :enqueue-state-fn (fn [state _ _ _ _] state)
             :tick-state-fn (fn [state] state)
             :build-plan-fn (fn [_ _ _ _] {:ops []})})
  (level-effects/register-level-effect!
    :fov-a {:initial-state {:x [1]}
            :enqueue-state-fn (fn [state _ _ _ _] state)
            :tick-state-fn (fn [state] state)
            :build-plan-fn (fn [_ _ _ _] {:ops []})
            :fov-offset-fn (fn [player-uuid]
                             (when (= "p1" (str player-uuid)) 6.5))})
  (level-effects/register-level-effect!
    :fov-b {:initial-state {:x [1]}
            :enqueue-state-fn (fn [state _ _ _ _] state)
            :tick-state-fn (fn [state] state)
            :build-plan-fn (fn [_ _ _ _] {:ops []})
            :fov-offset-fn (fn [_] 3.0)})
  (is (= 6.5 (level-effects/current-fov-offset "p1")) "max contribution wins")
  (is (= 3.0 (level-effects/current-fov-offset "other")) "unmatched contributor falls back to the unconditional one")
  (is (= 3.0 (level-effects/current-fov-offset nil)) "nil player uuid keeps unconditional contributions"))

(deftest one-effects-throw-does-not-stop-the-others-test
  ;; Both loops run every effect in registration order. Without isolation a
  ;; single throwing effect aborted the loop, so every effect registered after
  ;; it silently stopped advancing (tick) or contributing ops (build-plan)
  ;; while still rendering from stale state — a frozen visual on a skill that
  ;; has nothing to do with the actual bug.
  (level-effects/reset-effect-failure-reports-for-test!)
  (let [ticked* (atom [])]
    (level-effects/register-level-effect!
      :boom {:initial-state {:x [1]}
             :enqueue-state-fn (fn [state _ _ _ _] state)
             :tick-state-fn (fn [_] (throw (ex-info "boom" {})))
             :build-plan-fn (fn [_ _ _ _] (throw (ex-info "boom-plan" {})))})
    (level-effects/register-level-effect!
      :after {:initial-state {:x [1]}
              :enqueue-state-fn (fn [state _ _ _ _] state)
              :tick-state-fn (fn [state] (swap! ticked* conj :after) state)
              :build-plan-fn (fn [_ _ _ _] {:ops [{:op :after}]})})
    (level-effects/tick-level-effects!)
    (level-effects/tick-level-effects!)
    (is (= [:after :after] @ticked*)
        "an effect registered after a throwing one must keep ticking")
    (is (= {:ops [{:op :after}] :local-walk-speed nil}
           (level-effects/build-level-effect-plan nil nil 0 nil))
        "the throwing effect contributes no ops; the rest of the plan survives")
    ;; The throwing effect keeps its last good state rather than being dropped.
    (is (some? (level-effects/effect-state-snapshot :boom)))))

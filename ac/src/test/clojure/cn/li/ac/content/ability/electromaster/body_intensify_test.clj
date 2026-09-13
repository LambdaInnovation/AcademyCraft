(ns cn.li.ac.content.ability.electromaster.body-intensify-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.electromaster.body-intensify :as body-intensify]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.effects.potion :as potion-effects]))

(def ^:private apply-buffs! #'body-intensify/apply-body-intensify-buffs!)

(defn- with-buff-config [effect-entries f]
  (with-redefs [skill-config/tunable-string-list (fn [_ _] effect-entries)
                skill-config/tunable-double (fn [_ field-id]
                                             (case field-id
                                               :effect.probability-offset-ticks 0.0
                                               :effect.probability-divisor 20.0
                                               :effect.hunger-multiplier 3.0
                                               :progression.exp-use 0.02
                                               0.0))
                skill-config/lerp-double (fn [_ field-id _]
                                           (case field-id
                                             :effect.duration-multiplier 5.0
                                             :cost.down.overload 100.0
                                             0.0))
                skill-config/tunable-int (fn [_ field-id]
                                           (case field-id
                                             :charge.min-ticks 10
                                             :charge.max-ticks 40
                                             :charge.max-tolerant-ticks 100
                                             :effect.hunger-amplifier 0
                                             0))
                skill-config/lerp-int (fn [_ _ _] 25)
                clojure.core/rand (fn [] 0.0)]
    (f)))

(deftest apply-body-intensify-buffs-preserves-original-first-pick-test
  (let [applied* (atom [])]
    (with-buff-config ["speed:3" "jump-boost:1" "regeneration:1"]
      #(with-redefs [potion-effects/available? (constantly true)
                     potion-effects/apply-effect! (fn [player-uuid effect duration amplifier]
                                                    (swap! applied* conj
                                                           {:player-uuid player-uuid
                                                            :effect effect
                                                            :duration duration
                                                            :amplifier amplifier})
                                                    true)]
         (@apply-buffs! "player-a" 20 0.2)
         (is (= :jump-boost (:effect (first @applied*))))
         (is (not-any? (fn [applied]
                         (= :speed (:effect applied)))
                       @applied*))
         (is (= :hunger (:effect (last @applied*))))))))

(deftest max-charge-preserves-original-no-damage-boost-test
  (let [applied* (atom [])]
    (with-buff-config ["speed:3" "jump-boost:1" "regeneration:1"
                       "strength:1" "resistance:1"]
      #(with-redefs [potion-effects/available? (constantly true)
                     potion-effects/apply-effect! (fn [_player-uuid effect _duration _amplifier]
                                                    (swap! applied* conj effect)
                                                    true)]
         (@apply-buffs! "player-damage" 40 0.2)
         (is (= [:jump-boost :regeneration :hunger] @applied*))
         (is (not-any? #{:strength} @applied*))))))

(deftest apply-body-intensify-buffs-with-empty-pool-still-applies-hunger-test
  (let [applied* (atom [])]
    (with-buff-config []
      #(with-redefs [potion-effects/available? (constantly true)
                     potion-effects/apply-effect! (fn [player-uuid effect duration amplifier]
                                                            (swap! applied* conj {:player-uuid player-uuid
                                                                                  :effect effect
                                                                                  :duration duration
                                                                                  :amplifier amplifier})
                                                            true)]
         (@apply-buffs! "player-b" 20 0.2)
         (is (= 1 (count @applied*)))
         (is (= :hunger (:effect (first @applied*))))))))

(defn- test-owner [player-id]
  {:logical-side :server :server-session-id :test-session :player-uuid player-id})

(defn- seed-charge-context!
  "body-intensify-up! ignores the (never-populated-in-production) hold-ticks
  positional argument and instead self-tracks charge duration in
  :skill-state, so tests must seed a real registered context rather than
  passing :hold-ticks through cb/apply-invoke."
  [player-id ctx-id ticks]
  (let [owner (test-owner player-id)]
    (ctx/with-context-owner owner
      (ctx/register-context!
       (assoc (ctx/new-server-context player-id :body-intensify ctx-id owner)
              :status ctx/STATUS-ALIVE))
      (ctx-skill/update-skill-state-root! ctx-id identity {:hold-ticks ticks}))))

(defn- invoke-as-owner!
  "Skill callbacks always run inside context-state's owner binding in
  production; cb/apply-invoke does not establish one, and every :skill-state
  read here resolves its context through that binding."
  [action-fn player-id ctx-id]
  (ctx/with-context-owner (test-owner player-id)
    (cb/apply-invoke action-fn :player-id player-id :ctx-id ctx-id :exp 0.5)))

(deftest up-action-requires-min-charge-before-performing-test
  (let [up-fn (get-in body-intensify/body-intensify [:actions :up!])
        applied* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        cooldown-exp* (atom [])
        fx-calls* (atom [])
        context-registry-val (ctx/snapshot-context-registry)]
    (try
      (ctx/reset-contexts-for-test!)
      (with-buff-config ["speed:3"]
        #(with-redefs [potion-effects/available? (constantly true)
                       potion-effects/apply-effect! (fn [& _] (swap! applied* conj :applied) true)
                       skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                      (swap! exp-calls* conj [player-id skill-id amount])
                                                      {:data {:skill-exps {:body-intensify 0.52}}})
                       skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                          (swap! cooldown-calls* conj [player-id skill-id ticks]))
                       skill-config/lerp-int (fn [_skill-id _field-id exp]
                                               (swap! cooldown-exp* conj exp)
                                               25)
                       fx/send! (fn [& args] (swap! fx-calls* conj (vec args)) nil)]
           (seed-charge-context! "p-low" "ctx-low" 9)
           (invoke-as-owner! up-fn "p-low" "ctx-low")
           (is (empty? @applied*) "below min charge does not apply buffs")
           (is (empty? @exp-calls*))
           (is (empty? @cooldown-calls*))

           (seed-charge-context! "p-ok" "ctx-ok" 10)
           (invoke-as-owner! up-fn "p-ok" "ctx-ok")
           (is (pos? (count @applied*)) "successful release applies buffs")
           (is (= [["p-ok" :body-intensify 0.02]] @exp-calls*))
           (is (= [["p-ok" :body-intensify 25]] @cooldown-calls*))
           (is (= [0.52] @cooldown-exp*)
               "cooldown uses skill exp after the successful exp grant")
           (is (= 4 (count @fx-calls*))
               "miss and success releases each fan out to owner + nearby")
           (is (= [false false true true]
                  (mapv (fn [args] (get-in args [3 :performed?]))
                        @fx-calls*)))))
      (finally
        (ctx/reset-contexts-for-test! context-registry-val)))))

(deftest down-and-tick-use-post-cost-total-overload-floor-test
  (let [down-fn (get-in body-intensify/body-intensify [:actions :down!])
        tick-fn (get-in body-intensify/body-intensify [:actions :tick!])
        floors* (atom [])
        context-registry-val (ctx/snapshot-context-registry)]
    (try
      (ctx/reset-contexts-for-test!)
      (seed-charge-context! "p-floor" "ctx-floor" 0)
      (with-redefs [skill-effects/player-path
                    (fn [_player-id path default]
                      (if (= [:resource-data :cur-overload] path) 137.0 default))
                    skill-effects/enforce-overload-floor!
                    (fn [player-id floor]
                      (swap! floors* conj [player-id floor])
                      true)
                    fx/send! (fn [& _] nil)]
        (invoke-as-owner! down-fn "p-floor" "ctx-floor")
        (ctx/with-context-owner (test-owner "p-floor")
          (is (= 137.0
                 (get-in (ctx-skill/get-context "ctx-floor")
                         [:skill-state :overload-floor]))))
        (invoke-as-owner! tick-fn "p-floor" "ctx-floor")
        (is (= [["p-floor" 137.0]] @floors*))
        (ctx/with-context-owner (test-owner "p-floor")
          (is (= 1
                 (get-in (ctx-skill/get-context "ctx-floor")
                         [:skill-state :hold-ticks])))))
      (finally
        (ctx/reset-contexts-for-test! context-registry-val)))))

(deftest tolerant-timeout-and-abort-never-perform-test
  (let [tick-fn (get-in body-intensify/body-intensify [:actions :tick!])
        abort-fn (get-in body-intensify/body-intensify [:actions :abort!])
        fx-calls* (atom [])
        context-registry-val (ctx/snapshot-context-registry)]
    (try
      (ctx/reset-contexts-for-test!)
      (with-buff-config ["speed:3" "jump-boost:1"]
        #(with-redefs [skill-effects/enforce-overload-floor! (fn [& _] true)
                       fx/send! (fn [& args] (swap! fx-calls* conj (vec args)) nil)]
           (seed-charge-context! "p-timeout" "ctx-timeout" 99)
           (invoke-as-owner! tick-fn "p-timeout" "ctx-timeout")
           (is (= [false false]
                  (mapv (fn [args] (get-in args [3 :performed?]))
                        @fx-calls*))
               "100-tick timeout fans out a failed release")

           (reset! fx-calls* [])
           (seed-charge-context! "p-abort" "ctx-abort" 40)
           (invoke-as-owner! abort-fn "p-abort" "ctx-abort")
           (is (= [false]
                  (mapv (fn [args] (get-in args [3 :performed?]))
                        @fx-calls*))
               "abort only stops the owning client's charge effect")))
      (finally
        (ctx/reset-contexts-for-test! context-registry-val)))))

(deftest up-action-uses-current-ctx-hold-ticks-test
  (let [up-fn (get-in body-intensify/body-intensify [:actions :up!])
        applied* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        context-registry-val (ctx/snapshot-context-registry)
        owner {:logical-side :server :server-session-id :test-session :player-uuid "p-same"}]
    (try
      (ctx/reset-contexts-for-test!)
      (with-buff-config ["speed:3"]
        #(with-redefs [potion-effects/available? (constantly true)
                       potion-effects/apply-effect! (fn [& _] (swap! applied* conj :applied) true)
                       skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                      (swap! exp-calls* conj [player-id skill-id amount])
                                                      {:data {:skill-exps {:body-intensify 0.52}}})
                       skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                          (swap! cooldown-calls* conj [player-id skill-id ticks]))
                       fx/send! (fn [& _] nil)]
           (ctx/with-context-owner owner
             ;; Stale/other context for same player below min threshold.
             (ctx/register-context!
              (assoc (ctx/new-server-context "p-same" :body-intensify "ctx-old" owner)
                     :status ctx/STATUS-ALIVE))
             (ctx-skill/update-skill-state-root! "ctx-old" identity {:hold-ticks 2})

             ;; Current context at min threshold should perform.
             (ctx/register-context!
              (assoc (ctx/new-server-context "p-same" :body-intensify "ctx-new" owner)
                     :status ctx/STATUS-ALIVE))
             (ctx-skill/update-skill-state-root! "ctx-new" identity {:hold-ticks 10}))

           (invoke-as-owner! up-fn "p-same" "ctx-new")
           (is (pos? (count @applied*)) "current ctx-id hold-ticks controls perform gate")
           (is (= [["p-same" :body-intensify 0.02]] @exp-calls*))
           (is (= [["p-same" :body-intensify 25]] @cooldown-calls*))))
      (finally
        (ctx/reset-contexts-for-test! context-registry-val)))))

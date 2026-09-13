(ns cn.li.ac.content.ability.teleporter.flesh-ripping-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.content.ability.teleporter.flesh-ripping :as flesh]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.ability.effects.potion :as potion-effects]))

(defn- with-flesh-env [f]
  (skill-ctx/with-server-skill-context f))

(def ^:private hit-trace
  {:world-id "minecraft:overworld"
   :hit? true
   :target-uuid "target-1"
   :target-x 1.0 :target-y 2.0 :target-z 3.0
   :entity-x 1.0 :entity-y 2.0 :entity-z 3.0
   :target-width 0.6 :target-height 1.8})

;; fx-perform carries the entity box so the client can size the blood splash.
(def ^:private perform-payload
  {:target-x 1.0 :target-y 2.0 :target-z 3.0
   :hit? true
   :target-uuid "target-1"
   :entity-x 1.0 :entity-y 2.0 :entity-z 3.0
   :target-width 0.6 :target-height 1.8})

(deftest flesh-ripping-tick-caches-trace-and-sends-update-fx-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 12.0
                                               0.0))
                    helper/raycast-entity (fn [_ _]
                                            {:entity-uuid "target-1"
                                             :entity-x 1.0
                                             :entity-y 2.0
                                             :entity-z 3.0})
                    helper/player-position (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                    helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                    skill-effects/current-cp (fn [_] 100.0)
                    geom/world-id-of (fn [_] "minecraft:overworld")
                    skill-effects/skill-exp (fn [_ _] 0.5)]
         (cb/apply-invoke flesh/flesh-ripping-tick! :player-id "p1" :ctx-id "ctx-0" :hold-ticks 7)))
    (is (= 7 (get-in @ctx* [:skill-state :hold-ticks])))
    (is (= {:world-id "minecraft:overworld"
            :hit? true
            :target-uuid "target-1"
            :target-x 1.0
            :target-y 2.0
            :target-z 3.0
            :entity-x 1.0
            :entity-y 2.0
            :entity-z 3.0
            :target-width 0.6
            :target-height 1.8}
           (get-in @ctx* [:skill-state :trace])))
    (is (= [["ctx-0" :flesh-ripping/fx-update :update
             {:target-x 1.0
              :target-y 2.0
              :target-z 3.0
              :hit? true
              :target-uuid "target-1"
              :entity-x 1.0
              :entity-y 2.0
              :entity-z 3.0
              :target-width 0.6
              :target-height 1.8}]]
           @calls*))))

(deftest flesh-ripping-hit-critical-emits-crit-fx-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:trace hit-trace}})
        {:keys [get-context assoc-skill-state! update-skill-state-root! clear-skill-state!]}
        mocks
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        fx-calls* (atom [])
        potion-calls* (atom [])
        damage-calls* (atom [])]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    skill-effects/skill-exp (fn [_ _] 0.5)
                    skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :combat.damage 8.0
                                               :cooldown.ticks 20.0
                                               0.0))
                    skill-config/lerp-int (fn [_ _ _] 20)
                    skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :progression.exp-hit 0.003
                                                  0.0))
                    skill-config/lerp-int (fn [& _] 20)
                    skill-config/probability (fn [_ _] 0.0)
                    skill-config/tunable-int (fn [_ field-id]
                                               (case field-id
                                                 :effect.nausea-duration-ticks 60
                                                 :effect.nausea-amplifier 0
                                                 0))
                    helper/deal-magic-damage! (fn [_player-id _skill-id world-id target-uuid damage]
                                                (swap! damage-calls* conj [world-id target-uuid damage])
                                                {:critical? true
                                                 :crit-level 2
                                                 :crit-rate 2.6
                                                 :message-key "ability.teleporter.critical_hit"
                                                 :message-args ["x2.6"]
                                                 :damage-after damage
                                                 :applied? true})
                    potion-effects/available? (constantly true)
                    potion-effects/apply-effect! (fn [_ target-id effect duration amplifier]
                                                           (swap! potion-calls* conj [target-id effect duration amplifier])
                                                           true)
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp-calls* conj [player-id skill-id amount])
                                                   nil)
                    skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                       (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                       nil)
                    fx/send! (fn [_ctx-id entry _evt payload]
                               (swap! fx-calls* conj [(:topic entry) payload])
                               nil)
                    clojure.core/rand (fn [] 0.0)]
         (cb/apply-invoke flesh/flesh-ripping-up! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true)))
    (is (= [["minecraft:overworld" "target-1" 8.0]] @damage-calls*))
    (is (empty? @potion-calls*))
    (is (= [["p1" :flesh-ripping 0.003]] @exp-calls*))
    (is (= [["p1" :flesh-ripping 20]] @cooldown-calls*))
    ;; Upstream s_end sends MSG_EFFECT_END first; fx-crit-hit stays owner-only
    ;; (a per-player text message); fx-perform fans out to owner + nearby
    ;; (blood splash + sound are world-visible).
    (is (= [[:flesh-ripping/fx-perform perform-payload]
            [:flesh-ripping/fx-perform perform-payload]
            [:teleporter/fx-crit-hit {:x 1.0
                                      :y 2.0
                                      :z 3.0
                                      :crit-level 2
                                      :crit-rate 2.6
                                      :message-key "ability.teleporter.critical_hit"
                                      :message-args ["x2.6"]
                                      :target-uuid "target-1"
                                      :target-width 0.6
                                      :target-height 1.8
                                      :skill-id :flesh-ripping}]]
           @fx-calls*))))

;; crit-applied? keys off :critical? alone — upstream fires the crit event before
;; ctx.attack, so armor/invulnerability rejecting the hit must not cancel the fx.
(deftest flesh-ripping-critical-fx-survives-rejected-damage-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:trace hit-trace}})
        {:keys [get-context assoc-skill-state! update-skill-state-root! clear-skill-state!]}
        mocks
        fx-calls* (atom [])]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    skill-effects/skill-exp (fn [_ _] 0.5)
                    skill-config/lerp-double (fn [_ _ _] 0.0)
                    skill-config/lerp-int (fn [_ _ _] 20)
                    skill-config/tunable-double (fn [_ _] 0.003)
                    skill-config/probability (fn [_ _] 1.0)
                    skill-config/tunable-int (fn [_ _] 0)
                    helper/deal-magic-damage! (fn [& _]
                                              {:critical? true
                                               :crit-level 2
                                               :applied? false})
                    potion-effects/available? (constantly true)
                    potion-effects/apply-effect! (fn [& _] true)
                    skill-effects/add-skill-exp! (fn [& _] nil)
                    skill-effects/set-main-cooldown! (fn [& _] nil)
                    fx/send! (fn [_ctx-id entry _evt payload]
                               (swap! fx-calls* conj [(:topic entry) payload])
                               nil)
                    clojure.core/rand (fn [] 0.0)]
         (cb/apply-invoke flesh/flesh-ripping-up! :player-id "p1" :ctx-id "ctx-1b" :cost-ok? true)))
    (is (= [[:flesh-ripping/fx-perform perform-payload]
            [:flesh-ripping/fx-perform perform-payload]
            [:teleporter/fx-crit-hit {:x 1.0
                                      :y 2.0
                                      :z 3.0
                                      :crit-level 2
                                      :crit-rate nil
                                      :message-key nil
                                      :message-args nil
                                      :target-uuid "target-1"
                                      :target-width 0.6
                                      :target-height 1.8
                                      :skill-id :flesh-ripping}]]
           @fx-calls*))))

(deftest flesh-ripping-up-cost-fail-has-no-side-effects-test
  ;; Upstream has no cost-fail path (s_end force-consumes; unaffordable
  ;; contexts terminate in s_tick), so a failed local cost check still ends
  ;; the effect visibly (marker dies) but deals no damage / exp / cooldown.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:trace hit-trace}})
        {:keys [get-context]} mocks
        damage-calls* (atom 0)
        exp-calls* (atom 0)
        cooldown-calls* (atom 0)
        fx-calls* (atom 0)]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    skill-effects/skill-exp (fn [_ _] 0.5)
                    skill-config/lerp-double (fn [_ _ _] 0.0)
                    helper/deal-magic-damage! (fn [& _] (swap! damage-calls* inc))
                    skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc))
                    skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc))
                    fx/send! (fn [& _] (swap! fx-calls* inc) nil)]
         (cb/apply-invoke flesh/flesh-ripping-up! :player-id "p1" :ctx-id "ctx-2" :cost-ok? false)))
    (is (= 0 @damage-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))
    (is (= 2 @fx-calls*))))

(deftest flesh-ripping-miss-still-ends-effect-but-no-damage-test
  ;; Upstream s_end sends MSG_EFFECT_END unconditionally (marker dies even on
  ;; a miss); only damage/exp/cooldown are target-gated.
  (let [exp-calls* (atom 0)
        cooldown-calls* (atom 0)
        fx-calls* (atom 0)
        damage-calls* (atom 0)]
    (with-flesh-env
      #(with-redefs [ctx/get-context (fn ([_ctx-id] {:skill-state {:trace nil}})
                                       ([_ _ctx-id] {:skill-state {:trace nil}}))
                    skill-effects/skill-exp (fn [_ _] 0.5)
                    skill-config/lerp-double (fn [_ _ _] 0.0)
                    helper/raycast-entity (fn [_ _] nil)
                    helper/deal-magic-damage! (fn [& _] (swap! damage-calls* inc))
                    skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc))
                    skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc))
                    fx/send! (fn [& _] (swap! fx-calls* inc) nil)]
         (cb/apply-invoke flesh/flesh-ripping-up! :player-id "p1" :ctx-id "ctx-3" :cost-ok? true)))
    (is (= 0 @damage-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))
    (is (= 2 @fx-calls*))))

(deftest flesh-ripping-abort-clears-state-and-sends-end-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 3 :trace {:hit? true}}})
        {:keys [ctx* clear-skill-state!]} mocks
        fx-topics* (atom [])]
    (with-flesh-env
      #(with-redefs [ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! (fn [& args]
                               (swap! fx-topics* conj (:topic (second args)))
                               nil)]
         (cb/apply-invoke flesh/flesh-ripping-abort! :ctx-id "ctx-4")))
    (is (nil? (:skill-state @ctx*)))
    (is (= [:flesh-ripping/fx-end] @fx-topics*))))

(deftest flesh-ripping-down-sends-start-with-first-trace-test
  ;; Upstream l_startEffect spawns the marker on MSG_MADEALIVE.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 12.0
                                               0.0))
                    helper/raycast-entity (fn [_ _]
                                            {:entity-uuid "target-1"
                                             :entity-x 1.0
                                             :entity-y 2.0
                                             :entity-z 3.0})
                    helper/player-position (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                    helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                    geom/world-id-of (fn [_] "minecraft:overworld")
                    skill-effects/skill-exp (fn [_ _] 0.5)]
         (cb/apply-invoke flesh/flesh-ripping-down! :player-id "p1" :ctx-id "ctx-d")))
    (is (= :flesh-ripping/fx-start (get-in (first @calls*) [1])))
    (is (= :start (get-in (first @calls*) [2])))
    (let [payload (get-in (first @calls*) [3])]
      (is (= true (:hit? payload)))
      (is (= "target-1" (:target-uuid payload))))))

(deftest flesh-ripping-miss-charges-no-cp-test
  ;; Upstream s_end charges only on a hit — the dynamic cost fns must return
  ;; 0 for a miss so apply-cost! deducts nothing.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:trace {:hit? false :target-uuid nil}}})
        {:keys [get-context]} mocks]
    (with-flesh-env
      #(with-redefs [ctx/get-context get-context
                    ctx/active-contexts (fn [_] [{:id "ctx-c" :skill-id :flesh-ripping}])
                    skill-effects/skill-exp (fn [_ _] 0.5)
                    skill-config/lerp-double (fn [_ _ _] 200.0)]
         (is (= 0.0 (flesh/flesh-ripping-cost-up-cp "p1" :flesh-ripping 0.5)))
         (is (= 0.0 (flesh/flesh-ripping-cost-up-overload "p1" :flesh-ripping 0.5)))))))

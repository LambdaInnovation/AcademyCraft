(ns cn.li.ac.content.ability.teleporter.threatening-teleport-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.content.ability.teleporter.threatening-teleport :as tt]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- make-context-mocks [initial-ctx]
  (let [ctx* (atom initial-ctx)]
    {:ctx* ctx*
     :get-context (fn [_] @ctx*)
     :update-skill-state-root! (fn [_ f & args]
                                 (swap! ctx* update :skill-state
                                        (fn [ss]
                                          (if (and (= f identity) (= 1 (count args)))
                                            (first args)
                                            (apply f (or ss {}) args)))))}))

(deftest threatening-tp-down-sends-start-with-trace-test
  ;; Key-down spawns the aim marker immediately (upstream l_start on
  ;; MSG_MADEALIVE): fx-start carries the first trace.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ _ _] 12.0)
                  helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  geom/world-id-of (fn [_] "minecraft:overworld")
                  raycast/available? (constantly true)
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :entity
                                               :entity-id "enemy"
                                               :x 4.0 :y 5.0 :z 6.0
                                               :hit-x 4.0 :hit-y 6.0 :hit-z 6.0
                                               :height 1.95
                                               :distance 7.0})]
      (cb/apply-invoke tt/threatening-tp-down! :player-id "p1" :ctx-id "ctx-d" :player-ref :player :cost-ok? true))

    (is (= 0 (get-in @ctx* [:skill-state :hold-ticks])))
    (is (= :threatening-teleport/fx-start (get-in (first @calls*) [1])))
    (is (= :start (get-in (first @calls*) [2])))
    (let [payload (get-in (first @calls*) [3])]
      (is (= 4.0 (:drop-x payload)))
      (is (= 1.95 (:target-height payload))))))

(deftest threatening-tp-down-empty-hand-no-marker-test
  ;; Upstream s_madeAlive terminates the context without a main-hand item —
  ;; no marker, no state, and the release gate keeps it from firing.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 0)]
      (cb/apply-invoke tt/threatening-tp-down! :player-id "p1" :ctx-id "ctx-de" :player-ref :player :cost-ok? true))

    (is (= {} (:skill-state @ctx*)))
    (is (empty? @calls*))))

(deftest threatening-tp-abort-clears-state-and-sends-end-test
  ;; Key-abort (upstream l_onKeyAbort -> terminate) drops the marker.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {:hold-ticks 5 :trace {}}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/clear-skill-state! (fn [_] (swap! ctx* assoc :skill-state {}))
                  fx/send! send!]
      (cb/apply-invoke tt/threatening-tp-abort! :player-id "p1" :ctx-id "ctx-a"))

    (is (= {} (:skill-state @ctx*)))
    (is (some (fn [[_ topic _ _]] (= :threatening-teleport/fx-end topic)) @calls*))))

(deftest threatening-tp-up-cost-fail-no-side-effects-test
  (let [{:keys [get-context]} (make-context-mocks {:skill-state {:trace {:world-id "w"
                                                                          :drop-x 1.0 :drop-y 2.0 :drop-z 3.0
                                                                          :attacked? true :target-uuid "enemy"}}})
        damage-calls* (atom 0)
        exp-calls* (atom 0)
        cooldown-calls* (atom 0)
        fx-calls* (atom 0)
        ach-calls* (atom 0)]
    (with-redefs [ctx/get-context get-context
                  helper/deal-magic-damage! (fn [& _] (swap! damage-calls* inc))
                  skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc))
                  skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc))
                  fx/send! (fn [& _] (swap! fx-calls* inc))
                  ach-dispatcher/trigger-custom-event! (fn [& _] (swap! ach-calls* inc))]
      (cb/apply-invoke tt/threatening-tp-up! :player-id "p1" :ctx-id "ctx-1" :player-ref :player :cost-ok? false))

    (is (= 0 @damage-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))
    (is (= 0 @fx-calls*))
    (is (= 0 @ach-calls*))))

(deftest threatening-tp-tick-updates-trace-and-sends-update-fx-test
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ _ _] 12.0)
                  helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  geom/world-id-of (fn [_] "minecraft:overworld")
                  raycast/available? (constantly true)
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :entity
                                               :entity-id "enemy"
                                               :hit-x 4.0 :hit-y 5.0 :hit-z 6.0
                                               :distance 7.0})]
      (cb/apply-invoke tt/threatening-tp-tick! :player-id "p1" :ctx-id "ctx-2" :hold-ticks 9 :player-ref :player))

    (is (= 9 (get-in @ctx* [:skill-state :hold-ticks])))
    (is (= true (get-in @ctx* [:skill-state :trace :attacked?])))
    (is (= "enemy" (get-in @ctx* [:skill-state :trace :target-uuid])))
    ;; The trail starts half a block below the feet, not at the eye.
    (is (= [["ctx-2" :threatening-teleport/fx-update :update
             {:start-x 1.0 :start-y 1.5 :start-z 3.0
              :drop-x 4.0 :drop-y 5.0 :drop-z 6.0
              :attacked? true
              :target-uuid "enemy"
              :target-width 0.5
              :target-height 0.0}]]
           @calls*))))

(deftest threatening-tp-tick-entity-drop-point-is-top-of-box-test
  ;; Upstream calcDropPos ENTITY branch: ent.posY + ent.height (top of the
  ;; bounding box), not the ray intersection point.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ _ _] 12.0)
                  helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  geom/world-id-of (fn [_] "minecraft:overworld")
                  raycast/available? (constantly true)
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :entity
                                               :entity-id "enemy"
                                               :x 4.0 :y 5.0 :z 6.0
                                               :hit-x 4.0 :hit-y 6.0 :hit-z 6.0
                                               :height 1.95
                                               :distance 7.0})]
      (cb/apply-invoke tt/threatening-tp-tick! :player-id "p1" :ctx-id "ctx-2" :hold-ticks 9 :player-ref :player))

    (is (= 6.95 (get-in @ctx* [:skill-state :trace :drop-y]))
        "drop-y = feet 5.0 + height 1.95 (top of the target box)")
    (is (= 1.95 (get-in @ctx* [:skill-state :trace :height])))
    (let [payload (get-in (first @calls*) [3])]
      (is (= 6.95 (:drop-y payload)))
      (is (= 1.95 (:target-height payload))
          "payload carries the target height for the marker's feet placement"))))

(deftest threatening-tp-tick-empty-hand-drops-marker-test
  ;; Upstream s_tick terminates the context when the main hand is empty — the
  ;; hold visibly ends (fx-end) and no aim update is sent.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {:hold-ticks 3 :trace {}}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/clear-skill-state! (fn [_] (swap! ctx* assoc :skill-state {}))
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 0)]
      (cb/apply-invoke tt/threatening-tp-tick! :player-id "p1" :ctx-id "ctx-e" :hold-ticks 4 :player-ref :player))

    (is (= {} (:skill-state @ctx*)))
    (is (some (fn [[_ topic _ _]] (= :threatening-teleport/fx-end topic)) @calls*))))

(deftest threatening-tp-up-hit-success-test
  (let [{:keys [get-context]} (make-context-mocks {:skill-state {:trace {:world-id "minecraft:overworld"
                                                                          :start-x 1.0 :start-y 2.0 :start-z 3.0
                                                                          :drop-x 4.0 :drop-y 5.0 :drop-z 6.0
                                                                          :attacked? true
                                                                          :target-uuid "enemy"
                                                                          :width 0.6
                                                                          :height 1.95}}})
        damage-calls* (atom [])
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        fx-calls* (atom [])
        crit-fx-calls* (atom [])
        ach-calls* (atom [])
        consume-calls* (atom 0)
        drop-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ field _]
                                    (case field
                                      :combat.damage 4.0
                                      :targeting.range 10.0
                                      :cost.up.cp 60.0
                                      :cost.up.overload 12.0
                                      0.0))
                  skill-config/lerp-int (fn [_ _ _] 22)
                  skill-config/tunable-double (fn [_ field]
                                      (case field
                                        :progression.exp-base 0.003
                                        :progression.exp-hit-factor 1.0
                                        :progression.exp-miss-factor 0.2
                                        0.0))
                  skill-config/probability (fn [_ field]
                                           (case field
                                             :interaction.drop-prob.hit 0.3
                                             :interaction.drop-prob.miss 1.0
                                             0.0))
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  entity/player-get-main-hand-item-id (fn [_] nil)
                  entity/player-creative? (fn [_] false)
                  entity/player-consume-main-hand-item! (fn [_ _]
                                                         (swap! consume-calls* inc)
                                                         true)
                  entity/player-drop-main-hand-item-at! (fn [_ amount x y z]
                                                          (swap! drop-calls* conj [amount x y z])
                                                          true)
                  helper/deal-magic-damage! (fn [_player-id _skill-id world-id target-uuid damage]
                                              (swap! damage-calls* conj [world-id target-uuid damage])
                                              {:critical? true
                                               :crit-level 1
                                               :crit-rate 1.6
                                               :message-key "ability.teleporter.critical_hit"
                                               :message-args ["x1.6"]
                                               :damage-after damage
                                               :applied? true})
                  skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                 (swap! exp-calls* conj [player-id skill-id amount]))
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                     (swap! cooldown-calls* conj [player-id skill-id ticks]))
                  fx/send! (fn [ctx-id entry _evt payload]
                             (swap! fx-calls* conj [(:topic entry) payload])
                             (when (= (:topic entry) :teleporter/fx-crit-hit)
                               (swap! crit-fx-calls* conj payload))
                             nil)
                  ach-dispatcher/trigger-custom-event! (fn [player-id event-id]
                                                         (swap! ach-calls* conj [player-id event-id]))
                  rand (fn [] 0.0)]
      (cb/apply-invoke tt/threatening-tp-up! :player-id "p1" :ctx-id "ctx-3" :player-ref :player :cost-ok? true))

    (is (= [["minecraft:overworld" "enemy" 4.0]] @damage-calls*))
    (is (= 0 @consume-calls*))
    (is (= [[1 4.0 5.0 6.0]] @drop-calls*))
    (is (= [["p1" :threatening-teleport 0.003]] @exp-calls*))
    (is (= [["p1" :threatening-teleport 22]] @cooldown-calls*))
    (is (= [["p1" "teleporter.threatening_teleport"]] @ach-calls*))
    (let [perform-payload (some (fn [[topic payload]]
                                  (when (= topic :threatening-teleport/fx-perform)
                                    payload))
                                @fx-calls*)]
      (is (some? perform-payload))
      (is (= true (get perform-payload :attacked? false))))
    (is (= [{:x 4.0
             ;; Crit payload y = target FEET (drop-y top minus height) so the
             ;; client burst spreads over the box like upstream.
             :y 3.05
             :z 6.0
             :crit-level 1
             :crit-rate 1.6
             :message-key "ability.teleporter.critical_hit"
             :message-args ["x1.6"]
             :target-uuid "enemy"
             :target-width 0.6
             :target-height 1.95
             :skill-id :threatening-teleport}]
           @crit-fx-calls*))))

(deftest threatening-tp-tick-block-hit-trace-test
  ;; Firing at empty ground must work: the raycast hits a block, attacked? is
  ;; false, and the drop point is the block hit point (upstream calcDropPos
  ;; BLOCK branch).
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ _ _] 12.0)
                  helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  geom/world-id-of (fn [_] "minecraft:overworld")
                  raycast/available? (constantly true)
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :block
                                               :hit-x 4.0 :hit-y 5.0 :hit-z 6.0
                                               :distance 7.0})]
      (cb/apply-invoke tt/threatening-tp-tick! :player-id "p1" :ctx-id "ctx-b" :hold-ticks 1 :player-ref :player))

    (let [trace (get-in @ctx* [:skill-state :trace])]
      (is (some? trace))
      (is (false? (:attacked? trace)))
      (is (nil? (:target-uuid trace)))
      (is (= 4.0 (:drop-x trace)))
      (is (= 5.0 (:drop-y trace)))
      (is (= 6.0 (:drop-z trace))))))

(deftest threatening-tp-tick-miss-falls-back-to-range-endpoint-test
  ;; Aiming at open air still yields a drop point at eye + look * range
  ;; (upstream calcDropPos MISS branch), so the skill can be thrown anywhere.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! send!
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ _ _] 12.0)
                  helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  geom/world-id-of (fn [_] "minecraft:overworld")
                  raycast/available? (constantly true)
                  raycast/raycast-combined-from-player (fn [& _] nil)]
      (cb/apply-invoke tt/threatening-tp-tick! :player-id "p1" :ctx-id "ctx-m" :hold-ticks 1 :player-ref :player))

    (let [trace (get-in @ctx* [:skill-state :trace])]
      (is (some? trace))
      (is (false? (:attacked? trace)))
      (is (= 1.0 (:drop-x trace)))
      ;; eye-y 2.0 + 1.62 eye height + look.y 0.0 * range.
      (is (= 3.62 (:drop-y trace)))
      (is (= 15.0 (:drop-z trace))))))

(deftest threatening-tp-up-block-hit-drops-item-no-damage-test
  ;; Release over empty ground: no damage, item always drops at the aim point
  ;; (drop-prob.miss = 1.0), exp/cooldown/fx still settle.
  (let [{:keys [get-context]} (make-context-mocks {:skill-state {:trace {:world-id "minecraft:overworld"
                                                                          :start-x 1.0 :start-y 2.0 :start-z 3.0
                                                                          :drop-x 4.0 :drop-y 5.0 :drop-z 6.0
                                                                          :attacked? false
                                                                          :target-uuid nil}}})
        damage-calls* (atom 0)
        drop-calls* (atom [])
        fx-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ field _]
                                    (case field
                                      :combat.damage 4.0
                                      :targeting.range 10.0
                                      :cost.up.cp 60.0
                                      :cost.up.overload 12.0
                                      0.0))
                  skill-config/lerp-int (fn [_ _ _] 22)
                  skill-config/tunable-double (fn [_ field]
                                      (case field
                                        :progression.exp-base 0.003
                                        :progression.exp-hit-factor 1.0
                                        :progression.exp-miss-factor 0.2
                                        0.0))
                  skill-config/probability (fn [_ field]
                                           (case field
                                             :interaction.drop-prob.hit 0.3
                                             :interaction.drop-prob.miss 1.0
                                             0.0))
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  entity/player-get-main-hand-item-id (fn [_] nil)
                  entity/player-creative? (fn [_] false)
                  entity/player-consume-main-hand-item! (fn [& _] true)
                  entity/player-drop-main-hand-item-at! (fn [_ amount x y z]
                                                          (swap! drop-calls* conj [amount x y z])
                                                          true)
                  helper/deal-magic-damage! (fn [& _] (swap! damage-calls* inc) {})
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  ach-dispatcher/trigger-custom-event! (fn [& _] nil)
                  fx/send! (fn [_ctx-id entry _evt payload]
                             (swap! fx-calls* conj [(:topic entry) payload])
                             nil)
                  rand (fn [] 0.0)]
      (cb/apply-invoke tt/threatening-tp-up! :player-id "p1" :ctx-id "ctx-g" :player-ref :player :cost-ok? true))

    (is (= 0 @damage-calls*))
    (is (= [[1 4.0 5.0 6.0]] @drop-calls*))
    (is (some (fn [[topic _]] (= topic :threatening-teleport/fx-perform)) @fx-calls*))))

;; crit-applied? keys off :critical? alone — upstream fires the crit event before
;; ctx.attack, so armor/invulnerability rejecting the hit must not cancel the fx.
(deftest threatening-tp-up-critical-fx-survives-rejected-damage-test
  (let [{:keys [get-context]} (make-context-mocks {:skill-state {:trace {:world-id "minecraft:overworld"
                                                                          :start-x 1.0 :start-y 2.0 :start-z 3.0
                                                                          :drop-x 4.0 :drop-y 5.0 :drop-z 6.0
                                                                          :attacked? true
                                                                          :target-uuid "enemy"
                                                                          :width 0.6
                                                                          :height 1.95}}})
        fx-calls* (atom [])
        crit-fx-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-config/lerp-double (fn [_ field _]
                                    (case field
                                      :combat.damage 4.0
                                      :targeting.range 10.0
                                      0.0))
                  skill-config/lerp-int (fn [_ _ _] 22)
                  skill-config/tunable-double (fn [_ field]
                                      (case field
                                        :progression.exp-base 0.003
                                        :progression.exp-hit-factor 1.0
                                        :progression.exp-miss-factor 0.2
                                        0.0))
                  skill-config/probability (fn [_ field]
                                           (case field
                                             :interaction.drop-prob.hit 0.3
                                             :interaction.drop-prob.miss 1.0
                                             0.0))
                  entity/player-get-main-hand-item-count (fn [_] 1)
                  entity/player-get-main-hand-item-id (fn [_] nil)
                  entity/player-creative? (fn [_] false)
                  entity/player-drop-main-hand-item-at! (fn [& _] true)
                  helper/deal-magic-damage! (fn [& _]
                                              {:critical? true
                                               :crit-level 1
                                               :crit-rate 1.4
                                               :applied? false})
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  ach-dispatcher/trigger-custom-event! (fn [& _] nil)
                  fx/send! (fn [_ctx-id entry _evt payload]
                             (swap! fx-calls* conj [(:topic entry) payload])
                             (when (= (:topic entry) :teleporter/fx-crit-hit)
                               (swap! crit-fx-calls* conj payload))
                             nil)
                  rand (fn [] 0.0)]
      (cb/apply-invoke tt/threatening-tp-up! :player-id "p1" :ctx-id "ctx-4" :player-ref :player :cost-ok? true))

    (is (= 1 (count @crit-fx-calls*)))
    (is (some (fn [[topic _]] (= topic :threatening-teleport/fx-perform)) @fx-calls*))))

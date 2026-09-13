(ns cn.li.ac.content.ability.teleporter.mark-teleport-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.content.ability.teleporter.mark-teleport :as mark]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.ability.effects.motion :as motion-effects]))

(defn- with-mark-env [f]
  (skill-ctx/with-server-skill-context f))

(use-fixtures :each (fn [f]
                      (ps-fix/clean-player-states-fixture
                        #(with-mark-env f))))

(deftest mark-teleport-on-key-down-initializes-hold-state-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:legacy true}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  raycast/available? (constantly false)]
      (cb/apply-invoke mark/mark-teleport-on-key-down :ctx-id "ctx-1"))
    (is (= {:hold-ticks 0 :has-target false}
           (:skill-state @ctx*)))))

(deftest mark-teleport-on-key-down-resolves-target-and-sends-start-test
  ;; Upstream l_start spawns EntityTPMarking on MSG_MADEALIVE; fx-start
  ;; carries the first destination so the mark appears on key-down.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/current-cp (fn [_] 1000.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 60.0
                                               :cost.up.cp-per-block 4.0
                                               :targeting.eye-height 1.6
                                               0.0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  :targeting.range-per-hold-tick 2.0
                                                  0.0))
                  raycast/available? (constantly true)
                  raycast/player-position (fn [_] {:world-id "minecraft:overworld"
                                                   :x 1.0 :y 64.0 :z 3.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player (fn [& _] nil)]
      (cb/apply-invoke mark/mark-teleport-on-key-down :player-id "p1" :ctx-id "ctx-d" :player-ref :player))
    (is (= :mark-teleport/fx-start (get-in (first @calls*) [1])))
    (let [payload (get-in (first @calls*) [3])]
      (is (map? (:target payload)))
      (is (pos? (:distance payload))))
    (is (true? (get-in @ctx* [:skill-state :has-target])))))

(deftest mark-teleport-on-key-tick-sends-update-fx-test
  ;; Upstream l_update moves EntityTPMarking every tick; fx-update carries
  ;; the growing destination (2 blocks per held tick).
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 1 :has-target true}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/current-cp (fn [_] 1000.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 60.0
                                               :cost.up.cp-per-block 4.0
                                               :targeting.eye-height 1.6
                                               0.0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  :targeting.range-per-hold-tick 2.0
                                                  0.0))
                  raycast/available? (constantly true)
                  raycast/player-position (fn [_] {:world-id "minecraft:overworld"
                                                   :x 1.0 :y 64.0 :z 3.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player (fn [& _] nil)]
      (cb/apply-invoke mark/mark-teleport-on-key-tick :player-id "p1" :ctx-id "ctx-t" :player-ref :player))
    (is (= :mark-teleport/fx-update (get-in (first @calls*) [1])))
    (is (= :update (get-in (first @calls*) [2])))
    (let [payload (get-in (first @calls*) [3])]
      (is (map? (:target payload)))
      (is (pos? (:distance payload))))
    (is (= 2 (get-in @ctx* [:skill-state :hold-ticks])))))

(deftest mark-teleport-entity-hit-dest-uses-entity-feet-plus-eye-height-test
  ;; Upstream getDest: LambdaLib's entity raycast builds the result via
  ;; new RayTraceResult(entity) — hitVec is the entity's FEET position, not
  ;; the intersection — so dest.y = entity feet + eye height. Regression:
  ;; the previous code used the intersection (:hit-y), floating the marker up
  ;; to an eye-height too high.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {}})
        {:keys [get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/current-cp (fn [_] 1000.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 60.0
                                               :cost.up.cp-per-block 4.0
                                               :targeting.eye-height 1.6
                                               0.0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  :targeting.range-per-hold-tick 2.0
                                                  0.0))
                  raycast/available? (constantly true)
                  raycast/player-position (fn [_] {:world-id "minecraft:overworld"
                                                   :x 1.0 :y 64.0 :z 3.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :entity
                                               :x 8.0 :y 62.0 :z 3.0
                                               :hit-x 8.0 :hit-y 63.5 :hit-z 3.0
                                               :eye-height 1.62
                                               :distance 7.0})]
      (cb/apply-invoke mark/mark-teleport-on-key-down :player-id "p1" :ctx-id "ctx-e" :player-ref :player))
    (let [payload (get-in (first @calls*) [3])]
      ;; dest = entity feet + eye height, and the mark goes there: MarkRender
      ;; hangs the humanoid 1.5 below it, landing its feet just above the
      ;; entity's own.
      (is (= {:x 8.0 :y 63.62 :z 3.0} (:target payload))
          "the fx payload carries the dest itself"))))

(deftest mark-teleport-block-up-hit-dest-floats-above-the-surface-test
  ;; Up-face ground hit: dest is 1.8 above the top face (upstream case
  ;; UP => y += 1.8 — the player drops onto the block). The mark goes to that
  ;; same point and the humanoid hangs 1.5 below it, so it reads as standing
  ;; 0.3 above the surface.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! send!
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/current-cp (fn [_] 1000.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 60.0
                                               :cost.up.cp-per-block 4.0
                                               :targeting.eye-height 1.6
                                               0.0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  :targeting.range-per-hold-tick 2.0
                                                  0.0))
                  raycast/available? (constantly true)
                  raycast/player-position (fn [_] {:world-id "minecraft:overworld"
                                                   :x 1.0 :y 64.0 :z 3.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player (fn [& _]
                                              ;; The platform normalizes
                                              ;; :face to a keyword.
                                              {:hit-type :block
                                               :x 8.0 :y 63.0 :z 6.0
                                               :hit-x 8.0 :hit-y 64.0 :hit-z 6.0
                                               :face :up
                                               :distance 7.0})]
      (cb/apply-invoke mark/mark-teleport-on-key-down :player-id "p1" :ctx-id "ctx-b" :player-ref :player))
    (let [payload (get-in (first @calls*) [3])
          st (:skill-state @ctx*)]
      (is (= 65.8 (get-in st [:target-y])))
      (is (= {:x 8.0 :y 65.8 :z 6.0} (:target payload))
          "the fx payload carries the dest itself"))))

(deftest mark-teleport-on-key-up-short-tap-success-sends-perform-and-applies-effects-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 0 :has-target false}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        teleport-calls* (atom [])
        reset-calls* (atom [])
        dismount-calls* (atom [])
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp-calls* (atom [])
        cooldown-calls* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! send!
                  skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                 (swap! exp-calls* conj [player-id skill-id amount])
                                                 nil)
                  skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                     (swap! cooldown-calls* conj [player-id skill-id ticks])
                                                     nil)
                  skill-effects/skill-exp (fn [_ _] 0.5)
                  skill-effects/current-cp (fn [_] 1000.0)
                  skill-config/lerp-double (fn [_ field-id _]
                                             (case field-id
                                               :targeting.range 60.0
                                               :cost.up.cp-per-block 4.0
                                               :cost.up.overload 20.0
                                               :progression.exp-per-distance 0.00018
                                               0.0))
                  skill-config/lerp-int (fn [_ field-id _]
                                          (case field-id
                                            :cooldown.ticks 600
                                            0))
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  :targeting.range-per-hold-tick 4.0
                                                  :targeting.eye-height 0.0
                                                  :progression.exp-per-distance 0.00018
                                                  0.0))
                  entity/player-creative? (fn [_] false)
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [_]
                                                       {:world-id "minecraft:overworld"
                                                        :x 1.0 :y 64.0 :z 3.0})
                  motion-effects/dismount-riding! (fn [player-id]
                                                    (swap! dismount-calls* conj player-id)
                                                    true)
                  motion-effects/teleport-player! (fn [player-id world-id x y z]
                                                    (swap! teleport-calls* conj [player-id world-id x y z])
                                                    true)
                  motion-effects/reset-fall-damage! (fn [player-id]
                                                      (swap! reset-calls* conj player-id)
                                                      true)
                  raycast/available? (constantly true)
                  ;; resolve-destination prefers the live raycast position
                  raycast/player-position (fn [_] {:world-id "minecraft:overworld"
                                                   :x 1.0 :y 64.0 :z 3.0})
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player (fn [& _]
                                              {:hit-type :entity
                                               :hit-x 1.0 :hit-y 62.4 :hit-z 6.5
                                               :eye-height 1.6})]
      (cb/apply-invoke mark/mark-teleport-on-key-up :player-id "p1" :ctx-id "ctx-2" :player-ref :player :cost-ok? true))
    (is (= 1 (count @teleport-calls*)))
    ;; Upstream dismounts the player before setPositionAndUpdate.
    (is (= ["p1"] @dismount-calls*))
    (is (= ["p1"] @reset-calls*))
    (is (= 1 (count @exp-calls*)))
    (is (= 1 (count @cooldown-calls*)))
    (is (= 2 (count @calls*)) "fanned out to owner + nearby")
    (is (= :mark-teleport/fx-perform (nth (first @calls*) 1)))
    (is (map? (get (nth (first @calls*) 3) :target)))
    (is (= true (get-in @ctx* [:skill-state :has-target])))))

(deftest mark-teleport-on-key-abort-clears-state-and-sends-end-test
  ;; Upstream l_onKeyAbort terminates; the client kills EntityTPMarking on
  ;; MSG_TERMINATED.
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 3 :has-target true}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        fx-topics* (atom [])]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [& args]
                             (swap! fx-topics* conj (:topic (second args)))
                             nil)]
      (cb/apply-invoke mark/mark-teleport-on-key-abort :ctx-id "ctx-a"))
    (is (nil? (:skill-state @ctx*)))
    (is (= [:mark-teleport/fx-end] @fx-topics*))))

(deftest mark-teleport-on-key-up-cost-fail-has-no-side-effects-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 5
                                                          :has-target true
                                                          :world-id "minecraft:overworld"
                                                          :target-x 7.0 :target-y 70.0 :target-z 9.0
                                                          :distance 9.0 :exp 0.4}})
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        teleport-calls* (atom 0)
        fx-calls* (atom 0)
        exp-calls* (atom 0)
        cooldown-calls* (atom 0)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  skill-config/tunable-double (fn [_ _] 3.0)
                  raycast/available? (constantly false)
                  fx/send! (fn [& _] (swap! fx-calls* inc) nil)
                  skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc) nil)
                  skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc) nil)
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [& _] nil)
                  motion-effects/teleport-player! (fn [& _] (swap! teleport-calls* inc) true)]
      (cb/apply-invoke mark/mark-teleport-on-key-up :player-id "p1" :ctx-id "ctx-3" :player-ref :player :cost-ok? false))
    (is (= 0 @teleport-calls*))
    (is (= 0 @fx-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))
    (is (= true (get-in @ctx* [:skill-state :has-target])))))

(deftest mark-teleport-on-key-up-min-distance-does-not-perform-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 2
                                                          :has-target true
                                                          :world-id "minecraft:overworld"
                                                          :target-x 4.0 :target-y 65.0 :target-z 4.0
                                                          :distance 2.5 :exp 0.4}})
        {:keys [get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        teleport-calls* (atom 0)
        fx-calls* (atom 0)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  skill-config/tunable-double (fn [_ field-id]
                                                (case field-id
                                                  :targeting.min-distance 3.0
                                                  0.0))
                  raycast/available? (constantly false)
                  fx/send! (fn [& _] (swap! fx-calls* inc) nil)
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [& _] nil)
                  motion-effects/teleport-player! (fn [& _] (swap! teleport-calls* inc) true)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)]
      (cb/apply-invoke mark/mark-teleport-on-key-up :player-id "p1" :ctx-id "ctx-4" :player-ref :player :cost-ok? true))
    (is (= 0 @teleport-calls*))
    (is (= 0 @fx-calls*))))

(deftest mark-teleport-on-key-up-teleport-failure-does-not-send-perform-test
  (let [mocks (skill-ctx/content-ctx-mocks {:skill-state {:hold-ticks 1
                                                          :has-target true
                                                          :world-id "minecraft:overworld"
                                                          :target-x 10.0 :target-y 64.0 :target-z 12.0
                                                          :distance 8.0 :exp 0.5}})
        {:keys [get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        mocks
        fx-calls* (atom 0)
        exp-calls* (atom 0)
        cooldown-calls* (atom 0)]
    (with-redefs [ctx/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  skill-config/tunable-double (fn [_ _] 3.0)
                  raycast/available? (constantly false)
                  fx/send! (fn [& _] (swap! fx-calls* inc) nil)
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/player-position (fn [& _] nil)
                  motion-effects/dismount-riding! (fn [& _] true)
                  motion-effects/teleport-player! (fn [& _] false)
                  motion-effects/reset-fall-damage! (fn [& _] true)
                  skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc) nil)
                  skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc) nil)]
      (cb/apply-invoke mark/mark-teleport-on-key-up :player-id "p1" :ctx-id "ctx-5" :player-ref :player :cost-ok? true))
    (is (= 0 @fx-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))))

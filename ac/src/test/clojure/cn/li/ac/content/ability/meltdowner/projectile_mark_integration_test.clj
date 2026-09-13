(ns cn.li.ac.content.ability.meltdowner.projectile-mark-integration-test
  (:require [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.model.ability :as ad]
            [cn.li.ac.ability.server.damage.runtime :as rt]
            [cn.li.ac.ability.service.delayed-projectiles :as dp]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.effects.beam :as beam]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.content.ability.meltdowner.damage-helper :as dh]
            [cn.li.ac.content.ability.meltdowner.electron-missile :as missile]
            [cn.li.ac.content.ability.meltdowner.ray-barrage :as ray-barrage]
            [cn.li.ac.content.ability.meltdowner.jet-engine :as jet-engine]
            [cn.li.ac.content.ability.meltdowner.rad-intensify :as rad]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]))

(defn- with-fresh-meltdowner-runtimes [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (rt/reset-damage-handler-registry-for-test!)
      (try
        (f)
        (finally
          (dp/reset-pending-tasks-for-test!)
          (dh/reset-marks-for-test!)
          (rt/reset-damage-handler-registry-for-test!)
          (store/reset-store!))))))

(use-fixtures :each with-fresh-meltdowner-runtimes)

(defn- learned-rad-intensify-data []
  (-> (ad/new-ability-data) (ad/learn-skill :rad-intensify)))

(defn- learn-rad-intensify! [player-id]
  (ps-fix/seed-player-state! player-id {:ability-data (learned-rad-intensify-data)}))

(defn- missile-context-mocks [initial]
  (let [ctx* (atom initial)]
    {:ctx* ctx*
     :get-context (fn
                    ([_ctx-id] @ctx*)
                    ([_owner _ctx-id] @ctx*))
     :update-skill-state-root! (fn [_ f & args]
                                 (swap! ctx* update :skill-state
                                        (fn [ss]
                                          ;; replace-skill-state! passes identity + the whole map
                                          (if (and (= f identity) (= 1 (count args)))
                                            (first args)
                                            (apply f (or ss {}) args)))))}))

(defn- stub-missile-lerp-double [_skill-id field-id _exp]
  (case field-id
    :combat.damage 12.0
    :cost.tick.cp 7.0
    :cost.attack.cp 42.0
    :cost.attack.overload 6.0
    :targeting.seek-range 10.0
    0.0))

(defn- stub-missile-lerp-int [_skill-id field-id _exp]
  (case field-id
    :cooldown.ticks 550
    :charge.max-hold-ticks 80
    0))

(defn- stub-missile-tunable-int [_skill-id field-id]
  (case field-id
    :projectile.max-hold-balls 5
    :timing.spawn-interval-ticks 10
    :timing.fire-interval-ticks 8
    0))

(defn- stub-missile-tunable-double [_skill-id field-id]
  (case field-id
    :cost.down.overload 200.0
    :progression.exp-hit 0.001
    0.0))

(defn- stub-ray-lerp-double [_skill-id field-id _exp]
  (case field-id
    :combat.damage.plain 25.0
    :combat.damage.scattered 10.0
    :cost.down.cp 300.0
    :cost.down.overload 130.0
    0.0))

(defn- stub-ray-tunable-double [_skill-id field-id]
  (case field-id
    :targeting.range 22.0
    :scatter.target-radius 8.0
    :beam.radius 0.3
    :beam.query-radius 20.0
    :beam.step 0.8
    :beam.max-distance 22.0
    :beam.visual-distance 20.0
    :progression.exp-hit 0.003
    0.0))

(defn- stub-ray-tunable-int [_skill-id field-id]
  (case field-id
    :scatter.count 1
    0))

(defn- jet-context-mocks [initial]
  (let [ctx* (atom initial)]
    {:ctx* ctx*
     :get-context (fn
                    ([_ctx-id] @ctx*)
                    ([_owner _ctx-id] @ctx*))
     :update-skill-state-root! (fn [_ f & args]
                        (swap! ctx* update :skill-state (fn [ss] (apply f (or ss {}) args))))
     :terminate-context! (fn [& _] nil)
     :send! (fn [& _] nil)}))

(deftest electron-bomb-delayed-hit-installs-rad-mark-test
  (let [attacker "atk-eb"
        victim "victim-eb"]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker)
    (with-redefs [rad/rate (fn [_] 1.75)
                  rad/mark-duration-ticks (fn [] 100000)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  ctx-mgr/push-channel-to-player! (fn [& _] nil)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] nil)
                  ;; settle time re-reads world-id/eye live, per upstream
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  geom/body-pos (fn [_] {:x 0.0 :y 62.38 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-excluding
                  (fn [_world sx sy sz & _]
                    (when-not (= [sx sy sz] [0.0 64.0 0.0])
                      {:hit-type :entity :uuid victim
                       :x 0.0 :y 64.0 :z 8.0
                       :hit-x 0.0 :hit-y 64.0 :hit-z 8.0
                       :distance 8.0}))
                  world-effects/available? (constantly true)
                  world-effects/find-entities-in-aabb
                  ;; The ball orbits the caster, so its position is its own --
                  ;; which is also what tells the two traces apart here.
                  (fn [& _] [{:uuid "ball-eb" :x 0.9 :y 63.0 :z 0.3}])
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  entity-damage/apply-aoe-damage! (fn [& _] [])
                  entity-damage/apply-reflection-damage! (fn [& _] [])]
      (dp/schedule-electron-bomb-beam!
       {:player-id attacker
        :ctx-id "ctx-eb"
        :world-id "w"
        :ball-uuid "ball-eb"
        :damage 12.0
        :delay-ticks 1})
      (dp/tick-player! attacker)
      (is (= 1.75 (:rate (get (dh/marks-snapshot) victim))))
      (is (= 17.5 (double (rt/process-damage! victim attacker 10.0 :magic))))))

(deftest electron-missile-hit-installs-rad-mark-test
  (let [attacker "atk-em"
        victim "victim-em"
        {:keys [get-context update-skill-state-root!]} (missile-context-mocks {:skill-state {:ticks 8
                                              :ball-ids ["ball-1"]
                                              :active? true
                                              :overload-floor 200.0}})]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker)
    (with-redefs [rad/rate (fn [_] 1.6)
                  rad/mark-duration-ticks (fn [] 100000)
                  skill-effects/skill-exp (fn [& _] 0.5)
                  skill-config/lerp-double stub-missile-lerp-double
                  skill-config/lerp-int stub-missile-lerp-int
                  skill-config/tunable-int stub-missile-tunable-int
                  skill-config/tunable-double stub-missile-tunable-double
                  skill-effects/enforce-overload-floor! (fn [& _] nil)
                  skill-effects/perform-resource! (fn [& _] {:success? true})
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  ctx/get-context get-context
                  ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send! (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  world-effects/available? (constantly true)
                  world-effects/find-entities-in-radius (fn [& _]
                                                         [{:uuid victim
                                                           :x 3.0 :y 64.0 :z 0.0
                                                           :eye-height 1.6
                                                           :living? true}])
                  ;; the ray originates from the ACTUAL ball entity (no
                  ;; fallback to the eye)
                  motion-effects/entity-motion-available? (constantly true)
                  motion-effects/entity-position (fn [& _] {:x 1.0 :y 64.0 :z 0.0})
                  motion-effects/discard-entity! (fn [& _] nil)
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  entity-damage/apply-aoe-damage! (fn [& _] [])
                  entity-damage/apply-reflection-damage! (fn [& _] [])]
      (cb/apply-invoke missile/electron-missile-tick! :player-id attacker :ctx-id "ctx-em" :player-ref {:id "player-obj"}))
      (is (= 1.6 (:rate (get (dh/marks-snapshot) victim))))
      (is (= 16.0 (double (rt/process-damage! victim attacker 10.0 :magic))))))

(deftest ray-barrage-hit-installs-rad-mark-test
  (let [attacker "atk-rb"
        victim "victim-rb"]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker)
    (with-redefs [rad/rate (fn [_] 1.4)
                  rad/mark-duration-ticks (fn [] 100000)
                  skill-effects/skill-exp (fn [& _] 0.0)
                  skill-config/lerp-double stub-ray-lerp-double
                  skill-config/tunable-double stub-ray-tunable-double
                  skill-config/tunable-int stub-ray-tunable-int
                  fx/send! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  geom/body-pos (fn [_] {:x 0.0 :y 62.4 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  ;; a plain (non-silbarn) entity in front takes the direct hit
                  raycast/raycast-combined (fn [& _]
                                              {:hit-type :entity
                                               :uuid victim
                                               :type "minecraft:zombie"
                                               :x 0.0 :y 64.0 :z 5.0})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  ctx-mgr/push-channel-to-player! (fn [& _] nil)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] nil)]
      (cb/apply-invoke ray-barrage/ray-barrage-perform! :player-id attacker :ctx-id "ctx-rb"))
      (is (= 1.4 (:rate (get (dh/marks-snapshot) victim))))
      (is (= 14.0 (double (rt/process-damage! victim attacker 10.0 :magic)))))))

(deftest scatter-bomb-delayed-hit-installs-rad-mark-test
  (let [attacker "atk-sc"
        victim "victim-sc"]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker)
    (with-redefs [rad/rate (fn [_] 1.33)
                  rad/mark-duration-ticks (fn [] 100000)
                  ;; each ball resolves as a single origin->dest raycast, not a
                  ;; radius beam sweep
                  raycast/available? (constantly true)
                  ;; blocks as well as entities, matching Raytrace.perform
                  raycast/raycast-combined-all (fn [& _]
                                                 {:hit-type :entity :uuid victim
                                                  :x 0.0 :y 64.0 :z 5.0 :distance 5.0})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  ctx-mgr/push-channel-to-player! (fn [& _] nil)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] nil)]
      (dp/schedule-scatter-bomb-beam!
       {:player-id attacker
        :ctx-id "ctx-sc"
        :world-id "w"
        :origin {:x 0.0 :y 64.0 :z 0.0}
        :dest {:x 0.0 :y 64.0 :z 5.0}
        :damage 7.0
        :delay-ticks 1})
      (dp/tick-player! attacker)
      (is (= 1.33 (:rate (get (dh/marks-snapshot) victim))))
      (is (= 13.3 (double (rt/process-damage! victim attacker 10.0 :magic)))))))

(deftest jet-engine-hit-installs-rad-mark-test
  (let [attacker "atk-jet"
        victim "victim-jet"
        {:keys [get-context update-skill-state-root! terminate-context! send!]}
        (jet-context-mocks {:skill-state {:phase :triggering
                                          :start-pos {:x 0.0 :y 64.0 :z 0.0}
                                          :target-pos {:x 4.0 :y 64.0 :z 0.0}
                                          :last-pos {:x 0.0 :y 64.0 :z 0.0}
                                          :velocity {:x 0.5 :y 0.0 :z 0.0}
                                          :world-id "w"
                                          :trigger-ticks 0
                                          :hit-uuids #{}}})]
    (dh/ensure-damage-handler!)
    (learn-rad-intensify! attacker)
    (with-redefs [rad/rate (fn [_] 1.5)
                  rad/mark-duration-ticks (fn [] 100000)
                  skill-effects/skill-exp (fn [& _] 0.0)
                  ctx/get-context get-context
                  ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx/terminate-context! terminate-context!
                  fx/send! send!
                  motion-effects/teleportation-available? (constantly true)
                  motion-effects/teleport-player! (fn [& _] true)
                  motion-effects/reset-fall-damage! (fn [& _] true)
                  motion-effects/player-motion-available? (constantly true)
                  motion-effects/dismount-riding! (fn [& _] true)
                  motion-effects/set-player-velocity! (fn [& _] true)
                  raycast/available? (constantly true)
                  raycast/player-position (fn [& _] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/raycast-entities (fn [& _] {:uuid victim})
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage! (fn [& _] true)
                  entity-damage/apply-aoe-damage! (fn [& _] [])
                  entity-damage/apply-reflection-damage! (fn [& _] [])
                  ctx-mgr/push-channel-to-player! (fn [& _] nil)
                  ctx-mgr/push-channel-to-nearby-players! (fn [& _] nil)]
      (cb/apply-invoke jet-engine/jet-engine-tick! :player-id attacker :ctx-id "ctx-jet" :hold-ticks 1))
      (is (= 1.5 (:rate (get (dh/marks-snapshot) victim))))
      (is (= 15.0 (double (rt/process-damage! victim attacker 10.0 :magic))))))




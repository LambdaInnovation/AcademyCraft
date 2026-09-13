(ns cn.li.ac.content.ability.vecmanip.vec-reflection-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.reflection-damage :as reflection-damage]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.ability.service.skill-effects]
            [cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.server.damage.handler :as damage-handler]
            [cn.li.ac.content.ability.vecmanip.vec-reflection :as vr]
            [cn.li.ac.content.ability.vecmanip.arbitration]
            [cn.li.ac.ability.effects.damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.ability.effects.world]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]))

(defn- with-fresh-reflection-runtime [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (store/reset-store!)
      (reflection-damage/reset-for-test!)
      (ps-fix/seed-player-state! "p" {})
      (damage-handler/reset-attack-check-registries-for-test!)
      (try
        (f)
        (finally
          (damage-handler/reset-attack-check-registries-for-test!)
          (reflection-damage/reset-for-test!)
          (vr/reset-reflection-runtime-for-test! "p"))))))

(use-fixtures :each with-fresh-reflection-runtime)

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(deftest send-fx-reflect-entity-includes-reflected-flag-test
  (let [payload* (atom nil)
        mode* (atom nil)]
    (with-redefs [fx/send! (fn [_ctx-id entry _evt payload]
                             (reset! payload* payload)
                             (reset! mode* (:mode entry)))]
      (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/send-fx-reflect-entity!
       "ctx-1"
       "p1"
       {:x 1.0 :y 2.0 :z 3.0 :height 1.8}))
    (is (= true (:reflected? @payload*)))
    (is (= :reflect-entity @mode*))))

(deftest notify-beam-reflected-puts-a-wave-in-front-of-the-defender-test
  ;; onReflect(ReflectEvent) fires on the DEFENDER's context when a reflectible
  ;; beam is cancelled by their VecReflection, and spawns a wave at
  ;;   player.pos + (0, ranged(0.4, 1.3), 0) + normalize(attacker - self) * 0.5
  ;; -- a separate wave from the one handleAttack puts on the attacker.
  (let [payload* (atom nil)
        mode* (atom nil)]
    (with-redefs [fx/send! (fn [_ctx-id entry _evt payload]
                             (reset! payload* payload)
                             (reset! mode* (:mode entry)))
                  ctx/get-all-contexts (fn [] {"k" {:id "ctx-def" :player-uuid "defender"}})
                  toggle/is-toggle-active? (fn [_ _] true)
                  motion-effects/player-position (fn [_] {:x 10.0 :y 64.0 :z 10.0})
                  raycast/available? (constantly false)]
      (vr/notify-beam-reflected! "defender" {:x 14.0 :y 64.0 :z 10.0}))
    (is (= :play @mode*))
    ;; attacker is 4 blocks +x away, so the wave sits half a block that way
    (is (= 10.5 (:x @payload*)))
    (is (= 10.0 (:z @payload*)))
    (is (<= (+ 64.0 0.4) (:y @payload*) (+ 64.0 1.3)))))

(deftest init-registers-precheck-side-effect-test
  (let [reflect-calls (atom [])]
    (with-redefs [vr/can-cancel-attack? (fn [_ _ _ _] true)
                  vr/reflect-damage (fn [player-id attacker-id damage]
                                      (swap! reflect-calls conj [player-id attacker-id damage])
                                      [true 0.0])]
      (vr/init!)
      (is (true? (damage-handler/run-attack-precheck-side-effects! "p" "a" 7.0 :src)))
      (is (= [["p" "a" 7.0]] @reflect-calls)))))

(deftest precheck-side-effect-skips-when-cannot-cancel-test
  (let [reflect-calls (atom 0)]
    (with-redefs [vr/can-cancel-attack? (fn [_ _ _ _] false)
                  vr/reflect-damage (fn [_ _ _]
                                      (swap! reflect-calls inc)
                                      [true 0.0])]
      (vr/init!)
      (is (false? (damage-handler/run-attack-precheck-side-effects! "p" "a" 7.0 :src)))
      (is (= 0 @reflect-calls)))))

(deftest reflection-target-prefers-normalized-causing-entity-test
  (with-redefs [cn.li.mcmod.platform.entity-damage/available? (constantly true)
                cn.li.mcmod.platform.entity-damage/reflection-target-entity-id
                (fn [damage-source]
                  (when (= :dragon-part-source damage-source)
                    "dragon-parent"))]
    (is (= "dragon-parent"
           (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/reflection-target-id
             "dragon-part" :dragon-part-source)))
    (is (= "shooter"
           (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/reflection-target-id
             "shooter" :melee-source)))))

(deftest multipart-entities-are-not-reflected-as-projectiles-test
  (is (nil?
       (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/affect-difficulty-with-snapshot
        {:entity-id "minecraft:ender_dragon" :multipart? true}
        #{}
        {}))))

(deftest reflected-damage-source-cannot-trigger-reflection-test
  (with-redefs [cn.li.mcmod.platform.entity-damage/available? (constantly true)
                cn.li.mcmod.platform.entity-damage/vec-reflection-damage-source?
                #(= :reflection-source %)
                cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id
                (fn [_] "ctx-1")
                cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 99.0)
                cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp
                (fn [field _]
                  (case field
                    :cost.damage.cp 1.0
                    :combat.damage-multiplier 1.0
                    0.0))
                cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double
                (fn [field]
                  (case field
                    :combat.min-reflected-damage 0.0
                    0.0))
                cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})]
    (is (false? (vr/can-cancel-attack? "p" "a" 6.0 :reflection-source)))
    (is (true? (vr/can-cancel-attack? "p" "a" 6.0 :ordinary-source)))))

(deftest can-cancel-attack-precheck-threshold-test
  (with-redefs [cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id (fn [_] "ctx-1")
                cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 3.0)
                cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _exp]
                                                                            (case field
                                                                              :cost.damage.cp 2.0
                                                                              :combat.damage-multiplier 1.0
                                                                              0.0))
                cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                              (case field
                                                                                :combat.min-reflected-damage 5.0
                                                                                0.0))
                cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:ok true})]
    ;; Original passby check is based on reflected damage, not available CP.
    ;; damage=2 -> reflected=2 < min(5) => false
    (is (false? (vr/can-cancel-attack? "p" "a" 2.0)))
      (with-redefs [cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 0.0)]
        ;; damage=6 -> reflected=6 >= min(5) => true
      (is (true? (vr/can-cancel-attack? "p" "a" 6.0))))))

(deftest visited-map-prune-ttl-and-max-size-test
  (let [now 1000
        ttl-ms 200
        max-size 2
        input {"old" 600 "keep-a" 900 "keep-b" 950 "keep-c" 990}
        out (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/prune-visited-map
             input now ttl-ms max-size)]
    ;; old expired entry removed
    (is (not (contains? out "old")))
    ;; capped to max-size and keeps newest timestamps
    (is (= 2 (count out)))
    (is (contains? out "keep-c"))
    (is (contains? out "keep-b"))))

(deftest reflect-damage-recursion-guard-pair-level-test
  (let [applied (atom [])]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:position {:world-id "w"}})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 9999.0)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [_ _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :combat.damage-multiplier 1.0
                                                                                :cost.damage.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :combat.min-reflected-damage 0.0
                                                                                  :progression.exp-damage-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/max-reflections (fn [] 6)
                  cn.li.mcmod.platform.entity-damage/available? (constantly true)
                  reflection-damage/enqueue! (fn [task]
                                               (swap! applied conj task)
                                               true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [_ _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id (fn [_] nil)]
      (binding [vr/*reflection-chain-id* "chain-guard"]
        (vr/mark-reflecting-for-test! "p" "a" nil "chain-guard")
        (is (= [false 10.0] (vr/reflect-damage "p" "a" 10.0))))
      (is (empty? @applied)))))

(deftest reflect-damage-stops-when-max-reflections-reached-test
  (let [applied (atom [])]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:position {:world-id "w"}})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 9999.0)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [_ _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :combat.damage-multiplier 1.0
                                                                                :cost.damage.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :combat.min-reflected-damage 0.0
                                                                                  :progression.exp-damage-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/max-reflections (fn [] 2)
                  cn.li.mcmod.platform.entity-damage/available? (constantly true)
                  reflection-damage/enqueue! (fn [task]
                                               (swap! applied conj task)
                                               true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [_ _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id (fn [_] nil)]
      (binding [vr/*reflection-chain-id* "chain-depth"]
        (vr/set-reflection-depth-for-test! "p" "a" nil "chain-depth" 3)
        (is (= [false 10.0] (vr/reflect-damage "p" "a" 10.0))))
      (is (empty? @applied)))))

(deftest reflect-damage-recursion-state-isolated-by-context-test
  (let [applied (atom [])]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:position {:world-id "w"}})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 9999.0)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [_ _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :combat.damage-multiplier 1.0
                                                                                :cost.damage.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :combat.min-reflected-damage 0.0
                                                                                  :progression.exp-damage-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/max-reflections (fn [] 6)
                  cn.li.mcmod.platform.entity-damage/available? (constantly true)
                  reflection-damage/enqueue! (fn [task]
                                               (swap! applied conj task)
                                               true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [_ _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id (fn [_] "ctx-current")]
      (binding [vr/*reflection-chain-id* "chain-shared"]
        (vr/mark-reflecting-for-test! "p" "a" "ctx-other" "chain-shared")
        (ctx/with-context-owner (test-context-owner "p")
          (is (= [true 0.0] (vr/reflect-damage "p" "a" 10.0)))))
      (is (= [{:world-id "w"
               :caster-id "p"
               :target-id "a"
               :damage 10.0
               :chain-id "chain-shared"}]
             (mapv #(select-keys % [:world-id :caster-id :target-id :damage :chain-id])
                   @applied))))))

(deftest reflect-damage-recursion-state-isolated-by-chain-test
  (let [applied (atom [])]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state (fn [_] {:position {:world-id "w"}})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 9999.0)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [_ _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :combat.damage-multiplier 1.0
                                                                                :cost.damage.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :combat.min-reflected-damage 0.0
                                                                                  :progression.exp-damage-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/max-reflections (fn [] 6)
                  cn.li.mcmod.platform.entity-damage/available? (constantly true)
                  reflection-damage/enqueue! (fn [task]
                                               (swap! applied conj task)
                                               true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [_ _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id (fn [_] "ctx-current")]
      (vr/mark-reflecting-for-test! "p" "a" "ctx-current" "chain-other")
      (binding [vr/*reflection-chain-id* "chain-current"]
        (ctx/with-context-owner (test-context-owner "p")
          (is (= [true 0.0] (vr/reflect-damage "p" "a" 10.0)))))
      (is (= [{:world-id "w"
               :caster-id "p"
               :target-id "a"
               :damage 10.0
               :chain-id "chain-current"}]
             (mapv #(select-keys % [:world-id :caster-id :target-id :damage :chain-id])
                   @applied))))))

(deftest reflect-damage-force-consumes-available-cp-test
  (let [consumed (atom [])
        applied (atom [])]
    (with-redefs [cn.li.ac.ability.service.skill-effects/get-player-state
                  (fn [_] {:position {:world-id "w"}})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/current-cp (fn [_] 3.0)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp!
                  (fn [player-id amount]
                    (swap! consumed conj [player-id amount])
                    true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp
                  (fn [field _]
                    (case field
                      :combat.damage-multiplier 0.5
                      :cost.damage.cp 2.0
                      0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double
                  (fn [field]
                    (case field
                      :progression.exp-damage-scale 0.0
                      0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/max-reflections (fn [] 6)
                  cn.li.mcmod.platform.entity-damage/available? (constantly true)
                  reflection-damage/enqueue!
                  (fn [task]
                    (swap! applied conj task)
                    true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/active-vec-reflection-ctx-id
                  (fn [_] nil)]
      (ctx/with-context-owner (test-context-owner "p")
        (is (= [true 5.0] (vr/reflect-damage "p" "a" 10.0))))
      (is (= [["p" 3.0]] @consumed))
      (is (= [{:world-id "w" :caster-id "p" :target-id "a" :damage 5.0}]
             (mapv #(select-keys % [:world-id :caster-id :target-id :damage])
                   @applied))))))

(deftest vec-reflection-state-is-per-player-test
  (ps-fix/seed-player-state! "p2" {})
  (vr/mark-reflecting-for-test! "p" "a" "ctx-a" "chain-a")
  (vr/set-reflection-depth-for-test! "p" "a" "ctx-a" "chain-a" 2)
  (is (= 1 (count (:reflecting-pairs (vr/reflection-runtime-snapshot "p")))))
  (is (= 2 (get-in (vr/reflection-runtime-snapshot "p")
                   [:reflection-depths (vr/reflection-owner-key "p" "a" "ctx-a" "chain-a")])))
  (is (= 0 (count (:reflecting-pairs (vr/reflection-runtime-snapshot "p2")))))
  (vr/mark-reflecting-for-test! "p2" "a" "ctx-a" "chain-a")
  (vr/set-reflection-depth-for-test! "p2" "a" "ctx-a" "chain-a" 5)
  (is (= 5 (get-in (vr/reflection-runtime-snapshot "p2")
                   [:reflection-depths (vr/reflection-owner-key "p2" "a" "ctx-a" "chain-a")])))
  (is (= 2 (get-in (vr/reflection-runtime-snapshot "p")
                   [:reflection-depths (vr/reflection-owner-key "p" "a" "ctx-a" "chain-a")]))))

(deftest tick-reflect-fireball-spawn-and-discard-test
  (let [spawn-calls (atom [])
        discard-calls (atom [])
        fx-calls (atom 0)]
    (with-redefs [ctx-skill/get-context (fn [_]
                                          {:skill-state {:toggle {:vec-reflection {:active true}}
                                                         :vec-reflection-overload-keep 0.0
                                                         :vec-reflection-visited-map {}}})
                  ctx-skill/update-skill-state-root! (fn [& _] nil)
                  cn.li.ac.ability.util.toggle/is-toggle-active? (fn [_ _] true)
                  cn.li.ac.ability.util.toggle/update-toggle-tick! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/enforce-overload-floor! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/get-player-position (fn [_]
                                                                                          {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :targeting.radius 6.0
                                                                                  :progression.exp-reflect-entity-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :cost.reflect-entity.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [& _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/large-fireball-ids (fn [] #{"minecraft:fireball" "minecraft:large_fireball"})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/small-fireball-ids (fn [] #{"minecraft:small_fireball"})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/affected-entity-difficulty (fn [] {"minecraft:fireball" 1.0})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/excluded-entity-ids (fn [] #{})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/send-fx-reflect-entity! (fn [& _] (swap! fx-calls inc))
                  cn.li.ac.content.ability.vecmanip.arbitration/dual-active? (fn [_] false)
                  cn.li.ac.content.ability.vecmanip.arbitration/claim-projectile! (fn [& _] true)
                  cn.li.mcmod.platform.world-effects/available? (constantly true)
                  cn.li.mcmod.platform.world-effects/find-entities-in-radius (fn [& _]
                                                                               [{:uuid "e1"
                                                                                 :entity-id "minecraft:fireball"
                                                                                 :owner-uuid "shooter"
                                                                                 :explosion-power 3
                                                                                 :x 1.0 :y 65.0 :z 1.0}])
                  cn.li.mcmod.platform.world-effects/spawn-projectile! (fn [world-id spec]
                                                                         (swap! spawn-calls conj [world-id spec])
                                                                         {:success? true :uuid "spawned" :entity-id (:entity-id spec)})
                  cn.li.mcmod.platform.raycast/available? (constantly true)
                  cn.li.mcmod.platform.raycast/player-look-vector (fn [& _] {:x 1.0 :y 0.0 :z 0.0})
                  cn.li.mcmod.platform.raycast/player-position (fn [& _]
                                                                  {:world-id "w" :x 0.0 :y 64.0 :eye-y 65.62 :z 0.0})
                  cn.li.mcmod.platform.raycast/raycast-blocks (fn [& _]
                                                                {:hit-x 20.0 :hit-y 65.0 :hit-z 1.0 :distance 20.0})
                  cn.li.mcmod.platform.raycast/raycast-from-player (fn [& _] nil)
                  cn.li.ac.ability.effects.motion/entity-motion-available? (constantly true)
                  cn.li.ac.ability.effects.motion/entity-velocity (fn [& _] {:x 1.0 :y 0.0 :z 0.0})
                  cn.li.ac.ability.effects.motion/discard-entity! (fn [world-id entity-id]
                                                                       (swap! discard-calls conj [world-id entity-id]))]
      (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/vec-reflection-on-key-tick-body
         "p1" "ctx-1" 0.5 true)
      (is (= 1 (count @spawn-calls)))
      (let [[world-id spec] (first @spawn-calls)]
        (is (= "w" world-id))
        (is (= "minecraft:fireball" (:entity-id spec)))
        (is (= "shooter" (:owner-uuid spec)))
        (is (= 3 (:explosion-power spec)))
        (is (= 1.0 (:vx spec)))
        (is (= 0.0 (:vy spec)))
        (is (= 0.0 (:vz spec))))
      (is (= [["w" "e1"]] @discard-calls))
      (is (= 1 @fx-calls)))))

(deftest tick-reflect-fireball-spawn-fallback-to-set-velocity-test
  (let [spawn-calls (atom [])
        set-velocity-calls (atom [])
        discard-calls (atom [])]
    (with-redefs [ctx-skill/get-context (fn [_]
                                          {:skill-state {:toggle {:vec-reflection {:active true}}
                                                         :vec-reflection-overload-keep 0.0
                                                         :vec-reflection-visited-map {}}})
                  ctx-skill/update-skill-state-root! (fn [& _] nil)
                  cn.li.ac.ability.util.toggle/is-toggle-active? (fn [_ _] true)
                  cn.li.ac.ability.util.toggle/update-toggle-tick! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/enforce-overload-floor! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/skill-exp (fn [_] 0.5)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/get-player-position (fn [_]
                                                                                          {:world-id "w" :x 0.0 :y 64.0 :z 0.0})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-double (fn [field]
                                                                                (case field
                                                                                  :targeting.radius 6.0
                                                                                  :progression.exp-reflect-entity-scale 0.0
                                                                                  0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/cfg-lerp (fn [field _]
                                                                              (case field
                                                                                :cost.reflect-entity.cp 0.0
                                                                                0.0))
                  cn.li.ac.content.ability.vecmanip.vec-reflection/consume-cp! (fn [& _] true)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/large-fireball-ids (fn [] #{"minecraft:fireball" "minecraft:large_fireball"})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/small-fireball-ids (fn [] #{"minecraft:small_fireball"})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/affected-entity-difficulty (fn [] {"minecraft:fireball" 1.0})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/excluded-entity-ids (fn [] #{})
                  cn.li.ac.content.ability.vecmanip.vec-reflection/add-exp! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.vec-reflection/send-fx-reflect-entity! (fn [& _] nil)
                  cn.li.ac.content.ability.vecmanip.arbitration/dual-active? (fn [_] false)
                  cn.li.ac.content.ability.vecmanip.arbitration/claim-projectile! (fn [& _] true)
                  cn.li.mcmod.platform.world-effects/available? (constantly true)
                  cn.li.mcmod.platform.world-effects/find-entities-in-radius (fn [& _]
                                                                               [{:uuid "e1"
                                                                                 :entity-id "minecraft:fireball"
                                                                                 :x 1.0 :y 65.0 :z 1.0}])
                  cn.li.mcmod.platform.world-effects/spawn-projectile! (fn [world-id spec]
                                                                         (swap! spawn-calls conj [world-id spec])
                                                                         {:success? false})
                  cn.li.mcmod.platform.raycast/available? (constantly true)
                  cn.li.mcmod.platform.raycast/player-look-vector (fn [& _] {:x 0.0 :y 1.0 :z 0.0})
                  cn.li.mcmod.platform.raycast/player-position (fn [& _]
                                                                  {:world-id "w" :x 0.0 :y 64.0 :eye-y 65.62 :z 0.0})
                  cn.li.mcmod.platform.raycast/raycast-blocks (fn [& _]
                                                                {:hit-x 1.0 :hit-y 85.0 :hit-z 1.0 :distance 20.0})
                  cn.li.mcmod.platform.raycast/raycast-from-player (fn [& _] nil)
                  cn.li.ac.ability.effects.motion/entity-motion-available? (constantly true)
                  cn.li.ac.ability.effects.motion/entity-velocity (fn [& _] {:x 0.0 :y 0.0 :z 2.0})
                  cn.li.ac.ability.effects.motion/set-entity-velocity! (fn [world-id entity-id vx vy vz]
                                                                     (swap! set-velocity-calls conj [world-id entity-id vx vy vz]))
                  cn.li.ac.ability.effects.motion/discard-entity! (fn [world-id entity-id]
                                                                       (swap! discard-calls conj [world-id entity-id]))]
      (@#'cn.li.ac.content.ability.vecmanip.vec-reflection/vec-reflection-on-key-tick-body
         "p1" "ctx-1" 0.5 true)
      (is (= 1 (count @spawn-calls)))
      (is (= [["w" "e1" 0.0 2.0 0.0]] @set-velocity-calls))
      (is (empty? @discard-calls)))))

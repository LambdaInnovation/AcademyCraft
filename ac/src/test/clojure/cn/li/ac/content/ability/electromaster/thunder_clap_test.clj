(ns cn.li.ac.content.ability.electromaster.thunder-clap-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.world :as world-op]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.ac.content.ability.electromaster.thunder-clap :as thunder-clap]))

(def ^:private spec thunder-clap/thunder-clap)

(defn- reset-state! [f]
  (let [context-registry-val (ctx/snapshot-context-registry)]
    (try
      (ctx/reset-contexts-for-test!)
      (f)
      (finally
        (ctx/reset-contexts-for-test! context-registry-val)))))

(use-fixtures :each reset-state!)

(defn- seed-charge-context!
  "thunder-clap-up! ignores the (never-populated-in-production) hold-ticks
  positional argument and instead self-tracks charge duration in
  :skill-state — so tests must seed a real registered context rather than
  passing :hold-ticks through cb/apply-invoke."
  [owner player-id ctx-id skill-state]
  (ctx/with-context-owner owner
    (ctx/register-context!
     (assoc (ctx/new-server-context player-id :thunder-clap ctx-id owner)
            :status ctx/STATUS-ALIVE))
    (ctx-skill/update-skill-state-root! ctx-id identity skill-state)))

(deftest thunder-clap-short-release-remains-unperformed-test
  (testing "release before the minimum charge keeps performed false and surfaces it through the end payload"
    (let [up-fn (get-in spec [:actions :up!])
          end-payload-fn (get-in spec [:fx :end :payload])
          settle-perform? (get-in spec [:input-policy :settle-perform-on-key-up?])
          owner {:logical-side :server :server-session-id :test-session :player-uuid "p1"}
          cooldown-calls* (atom [])
          exp-calls* (atom [])
          run-ops* (atom [])]
      (with-redefs [thunder-clap/min-ticks (fn [] 40)
                    thunder-clap/max-ticks (fn [] 60)
                    skill-effects/set-main-cooldown! (fn [& args]
                                                       (swap! cooldown-calls* conj args))
                    skill-effects/add-skill-exp! (fn [& args]
                                                   (swap! exp-calls* conj args))
                    world-op/execute-spawn-lightning! (fn [_evt _params]
                                                        (swap! run-ops* conj [:spawn-lightning]))
                    thunder-clap/execute-thunder-clap-aoe! (fn [_evt radius amount]
                                                             (swap! run-ops* conj [:damage-aoe radius amount]))]
        (seed-charge-context! owner "p1" "ctx-short" {:hold-ticks 20
                                                       :performed? false
                                                       :hit-pos {:x 1.0 :y 2.0 :z 3.0}})
        (ctx/with-context-owner owner
          (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-short" :exp 0.5)
          (is (false? (get-in (ctx/get-context "ctx-short") [:skill-state :performed?])))
          (is (= {:x 1.0 :y 2.0 :z 3.0}
                 (get-in (ctx/get-context "ctx-short") [:skill-state :final-target])))
          (is (false? (settle-perform? {:ctx-id "ctx-short"})))
          (is (= {:performed? false
                  :charge-ticks 20
                  :ticks 20
                  :charge-ratio 0.0
                  :target {:x 1.0 :y 2.0 :z 3.0}
                  :caster-pos {:x 0.0 :y 65.62 :z 0.0}}
                 (end-payload-fn {:ctx-id "ctx-short" :player-id "p1" :hold-ticks 20}))))
        (is (empty? @cooldown-calls*))
        (is (empty? @exp-calls*))
        (is (empty? @run-ops*))))))

(deftest thunder-clap-successful-release-marks-performed-and-applies-effects-test
  (testing "release after the charge threshold records a performed strike and executes AOE ops"
    (let [up-fn (get-in spec [:actions :up!])
          end-payload-fn (get-in spec [:fx :end :payload])
          settle-perform? (get-in spec [:input-policy :settle-perform-on-key-up?])
          owner {:logical-side :server :server-session-id :test-session :player-uuid "p1"}
          cooldown-calls* (atom [])
          exp-calls* (atom [])
          run-ops* (atom [])]
      (with-redefs [thunder-clap/min-ticks (fn [] 40)
                    thunder-clap/max-ticks (fn [] 60)
                    thunder-clap/cfg-lerp (fn [field exp]
                                            (case field
                                              ;; combat.overcharge-multiplier is looked up by
                                              ;; charge-ratio, not skill exp — see
                                              ;; compute-overcharge-ratio in thunder_clap.clj.
                                              :combat.overcharge-multiplier (+ 1.0 (* 0.2 exp))
                                              :combat.damage (+ 36.0 (* 36.0 exp))
                                              :combat.aoe-radius (+ 15.0 (* 15.0 exp))
                                              :cooldown.ticks-per-hold (+ 10.0 (* -4.0 exp))
                                              0.0))
                    thunder-clap/cfg-double (fn [field]
                                              (case field
                                                :progression.exp-use 0.003
                                                0.0))
                    geom/world-id-of (fn [_] "world-1")
                    skill-effects/set-main-cooldown! (fn [& args]
                                                       (swap! cooldown-calls* conj args))
                    skill-effects/add-skill-exp! (fn [& args]
                                                   (swap! exp-calls* conj args))
                    world-op/execute-spawn-lightning! (fn [evt params]
                                                        (swap! run-ops* conj [:spawn-lightning evt params]))
                    thunder-clap/execute-thunder-clap-aoe! (fn [evt radius amount]
                                                             (swap! run-ops* conj [:damage-aoe evt radius amount]))]
        (seed-charge-context! owner "p1" "ctx-hit" {:hold-ticks 50
                                                     :performed? false
                                                     :hit-pos {:x 8.0 :y 64.0 :z 8.0}})
        (ctx/with-context-owner owner
          (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-hit" :exp 0.5)
          (is (true? (get-in (ctx/get-context "ctx-hit") [:skill-state :performed?])))
          (is (= {:x 8.0 :y 64.0 :z 8.0}
                 (get-in (ctx/get-context "ctx-hit") [:skill-state :final-target])))
          (is (true? (settle-perform? {:ctx-id "ctx-hit"})))
          ;; ticks=50, min=40, max=60 => charge-ratio=(50-40)/60=1/6 =>
          ;; overcharge-multiplier=1.0+0.2*(1/6)=31/30 => dmg=54*31/30=55.8
          (is (= [[:spawn-lightning
             {:player-id "p1"
              :ctx-id "ctx-hit"
              :world-id "world-1"
              :hit-pos {:x 8.0 :y 64.0 :z 8.0}
              :exp 0.5}
             {:at :hit-pos :visual-only? true}]
            [:damage-aoe
             {:player-id "p1"
              :ctx-id "ctx-hit"
              :world-id "world-1"
              :hit-pos {:x 8.0 :y 64.0 :z 8.0}
              :exp 0.5}
             22.5
             55.800000000000004]]
                 @run-ops*))
          (is (= [["p1" :thunder-clap 400]] @cooldown-calls*))
          (is (= [["p1" :thunder-clap 0.003 1.0]] @exp-calls*))
          (is (= {:performed? true
                  :charge-ticks 50
                  :ticks 50
                  :charge-ratio (/ 1.0 6.0)
                  :target {:x 8.0 :y 64.0 :z 8.0}
                  :caster-pos {:x 0.0 :y 65.62 :z 0.0}}
                 (end-payload-fn {:ctx-id "ctx-hit" :player-id "p1" :hold-ticks 50}))))))))

(deftest thunder-clap-targeting-ignores-entities-like-original-test
  (let [raycast-calls* (atom [])]
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-blocks (fn [& args]
                                           (swap! raycast-calls* conj args)
                                           {:hit-type :block
                                            :hit-x 3.0 :hit-y 66.0 :hit-z 9.0})
                  raycast/raycast-combined (fn [& _]
                                             (throw (AssertionError. "combined/entity raycast must not be used")))
                  geom/eye-pos (fn [_] {:x 1.0 :y 65.0 :z 2.0})
                  geom/world-id-of (fn [_] "world-1")
                  thunder-clap/targeting-range (constantly 40.0)]
      (is (= {:x 3.0 :y 66.0 :z 9.0}
             (#'thunder-clap/resolve-raycast-target "p1")))
      (is (= [["world-1" 1.0 65.0 2.0 0.0 0.0 1.0 40.0]]
             @raycast-calls*)))))

(deftest thunder-clap-strikes-the-block-hit-not-the-sky-test
  ;; The bolt was landing ~range blocks along the look vector — up in the air
  ;; — instead of at the aimed block. Raycast.raycastBlocks did not put
  ;; a "hit-type" (only the combined variants did), so attack/hit-kind
  ;; classified every real block hit as :miss and resolve-raycast-target took
  ;; the eye+look*range fallback every time.
  ;;
  ;; The fixture below is the exact key set Raycast.raycastBlocks puts:
  ;; hit-type, x/y/z (block pos), hit-x/hit-y/hit-z (precise), block-id, face,
  ;; distance. Tests that invent a friendlier shape are what let this ship.
  (let [block-hit {:hit-type :block
                   :x 3 :y 65 :z 9
                   :hit-x 3.5 :hit-y 65.0 :hit-z 9.25
                   :block-id "minecraft:stone"
                   :face "up"
                   :distance 12.0}
        ;; Looking steeply upward, so the fallback lands far overhead and is
        ;; unmistakable if it is taken by mistake.
        look {:x 0.0 :y 1.0 :z 0.0}
        eye {:x 1.0 :y 65.0 :z 2.0}]
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] look)
                  geom/eye-pos (fn [_] eye)
                  geom/world-id-of (fn [_] "world-1")
                  thunder-clap/targeting-range (constantly 40.0)]
      (with-redefs [raycast/raycast-blocks (fn [& _] block-hit)]
        (is (= {:x 3.5 :y 65.0 :z 9.25}
               (#'thunder-clap/resolve-raycast-target "p1"))
            "a block hit strikes its precise impact point"))
      (with-redefs [raycast/raycast-blocks (fn [& _] nil)]
        (is (= {:x 1.0 :y 105.0 :z 2.0}
               (#'thunder-clap/resolve-raycast-target "p1"))
            "only a genuine miss falls back to eye + look * range")))))

(deftest thunder-clap-aoe-applies-original-scaling-and-attribution-test
  (let [calc-calls* (atom [])
        damage-calls* (atom [])
        evt {:player-id "caster"
             :world-id "world-1"
             :hit-pos {:x 0.0 :y 0.0 :z 0.0}}]
    (with-redefs [world-effects/available? (constantly true)
                  world-effects/find-entities-in-radius
                  (fn [& _]
                    [{:uuid "caster" :x 0.0 :y 0.0 :z 0.0}
                     {:uuid "center" :x 0.0 :y 0.0 :z 0.0}
                     {:uuid "half" :x 5.0 :y 0.0 :z 0.0}])
                  entity-damage/available? (constantly true)
                  entity-damage/apply-direct-damage!
                  (fn [& args]
                    (swap! damage-calls* conj args)
                    true)
                  ability-event/fire-calc-event!
                  (fn [event-type value extra]
                    (swap! calc-calls* conj [event-type value extra])
                    (+ value 1.0))
                  skill-effects/scale-damage (fn [_ value] (* 2.0 value))]
      (is (= 2 (#'thunder-clap/execute-thunder-clap-aoe! evt 10.0 100.0))))
    (is (= [[ability-event/CALC-SKILL-ATTACK
             100.0
             {:player-id "caster" :target-id "center" :skill-id :thunder-clap}]
            [ability-event/CALC-SKILL-ATTACK
             50.0
             {:player-id "caster" :target-id "half" :skill-id :thunder-clap}]]
           @calc-calls*))
    (is (= [["world-1" "center" 202.0 :skill
             {:attacker-uuid "caster" :skill-id :thunder-clap}]
            ["world-1" "half" 102.0 :skill
             {:attacker-uuid "caster" :skill-id :thunder-clap}]]
           @damage-calls*))))

(deftest thunder-clap-auto-performs-at-maximum-charge-test
  (let [tick-fn (get-in spec [:actions :tick!])
        owner {:logical-side :server :server-session-id :test-session :player-uuid "p1"}
        perform-calls* (atom [])
        fx-calls* (atom [])]
    (seed-charge-context! owner "p1" "ctx-max" {:hold-ticks 59
                                                 :performed? false
                                                 :hit-pos {:x 4.0 :y 64.0 :z 4.0}})
    (with-redefs [thunder-clap/max-ticks (constantly 60)
                  thunder-clap/refresh-hit-pos! (fn [& _] nil)
                  thunder-clap/perform-thunder-clap!
                  (fn [& args] (swap! perform-calls* conj args))
                  thunder-clap/emit-thunder-clap-fx!
                  (fn [stage evt] (swap! fx-calls* conj [stage evt]))]
      (ctx/with-context-owner owner
        (cb/apply-invoke tick-fn :player-id "p1" :ctx-id "ctx-max" :exp 0.75))
      (is (= 60 (get-in (ctx/get-context owner "ctx-max") [:skill-state :hold-ticks])))
      (is (= [["ctx-max" "p1" 0.75 60]] @perform-calls*))
      (is (= :terminated (:status (ctx/get-context owner "ctx-max"))))
      (is (= [[:update {:ctx-id "ctx-max" :player-id "p1" :hold-ticks 60}]]
             @fx-calls*)))))

(deftest thunder-clap-minimum-tick-cost-failure-still-performs-test
  (let [cost-fail-fn (get-in spec [:actions :cost-fail!])
        owner {:logical-side :server :server-session-id :test-session :player-uuid "p1"}
        perform-calls* (atom [])]
    (seed-charge-context! owner "p1" "ctx-min-fail" {:hold-ticks 40
                                                      :performed? false
                                                      :hit-pos {:x 4.0 :y 64.0 :z 4.0}})
    (with-redefs [thunder-clap/min-ticks (constantly 40)
                  thunder-clap/perform-thunder-clap!
                  (fn [& args] (swap! perform-calls* conj args))]
      (ctx/with-context-owner owner
        (cb/apply-invoke cost-fail-fn
                         :player-id "p1"
                         :ctx-id "ctx-min-fail"
                         :exp 0.25
                         :cost-ok? false
                         :cost-stage :tick))
      (is (= [["ctx-min-fail" "p1" 0.25 40]] @perform-calls*))
      (is (= :terminated (:status (ctx/get-context owner "ctx-min-fail")))))))

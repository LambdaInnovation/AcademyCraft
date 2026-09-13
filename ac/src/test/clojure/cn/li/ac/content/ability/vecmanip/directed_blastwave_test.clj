(ns cn.li.ac.content.ability.vecmanip.directed-blastwave-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.content.ability.vecmanip.directed-blastwave :as db]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(def ^:private spec db/directed-blastwave)

(defn- mock-cfg-int [field]
  (case field
    :charge.min-ticks 6
    :charge.max-accepted-ticks 50
    :charge.max-tolerant-ticks 200
    :charge.punch-anim-ticks 6
    0))

(defn- mock-cfg-double [field]
  (case field
    :targeting.raycast-distance 4.0
    :targeting.eye-height 1.62
    :combat.aoe-radius 3.0
    :movement.knockback-y-adjust 0.4
    :movement.knockback-scale -1.2
    :progression.exp-hit 0.0025
    :progression.exp-miss 0.0012
    0.0))

(defn- mock-cfg-lerp [field _exp]
  (case field
    :cost.up.cp 160.0
    :cost.up.overload 50.0
    :combat.damage 20.0
    :cooldown.ticks 80
    :breaking.break-probability 1.0
    :breaking.drop-probability 1.0
    0.0))

(defn- mock-cfg-lerp-int [field _exp]
  (case field
    :cooldown.ticks 80
    0))

(defn- make-context-mocks [initial-ctx]
  (let [ctx-state (atom initial-ctx)
        terminate-calls (atom [])]
    {:ctx-state ctx-state
     :terminate-calls terminate-calls
     :get-context (fn
                   ([_ctx-id] @ctx-state)
                   ([_owner _ctx-id] @ctx-state))
     :update-skill-state-root! (fn [_ctx-id f & args]
                                 (swap! ctx-state update :skill-state
                                        (fn [ss]
                                          (let [current (or ss {})]
                                            (if (and (= f identity) (= 1 (count args)))
                                              (first args)
                                              (apply f current args))))))
     :clear-skill-state! (fn [_]
                           (swap! ctx-state dissoc :skill-state)
                           nil)
     :terminate-context! (fn [ctx-id _]
                           (swap! terminate-calls conj ctx-id))}))

(defn- with-mock-config [f]
  (with-redefs [skill-config/tunable-int (fn [_ field-id] (mock-cfg-int field-id))
                skill-config/tunable-double (fn [_ field-id] (mock-cfg-double field-id))
                skill-config/lerp-double (fn [_ field-id exp] (mock-cfg-lerp field-id exp))
                skill-config/lerp-int (fn [_ field-id exp] (mock-cfg-lerp-int field-id exp))]
    (f)))

(deftest charge-window-invalid-release-terminates-test
  (let [up-fn (get-in spec [:actions :up!])
        {:keys [ctx-state get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:charge-ticks 6 :performed? false}})
        end-calls* (atom [])]
    (skill-ctx/with-server-skill-context
      #(with-mock-config
         (fn []
           (with-redefs [ctx/get-context get-context
                         ctx-skill/update-skill-state-root! update-skill-state-root!
                         ctx-skill/clear-skill-state! clear-skill-state!
                         ctx/terminate-context! terminate-context!
                         fx/send! (fn [ctx-id entry _evt payload]
                                    (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))]
             (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-invalid" :exp 0.5 :cost-ok? true)))))

    (is (= [["ctx-invalid" :directed-blastwave/fx-end :end {:performed? false}]] @end-calls*))
    (is (= ["ctx-invalid"] @terminate-calls))
    (is (nil? (:skill-state @ctx-state)))))

(deftest nil-trace-uses-body-position-and-awards-miss-exp-test
  (let [up-fn (get-in spec [:actions :up!])
        {:keys [ctx-state get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:charge-ticks 10 :performed? false :punched? false :punch-ticks 0}})
        perform-calls* (atom [])
        punch-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])]
    (skill-ctx/with-server-skill-context
      #(with-mock-config
         (fn []
           (with-redefs [ctx/get-context get-context
                         ctx-skill/update-skill-state-root! update-skill-state-root!
                         ctx-skill/clear-skill-state! clear-skill-state!
                         ctx/terminate-context! terminate-context!
                         geom/world-id-of (fn [_] "w")
                         geom/body-pos (fn [_] {:x 2.0 :y 3.0 :z 4.0})
                         geom/eye-pos (fn [_] {:x 2.0 :y 4.62 :z 4.0})
                         raycast/available? (constantly true)
                         raycast/player-position
                         (fn [_] {:x 2.0 :y 3.0 :z 4.0 :eye-y 4.62 :world-id "w"})
                         raycast/raycast-blocks (fn [& _] nil)
                         raycast/raycast-from-player (fn [& _] nil)
                         raycast/player-look-vector (fn [& _] {:x 0.0 :y 0.0 :z 1.0})
                         world-effects/available? (constantly true)
                         world-effects/find-entities-in-radius (fn [& _] [])
                         fx/send! (fn [& args]
                                    (let [[ctx-id entry _evt payload] args]
                                      (when (= :directed-blastwave/fx-perform (:topic entry))
                                        (swap! perform-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))
                                      (when (= :directed-blastwave/fx-punch (:topic entry))
                                        (swap! punch-calls* conj [ctx-id (:topic entry) (:mode entry)]))))
                         skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks]))
                         skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                        (swap! exp-calls* conj [player-id skill-id amount]))]
             (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-miss" :exp 0.5 :cost-ok? true)))))
    (is (= 2 (count @perform-calls*)) "fanned out to owner + nearby")
    (is (= [["ctx-miss" :directed-blastwave/fx-punch :punch]] @punch-calls*)
        "punch anim is owner-only (upstream l_effect isLocal gate)")
    (is (= {:x 2.0 :y 3.0 :z 8.0}
           (get-in @perform-calls* [0 3 :pos])))
    (is (= [["p1" :directed-blastwave 80]] @cooldown-calls*))
    (is (= [["p1" :directed-blastwave 0.0012]] @exp-calls*))
    (is (true? (get-in @ctx-state [:skill-state :performed?])))
    (is (true? (get-in @ctx-state [:skill-state :punched?])))
    (is (empty? @terminate-calls))))

(deftest hit-path-applies-damage-velocity-and-cooldown-test
  (let [up-fn (get-in spec [:actions :up!])
        {:keys [ctx-state get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:charge-ticks 10 :performed? false :punched? false :punch-ticks 0}})
        damage-calls* (atom [])
        set-position-calls* (atom [])
        set-velocity-calls* (atom [])
        perform-calls* (atom [])
        cooldown-calls* (atom [])
        exp-calls* (atom [])]
    (skill-ctx/with-server-skill-context
      #(with-mock-config
         (fn []
           (with-redefs [ctx/get-context get-context
                         ctx-skill/update-skill-state-root! update-skill-state-root!
                         ctx-skill/clear-skill-state! clear-skill-state!
                         ctx/terminate-context! terminate-context!
                         geom/world-id-of (fn [_] "w")
                         geom/body-pos (fn [_] {:x 0.0 :y 0.0 :z 0.0})
                         geom/eye-pos (fn [_] {:x 0.0 :y 1.62 :z 0.0})
                         raycast/available? (constantly true)
                         raycast/player-position
                         (fn [_] {:x 0.0 :y 0.0 :z 0.0 :eye-y 1.62 :world-id "w"})
                         raycast/raycast-blocks (fn [& _] nil)
                         raycast/raycast-from-player (fn [& _]
                                                        {:x 1.0 :y 2.0 :z 3.0
                                                         :eye-height 1.8 :distance 2.0})
                         raycast/player-look-vector (fn [& _] {:x 0.0 :y 0.0 :z 1.0})
                         world-effects/available? (constantly true)
                         world-effects/find-entities-in-radius (fn [& _]
                                                                  [{:uuid "e1" :x 1.0 :y 2.0 :z 3.0 :eye-height 1.8}])
                         entity-damage/available? (constantly true)
                         entity-damage/apply-direct-damage!
                         (fn [world-id target-id damage kind opts]
                           (swap! damage-calls* conj [world-id target-id damage kind opts]))
                         motion-effects/entity-motion-available? (constantly true)
                         motion-effects/set-entity-position!
                         (fn [world-id target-id x y z]
                           (swap! set-position-calls* conj [world-id target-id x y z]))
                         motion-effects/set-entity-velocity! (fn [world-id target-id x y z]
                                                        (swap! set-velocity-calls* conj [world-id target-id x y z]))
                         fx/send! (fn [& args]
                                    (let [[ctx-id entry _evt payload] args]
                                      (when (= :directed-blastwave/fx-perform (:topic entry))
                                        (swap! perform-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))))
                         skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks]))
                         skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                        (swap! exp-calls* conj [player-id skill-id amount]))]
             (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-hit" :exp 0.5 :cost-ok? true)))))
    (is (= [["w" "e1" 20.0 :skill {:attacker-uuid "p1"}]] @damage-calls*))
    (is (= [["w" "e1" 1.0 2.1 3.0]] @set-position-calls*))
    (is (= 1 (count @set-velocity-calls*)))
    (is (= 2 (count @perform-calls*)) "fanned out to owner + nearby")
    (is (empty? @terminate-calls))
    (is (= [["p1" :directed-blastwave 80]] @cooldown-calls*))
    (is (= [["p1" :directed-blastwave 0.0025]] @exp-calls*))
    (is (true? (get-in @ctx-state [:skill-state :performed?])))
    (is (true? (get-in @ctx-state [:skill-state :punched?])))))

(deftest punch-tick-terminates-successful-context-test
  (let [tick-fn (get-in spec [:actions :tick!])
        {:keys [ctx-state get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:charge-ticks 12 :performed? true :punched? true :punch-ticks 6}})
        end-calls* (atom [])]
    (skill-ctx/with-server-skill-context
      #(with-mock-config
         (fn []
           (with-redefs [ctx/get-context get-context
                         ctx-skill/update-skill-state-root! update-skill-state-root!
                         ctx-skill/clear-skill-state! clear-skill-state!
                         ctx/terminate-context! terminate-context!
                         fx/send! (fn [ctx-id entry _evt payload]
                                    (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))]
             (cb/apply-invoke tick-fn :ctx-id "ctx-punch")))))

    (is (= [["ctx-punch" :directed-blastwave/fx-end :end {:performed? true}]]
           (filter #(= :directed-blastwave/fx-end (nth % 1)) @end-calls*)))
    (is (= ["ctx-punch"] @terminate-calls))
    (is (nil? (:skill-state @ctx-state)))))

(deftest abort-terminates-context-test
  (let [abort-fn (get-in spec [:actions :abort!])
        {:keys [ctx-state get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls]}
        (make-context-mocks {:skill-state {:charge-ticks 3 :performed? false}})
        end-calls* (atom [])]
    (skill-ctx/with-server-skill-context
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    ctx/terminate-context! terminate-context!
                    fx/send! (fn [ctx-id entry _evt payload]
                               (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))]
         (cb/apply-invoke abort-fn :ctx-id "ctx-abort")))

    (is (= [["ctx-abort" :directed-blastwave/fx-end :end {:performed? false}]] @end-calls*))
    (is (= ["ctx-abort"] @terminate-calls))
    (is (nil? (:skill-state @ctx-state)))))

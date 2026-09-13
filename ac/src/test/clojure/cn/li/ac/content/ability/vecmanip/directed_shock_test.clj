(ns cn.li.ac.content.ability.vecmanip.directed-shock-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.content.ability.vecmanip.directed-shock :as ds]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.raycast :as raycast]))

(def ^:private spec ds/directed-shock)

(defn- mock-cfg-int [field]
  (case field
    :charge.min-ticks 6
    :charge.max-accepted-ticks 50
    :charge.max-tolerant-ticks 200
    :charge.punch-anim-ticks 6
    0))

(defn- mock-cfg-double [field]
  (case field
    :targeting.raycast-distance 3.0
    :targeting.eye-height 1.62
    :movement.hit-impulse 0.24
    :movement.knockback-y-adjust 0.6
    :movement.knockback-scale -0.7
    :movement.knockback-exp-threshold 0.25
    :progression.exp-hit 0.0035
    :progression.exp-miss 0.001
    0.0))

(defn- mock-cfg-lerp [field _exp]
  (case field
    :combat.damage 12.0
    :cost.up.cp 75.0
    :cost.up.overload 14.0
    ;; cooldown now reads the double curve, not lerp-int
    :cooldown.ticks 40.0
    0.0))

(defn- mock-cfg-lerp-int [field _exp]
  (case field
    :cooldown.ticks 40
    0))

(defn- make-context-mocks [initial-ctx]
  (let [ctx* (atom initial-ctx)
        terminate-calls* (atom [])]
    {:ctx* ctx*
     :terminate-calls* terminate-calls*
     :get-context (fn
                   ([_ctx-id] @ctx*)
                   ([_owner _ctx-id] @ctx*))
     :update-skill-state-root! (fn [_ctx-id f & args]
                                 (swap! ctx* update :skill-state
                                        (fn [ss]
                                          (let [current (or ss {})]
                                            (if (and (= f identity) (= 1 (count args)))
                                              (first args)
                                              (apply f current args))))))
     :clear-skill-state! (fn [_]
                           (swap! ctx* dissoc :skill-state)
                           nil)
     :terminate-context! (fn [ctx-id _]
                           (swap! terminate-calls* conj ctx-id))}))

(defn- with-mock-config [f]
  (with-redefs [skill-config/tunable-int (fn [_ field-id] (mock-cfg-int field-id))
                skill-config/tunable-double (fn [_ field-id] (mock-cfg-double field-id))
                skill-config/lerp-double (fn [_ field-id exp] (mock-cfg-lerp field-id exp))
                skill-config/lerp-int (fn [_ field-id exp] (mock-cfg-lerp-int field-id exp))]
    (f)))

(deftest charge-window-open-interval-test
  (let [up-fn (get-in spec [:actions :up!])]
    (doseq [ticks [6 50]]
      (let [{:keys [get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls*]}
            (make-context-mocks {:skill-state {:charge-ticks ticks :performed? true}})
            end-calls* (atom [])
            trace-calls* (atom 0)]
        (skill-ctx/with-server-skill-context
          #(with-mock-config
             (fn []
               (with-redefs [ctx/get-context get-context
                             ctx-skill/update-skill-state-root! update-skill-state-root!
                             ctx-skill/clear-skill-state! clear-skill-state!
                             ctx/terminate-context! terminate-context!
                             raycast/available? (constantly true)
                             raycast/raycast-from-player (fn [& _]
                                                            (swap! trace-calls* inc)
                                                            nil)
                             fx/send! (fn [ctx-id entry _evt payload]
                                        (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))]
                 (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-1" :exp 0.5 :cost-ok? true)))))
        (is (= [["ctx-1" :directed-shock/fx-end :end {:performed? false}]] @end-calls*))
        (is (= ["ctx-1"] @terminate-calls*))
        (is (= 0 @trace-calls*))))))

(deftest hit-path-applies-effects-and-sets-cooldown-test
  (let [up-fn (get-in spec [:actions :up!])
        {:keys [ctx* get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls*]}
        (make-context-mocks {:skill-state {:charge-ticks 10 :performed? false :punched? false :punch-ticks 0}})
        damage-calls* (atom [])
        add-velocity-calls* (atom [])
        set-velocity-calls* (atom [])
        set-position-calls* (atom [])
        perform-calls* (atom [])
        punch-calls* (atom [])
        end-calls* (atom [])
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
                         geom/eye-pos (fn [_] {:x 0.0 :y 1.62 :z 0.0})
                         raycast/player-position (fn [_]
                                                   {:x 0.0 :y 0.0 :z 0.0
                                                    :eye-y 1.62 :world-id "w"})
                         raycast/available? (constantly true)
                         raycast/raycast-from-player (fn [& _]
                                                        {:entity-id "e1"
                                                         :x 1.0 :y 2.0 :z 3.0
                                                         :eye-height 1.8})
                         entity-damage/available? (constantly true)
                         entity-damage/apply-direct-damage! (fn [world-id target-id damage kind & _opts]
                                                                (swap! damage-calls* conj [world-id target-id damage kind]))
                         motion-effects/entity-motion-available? (constantly true)
                         motion-effects/add-entity-velocity! (fn [world-id target-id x y z]
                                                        (swap! add-velocity-calls* conj [world-id target-id x y z]))
                         motion-effects/set-entity-velocity! (fn [world-id target-id x y z]
                                                        (swap! set-velocity-calls* conj [world-id target-id x y z]))
                         motion-effects/set-entity-position! (fn [world-id target-id x y z]
                                                                (swap! set-position-calls* conj
                                                                       [world-id target-id x y z]))
                         fx/send! (fn [& args]
                                    (let [[ctx-id entry _evt payload] args]
                                      (case (:topic entry)
                                        :directed-shock/fx-perform
                                        (swap! perform-calls* conj [ctx-id (:topic entry) (:mode entry) payload])
                                        :directed-shock/fx-punch
                                        (swap! punch-calls* conj [ctx-id (:topic entry) (:mode entry)])
                                        :directed-shock/fx-end
                                        (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload])
                                        nil)))
                         skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks]))
                         skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                        (swap! exp-calls* conj [player-id skill-id amount]))]
             (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-hit" :exp 0.3 :cost-ok? true)))))
    (is (= [["w" "e1" 12.0 :skill]] @damage-calls*))
    (is (= 1 (count @set-velocity-calls*)))
    (is (= [["w" "e1" 1.0 2.1 3.0]] @set-position-calls*))
    (is (= (get-in @set-velocity-calls* [0 3])
           (get-in @set-velocity-calls* [0 4]))
        "original DirectedShock assigns motionZ from the adjusted Y delta")
    (is (= 1 (count @add-velocity-calls*)))
    (is (= 2 (count @perform-calls*)) "fx-perform fanned out to owner + nearby")
    (is (= {:x 0.0 :y 0.0 :z 0.0}
           (select-keys (get-in @perform-calls* [0 3]) [:x :y :z]))
        "punch sound position matches the caster entity position")
    (is (= [["ctx-hit" :directed-shock/fx-punch :punch]] @punch-calls*)
        "punch anim is owner-only (upstream l_effect isLocal gate)")
    (is (empty? @end-calls*))
    (is (empty? @terminate-calls*))
    (is (= [["p1" :directed-shock 40]] @cooldown-calls*))
    (is (= [["p1" :directed-shock 0.0035]] @exp-calls*))
    (is (= true (get-in @ctx* [:skill-state :performed?])))
    (is (= true (get-in @ctx* [:skill-state :punched?])))))

(deftest miss-path-adds-miss-exp-without-cooldown-test
  (let [up-fn (get-in spec [:actions :up!])
        {:keys [ctx* get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls*]}
        (make-context-mocks {:skill-state {:charge-ticks 10 :performed? true}})
        end-calls* (atom [])
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
                         geom/eye-pos (fn [_] {:x 0.0 :y 1.62 :z 0.0})
                         raycast/player-position (fn [_]
                                                   {:x 0.0 :y 0.0 :z 0.0
                                                    :eye-y 1.62 :world-id "w"})
                         raycast/available? (constantly true)
                         raycast/raycast-from-player (fn [& _] nil)
                         fx/send! (fn [ctx-id entry _evt payload]
                                    (swap! end-calls* conj [ctx-id (:topic entry) (:mode entry) payload]))
                         skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                            (swap! cooldown-calls* conj [player-id skill-id ticks]))
                         skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                        (swap! exp-calls* conj [player-id skill-id amount]))]
             (cb/apply-invoke up-fn :player-id "p1" :ctx-id "ctx-miss" :exp 0.3 :cost-ok? true)))))

    (is (= [["ctx-miss" :directed-shock/fx-end :end {:performed? false}]] @end-calls*))
    (is (= ["ctx-miss"] @terminate-calls*))
    (is (empty? @cooldown-calls*))
    (is (= [["p1" :directed-shock 0.001]] @exp-calls*))
    (is (nil? (:skill-state @ctx*)))))

(deftest punch-tick-terminates-successful-context-test
  (let [tick-fn (get-in spec [:actions :tick!])
        {:keys [ctx* get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls*]}
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

    (is (= [["ctx-punch" :directed-shock/fx-end :end {:performed? true}]]
           (filter #(= :directed-shock/fx-end (nth % 1)) @end-calls*)))
    (is (= ["ctx-punch"] @terminate-calls*))
    (is (nil? (:skill-state @ctx*)))))

(deftest abort-cleans-up-and-terminates-test
  (let [abort-fn (get-in spec [:actions :abort!])
        {:keys [ctx* get-context update-skill-state-root! clear-skill-state! terminate-context! terminate-calls*]}
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

    (is (= [["ctx-abort" :directed-shock/fx-end :end {:performed? false}]] @end-calls*))
    (is (= ["ctx-abort"] @terminate-calls*))
    (is (nil? (:skill-state @ctx*)))))

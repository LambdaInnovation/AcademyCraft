(ns cn.li.ac.content.ability.vecmanip.vec-accel-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.content.ability.vecmanip.vec-accel :as va]))

(def ^:private compute-init-vel
  @#'cn.li.ac.content.ability.vecmanip.vec-accel/compute-init-vel)

(def ^:private check-ground-raycast
  @#'cn.li.ac.content.ability.vecmanip.vec-accel/check-ground-raycast)

(def ^:private spec va/vec-accel)

(defmacro ^:private with-config-mocks [& body]
  `(with-redefs [cn.li.ac.content.ability.vecmanip.vec-accel/cfg-double
                 (fn [field#]
                   (case field#
                     :movement.pitch-offset-radians -0.174533
                     :movement.max-velocity 2.5
                     :targeting.groundless-exp-threshold 0.5
                     :targeting.ground-check-distance 2.0
                     :progression.exp-use 0.002
                     0.0))
                 cn.li.ac.content.ability.vecmanip.vec-accel/cfg-int
                 (fn [field#]
                   (case field#
                     :charge.max-ticks 20
                     0))
                 cn.li.ac.content.ability.vecmanip.vec-accel/cfg-lerp
                 (fn [field# _exp#]
                   (case field#
                     :cost.up.cp 120.0
                     :cost.up.overload 30.0
                     0.0))
                 cn.li.ac.content.ability.vecmanip.vec-accel/cfg-lerp-int
                 (fn [field# _exp#]
                   (case field#
                     :cooldown.ticks 80
                     0))
                 skill-config/lerp-double
                 (fn [_skill-id# field# t#]
                   (case field#
                     :movement.speed-progress (+ 0.4 (* 0.6 (double t#)))
                     0.0))]
     ~@body))

(deftest compute-init-vel-horizontal-test
  (testing "horizontal look is tilted upward by ten degrees"
    (with-config-mocks
      (let [vel (compute-init-vel {:x 0.0 :y 0.0 :z 1.0} 10)]
        (is (< (Math/abs (double (:x vel))) 1.0e-6))
        (is (pos? (double (:y vel))))
        (is (pos? (double (:z vel))))))))

(deftest compute-init-vel-keeps-original-pitch-overrotation-test
  (testing "looking almost straight up rotates past -90 degrees like EntityLook"
    (with-config-mocks
      (let [vel (compute-init-vel {:x 0.0 :y 1.0 :z 1.0e-9} 10)]
        (is (pos? (double (:y vel))))
        (is (neg? (double (:z vel))))))))

(deftest compute-init-vel-speed-scales-with-charge-test
  (with-config-mocks
    (let [speed (fn [ticks]
                  (let [{:keys [x y z]}
                        (compute-init-vel {:x 0.0 :y 0.0 :z 1.0} ticks)]
                    (Math/sqrt (+ (* x x) (* y y) (* z z)))))]
      (is (> (speed 20) (speed 0))))))

(deftest check-ground-raycast-platform-guards-test
  (with-config-mocks
    (with-redefs [raycast/available? (constantly false)]
      (is (nil? (check-ground-raycast "player-1"))))
    (with-redefs [raycast/available? (constantly true)
                  raycast/player-position (constantly nil)]
      (is (nil? (check-ground-raycast "player-1"))))))

(deftest down-starts-originally-performable-context-test
  (let [state* (atom nil)
        fx-calls* (atom [])
        down-fn (get-in spec [:actions :down!])]
    (with-redefs [ctx-skill/replace-skill-state! (fn [_ state] (reset! state* state))
                  fx/send! (fn [& args] (swap! fx-calls* conj (vec args)))]
      (cb/apply-invoke down-fn :ctx-id "ctx-1"))
    (is (= {:charge-ticks 0
            :can-perform? true
            :look-dir nil
            :init-vel nil
            :performed? false}
           @state*))
    (is (= :vec-accel/fx-start (get-in @fx-calls* [0 1 :topic])))))

(deftest tick-uses-own-ticker-and-strict-groundless-threshold-test
  (let [state* (atom {:charge-ticks 0 :can-perform? true})
        updates* (atom [])
        tick-fn (get-in spec [:actions :tick!])]
    (with-config-mocks
      (with-redefs [ctx-skill/get-context (fn [_] {:skill-state @state*})
                    ctx-skill/update-skill-state-root!
                    (fn [_ f & args]
                      (swap! state* #(apply f % args)))
                    raycast/available? (constantly true)
                    raycast/player-position
                    (constantly {:x 0.0 :y 10.0 :z 0.0 :world-id "w"})
                    raycast/raycast-blocks (fn [& _] nil)
                    raycast/player-look-vector
                    (constantly {:x 0.0 :y 0.0 :z 1.0})
                    fx/send! (fn [& args] (swap! updates* conj (vec args)))]
        (cb/apply-invoke tick-fn
                         :player-id "p1" :ctx-id "ctx-1"
                         :exp 0.5 :hold-ticks 99)))
    (is (= 1 (:charge-ticks @state*))
        "network hold-ticks are ignored; the context owns the original ticker")
    (is (false? (:can-perform? @state*))
        "original ignores ground only above, not at, 0.5 exp")
    (is (= :vec-accel/fx-update (get-in @updates* [0 1 :topic])))))

(deftest up-launches-and-emits-original-sound-path-test
  (let [vel-calls* (atom [])
        state* (atom {:can-perform? true
                      :init-vel {:x 1.0 :y 0.5 :z 0.0}})
        fx-calls* (atom [])
        up-fn (get-in spec [:actions :up!])]
    (with-config-mocks
      (with-redefs [ctx-skill/get-context (fn [_] {:skill-state @state*})
                    ctx-skill/update-skill-state-root!
                    (fn [_ f & args] (swap! state* #(apply f % args)))
                    motion-effects/player-motion-available? (constantly true)
                    motion-effects/set-player-velocity!
                    (fn [player-id x y z]
                      (swap! vel-calls* conj [player-id x y z]))
                    motion-effects/teleportation-available? (constantly true)
                    motion-effects/reset-fall-damage! (fn [_] true)
                    skill-effects/set-main-cooldown! (fn [& _] nil)
                    skill-effects/add-skill-exp! (fn [& _] nil)
                    fx/send! (fn [& args] (swap! fx-calls* conj (vec args)))]
        (cb/apply-invoke up-fn
                         :player-id "p1" :ctx-id "ctx-1"
                         :exp 0.5 :cost-ok? true)))
    (is (= [["p1" 1.0 0.5 0.0]] @vel-calls*))
    (is (true? (:performed? @state*)))
    (is (= [:vec-accel/fx-perform :vec-accel/fx-end]
           (mapv #(get-in % [1 :topic]) @fx-calls*)))))

(deftest up-does-not-launch-when-cost-fails-test
  (let [vel-calls* (atom [])
        assoc-calls* (atom [])
        fx-calls* (atom [])
        up-fn (get-in spec [:actions :up!])]
    (with-config-mocks
      (with-redefs [ctx-skill/get-context
                    (fn [_]
                      {:skill-state {:can-perform? true
                                     :init-vel {:x 1.0 :y 0.5 :z 0.0}}})
                    ctx-skill/assoc-skill-state!
                    (fn [& args] (swap! assoc-calls* conj args))
                    motion-effects/player-motion-available? (constantly true)
                    motion-effects/set-player-velocity!
                    (fn [& args] (swap! vel-calls* conj args))
                    fx/send! (fn [& args] (swap! fx-calls* conj (vec args)))]
        (cb/apply-invoke up-fn
                         :player-id "p1" :ctx-id "ctx-1"
                         :exp 0.5 :cost-ok? false)))
    (is (empty? @vel-calls*))
    (is (= [["ctx-1" :performed? false]] @assoc-calls*))
    (is (= :vec-accel/fx-end (get-in @fx-calls* [0 1 :topic])))))

(deftest skill-uses-release-cast-lifecycle-test
  (is (= :release-cast (:pattern spec)))
  (is (fn? (get-in spec [:actions :down!])))
  (is (fn? (get-in spec [:actions :tick!])))
  (is (fn? (get-in spec [:actions :up!]))))

(ns cn.li.ac.content.ability.meltdowner.mine-rays-base-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- context-mocks
  [initial]
  (let [ctx* (atom initial)]
    {:ctx* ctx*
     :get-context (fn [_] @ctx*)
     :update-skill-state-root! (fn [_ f & args]
                        (swap! ctx* (fn [ctx-data]
                                      (let [current (or (:skill-state ctx-data) {})
                                            next-state (if (and (= f identity) (= 1 (count args)))
                                                         (first args)
                                                         (apply f current args))]
                                        (assoc ctx-data :skill-state next-state)))))}))

(def ^:private base-cfg
  {:range 10.0 :break-speed 0.2 :skill-id :mine-ray-basic :exp-block 0.0005
   :tool-tier-capped? true :cooldown-ticks 40})

(deftest tick-new-block-acquisition-only-captures-no-progress-test
  ;; Matches original's "pos changed" branch: only x/y/z + hardness are
  ;; captured; no countdown progress and no particle FX on the acquisition
  ;; tick itself — decrementing only starts on a SUBSEQUENT tick that still
  ;; aims at the same block.
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x nil :target-y nil :target-z nil
                                      :hardness-left (double Float/MAX_VALUE)
                                      :starting-hardness (double Float/MAX_VALUE)}})
        fx-calls* (atom 0)]
    (with-redefs [ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] (swap! fx-calls* inc) nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-blocks (fn [& _] {:x 1 :y 64 :z 2})
                  bm/available? (constantly true)
                  bm/can-break-block? (constantly true)
                  bm/get-block-hardness (constantly 2.0)
                  bm/requires-high-tier-tool? (constantly false)]
      (base/mining-ray-tick! base-cfg "ctx-1" "p1" :mine-ray-basic 0.0 true 0 nil nil))
    (is (= {:target-x 1 :target-y 64 :target-z 2
            :hardness-left 2.0 :starting-hardness 2.0}
           (:skill-state @ctx*)))
    (is (zero? @fx-calls*))))

(deftest tick-blocked-by-tool-tier-rejects-new-block-test
  ;; Matches original's harvestLevel gate: a disallowed block is rejected
  ;; (never tracked), same as a canceled BlockDestroyEvent.
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x nil :target-y nil :target-z nil
                                      :hardness-left (double Float/MAX_VALUE)
                                      :starting-hardness (double Float/MAX_VALUE)}})]
    (with-redefs [ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-blocks (fn [& _] {:x 1 :y 64 :z 2})
                  bm/available? (constantly true)
                  bm/can-break-block? (constantly true)
                  bm/requires-high-tier-tool? (constantly true)]
      (base/mining-ray-tick! base-cfg "ctx-1" "p1" :mine-ray-basic 0.0 true 0 nil nil))
    (is (= {:target-x nil :target-y nil :target-z nil
            :hardness-left (double Float/MAX_VALUE)
            :starting-hardness (double Float/MAX_VALUE)}
           (:skill-state @ctx*)))))

(deftest tick-uncapped-variant-ignores-tool-tier-test
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x nil :target-y nil :target-z nil
                                      :hardness-left (double Float/MAX_VALUE)
                                      :starting-hardness (double Float/MAX_VALUE)}})
        tier-check-calls* (atom 0)]
    (with-redefs [ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-blocks (fn [& _] {:x 1 :y 64 :z 2})
                  bm/available? (constantly true)
                  bm/can-break-block? (constantly true)
                  bm/get-block-hardness (constantly 3.0)
                  bm/requires-high-tier-tool? (fn [& _] (swap! tier-check-calls* inc) true)]
      (base/mining-ray-tick! (assoc base-cfg :tool-tier-capped? false) "ctx-1" "p1" :mine-ray-basic 0.0 true 0 nil nil))
    (is (= {:target-x 1 :target-y 64 :target-z 2
            :hardness-left 3.0 :starting-hardness 3.0}
           (:skill-state @ctx*)))
    (is (zero? @tier-check-calls*))))

(deftest tick-enforces-overload-floor-every-tick-test
  (let [{:keys [get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x nil :target-y nil :target-z nil
                                      :hardness-left (double Float/MAX_VALUE)
                                      :starting-hardness (double Float/MAX_VALUE)
                                      :overload-floor 50.0}})
        floor-calls* (atom [])]
    (with-redefs [ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  skill-effects/enforce-overload-floor! (fn [player-id floor]
                                                          (swap! floor-calls* conj [player-id floor])
                                                          true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly false)
                  bm/available? (constantly false)]
      (base/mining-ray-tick! base-cfg "ctx-1" "p1" :mine-ray-basic 0.0 true 0 nil nil))
    (is (= [["p1" 50.0]] @floor-calls*))))

(deftest tick-carries-overload-floor-forward-while-progressing-test
  (let [{:keys [ctx* get-context update-skill-state-root!]}
        (context-mocks {:skill-state {:target-x 1 :target-y 64 :target-z 2
                                      :hardness-left 0.9 :starting-hardness 1.0
                                      :overload-floor 50.0}})]
    (with-redefs [ctx-skill/get-context get-context
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  fx/send-local-and-nearby! (fn [& _] nil)
                  skill-effects/enforce-overload-floor! (fn [& _] true)
                  geom/world-id-of (fn [_] "w")
                  geom/eye-pos (fn [_] {:x 0.0 :y 64.0 :z 0.0})
                  raycast/available? (constantly true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-blocks (fn [& _] {:x 1 :y 64 :z 2})
                  bm/available? (constantly true)
                  bm/get-block-hardness (fn [& _] 1.0)]
      (base/mining-ray-tick! base-cfg "ctx-1" "p1" :mine-ray-basic 0.0 true 0 nil nil))
    (is (= 50.0 (get-in @ctx* [:skill-state :overload-floor])))))

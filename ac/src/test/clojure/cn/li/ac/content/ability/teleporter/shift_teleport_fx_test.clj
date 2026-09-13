(ns cn.li.ac.content.ability.teleporter.shift-teleport-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.content.ability.teleporter.shift-teleport-fx :as stfx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- with-fresh-shift-teleport-fx-runtime [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (stfx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (stfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-shift-teleport-fx-runtime)

(deftest init-registers-shift-teleport-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (stfx/init!)
      (is (= :shift-teleport (first @registered-level*)))
      (is (= #{:shift-teleport/fx-start
               :shift-teleport/fx-update
               :shift-teleport/fx-perform
               :shift-teleport/fx-end}
             @registered-topics*)))))

;; The teleport sound is played server-side (world-effects/play-sound! with
;; tp.tp_shift) since the AcademyCraft alignment; the client fx only draws.
(deftest enqueue-perform-stores-tp-particle-trail-test
  ;; Upstream c_end walks the player->destination ray spawning white
  ;; TPParticleFactory particles (first step 1.0, then 0.6-1.0); the state
  ;; lingers until they fade and build-plan emits tp_particle quads — the
  ;; vanilla :portal alias would render purple.
  (stfx/init!)
  (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-perform
                                       {:mode :perform :from-x 0.0 :from-y 64.0 :from-z 0.0 :x 5.0 :y 64.0 :z 0.0}
                                       :owner-key [:ctx "ctx-1"])
  (let [st (get (:fx-state (stfx/fx-snapshot)) [:ctx "ctx-1"])]
    (is (some? st))
    ;; Markers die on execute (upstream c_end kills blockMarker + targets).
    (is (false? (:active? st)))
    (let [particles (:particles st)]
      ;; 5-block ray: ~5-9 particles (first 1.0 step, then random 0.6-1.0).
      (is (>= (count particles) 2))
      (is (every? #(= "academy:textures/effects/tp_particle.png" (:texture %)) particles))
      (is (every? #(= 20 (:life %)) particles))
      (is (every? #(= 20 (:fade-out %)) particles))
      (let [{:keys [ops]} (cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan
                           :shift-teleport {:x 0.0 :y 64.0 :z 0.0} {:player-uuid "viewer"} 0 nil)]
        ;; Trail quads only — no marker edges after perform.
        (is (pos? (count ops)))
        (is (every? #(= :quad (:kind %)) ops))
        (is (every? #(= "academy:textures/effects/tp_particle.png" (:texture %)) ops))))))

(deftest build-plan-emits-block-and-target-markers-test
  ;; Upstream l_start spawns the grey block marker; l_tick spawns one red
  ;; marker per entity in the line (refreshed every 3 ticks). Both boxes are
  ;; feet-anchored: the block marker spans dest[1]..dest[1]+1.2, an entity
  ;; marker spans its feet..feet+height.
  (stfx/init!)
  (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-update
                                         {:mode :update :x 10.0 :y 65.0 :z 12.0
                                          :target-count 2 :target-hit? true :hand-valid? true
                                          :entities [{:x 8.0 :y 64.0 :z 9.0}
                                                     {:x 9.0 :y 64.0 :z 10.0}]}
                                         :owner-key [:ctx "ctx-1"])
    (let [{:keys [ops]} (cn.li.ac.ability.client.fx-templates.arc-beam/effect-build-plan
                         :shift-teleport nil {:player-uuid "viewer"} 0 nil)]
      ;; 12 block-marker edges + 2 * 12 entity-marker edges.
      (is (= 36 (count ops)))
      (let [block-ops (filter #(= {:r 139 :g 139 :b 139 :a 180} (:color %)) ops)
            entity-ops (filter #(= {:r 235 :g 81 :b 81 :a 180} (:color %)) ops)
            min-y (fn [edged-ops]
                    (reduce min (map #(min (.y (:p1 %)) (.y (:p2 %))) edged-ops)))
            max-y (fn [edged-ops]
                    (reduce max (map #(max (.y (:p1 %)) (.y (:p2 %))) edged-ops)))]
        (is (= 12 (count block-ops)))
        (is (= 24 (count entity-ops)))
        ;; Feet-anchored at dest[1]=65.0: bottom exactly on the block, not
        ;; 0.6 below it (a centered draw would bottom out at 64.4).
        (is (< 64.99 (min-y block-ops) 65.01))
        (is (< 66.19 (max-y block-ops) 66.21))
        ;; Entity markers: default 0.6 x 1.8 box from the feet up.
        (is (< 63.99 (min-y entity-ops) 64.01))
        (is (< 65.79 (max-y entity-ops) 65.81)))))

(deftest perform-kills-markers-but-keeps-trail-until-fade-test
  (stfx/init!)
    (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-start
                                         {:mode :start :x 1.0 :y 2.0 :z 3.0 :hand-valid? true}
                                         :owner-key [:ctx "ctx-1"])
    (is (some? (get (:fx-state (stfx/fx-snapshot)) [:ctx "ctx-1"])))
    (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-perform
                                         {:mode :perform :from-x 0.0 :from-y 64.0 :from-z 0.0 :x 5.0 :y 64.0 :z 0.0}
                                         :owner-key [:ctx "ctx-1"])
    ;; Upstream c_end kills the block marker and every target marker — the
    ;; state survives only for the TPParticleFactory trail.
    (let [st (get (:fx-state (stfx/fx-snapshot)) [:ctx "ctx-1"])]
      (is (some? st))
      (is (false? (:active? st)))
      (is (seq (:particles st))))
    ;; The trail fades over 20 + 20 ticks, then the state is dropped.
    (let [store (loop [store (:fx-state (stfx/fx-snapshot)) ticks 0]
                  (if (>= ticks 41)
                    store
                    (recur (cn.li.ac.ability.client.fx-templates.arc-beam/effect-tick-state!
                            :shift-teleport :level {:fx-state store})
                           (inc ticks))))]
      (is (empty? store))))

(deftest enqueue-end-clears-state-test
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)]
    (stfx/init!)
    (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-start {:mode :start}
                                         :owner-key [:ctx "ctx-1"])
    (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-update {:mode :update :x 1.0 :y 2.0 :z 3.0 :target-count 1 :target-hit? false :hand-valid? true}
                                         :owner-key [:ctx "ctx-1"])
    (is (some? (get (:fx-state (stfx/fx-snapshot)) [:ctx "ctx-1"])))
    (level-effects/enqueue-level-effect! :shift-teleport "ctx-1" :shift-teleport/fx-end {:mode :end}
                                         :owner-key [:ctx "ctx-1"])
    (is (nil? (get (:fx-state (stfx/fx-snapshot)) [:ctx "ctx-1"])))))



(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:fx-state {}}
         (stfx/fx-snapshot))))

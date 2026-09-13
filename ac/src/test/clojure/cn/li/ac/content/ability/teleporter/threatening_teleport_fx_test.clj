(ns cn.li.ac.content.ability.teleporter.threatening-teleport-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.config.modid :as modid]
            [cn.li.ac.content.ability.teleporter.threatening-teleport-fx :as tfx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- with-fresh-threatening-teleport-fx-runtime [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (tfx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (tfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-threatening-teleport-fx-runtime)

(defn- enqueue! [ctx-id channel payload]
  (level-effects/enqueue-level-effect! :threatening-teleport ctx-id channel payload
                                       :owner-key [:ctx ctx-id]))

(defn- build-plan []
  (arc-beam/effect-build-plan :threatening-teleport nil {:player-uuid "viewer"} 0 nil))

(defn- with-fx-owner [f]
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "tt-test"})]
    (f)))

(deftest init-registers-threatening-teleport-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (tfx/init!)
      (is (= :threatening-teleport (first @registered-level*)))
      (is (= #{:threatening-teleport/fx-start
               :threatening-teleport/fx-update
               :threatening-teleport/fx-perform
               :threatening-teleport/fx-end}
             @registered-topics*)))))

(deftest two-owners-keep-threatening-teleport-state-independent-test
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "threatening-teleport-test"})]
    (tfx/init!)
    (enqueue! "ctx-a" :threatening-teleport/fx-start {:mode :start :target-x 1.0 :target-y 2.0 :target-z 3.0 :hit? true})
    (enqueue! "ctx-b" :threatening-teleport/fx-start {:mode :start :target-x 4.0 :target-y 5.0 :target-z 6.0 :hit? false})
    (enqueue! "ctx-a" :threatening-teleport/fx-update {:mode :update :target-x 1.0 :target-y 2.0 :target-z 3.0 :hit? true})
    (enqueue! "ctx-b" :threatening-teleport/fx-update {:mode :update :target-x 4.0 :target-y 5.0 :target-z 6.0 :hit? false})
    (let [snapshot (tfx/fx-snapshot)]
      (is (true? (:hit? (get (:fx-state snapshot) [:ctx "ctx-a"]))))
      (is (= {:x 4.0 :y 5.0 :z 6.0}
             (:aim (get (:fx-state snapshot) [:ctx "ctx-b"])))))
    (enqueue! "ctx-a" :threatening-teleport/fx-end {:mode :end})
    (let [snapshot (tfx/fx-snapshot)]
      (is (nil? (get (:fx-state snapshot) [:ctx "ctx-a"])))
      (is (some? (get (:fx-state snapshot) [:ctx "ctx-b"]))))
    (tfx/clear-fx-owner! [:ctx "ctx-b"])
    (is (empty? (:fx-state (tfx/fx-snapshot))))))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:fx-state {}}
         (tfx/fx-snapshot))))

(deftest start-holds-aim-from-trace-payload-test
  (with-fx-owner
    (fn []
      (tfx/init!)
      ;; :start carries the first trace so the marker appears the moment the key
      ;; goes down (upstream l_start spawns the marker on MSG_MADEALIVE).
      (enqueue! "ctx-s" :threatening-teleport/fx-start
                {:mode :start :target-x 7.0 :target-y 8.0 :target-z 9.0 :hit? false})
      (let [st (get (:fx-state (tfx/fx-snapshot)) [:ctx "ctx-s"])]
        (is (true? (:active? st)))
        (is (= {:x 7.0 :y 8.0 :z 9.0} (:aim st)))))))

(deftest marker-sits-at-target-feet-when-tracking-test
  ;; Upstream l_tick subtracts the target height: the marker hovers at the
  ;; target's FEET while the drop point is the top of the box.
  (with-fx-owner
    (fn []
      (tfx/init!)
      (enqueue! "ctx-ft" :threatening-teleport/fx-start
                {:mode :start :target-x 4.0 :target-y 8.0 :target-z 6.0 :hit? true :target-height 1.95})
      (let [st (get (:fx-state (tfx/fx-snapshot)) [:ctx "ctx-ft"])]
        (is (= 6.05 (:y (:aim st))))))))

(deftest build-plan-emits-marker-cube-at-aim-test
  ;; No state -> no ops.
  (is (nil? (build-plan)))
  (with-fx-owner
    (fn []
      (tfx/init!)
      (enqueue! "ctx-p" :threatening-teleport/fx-update
                {:mode :update :target-x 10.0 :target-y 11.0 :target-z 12.0
                 :hit? true :target-width 0.5 :target-height 0.5})))
  (let [{:keys [ops]} (build-plan)]
    ;; Upstream RenderMarker: 8 corners x 3 short line segments.
    (is (= 24 (count ops)))
    (is (every? #(= :line (:kind %)) ops))
    ;; Payload target-y 11.0 with target-height 0.5 -> marker bottom at the
    ;; FEET y=10.5 (tick 0 -> upstream float 0.05*sin(0)=0); box 0.5x0.5,
    ;; ticks extend 0.2*width beyond each corner (rotated along the edges,
    ;; same extent).
    (let [endpoints (mapcat (fn [op]
                              (let [^cn.li.mcmod.math.V3 p1 (:p1 op)
                                    ^cn.li.mcmod.math.V3 p2 (:p2 op)]
                                [[(.x p1) (.y p1) (.z p1)]
                                 [(.x p2) (.y p2) (.z p2)]]))
                            ops)]
      (is (every? (fn [[x y z]]
                    (and (<= 9.65 x 10.35)
                         (<= 10.4 y 11.1)
                         (<= 11.65 z 12.35)))
                  endpoints)))
    ;; Threatening color when targeting an entity (upstream COLOR_THREATENING).
    (is (= {:r 0xba :g 0xb2 :b 0x23 :a 0x2a} (:color (first ops))))))

(deftest build-plan-uses-normal-color-without-target-test
  (with-fx-owner
    (fn []
      (tfx/init!)
      ;; Real server payload: a block hit carries target-height 0.0 — the box
      ;; must stay 0.5x0.5 (upstream l_start pins the marker size), not
      ;; collapse flat.
      (enqueue! "ctx-g" :threatening-teleport/fx-update
                {:mode :update :target-x 1.0 :target-y 2.0 :target-z 3.0
                 :hit? false :target-width 0.5 :target-height 0.0})))
  (let [{:keys [ops]} (build-plan)]
    (is (= 24 (count ops)))
    (is (= {:r 0xba :g 0xba :b 0xba :a 0xba} (:color (first ops))))
    ;; Box bottom at aim y=2.0, full 0.5 height -> endpoints span [1.9..2.6].
    (let [endpoints (mapcat (fn [op]
                              (let [^cn.li.mcmod.math.V3 p1 (:p1 op)
                                    ^cn.li.mcmod.math.V3 p2 (:p2 op)]
                                [[(.x p1) (.y p1) (.z p1)]
                                 [(.x p2) (.y p2) (.z p2)]]))
                            ops)]
      (is (every? (fn [[_ y _]] (<= 1.9 y 2.6)) endpoints))
      (is (some (fn [[_ y _]] (>= y 2.4)) endpoints)
          "top corners must exist above the bottom plane"))))

(deftest build-plan-follows-target-entity-live-test
  ;; Upstream EntityMarker.target follow: the marker snaps to the target's
  ;; live position every frame and is sized to its bounding box.
  (with-fx-owner
    (fn []
      (tfx/init!)
      (enqueue! "ctx-f" :threatening-teleport/fx-update
                {:mode :update :target-x 1.0 :target-y 2.0 :target-z 3.0
                 :hit? true :target-uuid "enemy-1"
                 :target-width 0.6 :target-height 1.8})))
  (let [calls* (atom [])]
    (with-redefs [client-bridge/run-client-effect!
                  (fn [effect-key payload]
                    (swap! calls* conj [effect-key payload])
                    (when (= effect-key :mcmod/get-entity-position)
                      ;; Entity moved to a new spot since the last sync.
                      {:x 20.0 :y 64.0 :z 30.0 :width 1.0 :height 2.0}))]
      (let [{:keys [ops]} (build-plan)]
        (is (= 24 (count ops)))
        (is (= [[:mcmod/get-entity-position {:entity-uuid "enemy-1"}]] @calls*))
        ;; Box (1.0 x 2.0) bottom at the entity's live feet; ticks extend
        ;; 0.2*width beyond the corners.
        (let [endpoints (mapcat (fn [op]
                                  (let [^cn.li.mcmod.math.V3 p1 (:p1 op)
                                        ^cn.li.mcmod.math.V3 p2 (:p2 op)]
                                    [[(.x p1) (.y p1) (.z p1)]
                                     [(.x p2) (.y p2) (.z p2)]]))
                                ops)]
          (is (every? (fn [[x y z]]
                        (and (<= 19.3 x 20.7)
                             (<= 63.8 y 66.2)
                             (<= 29.3 z 30.7)))
                      endpoints)))))))

(deftest perform-clears-marker-plays-sound-and-spawns-green-trail-test
  (let [sounds* (atom [])]
    (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "tt-test"})
                  client-sounds/queue-sound-effect! (fn [owner fx]
                                                      (swap! sounds* conj fx)
                                                      nil)]
      (tfx/init!)
      (enqueue! "ctx-f" :threatening-teleport/fx-start
                {:mode :start :target-x 4.0 :target-y 5.0 :target-z 6.0 :hit? true})
      (is (some? (get (:fx-state (tfx/fx-snapshot)) [:ctx "ctx-f"])))
      (enqueue! "ctx-f" :threatening-teleport/fx-perform
                {:mode :perform
                 :start-x 1.0 :start-y 2.0 :start-z 3.0
                 :target-x 4.0 :target-y 5.0 :target-z 6.0
                 :hit? true})
      ;; Marker dies on execute (upstream c_end marker.setDead).
      (is (nil? (get (:fx-state (tfx/fx-snapshot)) [:ctx "ctx-f"])))
      ;; Hit: tp sound + one green TPParticle trail burst (upstream c_end).
      (is (= 1 (count @sounds*)))
      (is (= (modid/namespaced-path "tp.tp") (:sound-id (first @sounds*))))
      (let [trails (:trails (tfx/fx-snapshot))]
        (is (= 1 (count trails)))
        (is (seq (first trails)))
        (is (every? #(= (modid/asset-path "textures/effects" "tp_particle.png")
                        (:texture %))
                    (first trails))))
      ;; Build plan renders the trail as textured billboard quads (upstream
      ;; Particle sprite, camera-facing), no vanilla particles involved.
      (let [{:keys [ops]} (arc-beam/effect-build-plan
                           :threatening-teleport {:x 0.0 :y 0.0 :z 0.0}
                           {:player-uuid "viewer"} 0 nil)]
        (is (seq ops))
        (is (every? #(= :quad (:kind %)) ops))
        (is (every? #(= (modid/asset-path "textures/effects" "tp_particle.png")
                        (:texture %))
                    ops))))))

(deftest perform-miss-plays-no-sound-and-no-trail-test
  (let [sounds* (atom [])]
    (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "tt-test"})
                  client-sounds/queue-sound-effect! (fn [owner fx]
                                                      (swap! sounds* conj fx)
                                                      nil)]
      (tfx/init!)
      (enqueue! "ctx-m" :threatening-teleport/fx-perform
                {:mode :perform
                 :start-x 1.0 :start-y 2.0 :start-z 3.0
                 :target-x 4.0 :target-y 5.0 :target-z 6.0
                 :hit? false})
      ;; Upstream c_end: sound + trail only when attacked.
      (is (zero? (count @sounds*)))
      (is (empty? (:trails (tfx/fx-snapshot))))
      (is (nil? (build-plan))))))

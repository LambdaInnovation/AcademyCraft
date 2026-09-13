(ns cn.li.ac.content.ability.teleporter.penetrate-teleport-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.teleporter.penetrate-teleport-fx :as pfx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- with-fresh-penetrate-teleport-fx-runtime [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (pfx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (pfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-penetrate-teleport-fx-runtime)

(defn- owner [] {:client-session-id "penetrate-teleport-test"})

(deftest init-registers-penetrate-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (pfx/init!)
      (is (= :penetrate-teleport (first @registered-level*)))
      (is (= #{:penetrate-teleport/fx-start
               :penetrate-teleport/fx-update
               :penetrate-teleport/fx-perform
               :penetrate-teleport/fx-end}
             @registered-topics*)))))

(def ^:private color-available {:r 255 :g 255 :b 255 :a 255})
(def ^:private color-unavailable {:r 255 :g 51 :b 51 :a 255})

(defn- humanoid-quads [ops]
  (filter #(re-find #"^academy:textures/effects/tp_mark/\d+\.png$"
                    (str (:texture %)))
          ops))

(defn- ambient-quads [ops]
  (filter #(re-find #"^academy:textures/effects/tp_particle\.png$"
                    (str (:texture %)))
          ops))

(deftest marker-is-humanoid-at-dest-plus-eye-height-test
  ;; Upstream l_updateMark: mark.setPosition(dest.x, dest.y + player.eyeHeight,
  ;; dest.z); MarkRender draws the tp_mark humanoid there.
  (with-redefs [client-sounds/current-effect-owner owner
                ;; rand-range calls (rand (- b a)) — keep both arities.
                clojure.core/rand (fn [& _] 0.0)]
    (pfx/init!)
    (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-start
                                         {:mode :start :available? true :distance 12.0 :x 1.0 :y 64.0 :z 3.0}
                                         :owner-key [:ctx "ctx-1"])
    (let [{:keys [ops]} (arc-beam/effect-build-plan
                         :penetrate-teleport {:x 0.0 :y 0.0 :z 0.0}
                         {:player-uuid "viewer"} 0 nil)
          humanoid (humanoid-quads ops)
          ys (mapcat (fn [op]
                       (let [^cn.li.mcmod.math.V3 p1 (:p1 op)
                             ^cn.li.mcmod.math.V3 p2 (:p2 op)
                             ^cn.li.mcmod.math.V3 p3 (:p3 op)]
                         [(.y p1) (.y p2) (.y p3)]))
                     humanoid)]
      (is (= 42 (count humanoid)))
      ;; l_updateMark puts the mark at dest.y + eyeHeight (1.62), and
      ;; MarkRender hangs the figure from there rather than standing it on it
      ;; -- so its feet land 1.5 below the mark, just above the destination.
      (is (some #(<= (Math/abs (- % (- 65.62 1.5))) 0.001) ys)
          "humanoid feet just above the destination")
      (is (some #(<= (Math/abs (- % (+ 65.62 0.5))) 0.001) ys)
          "head top 0.5 above the mark")
      (is (every? #(<= (- 65.62 1.5) % (+ 65.62 0.53125)) ys)))))

(deftest marker-re-marches-against-the-client-world-test
  ;; l_updateMark re-runs getDest client-side every tick, setting the mark's
  ;; position AND its available flag -- the flag paints it red, so a stale one
  ;; lies about whether the blink lands. The march length still comes from the
  ;; server, since getDest clamps it by CP.
  (with-redefs [client-sounds/current-effect-owner owner
                clojure.core/rand (fn [& _] 0.0)]
    (pfx/init!)
    (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-live" :penetrate-teleport/fx-start
                                         {:mode :start
                                          ;; a deliberately stale server answer
                                          :available? true :distance 12.0 :march-distance 12.0
                                          :x 1.0 :y 64.0 :z 3.0}
                                         :owner-key [:ctx "ctx-live"])
    (let [view (fn [extra]
                 (merge {:player-uuid "viewer"
                         :player-x 0.0 :player-y 64.0 :player-z 0.0 :player-eye-y 65.62
                         :player-yaw-rad 0.0 :player-pitch-rad 0.0}
                        extra))
          plan (fn [v] (:ops (arc-beam/effect-build-plan
                              :penetrate-teleport {:x 0.0 :y 65.0 :z 0.0} v 0 nil)))
          span (fn [ops]
                 (let [ys (mapcat (fn [op]
                                    (map (fn [k] (.y ^cn.li.mcmod.math.V3 (get op k))) [:p0 :p1 :p2 :p3]))
                                  (humanoid-quads ops))]
                   [(apply min ys) (apply max ys)]))]
      ;; a solid slab at z in [4, 6] with clear air past it: the march stops
      ;; beyond the wall, so the mark stands there and is available (white).
      (let [ops (plan (view {:block-collidable-at?
                             (fn [_x _y z] (<= 4 z 6))}))
            anchor (+ 64.0 (- 65.62 64.0))]
        (is (= [(- anchor 1.5) (+ anchor 0.53125)] (span ops))
            "anchored at the marched point plus the caster's eye height")
        (is (every? #(= color-available (:color %)) (humanoid-quads ops))))
      ;; wall all the way: the march never comes out, so available? is false
      ;; and the figure must be red even though the server said otherwise.
      (let [ops (plan (view {:block-collidable-at? (fn [& _] true)}))]
        (is (every? #(= color-unavailable (:color %)) (humanoid-quads ops))))
      ;; no world access -> the server's answer stands
      (is (seq (plan (view {})))))))

(deftest unavailable-marker-is-red-tinted-and-silent-test
  ;; Upstream MarkRender tints the model glColor4d(1, 0.2, 0.2, 1) when
  ;; !mark.available, and EntityTPMarking spawns no particles while
  ;; unavailable.
  (with-redefs [client-sounds/current-effect-owner owner
                clojure.core/rand (fn [& _] 0.0)]
    (pfx/init!)
    (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-start
                                         {:mode :start :available? false :distance 12.0 :x 1.0 :y 64.0 :z 3.0}
                                         :owner-key [:ctx "ctx-1"])
    (dotimes [_ 3] (level-effects/tick-level-effects!))
    (let [{:keys [ops]} (arc-beam/effect-build-plan
                         :penetrate-teleport {:x 0.0 :y 0.0 :z 0.0}
                         {:player-uuid "viewer"} 0 nil)]
      (is (= 42 (count (humanoid-quads ops))))
      (is (every? #(= {:r 255 :g 51 :b 51 :a 255} (:color %)) (humanoid-quads ops)))
      (is (empty? (ambient-quads ops))
          "no TPParticleFactory particles while unavailable"))))

(deftest available-marker-spawns-green-ambient-particles-test
  ;; Upstream EntityTPMarking.onUpdate: available && rand.nextDouble() < 0.4.
  (with-redefs [client-sounds/current-effect-owner owner
                clojure.core/rand (fn [& _] 0.0)]
    (pfx/init!)
    (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-start
                                         {:mode :start :available? true :distance 12.0 :x 1.0 :y 64.0 :z 3.0}
                                         :owner-key [:ctx "ctx-1"])
    ;; l_updateMark runs every tick — an update must refresh the target but
    ;; keep the accumulated :ticks / :ambient-particles.
    (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-update
                                         {:mode :update :available? true :distance 14.0 :x 1.0 :y 64.0 :z 3.0}
                                         :owner-key [:ctx "ctx-1"])
    (dotimes [_ 2] (level-effects/tick-level-effects!))
    (let [{:keys [ops]} (arc-beam/effect-build-plan
                         :penetrate-teleport {:x 0.0 :y 0.0 :z 0.0}
                         {:player-uuid "viewer"} 0 nil)]
      (is (= 42 (count (humanoid-quads ops))))
      (is (seq (ambient-quads ops))
          "green TPParticle particles spawn while available"))))

(deftest perform-plays-sound-without-burst-and-clears-state-test
  ;; Upstream l_onKeyUp plays tp.tp (local) — no portal burst — then the
  ;; server terminate kills the mark (c_endEffect).
  (let [sound-calls* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                  client-sounds/current-effect-owner owner
                  client-sounds/queue-sound-effect! (fn [& args]
                                                      (swap! sound-calls* conj args)
                                                      nil)]
      (pfx/init!)
      (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-start {:mode :start}
                                         :owner-key [:ctx "ctx-1"])
      (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-update {:mode :update :available? true :distance 12.0 :x 1.0 :y 2.0 :z 3.0}
                                         :owner-key [:ctx "ctx-1"])
      (dotimes [_ 3] (level-effects/tick-level-effects!))
      (is (true? (get-in (pfx/fx-snapshot) [:fx-state [:ctx "ctx-1"] :available?])))
      (level-effects/enqueue-level-effect! :penetrate-teleport "ctx-1" :penetrate-teleport/fx-perform {:mode :perform :to-x 4.0 :to-y 5.0 :to-z 6.0}
                                         :owner-key [:ctx "ctx-1"])
      (is (= 1 (count @sound-calls*)))
      (is (= "academy:tp.tp" (:sound-id (second (first @sound-calls*)))))
      ;; Upstream c_endEffect kills the mark on MSG_TERMINATED.
      (is (nil? (get-in (pfx/fx-snapshot) [:fx-state [:ctx "ctx-1"]]))))))

(deftest fx-snapshot-default-without-registered-state-test
  (pfx/init!)
  (is (= {:fx-state {}}
         (pfx/fx-snapshot))))

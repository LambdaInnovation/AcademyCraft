(ns cn.li.ac.content.ability.teleporter.mark-teleport-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.content.ability.teleporter.mark-teleport-fx :as mfx]))

(defn- with-fresh-mark-teleport-fx-runtime [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (mfx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (mfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-mark-teleport-fx-runtime)

(deftest init-registers-mark-teleport-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (mfx/init!)
      (is (= :mark-teleport (first @registered-level*)))
      (is (= #{:mark-teleport/fx-start
               :mark-teleport/fx-update
               :mark-teleport/fx-perform
               :mark-teleport/fx-end}
             @registered-topics*)))))

(deftest enqueue-perform-plays-teleport-sound-test
  ;; Upstream s_execute -> MSG_SOUND -> c_sound: only the tp.tp sound, no
  ;; burst particles — the green ambient particles already surround the mark.
  (let [sounds* (atom [])]
    (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})
                  client-sounds/queue-sound-effect! (fn [& args]
                                                      (swap! sounds* conj args)
                                                      nil)]
      (mfx/init!)
      (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-perform {:mode :perform :target {:x 2.0 :y 64.0 :z 3.0} :distance 8.0}
                                         :owner-key [:ctx "ctx-1"])
      (is (= 1 (count @sounds*)))
      (is (= "academy:tp.tp" (:sound-id (second (first @sounds*))))))))

(deftest build-plan-emits-humanoid-and-ambient-particles-test
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})
                ;; rand-range calls (rand (- b a)) — keep both arities.
                clojure.core/rand (fn [& _] 0.0)]
    (mfx/init!)
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-start
                                         {:mode :start :target {:x 1.0 :y 64.0 :z 3.0} :distance 8.0}
                                         :owner-key [:ctx "ctx-1"])
    ;; Two ticks: the humanoid frame advances and ambient particles spawn
    ;; (rand 0.0 -> the 0.4 gate always passes).
    (level-effects/tick-level-effects!)
    (level-effects/tick-level-effects!)
    (let [{:keys [ops]} (arc-beam/effect-build-plan
                         :mark-teleport {:x 0.0 :y 0.0 :z 0.0}
                         {:player-uuid "viewer"} 0 nil)]
      ;; 7 ModelBiped parts (incl. the headwear layer) x 6 faces + 2 ambient
      ;; particles (2 ticks, rand 0).
      (is (<= 38 (count ops)))
      (is (every? #(= :quad (:kind %)) ops))
      ;; The humanoid is built from tp_mark frame quads (upstream MarkRender
      ;; effect sequence). MarkRender never applies RenderLivingBase's
      ;; translate(0, -1.5, 0), so the figure HANGS from the mark: head top
      ;; 0.5 above it and feet 1.5 below.
      (let [humanoid (filter #(re-find #"^academy:textures/effects/tp_mark/\d+\.png$"
                                       (str (:texture %)))
                             ops)
            ys (mapcat (fn [op]
                         (let [^cn.li.mcmod.math.V3 p1 (:p1 op)
                               ^cn.li.mcmod.math.V3 p2 (:p2 op)
                               ^cn.li.mcmod.math.V3 p3 (:p3 op)]
                           [(.y p1) (.y p2) (.y p3)]))
                       humanoid)]
        (is (= 42 (count humanoid)))
        (is (some #(<= (Math/abs (- % 62.5)) 0.001) ys)
            "feet 1.5 below the mark")
        (is (some #(<= (Math/abs (- % 64.5)) 0.001) ys)
            "head top 0.5 above the mark")
        ;; the headwear layer is inflated 0.5 model units a side
        (is (every? #(<= 62.5 % 64.53125) ys)))
      ;; Ambient green TPParticle particles at the mark. Their offsets are
      ;; measured from the mark too (+0.2..1.6 then -1.6), so relative to the
      ;; figure they wrap it from just above the feet to the head -- they must
      ;; never end up under its feet, which is where they went while the
      ;; humanoid stood on the mark instead of hanging from it.
      (let [motes (filter #(re-find #"^academy:textures/effects/tp_particle\.png$"
                                    (str (:texture %)))
                          ops)]
        (is (seq motes))
        (doseq [op motes]
          (let [^cn.li.mcmod.math.V3 p1 (:p1 op)]
            (is (<= 62.5 (.y p1) 64.5))))))))

(defn- head-front-quads
  "The head box's front face, picked out by its skin UV region."
  [ops]
  (filter #(and (re-find #"^academy:textures/effects/tp_mark/\d+\.png$" (str (:texture %)))
                (= [0.125 0.25 0.25 0.5] [(:u0 %) (:u1 %) (:v0 %) (:v1 %)]))
          ops))

(deftest humanoid-faces-the-caster-and-ignores-the-camera-test
  ;; MarkRender rotates by -rotationYaw where a normal entity renderer uses
  ;; 180 - rotationYaw, and the mark copies the caster's yaw, so the figure
  ;; faces back along their look. Deriving that from the camera->marker
  ;; direction (as the port did) made it turn with the view instead: the
  ;; caster only ever saw one side of it.
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                clojure.core/rand (fn [& _] 0.0)]
    (mfx/init!)
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-face" :mark-teleport/fx-start
                                         {:mode :start :target {:x 0.0 :y 64.0 :z 10.0} :distance 10.0}
                                         :owner-key [:ctx "ctx-face"])
    (let [plan (fn [cam yaw]
                 (:ops (arc-beam/effect-build-plan
                        :mark-teleport cam {:player-uuid "viewer" :player-yaw-rad yaw} 0 nil)))
          ;; yaw 0: the caster looks toward +Z, so the figure faces -Z and its
          ;; head-front face sits on the low-z side of the head box.
          front (head-front-quads (plan {:x 0.0 :y 65.6 :z 0.0} 0.0))]
      (is (seq front))
      (doseq [op front]
        (doseq [k [:p0 :p1 :p2 :p3]]
          (let [^cn.li.mcmod.math.V3 v (get op k)]
            (is (<= (Math/abs (- (.z v) (- 10.0 0.25))) 0.001)
                "head front face on the -Z side, i.e. looking back at the caster")))))
    (let [geom (fn [cam yaw]
                 (mapv (fn [op] [(:u0 op) (.x ^cn.li.mcmod.math.V3 (:p0 op))
                                 (.y ^cn.li.mcmod.math.V3 (:p0 op))
                                 (.z ^cn.li.mcmod.math.V3 (:p0 op))])
                       (filter #(re-find #"^academy:textures/effects/tp_mark/\d+\.png$"
                                         (str (:texture %)))
                               (:ops (arc-beam/effect-build-plan
                                      :mark-teleport cam
                                      {:player-uuid "viewer" :player-yaw-rad yaw} 0 nil)))))]
      (is (= (geom {:x 0.0 :y 65.6 :z 0.0} 0.0)
             (geom {:x 40.0 :y 90.0 :z -70.0} 0.0))
          "moving the camera must not turn the figure")
      (is (not= (geom {:x 0.0 :y 65.6 :z 0.0} 0.0)
                (geom {:x 0.0 :y 65.6 :z 0.0} (/ Math/PI 2.0)))
          "turning the caster must"))))

(defn- humanoid-y-span [ops]
  (let [ys (mapcat (fn [op]
                     (map (fn [k] (.y ^cn.li.mcmod.math.V3 (get op k))) [:p0 :p1 :p2 :p3]))
                   (filter #(re-find #"^academy:textures/effects/tp_mark/\d+\.png$" (str (:texture %)))
                           ops))]
    (when (seq ys) [(apply min ys) (apply max ys)])))

(deftest marker-re-solves-the-aim-from-the-live-view-test
  ;; MTContextC.l_update re-runs getDest client-side every tick, so the mark
  ;; tracks the crosshair instead of trailing the server by a tick plus the
  ;; round trip. The range comes from the server (getMaxDist reads CP/exp);
  ;; the trace and the six-face table are re-solved here.
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                clojure.core/rand (fn [& _] 0.0)]
    (mfx/init!)
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-live" :mark-teleport/fx-start
                                         {:mode :start
                                          ;; a deliberately stale server answer
                                          :target {:x 0.0 :y 64.0 :z 10.0}
                                          :dist 20.0 :distance 10.0}
                                         :owner-key [:ctx "ctx-live"])
    (let [view (fn [extra]
                 (merge {:player-uuid "viewer"
                         :player-x 0.0 :player-z 0.0 :player-eye-y 65.62
                         :player-yaw-rad 0.0 :player-pitch-rad 0.0}
                        extra))
          span (fn [v] (humanoid-y-span
                        (:ops (arc-beam/effect-build-plan
                               :mark-teleport {:x 0.0 :y 65.6 :z 0.0} v 0 nil))))]
      ;; up-face block hit at y=70 -> dest 71.8, and the figure hangs from it
      (is (= [(- 71.8 1.5) (+ 71.8 0.53125)]
             (span (view {:raycast-from-view
                          (fn [_ _] {:hit-type :block :face :up
                                     :x 0.0 :y 69.0 :z 12.0
                                     :hit-x 0.0 :hit-y 70.0 :hit-z 12.0})}))))
      ;; nothing hit -> eye + look * dist, i.e. z = 20 at eye height
      (is (= [(- 65.62 1.5) (+ 65.62 0.53125)]
             (span (view {:raycast-from-view (fn [_ _] nil)}))))
      ;; no world access in the view context -> the server's target stands
      (is (= [(- 64.0 1.5) (+ 64.0 0.53125)]
             (span (view {})))))))

(deftest perform-clears-mark-state-test
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)]
    (mfx/init!)
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-start
                                         {:mode :start :target {:x 1.0 :y 2.0 :z 3.0} :distance 8.0}
                                         :owner-key [:ctx "ctx-1"])
    (is (some? (get (:effect-state (mfx/fx-snapshot)) [:ctx "ctx-1"])))
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-perform
                                         {:mode :perform :target {:x 1.0 :y 2.0 :z 3.0} :distance 8.0}
                                         :owner-key [:ctx "ctx-1"])
    ;; Upstream l_end kills the mark on MSG_TERMINATED.
    (is (nil? (get (:effect-state (mfx/fx-snapshot)) [:ctx "ctx-1"])))))

(deftest enqueue-end-clears-state-test
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "mark-teleport-test"})]
    (mfx/init!)
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-start {:mode :start}
                                         :owner-key [:ctx "ctx-1"])
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-update {:mode :update :target {:x 1.0 :y 2.0 :z 3.0} :distance 2.0}
                                         :owner-key [:ctx "ctx-1"])
    (is (some? (get (:effect-state (mfx/fx-snapshot)) [:ctx "ctx-1"])))
    (level-effects/enqueue-level-effect! :mark-teleport "ctx-1" :mark-teleport/fx-end {:mode :end}
                                         :owner-key [:ctx "ctx-1"])
    (is (nil? (get (:effect-state (mfx/fx-snapshot)) [:ctx "ctx-1"])))))



(deftest fx-snapshot-default-without-registered-state-test
  ;; clojure.test runs deftests in ns-interns hash order, not declaration
  ;; order — this test may run before any other init! call, so register the
  ;; effect (and load the impl defmethods) here instead of relying on order.
  (mfx/init!)
  (is (= {:effect-state {}}
         (mfx/fx-snapshot))))

(ns cn.li.ac.content.ability.meltdowner.mine-ray-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.rv3]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.mine-ray-fx :as mr-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (mr-fx/reset-mine-ray-fx-for-test!)
          (f)
          (finally
            (mr-fx/reset-mine-ray-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

;; The loop-sound bridge needs the platform client bridge, which tests do not
;; install — stub it so :start/:end can enqueue in the unit environment.
(defn- with-bridge-stub [f]
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)]
    (f)))

;; MineRay has no arc-beam impl — it owns its enqueue/tick fns and registers
;; them through fx-spec, so tests drive those directly.
(defn- enqueue!
  [enqueue-state! ctx-id channel payload]
  (level-effects/update-effect-state! :mine-ray
    (fn [store] (enqueue-state! store ctx-id channel [:ctx ctx-id] payload)))
  nil)

(defn- tick!
  [tick-state!]
  (level-effects/update-effect-state! :mine-ray
    (fn [store] (tick-state! store)))
  nil)

(deftest init-registers-owner-aware-mine-ray-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (mr-fx/init!)
      (is (= :mine-ray (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:mine-ray/fx-start
               :mine-ray/fx-progress
               :mine-ray/fx-end}
             @registered-topics*)))))

(deftest start-progress-tick-end-manage-state-test
  (with-bridge-stub (fn []
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/tick-state!)
        build-plan (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/build-plan)
        particles* (atom [])
        sounds* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "mine-ray-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                             (swap! particles* conj args)
                                                             nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                        (swap! sounds* conj args)
                                                        nil)]
      (enqueue! enqueue-state! "ctx-mr" :mine-ray/fx-start {:mode :start :variant :expert :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-mr" :mine-ray/fx-progress {:mode :progress
                                                 :x 2 :y 64 :z 5
                                                 :progress 0.5
                                                 :source-player-id "player-a"})
      (is (= {:x 2 :y 64 :z 5}
             (get-in (mr-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-mr"] :target])))
      (is (seq (:ops (build-plan {:x 0.0 :y 65.0 :z 0.0} nil 0 nil))))
      (dotimes [_ 8]
        (tick! tick-state!))
      (is (seq @particles*))
      (enqueue! enqueue-state! "ctx-mr" :mine-ray/fx-end {:mode :end :source-player-id "player-a"})
      (is (nil? (get-in (mr-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-mr"]])))
      (is (seq @sounds*)))))
))

(deftest mine-ray-start-sound-varies-by-variant-test
  (with-bridge-stub (fn []
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/enqueue-state!)
        sounds* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "mine-ray-fx-test"})
                  client-particles/queue-particle-effect! (fn [& _] nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                        (swap! sounds* conj args)
                                                        nil)]
      (doseq [[ctx-id variant expected-sound-id]
              [["ctx-basic" :basic "academy:md.mine_basic_startup"]
               ["ctx-expert" :expert "academy:md.mine_expert_startup"]
               ["ctx-luck" :luck "academy:md.mine_luck_startup"]]]
        (enqueue! enqueue-state! ctx-id :mine-ray/fx-start {:mode :start :variant variant :source-player-id "player-a"})
        (is (= expected-sound-id
               (:sound-id (second (last @sounds*)))))))))
))

(deftest mine-ray-particle-cadence-test
  (with-bridge-stub (fn []
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/enqueue-state!)
        tick-state! (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/tick-state!)
        particles* (atom [])]
    (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "mine-ray-fx-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particles* conj args)
                                                            nil)
                  client-sounds/queue-sound-effect! (fn [& _] nil)]
      (enqueue! enqueue-state! "ctx-cadence" :mine-ray/fx-start {:mode :start :variant :basic :source-player-id "player-a"})
      (enqueue! enqueue-state! "ctx-cadence" :mine-ray/fx-progress {:mode :progress
                                                     :x 2 :y 64 :z 5
                                                     :progress 0.2
                                                     :source-player-id "player-a"})

      (dotimes [_ 16]
        (tick! tick-state!))

      (is (= 16 (get-in (mr-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-cadence"] :ticks])))
      (is (= 16 (count @particles*))
          "mine-ray should emit target particles EVERY same-target tick (upstream c_spawnParticles)")

      (enqueue! enqueue-state! "ctx-cadence" :mine-ray/fx-end {:mode :end :source-player-id "player-a"})
      (is (nil? (get-in (mr-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-cadence"]]))))))
))

;; ---------------------------------------------------------------------------
;; Composite parity per variant
;; ---------------------------------------------------------------------------

(def ^:private mine-ray-ops* (var-get #'cn.li.ac.content.ability.meltdowner.mine-ray-fx/mine-ray-ops))

(defn- variant-radii [variant]
  (let [beam {:start (cn.li.ac.ability.client.effects.rv3/v3 0.0 64.0 0.0)
              :end (cn.li.ac.ability.client.effects.rv3/v3 15.0 64.0 0.0)
              :ttl 1 :max-ttl 1}
        ops (mine-ray-ops* {:x 0.0 :y 70.0 :z 0.0} beam variant)
        chord->r (/ 1.0 (* 2.0 (Math/sin (/ Math/PI 12.0))))]
    {:glow (filter #(re-find #"effects/mdray" (str (:texture %))) ops)
     :radii (->> ops
                 (remove #(re-find #"effects/mdray" (str (:texture %))))
                 (map (fn [op]
                        (let [^cn.li.mcmod.math.V3 a (:p0 op)
                              ^cn.li.mcmod.math.V3 b (:p1 op)]
                          (* chord->r
                             (Math/sqrt (+ (Math/pow (- (.-x a) (.-x b)) 2)
                                           (Math/pow (- (.-y a) (.-y b)) 2)
                                           (Math/pow (- (.-z a) (.-z b)) 2))))))))
     :ops ops}))

(deftest mine-ray-widths-match-each-variants-renderer-test
  ;; basic  mdray_small  inner 0.03  outer 0.045
  ;; expert mdray_expert inner 0.045 outer 0.056, inner alpha 180 (doRender)
  ;; luck   mdray_luck   inner 0.04  outer 0.05, purple
  ;; The port derived the inner radius from the outer by a 0.86 ratio and ran
  ;; the outer 15-40% wide — about 1.5x the original bore on the inner.
  (let [{:keys [radii glow ops]} (variant-radii :basic)]
    (is (= 3 (count glow)))
    (is (< 0.04 (apply max radii) 0.05) "basic outer 0.045")
    (is (some (fn [r] (< 0.025 r 0.035)) radii) "basic inner 0.03")
    (is (every? #(re-find #"mdray_small" (str (:texture %))) glow)))
  (let [{:keys [radii glow ops]} (variant-radii :expert)]
    (is (< 0.05 (apply max radii) 0.06) "expert outer 0.056")
    (is (some (fn [r] (< 0.04 r 0.05)) radii) "expert inner 0.045")
    (is (every? #(re-find #"mdray_expert" (str (:texture %))) glow))
    (is (some #(= 180 (:a (:color %)))
              (remove #(re-find #"effects/mdray" (str (:texture %))) ops))
        "expert re-sets the inner colour to alpha 180 every frame"))
  (let [{:keys [radii glow ops]} (variant-radii :luck)]
    (is (< 0.045 (apply max radii) 0.055) "luck outer 0.05")
    (is (some (fn [r] (< 0.035 r 0.045)) radii) "luck inner 0.04")
    (is (every? #(re-find #"mdray_luck" (str (:texture %))) glow))
    (is (some #(= {:r 205 :g 166 :b 232 :a 50} (:color %)) ops)
        "luck outer is purple, not green")))

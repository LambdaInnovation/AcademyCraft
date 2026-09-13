(ns cn.li.ac.content.ability.meltdowner.ray-barrage-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.ray-barrage-fx :as rb-fx]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (rb-fx/reset-fx-for-test!)
          (f)
          (finally
            (rb-fx/reset-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(def ^:private p0 {:x 0.0 :y 0.0 :z 0.0})
(def ^:private p1 {:x 1.0 :y 0.0 :z 0.0})

(deftest init-registers-owner-aware-ray-barrage-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (rb-fx/init!)
      (is (= :ray-barrage (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:ray-barrage/fx-preray :ray-barrage/fx-barrage}
             @registered-topics*)))))

(deftest fx-handler-routes-preray-and-barrage-to-level-effect-test
  (let [handlers* (atom {})
        enqueued* (atom [])
        sounds* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)
                  client-sounds/queue-current-sound-effect! (fn [payload]
                                                              (swap! sounds* conj payload)
                                                              nil)]
      (rb-fx/init!)
      ((get @handlers* :ray-barrage/fx-preray) "ctx-pre" :ray-barrage/fx-preray
       {:start p0 :end p1 :hit? true :source-player-id "player-a" :world-id "world-a"})
      ((get @handlers* :ray-barrage/fx-barrage) "ctx-bar" :ray-barrage/fx-barrage
       {:silbarn p1 :yaw 12.0 :pitch -6.0 :source-player-id "player-a" :world-id "world-a"})
      (is (= [[:ray-barrage "ctx-pre" :ray-barrage/fx-preray
               {:source-player-id "player-a" :world-id "world-a"
                :mode :preray :start p0 :end p1 :hit? true}
               [:owner-key [:ctx "ctx-pre"]]]
              [:ray-barrage "ctx-bar" :ray-barrage/fx-barrage
               {:source-player-id "player-a" :world-id "world-a"
                :mode :barrage :silbarn p1 :yaw 12.0 :pitch -6.0}
               [:owner-key [:ctx "ctx-bar"]]]]
             @enqueued*))
      (is (= 2 (count @sounds*)) "preray + barrage sounds"))))

(deftest fx-handler-preray-and-barrage-play-ray-small-sounds-test
  (let [handlers* (atom {})
        sounds* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [& _] nil)
                  client-sounds/queue-current-sound-effect! (fn [payload]
                                                              (swap! sounds* conj payload)
                                                              nil)]
      (rb-fx/init!)
      ((get @handlers* :ray-barrage/fx-preray) "ctx-pre" :ray-barrage/fx-preray {:start p0 :end p1})
      ((get @handlers* :ray-barrage/fx-barrage) "ctx-bar" :ray-barrage/fx-barrage {:silbarn p1 :yaw 0.0 :pitch 0.0})
      (is (= 2 (count @sounds*)))
      (is (= [0.8 0.5] (map :volume @sounds*))))))

(deftest enqueue-preray-tick-and-build-plan-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :ray-barrage "ctx-a" :ray-barrage/fx-preray
             {:mode :preray :start {:x 1.0 :y 64.0 :z 2.0}
              :end {:x 4.0 :y 64.0 :z 6.0}
              :hit? false
              :source-player-id "player-a"
              :world-id "world-a"})
    (is (some? (get-in (rb-fx/fx-snapshot) [:beam-queue [:ctx "ctx-a"]])))
    (is (seq (:ops (arc-beam/effect-build-plan :ray-barrage {:x 0.0 :y 65.0 :z 0.0}
                                               {:player-uuid "player-a"} 0))))
    (dotimes [_ 30]
      (level-effects/update-effect-state! :ray-barrage
        (fn [store] (arc-beam/effect-tick-state! :level :ray-barrage store))))
    (is (nil? (arc-beam/effect-build-plan :ray-barrage {:x 0.0 :y 65.0 :z 0.0} nil 0)))
    (is (empty? (:beam-queue (rb-fx/fx-snapshot))))))

(deftest rays-survive-context-clear-and-expire-on-their-own-ttl-test
  ;; :instant context ends on the same tick as perform — clear-effect-owner!
  ;; must NOT kill the beams (upstream rays carry their own lives).
  (rb-fx/init!)
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :ray-barrage "ctx-a" :ray-barrage/fx-preray
             {:mode :preray :start {:x 1.0 :y 64.0 :z 2.0}
              :end {:x 4.0 :y 64.0 :z 6.0}
              :hit? false
              :source-player-id "player-a"
              :world-id "world-a"})
    (rb-fx/clear-fx-owner! [:ctx "ctx-a"])
    (is (seq (get-in (rb-fx/fx-snapshot) [:beam-queue [:ctx "ctx-a"]]))
        "the ray survives the context ending")
    (is (seq (:ops (arc-beam/effect-build-plan :ray-barrage {:x 0.0 :y 65.0 :z 0.0}
                                               {:player-uuid "player-a"} 0)))
        "and still renders")
    (dotimes [_ 31]
      (level-effects/update-effect-state! :ray-barrage
        (fn [store] (arc-beam/effect-tick-state! :level :ray-barrage store))))
    (is (empty? (:beam-queue (rb-fx/fx-snapshot)))
        "expires on its own ttl")))

(deftest preray-hit-extends-life-and-barrage-scatters-many-sub-rays-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :ray-barrage "ctx-hit" :ray-barrage/fx-preray
             {:mode :preray :start p0 :end p1 :hit? true :source-player-id "player-a"})
    (is (= 50 (get-in (rb-fx/fx-snapshot) [:beam-queue [:ctx "ctx-hit"] 0 :ttl]))
        "preray that hit a silbarn lives 50 ticks like the original")
    (arc-beam/enqueue-for-test! :ray-barrage "ctx-bar" :ray-barrage/fx-barrage
             {:mode :barrage :silbarn p1 :yaw 0.0 :pitch 0.0 :source-player-id "player-a"})
    (let [sub-rays (get-in (rb-fx/fx-snapshot) [:beam-queue [:ctx "ctx-bar"]])]
      (is (<= 25 (count sub-rays) 30))
      (is (every? #(= 50 (:ttl %)) sub-rays))
      ;; every sub ray issues from the silbarn position at length 15
      (is (every? (fn [ray]
                    (let [sx (.-x (:start ray)) sy (.-y (:start ray)) sz (.-z (:start ray))
                          ex (.-x (:end ray)) ey (.-y (:end ray)) ez (.-z (:end ray))
                          dx (- ex sx) dy (- ey sy) dz (- ez sz)]
                      (and (< (Math/abs (- (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz))) 15.0))
                              1.0e-6)
                           (and (= sx 1.0) (= sz 0.0)))))
                  sub-rays)))))

;; ---------------------------------------------------------------------------
;; Composite parity: the pre-ray and the fan are different renderers upstream
;; ---------------------------------------------------------------------------

(defn- tick-fx! [n]
  (dotimes [_ n]
    (level-effects/update-effect-state! :ray-barrage
      (fn [store] (arc-beam/effect-tick-state! :level :ray-barrage store)))))

(defn- ops-for [ctx-id]
  (:ops (arc-beam/effect-build-plan :ray-barrage {:x 0.0 :y 70.0 :z 0.0}
                                    {:player-uuid "other" :x 0.0 :y 64.0 :z 0.0} 0)))

(defn- tube-radii
  "Radius of each tube quad, from the chord between its two leading corners:
  a 12-segment ring of radius r has chord 2r*sin(pi/12). Direction-independent,
  which matters for the fan (every sub-ray points somewhere else)."
  [ops]
  (let [chord->r (/ 1.0 (* 2.0 (Math/sin (/ Math/PI 12.0))))]
    (->> ops
         (remove #(re-find #"effects/mdray" (str (:texture %))))
         (map (fn [op]
                (let [^cn.li.mcmod.math.V3 a (:p0 op)
                      ^cn.li.mcmod.math.V3 b (:p1 op)]
                  (* chord->r
                     (Math/sqrt (+ (Math/pow (- (.-x a) (.-x b)) 2)
                                   (Math/pow (- (.-y a) (.-y b)) 2)
                                   (Math/pow (- (.-z a) (.-z b)) 2))))))))))

(deftest preray-uses-the-barrage-pre-widths-test
  ;; EntityBarrageRayPre: inner 0.045, outer 0.052, glow 0.4. The port drew
  ;; every ray at these widths but left the glow board at its 1.5 default.
  (arc-beam/enqueue-for-test! :ray-barrage "ctx-pre" :ray-barrage/fx-preray
    {:mode :preray :hit? true
     :start {:x 0.0 :y 64.0 :z 0.0}
     :end {:x 15.0 :y 64.0 :z 0.0}})
  (tick-fx! 5)
  (let [ops (ops-for "ctx-pre")
        glow (filter #(re-find #"effects/mdray_small/" (str (:texture %))) ops)
        radii (tube-radii ops)]
    (is (= 3 (count glow)))
    (is (< 0.05 (apply max radii) 0.055) "outer 0.052")
    (is (some (fn [r] (< 0.04 r 0.05)) radii) "inner 0.045")
    ;; glow board width 0.4 — measured across the board
    (let [board (first glow)
          ^cn.li.mcmod.math.V3 a (:p0 board)
          ^cn.li.mcmod.math.V3 d (:p3 board)
          w (Math/sqrt (+ (Math/pow (- (.-x a) (.-x d)) 2)
                          (Math/pow (- (.-y a) (.-y d)) 2)
                          (Math/pow (- (.-z a) (.-z d)) 2)))]
      (is (< 0.35 w 0.45) (str "glow board is 0.4 across, was 1.5: " w)))))

(deftest barrage-fan-uses-the-small-ray-widths-test
  ;; EntityMdRayBarrage extends SmallMdRayRender: inner 0.03, outer 0.045,
  ;; glow 0.3 — narrower than the pre-ray, which the port did not distinguish.
  (arc-beam/enqueue-for-test! :ray-barrage "ctx-fan" :ray-barrage/fx-barrage
    {:mode :barrage
     :silbarn {:x 0.0 :y 64.0 :z 0.0}
     :yaw 0.0 :pitch 0.0})
  (tick-fx! 5)
  (let [ops (ops-for "ctx-fan")
        radii (tube-radii ops)]
    (is (seq radii))
    (is (< (apply max radii) 0.05) "outer 0.045, not the pre-ray's 0.052")
    (is (some (fn [r] (< 0.025 r 0.035)) radii) "inner 0.03")))

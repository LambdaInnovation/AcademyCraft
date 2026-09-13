(ns cn.li.ac.content.ability.meltdowner.jet-engine-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.jet-engine-fx :as je-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks])
  (:import [cn.li.mcmod.math V3]))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (try
          (level-effects/reset-level-effect-registry-for-test!)
          (je-fx/reset-fx-for-test!)
          (f)
          (finally
            (je-fx/reset-fx-for-test!)
            (level-effects/reset-level-effect-registry-for-test!))))))

(use-fixtures :each reset-fixture)

(defn- dispatch! [ctx-id channel payload]
  (level-effects/enqueue-level-effect! :jet-engine ctx-id channel payload :owner-key [:ctx ctx-id]))

(deftest init-registers-parity-jet-engine-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (je-fx/init!)
      (is (= :jet-engine (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:jet-engine/fx-start
               :jet-engine/fx-update
               :jet-engine/fx-end
               :jet-engine/fx-trigger-start
               :jet-engine/fx-trigger-update
               :jet-engine/fx-trigger-end}
             @registered-topics*)))))

(deftest mark-and-trigger-state-flow-with-snapshot-test
  (let [
        sounds* (atom [])
        local-effects* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& args]
                                                               (swap! sounds* conj (last args))
                                                               nil)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! local-effects* conj [effect-key payload])
                                                     (when (= :mcmod/spawn-local-scripted-effect effect-key)
                                                       "shield-uuid-1"))]
      (je-fx/init!)
      (dispatch! "ctx-je" :jet-engine/fx-start {:mode :mark-start :target {:x 1.0 :y 64.0 :z 1.0} :hold-ticks 0})
      (is (= :marking (get-in (je-fx/fx-snapshot) [:fx-state [:ctx "ctx-je"] :phase])))
      (is (seq (:ops (arc-beam/effect-build-plan :jet-engine {:x 0.0 :y 65.0 :z 0.0} nil 0))))

      (dispatch! "ctx-je" :jet-engine/fx-trigger-start
                 {:mode :trigger-start
                  :start {:x 0.0 :y 64.0 :z 0.0}
                  :target {:x 4.0 :y 64.0 :z 0.0}
                  :pos {:x 1.0 :y 64.0 :z 0.0}
                  :trigger-ticks 0})
      (is (= :triggering (get-in (je-fx/fx-snapshot) [:fx-state [:ctx "ctx-je"] :phase])))
      (let [ops (:ops (arc-beam/effect-build-plan :jet-engine {:x 0.0 :y 65.0 :z 0.0} nil 1))]
        (is (seq ops))
        (is (some #(= :line (:kind %)) ops))
        (is (some #(= :quad (:kind %)) ops)))

      (dotimes [_ 20]
        (level-effects/tick-level-effects!))
      (is (nil? (arc-beam/effect-build-plan :jet-engine {:x 0.0 :y 65.0 :z 0.0} nil 2)))
      ;; Skill sounds are commented out until fitting ones are found.
      (is (empty? @sounds*))
            (is (= [[:mcmod/spawn-local-scripted-effect {:effect-id "entity_diamond_shield"}]
              [:mcmod/remove-local-scripted-effect {:entity-uuid "shield-uuid-1"}]]
              @local-effects*)))))

(deftest mark-draws-three-ripples-not-a-line-ring-test
  ;; EntityRippleMark + RippleMarkRender: three quads on the aim point's XZ
  ;; plane, staggered {0, -1.2, -2.4} across a 3.6s cycle, each rising
  ;; mod * 0.3 while shrinking lerp(1.9, 1.4) and fading in/out over 1.6s.
  ;; The port drew one pulsing line ring at a constant alpha instead.
  (je-fx/init!)
  (dispatch! "ctx-ripple" :jet-engine/fx-start
             {:mode :mark-start :target {:x 1.0 :y 64.0 :z 1.0} :hold-ticks 0})
  ;; 40 ticks = 2.0s in, so all three ripples are mid-cycle and visible.
  (dispatch! "ctx-ripple" :jet-engine/fx-update
             {:mode :mark-update :target {:x 1.0 :y 64.0 :z 1.0} :hold-ticks 40})
  (let [ops (:ops (arc-beam/effect-build-plan :jet-engine {:x 0.0 :y 65.0 :z 0.0} nil 0))]
    (is (= 3 (count ops)))
    (is (every? #(= :quad (:kind %)) ops))
    (is (every? #(= "academy:textures/effects/ripple.png" (:texture %)) ops))
    ;; glDisable(GL_DEPTH_TEST) + glDepthMask(false).
    (is (every? :no-depth-test? ops))
    (is (every? #(= {:r 51 :g 255 :b 51} (dissoc (:color %) :a)) ops))
    (let [halves (mapv (fn [op] (- (.-x ^V3 (:p1 op)) 1.0)) ops)
          heights (mapv (fn [op] (- (.-y ^V3 (:p0 op)) 64.0)) ops)]
      ;; sizes stay inside lerp(1.9, 1.4) -> half-extents 0.95 down to 0.70
      (is (every? #(<= 0.70 % 0.95) halves))
      ;; each ripple sits at its own point in the cycle, so no two coincide
      (is (= 3 (count (set halves))))
      (is (every? #(<= 0.0 % (* 3.6 0.3)) heights))
      (is (= 3 (count (set heights)))))
    ;; ...and 2.0s in, one of the three has just wrapped and is fading back in
    (is (some #(< (:a (:color %)) 255) ops)))
  (dispatch! "ctx-ripple" :jet-engine/fx-end {:mode :mark-end}))

(deftest trigger-start-spawns-diamond-shield-once-per-phase-entry-test
  (let [local-effects* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! local-effects* conj [effect-key payload])
                                                     (when (= :mcmod/spawn-local-scripted-effect effect-key)
                                                       "shield-uuid-2"))]
      (je-fx/init!)
      (dispatch! "ctx-je" :jet-engine/fx-trigger-start
                 {:mode :trigger-start
                  :start {:x 0.0 :y 64.0 :z 0.0}
                  :target {:x 4.0 :y 64.0 :z 0.0}
                  :pos {:x 1.0 :y 64.0 :z 0.0}
                  :trigger-ticks 0})
      (dispatch! "ctx-je" :jet-engine/fx-trigger-start
                 {:mode :trigger-start
                  :start {:x 0.0 :y 64.0 :z 0.0}
                  :target {:x 4.0 :y 64.0 :z 0.0}
                  :pos {:x 2.0 :y 64.0 :z 0.0}
                  :trigger-ticks 1})
      (is (= [[:mcmod/spawn-local-scripted-effect {:effect-id "entity_diamond_shield"}]]
             @local-effects*)))))

(deftest trigger-end-clears-owner-state-and-explicitly-removes-diamond-shield-test
  (let [local-effects* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! local-effects* conj [effect-key payload])
                                                     (when (= :mcmod/spawn-local-scripted-effect effect-key)
                                                       "shield-uuid-3"))]
      (je-fx/init!)
      (dispatch! "ctx-je" :jet-engine/fx-trigger-start
                 {:mode :trigger-start
                  :start {:x 0.0 :y 64.0 :z 0.0}
                  :target {:x 4.0 :y 64.0 :z 0.0}
                  :pos {:x 1.0 :y 64.0 :z 0.0}
                  :trigger-ticks 0})
      (is (contains? (set (keys (:fx-state (je-fx/fx-snapshot)))) [:ctx "ctx-je"]))
      (dispatch! "ctx-je" :jet-engine/fx-trigger-end {:mode :trigger-end})
      (is (not (contains? (set (keys (:fx-state (je-fx/fx-snapshot)))) [:ctx "ctx-je"])))
      (is (= [[:mcmod/spawn-local-scripted-effect {:effect-id "entity_diamond_shield"}]
              [:mcmod/remove-local-scripted-effect {:entity-uuid "shield-uuid-3"}]]
             @local-effects*)))))

(deftest trigger-flash-fades-with-ttl-test
  (do
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                  client-bridge/run-client-effect! (fn [effect-key _payload]
                                                     (when (= :mcmod/spawn-local-scripted-effect effect-key)
                                                       "shield-uuid-4"))]
      (je-fx/init!)
      (dispatch! "ctx-fade" :jet-engine/fx-trigger-start
                 {:mode :trigger-start
                  :start {:x 0.0 :y 64.0 :z 0.0}
                  :target {:x 4.0 :y 64.0 :z 0.0}
                  :pos {:x 1.0 :y 64.0 :z 0.0}
                  :trigger-ticks 0})

      ;; Screen-flash intensity is exposed via je-fx/flash-alpha (consumed by
      ;; the 2D reactive-hud overlay), not via the world-space :ops vector —
      ;; see jet-engine-fx.clj's flash-alpha docstring.
      (let [a0 (je-fx/flash-alpha nil)]
        (is (pos? a0))
        (dotimes [_ 13]
          (level-effects/tick-level-effects!))
        (let [a1 (je-fx/flash-alpha nil)]
          (is (> a0 a1)
              "screen flash alpha should fade as trigger ttl decreases")))

      (dotimes [_ 7]
        (level-effects/tick-level-effects!))

      (is (nil? (arc-beam/effect-build-plan :jet-engine {:x 0.0 :y 65.0 :z 0.0} nil 20)))
      (is (zero? (je-fx/flash-alpha nil)))
      (is (empty? (:fx-state (je-fx/fx-snapshot)))))))

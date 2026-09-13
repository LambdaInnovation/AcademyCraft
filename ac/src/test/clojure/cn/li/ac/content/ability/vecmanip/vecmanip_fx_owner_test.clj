(ns cn.li.ac.content.ability.vecmanip.vecmanip-fx-owner-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.content.ability.vecmanip.plasma-cannon-fx :as plasma-cannon-fx]
            [cn.li.ac.content.ability.vecmanip.storm-wing-fx :as storm-wing-fx]
            [cn.li.ac.content.ability.vecmanip.vec-accel-fx :as vec-accel-fx]
            [cn.li.ac.content.ability.vecmanip.vec-deviation-fx :as vec-deviation-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-fixture [f]
  (vec-deviation-fx/init!)
      (vec-accel-fx/init!)
      (storm-wing-fx/init!)
      (plasma-cannon-fx/init!)
      (vec-deviation-fx/reset-fx-for-test!)
      (vec-accel-fx/reset-fx-for-test!)
      (storm-wing-fx/reset-storm-wing-fx-for-test!)
      (plasma-cannon-fx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (vec-deviation-fx/reset-fx-for-test!)
          (vec-accel-fx/reset-fx-for-test!)
          (storm-wing-fx/reset-storm-wing-fx-for-test!)
          (plasma-cannon-fx/reset-fx-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(defn- dispatch! [effect-id {:keys [payload ctx-id channel owner-key]}]
  (level-effects/enqueue-level-effect! effect-id ctx-id channel payload :owner-key owner-key))

(deftest vec-deviation-keeps-state-and-waves-per-owner-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
  (dispatch! :vec-deviation (event "ctx-a" :vec-deviation/fx-start {:mode :start}))
  (dispatch! :vec-deviation (event "ctx-b" :vec-deviation/fx-start {:mode :start}))
  (dispatch! :vec-deviation (event "ctx-a" :vec-deviation/fx-stop-entity
                                   {:mode :stop-entity :x 1.0 :y 64.0 :z 1.0 :marked? true}))
  (dispatch! :vec-deviation (event "ctx-b" :vec-deviation/fx-stop-entity
                                   {:mode :stop-entity :x 2.0 :y 64.0 :z 2.0 :marked? true}))
    (let [snapshot (vec-deviation-fx/fx-snapshot)]
      (is (:active? (get (:effect-state snapshot) [:ctx "ctx-a"])))
      (is (:active? (get (:effect-state snapshot) [:ctx "ctx-b"])))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-b"]))))
      (vec-deviation-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (vec-deviation-fx/fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (nil? (get (:wave-effects after-clear) [:ctx "ctx-a"])))
        (is (:active? (get (:effect-state after-clear) [:ctx "ctx-b"])))))))

(deftest vec-accel-keeps-preview-state-per-owner-test
  (dispatch! :vec-accel (event "ctx-a" :vec-accel/fx-start {:mode :start}))
  (dispatch! :vec-accel (event "ctx-b" :vec-accel/fx-start {:mode :start}))
  (dispatch! :vec-accel (event "ctx-a" :vec-accel/fx-update
                               {:mode :update :charge-ticks 12 :can-perform? true
                                :look-dir {:x 1.0 :y 0.0 :z 0.0}
                                :init-vel {:x 1.0 :y 0.5 :z 0.0}}))
  (dispatch! :vec-accel (event "ctx-b" :vec-accel/fx-update
                               {:mode :update :charge-ticks 3 :can-perform? false
                                :look-dir {:x 0.0 :y 0.0 :z 1.0}
                                :init-vel {:x 0.0 :y 0.5 :z 1.0}}))
    (let [snapshot (vec-accel-fx/fx-snapshot)]
      (is (= 12 (:charge-ticks (get (:effect-state snapshot) [:ctx "ctx-a"]))))
      (is (= 3 (:charge-ticks (get (:effect-state snapshot) [:ctx "ctx-b"]))))
      (vec-accel-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (vec-accel-fx/fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (some? (get (:effect-state after-clear) [:ctx "ctx-b"]))))))

(deftest storm-wing-keeps-state-per-owner-test
  (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "storm-wing-owner"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (dispatch! :storm-wing (event "ctx-a" :storm-wing/fx-start {:mode :start :charge-ticks 70}))
    (dispatch! :storm-wing (event "ctx-b" :storm-wing/fx-start {:mode :start :charge-ticks 40}))
    (dispatch! :storm-wing (event "ctx-a" :storm-wing/fx-update
                                 {:mode :update :phase :charging :charge-ticks 35 :charge-ratio 0.5}))
    (dispatch! :storm-wing (event "ctx-b" :storm-wing/fx-update
                                 {:mode :update :phase :flying :charge-ticks 40 :charge-ratio 1.0}))
    (let [snapshot (storm-wing-fx/storm-wing-fx-snapshot)]
      (is (= :charging (:phase (get (:effect-state snapshot) [:ctx "ctx-a"]))))
      (is (= :flying (:phase (get (:effect-state snapshot) [:ctx "ctx-b"]))))
      (storm-wing-fx/clear-storm-wing-owner! [:ctx "ctx-a"])
      (let [after-clear (storm-wing-fx/storm-wing-fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (some? (get (:effect-state after-clear) [:ctx "ctx-b"])))))))

(deftest plasma-cannon-keeps-charge-state-per-owner-test
  (with-redefs [client-particles/current-effect-owner (fn [] {:client-session-id "plasma-cannon-owner"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (dispatch! :plasma-cannon (event "ctx-a" :plasma-cannon/fx-start
                                    {:mode :start :charge-pos {:x 1.0 :y 64.0 :z 1.0}}))
    (dispatch! :plasma-cannon (event "ctx-b" :plasma-cannon/fx-start
                                    {:mode :start :charge-pos {:x 2.0 :y 64.0 :z 2.0}}))
    (dispatch! :plasma-cannon (event "ctx-a" :plasma-cannon/fx-update
                                    {:mode :update :charge-ticks 24 :charge-pos {:x 1.0 :y 64.0 :z 1.0}
                                     :flight-ticks 0 :state :charging}))
    (dispatch! :plasma-cannon (event "ctx-b" :plasma-cannon/fx-update
                                    {:mode :update :charge-ticks 8 :charge-pos {:x 2.0 :y 64.0 :z 2.0}
                                     :flight-ticks 2 :state :go}))
    ;; The bodies fade in from alpha 0, so they need to have ticked before they
    ;; contribute any ops.
    (dotimes [_ 40]
      (level-effects/update-effect-state! :plasma-cannon
        (fn [store] (arc-beam/effect-tick-state! :level :plasma-cannon store))))
    (let [snapshot (plasma-cannon-fx/fx-snapshot)
          plan (level-effects/build-level-effect-plan nil nil 0 nil)]
      (is (= 24 (:charge-ticks (get (:effect-state snapshot) [:ctx "ctx-a"]))))
      (is (= :go (:state (get (:effect-state snapshot) [:ctx "ctx-b"]))))
      (is (= 2 (count (filter #(= :plasma-body (:kind %)) (:ops plan))))
          "one plasma body per owner")
      ;; Context termination marks the owner instead of deleting it — upstream's
      ;; two entities outlive the context while they fade — and it is dropped
      ;; once that fade finishes. The other owner is untouched throughout.
      (plasma-cannon-fx/clear-fx-owner! [:ctx "ctx-a"])
      (is (true? (:terminated? (get (:effect-state (plasma-cannon-fx/fx-snapshot)) [:ctx "ctx-a"]))))
      (is (some? (get (:effect-state (plasma-cannon-fx/fx-snapshot)) [:ctx "ctx-b"])))
      (dotimes [_ 40]
        (level-effects/update-effect-state! :plasma-cannon
          (fn [store] (arc-beam/effect-tick-state! :level :plasma-cannon store))))
      (let [after-fade (plasma-cannon-fx/fx-snapshot)]
        (is (nil? (get (:effect-state after-fade) [:ctx "ctx-a"])))
        (is (some? (get (:effect-state after-fade) [:ctx "ctx-b"])))))))

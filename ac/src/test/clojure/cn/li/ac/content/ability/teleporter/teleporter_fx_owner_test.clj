(ns cn.li.ac.content.ability.teleporter.teleporter-fx-owner-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.content.ability.teleporter.flashing-fx :as flashing-fx]
            [cn.li.ac.content.ability.teleporter.flesh-ripping-fx :as flesh-ripping-fx]
            [cn.li.ac.content.ability.teleporter.mark-teleport-fx :as mark-teleport-fx]
            [cn.li.ac.content.ability.teleporter.penetrate-teleport-fx :as penetrate-teleport-fx]
            [cn.li.ac.content.ability.teleporter.shift-teleport-fx :as shift-teleport-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-fixture [f]
  (level-effects/reset-level-effect-registry-for-test!)
      (flashing-fx/reset-fx-for-test!)
      (flesh-ripping-fx/reset-fx-for-test!)
      (mark-teleport-fx/reset-fx-for-test!)
      (penetrate-teleport-fx/reset-fx-for-test!)
      (shift-teleport-fx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (flashing-fx/reset-fx-for-test!)
          (flesh-ripping-fx/reset-fx-for-test!)
          (mark-teleport-fx/reset-fx-for-test!)
          (penetrate-teleport-fx/reset-fx-for-test!)
          (shift-teleport-fx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(defn- enqueue! [effect-id {:keys [payload ctx-id channel owner-key]}]
  (level-effects/enqueue-level-effect! effect-id ctx-id channel payload :owner-key owner-key))

(deftest teleporter-stateful-fx-keep-state-per-owner-test
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-particles/current-effect-owner (fn [] {:client-session-id "teleporter-owner-test"})
                client-sounds/current-effect-owner (fn [] {:client-session-id "teleporter-owner-test"})
                client-sounds/queue-sound-effect! (fn [& _] nil)]
    (flashing-fx/init!)
    (flesh-ripping-fx/init!)
    (mark-teleport-fx/init!)
    (penetrate-teleport-fx/init!)
    (shift-teleport-fx/init!)

    (enqueue! :flashing (event "ctx-a" :flashing/fx-state-start {:mode :state-start}))
    (enqueue! :flashing (event "ctx-b" :flashing/fx-state-start {:mode :state-start}))
    (enqueue! :flashing (event "ctx-a" :flashing/fx-preview-update {:mode :preview-update :to-x 1.0 :to-y 64.0 :to-z 1.0}))
    (enqueue! :flashing (event "ctx-b" :flashing/fx-preview-update {:mode :preview-update :to-x 2.0 :to-y 64.0 :to-z 2.0}))

    (enqueue! :flesh-ripping (event "ctx-a" :flesh-ripping/fx-start {:mode :start}))
    (enqueue! :flesh-ripping (event "ctx-b" :flesh-ripping/fx-start {:mode :start}))
    (enqueue! :flesh-ripping (event "ctx-a" :flesh-ripping/fx-update {:mode :update :target-x 1.0 :target-y 64.0 :target-z 1.0 :hit? false}))
    (enqueue! :flesh-ripping (event "ctx-b" :flesh-ripping/fx-update {:mode :update :target-x 2.0 :target-y 64.0 :target-z 2.0 :hit? true}))

    (enqueue! :mark-teleport (event "ctx-a" :mark-teleport/fx-start {:mode :start}))
    (enqueue! :mark-teleport (event "ctx-b" :mark-teleport/fx-start {:mode :start}))
    (enqueue! :mark-teleport (event "ctx-a" :mark-teleport/fx-update {:mode :update :target {:x 1.0 :y 64.0 :z 1.0} :distance 3.0}))
    (enqueue! :mark-teleport (event "ctx-b" :mark-teleport/fx-update {:mode :update :target {:x 2.0 :y 64.0 :z 2.0} :distance 9.0}))

    (enqueue! :penetrate-teleport (event "ctx-a" :penetrate-teleport/fx-start {:mode :start}))
    (enqueue! :penetrate-teleport (event "ctx-b" :penetrate-teleport/fx-start {:mode :start}))
    (enqueue! :penetrate-teleport (event "ctx-a" :penetrate-teleport/fx-update {:mode :update :x 1.0 :y 64.0 :z 1.0 :available? false :distance 5.0}))
    (enqueue! :penetrate-teleport (event "ctx-b" :penetrate-teleport/fx-update {:mode :update :x 2.0 :y 64.0 :z 2.0 :available? true :distance 7.0}))

    (enqueue! :shift-teleport (event "ctx-a" :shift-teleport/fx-start {:mode :start}))
    (enqueue! :shift-teleport (event "ctx-b" :shift-teleport/fx-start {:mode :start}))
    (enqueue! :shift-teleport (event "ctx-a" :shift-teleport/fx-update {:mode :update :x 1.0 :y 64.0 :z 1.0 :target-count 0 :target-hit? false :hand-valid? true}))
    (enqueue! :shift-teleport (event "ctx-b" :shift-teleport/fx-update {:mode :update :x 2.0 :y 64.0 :z 2.0 :target-count 2 :target-hit? true :hand-valid? true}))

    (is (= 1.0 (get-in (flashing-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"] :preview :x])))
    (is (= 2.0 (get-in (flashing-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"] :preview :x])))
    (is (false? (get-in (flesh-ripping-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"] :hit?])))
    (is (true? (get-in (flesh-ripping-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"] :hit?])))
    (is (= 3.0 (get-in (mark-teleport-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"] :distance])))
    (is (= 9.0 (get-in (mark-teleport-fx/fx-snapshot) [:effect-state [:ctx "ctx-b"] :distance])))
    (is (false? (get-in (penetrate-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"] :available?])))
    (is (true? (get-in (penetrate-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"] :available?])))
    (is (= 0 (get-in (shift-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"] :target-count])))
    (is (= 2 (get-in (shift-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"] :target-count])))

    (flashing-fx/clear-fx-owner! [:ctx "ctx-a"])
    (flesh-ripping-fx/clear-fx-owner! [:ctx "ctx-a"])
    (mark-teleport-fx/clear-fx-owner! [:ctx "ctx-a"])
    (penetrate-teleport-fx/clear-fx-owner! [:ctx "ctx-a"])
    (shift-teleport-fx/clear-fx-owner! [:ctx "ctx-a"])

    (is (nil? (get-in (flashing-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"]])))
    (is (some? (get-in (flashing-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"]])))
    (is (nil? (get-in (flesh-ripping-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"]])))
    (is (some? (get-in (flesh-ripping-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"]])))
    (is (nil? (get-in (mark-teleport-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
    (is (some? (get-in (mark-teleport-fx/fx-snapshot) [:effect-state [:ctx "ctx-b"]])))
    (is (nil? (get-in (penetrate-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"]])))
    (is (some? (get-in (penetrate-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"]])))
    (is (nil? (get-in (shift-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"]])))
    (is (some? (get-in (shift-teleport-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"]])))))

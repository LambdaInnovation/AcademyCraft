(ns cn.li.ac.content.ability.electromaster.electromaster-fx-owner-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.content.ability.electromaster.mag-manip-fx :as mag-manip-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- invoke-mag-enqueue! [ctx-id channel payload]
  (arc-beam/enqueue-for-test! :mag-manip ctx-id channel payload {:runtime :hand}))

(defn- invoke-charging-enqueue! [ctx-id channel payload]
  ;; CurrentCharging is a world-space beam/surround effect and therefore
  ;; owns one :level track; do not route this ownership test through the
  ;; obsolete hand track.
  (arc-beam/enqueue-for-test! :current-charging ctx-id channel payload))

(defn- reset-fixture [f]
  (try
    (hand-effects/reset-hand-effect-registry-for-test!)
    (mag-manip-fx/reset-fx-for-test!)
    (current-charging-fx/reset-fx-for-test!)
    (mag-manip-fx/init!)
    (current-charging-fx/init!)
    (f)
    (finally
      (mag-manip-fx/reset-fx-for-test!)
      (current-charging-fx/reset-fx-for-test!)
      (hand-effects/reset-hand-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(deftest electromaster-fx-keep-state-per-owner-test
  (with-redefs [client-sounds/current-effect-owner (fn [] {:client-session-id "electromaster-owner-test"})
                client-sounds/queue-current-sound-effect! (fn [& _] nil)
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-bridge/game-time-ms (constantly 1000)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (invoke-mag-enqueue! "ctx-a" :mag-manip/fx-hold {:mode :hold-start :block-id "minecraft:iron_block"})
    (invoke-mag-enqueue! "ctx-b" :mag-manip/fx-hold {:mode :hold-start :block-id "minecraft:gold_block"})
    (invoke-mag-enqueue! "ctx-a" :mag-manip/fx-hold {:mode :hold-loop :block-id "minecraft:copper_block"})

    (invoke-charging-enqueue! "ctx-a" :current-charging/fx-start {:mode :start :is-item false})
    (invoke-charging-enqueue! "ctx-b" :current-charging/fx-start {:mode :start :is-item true})
    (invoke-charging-enqueue! "ctx-a" :current-charging/fx-update {:mode :update :charge-ticks 12 :good? true})
    (invoke-charging-enqueue! "ctx-b" :current-charging/fx-update {:mode :update :charge-ticks 30 :good? false})

    (let [mag-snapshot (mag-manip-fx/fx-snapshot)
          charging-snapshot (current-charging-fx/fx-snapshot)]
      (is (= "minecraft:copper_block" (get-in mag-snapshot [:states [:ctx "ctx-a"] :block-id])))
      (is (= "minecraft:gold_block" (get-in mag-snapshot [:states [:ctx "ctx-b"] :block-id])))
      (is (= 12 (get-in charging-snapshot [:states [:ctx "ctx-a"] :charge-ticks])))
      (is (= 30 (get-in charging-snapshot [:states [:ctx "ctx-b"] :charge-ticks]))))

    (mag-manip-fx/clear-fx-owner! [:ctx "ctx-a"])
    (current-charging-fx/clear-fx-owner! [:ctx "ctx-a"])

    (let [mag-snapshot (mag-manip-fx/fx-snapshot)
          charging-snapshot (current-charging-fx/fx-snapshot)]
      (is (nil? (get-in mag-snapshot [:states [:ctx "ctx-a"]])))
      (is (some? (get-in mag-snapshot [:states [:ctx "ctx-b"]])))
      (is (nil? (get-in charging-snapshot [:states [:ctx "ctx-a"]])))
      (is (some? (get-in charging-snapshot [:states [:ctx "ctx-b"]]))))))

(ns cn.li.ac.content.ability.vecmanip.groundshock-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.content.ability.vecmanip.groundshock-fx :as gfx]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- invoke-hand-enqueue! [ctx-id channel payload]
  (arc-beam/enqueue-for-test! :groundshock ctx-id channel payload))

(defn- reset-fixture [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session} (fn [] (hand-effects/reset-hand-effect-registry-for-test!)
    (gfx/reset-fx-for-test!)
    (gfx/init!)
    (try
      (f)
      (finally
        (hand-effects/reset-hand-effect-registry-for-test!)
        (gfx/reset-fx-for-test!))))))

(use-fixtures :each reset-fixture)

(deftest init-registers-groundshock-fx-channels-test
  (let [registered-level (atom nil)
        registered-hand (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level [effect-id effect-map])
                                                         nil)
                  hand-effects/register-hand-effect! (fn [effect-id effect-map]
                                                       (reset! registered-hand [effect-id effect-map])
                                                       nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (gfx/init!)
      (is (= :groundshock (first @registered-level)))
      (is (= :groundshock (first @registered-hand)))
      (is (= #{:groundshock/fx-start
               :groundshock/fx-update
               :groundshock/fx-perform
               :groundshock/fx-end}
             @registered-topics*)))))

(deftest fx-handler-routes-start-update-perform-end-payloads-test
  (let [handlers* (atom {})
        hand-enqueued* (atom [])
        level-enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  hand-effects/register-hand-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  hand-effects/enqueue-hand-effect! (fn [effect-id ctx-id channel payload & opts]
                                                      (swap! hand-enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! level-enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                        nil)]
      (gfx/init!)
      ((get @handlers* :groundshock/fx-start) "ctx-1" :groundshock/fx-start nil)
      ((get @handlers* :groundshock/fx-update) "ctx-1" :groundshock/fx-update {:charge-ticks 7})
      ((get @handlers* :groundshock/fx-perform) "ctx-1" :groundshock/fx-perform {:affected-blocks [{:x 1 :y 2 :z 3 :block-id "minecraft:stone"}]
                                                   :broken-blocks [{:x 1 :y 2 :z 3}]})
      ((get @handlers* :groundshock/fx-end) "ctx-1" :groundshock/fx-end {:performed? false})

      (is (= [[:groundshock "ctx-1" :groundshock/fx-start {:mode :start} :owner-key [:ctx "ctx-1"]]
              [:groundshock "ctx-1" :groundshock/fx-update {:mode :update :charge-ticks 7} :owner-key [:ctx "ctx-1"]]
              [:groundshock "ctx-1" :groundshock/fx-perform {:mode :perform} :owner-key [:ctx "ctx-1"]]
              [:groundshock "ctx-1" :groundshock/fx-end {:mode :end :performed? false} :owner-key [:ctx "ctx-1"]]]
             @hand-enqueued*))
      (is (= [[:groundshock "ctx-1" :groundshock/fx-perform
               {:mode :perform
                :affected-blocks [{:x 1 :y 2 :z 3 :block-id "minecraft:stone"}]
                :broken-blocks [{:x 1 :y 2 :z 3}]}
               :owner-key [:ctx "ctx-1"]]]
             @level-enqueued*)))))

(deftest two-owners-keep-groundshock-hand-state-independent-test
  (let [pitch-deltas* (atom [])]
    (with-redefs [hand-effects/add-camera-pitch-delta! (fn
                                                         ([delta]
                                                          (swap! pitch-deltas* conj delta)
                                                          nil)
                                                         ([_owner delta]
                                                          (swap! pitch-deltas* conj delta)
                                                          nil))]
      (invoke-hand-enqueue! "ctx-a" :groundshock/fx-start {:mode :start})
      (invoke-hand-enqueue! "ctx-b" :groundshock/fx-start {:mode :start})
      (invoke-hand-enqueue! "ctx-a" :groundshock/fx-update {:mode :update :charge-ticks 2})
      (invoke-hand-enqueue! "ctx-b" :groundshock/fx-update {:mode :update :charge-ticks 4})
      (let [snapshot (gfx/fx-snapshot)]
        (is (= 2 (:charge-ticks (get (:hand-state snapshot) [:ctx "ctx-a"]))))
        (is (= 4 (:charge-ticks (get (:hand-state snapshot) [:ctx "ctx-b"]))))
        (is (= 6 (count @pitch-deltas*))))
      (invoke-hand-enqueue! "ctx-a" :groundshock/fx-perform {:mode :perform})
      (invoke-hand-enqueue! "ctx-b" :groundshock/fx-end {:mode :end :performed? false})
      (hand-effects/update-effect-state! :groundshock
        (fn [store] (arc-beam/effect-tick-state! :hand :groundshock store)))
      (let [snapshot (gfx/fx-snapshot)]
        (is (= 3 (:perform-ticks (get (:hand-state snapshot) [:ctx "ctx-a"]))))
        (is (nil? (get (:hand-state snapshot) [:ctx "ctx-b"]))))
      (gfx/clear-fx-owner! [:ctx "ctx-a"])
      (is (empty? (:hand-state (gfx/fx-snapshot)))))))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:hand-state {}}
         (gfx/fx-snapshot))))

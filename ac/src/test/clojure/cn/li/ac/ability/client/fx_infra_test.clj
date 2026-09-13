(ns cn.li.ac.ability.client.fx-infra-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.particles :as particles]
            [cn.li.ac.ability.client.effects.sounds :as sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- reset-fixture [f]
  (fx-registry/reset-fx-registry-for-test!)
  (level-effects/reset-level-effect-registry-for-test!)
  (hand-effects/reset-hand-effect-registry-for-test!)
  (particles/reset-particle-queue-for-test!)
  (sounds/reset-sound-queue-for-test!)
  (try
    (f)
    (finally
      (fx-registry/reset-fx-registry-for-test!)
      (level-effects/reset-level-effect-registry-for-test!)
      (hand-effects/reset-hand-effect-registry-for-test!)
      (particles/reset-particle-queue-for-test!)
      (sounds/reset-sound-queue-for-test!))))

(use-fixtures :each reset-fixture)

(defn- level-handler []
  {:enqueue-state-fn (fn [state _ _ _ _] state)
   :tick-state-fn (fn [state] state)
   :build-plan-fn (fn [_ _ _ _] nil)})

(defn- hand-handler []
  {:enqueue-state-fn (fn [state _ _ _ _] state)
   :tick-state-fn (fn [state] state)
   :transform-fn (fn [] nil)})

(deftest fx-channel-registry-freezes-after-idempotent-registration-test
  (let [calls (atom [])
        handler (fn [ctx-id channel payload]
                  (swap! calls conj [ctx-id channel payload]))]
    (fx-registry/register-fx-channel! :test/fx handler)
    (fx-registry/register-fx-channel! :test/fx (fn [& _] :ignored))
    (is (= #{:test/fx} (fx-registry/registered-channels)))
    (is (true? (fx-registry/dispatch-fx-channel! "ctx-1" :test/fx {:v 1})))
    (is (= [["ctx-1" :test/fx {:v 1}]] @calls))
    (fx-registry/freeze-fx-registry!)
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #"FX channel registry is frozen"
                          (fx-registry/register-fx-channel! :test/other handler)))))

(deftest level-and-hand-effect-registries-freeze-test
  (level-effects/register-level-effect! :test/level (level-handler))
  (level-effects/register-level-effect! :test/level (level-handler))
  (is (= [:test/level] (:order (level-effects/level-effect-registry-snapshot))))
  (level-effects/freeze-level-effect-registry!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Level effect registry is frozen"
                        (level-effects/register-level-effect! :test/level-2 (level-handler))))
  (hand-effects/register-hand-effect! :test/hand (hand-handler))
  (hand-effects/register-hand-effect! :test/hand (hand-handler))
  (is (= [:test/hand] (:order (hand-effects/hand-effect-registry-snapshot))))
  (hand-effects/freeze-hand-effect-registry!)
  (is (thrown-with-msg? clojure.lang.ExceptionInfo
                        #"Hand effect registry is frozen"
                        (hand-effects/register-hand-effect! :test/hand-2 (hand-handler)))))

(deftest level-effect-enqueue-carries-owner-metadata-test
  (let [events* (atom [])]
    (level-effects/register-level-effect!
      :test/level-event
      {:enqueue-state-fn (fn [state ctx-id channel owner-key payload]
                           (swap! events* conj {:effect-id :test/level-event
                                                :payload payload
                                                :ctx-id ctx-id
                                                :channel channel
                                                :owner-key owner-key})
                           state)
       :tick-state-fn (fn [state] state)
       :build-plan-fn (fn [_ _ _ _] nil)})
    (level-effects/enqueue-level-effect! :test/level-event "ctx-1" :test/fx {:v 1})
    (is (= [{:effect-id :test/level-event
             :payload {:v 1}
             :ctx-id "ctx-1"
             :channel :test/fx
             :owner-key [:ctx "ctx-1"]}]
           @events*))))

(deftest client-effect-queues-are-session-scoped-and-resettable-test
  (runtime-hooks/with-client-ctx-fn {:session-id :session-a} (fn [] (particles/queue-particle-effect! {:type :particle :particle-type :spark-a})
    (sounds/queue-sound-effect! {:type :sound :sound-id "minecraft:test-a"})
    (hand-effects/add-camera-pitch-delta! 1.25)))
  (runtime-hooks/with-client-ctx-fn {:session-id :session-b} (fn [] (particles/queue-particle-effect! {:type :particle :particle-type :spark-b})
    (sounds/queue-sound-effect! {:type :sound :sound-id "minecraft:test-b"})
    (hand-effects/add-camera-pitch-delta! 2.5)))
  (is (= [{:type :particle :particle-type :spark-a}]
         (particles/particle-queue-snapshot :session-a)))
  (is (= [{:type :sound :sound-id "minecraft:test-a"}]
         (sounds/sound-queue-snapshot :session-a)))
  (is (= [[:session-a 1.25] [:session-b 2.5]]
         (sort-by first (:camera-pitch-deltas (hand-effects/hand-effect-registry-snapshot)))))
  (is (= [{:type :particle :particle-type :spark-b}]
         (particles/poll-particle-effects! {:client-session-id :session-b})))
  (is (= [{:type :sound :sound-id "minecraft:test-b"}]
         (sounds/poll-sound-effects! {:client-session-id :session-b})))
  (is (= [2.5]
         (hand-effects/drain-camera-pitch-deltas! {:client-session-id :session-b})))
  (is (= [{:type :particle :particle-type :spark-a}]
         (particles/particle-queue-snapshot :session-a)))
  (is (= [{:type :sound :sound-id "minecraft:test-a"}]
         (sounds/sound-queue-snapshot :session-a)))
  (particles/reset-particle-queue-for-test!)
  (sounds/reset-sound-queue-for-test!)
  (hand-effects/reset-hand-effect-registry-for-test!)
  (is (empty? (particles/particle-queue-snapshot)))
  (is (empty? (sounds/sound-queue-snapshot)))
  (is (empty? (:camera-pitch-deltas (hand-effects/hand-effect-registry-snapshot)))))

(deftest client-effect-queues-require-client-session-test
  (runtime-hooks/with-client-ctx-fn {:session-id nil :player-owner nil} (fn [] (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #":client-session-id"
                          (particles/queue-particle-effect! {:type :particle :particle-type :spark})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #":client-session-id"
                          (sounds/queue-sound-effect! {:type :sound :sound-id "minecraft:test"})))
    (is (thrown-with-msg? clojure.lang.ExceptionInfo
                          #":client-session-id"
                          (hand-effects/add-camera-pitch-delta! 1.0))))))

(ns cn.li.ac.content.ability.vecmanip.blood-retrograde-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.content.ability.vecmanip.blood-retrograde-fx :as brfx]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (brfx/reset-fx-for-test!)
        (f)
        (finally
          (brfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :blood-retrograde/fx-perform
   :owner-key [:ctx ctx-id]})

(defn- apply-event!
  [ctx-id payload]
  (let [channel (case (:mode payload)
                  :start :blood-retrograde/fx-start
                  :update :blood-retrograde/fx-update
                  :perform :blood-retrograde/fx-perform
                  :end :blood-retrograde/fx-end
                  :blood-retrograde/fx-update)]
    (arc-beam/enqueue-for-test! :blood-retrograde ctx-id channel payload)))

(deftest init-registers-blood-retrograde-fx-channels-test
  (let [registered-effect (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-effect [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (brfx/init!)
      (is (= :blood-retrograde (first @registered-effect)))
      (is (= #{:blood-retrograde/fx-start
               :blood-retrograde/fx-update
               :blood-retrograde/fx-end
               :blood-retrograde/fx-perform}
             @registered-topics*)))))

(deftest fx-handler-routes-start-update-perform-end-test
  (let [handlers* (atom {})
        enqueue-fn* (atom nil)
        enqueued-effects* (atom [])
        sound-calls* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [_effect-id effect-map]
                                                         (reset! enqueue-fn* (:enqueue-state-fn effect-map))
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued-effects* conj [effect-id ctx-id channel payload opts])
                                                        (level-effects/update-effect-state! effect-id
                                                          @enqueue-fn*
                                                          ctx-id channel [:ctx ctx-id] payload)
                                                        nil)
                  client-sounds/queue-current-sound-effect! (fn [payload]
                                                              (swap! sound-calls* conj payload)
                                                              nil)]
      (brfx/init!)
      ((get @handlers* :blood-retrograde/fx-start) "ctx-1" :blood-retrograde/fx-start nil)
      ((get @handlers* :blood-retrograde/fx-update) "ctx-1" :blood-retrograde/fx-update {:ticks 7 :charge-ratio 0.35})
      ((get @handlers* :blood-retrograde/fx-perform) "ctx-1" :blood-retrograde/fx-perform {:sound-pos {:x 1.0 :y 2.0 :z 3.0}
                                                       :splashes [{:x 1.0 :y 2.0 :z 3.0 :size 1.4}]
                                                       :sprays [{:x 4.0 :y 5.0 :z 6.0 :face :up :size 1.2 :rotation 0.0
                                                                :offset-u 0.0 :offset-v 0.0 :texture-id 1}]})
      ((get @handlers* :blood-retrograde/fx-end) "ctx-1" :blood-retrograde/fx-end {:performed? true})
      (is (= [[:blood-retrograde "ctx-1" :blood-retrograde/fx-start
               {:mode :start}
               [:owner-key [:ctx "ctx-1"]]]
              [:blood-retrograde "ctx-1" :blood-retrograde/fx-update
               {:mode :update :ticks 7 :charge-ratio 0.35}
               [:owner-key [:ctx "ctx-1"]]]
              [:blood-retrograde "ctx-1" :blood-retrograde/fx-perform
               {:mode :perform
                :sound-pos {:x 1.0 :y 2.0 :z 3.0}
                :splashes [{:x 1.0 :y 2.0 :z 3.0 :size 1.4}]
                :sprays [{:x 4.0 :y 5.0 :z 6.0 :face :up :size 1.2 :rotation 0.0
                          :offset-u 0.0 :offset-v 0.0 :texture-id 1}]}
               [:owner-key [:ctx "ctx-1"]]]
              [:blood-retrograde "ctx-1" :blood-retrograde/fx-end
               {:mode :end :performed? true}
               [:owner-key [:ctx "ctx-1"]]]]
             @enqueued-effects*))
      (is (= 1 (count @sound-calls*)))
      (is (= "academy:vecmanip.blood_retro" (:sound-id (first @sound-calls*)))))))

(defn- approx= [expected actual]
  (< (Math/abs (- (double expected) (double actual))) 1.0e-6))

(deftest walk-speed-curve-and-tick-cleanup-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [_] nil)]
      (apply-event! "ctx-main" {:mode :start})
      (is (approx= 0.1 (:local-walk-speed (arc-beam/effect-build-plan :blood-retrograde {:x 0.0 :y 0.0 :z 0.0}
                                                      {:x 0.0 :y 0.0 :z 0.0}
                                                      0))))
      (apply-event! "ctx-main" {:mode :update :ticks 10 :charge-ratio 0.5})
      (is (approx= 0.0535 (:local-walk-speed (arc-beam/effect-build-plan :blood-retrograde {:x 0.0 :y 0.0 :z 0.0}
                                                         {:x 0.0 :y 0.0 :z 0.0}
                                                         10))))
      (apply-event! "ctx-main" {:mode :update :ticks 20 :charge-ratio 1.0})
      (is (approx= 0.007 (:local-walk-speed (arc-beam/effect-build-plan :blood-retrograde {:x 0.0 :y 0.0 :z 0.0}
                                                        {:x 0.0 :y 0.0 :z 0.0}
                                                        20))))
      (apply-event! "ctx-main"
                    {:mode :perform
                     :sound-pos {:x 0.0 :y 0.0 :z 0.0}
                     :splashes [{:x 1.0 :y 2.0 :z 3.0 :size 1.0}]
                     :sprays [{:x 4.0 :y 5.0 :z 6.0 :face :up :size 1.0 :rotation 0.0
                               :offset-u 0.0 :offset-v 0.0 :texture-id 1}]})
      (level-effects/update-effect-state! :blood-retrograde
        (fn [store] (arc-beam/effect-tick-state! :level :blood-retrograde store)))
      (is (= 21 (:ticks (get (:effect-state (brfx/fx-snapshot)) [:ctx "ctx-main"]))))
      (dotimes [_ 9]
        (level-effects/update-effect-state! :blood-retrograde
          (fn [store] (arc-beam/effect-tick-state! :level :blood-retrograde store))))
      (let [snapshot (brfx/fx-snapshot)]
        (is (nil? (get (:splashes snapshot) [:ctx "ctx-main"])))
        (is (= 1 (count (get (:sprays snapshot) [:ctx "ctx-main"])))))))

(deftest two-owners-keep-blood-retrograde-state-and-queues-independent-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [_] nil)]
    (apply-event! "ctx-a" {:mode :start})
    (apply-event! "ctx-b" {:mode :start})
    (apply-event! "ctx-a" {:mode :update :ticks 5 :charge-ratio 0.25})
    (apply-event! "ctx-b" {:mode :update :ticks 15 :charge-ratio 0.75})
    (apply-event! "ctx-a"
                  {:mode :perform
                   :sound-pos {:x 1.0 :y 2.0 :z 3.0}
                   :splashes [{:x 1.0 :y 2.0 :z 3.0 :size 1.0}]
                   :sprays [{:x 4.0 :y 5.0 :z 6.0 :face :up :size 1.0}]})
    (apply-event! "ctx-b"
                  {:mode :perform
                   :sound-pos {:x 2.0 :y 3.0 :z 4.0}
                   :splashes [{:x 2.0 :y 3.0 :z 4.0 :size 1.0}]
                   :sprays [{:x 5.0 :y 6.0 :z 7.0 :face :north :size 1.0}]})
    (let [snapshot (brfx/fx-snapshot)]
      (is (= 5 (:ticks (get (:effect-state snapshot) [:ctx "ctx-a"]))))
      (is (= 15 (:ticks (get (:effect-state snapshot) [:ctx "ctx-b"]))))
      (is (= 1 (count (get (:splashes snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:sprays snapshot) [:ctx "ctx-b"]))))
      (brfx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (brfx/fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        ;; Splashes and sprays are spawned world entities upstream — the
        ;; channel ending does not kill them, they expire on their own ttl.
        (is (= 1 (count (get (:splashes after-clear) [:ctx "ctx-a"]))))
        (is (= 1 (count (get (:sprays after-clear) [:ctx "ctx-b"]))))))))



(ns cn.li.ac.content.ability.vecmanip.vec-accel-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.vecmanip.vec-accel-fx :as vafx]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (vafx/reset-fx-for-test!)
        (f)
        (finally
          (vafx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :vec-accel/fx-update
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-vec-accel-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (vafx/init!)
      (is (= :vec-accel (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:vec-accel/fx-start
               :vec-accel/fx-update
               :vec-accel/fx-perform
               :vec-accel/fx-end}
             @registered-topics*)))))

(deftest update-keeps-preview-state-per-owner-test
  (do
    (arc-beam/enqueue-for-test! :vec-accel "ctx-a" :vec-accel/fx-update {:mode :start :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :vec-accel "ctx-b" :vec-accel/fx-update {:mode :start :source-player-id "player-b"})
    (arc-beam/enqueue-for-test! :vec-accel "ctx-a" :vec-accel/fx-update {:mode :update
                       :charge-ticks 12
                       :can-perform? true
                       :look-dir {:x 1.0 :y 0.0 :z 0.0}
                       :init-vel {:x 1.0 :y 0.5 :z 0.0}
                       :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :vec-accel "ctx-b" :vec-accel/fx-update {:mode :update
                       :charge-ticks 3
                       :can-perform? false
                       :look-dir {:x 0.0 :y 0.0 :z 1.0}
                       :init-vel {:x 0.0 :y 0.5 :z 1.0}
                       :source-player-id "player-b"})
    (let [snapshot (vafx/fx-snapshot)]
      (is (= 12 (:charge-ticks (get (:effect-state snapshot) [:ctx "ctx-a"]))))
      (is (= 3 (:charge-ticks (get (:effect-state snapshot) [:ctx "ctx-b"]))))
      (vafx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (vafx/fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (some? (get (:effect-state after-clear) [:ctx "ctx-b"])))))))

(deftest init-handler-routes-update-through-level-effects-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)
                  client-sounds/queue-current-sound-effect! (fn [& _] nil)]
      (vafx/init!)
      ((get @handlers* :vec-accel/fx-update) "ctx-1" :vec-accel/fx-update
       {:source-player-id "player-a"
        :charge-ticks 9
        :can-perform? true
        :look-dir {:x 1.0 :y 0.0 :z 0.0}
        :init-vel {:x 0.0 :y 0.5 :z 1.0}})
      (is (= [[:vec-accel "ctx-1" :vec-accel/fx-update
               {:source-player-id "player-a"
                :mode :update
                :charge-ticks 9
                :can-perform? true
                :look-dir {:x 1.0 :y 0.0 :z 0.0}
                :init-vel {:x 0.0 :y 0.5 :z 1.0}}
               [:owner-key [:ctx "ctx-1"]]]]
             @enqueued*)))))



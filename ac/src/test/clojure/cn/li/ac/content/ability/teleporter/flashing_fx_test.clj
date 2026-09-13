(ns cn.li.ac.content.ability.teleporter.flashing-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.content.ability.teleporter.flashing-fx :as ffx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- with-fresh-flashing-fx-runtime [f]
  (level-effects/reset-level-effect-registry-for-test!)
  (ffx/reset-fx-for-test!)
      (try
        (f)
        (finally
          (ffx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each with-fresh-flashing-fx-runtime)

(deftest init-registers-flashing-fx-channels-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (ffx/init!)
      (is (= :flashing (first @registered-level*)))
      (is (= #{:flashing/fx-state-start
               :flashing/fx-preview-start
               :flashing/fx-preview-update
               :flashing/fx-preview-end
               :flashing/fx-perform
               :flashing/fx-state-end}
             @registered-topics*)))))

(deftest fx-handler-routes-preview-and-perform-events-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                        nil)]
      (ffx/init!)
      ((get @handlers* :flashing/fx-state-start) "ctx-1" :flashing/fx-state-start nil)
      ((get @handlers* :flashing/fx-preview-start) "ctx-1" :flashing/fx-preview-start
       {:to-x 1.0 :to-y 64.0 :to-z 2.0 :direction :forward :distance 12.0})
      ((get @handlers* :flashing/fx-preview-update) "ctx-1" :flashing/fx-preview-update
       {:to-x 2.0 :to-y 64.0 :to-z 3.0 :direction :forward :distance 12.0})
      ((get @handlers* :flashing/fx-perform) "ctx-1" :flashing/fx-perform {:from-x 0.0 :from-y 64.0 :from-z 0.0
                                                :to-x 2.0 :to-y 64.0 :to-z 3.0
                                                :direction :forward :distance 12.0})
      ((get @handlers* :flashing/fx-preview-end) "ctx-1" :flashing/fx-preview-end nil)
      ((get @handlers* :flashing/fx-state-end) "ctx-1" :flashing/fx-state-end nil)
      (is (= [[:flashing "ctx-1" :flashing/fx-state-start {:mode :state-start} :owner-key [:ctx "ctx-1"]]
              [:flashing "ctx-1" :flashing/fx-preview-start {:mode :preview-start :to-x 1.0 :to-y 64.0 :to-z 2.0 :direction :forward :distance 12.0} :owner-key [:ctx "ctx-1"]]
              [:flashing "ctx-1" :flashing/fx-preview-update {:mode :preview-update :to-x 2.0 :to-y 64.0 :to-z 3.0 :direction :forward :distance 12.0} :owner-key [:ctx "ctx-1"]]
              [:flashing "ctx-1" :flashing/fx-perform {:mode :perform :from-x 0.0 :from-y 64.0 :from-z 0.0 :to-x 2.0 :to-y 64.0 :to-z 3.0
                :direction :forward :distance 12.0} :owner-key [:ctx "ctx-1"]]
              [:flashing "ctx-1" :flashing/fx-preview-end {:mode :preview-end} :owner-key [:ctx "ctx-1"]]
              [:flashing "ctx-1" :flashing/fx-state-end {:mode :state-end} :owner-key [:ctx "ctx-1"]]]
             @enqueued*)))))

(deftest enqueue-perform-emits-sound-and-particles-test
  (let [particles* (atom [])
        sounds* (atom [])]
    (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                  client-particles/current-effect-owner (fn [] {:client-session-id "flashing-test"})
                  client-particles/queue-particle-effect! (fn [& args]
                                                            (swap! particles* conj args)
                                                            nil)
                  client-sounds/queue-sound-effect! (fn [& args]
                                                      (swap! sounds* conj args)
                                                      nil)]
      (ffx/init!)
      (level-effects/enqueue-level-effect! :flashing "ctx-1" :flashing/fx-state-start {:mode :state-start}
                                         :owner-key [:ctx "ctx-1"])
      (level-effects/enqueue-level-effect! :flashing "ctx-1" :flashing/fx-preview-update {:mode :preview-update :to-x 1.0 :to-y 64.0 :to-z 1.0}
                                         :owner-key [:ctx "ctx-1"])
      (level-effects/enqueue-level-effect! :flashing "ctx-1" :flashing/fx-perform {:mode :perform :from-x 0.0 :from-y 64.0 :from-z 0.0 :to-x 2.0 :to-y 64.0 :to-z 2.0}
                                         :owner-key [:ctx "ctx-1"])
      (level-effects/tick-level-effects!)
      (is (= 1 (count @sounds*)))
      (is (>= (count @particles*) 2))
      (is (= "academy:tp.tp_flashing" (:sound-id (second (first @sounds*))))))))



(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:fx-state {}}
         (ffx/fx-snapshot))))

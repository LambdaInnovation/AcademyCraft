(ns cn.li.ac.content.ability.teleporter.teleporter-crit-fx-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.teleporter.teleporter-crit-fx :as crit-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest init-registers-teleporter-crit-channel-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (crit-fx/init!)
      (is (= :teleporter-crit (first @registered-level*)))
      (is (= #{:teleporter/fx-crit-hit}
             @registered-topics*)))))

(deftest fx-handler-routes-crit-payload-test
  (let [handlers* (atom {})
        enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! enqueued* conj [effect-id ctx-id channel payload opts])
                                                        nil)]
      (crit-fx/init!)
      ((get @handlers* :teleporter/fx-crit-hit) "ctx-1" :teleporter/fx-crit-hit {:x 1.0 :y 2.0 :z 3.0 :crit-level 2 :crit-rate 2.6 :message-key "ability.teleporter.critical_hit" :message-args ["x2.6"] :target-uuid "t" :skill-id :flesh-ripping})
      (is (= [[:teleporter-crit
               "ctx-1"
               :teleporter/fx-crit-hit
               {:mode :crit-hit
                :x 1.0
                :y 2.0
                :z 3.0
                :crit-level 2
                :crit-rate 2.6
                :message-key "ability.teleporter.critical_hit"
                :message-args ["x2.6"]
                :target-uuid "t"
                :skill-id :flesh-ripping}
               [:owner-key [:ctx "ctx-1"]]]]
             @enqueued*)))))

(deftest context-termination-does-not-wipe-crit-burst-test
  ;; MSG-CTX-TERMINATED arrives right after up! — clear-effect-owner! must
  ;; not erase the just-enqueued burst (upstream particles live out their
  ;; life after the event instead of dying with the context).
  (level-effects/reset-level-effect-registry-for-test!)
  (try
    (with-redefs [client-bridge/run-client-effect!
                  (fn [effect-key _payload]
                    (when (= effect-key :mcmod/get-entity-position)
                      {"x" 20.0 "y" 64.0 "z" 30.0
                       "width" 1.0 "height" 2.0}))
                  runtime-hooks/client-show-combat-notice! (fn [& _] nil)]
      (crit-fx/init!)
      (level-effects/enqueue-level-effect! :teleporter-crit "ctx-1" :teleporter/fx-crit-hit
                                           {:mode :crit-hit
                                            :x 1.0 :y 2.0 :z 3.0
                                            :target-uuid "t"}
                                           :owner-key [:ctx "ctx-1"])
      (level-effects/clear-effect-owner! [:ctx "ctx-1"])
      (let [{:keys [ops]} (arc-beam/effect-build-plan
                           :teleporter-crit {:x 0.0 :y 0.0 :z 0.0}
                           {:player-uuid "viewer"} 0 nil)]
        (is (<= 5 (count ops) 8))))
    (finally
      (level-effects/reset-level-effect-registry-for-test!))))

(deftest enqueue-crit-hit-spawns-formula-burst-and-notice-test
  (level-effects/reset-level-effect-registry-for-test!)
  (try
    (let [notices* (atom [])]
      (with-redefs [client-bridge/run-client-effect!
                    (fn [effect-key _payload]
                      (when (= effect-key :mcmod/get-entity-position)
                        ;; McClientAccess.clientEntitySnapshot returns String-keyed maps.
                        {"x" 20.0 "y" 64.0 "z" 30.0
                         "width" 1.0 "height" 2.0}))
                    runtime-hooks/client-show-combat-notice! (fn [notice-id payload]
                                                               (swap! notices* conj [notice-id payload]))]
        (crit-fx/init!)
        (level-effects/enqueue-level-effect! :teleporter-crit "ctx-1" :teleporter/fx-crit-hit
                                             {:mode :crit-hit
                                              :x 1.0 :y 2.0 :z 3.0
                                              :crit-level 2 :crit-rate 2.6
                                              :message-key "ability.teleporter.critical_hit"
                                              :message-args ["x2.6"]
                                              :target-uuid "t" :skill-id :flesh-ripping}
                                             :owner-key [:ctx "ctx-1"])
        ;; Notice = upstream chat message (no sound, no vanilla particles).
        (is (= [[:teleporter-crit {:message-key "ability.teleporter.critical_hit"
                                   :args ["x2.6"]
                                   :duration-ms 1500
                                   :color [255 226 120]}]]
               @notices*))
        (let [{:keys [ops]} (arc-beam/effect-build-plan
                             :teleporter-crit {:x 0.0 :y 0.0 :z 0.0}
                             {:player-uuid "viewer"} 0 nil)]
          ;; Upstream CriticalHitEffect: 5-8 formula particles, each a textured
          ;; billboard quad, hugging the live entity box (1.0 x 2.0 at 20/64/30).
          (is (<= 5 (count ops) 8))
          (is (every? #(= :quad (:kind %)) ops))
          (is (every? #(re-find #"^academy:textures/effects/formula/\d+\.png$" (:texture %)) ops))
          (let [ps (map (fn [op]
                          (let [^cn.li.mcmod.math.V3 p1 (:p1 op)]
                            [(.x p1) (.y p1) (.z p1)]))
                        ops)]
            ;; Corner = center + billboard half-extent (up to size/2 * sqrt2).
            (is (every? (fn [[x y z]]
                          (and (<= 18.0 x 22.0)
                               (<= 62.7 y 67.3)
                               (<= 28.0 z 32.0)))
                        ps))))))
    (finally
      (level-effects/reset-level-effect-registry-for-test!))))

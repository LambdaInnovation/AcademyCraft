(ns cn.li.ac.content.ability.vecmanip.vec-reflection-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.vecmanip.vec-reflection-fx :as vrfx]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (vrfx/reset-vec-reflection-fx-for-test!)
        (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)]
          (f))
        (finally
          (vrfx/reset-vec-reflection-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :vec-reflection/fx-reflect-entity
   :owner-key [:ctx ctx-id]})

(defn- update-from-event!
  [enqueue-state! {:keys [ctx-id channel owner-key payload]}]
  (level-effects/update-effect-state!
    :vec-reflection enqueue-state! ctx-id channel owner-key payload))

(deftest init-registers-owner-aware-vec-reflection-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (vrfx/init!)
      (is (= :vec-reflection (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:vec-reflection/fx-start
               :vec-reflection/fx-end
               :vec-reflection/fx-reflect-entity
               :vec-reflection/fx-play}
             @registered-topics*)))))

(deftest enqueue-reflect-entity-requires-reflected-flag-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.vec-reflection-fx/enqueue-state!)]
    (is (= (vrfx/default-vec-reflection-fx-runtime-state)
           (vrfx/vec-reflection-fx-snapshot)))
    (update-from-event!
      enqueue-state!
      (event "ctx-main" {:mode :reflect-entity :x 1.0 :y 2.0 :z 3.0 :reflected? false}))
    (is (empty? (:wave-effects (vrfx/vec-reflection-fx-snapshot))))
    (update-from-event!
      enqueue-state!
      (event "ctx-main" {:mode :reflect-entity :x 1.0 :y 2.0 :z 3.0 :reflected? true}))
    (let [waves (get (:wave-effects (vrfx/vec-reflection-fx-snapshot)) [:ctx "ctx-main"])]
      (is (= 1 (count waves)))
      (is (= 3.0 (:z (first waves)))))))

(deftest projectile-and-damage-reflection-both-play-sound-and-wave-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.vec-reflection-fx/enqueue-state!)
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect!
                  (fn [sound]
                    (swap! sounds* conj sound))]
      (update-from-event!
        enqueue-state!
        (event "ctx-main"
               {:mode :reflect-entity :x 1.0 :y 2.0 :z 3.0 :reflected? true}))
      (update-from-event!
        enqueue-state!
        {:payload {:mode :play :x 4.0 :y 5.0 :z 6.0}
         :ctx-id "ctx-main"
         :channel :vec-reflection/fx-play
         :owner-key [:ctx "ctx-main"]}))
    (is (= 2 (count @sounds*)))
    (is (= 2 (count (get (:wave-effects (vrfx/vec-reflection-fx-snapshot))
                         [:ctx "ctx-main"]))))))

(deftest reflect-entity-redirects-the-projectile-on-the-client-test
  ;; c_reflectEntity re-runs the reflection locally so a bounced arrow turns as
  ;; soon as the message lands, instead of holding its old course until the
  ;; server's next velocity sync. The velocity comes down with the message, so
  ;; the client never has to solve the aim itself and cannot disagree.
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.vec-reflection-fx/enqueue-state!)
        bridge* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                  client-bridge/run-client-effect! (fn [k payload]
                                                     (swap! bridge* conj [k payload])
                                                     true)]
      (enqueue-state! nil "ctx-1" :vec-reflection/fx-reflect-entity [:ctx "ctx-1"]
                      {:mode :reflect-entity :reflected? true
                       :x 1.0 :y 2.0 :z 3.0
                       :entity-uuid "arrow-1" :vx 0.1 :vy -0.2 :vz 0.3})
      (is (= [[:mcmod/set-client-entity-motion
               {:entity-uuid "arrow-1" :vx 0.1 :vy -0.2 :vz 0.3}]]
             @bridge*))
      ;; a reflect the server could not attribute to an entity redirects nothing
      (reset! bridge* [])
      (enqueue-state! nil "ctx-1" :vec-reflection/fx-reflect-entity [:ctx "ctx-1"]
                      {:mode :reflect-entity :reflected? true :x 1.0 :y 2.0 :z 3.0})
      (is (empty? @bridge*)))))

(deftest init-registers-reflected-flag-through-fx-channel-handler-test
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
      (vrfx/init!)
      ((get @handlers* :vec-reflection/fx-reflect-entity) "ctx-1" :vec-reflection/fx-reflect-entity
       {:x 1.0 :y 2.0 :z 3.0 :reflected? true :yaw-rad 0.5 :pitch-rad -0.25
        :entity-uuid "arrow-1" :vx 0.1 :vy 0.2 :vz 0.3})
      ((get @handlers* :vec-reflection/fx-reflect-entity) "ctx-1" :vec-reflection/fx-reflect-entity
       {:x 4.0 :y 5.0 :z 6.0 :reflected? false :yaw-rad 0.5 :pitch-rad -0.25})
      (is (= [[:vec-reflection "ctx-1" :vec-reflection/fx-reflect-entity
                {:mode :reflect-entity
                 :x 1.0 :y 2.0 :z 3.0 :reflected? true
                 :yaw-rad 0.5 :pitch-rad -0.25
                 :entity-uuid "arrow-1" :vx 0.1 :vy 0.2 :vz 0.3}
                '(:owner-key [:ctx "ctx-1"])]
              [:vec-reflection "ctx-1" :vec-reflection/fx-reflect-entity
                {:mode :reflect-entity
                 :x 4.0 :y 5.0 :z 6.0 :reflected? false
                 :yaw-rad 0.5 :pitch-rad -0.25
                 :entity-uuid nil :vx nil :vy nil :vz nil}
                '(:owner-key [:ctx "ctx-1"])]]
             @enqueued*)))))

(deftest two-owners-keep-vec-reflection-state-and-waves-independent-test
  (let [enqueue-state! (var-get #'cn.li.ac.content.ability.vecmanip.vec-reflection-fx/enqueue-state!)]
    (update-from-event!
      enqueue-state!
      (event "ctx-a" {:mode :start :source-player-id "player-a"}))
    (update-from-event!
      enqueue-state!
      (event "ctx-b" {:mode :start :source-player-id "player-b"}))
    (update-from-event!
      enqueue-state!
      (event "ctx-a" {:mode :reflect-entity :x 1.0 :y 2.0 :z 3.0 :reflected? true :source-player-id "player-a"}))
    (update-from-event!
      enqueue-state!
      (event "ctx-b" {:mode :reflect-entity :x 4.0 :y 5.0 :z 6.0 :reflected? true :source-player-id "player-b"}))
    (let [snapshot (vrfx/vec-reflection-fx-snapshot)]
      (is (= #{[:ctx "ctx-a"] [:ctx "ctx-b"]}
             (set (keys (:effect-state snapshot)))))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-b"]))))
      (vrfx/clear-vec-reflection-owner! [:ctx "ctx-a"])
      (let [after-clear (vrfx/vec-reflection-fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (= 1 (count (get (:wave-effects after-clear) [:ctx "ctx-b"]))))))))

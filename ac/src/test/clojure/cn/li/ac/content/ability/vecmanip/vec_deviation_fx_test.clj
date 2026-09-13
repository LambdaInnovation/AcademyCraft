(ns cn.li.ac.content.ability.vecmanip.vec-deviation-fx-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.vecmanip.vec-deviation-fx :as vdfx]
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.vec-deviation]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-fixture [f]
  (try
        (level-effects/reset-level-effect-registry-for-test!)
        (vdfx/reset-fx-for-test!)
        (f)
        (finally
          (vdfx/reset-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event
  [ctx-id payload]
  {:payload payload
   :ctx-id ctx-id
   :channel :vec-deviation/fx-stop-entity
   :owner-key [:ctx ctx-id]})

(deftest init-registers-owner-aware-vec-deviation-fx-test
  (let [registered-level* (atom nil)
        registered-topics* (atom #{})]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                         (reset! registered-level* [effect-id effect-map])
                                                         nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (vdfx/init!)
      (is (= :vec-deviation (first @registered-level*)))
      (is (fn? (:enqueue-state-fn (second @registered-level*))))
      (is (= #{:vec-deviation/fx-start
               :vec-deviation/fx-end
               :vec-deviation/fx-stop-entity
               :vec-deviation/fx-play}
             @registered-topics*)))))

(deftest enqueue-stop-entity-requires-marked-flag-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (is (= {:effect-state {} :wave-effects {}}
           (vdfx/fx-snapshot)))
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-main" :vec-deviation/fx-stop-entity {:mode :stop-entity :x 1.0 :y 2.0 :z 3.0 :marked? false})
    (is (empty? (:wave-effects (vdfx/fx-snapshot))))
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-main" :vec-deviation/fx-stop-entity {:mode :stop-entity :x 1.0 :y 2.0 :z 3.0 :marked? true})
    (let [waves (get (:wave-effects (vdfx/fx-snapshot)) [:ctx "ctx-main"])]
      (is (= 1 (count waves)))
      (is (= 3.0 (:z (first waves)))))))

(deftest stop-entity-halts-the-projectile-locally-and-only-marks-make-a-wave-test
  ;; c_stopEntity zeroes the entity's motion OUTSIDE the isMarked branch, so a
  ;; deflected projectile stops the instant the message lands rather than
  ;; coasting to the next velocity sync; the wave and its sound come only when
  ;; the entity was marked.
  (let [enqueue-state! (var-get #'cn.li.ac.ability.client.fx-templates.arc-beam.impl.vec-deviation/enqueue-state!)
        bridge* (atom [])
        sounds* (atom [])]
    (with-redefs [client-sounds/queue-current-sound-effect! (fn [c] (swap! sounds* conj c) nil)
                  client-bridge/run-client-effect! (fn [k p] (swap! bridge* conj [k p]) true)]
      ;; unmarked (a fireball): stopped locally, no wave, no sound
      (let [store (enqueue-state! nil "ctx-1" :vec-deviation/fx-stop-entity [:ctx "ctx-1"]
                                  {:mode :stop-entity :marked? false :entity-uuid "fb-1"
                                   :x 1.0 :y 2.0 :z 3.0})]
        (is (= [[:mcmod/set-client-entity-motion
                 {:entity-uuid "fb-1" :vx 0.0 :vy 0.0 :vz 0.0}]]
               @bridge*))
        (is (empty? @sounds*))
        (is (empty? (:wave-effects store))))
      (reset! bridge* [])
      ;; marked (an arrow): stopped locally AND a wave with its sound
      (let [store (enqueue-state! nil "ctx-1" :vec-deviation/fx-stop-entity [:ctx "ctx-1"]
                                  {:mode :stop-entity :marked? true :entity-uuid "arrow-1"
                                   :x 1.0 :y 2.0 :z 3.0 :yaw-rad 0.0 :pitch-rad 0.0})
            wave (first (get (:wave-effects store) [:ctx "ctx-1"]))]
        (is (= [[:mcmod/set-client-entity-motion
                 {:entity-uuid "arrow-1" :vx 0.0 :vy 0.0 :vz 0.0}]]
               @bridge*))
        (is (= 1 (count @sounds*)))
        (is (= "academy:vecmanip.vec_deviation" (:sound-id (first @sounds*))))
        ;; WaveEffect(world, 1, 0.6): one ring, and the entity's 15-tick life
        (is (= 1 (count (:rings wave))))
        (is (= 15 (:ttl wave)))
        (is (<= (* 0.6 0.8) (:size (first (:rings wave))) (* 0.6 1.2)))))))

(deftest init-registers-marked-flag-through-fx-channel-handler-test
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
      (vdfx/init!)
      ((get @handlers* :vec-deviation/fx-stop-entity) "ctx-1" :vec-deviation/fx-stop-entity
       {:x 1.0 :y 2.0 :z 3.0 :marked? true
        :entity-uuid "arrow-1" :yaw-rad 0.5 :pitch-rad -0.25})
      ((get @handlers* :vec-deviation/fx-stop-entity) "ctx-1" :vec-deviation/fx-stop-entity
       {:x 4.0 :y 5.0 :z 6.0 :marked? false
        :entity-uuid "fireball-1" :yaw-rad 0.5 :pitch-rad -0.25})
      (is (= [[:vec-deviation "ctx-1" :vec-deviation/fx-stop-entity
               {:mode :stop-entity :x 1.0 :y 2.0 :z 3.0 :marked? true
                :entity-uuid "arrow-1" :yaw-rad 0.5 :pitch-rad -0.25}
               '(:owner-key [:ctx "ctx-1"])]
              [:vec-deviation "ctx-1" :vec-deviation/fx-stop-entity
               {:mode :stop-entity :x 4.0 :y 5.0 :z 6.0 :marked? false
                :entity-uuid "fireball-1" :yaw-rad 0.5 :pitch-rad -0.25}
               '(:owner-key [:ctx "ctx-1"])]]
             @enqueued*)))))

(deftest two-owners-keep-vec-deviation-state-and-waves-independent-test
  (with-redefs [client-sounds/queue-current-sound-effect! (fn [& _] nil)
                client-bridge/run-client-effect! (fn [& _] nil)]
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-a" :vec-deviation/fx-stop-entity {:mode :start :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-b" :vec-deviation/fx-stop-entity {:mode :start :source-player-id "player-b"})
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-a" :vec-deviation/fx-stop-entity {:mode :stop-entity :x 1.0 :y 64.0 :z 1.0 :marked? true :source-player-id "player-a"})
    (arc-beam/enqueue-for-test! :vec-deviation "ctx-b" :vec-deviation/fx-stop-entity {:mode :stop-entity :x 2.0 :y 64.0 :z 2.0 :marked? true :source-player-id "player-b"})
    (let [snapshot (vdfx/fx-snapshot)]
      (is (:active? (get (:effect-state snapshot) [:ctx "ctx-a"])))
      (is (:active? (get (:effect-state snapshot) [:ctx "ctx-b"])))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-a"]))))
      (is (= 1 (count (get (:wave-effects snapshot) [:ctx "ctx-b"]))))
      (vdfx/clear-fx-owner! [:ctx "ctx-a"])
      (let [after-clear (vdfx/fx-snapshot)]
        (is (nil? (get (:effect-state after-clear) [:ctx "ctx-a"])))
        (is (nil? (get (:wave-effects after-clear) [:ctx "ctx-a"])))
        (is (:active? (get (:effect-state after-clear) [:ctx "ctx-b"])))))))



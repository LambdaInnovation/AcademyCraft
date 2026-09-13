(ns cn.li.ac.content.ability.meltdowner.meltdowner-fx-owner-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.meltdowner.electron-bomb-fx :as electron-bomb-fx]
            [cn.li.ac.content.ability.meltdowner.electron-missile-fx :as electron-missile-fx]
            [cn.li.ac.content.ability.meltdowner.jet-engine-fx :as jet-engine-fx]
            [cn.li.ac.content.ability.meltdowner.light-shield-fx :as light-shield-fx]
            [cn.li.ac.content.ability.meltdowner.meltdowner-fx :as meltdowner-fx]
            [cn.li.ac.content.ability.meltdowner.mine-ray-fx :as mine-ray-fx]
            [cn.li.ac.content.ability.meltdowner.ray-barrage-fx :as ray-barrage-fx]
            [cn.li.ac.content.ability.meltdowner.scatter-bomb-fx :as scatter-bomb-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(defn- reset-fixture [f]
  (level-effects/reset-level-effect-registry-for-test!)
      (electron-bomb-fx/init!)
      (electron-missile-fx/init!)
      (jet-engine-fx/init!)
      (light-shield-fx/init!)
      (meltdowner-fx/init!)
      (mine-ray-fx/init!)
      (ray-barrage-fx/init!)
      (scatter-bomb-fx/init!)
      (electron-bomb-fx/reset-fx-for-test!)
      (electron-missile-fx/reset-electron-missile-fx-for-test!)
      (jet-engine-fx/reset-fx-for-test!)
      (light-shield-fx/reset-fx-for-test!)
      (meltdowner-fx/reset-fx-for-test!)
      (mine-ray-fx/reset-mine-ray-fx-for-test!)
      (ray-barrage-fx/reset-fx-for-test!)
      (scatter-bomb-fx/reset-scatter-bomb-fx-for-test!)
      (try
        (f)
        (finally
          (electron-bomb-fx/reset-fx-for-test!)
          (electron-missile-fx/reset-electron-missile-fx-for-test!)
          (jet-engine-fx/reset-fx-for-test!)
          (light-shield-fx/reset-fx-for-test!)
          (meltdowner-fx/reset-fx-for-test!)
          (mine-ray-fx/reset-mine-ray-fx-for-test!)
          (ray-barrage-fx/reset-fx-for-test!)
          (scatter-bomb-fx/reset-scatter-bomb-fx-for-test!)
          (level-effects/reset-level-effect-registry-for-test!))))

(use-fixtures :each reset-fixture)

(defn- event [ctx-id channel payload]
  {:payload payload
   :ctx-id ctx-id
   :channel channel
   :owner-key [:ctx ctx-id]})

(defn- dispatch! [effect-id {:keys [payload ctx-id channel owner-key]}]
  (level-effects/enqueue-level-effect! effect-id ctx-id channel payload :owner-key owner-key))

(def ^:private p0 {:x 0.0 :y 64.0 :z 0.0})
(def ^:private p1 {:x 1.0 :y 64.0 :z 0.0})
(def ^:private p2 {:x 2.0 :y 64.0 :z 0.0})

(deftest meltdowner-stateful-fx-keep-state-per-owner-test
  (with-redefs [client-bridge/run-client-effect! (fn [& _] nil)
                client-particles/queue-particle-effect! (fn [& _] nil)
                client-particles/queue-current-particle-effect! (fn [& _] nil)
                client-particles/current-effect-owner (fn [] {:client-session-id "meltdowner-fx-owner"})
                client-sounds/queue-sound-effect! (fn [& _] nil)
                client-sounds/queue-current-sound-effect! (fn [& _] nil)
                client-sounds/current-effect-owner (fn [] {:client-session-id "meltdowner-fx-owner"})]
    (dispatch! :electron-bomb (event "ctx-a" :electron-bomb/fx-spawn
                                    {:mode :spawn :x 1.0 :y 64.0 :z 1.0}))
    (dispatch! :electron-bomb (event "ctx-b" :electron-bomb/fx-spawn
                                    {:mode :spawn :x 2.0 :y 64.0 :z 2.0}))

    (dispatch! :electron-missile (event "ctx-a" :electron-missile/fx-fire
                                       {:mode :fire
                                        :start {:x 0.0 :y 64.0 :z 0.0}
                                        :end {:x 1.0 :y 65.6 :z 1.0}}))
    (dispatch! :electron-missile (event "ctx-b" :electron-missile/fx-fire
                                       {:mode :fire
                                        :start {:x 0.0 :y 64.0 :z 0.0}
                                        :end {:x 2.0 :y 65.6 :z 2.0}}))

    (dispatch! :jet-engine (event "ctx-a" :jet-engine/fx-start {:mode :mark-start :target {:x 1.0 :y 64.0 :z 1.0}}))
    (dispatch! :jet-engine (event "ctx-b" :jet-engine/fx-start {:mode :mark-start :target {:x 2.0 :y 64.0 :z 2.0}}))
    (dispatch! :jet-engine (event "ctx-a" :jet-engine/fx-trigger-start {:mode :trigger-start :pos {:x 1.2 :y 64.0 :z 1.2}}))
    (dispatch! :jet-engine (event "ctx-b" :jet-engine/fx-trigger-start {:mode :trigger-start :pos {:x 2.2 :y 64.0 :z 2.2}}))

    (dispatch! :light-shield (event "ctx-a" :light-shield/fx-start {:mode :start}))
    (dispatch! :light-shield (event "ctx-b" :light-shield/fx-start {:mode :start}))

    (dispatch! :meltdowner (event "ctx-a" :meltdowner/fx-start {:mode :start}))
    (dispatch! :meltdowner (event "ctx-b" :meltdowner/fx-start {:mode :start}))
    (dispatch! :meltdowner (event "ctx-a" :meltdowner/fx-update
                                {:mode :update :ticks 12 :charge-ratio 0.4}))
    (dispatch! :meltdowner (event "ctx-b" :meltdowner/fx-update
                                {:mode :update :ticks 21 :charge-ratio 0.8}))
    (dispatch! :meltdowner (event "ctx-a" :meltdowner/fx-perform
                                {:mode :perform :start p0 :end p1}))
    (dispatch! :meltdowner (event "ctx-b" :meltdowner/fx-reflect
                                {:mode :reflect :start p1 :end p2}))

    (dispatch! :mine-ray (event "ctx-a" :mine-ray/fx-start {:mode :start :variant :basic}))
    (dispatch! :mine-ray (event "ctx-b" :mine-ray/fx-start {:mode :start :variant :expert}))
    (dispatch! :mine-ray (event "ctx-a" :mine-ray/fx-progress
                              {:mode :progress :x 1 :y 64 :z 1 :progress 0.25}))
    (dispatch! :mine-ray (event "ctx-b" :mine-ray/fx-progress
                              {:mode :progress :x 2 :y 64 :z 2 :progress 0.75}))

    (dispatch! :ray-barrage (event "ctx-a" :ray-barrage/fx-preray
                                 {:mode :preray :start p0 :end p1}))
    (dispatch! :ray-barrage (event "ctx-b" :ray-barrage/fx-preray
                                 {:mode :preray :start p1 :end p2}))
    (dispatch! :ray-barrage (event "ctx-a" :ray-barrage/fx-barrage
                                 {:mode :barrage :silbarn p0 :yaw 0.0 :pitch 0.0}))
    (dispatch! :ray-barrage (event "ctx-b" :ray-barrage/fx-barrage
                                 {:mode :barrage :silbarn p1 :yaw 10.0 :pitch -5.0}))

    (dispatch! :scatter-bomb (event "ctx-a" :scatter-bomb/fx-start {:mode :start}))
    (dispatch! :scatter-bomb (event "ctx-b" :scatter-bomb/fx-start {:mode :start}))
    (dispatch! :scatter-bomb (event "ctx-a" :scatter-bomb/fx-ball
                                  {:mode :ball :x 1.0 :y 64.0 :z 1.0 :count 3}))
    (dispatch! :scatter-bomb (event "ctx-b" :scatter-bomb/fx-ball
                                  {:mode :ball :x 2.0 :y 64.0 :z 2.0 :count 5}))

    (is (:active? (get-in (electron-bomb-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
    (is (:active? (get-in (electron-bomb-fx/fx-snapshot) [:effect-state [:ctx "ctx-b"]])))
    (is (= 1 (count (get-in (electron-missile-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-a"]]))))
    (is (= 1 (count (get-in (electron-missile-fx/electron-missile-fx-snapshot) [:beams [:ctx "ctx-b"]]))))
    (is (= :triggering (get-in (jet-engine-fx/fx-snapshot) [:fx-state [:ctx "ctx-a"] :phase])))
    (is (= :triggering (get-in (jet-engine-fx/fx-snapshot) [:fx-state [:ctx "ctx-b"] :phase])))
    (is (:active? (get-in (light-shield-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"]])))
    (is (:active? (get-in (light-shield-fx/fx-snapshot) [:effect-state [:ctx "ctx-b"]])))
    (is (= 12 (get-in (meltdowner-fx/fx-snapshot) [:effect-state [:ctx "ctx-a"] :ticks])))
    (is (= 21 (get-in (meltdowner-fx/fx-snapshot) [:effect-state [:ctx "ctx-b"] :ticks])))
    (is (= 1 (count (get-in (meltdowner-fx/fx-snapshot) [:rays [:ctx "ctx-a"]]))))
    (is (= 1 (count (get-in (meltdowner-fx/fx-snapshot) [:rays [:ctx "ctx-b"]]))))
    (is (= 0.25 (get-in (mine-ray-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-a"] :progress])))
    (is (= 0.75 (get-in (mine-ray-fx/mine-ray-fx-snapshot) [:effect-state [:ctx "ctx-b"] :progress])))
    (let [a-count (count (get-in (ray-barrage-fx/fx-snapshot) [:beam-queue [:ctx "ctx-a"]]))
          b-count (count (get-in (ray-barrage-fx/fx-snapshot) [:beam-queue [:ctx "ctx-b"]]))]
      ;; per-owner isolation, and the queue is NOT merged across owners even
      ;; though both happen to hold 1 preray + a random 25~30 sub rays
      (is (<= 26 a-count 31))
      (is (<= 26 b-count 31)))
    (is (= 3 (get-in (scatter-bomb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-a"] :balls])))
    (is (= 5 (get-in (scatter-bomb-fx/scatter-bomb-fx-snapshot) [:effect-state [:ctx "ctx-b"] :balls])))))



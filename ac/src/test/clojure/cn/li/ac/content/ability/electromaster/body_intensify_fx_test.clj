(ns cn.li.ac.content.ability.electromaster.body-intensify-fx-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.ac.content.ability.electromaster.body-intensify-fx :as body-intensify-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]))

(deftest init-registers-body-intensify-fx-channel-test
  (let [registered* (atom nil)]
    (with-redefs [fx-registry/register-fx-channel! (fn [channel handler]
                                                     (reset! registered* {:channel channel
                                                                          :handler handler})
                                                     nil)]
      (body-intensify-fx/init!)
      (is (= :body-intensify/fx-end (:channel @registered*)))
      (is (fn? (:handler @registered*))))))

(deftest fx-handler-plays-local-effect-when-performed-test
  (let [handlers* (atom {})
        sounds* (atom [])
        client-effects* (atom [])
        blends* (atom [])]
    (with-redefs [fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)
                  client-sounds/queue-current-sound-effect! (fn [payload]
                                                               (swap! sounds* conj payload)
                                                               nil)
                  reactive-hud/start-charging-blend! (fn [source-player-id performed?]
                                                       (swap! blends* conj [source-player-id performed?])
                                                       nil)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! client-effects* conj [effect-key payload])
                                                     nil)]
      (body-intensify-fx/init!)
      ((get @handlers* :body-intensify/fx-start)
       "ctx-1" :body-intensify/fx-start
       {:source-player-id "player-1"})
      ((get @handlers* :body-intensify/fx-end)
       "ctx-1" :body-intensify/fx-end
       {:performed? true
        :source-player-id "player-1"
        :caster-pos {:x 1.0 :y 2.0 :z 3.0}})
      ((get @handlers* :body-intensify/fx-end)
       "ctx-2" :body-intensify/fx-end
       {:performed? false
        :source-player-id "player-2"})
      (is (= 1 (count @sounds*)))
      (is (= {:source :ambient
              :volume 0.5
              :pitch 1.0
              :x 1.0
              :y 2.0
              :z 3.0}
             (dissoc (first @sounds*) :sound-id)))
      (is (= [:mcmod/start-loop-sound-at-player
              :mcmod/stop-loop-sound
              :mcmod/spawn-scripted-effect-at-player
              :mcmod/stop-loop-sound]
             (mapv first @client-effects*)))
      (is (= "player-1"
             (get-in @client-effects* [0 1 :owner-uuid])))
      (is (= {:effect-id "intensify_effect"
              :owner-uuid "player-1"
              :ctx-id "ctx-1"
              :channel :body-intensify/fx-end}
             (get-in @client-effects* [2 1])))
      (is (= [["player-1" true] ["player-2" false]] @blends*)
          "every release drives CurrentChargingHUD.startBlend(performed)"))))

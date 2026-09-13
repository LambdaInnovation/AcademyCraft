(ns cn.li.ac.content.ability.teleporter.location-teleport-fx-test

  (:require [clojure.test :refer [deftest is]]

            [cn.li.ac.ability.client.fx-registry :as fx-registry]

            [cn.li.ac.ability.client.effects.sounds :as client-sounds]

            [cn.li.ac.content.ability.teleporter.location-teleport-fx :as lfx]))

(deftest init-registers-location-teleport-success-channel-test

  (let [registered-topics* (atom #{})]

    (with-redefs [fx-registry/register-fx-channel! (fn [topic _handler]

                                                    (swap! registered-topics* conj topic)

                                                    nil)]

      (lfx/init!)

      (is (= #{:location-teleport/fx-perform-success}

             @registered-topics*)))))

(deftest fx-handler-routes-success-event-to-immediate-sound-test

  (let [handlers* (atom {})

        sounds* (atom [])]

    (with-redefs [fx-registry/register-fx-channel! (fn [topic handler]

                                                    (swap! handlers* assoc topic handler)

                                                    nil)

                  client-sounds/queue-current-sound-effect! (fn [payload]

                                                              (swap! sounds* conj payload)

                                                              nil)]

      (lfx/init!)

      ((get @handlers* :location-teleport/fx-perform-success)

        "ctx-1"

        :location-teleport/fx-perform-success

        {:target {:x 1.0 :y 64.0 :z 2.0}

         :distance 10.0})

      (is (= 1 (count @sounds*)))

      (is (= "academy:tp.tp" (:sound-id (first @sounds*)))))))

(deftest enqueue-perform-success-plays-teleport-sound-test
  (let [handlers* (atom {})
        sounds* (atom [])]
    (with-redefs [fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)
                  client-sounds/queue-current-sound-effect! (fn [payload]
                                                               (swap! sounds* conj payload)
                                                               nil)]
      (lfx/init!)
      ((get @handlers* :location-teleport/fx-perform-success)
        "ctx-test" :location-teleport/fx-perform-success
        {:mode :perform-success :target {:x 1.0 :y 2.0 :z 3.0}})
      (is (= 1 (count @sounds*)))
      (is (= "academy:tp.tp" (:sound-id (first @sounds*)))))))

;; Success is gated by the channel itself — there is only one topic, and it is
;; only ever sent on a successful teleport, so the handler needs no :mode check.
(deftest only-the-success-channel-is-registered-test
  (let [handlers* (atom {})]
    (with-redefs [fx-registry/register-fx-channel! (fn [topic handler]
                                                     (swap! handlers* assoc topic handler)
                                                     nil)]
      (lfx/init!)
      (is (= #{:location-teleport/fx-perform-success} (set (keys @handlers*)))))))


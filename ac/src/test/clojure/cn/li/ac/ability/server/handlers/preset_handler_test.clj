(ns cn.li.ac.ability.server.handlers.preset-handler-test
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.test.support.player-state :as test-player]
            [cn.li.ac.ability.model.ability :as ability-data]
            [cn.li.ac.ability.model.preset :as preset-data]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.server.handlers.preset-handler :as preset-handler]            [cn.li.ac.ability.util.uuid :as uuid]))

(use-fixtures :each test-player/clean-player-states-fixture)

(defn- seed-player!
  [player-uuid learned-skills slot]
  (store/set-player-state! test-player/test-session-id
                            player-uuid
                            {:ability-data (reduce ability-data/learn-skill
                                                   (ability-data/new-ability-data)
                                                   learned-skills)
                             :preset-data (cond-> (preset-data/new-preset-data)
                                            slot (preset-data/set-slot 0 0 slot))}))

(deftest set-preset-slot-accepts-only-learned-controllable-skill-test
  (with-redefs [uuid/player-uuid identity
                skill-query/get-skill-by-controllable (fn [cat-id ctrl-id]
                                                        (when (= [cat-id ctrl-id] [:electromaster :arc-gen])
                                                          :arc-gen))]
    (seed-player! "p1" [:arc-gen] nil)
    (preset-handler/handle-set-preset-request {:preset-idx 0
                                               :key-idx 0
                                               :cat-id :electromaster
                                               :ctrl-id :arc-gen}
                                              "p1")
    (is (= [:electromaster :arc-gen]
          (get-in (store/get-player-state test-player/test-session-id "p1") [:preset-data :slots [0 0]])))))

(deftest set-preset-slot-ignores-unlearned-or-invalid-controllable-test
  (with-redefs [uuid/player-uuid identity
                skill-query/get-skill-by-controllable (fn [cat-id ctrl-id]
                                                        (case [cat-id ctrl-id]
                                                          [:electromaster :arc-gen] :arc-gen
                                                          [:electromaster :disabled] nil
                                                          nil))]
    (seed-player! "p1" [] [:electromaster :existing])
    (preset-handler/handle-set-preset-request {:preset-idx 0
                                               :key-idx 0
                                               :cat-id :electromaster
                                               :ctrl-id :arc-gen}
                                              "p1")
    (is (= [:electromaster :existing]
          (get-in (store/get-player-state test-player/test-session-id "p1") [:preset-data :slots [0 0]])))

    (seed-player! "p2" [:disabled] [:electromaster :existing])
    (preset-handler/handle-set-preset-request {:preset-idx 0
                                               :key-idx 0
                                               :cat-id :electromaster
                                               :ctrl-id :disabled}
                                              "p2")
    (is (= [:electromaster :existing]
          (get-in (store/get-player-state test-player/test-session-id "p2") [:preset-data :slots [0 0]])))))

(deftest set-preset-slot-clear-request-removes-slot-test
  (with-redefs [uuid/player-uuid identity]
    (seed-player! "p1" [:arc-gen] [:electromaster :arc-gen])
    (preset-handler/handle-set-preset-request {:preset-idx 0
                                               :key-idx 0
                                               :cat-id nil
                                               :ctrl-id nil}
                                              "p1")
    (is (nil? (get-in (store/get-player-state test-player/test-session-id "p1") [:preset-data :slots [0 0]])))))

(deftest set-preset-slot-fires-preset-update-event-on-success-test
  (let [events* (atom [])]
    (with-redefs [uuid/player-uuid identity
                  skill-query/get-skill-by-controllable (fn [cat-id ctrl-id]
                                                          (when (= [cat-id ctrl-id] [:electromaster :arc-gen])
                                                            :arc-gen))
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (seed-player! "p1" [:arc-gen] nil)
      (preset-handler/handle-set-preset-request {:preset-idx 0
                                                 :key-idx 0
                                                 :cat-id :electromaster
                                                 :ctrl-id :arc-gen}
                                                "p1")
      (preset-handler/handle-set-preset-request {:preset-idx 0
                                                 :key-idx 0
                                                 :cat-id nil
                                                 :ctrl-id nil}
                                                "p1")
      (is (= [{:event/type evt/EVT-PRESET-UPDATE
               :event/side :both
               :uuid "p1"
               :preset-idx 0
               :key-idx 0
               :slot [:electromaster :arc-gen]}
              {:event/type evt/EVT-PRESET-UPDATE
               :event/side :both
               :uuid "p1"
               :preset-idx 0
               :key-idx 0
               :slot nil}]
             @events*)))))

(deftest switch-preset-request-fires-old-new-payload-test
  (let [events* (atom [])]
    (with-redefs [uuid/player-uuid identity
                  evt/fire-ability-event! (fn [event]
                                            (swap! events* conj event))]
      (store/set-player-state! test-player/test-session-id
                                "p1"
                                {:ability-data (ability-data/new-ability-data)
                                 :preset-data (preset-data/set-active-preset (preset-data/new-preset-data) 1)})
      (preset-handler/handle-switch-preset-request {:preset-idx 3} "p1")
      (is (= 3 (get-in (store/get-player-state test-player/test-session-id "p1") [:preset-data :active-preset])))
      (is (= [{:event/type evt/EVT-PRESET-SWITCH
               :event/side :both
               :uuid "p1"
               :old-preset 1
               :new-preset 3}]
             @events*)))))



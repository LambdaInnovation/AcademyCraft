(ns cn.li.ac.content.ability.teleporter.penetrate-teleport-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.content.ability.teleporter.penetrate-teleport :as pt]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.ability.effects.geom :as geom]))

(defn- with-pt-env [f]
  (ps-fix/with-test-player-state-owner
    (fn []
      (skill-ctx/with-server-skill-context f))))

(defn- make-context-mocks [initial]
  (let [base (skill-ctx/content-ctx-mocks initial)
        listeners* (atom {})]
    (assoc base
           :listeners* listeners*
           :ctx-on! (fn [_ channel handler]
                      (swap! listeners* assoc channel handler)
                      nil))))

(deftest penetrate-down-registers-distance-listener-and-updates-state-test
  (let [{:keys [ctx* listeners* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! ctx-on!]}
        (make-context-mocks {:skill-state {}})]
    (with-pt-env
      #(with-redefs [ctx/get-context get-context
                    ctx/ctx-on! ctx-on!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    skill-effects/skill-exp (fn [_ _] 0.2)
                    skill-config/lerp-double (fn [_ field-id _]
                                               (case field-id
                                                 :targeting.max-distance 20.0
                                                 0.0))
                    pt/resolve-preview (fn [_player-id desired]
                                         {:distance desired
                                          :cp-per-block 10.0
                                          :available? true
                                          :dest {:x desired :y 64.0 :z 0.0}})]
         (cb/apply-invoke pt/penetrate-tp-down! :player-id "p1" :ctx-id "ctx-1" :cost-ok? true)
         ((get @listeners* :penetrate-tp/set-distance) {:delta -50.0})))
    (is (= 0 (get-in @ctx* [:skill-state :hold-ticks])))
    (is (= 0.5 (get-in @ctx* [:skill-state :desired-distance])))
    (is (= true (get-in @ctx* [:skill-state :preview :available?])))))

(deftest penetrate-tick-refreshes-preview-and-resets-up-resolve-test
  (let [{:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        (make-context-mocks {:skill-state {:desired-distance 6.0
                                           :up-resolve {:distance 3.0}}})]
    (with-pt-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    pt/resolve-preview (fn [_player-id desired]
                                         {:distance desired
                                          :cp-per-block 9.0
                                          :available? true
                                          :dest {:x 1.0 :y 2.0 :z 3.0}})]
         (cb/apply-invoke pt/penetrate-tp-tick! :player-id "p1" :ctx-id "ctx-2" :hold-ticks 7)))
    (is (= 7 (get-in @ctx* [:skill-state :hold-ticks])))
    (is (= 6.0 (get-in @ctx* [:skill-state :preview :distance])))
    (is (nil? (get-in @ctx* [:skill-state :up-resolve])))))

(deftest penetrate-up-success-and-cost-fail-side-effects-test
  (let [exp-calls* (atom [])
        cooldown-calls* (atom [])
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        ach-calls* (atom [])
        teleport-calls* (atom [])]
    (with-pt-env
      #(with-redefs [pt/ensure-up-resolve! (fn [_ctx-id _player-id]
                                              {:exp 0.3
                                               :distance 8.0
                                               :available? true
                                               :dest {:x 10.0 :y 64.0 :z 12.0}})
                    geom/world-id-of (fn [_] "minecraft:overworld")
                    helper/teleport-to! (fn [player-id world-id x y z]
                                          (swap! teleport-calls* conj [player-id world-id x y z])
                                          true)
                    pt/cooldown-ticks (fn [_] 40)
                    pt/exp-per-distance (fn [] 0.001)
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp-calls* conj [player-id skill-id amount]))
                    skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                                       (swap! cooldown-calls* conj [player-id skill-id ticks]))
                    fx/send! send!
                    ach-dispatcher/trigger-custom-event! (fn [player-id event-id]
                                                            (swap! ach-calls* conj [player-id event-id]))]
         (cb/apply-invoke pt/penetrate-tp-up! :player-id "p1" :ctx-id "ctx-ok" :cost-ok? true)
         (cb/apply-invoke pt/penetrate-tp-up! :player-id "p2" :ctx-id "ctx-fail" :cost-ok? false)))
    (is (= [["p1" "minecraft:overworld" 10.0 64.0 12.0]] @teleport-calls*))
    (is (= [["p1" :penetrate-teleport 0.008]] @exp-calls*))
    (is (= [["p1" :penetrate-teleport 40]] @cooldown-calls*))
    ;; The release sound fires before server validation, so the cost-fail
    ;; context still gets its fx-perform — matching the original.
    (is (= [["ctx-ok" :penetrate-teleport/fx-perform :perform {:to-x 10.0 :to-y 64.0 :to-z 12.0}]
            ["ctx-fail" :penetrate-teleport/fx-perform :perform {:to-x 10.0 :to-y 64.0 :to-z 12.0}]]
           @calls*))
    (is (= [["p1" "teleporter.ignore_barrier"]] @ach-calls*))))

(deftest penetrate-tp-down-sends-start-fx-test
  ;; Upstream l_spawnMark spawns EntityTPMarking on MSG_MADEALIVE; fx-start
  ;; carries the first preview so the mark appears on key-down.
  (let [{:keys [ctx* listeners* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! ctx-on!]}
        (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-pt-env
      #(with-redefs [ctx/get-context get-context
                    ctx/ctx-on! ctx-on!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    skill-effects/skill-exp (fn [_ _] 0.2)
                    skill-config/lerp-double (fn [_ field-id _]
                                               (case field-id
                                                 :targeting.max-distance 20.0
                                                 0.0))
                    pt/resolve-preview (fn [_player-id desired]
                                         {:distance desired
                                          :cp-per-block 10.0
                                          :available? true
                                          :dest {:x 3.0 :y 64.0 :z 0.0}})]
         (cb/apply-invoke pt/penetrate-tp-down! :player-id "p1" :ctx-id "ctx-s" :cost-ok? true)))
    (is (= :penetrate-teleport/fx-start (get-in (first @calls*) [1])))
    (let [payload (get-in (first @calls*) [3])]
      (is (= 3.0 (:x payload)))
      (is (true? (:available? payload))))))

(deftest penetrate-tp-tick-sends-update-fx-test
  ;; Upstream l_updateMark moves EntityTPMarking every client tick.
  (let [{:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        (make-context-mocks {:skill-state {:desired-distance 6.0}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-pt-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    pt/resolve-preview (fn [_player-id desired]
                                         {:distance desired
                                          :cp-per-block 9.0
                                          :available? true
                                          :dest {:x 1.0 :y 2.0 :z 3.0}})]
         (cb/apply-invoke pt/penetrate-tp-tick! :player-id "p1" :ctx-id "ctx-u" :hold-ticks 7)))
    (is (= :penetrate-teleport/fx-update (get-in (first @calls*) [1])))
    (let [payload (get-in (first @calls*) [3])]
      (is (= 6.0 (:distance payload)))
      (is (true? (:available? payload))))))

(deftest penetrate-tp-abort-clears-state-and-sends-end-test
  ;; Upstream l_onKeyAbort terminates; c_endEffect kills the mark.
  (let [{:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        (make-context-mocks {:skill-state {:desired-distance 6.0}})
        fx-topics* (atom [])]
    (with-pt-env
      #(with-redefs [ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! (fn [& args]
                               (swap! fx-topics* conj (:topic (second args)))
                               nil)]
         (cb/apply-invoke pt/penetrate-tp-abort! :ctx-id "ctx-a")))
    (is (nil? (:skill-state @ctx*)))
    (is (= [:penetrate-teleport/fx-end] @fx-topics*))))

(deftest penetrate-up-unavailable-no-teleport-and-cost-zero-test
  ;; Upstream s_execute terminates without cost/exp/cooldown when the
  ;; destination is unavailable (the walk is still inside a wall).
  (let [{:keys [ctx* get-context update-skill-state-root! assoc-skill-state! clear-skill-state!]}
        (make-context-mocks {:skill-state {:desired-distance 6.0
                                           :up-resolve {:desired-distance 6.0
                                                        :distance 6.0
                                                        :cp-per-block 10.0
                                                        :exp 0.2
                                                        :available? false
                                                        :dest {:x 1.0 :y 64.0 :z 2.0}}}})
        teleport-calls* (atom 0)
        exp-calls* (atom 0)
        cooldown-calls* (atom 0)]
    (with-pt-env
      #(with-redefs [ctx/get-context get-context
                    ctx-skill/get-context get-context
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    helper/teleport-to! (fn [& _] (swap! teleport-calls* inc) true)
                    skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc) nil)
                    skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc) nil)]
         (cb/apply-invoke pt/penetrate-tp-up! :player-id "p1" :ctx-id "ctx-uav" :cost-ok? true)))
    (is (= 0 @teleport-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))
    (is (= 0.0 (pt/up-cost-cp "p1" :penetrate-teleport 0.2)))
    (is (= 0.0 (pt/up-cost-overload "p1" :penetrate-teleport 0.2)))))

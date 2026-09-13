(ns cn.li.ac.content.ability.electromaster.current-charging-fx-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            ;; arc-beam MUST precede the impl (AOT classes don't self-require);
            ;; the impl ns is otherwise loaded lazily by init!, so target-body
            ;; would not resolve at load time.
            [cn.li.ac.ability.client.fx-templates.arc-beam.impl.current-charging]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- invoke-hand-enqueue! [ctx-id channel payload]
  ;; :runtime :level (single track, world-space beam/ring rendering only —
  ;; the hand track never had a visual for this skill).
  (arc-beam/enqueue-for-test! :current-charging ctx-id channel payload))

(defn- with-fresh-current-charging-fx-runtime [f]
  (runtime-hooks/with-client-ctx-fn {:session-id :test-session}
    (fn []
      (with-redefs [client-bridge/game-time-ms (constantly 1000)]
        (try
          (hand-effects/reset-hand-effect-registry-for-test!)
          (current-charging-fx/reset-fx-for-test!)
          (current-charging-fx/init!)
          (f)
          (finally
            (hand-effects/reset-hand-effect-registry-for-test!)
            (current-charging-fx/reset-fx-for-test!)))))))

(use-fixtures :each with-fresh-current-charging-fx-runtime)

(deftest init-registers-current-charging-fx-channels-test
  (let [registered-topics* (atom #{})
        registered-level* (atom nil)]
    (with-redefs [level-effects/register-level-effect! (fn [effect-id effect-map]
                                                          (reset! registered-level* [effect-id effect-map])
                                                          nil)
                  level-effects/reset-level-effect-state-for-test! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic _handler]
                                                      (swap! registered-topics* conj topic)
                                                      nil)]
      (current-charging-fx/init!)
      (is (= :current-charging (first @registered-level*)))
      (is (= #{:current-charging/fx-start
               :current-charging/fx-update
               :current-charging/fx-end}
             @registered-topics*)))))

(deftest fx-handler-routes-through-level-effects-test
  (let [handlers* (atom {})
        level-enqueued* (atom [])]
    (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                  level-effects/reset-level-effect-state-for-test! (fn [& _] nil)
                  fx-registry/register-fx-channel! (fn [topic handler]
                                                      (swap! handlers* assoc topic handler)
                                                      nil)
                  level-effects/enqueue-level-effect! (fn [effect-id ctx-id channel payload & opts]
                                                        (swap! level-enqueued* conj (into [effect-id ctx-id channel payload] opts))
                                                        nil)]
      (current-charging-fx/init!)
      ((get @handlers* :current-charging/fx-start) "ctx-1" :current-charging/fx-start {:is-item true})
      ((get @handlers* :current-charging/fx-update) "ctx-1" :current-charging/fx-update {:good? true :charge-ticks 20})
      ((get @handlers* :current-charging/fx-end) "ctx-1" :current-charging/fx-end {:is-item true})
      (is (= [[:current-charging "ctx-1" :current-charging/fx-start {:mode :start :is-item true} :owner-key [:ctx "ctx-1"]]
              [:current-charging "ctx-1" :current-charging/fx-update {:mode :update :good? true :charge-ticks 20} :owner-key [:ctx "ctx-1"]]
              [:current-charging "ctx-1" :current-charging/fx-end {:mode :end :is-item true} :owner-key [:ctx "ctx-1"]]]
             @level-enqueued*)))))

(deftest fx-state-updates-and-queues-loop-sound-test
  (let [effects* (atom [])]
    (current-charging-fx/reset-fx-for-test!)
    (with-redefs [client-bridge/run-client-effect! (fn [effect-key payload]
                                                       (swap! effects* conj [effect-key payload])
                                                       nil)]
      (invoke-hand-enqueue! "ctx-1" :current-charging/fx-start {:mode :start :is-item true})
      (is (true? (:active? (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (true? (:is-item (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (= 1 (count @effects*)))
      (is (every? #(= :mcmod/start-loop-sound (first %)) @effects*))
      (is (= 0.3 (get-in @effects* [0 1 :volume])))
      (invoke-hand-enqueue! "ctx-1" :current-charging/fx-update
        {:mode :update
         :is-item true
         :good? true
         :charge-ticks 20
         :target {:x 1.0 :y 2.0 :z 3.0}
         :block-pos [1 2 3]
         :charged 4.0})
      (is (= 20 (:charge-ticks (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (= 0.5 (:charge-ratio (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (= {:x 1.0 :y 2.0 :z 3.0} (:target (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (= [1 2 3] (:block-pos (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (invoke-hand-enqueue! "ctx-1" :current-charging/fx-end {:mode :end :is-item true})
      (is (false? (:active? (current-charging-fx/current-state [:ctx "ctx-1"]))))
      (is (nil? (get-in (current-charging-fx/fx-snapshot)
                        [:states [:ctx "ctx-1"]])))
      (is (= :mcmod/stop-loop-sound (first (last @effects*))))
      (is (= 2 (count @effects*))))))

(deftest block-start-is-immediately-visible-and-lives-until-explicit-cleanup-test
  (let [effects* (atom [])
        now-ms* (atom 1000)
        target {:x 1.0 :y 65.0 :z 17.0}]
    (current-charging-fx/reset-fx-for-test!)
    (with-redefs [client-bridge/game-time-ms #(deref now-ms*)
                  client-bridge/run-client-effect! (fn [effect-key payload]
                                                     (swap! effects* conj [effect-key payload])
                                                     nil)]
      (invoke-hand-enqueue! "ctx-block" :current-charging/fx-start
        {:mode :start
         :is-item false
         :caster-pos {:x 1.0 :y 65.0 :z 2.0}
         :target target})
      (is (true? (:beam-visible?
                  (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= 0 (:visual-ticks
                (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= target
             (:target (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= :mcmod/start-loop-sound (ffirst @effects*)))
      (is (= "academy:em.charge_loop" (get-in @effects* [0 1 :sound-id])))
      (let [plan (level-effects/build-level-effect-plan
                  {:x 1.0 :y 65.0 :z 2.0}
                  {:player-uuid "local-player"}
                  0
                  (fn [& _] []))]
        (is (seq (:ops plan)))
        (is (every? #(= :current-charging/beam (:effect-part %))
                    (:ops plan))))

      ;; Visual animation is driven by client ticks, not by the most recent
      ;; server charge-ticks packet. This prevents a hidden network sample
      ;; from making the beam disappear for an arbitrary number of frames.
      (level-effects/tick-level-effects!)
      (is (= 1 (:visual-ticks
                (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= 0 (:charge-ticks
                (current-charging-fx/current-state [:ctx "ctx-block"]))))

      ;; No update packet arrives for two seconds. A held upstream effect
      ;; remains alive; only an explicit end/owner cleanup may remove it.
      (reset! now-ms* 3000)
      (level-effects/tick-level-effects!)
      (is (true? (:active? (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= target
             (:target (current-charging-fx/current-state [:ctx "ctx-block"]))))
      (is (= 1 (count @effects*)))

      (current-charging-fx/clear-fx-owner! [:ctx "ctx-block"])
      (is (nil? (get-in (current-charging-fx/fx-snapshot)
                        [:states [:ctx "ctx-block"]])))
      (is (= [:mcmod/start-loop-sound :mcmod/stop-loop-sound]
             (mapv first @effects*))))))

(deftest two-owners-keep-current-charging-state-independent-test
  (let [effects* (atom [])]
    (current-charging-fx/reset-fx-for-test!)
    (with-redefs [client-bridge/run-client-effect! (fn [effect-key payload]
                                                       (swap! effects* conj [effect-key payload])
                                                       nil)]
      (invoke-hand-enqueue! "ctx-a" :current-charging/fx-start {:mode :start :is-item false})
      (invoke-hand-enqueue! "ctx-b" :current-charging/fx-start {:mode :start :is-item true})
      (invoke-hand-enqueue! "ctx-a" :current-charging/fx-update {:mode :update :good? true :charge-ticks 10})
      (invoke-hand-enqueue! "ctx-b" :current-charging/fx-update {:mode :update :good? false :charge-ticks 30})
      (let [snapshot (current-charging-fx/fx-snapshot)
            state-a (get (:states snapshot) [:ctx "ctx-a"])
            state-b (get (:states snapshot) [:ctx "ctx-b"])]
        (is (= #{[:ctx "ctx-a"] [:ctx "ctx-b"]}
               (set (keys (:states snapshot)))))
        (is (= 10 (:charge-ticks state-a)))
        (is (= 0.25 (:charge-ratio state-a)))
        (is (= 30 (:charge-ticks state-b)))
        (is (= 0.75 (:charge-ratio state-b))))
      (invoke-hand-enqueue! "ctx-a" :current-charging/fx-end {:mode :end :is-item false})
      (is (false? (:active? (current-charging-fx/current-state [:ctx "ctx-a"]))))
      (is (nil? (get-in (current-charging-fx/fx-snapshot)
                        [:states [:ctx "ctx-a"]])))
      (is (true? (:active? (current-charging-fx/current-state [:ctx "ctx-b"]))))
      (current-charging-fx/clear-fx-owner! [:ctx "ctx-a"])
      (let [snapshot (current-charging-fx/fx-snapshot)]
        (is (nil? (get (:states snapshot) [:ctx "ctx-a"])))
        (is (= 30 (:charge-ticks (get (:states snapshot) [:ctx "ctx-b"]))))))
    ;; Two starts plus the ended owner's immediate stop.
    (is (= 3 (count @effects*)))
    (is (= [:mcmod/start-loop-sound
            :mcmod/start-loop-sound
            :mcmod/stop-loop-sound]
           (mapv first @effects*)))))

(deftest effective-block-plan-emits-surround-arcs-test
  (invoke-hand-enqueue! "ctx-block-ring" :current-charging/fx-start
    {:mode :start
     :is-item false
     :caster-pos {:x 0.5 :y 65.62 :z 0.5}
     :target {:x 0.5 :y 64.5 :z 10.0}})
  (invoke-hand-enqueue! "ctx-block-ring" :current-charging/fx-update
    {:mode :update
     :is-item false
     :good? true
     :charge-ticks 1
     :caster-pos {:x 0.5 :y 65.62 :z 0.5}
     :target {:x 0.5 :y 64.5 :z 10.0}
     :block-pos [0 64 10]})
  (let [immediate-plan
        (level-effects/build-level-effect-plan
         {:x 0.5 :y 65.62 :z 0.5}
         {:player-uuid "local-player"}
         0
         (fn [& _] []))
        immediate-parts (set (keep :effect-part (:ops immediate-plan)))
        parts*
        (reduce
         (fn [parts tick]
           (level-effects/tick-level-effects!)
           (let [plan (level-effects/build-level-effect-plan
                       {:x 0.5 :y 65.62 :z 0.5}
                       {:player-uuid "local-player"}
                       tick
                       (fn [& _] []))]
             (into parts (keep :effect-part (:ops plan)))))
         #{}
         (range 1 13))]
    (is (contains? immediate-parts :current-charging/beam))
    (is (contains? immediate-parts :current-charging/surround))
    (is (contains? parts* :current-charging/beam))
    (is (contains? parts* :current-charging/surround))))

(deftest item-plan-emits-thin-surround-arcs-without-beam-test
  (invoke-hand-enqueue! "ctx-item-ring" :current-charging/fx-start
    {:mode :start
     :is-item true
     :caster-pos {:x 2.0 :y 65.62 :z 2.0}})
  (let [parts*
        (reduce
         (fn [parts tick]
           (level-effects/tick-level-effects!)
           (let [plan (level-effects/build-level-effect-plan
                       {:x 2.0 :y 65.62 :z -2.0}
                       {:player-uuid "local-player"
                        :player-x 2.0
                        :player-y 64.0
                        :player-z 2.0
                        :player-width 0.6
                        :player-height 1.8
                        :player-yaw-rad 0.0}
                       tick
                       (fn [& _] []))]
             (into parts (keep :effect-part (:ops plan)))))
         #{}
         (range 1 13))]
    (is (not (contains? parts* :current-charging/beam)))
    (is (contains? parts* :current-charging/surround))))

(deftest fx-snapshot-default-without-registered-state-test
  (is (= {:states {}}
         (current-charging-fx/fx-snapshot)))
  (is (= false
         (:active? (current-charging-fx/current-state :missing-selector)))))

(def ^:private target-body
  (var-get (ns-resolve 'cn.li.ac.ability.client.fx-templates.arc-beam.impl.current-charging
                       'target-body)))

(deftest surround-cube-wraps-the-whole-multiblock-test
  ;; Upstream EntitySurroundArc(world, x, y, z, 1, 1) is a fixed 1x1x1 cube on
  ;; the hit cell, because upstream can only ever target a multiblock's origin
  ;; cell. This port charges through the controller from any cell, so it sizes
  ;; the cube to the machine the server reported.
  ;;
  ;; CubePointFactory is centered on X/Z only and EntitySurroundArc sits at
  ;; (blockX + .5, blockY, blockZ + .5), so the body origin is bottom-center.
  (is (= {:x 11.0 :y 64.0 :z 11.0
          :width 2.0 :height 2.0 :depth 2.0
          :yaw-rad 0.0}
         (target-body [10 64 10] [10 64 10 11 65 11])))
  (testing "no extent reported -> upstream's exact 1x1x1 cube on the hit cell"
    (is (= {:x 0.5 :y 64.0 :z 10.5
            :width 1.0 :height 1.0 :depth 1.0
            :yaw-rad 0.0}
           (target-body [0 64 10] nil)))
    (is (= (target-body [0 64 10] nil)
           (target-body [0 64 10] [1 2 3]))
        "a malformed extent must not be trusted")))

(deftest surround-sparks-render-depth-read-only-test
  ;; SubArcHandler.drawAll wraps the batch in glDepthMask(false): the sparks
  ;; depth-TEST against the world but never write depth, so overlapping arcs
  ;; blend instead of punching holes through each other. EntityArc's renderer
  ;; has no such call, so the beam keeps depth write.
  (invoke-hand-enqueue! "ctx-depth" :current-charging/fx-start
    {:mode :start :is-item false
     :caster-pos {:x 0.5 :y 65.62 :z 0.5}
     :target {:x 0.5 :y 64.5 :z 10.0}})
  (invoke-hand-enqueue! "ctx-depth" :current-charging/fx-update
    {:mode :update :is-item false :good? true :charge-ticks 1
     :caster-pos {:x 0.5 :y 65.62 :z 0.5}
     :target {:x 0.5 :y 64.5 :z 10.0}
     :block-pos [0 64 10]})
  (level-effects/tick-level-effects!)
  (let [ops (:ops (level-effects/build-level-effect-plan
                   {:x 0.5 :y 65.62 :z 0.5}
                   {:player-uuid "local-player"}
                   1
                   (fn [& _] [])))
        surround (filter #(= :current-charging/surround (:effect-part %)) ops)
        beam (filter #(= :current-charging/beam (:effect-part %)) ops)]
    (is (seq surround))
    (is (every? :no-depth-write? surround))
    (is (not-any? :no-depth-write? beam))))

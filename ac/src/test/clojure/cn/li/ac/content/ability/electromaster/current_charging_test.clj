(ns cn.li.ac.content.ability.electromaster.current-charging-test
  (:require [clojure.test :refer [deftest is testing use-fixtures]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.content.ability.electromaster.current-charging :as current-charging]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.mcmod.block.multiblock-core :as multiblock]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as position]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world :as world]))

(defn- skill-actions []
  (:actions (var-get (ns-resolve 'cn.li.ac.content.ability.electromaster.current-charging
                                 'current_charging_skill))))

(defn- skill-def []
  (var-get (ns-resolve 'cn.li.ac.content.ability.electromaster.current-charging
                       'current_charging_skill)))

(defn- context-mocks
  [ctx-id seed & {:keys [terminate-calls*]}]
  (let [contexts* (atom {ctx-id seed})
        get-context (fn
                      ([id] (get @contexts* id))
                      ([_owner id] (get @contexts* id)))
        update-skill-state-root! (fn [id f & args]
                                   (swap! contexts* update id
                                          (fn [ctx-data]
                                            (let [current (or (:skill-state ctx-data) {})
                                                  next-state (if (and (= f identity) (= 1 (count args)))
                                                               (first args)
                                                               (apply f current args))]
                                              (assoc ctx-data :skill-state next-state))))
                                   nil)
        assoc-skill-state! (fn [id k v]
                             (swap! contexts* update id
                                    (fn [ctx-data]
                                      (let [path (if (vector? k) k [k])]
                                        (update ctx-data :skill-state #(assoc-in (or % {}) path v)))))
                             nil)
        clear-skill-state! (fn [id]
                             (swap! contexts* update id dissoc :skill-state)
                             nil)
        terminate-context! (fn
                             ([id _]
                              (when terminate-calls*
                                (swap! terminate-calls* conj id))
                              nil)
                             ([_owner id _]
                              (when terminate-calls*
                                (swap! terminate-calls* conj id))
                              nil))]
    {:contexts* contexts*
     :ctx-id ctx-id
     :get-context get-context
     :update-skill-state-root! update-skill-state-root!
     :assoc-skill-state! assoc-skill-state!
     :clear-skill-state! clear-skill-state!
     :terminate-context! terminate-context!}))

(defn- with-charging-env [f]
  (skill-ctx/with-server-skill-context f))

(use-fixtures :each (fn [f]
                      (ps-fix/with-test-player-state-owner
                        #(with-charging-env f))))

(deftest pattern-is-hold-channel-test
  (is (= :hold-channel (:pattern (skill-def)))))

(deftest developer-part-resolves-to-controller-energy-tile-test
  (let [resolve-target
        (var-get
         (ns-resolve 'cn.li.ac.content.ability.electromaster.current-charging
                     'resolve-energy-target-tile))
        routed* (atom [])]
    (with-redefs [platform-be/be-get-world-safe (constantly :server-level)
                  position/block-pos (constantly :developer-part-pos)
                  platform-be/get-block-id (constantly "developer-normal-part")
                  multiblock/resolve-controller-pos
                  (fn [ctx]
                    (swap! routed* conj ctx)
                    :developer-controller-pos)
                  world/get-tile-entity
                  (fn [level pos]
                    (when (= [level pos]
                             [:server-level :developer-controller-pos])
                      :developer-controller))]
      (is (= :developer-controller (resolve-target :developer-part)))
      (is (= [{:world :server-level
               :pos :developer-part-pos
               :block-id "developer-normal-part"}]
             @routed*)))))

(deftest down-action-initializes-state-and-starts-fx-test
  (let [ctx-id "ctx-1"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {})
        fx* (atom [])
        down! (get (skill-actions) :down!)]
    (with-redefs [current-charging/main-hand-item (fn [_] :stack)
                  current-charging/player-view (fn [_] {:world-id "minecraft:overworld"
                                                         :x 1.0 :y 65.0 :z 2.0
                                                         :look-x 0.0 :look-y 0.0 :look-z 1.0})
                  current-charging/cfg-lerp (fn [_ _] 52.0)
                  skill-effects/get-player-state
                  (fn [_] {:resource-data {:cur-overload 82.0}})
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload])
                             nil)]
      (cb/apply-invoke down! :player-id "p1" :ctx-id ctx-id :exp 0.4))
    (is (= {:mode :item
            :is-item true
            :good? false
            :exp 0.4
            :charge-ticks 0
            :overload-floor 82.0
            :target nil
            :caster-pos {:x 1.0 :y 65.0 :z 2.0}
            :block-pos nil
            :charged 0.0}
           (get-in @contexts* [ctx-id :skill-state])))
    (is (= [[ctx-id :current-charging/fx-start {:is-item true
                                                :target nil
                                                :caster-pos {:x 1.0 :y 65.0 :z 2.0}
                                                :source-player-id "p1"}]] @fx*))))

(deftest block-down-seeds-visible-arc-endpoint-test
  (let [ctx-id "ctx-block-start"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {})
        fx* (atom [])
        down! (get (skill-actions) :down!)]
    (with-redefs [current-charging/main-hand-item (constantly nil)
                  current-charging/player-view
                  (constantly {:world-id "minecraft:overworld"
                               :x 1.0 :y 65.0 :z 2.0
                               :look-x 0.0 :look-y 0.0 :look-z 1.0})
                  current-charging/cfg-lerp (fn [_ _] 52.0)
                  current-charging/cfg-double (constantly 15.0)
                  skill-effects/get-player-state
                  (constantly {:resource-data {:cur-overload 82.0}})
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload])
                             nil)]
      (cb/apply-invoke down! :player-id "p1" :ctx-id ctx-id :exp 0.4))
    (let [target {:x 1.0 :y 65.0 :z 17.0}]
      (is (= target (get-in @contexts* [ctx-id :skill-state :target])))
      (is (= [[ctx-id :current-charging/fx-start
               {:is-item false
                :target target
                :caster-pos {:x 1.0 :y 65.0 :z 2.0}
                :source-player-id "p1"}]]
             @fx*)))))

(deftest multiblock-target-reports-structure-bounds-test
  ;; Upstream can only ever target a multiblock's origin cell (the other cells
  ;; hold a bare TileMulti and fail EnergyBlockHelper.isSupported), so its
  ;; surround arc is always a 1x1x1 cube on that one cell. This port charges
  ;; through the controller from any cell, so it also reports the machine's
  ;; extent and the client wraps the whole structure instead of hopping between
  ;; cells with the crosshair.
  (let [bounds-of
        (var-get
         (ns-resolve 'cn.li.ac.content.ability.electromaster.current-charging
                     'target-structure-bounds))
        asked* (atom [])]
    (with-redefs [platform-be/be-get-world-safe (constantly :server-level)
                  position/block-pos (constantly :developer-part-pos)
                  platform-be/get-block-id (constantly "developer-normal-part")
                  multiblock/structure-bounds
                  (fn [ctx]
                    (swap! asked* conj ctx)
                    [10 64 -3 11 65 -2])]
      (is (= [10 64 -3 11 65 -2] (bounds-of :developer-part)))
      (is (= [{:world :server-level
               :pos :developer-part-pos
               :block-id "developer-normal-part"}]
             @asked*))))
  (testing "a plain block reports no extent, so the client keeps upstream's 1x1x1"
    (let [bounds-of
          (var-get
           (ns-resolve 'cn.li.ac.content.ability.electromaster.current-charging
                       'target-structure-bounds))]
      (with-redefs [platform-be/be-get-world-safe (constantly :server-level)
                    position/block-pos (constantly :plain-pos)
                    platform-be/get-block-id (constantly "metal-former")
                    multiblock/structure-bounds (constantly nil)]
        (is (nil? (bounds-of :plain-block)))))))

(deftest tick-item-path-updates-state-exp-and-fx-test
  (let [ctx-id "ctx-item"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {:skill-state {:mode :item
                                              :is-item true
                                              :exp 0.5
                                              :charge-ticks 0
                                              :overload-floor 40.0}})
        fx* (atom [])
        exp* (atom [])
        charge* (atom [])
        floor* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-redefs [current-charging/main-hand-item (fn [_] :stack)
                  current-charging/cfg-lerp (fn [_ _] 30.0)
                  current-charging/cfg-double (fn [k]
                                               (case k
                                                 :progression.exp-effective 0.0001
                                                 :progression.exp-ineffective 0.00003
                                                 0.0))
                  energy/is-energy-item-supported? (fn [_] true)
                  energy/charge-energy-to-item (fn [stack amt sim?]
                                                (swap! charge* conj [stack amt sim?])
                                                nil)
                  current-charging/player-view
                  (fn [_] {:x 4.0 :y 65.0 :z 6.0})
                  skill-effects/add-skill-exp! (fn [pid sid amount]
                                                 (swap! exp* conj [pid sid amount])
                                                 nil)
                  skill-effects/enforce-overload-floor! (fn [pid floor]
                                                          (swap! floor* conj [pid floor])
                                                          nil)
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload])
                             nil)]
      (cb/apply-invoke tick! :player-id "p1" :ctx-id ctx-id))
    (is (= 1 (get-in @contexts* [ctx-id :skill-state :charge-ticks])))
    (is (true? (get-in @contexts* [ctx-id :skill-state :good?])))
    (is (= [["p1" :current-charging 1.0E-4]] @exp*))
    (is (= [[:stack 30.0 false]] @charge*))
    (is (= [["p1" 40.0]] @floor*))
    (is (= [[ctx-id :current-charging/fx-update
             {:is-item true :good? true :charge-ticks 1 :exp 0.5
              :caster-pos {:x 4.0 :y 65.0 :z 6.0}
              :source-player-id "p1"}]]
           @fx*))))

(deftest item-mode-does-not-charge-when-cp-payment-fails-test
  (let [ctx-id "ctx-item-cost-fail"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {:skill-state {:mode :item
                                              :is-item true
                                              :exp 0.5
                                              :charge-ticks 0
                                              :overload-floor 40.0}})
        charged* (atom [])
        exp* (atom [])
        fx* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-redefs [current-charging/cfg-lerp (fn [_ _] 30.0)
                  current-charging/main-hand-item (fn [_] :stack)
                  energy/charge-energy-to-item (fn [& args] (swap! charged* conj args))
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp* conj args))
                  skill-effects/enforce-overload-floor! (fn [& _] nil)
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [& args] (swap! fx* conj args))]
      (cb/apply-invoke tick!
                       :player-id "p1"
                       :ctx-id ctx-id
                       :cost-ok? false))
    (is (= 1 (get-in @contexts* [ctx-id :skill-state :charge-ticks])))
    (is (empty? @charged*))
    (is (empty? @exp*))
    (is (empty? @fx*))))

(deftest tick-block-path-charges-and-tracks-target-and-caster-test
  (let [ctx-id "ctx-block"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {:skill-state {:mode :block
                                              :is-item false
                                              :exp 0.5
                                              :charge-ticks 5
                                              :overload-floor 50.0}})
        fx* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-redefs [current-charging/cfg-lerp (fn [_ _] 20.0)
                  current-charging/cfg-double (fn [k]
                                               (case k
                                                 :targeting.range 5.0
                                                 :progression.exp-effective 0.0001
                                                 0.0))
                  raycast/available? (constantly true)
                  raycast/raycast-blocks (fn [_ _ _ _ _ _ _ _]
                                            {:x 1 :y 2 :z 3 :distance 2.0})
                  raycast/raycast-from-player (fn [& _] nil)
                  current-charging/block-entity-at (fn [_ _ _ _] :block-entity)
                  energy/is-node-supported? (fn [_] true)
                  energy/charge-node (fn [_ charge _sim?] (- charge 8.0))
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  skill-effects/enforce-overload-floor! (fn [& _] nil)
                  current-charging/player-view (fn [_]
                                             {:world-id "minecraft:overworld"
                                              :x 0.0 :y 64.0 :z 0.0
                                              :look-x 1.0 :look-y 0.0 :look-z 0.0})
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload])
                             nil)]
      ;; Upstream block mode charges before checking this tick's CP result.
      (cb/apply-invoke tick!
                       :player-id "p1"
                       :ctx-id ctx-id
                       :cost-ok? false))
    (is (= true (get-in @contexts* [ctx-id :skill-state :good?])))
    (is (= {:x 0.0 :y 64.0 :z 0.0} (get-in @contexts* [ctx-id :skill-state :caster-pos])))
    (is (= [1 2 3] (get-in @contexts* [ctx-id :skill-state :block-pos])))
    (is (= [[ctx-id :current-charging/fx-update
             {:is-item false
              :good? true
              :charged 8.0
              :charge-ticks 6
              :target {:x 2.0 :y 64.0 :z 0.0}
              :caster-pos {:x 0.0 :y 64.0 :z 0.0}
              :block-pos [1 2 3]
              ;; plain block: no multiblock extent to report
              :block-bounds nil
              :source-player-id "p1"}]]
           @fx*))))

(deftest entity-hit-obstructs-block-charging-without-taking-damage-test
  (let [ctx-id "ctx-entity-obstruction"
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {:skill-state {:mode :block
                                              :is-item false
                                              :exp 0.5
                                              :charge-ticks 0
                                              :overload-floor 50.0}})
        block-lookups* (atom [])
        charges* (atom [])
        exp* (atom [])
        fx* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-redefs [current-charging/cfg-lerp (fn [_ _] 20.0)
                  current-charging/cfg-double
                  (fn [k]
                    (case k
                      :targeting.range 15.0
                      :progression.exp-effective 0.0001
                      :progression.exp-ineffective 0.00003
                      0.0))
                  current-charging/player-view
                  (fn [_] {:world-id "minecraft:overworld"
                           :x 0.0 :y 64.0 :z 0.0
                           :look-x 1.0 :look-y 0.0 :look-z 0.0})
                  raycast/available? (constantly true)
                  raycast/raycast-blocks
                  (fn [& _] {:x 5 :y 64 :z 0 :distance 5.0
                             :hit-x 5.0 :hit-y 64.0 :hit-z 0.0})
                  raycast/raycast-from-player
                  (fn [& _] {:entity-id 42 :distance 2.0
                             :x 2.4 :y 64.0 :z 0.0
                             :hit-x 2.0 :hit-y 64.0 :hit-z 0.0
                             :eye-height 1.62})
                  current-charging/block-entity-at
                  (fn [& args] (swap! block-lookups* conj args) :block-entity)
                  energy/charge-node (fn [& args] (swap! charges* conj args) 0.0)
                  energy/charge-receiver (fn [& args] (swap! charges* conj args) 0.0)
                  skill-effects/add-skill-exp! (fn [& args] (swap! exp* conj args))
                  skill-effects/enforce-overload-floor! (fn [& _] nil)
                  ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload]))]
      (cb/apply-invoke tick!
                       :player-id "p1"
                       :ctx-id ctx-id
                       :cost-ok? false))
    ;; Block mode performs its charge/exp attempt before the failed CP payment
    ;; ends the context. The nearer entity only obstructs the ray.
    (is (empty? @block-lookups*))
    (is (empty? @charges*))
    (is (= [["p1" :current-charging 3.0E-5]] @exp*))
    (is (false? (get-in @contexts* [ctx-id :skill-state :good?])))
    (is (nil? (get-in @contexts* [ctx-id :skill-state :block-pos])))
    (is (= {:x 2.4 :y 65.62 :z 0.0}
           (get-in @contexts* [ctx-id :skill-state :target])))
    (is (= {:x 2.4 :y 65.62 :z 0.0}
           (get-in (first @fx*) [2 :target])))))

(deftest cost-fail-sends-end-and-terminates-test
  (let [ctx-id "ctx-fail"
        terminated* (atom [])
        {:keys [contexts* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks ctx-id {:skill-state {:is-item false :caster-pos {:x 1.0 :y 64.0 :z 2.0}}}
                       :terminate-calls* terminated*)
        fx* (atom [])
        cost-fail! (get (skill-actions) :cost-fail!)]
    (with-redefs [ctx/get-context get-context
                  ctx/terminate-context! terminate-context!
                  ctx-skill/update-skill-state-root! update-skill-state-root!
                  ctx-skill/assoc-skill-state! assoc-skill-state!
                  ctx-skill/clear-skill-state! clear-skill-state!
                  fx/send! (fn [id entry _evt payload]
                             (swap! fx* conj [id (:topic entry) payload])
                             nil)]
      (cb/apply-invoke cost-fail! :ctx-id ctx-id))
    (is (= [[ctx-id :current-charging/fx-end {:is-item false :caster-pos {:x 1.0 :y 64.0 :z 2.0}}]] @fx*))
    (is (= [ctx-id] @terminated*))
    (is (nil? (get-in @contexts* [ctx-id :skill-state])))))

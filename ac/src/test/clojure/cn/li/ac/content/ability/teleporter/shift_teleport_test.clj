(ns cn.li.ac.content.ability.teleporter.shift-teleport-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.content.ability.teleporter.shift-teleport :as shift]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

;; Redef sets are composed with `merge` and handed to `with-redefs-fn`; the
;; vector form of `with-redefs` cannot take a computed binding list.

(defn- test-context-owner
  [player-uuid]
  {:logical-side :server :server-session-id :test-session :player-uuid (str player-uuid)})

(defn- make-context-mocks [initial-ctx]
  (let [ctx* (atom initial-ctx)]
    {:ctx* ctx*
     :get-context (fn [& _] @ctx*)
     :update-skill-state-root! (fn [_ f & args]
                                 (swap! ctx* update :skill-state
                                        (fn [ss]
                                          (if (and (= f identity) (= 1 (count args)))
                                            (first args)
                                            (apply f (or ss {}) args)))))}))

(defn- shift-tp-platform-redefs [block-hit entities]
  {#'raycast/available? (constantly true)
   #'raycast/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0 :eye-y 65.6})
   ;; resolve-target reads the precise :hit-* coords for the drop point
   #'raycast/raycast-blocks (fn [& _]
                              (when block-hit
                                (merge {:hit-x (double (:x block-hit))
                                        :hit-y (double (:y block-hit))
                                        :hit-z (double (:z block-hit))}
                                       block-hit)))
   #'world-effects/available? (constantly true)
   #'world-effects/play-sound! (fn [& _] nil)
   #'world-effects/find-entities-in-aabb (fn [& _] entities)})

;; try-place-or-drop! gates on the block-manipulation platform: placing needs
;; destruction allowed, a free destination and break permission on the hit block.
(defn- placement-redefs
  [& {:keys [can-place?] :or {can-place? true}}]
  {#'bm/destroy-allowed? (constantly true)
   #'bm/block-collidable? (constantly (not can-place?))
   #'bm/can-break-block? (constantly true)})

(defn- shift-tp-trace
  [entities & {:keys [eye line drop dest place hit-block face target-hit?]
               :or {eye {:x 1.0 :y 65.6 :z 3.0}
                    line {:x 1.0 :y 64.0 :z 3.0}
                    drop [20.5 65.0 21.5]
                    dest [20.5 65.0 21.5]
                    place [20 65 21]
                    hit-block [20 64 21]
                    face :up
                    target-hit? true}}]
  {:world-id "minecraft:overworld"
   :eye-pos eye
   ;; fx-perform anchors on the player body, not the eye
   :line-pos line
   :drop-x (nth drop 0) :drop-y (nth drop 1) :drop-z (nth drop 2)
   :dest-x (nth dest 0) :dest-y (nth dest 1) :dest-z (nth dest 2)
   :place-x (nth place 0) :place-y (nth place 1) :place-z (nth place 2)
   :hit-block-x (nth hit-block 0) :hit-block-y (nth hit-block 1) :hit-block-z (nth hit-block 2)
   :face face
   :target-hit? target-hit?
   :range 25.0
   :exp 0.5
   :entities entities})

(defn- shift-tp-ctx-trace-redef [trace]
  (make-context-mocks {:skill-state {:trace trace}}))

(deftest shift-tp-up-place-success-hit-critical-emits-crit-fx-test
  (let [{:keys [get-context]} (shift-tp-ctx-trace-redef
                               (shift-tp-trace [{:uuid "enemy-1" :x 12.0 :y 64.9 :z 13.4 :width 0.6 :height 1.8}
                                                {:uuid "enemy-2" :x 13.0 :y 64.8 :z 14.3 :width 0.6 :height 1.8}]))
        exp-calls* (atom [])
        cooldown-calls* (atom [])
        fx-calls* (atom [])
        damage-calls* (atom [])
        consume-calls* (atom [])]
    (with-redefs-fn
      (merge
        {#'ctx/get-context get-context
         #'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ field _]
                                      (case field
                                        :targeting.range 25.0
                                        :combat.damage 20.0
                                        :cost.up.cp 260.0
                                        :cost.up.overload 40.0
                                        :cooldown.ticks 18.0
                                        0.0))
         #'skill-config/tunable-double (fn [_ field]
                                         (case field
                                           :targeting.eye-height 1.6
                                           :progression.exp-base 0.002
                                           0.0))
         #'skill-config/lerp-int (fn [& _] 18)
         #'helper/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'world-effects/available? (constantly false)
         #'entity/player-main-hand-placeable-block? (fn [_] true)
         #'entity/player-creative? (fn [_] false)
         #'entity/player-drop-main-hand-item-at! (fn [& _] false)
         #'entity/player-place-main-hand-block-at-hit! (fn [_ _ _ _ _ _]
                                                         {:placed? true
                                                          :fallback-drop? false
                                                          :pos {:x 20 :y 64 :z 21}
                                                          :face :up})
         #'entity/player-consume-main-hand-item! (fn [_ n]
                                                   (swap! consume-calls* conj n)
                                                   true)
         #'helper/deal-skill-damage! (fn [_player-id _skill-id world-id entity-uuid damage]
                                       (swap! damage-calls* conj [world-id entity-uuid damage])
                                       {:critical? (= entity-uuid "enemy-1")
                                        :crit-level 1
                                        :crit-rate (if (= entity-uuid "enemy-1") 1.6 1.0)
                                        :message-key (when (= entity-uuid "enemy-1") "ability.teleporter.critical_hit")
                                        :message-args (when (= entity-uuid "enemy-1") ["x1.6"])
                                        :damage-after damage
                                        :applied? true})
         #'skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                          (swap! exp-calls* conj [player-id skill-id amount])
                                          nil)
         #'skill-effects/set-main-cooldown! (fn [player-id skill-id ticks]
                                              (swap! cooldown-calls* conj [player-id skill-id ticks])
                                              nil)
         #'fx/send! (fn [_ctx-id entry _evt payload]
                      (swap! fx-calls* conj [(:topic entry) payload])
                      nil)}
        (placement-redefs))
      (fn []
        (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-1" :player-ref :player :cost-ok? true)))
    (is (= [["minecraft:overworld" "enemy-1" 20.0]
            ["minecraft:overworld" "enemy-2" 20.0]]
           @damage-calls*))
    (is (= [1] @consume-calls*))
    (is (= [["p1" :shift-teleport 0.006]] @exp-calls*))
    (is (= [["p1" :shift-teleport 18]] @cooldown-calls*))
    ;; fx-perform goes out before resource settlement (owner + nearby), so the
    ;; crit fx from the damage loop trails it.
    (is (= [:shift-teleport/fx-perform :shift-teleport/fx-perform :teleporter/fx-crit-hit]
           (mapv first @fx-calls*)))
    (is (= {:x 12.0
            :y 64.9
            :z 13.4
            :crit-level 1
            :crit-rate 1.6
            :message-key "ability.teleporter.critical_hit"
            :message-args ["x1.6"]
            :target-uuid "enemy-1"
            :skill-id :shift-teleport}
           (second (nth @fx-calls* 2))))))

;; crit-applied? keys off :critical? alone — upstream fires the crit event before
;; ctx.attack, so armor/invulnerability rejecting the hit must not cancel the fx.
(deftest shift-tp-up-critical-fx-survives-rejected-damage-test
  (let [{:keys [get-context]} (shift-tp-ctx-trace-redef
                               (shift-tp-trace [{:uuid "enemy-1" :x 12.0 :y 64.9 :z 13.4 :width 0.6 :height 1.8}]))
        fx-calls* (atom [])]
    (with-redefs-fn
      (merge
        {#'ctx/get-context get-context
         #'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ _ _] 20.0)
         #'skill-config/tunable-double (fn [_ field]
                                         (case field
                                           :targeting.eye-height 1.6
                                           :progression.exp-base 0.002
                                           0.0))
         #'skill-config/lerp-int (fn [& _] 18)
         #'helper/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'world-effects/available? (constantly false)
         #'entity/player-main-hand-placeable-block? (fn [_] true)
         #'entity/player-creative? (fn [_] false)
         #'entity/player-drop-main-hand-item-at! (fn [& _] false)
         #'entity/player-place-main-hand-block-at-hit! (fn [& _]
                                                         {:placed? true
                                                          :fallback-drop? false
                                                          :pos {:x 20 :y 64 :z 21}
                                                          :face :up})
         #'entity/player-consume-main-hand-item! (fn [& _] true)
         #'helper/deal-skill-damage! (fn [& _]
                                       {:critical? true
                                        :crit-level 1
                                        :crit-rate 1.5
                                        :applied? false})
         #'skill-effects/add-skill-exp! (fn [& _] nil)
         #'skill-effects/set-main-cooldown! (fn [& _] nil)
         #'fx/send! (fn [_ctx-id entry _evt payload]
                      (swap! fx-calls* conj [(:topic entry) payload])
                      nil)}
        (placement-redefs))
      (fn []
        (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-1b" :player-ref :player :cost-ok? true)))
    (let [perform {:from-x 1.0
                   :from-y 63.5
                   :from-z 3.0
                   :x 20.5
                   :y 65.0
                   :z 21.5
                   :target-count 1}]
      (is (= [[:shift-teleport/fx-perform perform]
              [:shift-teleport/fx-perform perform]]
             (vec (take 2 @fx-calls*))))
      (is (= :teleporter/fx-crit-hit (first (nth @fx-calls* 2)))))))

(deftest shift-tp-up-cost-fail-emits-fx-but-no-side-effects-test
  (let [exp-calls* (atom 0)
        cooldown-calls* (atom 0)
        fx-calls* (atom 0)
        damage-calls* (atom 0)
        place-calls* (atom 0)]
    (with-redefs-fn
      (merge
        {#'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ _ _] 20.0)
         #'skill-config/tunable-double (fn [_ _] 1.6)
         #'helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'entity/player-main-hand-placeable-block? (fn [_] true)
         #'entity/player-creative? (fn [_] false)
         #'entity/player-drop-main-hand-item-at! (fn [& _] true)
         #'entity/player-place-main-hand-block-at-hit! (fn [& _]
                                                         (swap! place-calls* inc)
                                                         {:placed? true
                                                          :fallback-drop? false
                                                          :pos {:x 4 :y 5 :z 6}
                                                          :face :up})
         #'entity/player-consume-main-hand-item! (fn [& _] true)
         #'helper/deal-skill-damage! (fn [& _] (swap! damage-calls* inc))
         #'skill-effects/add-skill-exp! (fn [& _] (swap! exp-calls* inc))
         #'skill-effects/set-main-cooldown! (fn [& _] (swap! cooldown-calls* inc))
         #'fx/send! (fn [& _] (swap! fx-calls* inc) nil)}
        (placement-redefs)
        (shift-tp-platform-redefs {:x 4 :y 5 :z 6 :face :up} []))
      (fn []
        (ctx/with-context-owner (test-context-owner "p1")
          (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-2" :player-ref :player :cost-ok? false))))

    ;; The block trail still shows (owner + nearby) — only settlement is skipped.
    (is (= 2 @fx-calls*))
    (is (= 0 @place-calls*))
    (is (= 0 @damage-calls*))
    (is (= 0 @exp-calls*))
    (is (= 0 @cooldown-calls*))))

(deftest shift-tp-up-place-fail-fallback-drop-test
  (let [{:keys [get-context]} (shift-tp-ctx-trace-redef
                               (shift-tp-trace []
                                               :eye {:x 1.0 :y 3.6 :z 3.0}
                                               :drop [8.5 10.0 10.5]
                                               :dest [8.5 10.0 10.5]
                                               :place [8 10 10]))
        drop-calls* (atom [])
        consume-calls* (atom 0)]
    (with-redefs-fn
      (merge
        {#'ctx/get-context get-context
         #'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ _ _] 20.0)
         #'skill-config/tunable-double (fn [_ field]
                                         (case field
                                           :targeting.eye-height 1.6
                                           :progression.exp-base 0.002
                                           0.0))
         #'skill-config/lerp-int (fn [& _] 30)
         #'helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'entity/player-main-hand-placeable-block? (fn [_] true)
         #'entity/player-creative? (fn [_] false)
         #'entity/player-drop-main-hand-item-at! (fn [_ n x y z]
                                                   (swap! drop-calls* conj [n x y z])
                                                   true)
         #'entity/player-consume-main-hand-item! (fn [& _]
                                                   (swap! consume-calls* inc)
                                                   true)
         #'world-effects/available? (constantly false)
         #'helper/deal-skill-damage! (fn [& _] {:critical? false})
         #'skill-effects/add-skill-exp! (fn [& _] nil)
         #'skill-effects/set-main-cooldown! (fn [& _] nil)
         #'fx/send! (fn [& _] nil)}
        ;; Destination is occupied → placement is off the table, so the item is
        ;; dropped and the drop itself counts as the consume.
        (placement-redefs :can-place? false))
      (fn []
        (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-3" :player-ref :player :cost-ok? true)))
    (is (= [[1 8.5 10.0 10.5]] @drop-calls*))
    (is (= 0 @consume-calls*))))

(deftest shift-tp-up-invalid-main-hand-skips-execution-test
  (let [fx-calls* (atom 0)]
    (with-redefs-fn
      (merge
        {#'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ _ _] 20.0)
         #'skill-config/tunable-double (fn [_ _] 1.6)
         #'helper/player-position (fn [_] {:x 1.0 :y 2.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'entity/player-main-hand-placeable-block? (fn [_] false)
         #'fx/send! (fn [& _] (swap! fx-calls* inc) nil)}
        (shift-tp-platform-redefs {:x 8 :y 9 :z 10 :face :up} []))
      (fn []
        (ctx/with-context-owner (test-context-owner "p1")
          (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-4" :player-ref :player :cost-ok? true))))

    (is (= 0 @fx-calls*))))

(deftest shift-tp-down-initializes-state-regardless-of-cost-test
  ;; Upstream madeAlive has no cost gate — only hand validity (s_madeAlive
  ;; terminates an invalid hand). An invalid hand skips the marker.
  (let [updates* (atom [])
        fx-calls* (atom 0)]
    (with-redefs [ctx-skill/update-skill-state-root! (fn [ctx-id f & args]
                                                       (swap! updates* conj [ctx-id f args])
                                                       nil)
                  entity/player-main-hand-placeable-block? (fn [_] false)
                  fx/send! (fn [& _] (swap! fx-calls* inc) nil)]
      (cb/apply-invoke shift/shift-tp-down! :ctx-id "ctx-a" :cost-ok? false)
      (cb/apply-invoke shift/shift-tp-down! :ctx-id "ctx-b" :cost-ok? true))

    (is (= 2 (count @updates*)))
    (is (= 0 @fx-calls*))))

(deftest shift-tp-down-valid-hand-sends-start-test
  ;; Upstream l_start spawns the block marker on MSG_MADEALIVE; fx-start
  ;; carries the first destination so the marker appears on key-down.
  (let [{:keys [ctx* get-context update-skill-state-root!]} (make-context-mocks {:skill-state {}})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)]
    (with-redefs-fn
      (merge
        {#'ctx/get-context get-context
         #'ctx-skill/update-skill-state-root! update-skill-state-root!
         #'fx/send! send!
         #'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ field _]
                                      (case field
                                        :targeting.range 25.0
                                        :targeting.eye-height 1.6
                                        0.0))
         #'helper/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'raycast/available? (constantly true)
         #'raycast/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0 :eye-y 65.6})
         #'raycast/raycast-blocks (fn [& _]
                                    {:face :up
                                     :x 1 :y 64 :z 5
                                     :hit-x 1.5 :hit-y 64.0 :hit-z 5.5})
         #'world-effects/available? (constantly false)
         #'entity/player-main-hand-placeable-block? (fn [_] true)}
        (placement-redefs))
      (fn []
        (ctx/with-context-owner (test-context-owner "p1")
          (cb/apply-invoke shift/shift-tp-down! :ctx-id "ctx-c" :player-id "p1"
                           :player-ref :player :cost-ok? true))))

    (is (= :shift-teleport/fx-start (get-in (first @calls*) [1])))
    (let [payload (get-in (first @calls*) [3])]
      ;; Block hit at (1,64,5) with :up face -> dest block (1,65,5), centered.
      (is (= 1.5 (:x payload)))
      (is (= 65.0 (:y payload)))
      (is (= 5.5 (:z payload)))
      (is (true? (:target-hit? payload)))
      (is (empty? (:entities payload))))))

(deftest shift-tp-tick-invalid-main-hand-clears-trace-and-skips-fx-test
  (let [updates* (atom [])
        fx-calls* (atom 0)]
    (with-redefs [entity/player-main-hand-placeable-block? (fn [_] false)
                  ctx/terminate-context! (fn [& _] nil)
                  ctx-skill/update-skill-state-root! (fn [ctx-id f & args]
                                                       (swap! updates* conj [ctx-id f args])
                                                       nil)
                  fx/send! (fn [& _] (swap! fx-calls* inc) nil)]
      (cb/apply-invoke shift/shift-tp-tick! :player-id "p1" :player-ref :player :ctx-id "ctx-tick" :hold-ticks 9))

    (is (= 1 (count @updates*)))
    (is (= 0 @fx-calls*))
    (let [[_ _ args] (first @updates*)]
      (is (= {:hold-ticks 9 :hand-valid? false :trace nil} (first args))))))

(deftest shift-tp-up-creative-mode-skips-consume-test
  (let [{:keys [get-context]} (shift-tp-ctx-trace-redef
                               (shift-tp-trace []
                                               :drop [8.5 65.0 10.5]
                                               :dest [8.5 65.0 10.5]
                                               :place [8 65 10]))
        consume-calls* (atom 0)
        place-calls* (atom 0)]
    (with-redefs-fn
      (merge
        {#'ctx/get-context get-context
         #'skill-effects/skill-exp (fn [_ _] 0.5)
         #'skill-config/lerp-double (fn [_ _ _] 10.0)
         #'skill-config/tunable-double (fn [_ field]
                                         (case field
                                           :targeting.eye-height 1.6
                                           :progression.exp-base 0.002
                                           0.0))
         #'skill-config/lerp-int (fn [& _] 20)
         #'helper/player-position (fn [_] {:x 1.0 :y 64.0 :z 3.0})
         #'helper/player-look-vec (fn [_] {:x 0.0 :y 0.0 :z 1.0})
         #'geom/world-id-of (fn [_] "minecraft:overworld")
         #'entity/player-main-hand-placeable-block? (fn [_] true)
         #'entity/player-creative? (fn [_] true)
         #'entity/player-drop-main-hand-item-at! (fn [& _] false)
         #'entity/player-place-main-hand-block-at-hit! (fn [& _]
                                                         (swap! place-calls* inc)
                                                         {:placed? true
                                                          :fallback-drop? false
                                                          :pos {:x 8 :y 65 :z 10}
                                                          :face :up})
         #'entity/player-consume-main-hand-item! (fn [& _]
                                                   (swap! consume-calls* inc)
                                                   true)
         #'world-effects/available? (constantly false)
         #'helper/deal-skill-damage! (fn [& _] {:critical? false})
         #'skill-effects/add-skill-exp! (fn [& _] nil)
         #'skill-effects/set-main-cooldown! (fn [& _] nil)
         #'fx/send! (fn [& _] nil)}
        (placement-redefs))
      (fn []
        (cb/apply-invoke shift/shift-tp-up! :player-id "p1" :ctx-id "ctx-creative" :player-ref :player :cost-ok? true)))
    (is (= 1 @place-calls*))
    (is (= 0 @consume-calls*))))

(ns cn.li.ac.content.ability.electromaster.mag-movement-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.test.support.skill-context :as skill-ctx]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion-op]
            [cn.li.ac.ability.effects.state :as state-op]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.content.ability.electromaster.mag-movement :as mag-movement]
            [cn.li.mcmod.platform.raycast :as raycast]))

(defn- skill-actions []
  (:actions (var-get (ns-resolve 'cn.li.ac.content.ability.electromaster.mag-movement
                                 'mag-movement-skill))))

(defn- skill-def []
  (var-get (ns-resolve 'cn.li.ac.content.ability.electromaster.mag-movement
                       'mag-movement-skill)))

(defn- context-mocks
  [seed & {:keys [terminate-calls*]}]
  (let [ctx* (atom seed)
        get-context (fn
                      ([_ctx-id] @ctx*)
                      ([_owner _ctx-id] @ctx*))
        update-skill-state-root! (fn [_ctx-id f & args]
                                   (swap! ctx*
                                          (fn [ctx-data]
                                            (let [current (or (:skill-state ctx-data) {})
                                                  next-state (if (and (= f identity) (= 1 (count args)))
                                                               (first args)
                                                               (apply f current args))]
                                              (assoc ctx-data :skill-state next-state))))
                                   nil)
        assoc-skill-state! (fn [_ctx-id k v]
                             (swap! ctx*
                                    (fn [ctx-data]
                                      (let [path (if (vector? k) k [k])]
                                        (update ctx-data :skill-state #(assoc-in (or % {}) path v)))))
                             nil)
        clear-skill-state! (fn [_ctx-id]
                             (swap! ctx* dissoc :skill-state)
                             nil)
        terminate-context! (fn
                             ([ctx-id _]
                              (when terminate-calls*
                                (swap! terminate-calls* conj ctx-id))
                              nil)
                             ([_owner ctx-id _]
                              (when terminate-calls*
                                (swap! terminate-calls* conj ctx-id))
                              nil))]
    {:ctx* ctx*
     :get-context get-context
     :update-skill-state-root! update-skill-state-root!
     :assoc-skill-state! assoc-skill-state!
     :clear-skill-state! clear-skill-state!
     :terminate-context! terminate-context!}))

(defn- with-mag-env [f]
  (skill-ctx/with-server-skill-context f))

(deftest description-ids-normalize-for-vanilla-and-mod-entities-test
  (is (= "minecraft:iron_block"
         (#'mag-movement/normalize-id "block.minecraft.iron_block")))
  (is (= "academy:entity_mag_hook"
         (#'mag-movement/normalize-id "entity.academy.entity_mag_hook"))))

(deftest pattern-is-hold-channel-and-cost-fail-present-test
  (let [spec (skill-def)]
    (is (= :hold-channel (:pattern spec)))
    (is (fn? (get-in spec [:actions :cost-fail!])))
    (is (= 1.0 (skill-config/cp-consume-speed :mag-movement)))
    (is (= 1.0 (skill-config/overload-consume-speed :mag-movement)))))

(deftest creative-mode-matches-original-resource-bypass-test
  (let [spec (skill-def)
        down-overload (get-in spec [:cost :down :overload])
        tick-cp (get-in spec [:cost :tick :cp])]
    (with-redefs [mag-movement/player-creative? (constantly true)]
      (is (zero? (down-overload "p1" :mag-movement 0.5)))
      (is (zero? (tick-cp "p1" :mag-movement 0.5))))))

(deftest cost-fail-down-finalizes-without-exp-test
  (let [ctx-id "ctx-down-fail"
        terminated* (atom [])
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {:skill-state {:has-target false :finalized? false}}
                       :terminate-calls* terminated*)
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp* (atom [])
        cost-fail! (get (skill-actions) :cost-fail!)]
    (with-mag-env
      #(with-redefs [ctx/get-context get-context
                    ctx/terminate-context! terminate-context!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    motion-op/execute-reset-fall-damage! (fn [_evt _params] nil)
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp* conj [player-id skill-id amount])
                                                   nil)]
         (cb/apply-invoke cost-fail! :ctx-id ctx-id :player-id "p1" :cost-stage :down)))
    (is (empty? @exp*))
    (is (= [[ctx-id :mag-movement/fx-end :end nil] [ctx-id :mag-movement/fx-end :end nil]] @calls*))
    (is (= [ctx-id] @terminated*))
    (is (nil? (:skill-state @ctx*)))))

(deftest down-no-target-terminates-with-original-min-exp-test
  (let [ctx-id "ctx-no-target"
        terminated* (atom [])
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {} :terminate-calls* terminated*)
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp* (atom [])
        down! (get (skill-actions) :down!)]
    (with-mag-env
      #(with-redefs [mag-movement/resolve-target (fn [_] nil)
                    mag-movement/player-pos (fn [_] {:x 0.0 :y 0.0 :z 0.0})
                    mag-movement/cfg-double (fn [k]
                                              (case k
                                                :progression.exp-min 0.005
                                                :progression.exp-distance-scale 0.0011
                                                0.0))
                    ctx/get-context get-context
                    ctx/terminate-context! terminate-context!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    motion-op/execute-reset-fall-damage! (fn [_evt _params] nil)
                    skill-effects/add-skill-exp! (fn [& args]
                                                   (swap! exp* conj args)
                                                   nil)]
         (cb/apply-invoke down! :ctx-id ctx-id :player-id "p1" :exp 0.4)))
    (is (= [["p1" :mag-movement 0.005]] @exp*))
    (is (= [[ctx-id :mag-movement/fx-end :end nil] [ctx-id :mag-movement/fx-end :end nil]] @calls*))
    (is (= [ctx-id] @terminated*))
    (is (nil? (:skill-state @ctx*)))))

(deftest tick-cost-fail-finalizes-once-with-exp-test
  (let [ctx-id "ctx-tick-fail"
        terminated* (atom [])
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {:skill-state {:has-target true
                                       :finalized? false
                                       :target-kind :block
                                       :target-x 1.0
                                       :target-y 0.0
                                       :target-z 0.0
                                       :motion-x 0.0
                                       :motion-y 0.0
                                       :motion-z 0.0
                                       :movement-ticks 0
                                       :start-x 0.0
                                       :start-y 0.0
                                       :start-z 0.0
                                       :overload-floor 10.0}}
                       :terminate-calls* terminated*)
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-mag-env
      #(with-redefs [mag-movement/player-pos (fn [_] {:x 3.0 :y 4.0 :z 0.0})
                    mag-movement/cfg-double (fn [k]
                                             (case k
                                               :progression.exp-min 0.005
                                               :progression.exp-distance-scale 0.0011
                                               :movement.acceleration 0.08
                                               0.0))
                    ctx/get-context get-context
                    ctx/terminate-context! terminate-context!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    motion-op/execute-reset-fall-damage! (fn [& _] nil)
                    state-op/execute-overload-floor! (fn [& _] nil)
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp* conj [player-id skill-id amount])
                                                   nil)]
         (cb/apply-invoke tick! :ctx-id ctx-id :player-id "p1" :cost-ok? false)
         (cb/apply-invoke tick! :ctx-id ctx-id :player-id "p1" :cost-ok? false)))
    (is (= 1 (count @exp*)))
    (is (= 2 (count (filter #(= :mag-movement/fx-end (second %)) @calls*))))
    (is (= [ctx-id] @terminated*))
    (is (nil? (:skill-state @ctx*)))))

(deftest tick-target-lost-finalizes-with-exp-test
  (let [ctx-id "ctx-target-lost"
        terminated* (atom [])
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {:skill-state {:has-target true
                                       :finalized? false
                                       :target-kind :entity
                                       :target-world-id "w"
                                       :target-entity-uuid "e1"
                                       :target-x 2.0
                                       :target-y 0.0
                                       :target-z 0.0
                                       :motion-x 0.0
                                       :motion-y 0.0
                                       :motion-z 0.0
                                       :movement-ticks 2
                                       :start-x 0.0
                                       :start-y 0.0
                                       :start-z 0.0
                                       :overload-floor 10.0}}
                       :terminate-calls* terminated*)
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp* (atom [])
        tick! (get (skill-actions) :tick!)]
    (with-mag-env
      #(with-redefs [mag-movement/update-entity-target (fn [_] nil)
                    mag-movement/player-pos (fn [_] {:x 0.0 :y 0.0 :z 5.0})
                    mag-movement/cfg-double (fn [k]
                                             (case k
                                               :progression.exp-min 0.005
                                               :progression.exp-distance-scale 0.0011
                                               0.0))
                    ctx/get-context get-context
                    ctx/terminate-context! terminate-context!
                    ctx-skill/update-skill-state-root! update-skill-state-root!
                    ctx-skill/assoc-skill-state! assoc-skill-state!
                    ctx-skill/clear-skill-state! clear-skill-state!
                    fx/send! send!
                    motion-op/execute-reset-fall-damage! (fn [& _] nil)
                    state-op/execute-overload-floor! (fn [& _] nil)
                    skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                   (swap! exp* conj [player-id skill-id amount])
                                                   nil)]
         (cb/apply-invoke tick! :ctx-id ctx-id :player-id "p1" :cost-ok? true)))
    (is (= 1 (count @exp*)))
    (is (= [[ctx-id :mag-movement/fx-end :end nil] [ctx-id :mag-movement/fx-end :end nil]] @calls*))
    (is (= [ctx-id] @terminated*))
    (is (nil? (:skill-state @ctx*)))))

(deftest resolve-target-uses-hit-vec-and-eye-height-test
  (with-redefs [mag-movement/is-metal-block? (fn [_] true)
                 mag-movement/is-metal-entity? (fn [_] true)
                 raycast/available? (constantly true)
                 raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                 geom/eye-pos (fn [_] {:x 10.0 :y 20.0 :z 30.0})
                 geom/world-id-of (fn [_] "w")
                 mag-movement/cfg-double (fn [k]
                                           (case k
                                             :targeting.range 25.0
                                             0.0))
                 raycast/raycast-blocks (fn [_ _ _ _ _ _ _ _]
                                          {:block-id "minecraft:iron_block"
                                           :x 1.0 :y 2.0 :z 3.0
                                           :hit-x 1.25 :hit-y 2.5 :hit-z 3.75
                                           :distance 4.0})
                 raycast/raycast-from-player (fn [_ _ _] nil)]
    (is (= {:target-kind :block
            :target-world-id "w"
            :target-x 1.25
            :target-y 2.5
            :target-z 3.75
            :target-block-id "minecraft:iron_block"}
           (@#'cn.li.ac.content.ability.electromaster.mag-movement/resolve-target "p1"))))
  (with-redefs [mag-movement/is-metal-block? (fn [_] true)
                 mag-movement/is-metal-entity? (fn [_] true)
                 raycast/available? (constantly true)
                 raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                 geom/eye-pos (fn [_] {:x 10.0 :y 20.0 :z 30.0})
                 geom/world-id-of (fn [_] "w")
                 mag-movement/cfg-double (fn [k]
                                           (case k
                                             :targeting.range 25.0
                                             0.0))
                 raycast/raycast-blocks (fn [_ _ _ _ _ _ _ _]
                                          {:block-id "minecraft:iron_block"
                                           :x 7.0 :y 8.0 :z 9.0
                                           :hit-x 7.5 :hit-y 8.5 :hit-z 9.5
                                           :distance 4.0})
                 raycast/raycast-from-player (fn [_ _ living-only?]
                                               (is (false? living-only?))
                                               {:entity-id "e-1"
                                                :distance 4.0})
                 motion-op/entity-position (fn [world-id entity-uuid]
                                             (is (= "w" world-id))
                                             (is (= "e-1" entity-uuid))
                                             {:x 4.0 :y 5.0 :z 6.0
                                              :eye-height 1.9
                                              :type "entity.minecraft.iron_golem"
                                              :alive? true})]
    (is (= {:target-kind :entity
            :target-world-id "w"
            :target-entity-uuid "e-1"
            :target-entity-type "minecraft:iron_golem"
            :target-x 4.0
            :target-y 6.9
            :target-z 6.0}
            (@#'cn.li.ac.content.ability.electromaster.mag-movement/resolve-target "p1")))))

(deftest resolve-target-recognizes-anchored-mag-hook-from-raycast-type-test
  ;; The original RayTraceResult retains entityHit. The bridge must retain
  ;; enough metadata to make an anchored magnetic hook a valid target.
  (with-redefs [mag-movement/is-metal-entity? (fn [entity-type]
                                                (= "academy:entity_mag_hook" entity-type))
                raycast/available? (constantly true)
                raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                geom/eye-pos (fn [_] {:x 0.0 :y 1.62 :z 0.0})
                geom/world-id-of (fn [_] "w")
                mag-movement/cfg-double (fn [_] 25.0)
                raycast/raycast-blocks (fn [& _] nil)
                raycast/raycast-from-player
                (fn [_ _ living-only?]
                  (is (false? living-only?))
                  {:entity-id "mag-hook-uuid"
                   :type "entity.academy.entity_mag_hook"
                   :x 0.0 :y 5.0 :z 8.0
                   :eye-height 0.0
                   :alive? true
                   :distance 8.0})
                motion-op/entity-position (fn [_ _] nil)]
    (is (= {:target-kind :entity
            :target-world-id "w"
            :target-entity-uuid "mag-hook-uuid"
            :target-entity-type "academy:entity_mag_hook"
            :target-x 0.0
            :target-y 5.0
            :target-z 8.0}
           (@#'cn.li.ac.content.ability.electromaster.mag-movement/resolve-target "p1")))))

(deftest entity-target-refresh-tracks-uuid-without-radius-cutoff-test
  (let [calls* (atom [])
        skill-state {:target-kind :entity
                     :target-world-id "w"
                     :target-entity-uuid "e-1"
                     :target-x 1.0
                     :target-y 2.0
                     :target-z 3.0}]
    (with-redefs [motion-op/entity-position
                  (fn [world-id entity-uuid]
                    (swap! calls* conj [world-id entity-uuid])
                    {:x 101.0 :y 50.0 :z -80.0
                     :eye-height 2.0
                     :alive? true})]
      (is (= (assoc skill-state
                    :target-x 101.0
                    :target-y 52.0
                    :target-z -80.0)
             (@#'cn.li.ac.content.ability.electromaster.mag-movement/update-entity-target
               skill-state))))
    (is (= [["w" "e-1"]] @calls*))))

(deftest down-captures-post-cost-overload-as-recovery-floor-test
  (let [ctx-id "ctx-overload-floor"
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {})
        {:keys [send!]} (fx-mocks/capture-fx-send!)
        down! (get (skill-actions) :down!)]
    (with-mag-env
      #(with-redefs [mag-movement/resolve-target
                     (fn [_]
                       {:target-kind :block
                        :target-world-id "w"
                        :target-x 1.0 :target-y 2.0 :target-z 3.0
                        :target-block-id "minecraft:iron_block"})
                     mag-movement/player-pos (fn [_] {:x 0.0 :y 1.0 :z 2.0})
                     mag-movement/cfg-lerp (fn [_ _] 45.0)
                     motion-op/player-motion-available? (constantly false)
                     skill-effects/get-player-state
                     (fn [_] {:resource-data {:cur-overload 77.0}})
                     ctx/get-context get-context
                     ctx/terminate-context! terminate-context!
                     ctx-skill/update-skill-state-root! update-skill-state-root!
                     ctx-skill/assoc-skill-state! assoc-skill-state!
                     ctx-skill/clear-skill-state! clear-skill-state!
                     fx/send! send!]
         (cb/apply-invoke down! :ctx-id ctx-id :player-id "p1" :exp 0.5)))
    (is (= 77.0 (get-in @ctx* [:skill-state :overload-floor])))))

(deftest fx-payloads-carry-the-caster-eye-for-anchoring-test
  ;; The client's build-plan anchors the arc to the CASTER's eye (upstream
  ;; setFromTo(player.posX, player.posY + 1.6, ...)) plus the ViewOptimize
  ;; shift — without the eye in the payload every viewer drew the arc from
  ;; their own hand, which at close range crossed into the target block and
  ;; vanished. Every fx-start/fx-update must carry :caster-pos.
  (let [ctx-id "ctx-anchor"
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {})
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        down! (get (skill-actions) :down!)
        tick! (get (skill-actions) :tick!)]
    (with-mag-env
      #(with-redefs [mag-movement/resolve-target
                     (fn [_]
                       {:target-kind :block
                        :target-world-id "w"
                        :target-x 1.0 :target-y 2.0 :target-z 3.0
                        :target-block-id "minecraft:iron_block"})
                     mag-movement/player-pos (fn [_] {:x 0.0 :y 1.0 :z 2.0})
                     geom/eye-pos (fn [_] {:x 0.0 :y 2.62 :z 2.0})
                     mag-movement/cfg-lerp (fn [_ _] 45.0)
                     mag-movement/cfg-double (fn [k]
                                               (case k
                                                 :movement.acceleration 0.08
                                                 0.0))
                     motion-op/player-motion-available? (constantly false)
                     skill-effects/get-player-state
                     (fn [_] {:resource-data {:cur-overload 77.0}})
                     state-op/execute-overload-floor! (fn [& _] nil)
                     ctx/get-context get-context
                     ctx/terminate-context! terminate-context!
                     ctx-skill/update-skill-state-root! update-skill-state-root!
                     ctx-skill/assoc-skill-state! assoc-skill-state!
                     ctx-skill/clear-skill-state! clear-skill-state!
                     fx/send! send!]
         (cb/apply-invoke down! :ctx-id ctx-id :player-id "p1" :exp 0.5)
         (cb/apply-invoke tick! :ctx-id ctx-id :player-id "p1" :cost-ok? true)))
    (is (= 6 (count @calls*))
        "down! = start + update, tick! = update — each fanned out to owner + nearby")
    (doseq [[_ _ _ payload] @calls*]
      (is (= {:x 0.0 :y 2.62 :z 2.0} (:caster-pos payload))
          "every fx payload anchors the arc to the caster eye"))))

(deftest up-then-abort-does-not-double-finalize-test
  (let [ctx-id "ctx-up-abort"
        terminated* (atom [])
        {:keys [ctx* get-context update-skill-state-root! assoc-skill-state!
                clear-skill-state! terminate-context!]}
        (context-mocks {:skill-state {:has-target true
                                       :finalized? false
                                       :target-kind :block
                                       :target-x 1.0
                                       :target-y 0.0
                                       :target-z 0.0
                                       :movement-ticks 5
                                       :start-x 0.0
                                       :start-y 0.0
                                       :start-z 0.0}}
                       :terminate-calls* terminated*)
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        exp* (atom [])
        up! (get (skill-actions) :up!)
        abort! (get (skill-actions) :abort!)]
    (with-mag-env
      #(with-redefs-fn {#'mag-movement/player-pos (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                        #'mag-movement/cfg-double (fn [k]
                                                    (case k
                                                      :progression.exp-min 0.005
                                                      :progression.exp-distance-scale 0.0011
                                                      0.0))
                        #'ctx/get-context get-context
                        #'ctx/terminate-context! terminate-context!
                        #'ctx-skill/update-skill-state-root! update-skill-state-root!
                        #'ctx-skill/assoc-skill-state! assoc-skill-state!
                        #'ctx-skill/clear-skill-state! clear-skill-state!
                        #'fx/send! send!
                        #'motion-op/execute-reset-fall-damage! (fn [& _] nil)
                        #'skill-effects/add-skill-exp! (fn [player-id skill-id amount]
                                                         (swap! exp* conj [player-id skill-id amount])
                                                         nil)}
         (fn []
           (cb/apply-invoke up! :ctx-id ctx-id :player-id "p1")
           (cb/apply-invoke abort! :ctx-id ctx-id :player-id "p1"))))
    (is (= 1 (count @exp*)))
    (is (= 2 (count @calls*)) "one logical fx-end event, fanned out to owner + nearby")
    (is (= [ctx-id] @terminated*))))

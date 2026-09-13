(ns cn.li.ac.content.ability.electromaster.mag-manip-test
  (:require [clojure.test :refer [deftest is use-fixtures]]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.ac.ability.test.skill-callback-test-helpers :as cb]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.test.support.fx-mocks :as fx-mocks]
            [cn.li.ac.test.support.player-state :as ps-fix]
            [cn.li.ac.content.ability.electromaster.mag-manip :as mag-manip]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity :as entity]))

(use-fixtures :each ps-fix/clean-player-states-fixture)

(defn- skill-actions []
  (:actions (var-get (ns-resolve 'cn.li.ac.content.ability.electromaster.mag-manip
                                 'mag-manip))))

(defn- mk-context-store [ctx-id seed]
  (atom {ctx-id seed}))

(deftest start-holding-configures-captured-block-body-test
  (let [ctx-id "ctx-configure"
        contexts* (mk-context-store ctx-id {})
        block-calls* (atom [])
        position-calls* (atom [])]
    (with-redefs [mag-manip/hold-focus (fn [_]
                                        {:x 4.0 :y 5.0 :z 6.0})
                  entity/player-spawn-tracked-entity-by-id!
                  (fn [_player _entity-id _speed] "entity-uuid")
                  motion/entity-motion-available? (constantly true)
                  motion/set-block-body-block-id!
                  (fn [& args]
                    (swap! block-calls* conj args)
                    true)
                  motion/set-entity-position!
                  (fn [& args]
                    (swap! position-calls* conj args)
                    true)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root!
                  (fn [id f & args]
                    (swap! contexts* update id
                           (fn [context]
                             (assoc context :skill-state
                                    (if (and (= f identity) (= 1 (count args)))
                                      (first args)
                                      (apply f (or (:skill-state context) {}) args)))))
                    nil)
                  fx/send! (fn [& _] nil)]
      (is (true?
            ;; start-holding! is defn- — go through the var to call it here.
            (#'mag-manip/start-holding!
              ctx-id "p1" {:player true}
              {:block-id "minecraft:iron_block"
               :from-world? true
               :world-id "w"
               :source-x 1
               :source-y 2
               :source-z 3}))))
    (is (= [["w" "entity-uuid" "minecraft:iron_block"]]
           @block-calls*))
    (is (= [["w" "entity-uuid" 1.5 2.5 3.5]]
           @position-calls*))
    (is (= "entity-uuid"
           (get-in @contexts* [ctx-id :skill-state :entity-uuid])))))

(deftest down-world-capture-requires-successful-break-test
  (let [ctx-id "ctx-world"
        contexts* (mk-context-store ctx-id {})
        down! (get (skill-actions) :down!)]
    (with-redefs [mag-manip/skill-exp (fn [_] 0.8)
                  mag-manip/pick-up-target-block (fn [_]
                                                   {:world-id "w"
                                                    :x 1 :y 2 :z 3
                                                    :block-id "minecraft:iron_block"})
                  entity/player-get-main-hand-item-id (fn [_] nil)
                  block-manip/available? (constantly true)
                  block-manip/can-break-block? (fn [& _] true)
                  block-manip/break-block! (fn [& _] false)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root! (fn [id f & args]
                                                      (swap! contexts* update id
                                                             (fn [ctx]
                                                               (assoc ctx :skill-state
                                                                      (if (and (= f identity) (= 1 (count args)))
                                                                        (first args)
                                                                        (apply f (or (:skill-state ctx) {}) args)))))
                                                      nil)
                  fx/send! (fn [& _] nil)]
      (cb/apply-invoke down! :player-id "p1" :ctx-id ctx-id :player-ref {:id "p"}))
    (is (= :capture-failed (get-in @contexts* [ctx-id :skill-state :mode])))))

(deftest up-too-far-does-not-roll-back-and-lets-block-fall-test
  ;; Matches original s_terminate: too-far/no-resource release does NOT
  ;; restore the block/item to the player — the held entity is simply no
  ;; longer homed, so it falls under its own gravity and self-places on
  ;; collision (see ScriptedBlockBodyEntity). No rollback call exists anymore.
  (let [ctx-id "ctx-hand"
        contexts* (mk-context-store ctx-id
                                    {:skill-state {:mode :holding
                                                   :focus {:x 10.0 :y 0.0 :z 10.0}
                                                   :entity-uuid "uuid-1"
                                                   :world-id "w"
                                                   :held-block {:block-id "minecraft:iron_block"
                                                                :from-hand? true
                                                                :from-world? false}}})
        give-calls* (atom [])
        place-calls* (atom [])
        velocity-calls* (atom [])
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        up! (get (skill-actions) :up!)]
    (with-redefs [mag-manip/skill-exp (fn [_] 0.5)
                  mag-manip/max-hold-distance-sq (fn [] 1.0)
                  geom/body-pos (fn [_] {:x 0.0 :y 0.0 :z 0.0})
                  entity/player-creative? (fn [_] false)
                  entity/player-give-item-stack! (fn [player stack]
                                                   (swap! give-calls* conj [player stack])
                                                   true)
                  motion/entity-motion-available? (fn [] true)
                  motion/entity-position (fn [_world-id _uuid]
                                           {:x 10.0 :y 0.0 :z 10.0})
                  motion/set-block-body-place-when-collide!
                  (fn [& args]
                    (swap! place-calls* conj args)
                    true)
                  motion/set-entity-velocity! (fn [& args]
                                                 (swap! velocity-calls* conj args)
                                                 true)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root! (fn [id f & args]
                                                        (swap! contexts* update id
                                                               (fn [ctx]
                                                                 (assoc ctx :skill-state
                                                                        (if (and (= f identity) (= 1 (count args)))
                                                                          (first args)
                                                                          (apply f (or (:skill-state ctx) {}) args)))))
                                                        nil)
                  fx/send! send!]
      (cb/apply-invoke up! :player-id "p1" :ctx-id ctx-id :player-ref {:id "p"} :cost-ok? true))
    (is (= 0 (count @give-calls*)))
    (is (= [["w" "uuid-1" true]] @place-calls*))
    (is (= 0 (count @velocity-calls*)))
    (is (= :too-far (get-in @contexts* [ctx-id :skill-state :mode])))
    (is (= [[ctx-id :mag-manip/fx-end :end {:reason :too-far}]]
           @calls*))))

(deftest up-success-applies-cooldown-and-exp-test
  (let [ctx-id "ctx-success"
        contexts* (mk-context-store ctx-id
                                    {:skill-state {:mode :holding
                                                   :focus {:x 1.0 :y 2.0 :z 3.0}
                                                   :entity-uuid "uuid-success"
                                                   :world-id "w"
                                                   :held-block {:block-id "minecraft:iron_block"
                                                                :from-world? true
                                                                :world-id "w"
                                                                :source-x 1 :source-y 2 :source-z 3}}})
        cooldown* (atom [])
        exp* (atom [])
        up! (get (skill-actions) :up!)]
    (with-redefs [mag-manip/skill-exp (fn [_] 0.6)
                  mag-manip/max-hold-distance-sq (fn [] 100.0)
                  geom/body-pos (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  mag-manip/look-dir (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  motion/entity-motion-available? (fn [] true)
                  motion/entity-position (fn [_world-id _uuid]
                                           {:x 1.0 :y 2.0 :z 3.0})
                  motion/set-block-body-place-when-collide! (fn [& _] true)
                  motion/set-entity-velocity! (fn [& _] true)
                  skill-effects/set-main-cooldown! (fn [pid sid ticks]
                                                     (swap! cooldown* conj [pid sid ticks])
                                                     nil)
                  skill-effects/add-skill-exp! (fn [pid sid amount]
                                                 (swap! exp* conj [pid sid amount])
                                                 nil)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root! (fn [id f & args]
                                                        (swap! contexts* update id
                                                               (fn [ctx]
                                                                 (assoc ctx :skill-state
                                                                        (if (and (= f identity) (= 1 (count args)))
                                                                          (first args)
                                                                          (apply f (or (:skill-state ctx) {}) args)))))
                                                        nil)
                  fx/send! (fn [& _] nil)]
      (cb/apply-invoke up! :player-id "p1" :ctx-id ctx-id :player-ref {:id "p"} :cost-ok? true))
    (is (= :thrown (get-in @contexts* [ctx-id :skill-state :mode])))
    (is (= 1 (count @cooldown*)))
    (is (= 1 (count @exp*)))))

(deftest up-success-throws-tracked-entity-with-real-velocity-test
  ;; Matches original s_perform: a one-shot velocity toward the aim point at
  ;; lerp(0.5,1.0,exp) speed — damage/placement now come from the entity's
  ;; own real collision, not from this skill directly.
  (let [ctx-id "ctx-throw"
        contexts* (mk-context-store ctx-id
                                    {:skill-state {:mode :holding
                                                   :focus {:x 1.0 :y 2.0 :z 3.0}
                                                   :entity-uuid "uuid-1"
                                                   :world-id "w"
                                                   :held-block {:block-id "minecraft:iron_block"
                                                                :from-world? true
                                                                :world-id "w"
                                                                :source-x 1 :source-y 2 :source-z 3}}})
        velocity-calls* (atom [])
        up! (get (skill-actions) :up!)]
    (with-redefs [mag-manip/skill-exp (fn [_] 0.6)
                  mag-manip/max-hold-distance-sq (fn [] 100.0)
                  geom/body-pos (fn [_] {:x 1.0 :y 2.0 :z 3.0})
                  mag-manip/look-dir (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  motion/entity-motion-available? (fn [] true)
                  motion/entity-position (fn [_world-id _uuid] {:x 1.0 :y 2.0 :z 3.0})
                  motion/set-block-body-place-when-collide! (fn [& _] true)
                  motion/set-entity-velocity! (fn [world-id entity-uuid x y z]
                                                 (swap! velocity-calls* conj
                                                        {:world-id world-id :entity-uuid entity-uuid
                                                         :x x :y y :z z})
                                                 true)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root! (fn [id f & args]
                                                        (swap! contexts* update id
                                                               (fn [ctx]
                                                                 (assoc ctx :skill-state
                                                                        (if (and (= f identity) (= 1 (count args)))
                                                                          (first args)
                                                                          (apply f (or (:skill-state ctx) {}) args)))))
                                                        nil)
                  fx/send! (fn [& _] nil)]
      (cb/apply-invoke up! :player-id "p1" :ctx-id ctx-id :player-ref {:id "p"} :cost-ok? true :exp 0.6))
    (is (= 1 (count @velocity-calls*)))
    (let [{:keys [world-id entity-uuid x y z]} (first @velocity-calls*)]
      (is (= "w" world-id))
      (is (= "uuid-1" entity-uuid))
      (is (< (Math/abs (double x)) 1.0e-9))
      (is (< (Math/abs (double y)) 1.0e-9))
      ;; speed = lerp(0.5, 1.0, 0.6) = 0.8, thrown straight along +Z
      (is (< (Math/abs (- 0.8 (double z))) 1.0e-6)))))

(deftest up-throws-when-player-is-far-from-the-world-origin-test
  ;; The distance guard used to read [:position] out of the ability player
  ;; state, which nothing ever writes: it always came back as the {0,0,0}
  ;; default, so anywhere but spawn the held block measured as "too far" and
  ;; was released without a throw — it just dropped. Upstream compares
  ;; player.getDistanceSq(entity) on the live entities.
  (let [ctx-id "ctx-far-from-origin"
        contexts* (mk-context-store ctx-id
                                    {:skill-state {:mode :holding
                                                   :focus {:x 1002.0 :y 65.0 :z 1000.0}
                                                   :entity-uuid "uuid-far"
                                                   :world-id "w"
                                                   :held-block {:block-id "minecraft:iron_block"
                                                                :from-hand? true
                                                                :from-world? false}}})
        velocity-calls* (atom [])
        {:keys [calls* send!]} (fx-mocks/capture-fx-send!)
        up! (get (skill-actions) :up!)]
    (with-redefs [mag-manip/skill-exp (fn [_] 0.5)
                  ;; player far from origin, block held 2 blocks in front
                  geom/body-pos (fn [_] {:x 1000.0 :y 64.0 :z 1000.0})
                  geom/eye-pos (fn [_] {:x 1000.0 :y 65.62 :z 1000.0})
                  motion/entity-motion-available? (fn [] true)
                  motion/entity-position (fn [_ _] {:x 1002.0 :y 65.0 :z 1000.0})
                  motion/set-block-body-place-when-collide! (fn [& _] true)
                  motion/set-entity-velocity! (fn [& args]
                                                (swap! velocity-calls* conj args)
                                                true)
                  raycast/available? (fn [] true)
                  raycast/player-look-vector (fn [_] {:x 1.0 :y 0.0 :z 0.0})
                  raycast/raycast-combined (fn [& _] nil)
                  ctx/get-context (fn
                                    ([id] (get @contexts* id))
                                    ([_owner id] (get @contexts* id)))
                  ctx-skill/update-skill-state-root! (fn [id f & args]
                                                       (swap! contexts* update id
                                                              (fn [ctx]
                                                                (assoc ctx :skill-state
                                                                       (if (and (= f identity) (= 1 (count args)))
                                                                         (first args)
                                                                         (apply f (or (:skill-state ctx) {}) args)))))
                                                       nil)
                  skill-effects/set-main-cooldown! (fn [& _] nil)
                  skill-effects/add-skill-exp! (fn [& _] nil)
                  fx/send! send!]
      (cb/apply-invoke up! :player-id "p1" :ctx-id ctx-id :player-ref {:id "p"} :cost-ok? true))
    (is (= :thrown (get-in @contexts* [ctx-id :skill-state :mode])))
    (is (= 1 (count @velocity-calls*)) "the block is launched, not dropped")
    (is (some (fn [[_ topic _ payload]]
                (and (= :mag-manip/fx-end topic) (= :performed (:reason payload))))
              @calls*))))

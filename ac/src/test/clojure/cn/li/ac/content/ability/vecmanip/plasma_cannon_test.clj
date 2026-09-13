(ns cn.li.ac.content.ability.vecmanip.plasma-cannon-test
  "Regression pins for PlasmaCannon's charge/fire model.

  These used to grep the source text for literal fragments; the fx layer has
  since moved behind arc-beam's spec builder and :level moved to
  skill-config/skill-definitions, so the assertions now read the data those
  refactors produce instead of the code that produces it."
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.skill-config.common :as config-common]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.vecmanip.plasma-cannon :as pc]
            [cn.li.ac.content.ability.vecmanip.plasma-cannon-fx :as pc-fx]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]))

(defn- definition []
  (some #(when (= :plasma-cannon (:id %)) %) config-common/skill-definitions))

(deftest defskill-registration
  (testing "plasma-cannon is a level-5 charge-window vecmanip skill"
    (is (= :plasma-cannon (:id pc/plasma-cannon)))
    (is (= :vecmanip (:category-id pc/plasma-cannon)))
    (is (= :charge-window (:pattern pc/plasma-cannon)))
    ;; :level lives in skill-definitions — defskill rejects it outright.
    (is (= 5 (:level (definition))))))

(deftest key-up-keeps-the-context-alive
  (testing "the projectile keeps flying after the key is released"
    (is (false? (get-in pc/plasma-cannon [:input-policy :terminate-on-key-up?])))
    (is (true? (get-in pc/plasma-cannon [:input-policy :keep-active-on-key-up?])))))

;; Bug 2: charge CP is consumed manually inside the :charging branch, so the
;; declarative cost machinery must stay out of it entirely — a :tick cost here
;; would double-charge, and the 0.0 consume speeds would zero a :cost block.
(deftest plasma-cannon-cost-structure
  (testing "no declarative cost block; charge cost is paid manually"
    (is (nil? (:cost pc/plasma-cannon)))
    (is (= 0.0 (skill-config/cp-consume-speed :plasma-cannon)))
    (is (= 0.0 (skill-config/overload-consume-speed :plasma-cannon)))))

(deftest fx-handlers-updated
  (testing "the fx spec registers all four channels and forwards charge-pos"
    (let [handlers* (atom {})
          enqueued* (atom [])]
      (with-redefs [level-effects/register-level-effect! (fn [& _] nil)
                    fx-registry/register-fx-channel! (fn [topic handler]
                                                       (swap! handlers* assoc topic handler)
                                                       nil)
                    level-effects/enqueue-level-effect! (fn [_ _ _ payload & _]
                                                          (swap! enqueued* conj payload)
                                                          nil)]
        (pc-fx/init!)
        (is (= #{:plasma-cannon/fx-start
                 :plasma-cannon/fx-update
                 :plasma-cannon/fx-perform
                 :plasma-cannon/fx-end}
               (set (keys @handlers*))))
        ((get @handlers* :plasma-cannon/fx-start)
         "ctx-pc" :plasma-cannon/fx-start {:charge-pos {:x 1.0 :y 64.0 :z 2.0}})
        (is (= {:x 1.0 :y 64.0 :z 2.0}
               (:charge-pos (first @enqueued*))))))))

;; ---------------------------------------------------------------------------
;; Explosion / targeting shape (upstream s_perform + explode)
;; ---------------------------------------------------------------------------

(def ^:private do-explode! #'pc/do-explode!)
(def ^:private resolve-destination #'pc/resolve-destination)

(deftest explosion-damages-a-sphere-not-a-box-test
  ;; Upstream WorldUtils.getEntities builds the ±r box and then filters it with
  ;; EntitySelectors.within(x,y,z,r). find-entities-in-radius only does the box,
  ;; so a corner entity r*sqrt(3) away used to take full damage.
  (let [damaged* (atom [])
        origin {:x 0.0 :y 64.0 :z 0.0}
        entities [{:uuid "inside" :x 9.0 :y 64.0 :z 0.0}
                  {:uuid "corner" :x 9.0 :y 73.0 :z 9.0}
                  {:uuid "edge" :x 10.0 :y 64.0 :z 0.0}]]
    (with-redefs [world-effects/available? (fn [] true)
                  world-effects/find-entities-in-radius (fn [& _] entities)
                  world-effects/create-explosion! (fn [& _] nil)
                  entity-damage/available? (fn [] true)
                  entity-damage/apply-direct-damage! (fn [_ target-id & _]
                                                       (swap! damaged* conj target-id)
                                                       nil)
                  block-manip/destroy-allowed? (fn [] false)
                  skill-effects/scale-damage (fn [_ dmg] dmg)
                  ability-event/fire-calc-event! (fn [_ v & _] v)]
      (do-explode! "p1" "minecraft:overworld" origin 0.0)
      (is (= #{"inside" "edge"} (set @damaged*))
          "the box corner is 15.6 blocks out — outside the 10-block sphere"))))

(deftest destination-trace-is-living-only-test
  ;; s_perform: Raytrace.getLookingPos(player, 100, EntitySelectors.living).
  ;; The generic combined trace also stops on any pickable entity (armour
  ;; stands, boats, item frames), dropping the shot short of what was aimed at.
  (let [calls* (atom [])]
    (with-redefs [raycast/available? (fn [] true)
                  raycast/player-look-vector (fn [_] {:x 0.0 :y 0.0 :z 1.0})
                  raycast/raycast-combined-from-player
                  (fn [player-uuid max-dist living-only?]
                    (swap! calls* conj [player-uuid max-dist living-only?])
                    {:hit-type :entity :hit-x 0.0 :hit-y 64.0 :hit-z 30.0 :eye-height 1.8})]
      (let [dest (resolve-destination "p1" "minecraft:overworld" {:x 0.0 :y 64.0 :z 0.0})]
        (is (= [["p1" 100.0 true]] @calls*))
        ;; getLookingPos raises an entity hit by eyeHeight * 0.6.
        (is (< 65.07 (:y dest) 65.09))
        (is (= 30.0 (:z dest)))))))

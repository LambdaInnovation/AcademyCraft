(ns cn.li.ac.content.ability.electromaster.mag-manip
  "MagManip skill - grab a magnetizable block, hold it, throw it on key release.

  Pattern: :release-cast
  Cost:     CP lerp(140,270) / overload lerp(35,20) - only charged when holding block nearby
  Cooldown: manual, lerp(60,40) ticks by exp
  Exp:      +0.005 on successful throw

  Matches original AcademyCraft MagManipContext: the held/thrown block is a
  real physics entity (entity_magmanip_block_body) that homes toward the
  player's aim while held and gets a one-shot velocity on throw — damage and
  block placement both come from the entity's own real collision (see
  ScriptedBlockBodyEntity.onHitEntity/onHit), not from instant hit-scan math."
  (:require
            [cn.li.ac.config.modid :as modid]
            [clojure.string :as str]
            [cn.li.ac.ability.config :as ability-config]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
                        [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.effects.motion :as motion]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
                        [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.util.log :as log]))

;; --- Constants ---

(def-skill-config-ops :mag-manip)
(def ^:private mag-manip-skill-id :mag-manip)
(def ^:private tracked-block-body-entity-id (modid/namespaced-path "entity_magmanip_block_body"))

(defn- max-hold-distance-sq []
  (let [distance (cfg-double :targeting.max-hold-distance)]
    (* distance distance)))

;; --- Domain helpers ---

(defn- normalize-block-id [block-id]
  (let [id (some-> block-id str str/lower-case)]
    (cond
      (nil? id) nil
      (str/includes? id ":") id
      (re-matches #"(?:block|item)\.[^.]+\..+" id)
      (let [[_ namespace path]
            (re-matches #"(?:block|item)\.([^.]+)\.(.+)" id)]
        (str namespace ":" path))
      :else id)))

(defn- accepted-metal-blocks []
  (->> (concat (ability-config/get-normal-metal-blocks)
               (ability-config/get-weak-metal-blocks))
       (keep normalize-block-id)
       distinct
       vec))

;; Original delegates to CatElectromaster#isMetalBlock, whose normal and weak
;; sets are shared by every magnetic skill and both apply without an exp gate.
(defn- metal-block-id? [block-id]
  (boolean
    (some-> block-id normalize-block-id ability-config/is-metal-block?)))

(defn- look-dir [player-id]
  (when-let [look (when (raycast/available?)
                    (raycast/player-look-vector player-id))]
    (geom/vnorm {:x (double (:x look))
                 :y (double (:y look))
                 :z (double (:z look))})))

;; origin = eye - (0, hold-head-y-offset, 0); focus = origin + look * hold-distance
(defn- hold-focus [player-id]
  (let [eye (geom/eye-pos player-id)
        origin (geom/v+ eye {:x 0.0 :y (- (cfg-double :movement.hold-head-y-offset)) :z 0.0})
        dir (or (look-dir player-id) {:x 0.0 :y 0.0 :z 1.0})]
    (geom/v+ origin (geom/v* dir (cfg-double :movement.hold-distance)))))

;; --- Held-entity homing (matches original MagManipEntityBlock's ActMoveTo) ---

(defn- home-entity-toward!
  "Push one tick of homing velocity toward `target` — exact original formula:
  motion = normalize(target - pos) * 0.2 * (distSq < 4 ? distSq/4 : 1.0)."
  [world-id entity-uuid target]
  (when-let [pos (motion/entity-position world-id entity-uuid)]
    (let [delta (geom/v- target pos)
          len (geom/vlen delta)]
      (when (pos? len)
        (let [dist-sq (geom/vdist-sq pos target)
              scale (* 0.2 (if (< dist-sq 4.0) (/ dist-sq 4.0) 1.0))
              mv (geom/v* (geom/vnorm delta) scale)]
          (motion/set-entity-velocity! world-id entity-uuid (:x mv) (:y mv) (:z mv)))))))

;; --- Block-manipulation helpers ---

(defn- restore-captured-block!
  [{:keys [from-world? source-x source-y source-z block-id world-id]}]
  (when (and from-world?
             world-id
             block-id
             (number? source-x)
             (number? source-y)
             (number? source-z))
    (let [x (int source-x)
          y (int source-y)
          z (int source-z)
          current (block-manip/get-block world-id x y z)]
      (when (or (nil? current) (= "minecraft:air" current))
        (block-manip/set-block! world-id x y z block-id)))))

(defn- restore-consumed-item!
  [player {:keys [from-hand? creative? block-id]}]
  (when (and from-hand?
             (not creative?)
             player
             (string? block-id))
    (when-let [stack (pitem/stack-by-id block-id 1)]
      (entity/player-give-item-stack! player stack))))

(defn- roll-back-failed-spawn!
  [player held-block]
  (restore-captured-block! held-block)
  (restore-consumed-item! player held-block)
  nil)

(defn- pick-up-target-block [player-id]
  (when (and (raycast/available?) (block-manip/available?))
    (when-let [dir (look-dir player-id)]
      (let [start (geom/eye-pos player-id)
            world-id (geom/world-id-of player-id)
            hit (raycast/raycast-blocks-matching
                  world-id
                  (:x start) (:y start) (:z start)
                  (:x dir) (:y dir) (:z dir)
                  (cfg-double :targeting.grab-range)
                  (accepted-metal-blocks))]
        (when hit
          (let [bx (int (or (:x hit) 0))
                by (int (or (:y hit) 0))
                bz (int (or (:z hit) 0))
                block-id (or (block-manip/get-block world-id bx by bz)
                             (:block-id hit))
                hardness (block-manip/get-block-hardness world-id bx by bz)]
            (when (and (string? block-id)
                       (metal-block-id? block-id)
                       (number? hardness)
                       (not (neg? (double hardness))))
              {:world-id world-id
               :x bx :y by :z bz
               :block-id block-id
               :hardness hardness})))))))

;; --- Cost helpers ---

(defn- held-entity-position
  [{:keys [world-id entity-uuid]}]
  (when (and world-id entity-uuid (motion/entity-motion-available?))
    (motion/entity-position world-id entity-uuid)))

(defn- release-held-entity!
  "Match setPlaceFromServer(true): once homing ends, the body falls and places
  its captured block on the next block collision."
  [{:keys [world-id entity-uuid]}]
  (when (and world-id entity-uuid (motion/entity-motion-available?))
    (motion/set-block-body-place-when-collide!
      world-id entity-uuid true)))

(defn- holding-nearby? [player-id ctx-id]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [ss (:skill-state ctx-data)]
      (when (and (= :holding (:mode ss)) (:held-block ss))
        (when-let [entity-pos (held-entity-position ss)]
          ;; Nothing ever writes [:position] into the ability player state, so
          ;; reading it here always yielded the default and the distance was
          ;; measured from the world origin. geom/body-pos asks the live entity.
          (let [player-pos (geom/body-pos player-id)]
            (< (geom/vdist-sq player-pos entity-pos)
               (max-hold-distance-sq))))))))

(defn- active-ctx-id [player-id skill-id]
  (some (fn [[ctx-id ctx-data]]
          (when (and (= (:player-uuid ctx-data) player-id)
                     (= skill-id (:skill-id ctx-data)))
            ctx-id))
        (ctx/get-all-contexts)))

(defn- cost-up-cp [player-id _skill-id exp]
  (if-let [ctx-id (active-ctx-id player-id mag-manip-skill-id)]
    (if (holding-nearby? player-id ctx-id)
      (cfg-lerp :cost.up.cp exp)
      0.0)
    0.0))

(defn- cost-up-overload [player-id _skill-id exp]
  (if-let [ctx-id (active-ctx-id player-id mag-manip-skill-id)]
    (if (holding-nearby? player-id ctx-id)
      (cfg-lerp :cost.up.overload exp)
      0.0)
    0.0))

(defn- cost-creative? [player-id _skill-id _exp]
  (if-let [ctx-id (active-ctx-id player-id mag-manip-skill-id)]
    (boolean
      (get-in (ctx-skill/get-context ctx-id) [:skill-state :creative?]))
    false))

;; --- Action hooks ---

;; Original s_makeAlive: check hand item first, fallback to raycast world block,
;; if neither found store :no-target mode. Spawns a real tracked physics
;; entity (matching original's new MagManipEntityBlock(player, 10)) instead of
;; a fire-and-forget cosmetic one — held/thrown behavior now rides on its real
;; collision via ScriptedBlockBodyEntity.
(defn- start-holding! [ctx-id player-id player held-block]
  (let [focus (hold-focus player-id)
        entity-uuid (when player
                      (entity/player-spawn-tracked-entity-by-id!
                        player tracked-block-body-entity-id 0.0))
        world-id (:world-id held-block)
        configured? (and entity-uuid
                         world-id
                         (motion/entity-motion-available?)
                         (motion/set-block-body-block-id!
                           world-id entity-uuid (:block-id held-block)))]
    (if configured?
      (do
        (when (:from-world? held-block)
          (motion/set-entity-position!
            world-id entity-uuid
            (+ 0.5 (double (:source-x held-block)))
            (+ 0.5 (double (:source-y held-block)))
            (+ 0.5 (double (:source-z held-block)))))
        (ctx-skill/replace-skill-state!
          ctx-id
          {:fired false
           :mode :holding
           :hold-ticks 0
           :held-block held-block
           :focus focus
           :entity-uuid entity-uuid
           :world-id world-id
           :creative? (boolean (:creative? held-block))})
        ;; :source-player-id anchors the looping hold sound to the caster
        ;; (original FollowEntitySound(player, "em.lf_loop")).
        (fx/send! ctx-id {:topic :mag-manip/fx-hold :mode :hold-start} nil
                  {:source-player-id player-id
                   :focus focus
                   :block-id (:block-id held-block)})
        true)
      (do
        (when (and entity-uuid world-id (motion/entity-motion-available?))
          (motion/discard-entity! world-id entity-uuid))
        (roll-back-failed-spawn! player held-block)
        (ctx-skill/replace-skill-state!
          ctx-id {:fired false :mode :spawn-failed})
        (ctx/terminate-context! ctx-id nil)
        false))))

;; Original s_makeAlive: check hand item first, fallback to raycast world block,
;; if neither found store :no-target mode.
(defn- on-down
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage player]
  (let [world-id (geom/world-id-of player-id)
        hand-item-id (when player (entity/player-get-main-hand-item-id player))
        held-metal? (and (string? hand-item-id) (metal-block-id? hand-item-id))]
    (if held-metal?
      (let [creative? (boolean (and player (entity/player-creative? player)))
            consumed? (or creative?
                          (and player (entity/player-consume-main-hand-item! player 1)))]
        (if consumed?
          (start-holding! ctx-id player-id player
                          {:block-id (normalize-block-id hand-item-id)
                           :from-world? false
                           :from-hand? true
                           :creative? creative?
                           :world-id world-id})
          (do
            (ctx-skill/replace-skill-state!
              ctx-id {:fired false :mode :capture-failed})
            (ctx/terminate-context! ctx-id nil))))
      (if-let [{:keys [world-id x y z block-id]} (pick-up-target-block player-id)]
        (let [can-break? (and (skill-effects/skill-destroy-allowed? mag-manip-skill-id)
                               (block-manip/can-break-block? player-id world-id x y z))
              broken? (and can-break?
                           (block-manip/break-block! player-id world-id x y z false))]
          (if broken?
            (start-holding! ctx-id player-id player
                            {:block-id (normalize-block-id block-id)
                             :from-world? true
                             :creative? (boolean
                                          (and player
                                               (entity/player-creative? player)))
                             :world-id world-id
                             :source-x x
                             :source-y y
                             :source-z z})
            (do
              (log/debug "MagManip capture failed" {:world-id world-id :x x :y y :z z :block-id block-id})
              (ctx-skill/replace-skill-state!
                ctx-id {:fired false :mode :capture-failed})
              (ctx/terminate-context! ctx-id nil))))
        (do
          (ctx-skill/replace-skill-state!
            ctx-id {:fired false :mode :no-target})
          (ctx/terminate-context! ctx-id nil))))))

;; Original s_tick: update hold position (both ends ran updateMoveTo() every
;; tick) — here the server pushes fresh homing velocity to the real entity
;; every tick, and FX is still sent every 2 ticks for the client visuals.
(defn- on-tick
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [ss (:skill-state ctx-data)]
      (when (= :holding (:mode ss))
        (let [ticks (inc (int (or (:hold-ticks ss) 0)))
              focus (hold-focus player-id)
              entity-uuid (:entity-uuid ss)
              world-id (:world-id ss)]
          (ctx-skill/replace-skill-state! ctx-id
                                 (assoc ss :hold-ticks ticks :focus focus))
          (when (and entity-uuid world-id (motion/entity-motion-available?))
            (home-entity-toward! world-id entity-uuid focus))
          (when (zero? (mod ticks 2))
            (fx/send! ctx-id {:topic :mag-manip/fx-hold :mode :hold-loop} nil
                      {:focus focus
                       :block-id (get-in ss [:held-block :block-id])})))))))

;; Original s_perform: check dist < 5 blocks, consume resources, then give the
;; entity a one-shot velocity toward the looked-at point (raytrace 20 blocks)
;; at lerp(0.5,1.0,exp) speed. Damage and block placement are no longer done
;; here — they happen via the entity's own real collision once it's flying.
;; On failure (too far / no resource) the entity is simply no longer homed —
;; it falls under its own gravity and self-places wherever it lands, matching
;; original's ActNothing fallback (no rollback to the player).
(defn- throw-target
  [player-id world-id dir]
  (let [range (cfg-double :targeting.throw-range)
        eye (geom/eye-pos player-id)
        ;; [:position] is never written into the ability player state; reading
        ;; it here aimed the no-hit fallback 20 blocks from the world ORIGIN.
        body (geom/body-pos player-id)
        hit (when (raycast/available?)
              (raycast/raycast-combined
                world-id
                (:x eye) (:y eye) (:z eye)
                (:x dir) (:y dir) (:z dir)
                range))
        end (case (:hit-type hit)
              :entity
              {:x (double (or (:x hit) (:hit-x hit) 0.0))
               :y (+ (double (or (:y hit) (:hit-y hit) 0.0))
                     (* 0.6 (double (or (:eye-height hit) 0.0))))
               :z (double (or (:z hit) (:hit-z hit) 0.0))}

              :block
              {:x (double (or (:hit-x hit) (:x hit) 0.0))
               :y (double (or (:hit-y hit) (:y hit) 0.0))
               :z (double (or (:hit-z hit) (:z hit) 0.0))}

              ;; LambdaLib2 getLookingPos falls back from the player's body
              ;; position, even though its hit trace begins at the eyes.
              (geom/v+ body (geom/v* dir range)))]
    {:hit hit :end end}))

(defn- on-up
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [ss (:skill-state ctx-data)
          held-block (get ss :held-block)
          entity-uuid (:entity-uuid ss)
          world-id (:world-id ss)]
      (if-not (and (= :holding (:mode ss)) held-block)
        (ctx-skill/replace-skill-state! ctx-id
                               (assoc ss :fired false :mode :idle))
        (let [entity-pos (held-entity-position ss)
              ;; Same as holding-nearby?: [:position] is never populated, so
              ;; this compared the held block against the world origin and
              ;; every release outside spawn came out "too far" — the block was
              ;; released without a throw and simply fell (upstream compares
              ;; player.getDistanceSq(entity) against 25).
              player-pos (geom/body-pos player-id)
              too-far? (and entity-pos
                            (>= (geom/vdist-sq player-pos entity-pos)
                                (max-hold-distance-sq)))]
          ;; Original enables placement before checking distance/resources, so
          ;; every non-throw outcome still lets the released body fall/settle.
          (release-held-entity! ss)
          (cond
            (nil? entity-pos)
            (do
              (fx/send! ctx-id {:topic :mag-manip/fx-end :mode :end} nil
                        {:reason :entity-missing})
              (ctx-skill/replace-skill-state!
                ctx-id (assoc ss :fired false :mode :entity-missing)))

            too-far?
            (do
              (fx/send! ctx-id {:topic :mag-manip/fx-end :mode :end} nil {:reason :too-far})
              (ctx-skill/replace-skill-state! ctx-id
                                     (assoc ss :fired false :mode :too-far)))

            (not cost-ok?)
            (do
              (fx/send! ctx-id {:topic :mag-manip/fx-end :mode :end} nil {:reason :no-resource})
              (ctx-skill/replace-skill-state! ctx-id
                                     (assoc ss :fired false :mode :no-resource)))

            :else
            (let [dir (or (look-dir player-id) {:x 0.0 :y 0.0 :z 1.0})
                  {:keys [hit end]} (throw-target player-id world-id dir)
                  speed (cfg-lerp :movement.throw-speed exp)
                  delta (geom/v- end entity-pos)
                  throw-dir (if (pos? (geom/vlen delta)) (geom/vnorm delta) dir)]
              (when (and entity-uuid world-id (motion/entity-motion-available?))
                (let [vel (geom/v* throw-dir speed)]
                  (motion/set-entity-velocity! world-id entity-uuid (:x vel) (:y vel) (:z vel))))

              (fx/send! ctx-id {:topic :mag-manip/fx-throw :mode :throw} nil
                        {:start entity-pos
                         :end end
                         :hit-type (:hit-type hit)
                         :block-id (:block-id held-block)})
              (fx/send! ctx-id {:topic :mag-manip/fx-end :mode :end} nil {:reason :performed})

              ;; Manual cooldown and exp - only on successful throw
              (skill-effects/set-main-cooldown! player-id mag-manip-skill-id
                                                (skill-config/lerp-int mag-manip-skill-id
                                                                       :cooldown.ticks
                                                                       exp))
              (skill-effects/add-skill-exp! player-id mag-manip-skill-id
                                            (cfg-double :progression.exp-throw))
              (ctx-skill/replace-skill-state! ctx-id
                                     {:fired true
                                      :mode :thrown
                                      :held-block nil}))))))))

(defn- on-abort
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (when-let [skill-state (:skill-state ctx-data)]
      (when (:held-block skill-state)
        (release-held-entity! skill-state)
        (fx/send! ctx-id {:topic :mag-manip/fx-end :mode :end} nil
                  {:reason :abort})))
    (ctx-skill/clear-skill-state! ctx-id)))

;; --- Skill definition ---

(declare mag-manip)

(defskill mag-manip
  :id :mag-manip
  :category-id :electromaster
  :name-key "ability.skill.electromaster.mag_manip"
  :description-key "ability.skill.electromaster.mag_manip.desc"
  :icon "textures/abilities/electromaster/skills/mag_manip.png"
  :ui-position [204 33]
  :ctrl-id :mag-manip
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks (fn [{:keys [exp]}]
                    (skill-config/lerp-int mag-manip-skill-id
                                           :cooldown.ticks
                                           (double (or exp 0.0))))
  :pattern :release-cast
  :cooldown {:mode :manual}
  :cost {:up {:cp cost-up-cp
              :overload cost-up-overload
              :creative? cost-creative?}}
  :actions {:down! on-down
            :tick! on-tick
            :up! on-up
            :abort! on-abort}
  :prerequisites [{:skill-id :mag-movement :min-exp 0.5}])

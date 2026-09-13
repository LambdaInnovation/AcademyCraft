(ns cn.li.ac.content.ability.vecmanip.directed-blastwave
  "DirectedBlastwave - AOE blast with block breaking.

  Pattern: :charge-window (valid 6-50, abort 200)
  Cost on perform: CP lerp(160,200), overload lerp(50,30) by exp
  Cooldown: lerp(80,50) ticks by exp
  Exp: +0.0025 on hit / +0.0012 on miss"
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.mcmod.platform.block-manipulation :as block-manip]
            [cn.li.mcmod.server.platform-bridge :as server-bridge]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :directed-blastwave)

(defn- scaled-damage [player-id target-id raw-damage]
  (skill-effects/scale-damage
   (skill-registry/get-skill :directed-blastwave)
   (ability-event/fire-calc-event!
    ability-event/CALC-SKILL-ATTACK
    raw-damage
    {:player-id player-id
     :target-id target-id
     :skill-id :directed-blastwave})))

(defn- player-body-pos [player-id]
  (geom/body-pos player-id))

(defn- terminate-with-end!
  [ctx-id performed?]
  (fx/send! ctx-id {:topic :directed-blastwave/fx-end :mode :end} nil {:performed? performed?})
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (ctx-skill/replace-skill-state! ctx-id (assoc (:skill-state ctx-data) :performed? performed?)))
  (ctx-skill/clear-skill-state! ctx-id)
  (ctx/terminate-context! ctx-id nil))

(defn- nearer-hit
  [block-hit entity-hit]
  (cond
    (nil? block-hit) (when entity-hit (assoc entity-hit :hit-type :entity))
    (nil? entity-hit) (assoc block-hit :hit-type :block)
    (<= (double (or (:distance block-hit) Double/POSITIVE_INFINITY))
        (double (or (:distance entity-hit) Double/POSITIVE_INFINITY)))
    (assoc block-hit :hit-type :block)
    :else
    (assoc entity-hit :hit-type :entity)))

(defn- target-trace
  [player-id world-id]
  (when (raycast/available?)
    (let [pos (raycast/player-position player-id)
          look (raycast/player-look-vector player-id)
          max-distance (cfg-double :targeting.raycast-distance)
          block-hit (when (and pos look)
                      (raycast/raycast-blocks
                       world-id
                       (double (:x pos))
                       (double (or (:eye-y pos) (:y pos)))
                       (double (:z pos))
                       (double (:x look))
                       (double (:y look))
                       (double (:z look))
                       max-distance))
          entity-hit (raycast/raycast-from-player player-id max-distance true)]
      (nearer-hit block-hit entity-hit))))

(defn- hit-pos-from-trace [player-id trace]
  (let [look (or (when (raycast/available?)
                   (raycast/player-look-vector player-id))
                 {:x 0.0 :y 0.0 :z 1.0})]
    (cond
      (nil? trace)
      (geom/v+ (player-body-pos player-id)
               (geom/v* (geom/vnorm look) (cfg-double :targeting.raycast-distance)))
      (= :block (:hit-type trace))
      {:x (double (or (:x trace) 0.0))
       :y (double (or (:y trace) 0.0))
       :z (double (or (:z trace) 0.0))}
      (= :entity (:hit-type trace))
      {:x (double (or (:x trace) 0.0))
       :y (+ (double (or (:y trace) 0.0))
             (double (or (:eye-height trace) (cfg-double :targeting.eye-height))))
       :z (double (or (:z trace) 0.0))}
      :else
      {:x (double (or (:hit-x trace) (:x trace) 0.0))
       :y (double (or (:hit-y trace) (:y trace) 0.0))
       :z (double (or (:hit-z trace) (:z trace) 0.0))})))

(defn- push-velocity [player-id entity]
  (let [player-pos (player-body-pos player-id)
        target-pos {:x (double (or (:x entity) 0.0))
                    :y (double (or (:y entity) 0.0))
                    :z (double (or (:z entity) 0.0))}
        d0 (geom/vnorm (geom/v- target-pos player-pos))]
    (geom/v* d0 0.24)))

(defn- break-hardness [exp]
  (let [[low-cap mid-cap high-cap] (cfg-double-list :breaking.hardness-caps)]
    (cond (< (double exp) (cfg-double :breaking.hardness-low-threshold)) low-cap
          (< (double exp) (cfg-double :breaking.hardness-mid-threshold)) mid-cap
          :else high-cap)))

(defn- try-break-blastwave-block!
  [player-id world-id x0 y0 z0 x y z hard-cap p-break p-drop full-exp?]
  (let [dx (- x x0) dy (- y y0) dz (- z z0)
        dist-sq (+ (* dx dx) (* dy dy) (* dz dz))]
    (when (and (<= dist-sq 6)
               (or (zero? dist-sq) (< (rand) p-break)))
      (let [hardness (block-manip/get-block-hardness world-id x y z)
            block-id (block-manip/get-block world-id x y z)
            breakable? (and (number? hardness)
                            (>= (double hardness) 0.0)
                            (<= (double hardness) (double hard-cap))
                            (some? block-id)
                            (skill-effects/skill-destroy-allowed? :directed-blastwave)
                            (block-manip/can-break-block? player-id world-id x y z))]
        (when breakable?
          (if full-exp?
            (do
              ;; Exact mastery uses one ItemStack of the block itself,
              ;; rather than the block's normal loot table.
              (when (server-bridge/server-bridge-available?)
                (server-bridge/spawn-item-stack-at!
                 world-id x y z block-id 1))
              (block-manip/break-block! player-id world-id x y z false))
            (block-manip/break-block!
             player-id world-id x y z (< (rand) p-drop))))))))

(defn- break-blastwave-column!
  "Named Z-axis walk — keeps AOT class names short without materializing coords."
  [player-id world-id x0 y0 z0 x y z-lo z-hi hard-cap p-break p-drop full-exp?]
  (let [z-lo (long z-lo)
        z-hi (long z-hi)]
    (loop [z z-lo]
      (when (< z z-hi)
        (try-break-blastwave-block!
         player-id world-id x0 y0 z0 x y z hard-cap p-break p-drop full-exp?)
        (recur (unchecked-inc z))))))

(defn- break-blastwave-slice!
  [player-id world-id x0 y0 z0 x y-lo y-hi z-lo z-hi
   hard-cap p-break p-drop full-exp?]
  (let [y-lo (long y-lo)
        y-hi (long y-hi)]
    (loop [y y-lo]
      (when (< y y-hi)
        (break-blastwave-column!
         player-id world-id x0 y0 z0 x y z-lo z-hi hard-cap p-break p-drop full-exp?)
        (recur (unchecked-inc y))))))

(defn- break-nearby-blocks! [player-id world-id pos exp]
  (when (block-manip/available?)
    (let [x0        (int (Math/round (double (:x pos))))
          y0        (int (Math/round (double (:y pos))))
          z0        (int (Math/round (double (:z pos))))
          hard-cap  (break-hardness exp)
          p-break   (cfg-lerp :breaking.break-probability exp)
          p-drop    (cfg-lerp :breaking.drop-probability exp)
          full-exp? (= 1.0 (double exp))
          x-lo (- x0 3) x-hi (+ x0 3)
          y-lo (- y0 3) y-hi (+ y0 3)
          z-lo (- z0 3) z-hi (+ z0 3)]
      ;; Primitive nested loops — no coord vector / range seq allocation.
      (loop [x x-lo]
        (when (< x x-hi)
          (break-blastwave-slice!
           player-id world-id x0 y0 z0 x y-lo y-hi z-lo z-hi
           hard-cap p-break p-drop full-exp?)
          (recur (unchecked-inc x)))))))

(defn- directed-blastwave-down!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (ctx-skill/replace-skill-state! ctx-id
                         {:charge-ticks 0 :punched? false
                          :punch-ticks 0 :performed? false})
  ;; Upstream l_handEffectStart: key-down plays only the 0.15s hand-raise
  ;; (prepare) anim — no particles, no sound. Owner-only, like the original's
  ;; isLocal-gated hand override.
  (fx/send! ctx-id {:topic :directed-blastwave/fx-start :mode :start}))

(defn- directed-blastwave-tick!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [ss          (:skill-state ctx-data)
          next-charge (inc (long (or (:charge-ticks ss) 0)))
          punched?    (boolean (:punched? ss))
          next-punch  (if punched? (inc (long (or (:punch-ticks ss) 0))) 0)]
      (ctx-skill/replace-skill-state! ctx-id (assoc ss :charge-ticks next-charge :punch-ticks next-punch))
      (cond
        (>= next-charge (cfg-int :charge.max-tolerant-ticks))
        (terminate-with-end! ctx-id false)
        (and punched? (> next-punch (cfg-int :charge.punch-anim-ticks)))
        (terminate-with-end! ctx-id true)))))

(defn- directed-blastwave-up!
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [charge-ticks (long (or (get-in ctx-data [:skill-state :charge-ticks]) 0))
          exp*         (scaling/clamp-exp (double (or exp 0.0)))]
      (if (and (> charge-ticks (cfg-int :charge.min-ticks))
               (< charge-ticks (cfg-int :charge.max-accepted-ticks)))
        (if-not cost-ok?
          (terminate-with-end! ctx-id false)
          (let [world-id (geom/world-id-of player-id)
                trace (target-trace player-id world-id)
                hit-pos (hit-pos-from-trace player-id trace)
                look (when (raycast/available?)
                       (raycast/player-look-vector player-id))
                entities (if (world-effects/available?)
                           (->> (world-effects/find-entities-in-radius
                                 world-id (:x hit-pos) (:y hit-pos) (:z hit-pos)
                                 (cfg-double :combat.aoe-radius))
                                (remove #(= (:uuid %) player-id))
                                vec)
                           [])
                damage (cfg-lerp :combat.damage exp*)]
            (skill-effects/set-main-cooldown!
             player-id :directed-blastwave
             (int (cfg-lerp :cooldown.ticks exp*)))
            (doseq [entity entities]
              (when (entity-damage/available?)
                (entity-damage/apply-direct-damage!
                 world-id
                 (:uuid entity)
                 (scaled-damage player-id (:uuid entity) damage)
                 :skill
                 {:attacker-uuid player-id}))
              ;; The original knockback call raises the target by 0.1, then
              ;; its following setMotion(delta) overwrites the knockback
              ;; velocity with this outward 0.24 motion.
              (let [moved-entity (update entity :y
                                         (fn [y] (+ (double (or y 0.0)) 0.1)))
                    push (push-velocity player-id moved-entity)]
                (when (motion-effects/entity-motion-available?)
                  (motion-effects/set-entity-position!
                   world-id (:uuid entity)
                   (:x moved-entity) (:y moved-entity) (:z moved-entity))
                  (motion-effects/set-entity-velocity!
                   world-id (:uuid entity)
                   (:x push) (:y push) (:z push)))))
            (break-nearby-blocks! player-id world-id hit-pos exp*)
            ;; Upstream s_perform sendToClient(MSG_PERFORM, position); each
            ;; recipient's c_perform (owner + nearby, no isLocal gate) then
            ;; locally triggers effectAt — a world-positioned sound + WaveEffect
            ;; — so every recipient independently renders it, bystanders
            ;; included. On air release the position is player pos + look*4 and
            ;; the effect fires the same way (exp-miss, no entities).
            (fx/send-local-and-nearby! ctx-id {:topic :directed-blastwave/fx-perform :mode :perform} nil {:pos hit-pos
                                                                                          :look-dir (or look {:x 0.0 :y 0.0 :z 1.0})})
            ;; Upstream l_effect (MSG_PERFORM) starts the punch anim under an
            ;; isLocal gate — owner-only send, unlike the fan-out above.
            (fx/send! ctx-id {:topic :directed-blastwave/fx-punch :mode :punch})
            (ctx-skill/replace-skill-state! ctx-id
                                            (assoc (:skill-state ctx-data)
                                                   :punched? true :punch-ticks 0 :performed? true))
            (skill-effects/add-skill-exp!
             player-id :directed-blastwave (if (seq entities)
                                             (cfg-double :progression.exp-hit)
                                             (cfg-double :progression.exp-miss)))
            (log/debug "DirectedBlastwave executed" "charge" charge-ticks
                      "entities" (count entities))))
        (terminate-with-end! ctx-id false)))))

(defn- directed-blastwave-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (terminate-with-end! ctx-id false))

(defskill directed-blastwave
  :id          :directed-blastwave
  :category-id :vecmanip
  :name-key    "ability.skill.vecmanip.directed_blastwave"
  :description-key "ability.skill.vecmanip.directed_blastwave.desc"
  :icon        "textures/abilities/vecmanip/skills/dir_blast.png"
  :ui-position [136 80]
  :ctrl-id     :directed-blastwave
  :pattern     :charge-window
  :cooldown    {:mode :manual}
  :cost        {:up {:cp       (fn [_player-id _skill-id exp]
                                 (cfg-lerp :cost.up.cp (double (or exp 0.0))))
                     :overload (fn [_player-id _skill-id exp]
                                 (cfg-lerp :cost.up.overload (double (or exp 0.0))))}}
  :actions
  {:down!  directed-blastwave-down!
   :tick!  directed-blastwave-tick!
   :up!    directed-blastwave-up!
   :abort! directed-blastwave-abort!}
  :prerequisites [{:skill-id :groundshock :min-exp 0.0}])


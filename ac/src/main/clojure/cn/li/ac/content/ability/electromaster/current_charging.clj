(ns cn.li.ac.content.ability.electromaster.current-charging
  "CurrentCharging - channel energy into blocks or held item.

  Pattern: :hold-channel
  Cost: overload lerp(65,48) on down; CP lerp(3,7)/tick while charging
  Exp: +0.0001 effective / +0.00003 ineffective per tick"
  (:require
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-manager :as ctx-mgr]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.block.multiblock-core :as multiblock]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as position]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world :as world]))

(def-skill-config-ops :current-charging)
(def ^:private current-charging-skill-id :current-charging)

(defn- targeting-range []
  (cfg-double :targeting.range))

(defn- main-hand-item [player-id]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :runtime-interop :get-player-main-hand-item player-id)))

(defn- player-view [player-id]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :runtime-interop :get-player-view player-id)))

(defn- block-entity-at [world-id x y z]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/call-adapter fw-atom :runtime-interop :get-block-entity-at world-id x y z)))

(defn- resolve-energy-target-tile
  "Route a hit multiblock part to its controller before checking energy APIs.

  AcademyCraft's old BlockMulti gave every Developer cell a TileDeveloper
  receiver. In this port, part cells carry a controller position and the
  authoritative machine energy lives on that controller. Keep the hit
  coordinates for the surround arc, but perform support lookup and charging
  against the controller so aiming at any visible Developer cell works."
  [hit-tile]
  (or
   (try
     (when-let [level (platform-be/be-get-world-safe hit-tile)]
       (let [hit-pos (position/block-pos hit-tile)
             block-id (platform-be/get-block-id hit-tile)
             controller-pos
             (when (and hit-pos block-id)
               (multiblock/resolve-controller-pos
                {:world level :pos hit-pos :block-id block-id}))]
         (when controller-pos
           (world/get-tile-entity level controller-pos))))
     (catch Throwable _
       nil))
   hit-tile))

(defn- target-structure-bounds
  "Block bounds of the machine the hit cell belongs to, or nil for a plain
  block. Upstream always draws the surround arc as a fixed 1x1x1 cube on the
  hit cell (EntitySurroundArc(world, x, y, z, 1, 1)) because it can only ever
  target the origin cell — a multiblock's other cells hold a bare TileMulti
  and fail EnergyBlockHelper.isSupported. This port charges through the
  controller from any cell, so it also reports the structure's extent and
  wraps the whole machine instead of whichever cell the crosshair landed on."
  [hit-tile]
  (try
    (when-let [level (platform-be/be-get-world-safe hit-tile)]
      (let [hit-pos (position/block-pos hit-tile)
            block-id (platform-be/get-block-id hit-tile)]
        (when (and hit-pos block-id)
          (multiblock/structure-bounds
           {:world level :pos hit-pos :block-id block-id}))))
    (catch Throwable _
      nil)))

(defn- view->pos [view]
  (when (map? view)
    {:x (double (:x view))
     :y (double (:y view))
     :z (double (:z view))}))

(defn- fx-payload [player-id payload]
  (cond-> (or payload {})
    (some? player-id) (assoc :source-player-id player-id)))

(defn- set-skill-state! [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- next-charge-ticks! [ctx-id]
  (let [current (long (or (get-in (ctx-skill/get-context ctx-id) [:skill-state :charge-ticks]) 0))
        next (inc current)]
    (set-skill-state! ctx-id [:charge-ticks] next)
    next))

(defn- active-ctx-id [player-id skill-id]
  (some (fn [[ctx-id ctx-data]]
          (when (and (= (:player-uuid ctx-data) player-id)
                     (= skill-id (:skill-id ctx-data)))
            ctx-id))
        (ctx/get-all-contexts)))

(defn- down-overload-cost [_player-id _skill-id exp]
  (cfg-lerp :cost.down.overload (double (or exp 0.0))))

(defn- current-overload
  [player-id fallback]
  (double
   (or (get-in (skill-effects/get-player-state player-id)
               [:resource-data :cur-overload])
       fallback
       0.0)))

(defn- tick-cp-cost [player-id _skill-id exp]
  (if-let [ctx-id (active-ctx-id player-id current-charging-skill-id)]
    (let [state (:skill-state (ctx-skill/get-context ctx-id))]
      (if (and (:is-item state) (nil? (main-hand-item player-id)))
        0.0
        (cfg-lerp :cost.tick.cp (double (or (:exp state) exp 0.0)))))
    (cfg-lerp :cost.tick.cp (double (or exp 0.0)))))

(defn- end-and-terminate! [ctx-id is-item player-id]
  (fx/send! ctx-id {:topic :current-charging/fx-end :mode :end} nil
            (fx-payload player-id {:is-item (boolean is-item)
                                   :caster-pos (get-in (ctx-skill/get-context ctx-id) [:skill-state :caster-pos])}))
  (ctx-skill/clear-skill-state! ctx-id)
  ;; Must pass the real notify callback, not nil: handle-key-up!/
  ;; handle-key-abort! (context_state.clj) also call terminate-context!
  ;; right after this skill callback returns, but terminate-context! only
  ;; fires send-terminated-fn once (guarded on status not already
  ;; :terminated) — whichever call transitions the status wins the
  ;; notification. Passing nil here let this call win with no client
  ;; notification, so the client's context list never learned the context
  ;; ended, permanently blocking V (has-active-contexts? stayed true).
  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))

(defn- charge-item-tick!
  [player-id ctx-id exp charge charge-ticks]
  (let [stack (main-hand-item player-id)]
    (if (nil? stack)
      (end-and-terminate! ctx-id true player-id)
      (let [effective? (energy/is-energy-item-supported? stack)
            caster-pos (view->pos (player-view player-id))]
        (when effective?
          (energy/charge-energy-to-item stack charge false))
        (skill-effects/add-skill-exp! player-id current-charging-skill-id
                                      (if effective?
                                        (cfg-double :progression.exp-effective)
                                        (cfg-double :progression.exp-ineffective)))
        (set-skill-state! ctx-id [:good?] (boolean effective?))
        (fx/send! ctx-id {:topic :current-charging/fx-update :mode :update} nil
                  (fx-payload player-id
                              {:is-item true
                               :good? (boolean effective?)
                               :charge-ticks charge-ticks
                               :exp exp
                               :caster-pos caster-pos}))))))

(defn- hit-distance
  [hit]
  (when (number? (:distance hit))
    (double (:distance hit))))

(defn- view-end
  [view distance]
  {:x (+ (double (:x view)) (* (double (:look-x view)) (double distance)))
   :y (+ (double (:y view)) (* (double (:look-y view)) (double distance)))
   :z (+ (double (:z view)) (* (double (:look-z view)) (double distance)))})

(defn- block-impact-point
  [view hit]
  (if (every? number? [(:hit-x hit) (:hit-y hit) (:hit-z hit)])
    {:x (double (:hit-x hit))
     :y (double (:hit-y hit))
     :z (double (:hit-z hit))}
    (view-end view (or (hit-distance hit) (targeting-range)))))

(defn- entity-impact-point
  [view hit]
  (cond
    ;; LambdaLib2 returns RayTraceResult(entity), whose hitVec is the
    ;; entity's position rather than the AABB intercept. CurrentCharging then
    ;; adds the entity eye height to that position for the visual endpoint.
    (every? number? [(:x hit) (:y hit) (:z hit)])
    {:x (double (:x hit))
     :y (+ (double (:y hit))
           (double (or (:eye-height hit) 0.0)))
     :z (double (:z hit))}

    (every? number? [(:hit-x hit) (:hit-y hit) (:hit-z hit)])
    {:x (double (:hit-x hit))
     :y (+ (double (:hit-y hit))
           (double (or (:eye-height hit) 0.0)))
     :z (double (:hit-z hit))}

    :else
    (view-end view (or (hit-distance hit) (targeting-range)))))

(defn- nearest-view-hit
  "Match LambdaLib2 Raytrace.traceLiving: trace blocks and collidable
   entities, exclude the caster, and let an entity win equal-distance ties."
  [player-id view]
  (when (raycast/available?)
    (let [world-id (or (:world-id view) "minecraft:overworld")
          range (targeting-range)
          block-hit (raycast/raycast-blocks
                     world-id
                     (double (:x view)) (double (:y view)) (double (:z view))
                     (double (:look-x view)) (double (:look-y view)) (double (:look-z view))
                     range)
          entity-hit (raycast/raycast-from-player player-id range false)
          block-distance (or (hit-distance block-hit) Double/POSITIVE_INFINITY)
          entity-distance (or (hit-distance entity-hit) Double/POSITIVE_INFINITY)]
      (cond
        (and entity-hit (<= entity-distance block-distance))
        {:hit-type :entity
         :hit entity-hit
         :target (entity-impact-point view entity-hit)}

        block-hit
        {:hit-type :block
         :hit block-hit
         :target (block-impact-point view block-hit)}

        :else
        {:hit-type :miss
         :hit nil
         :target (view-end view range)}))))

(defn- charge-block-target!
  "Trace exactly like the original skill: a nearer entity blocks charging,
   while the beam still terminates on that entity. Only block hits can charge."
  [player-id view charge]
  (let [world-id (or (:world-id view) "minecraft:overworld")
        {:keys [hit-type hit target]}
        (or (nearest-view-hit player-id view)
            {:hit-type :miss :hit nil :target (view-end view (targeting-range))})]
    (if (not= :block hit-type)
      {:effective? false :charged 0.0 :block-pos nil :ray-end target}
      (let [bx (int (:x hit)) by (int (:y hit)) bz (int (:z hit))
            hit-be (block-entity-at world-id bx by bz)
            energy-be (resolve-energy-target-tile hit-be)
            bounds (when hit-be (target-structure-bounds hit-be))]
        (if-not energy-be
          {:effective? false :charged 0.0 :block-pos [bx by bz] :block-bounds bounds
           :ray-end target}
          (cond
            (energy/is-node-supported? energy-be)
            {:effective? true
             :charged (max 0.0 (- (double charge)
                                  (double (energy/charge-node energy-be charge true))))
             :block-pos [bx by bz] :block-bounds bounds :ray-end target}

            (energy/is-receiver-supported? energy-be)
            {:effective? true
             :charged (max 0.0 (- (double charge)
                                  (double (energy/charge-receiver energy-be charge))))
             :block-pos [bx by bz] :block-bounds bounds :ray-end target}

            :else
            {:effective? false :charged 0.0 :block-pos [bx by bz] :block-bounds bounds
             :ray-end target}))))))

(defn- charge-block-tick!
  [player-id ctx-id _player charge charge-ticks]
  (let [view (player-view player-id)
        caster-pos (view->pos view)
        result (when view (charge-block-target! player-id view charge))
        {:keys [effective? charged block-pos block-bounds ray-end]}
        (or result {:effective? false :charged 0.0 :block-pos nil :block-bounds nil
                    :ray-end nil})]
    (skill-effects/add-skill-exp! player-id current-charging-skill-id
                                  (if effective?
                                    (cfg-double :progression.exp-effective)
                                    (cfg-double :progression.exp-ineffective)))
    (set-skill-state! ctx-id [:good?] (boolean effective?))
    (set-skill-state! ctx-id [:target] ray-end)
    (set-skill-state! ctx-id [:caster-pos] caster-pos)
    (set-skill-state! ctx-id [:block-pos] block-pos)
    (set-skill-state! ctx-id [:charged] (double charged))
    (fx/send! ctx-id {:topic :current-charging/fx-update :mode :update} nil
              (fx-payload player-id
                          {:is-item false
                           :good? (boolean effective?)
                           :charged (double charged)
                           :charge-ticks charge-ticks
                           :target ray-end
                           :caster-pos caster-pos
                           :block-pos block-pos
                           :block-bounds block-bounds}))))

(defn- current-charging-cost-fail!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [skill-state (:skill-state (or (ctx-skill/get-context ctx-id) {}))
        is-item (boolean (:is-item skill-state))
        player-id (:player-uuid (or (ctx-skill/get-context ctx-id) {}))]
    (end-and-terminate! ctx-id is-item player-id)))

(defn- current-charging-down!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player]
  (let [is-item (boolean (main-hand-item player-id))
        view (player-view player-id)
        caster-pos (view->pos view)
        ;; Upstream creates the held EntityArc as soon as EFFECT_START is
        ;; received, before the first context tick updates its endpoint.
        ;; Seed our arc with the same 15-block look endpoint so the start
        ;; packet alone is sufficient to render it.
        target (when (and (not is-item) (map? view))
                 (view-end view (targeting-range)))
        exp* (double (or exp 0.0))
        overload-cost (cfg-lerp :cost.down.overload exp*)
        ;; Upstream stores cpData.getOverload *after* the activation cost,
        ;; preserving any overload the player already had.
        overload-floor (current-overload player-id overload-cost)]
    (ctx-skill/replace-skill-state! ctx-id
                                    {:mode (if is-item :item :block)
                                     :is-item is-item
                                     :good? false
                                     :exp exp*
                                     :charge-ticks 0
                                     :overload-floor overload-floor
                                     :target target
                                     :caster-pos caster-pos
                                     :block-pos nil
                                     :charged 0.0})
    (fx/send! ctx-id {:topic :current-charging/fx-start :mode :start} nil
              (fx-payload player-id {:is-item is-item
                                     :target target
                                     :caster-pos caster-pos}))))

(defn- current-charging-tick!
  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage player]
  (when-let [skill-state (:skill-state (ctx-skill/get-context ctx-id))]
    (let [is-item (boolean (:is-item skill-state))
          exp (double (or (:exp skill-state) 0.0))
          charge (Math/floor (cfg-lerp :effect.charge-amount exp))
          charge-ticks (next-charge-ticks! ctx-id)
          overload-floor (double (or (:overload-floor skill-state) 0.0))]
      (skill-effects/enforce-overload-floor! player-id overload-floor)
      ;; Original ordering differs by mode:
      ;; - item: pay CP first, then charge only on success;
      ;; - block: charge/award exp first, then a failed payment ends the cast.
      ;; context-state invokes this callback even when the generic cost failed,
      ;; followed by :cost-fail!, so branch on cost-ok? only for item mode.
      (cond
        (and is-item cost-ok?)
        (charge-item-tick! player-id ctx-id exp charge charge-ticks)

        (not is-item)
        (charge-block-tick! player-id ctx-id player charge charge-ticks)))))

(defn- current-charging-up!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [{:keys [skill-state player-uuid]} (ctx-skill/get-context ctx-id)]
    (end-and-terminate! ctx-id (boolean (:is-item skill-state)) player-uuid)))

(defn- current-charging-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [{:keys [skill-state player-uuid]} (ctx-skill/get-context ctx-id)]
    (end-and-terminate! ctx-id (boolean (:is-item skill-state)) player-uuid)))

(declare current_charging_skill)

(defskill current_charging_skill
  :id          :current-charging
  :category-id :electromaster
  :name-key    "ability.skill.electromaster.current_charging"
  :description-key "ability.skill.electromaster.current_charging.desc"
  :icon        "textures/abilities/electromaster/skills/charging.png"
  :ui-position [55 18]
  :ctrl-id     :current-charging
  :pattern     :hold-channel
  :cooldown    {:mode :manual}
  :cost        {:down {:overload down-overload-cost}
                :tick {:cp tick-cp-cost}}
  :translations {:en_us {"ability.skill.electromaster.current_charging" "Current Charging"
                         "ability.skill.electromaster.current_charging.desc"
                         "Channel electricity into energy blocks or held energy items while holding."}
                 :zh_cn {"ability.skill.electromaster.current_charging" "电流充能"
                         "ability.skill.electromaster.current_charging.desc"
                         "按住持续引导电流，为目标能量方块或手持能量物品充能。"}
                 :zh_tw {"ability.skill.electromaster.current_charging" "電流充能"
                         "ability.skill.electromaster.current_charging.desc"
                         "按住持續引導電流，為目標能量方塊或手持能量物品充能。"}
                 :ja_jp {"ability.skill.electromaster.current_charging" "電流充能"
                         "ability.skill.electromaster.current_charging.desc"
                         "ホールドして電流を誘導し、対象のエネルギーブロックまたは手持ちのエネルギーアイテムを充能します。"}
                 :ko_kr {"ability.skill.electromaster.current_charging" "전류 충전"
                         "ability.skill.electromaster.current_charging.desc"
                         "홀드하여 전류를 유도하고 대상 에너지 블록 또는 손에 든 에너지 아이템을 충전합니다."}
                 :ru_ru {"ability.skill.electromaster.current_charging" "Текущая зарядка"
                         "ability.skill.electromaster.current_charging.desc"
                         "Удерживайте для направления тока, заряжая целевые энергоблоки или удерживаемые энергопредметы."}}
  :actions {:cost-fail! current-charging-cost-fail!
            :down!      current-charging-down!
            :tick!      current-charging-tick!
            :up!        current-charging-up!
            :abort!     current-charging-abort!}
  :prerequisites [{:skill-id :arc-gen :min-exp 0.3}])

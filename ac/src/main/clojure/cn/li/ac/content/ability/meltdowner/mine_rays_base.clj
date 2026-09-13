(ns cn.li.ac.content.ability.meltdowner.mine-rays-base
  "Shared hold-channel mining ray base logic for Basic/Expert/Luck variants.

  Each variant provides parameterized configuration; this namespace
  provides the actual tick/up/down implementations.

  Mining ray mechanic (matches original's MRContext):
  - Raycast in look direction each tick
  - Acquiring a NEW block only captures it (no countdown progress that tick)
  - Continuing to aim at the SAME block decrements a hardness countdown
  - Player must keep aiming at the same block to continue countdown
  - Mine-ray-luck grants fortune-style extra drops
  - Overload floor is snapshotted on activation and re-enforced every tick
  - Cooldown is applied on every termination path (up/abort/tick cost-fail)

  No Minecraft imports."
  (:require [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Mining tick logic (shared)
;; ---------------------------------------------------------------------------

(defn- empty-skill-state
  []
  {:target-x nil :target-y nil :target-z nil
   :hardness-left (double Float/MAX_VALUE)
   :starting-hardness (double Float/MAX_VALUE)})

(defn- with-floor
  "Carry :overload-floor forward across replace-skill-state! calls, which
  replace the whole :skill-state map."
  [state ctx-data]
  (if-let [floor (get-in ctx-data [:skill-state :overload-floor])]
    (assoc state :overload-floor floor)
    state))

(defn- apply-mining-cooldown!
  [cfg player-id]
  (let [{:keys [skill-id cooldown-ticks]} cfg]
    (when (and skill-id cooldown-ticks)
      (skill-effects/set-main-cooldown! player-id skill-id cooldown-ticks))))

(defn- ray-variant
  [skill-id]
  (case skill-id
    :mine-ray-expert :expert
    :mine-ray-luck :luck
    :basic))

(defn mining-ray-down!
  "Initialize mining ray context state.
  Matches original's s_onStart: overloadKeep snapshots the actual
  post-consumption overload stat, not the raw cost delta, and
  MSG_MADEALIVE spawns the ray + loop sound on the client."
  [skill-id ctx-id player-id _callback-skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when cost-ok?
    (let [overload-floor (double (or (skill-effects/player-path player-id [:resource-data :cur-overload] 0.0) 0.0))]
      (ctx-skill/replace-skill-state! ctx-id (assoc (empty-skill-state) :overload-floor overload-floor))
      ;; Original c_start (MSG_MADEALIVE) spawns the composite ray entity and
      ;; the loop sound for everyone nearby — the beam is world geometry, not
      ;; an isLocal overlay.
      (fx/send-local-and-nearby! ctx-id {:topic :mine-ray/fx-start :mode :start} nil
        {:variant (ray-variant skill-id)
         :source-player-id player-id}))))

(defn mining-ray-tick!
  "Tick handler for mining ray.
  cfg: {:range double :break-speed double :skill-id keyword :fortune-level int
        :exp-block double :tool-tier-capped? boolean :cooldown-ticks long}"
  [cfg ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (try
    (let [{:keys [range break-speed skill-id fortune-level exp-block tool-tier-capped?]} cfg
          ctx-data  (ctx-skill/get-context ctx-id)
          world-id  (geom/world-id-of player-id)
          eye       (geom/eye-pos player-id)
          look-vec  (when (raycast/available?)
                      (raycast/player-look-vector player-id))]
      ;; Matches original's s_onTick floor re-enforcement, run unconditionally
      ;; before any raycast/mining logic.
      (when-let [floor (get-in ctx-data [:skill-state :overload-floor])]
        (skill-effects/enforce-overload-floor! player-id floor))
      (if (and look-vec (bm/available?))
        (let [hit (raycast/raycast-blocks
                    world-id
                    (:x eye) (:y eye) (:z eye)
                    (:x look-vec) (:y look-vec) (:z look-vec)
                    (double range))]
          (if (nil? hit)
            (ctx-skill/replace-skill-state! ctx-id (with-floor (empty-skill-state) ctx-data))
            (let [hx (int (:x hit)) hy (int (:y hit)) hz (int (:z hit))
                  prev-x (get-in ctx-data [:skill-state :target-x])
                  prev-y (get-in ctx-data [:skill-state :target-y])
                  prev-z (get-in ctx-data [:skill-state :target-z])
                  same-target? (and (= hx prev-x) (= hy prev-y) (= hz prev-z))]
              (if same-target?
                ;; Original snapshots hardness when a block is acquired and
                ;; subtracts raw speed on later ticks. Negative hardness is
                ;; converted to Float/MAX_VALUE, so unbreakable blocks never
                ;; complete instead of being treated as very soft blocks.
                (let [prev-hardness (double (or (get-in ctx-data [:skill-state :hardness-left])
                                                Float/MAX_VALUE))
                      starting-hardness (double (or (get-in ctx-data [:skill-state :starting-hardness])
                                                   prev-hardness))
                      new-hardness (- prev-hardness (double break-speed))
                      progress (if (and (pos? starting-hardness)
                                        (< starting-hardness (double Float/MAX_VALUE)))
                                 (min 1.0 (max 0.0 (- 1.0 (/ new-hardness starting-hardness))))
                                 0.0)]
                  ;; Original's sendToClient(MSG_PARTICLES,...) has no isLocal
                  ;; gate in c_spawnParticles — visible to everyone nearby.
                  (fx/send-local-and-nearby! ctx-id {:topic :mine-ray/fx-progress} nil
                            {:x hx :y hy :z hz
                             :progress progress})
                  (if (<= new-hardness 0.0)
                    (do
                      ;; Permission was checked when this target was acquired,
                      ;; exactly where original posts BlockDestroyEvent.
                      (if (pos? (long (or fortune-level 0)))
                        (bm/break-block! player-id world-id hx hy hz true fortune-level)
                        (bm/break-block! player-id world-id hx hy hz true))
                      (skill-effects/add-skill-exp! player-id skill-id (double (or exp-block 0.001)))
                      (ctx-skill/replace-skill-state! ctx-id (with-floor (empty-skill-state) ctx-data)))
                    (ctx-skill/replace-skill-state! ctx-id
                                           (with-floor {:target-x hx
                                                        :target-y hy
                                                        :target-z hz
                                                        :hardness-left new-hardness
                                                        :starting-hardness starting-hardness}
                                                       ctx-data))))
                ;; New block acquired this tick: original only captures
                ;; x/y/z + starting hardness here — no countdown progress
                ;; and no particles until a SUBSEQUENT tick still aims at it.
                ;; Matches original's BlockDestroyEvent+harvestLevel gate: a
                ;; disallowed block is rejected (never tracked), same as a
                ;; canceled BlockDestroyEvent.
                (if (or (not (skill-effects/skill-destroy-allowed? skill-id))
                        (not (bm/can-break-block? player-id world-id hx hy hz))
                        (and tool-tier-capped? (bm/requires-high-tier-tool? world-id hx hy hz)))
                  (ctx-skill/replace-skill-state! ctx-id (with-floor (empty-skill-state) ctx-data))
                  (let [raw-hardness (double (or (bm/get-block-hardness world-id hx hy hz)
                                                 Float/MAX_VALUE))
                        hardness (if (neg? raw-hardness)
                                   (double Float/MAX_VALUE)
                                   raw-hardness)]
                    (ctx-skill/replace-skill-state! ctx-id
                                           (with-floor {:target-x hx
                                                        :target-y hy
                                                        :target-z hz
                                                        :hardness-left hardness
                                                        :starting-hardness hardness}
                                                       ctx-data))))))))
        (ctx-skill/replace-skill-state! ctx-id (with-floor (empty-skill-state) ctx-data))))
    (catch Exception e
      (log/debug "MiningRay tick! failed:" (ex-message e)))))

(defn- send-ray-end!
  [ctx-id player-id]
  ;; Original c_end (MSG_TERMINATED) kills the ray and stops the loop sound
  ;; on every nearby client — broadcast, like the start.
  (fx/send-local-and-nearby! ctx-id {:topic :mine-ray/fx-end :mode :end} nil
    {:source-player-id player-id}))

(defn mining-ray-up!
  "Key-up: reset mining state and apply cooldown.
  Matches original's unified s_terminated (MSG_TERMINATED) — every
  termination path sets the cooldown."
  [cfg ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (apply-mining-cooldown! cfg player-id)
  (send-ray-end! ctx-id player-id)
  (ctx-skill/replace-skill-state! ctx-id (empty-skill-state)))

(defn mining-ray-abort!
  "Abort: reset mining state and apply cooldown (same as key-up — see
  mining-ray-up!)."
  [cfg ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (apply-mining-cooldown! cfg player-id)
  (send-ray-end! ctx-id player-id)
  (ctx-skill/replace-skill-state! ctx-id (empty-skill-state)))

(defn mining-ray-cost-fail!
  "Matches original's s_onTick: !ctx.consume(...) still lets the current
  tick's mining logic run (already dispatched separately), but terminates
  the context and applies cooldown — same unified s_terminated path as
  key-up/key-abort."
  [cfg ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks cost-stage _player-ref]
  (when (= cost-stage :tick)
    (apply-mining-cooldown! cfg player-id)
    (ctx/terminate-context! ctx-id nil)))

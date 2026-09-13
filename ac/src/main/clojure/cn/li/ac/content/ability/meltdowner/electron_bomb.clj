(ns cn.li.ac.content.ability.meltdowner.electron-bomb
  "ElectronBomb skill - delayed MdBall settlement with single-ray hit.

  Pattern: :instant (key press fires the bomb)
  Cost: none
  Damage: lerp(6, 12, exp) at target, settled near MdBall end-of-life callback
  Cooldown: lerp(20, 10, exp) ticks
  Exp: +0.005 per cast, unconditional (matches original's ctx.addSkillExp
  firing immediately regardless of whether the delayed ray hits)

  Implementation note: the 'ball' delay is driven by delayed projectile
  settlement and the client FX receives a single-ray visual event. The
  settle delay shortens once skill exp exceeds charge.improved-exp-threshold,
  matching original's Life(20)/LifeImproved(5) split.

  No Minecraft imports."
  (:require
            [cn.li.ac.config.modid :as modid] [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.delayed-projectiles :as delayed-projectiles]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :electron-bomb)
(def ^:private mdball-entity-id (modid/namespaced-path "entity_md_ball"))
;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

;; ---------------------------------------------------------------------------
;; Action
;; ---------------------------------------------------------------------------

(defn- life-ticks-for-exp
  [exp]
  (if (> (double (or exp 0.0)) (cfg-double :charge.improved-exp-threshold))
    (cfg-int :charge.settle-ticks-improved)
    (cfg-int :charge.settle-ticks)))

(defn- perform-electron-bomb! [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage player-ref]
  (try
    (let [damage   (cfg-lerp :combat.damage exp)
          eye      (geom/eye-pos player-id)
          look-vec (when (raycast/available?)
                     (raycast/player-look-vector player-id))]
      (when look-vec
        ;; Tracked spawn: keep the ball's uuid so the settle task can read the
        ;; ball's current orbit position and shoot the ray from it (matching
        ;; the original's EntityMdBall callback, which raycasts from ball.pos
        ;; toward getDest(player)).
        (let [life-ticks (life-ticks-for-exp exp)
              ;; Ball dies at life-ticks (life override below); the settle ray
              ;; fires life-ticks - 2 — 2-tick margin so the ball entity is
              ;; still resolvable at settle time, visually the ray appears
              ;; just as the ball expires (original death callback).
              ball-uuid (when player-ref
                          (entity/player-spawn-tracked-entity-by-id!
                            player-ref
                            mdball-entity-id
                            0.0
                            life-ticks))]
          ;; Send spawn FX first; the delayed task owns the actual hit settlement.
          ;; Original has no explicit sendTo* here at all — ElectronBomb relies
          ;; entirely on the base Context's implicit MSG_MAKEALIVE replication
          ;; (owner + nearby), so every visible/audible part of casting it is
          ;; broadcast by default.
          (fx/send-local-and-nearby! ctx-id {:topic :electron-bomb/fx-spawn} nil
                    {:x (:x eye) :y (:y eye) :z (:z eye)
                     :dx (:x look-vec) :dy (:y look-vec) :dz (:z look-vec)})
          ;; Exp/cooldown are granted at cast time in original, unconditionally,
          ;; before the ball's delayed callback ever fires.
          (skill-effects/add-skill-exp! player-id :electron-bomb (cfg-double :progression.exp-hit))
          (skill-effects/set-main-cooldown! player-id :electron-bomb
                                            (cfg-lerp-int :cooldown.ticks exp))
          (let [delay-ticks (delayed-projectiles/mdball-near-expire-delay life-ticks 2)]
            (delayed-projectiles/schedule-electron-bomb-beam!
              {:player-id player-id
               :ctx-id ctx-id
               :damage damage
               :ball-uuid ball-uuid
               :delay-ticks delay-ticks})
            (log/debug "ElectronBomb: scheduled delayed beam"
                       {:delay delay-ticks
                        :player player-id})))))
    (catch Exception e
      (log/warn "ElectronBomb perform! failed:" (ex-message e)))))

(defn electron-bomb-perform!
  [& args]
  (apply perform-electron-bomb! args))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill electron-bomb
  :id             :electron-bomb
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.electron_bomb"
  :description-key "ability.skill.meltdowner.electron_bomb.desc"
  :icon           "textures/abilities/meltdowner/skills/electron_bomb.png"
  :ui-position    [15 45]
  :ctrl-id        :electron-bomb
  :pattern        :instant
  :cooldown       {:mode :manual}
  :cooldown-ticks (fn [{:keys [exp]}]
                    (cfg-lerp-int :cooldown.ticks (double (or exp 0.0))))
  :actions        {:perform! electron-bomb-perform!}
  :prerequisites  [])


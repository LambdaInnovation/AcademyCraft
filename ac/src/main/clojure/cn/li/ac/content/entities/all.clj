(ns cn.li.ac.content.entities.all
  "Content entrypoint for AC entity declarations."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.tutorial.config :as tutorial-config]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.entity.dsl :as edsl]
            [cn.li.mcmod.runtime.install :as install]))

(defn init-entities!
  []
  (install/framework-once! ::entities-installed?
  (fn []
    ;; Keep existing registry IDs stable for world/network compatibility.
    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_mag_hook"
        {:entity-kind :scripted-projectile
         :category :misc
         :width 0.5
         :height 0.5
         :client-tracking-range 64
         :update-interval 1
         :properties {:projectile {:default-item-id (modid/namespaced-path "mag_hook")
                                   :gravity 0.05
                                   :damage 4.0
                                   :drop-item-on-discard? true
                                   :hooks {:on-hit-block :anchor
                                           :on-hit-entity :damage-and-discard
                                           :on-anchored-tick :drop-when-invalid
                                           :on-anchored-hurt :discard-when-hurt}}}}))
    (edsl/register-entity!
      (edsl/create-entity-spec
        "railgun_charge"
        {:entity-kind :scripted-effect
         :category :misc
         :width 0.1
         :height 0.1
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 32
                               :follow-owner? true
                               :renderer-id "railgun-charge-glow"
                               :hook :noop}}}))
    (edsl/register-entity!
      (edsl/create-entity-spec
        "intensify_effect"
        {:entity-kind :scripted-effect
         :category :misc
         :width 0.1
         :height 0.1
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 15
                               :follow-owner? true
                               :renderer-id "intensify-arcs"
                               :hook :intensify-arcs
                               :hook-params {:arc-life-ticks 3
                                             :tier-batch-base 3
                                             :tier-batch-random 2
                                             :tier-radius-base 0.5
                                             :tier-radius-random 0.1
                                             :tier-theta-max 6.283185307179586
                                             ;; ScriptedEffectEntity is anchored at
                                             ;; owner Y+1; upstream tiers are relative
                                             ;; to owner Y, so cancel that shared offset.
                                             :tier-origin-offset-y -1.0
                                             :tier-heights [2.0 1.8 1.5 1.0 0.5 0.0 -0.1]
                                             :tier-delays [0 1 3 4 6 7 8]
                                             :branch-count-base 1
                                             :branch-count-random 2
                                             :branch-points-base 3
                                             :branch-points-random 2
                                             :side-amp-base 0.06
                                             :side-amp-random 0.05
                                             :rise-base 0.26
                                             :rise-random 0.1
                                             :rebound-base 0.12
                                             :rebound-random 0.07
                                             :branch-theta-base 0.55
                                             :branch-theta-random 0.4
                                             :branch-len-base 0.12
                                             :branch-len-random 0.09
                                             :branch-wobble-amp 0.025
                                             :arc-damp-factor 0.65
                                             :arc-wobble-freq 6.8
                                             :arc-pull-factor 0.22
                                             :branch-wobble-phase-mul 0.7
                                             :branch-wobble-time-mul 5.2
                                             :branch-vel-y 0.05
                                             :branch-accel-y 0.03
                                             :phase-max 6.283185307179586
                                             :flicker-seed-scale 13.0
                                             :main-points 7
                                             :branch-attach-start 2
                                             :branch-attach-random-span-sub 3}}}}))

    ;; EntityCoinThrowing - Railgun skill QTE throw; also carries upstream's
    ;; standalone ItemCoin/EntityCoinThrowing "heads or tails" flavor message
    ;; (upstream: right-click-throw a coin, random chat message on landing,
    ;; gated by the "Play Heads or Tails" setting). This port merged that
    ;; item/entity into the Railgun QTE, so the message now fires whenever
    ;; *any* thrown coin lands — including QTE throws — matching upstream's
    ;; unconditional-on-landing behavior as closely as the merged design allows.
    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_coin_throwing"
        {:entity-kind :scripted-effect
         :category :misc
         :width 0.2
         :height 0.2
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 120
                               :follow-owner? true
                               :hook :vertical-ballistic
                               :hook-params {:gravity 0.06
                                             :init-vel 0.92
                                             :return-item-id (modid/namespaced-path "coin")
                                             :on-landed-fn
                                             ;; Ticks (and thus this callback) run on every
                                             ;; nearby client, not just the thrower's — only
                                             ;; the throwing player's own client can actually
                                             ;; display a system message via itself, so guard
                                             ;; on identity with the local client player
                                             ;; (matches upstream's implicit isRemote-and-
                                             ;; it's-my-own-EntityPlayerSP behavior).
                                             (fn [owner-player]
                                               (when (and (tutorial-config/heads-or-tails-enabled?)
                                                          (= owner-player (bridge/get-client-player)))
                                                 (bridge/send-system-message! owner-player
                                                   (str "heads_or_tails." modid/MOD-ID "." (rand-int 2)))))}
                               :renderer-id "railgun-coin"}}}))

    ;; No ray family here any more. entity_md_ray_small / entity_md_ray_barrage
    ;; / entity_barrage_ray_pre were registered with (mostly) upstream's
    ;; RendererRayComposite numbers and then never spawned by anything: every
    ;; skill draws its rays through the level-effect plan and
    ;; effects/ray-composite instead. The :scripted-ray entity kind itself
    ;; stays — it is platform facility, and the loaders fall back to the
    ;; "ray-composite" profile when no content spec claims it.

    ;; Marker/UI family.
    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_tp_marking"
        {:entity-kind :scripted-marker
         :category :misc
         :width 0.5
         :height 0.5
         :client-tracking-range 64
         :update-interval 1
         :properties {:marker {:life-ticks 120
                               :follow-target? true
                               :ignore-depth? false
                               :available? true
                               :renderer-id "tp-marking"
                               :hook :tp-marking}}}))

    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_marker"
        {:entity-kind :scripted-marker
         :category :misc
         :width 0.5
         :height 0.5
         :client-tracking-range 64
         :update-interval 1
         :properties {:marker {:life-ticks 120
                               :follow-target? true
                               :ignore-depth? true
                               :available? true
                               :renderer-id "wire-marker"
                               :hook :marker}}}))

    ;; Shield / arc visuals (effect family).
    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_diamond_shield"
        {:entity-kind :scripted-effect
         :category :misc
         :width 1.8
         :height 1.8
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 120
                               :follow-owner? false
                               :renderer-id "diamond-shield"
                               :hook :diamond-shield
                               :hook-params {:forward 1.0
                                             :vertical 1.1}}}}))

    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_md_shield"
        {:entity-kind :scripted-effect
         :category :misc
         :width 1.8
         :height 1.8
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 120
                               :follow-owner? false
                               :renderer-id "md-shield"
                               :hook :md-shield
                               :hook-params {:forward 1.0
                                             :vertical 1.1}}}}))

    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_blood_splash"
        {:entity-kind :scripted-effect
         :category :misc
         :width 1.0
         :height 1.0
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 10
                               :follow-owner? false
                               :renderer-id "blood-splash"
                               :hook :blood-splash}}}))

    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_md_ball"
        {:entity-kind :scripted-effect
         :category :misc
         :width 0.6
         :height 0.6
         :client-tracking-range 64
         :update-interval 1
         :properties {:effect {:life-ticks 50
                               :follow-owner? true
                               :renderer-id "md-ball"
                               :hook :md-ball
                               :hook-params {:range-from 0.8
                                             :range-to 1.3
                                             ;; Upstream subY is rangef(-1.2, 0.2)
                                             ;; from the FEET, and the renderer
                                             ;; then adds a hard-coded +1.6 —
                                             ;; the balls read at 0.4..1.8. Bake
                                             ;; that in so the entity is where it
                                             ;; looks (the rays start there too).
                                             :y-from 0.4
                                             :y-to 1.8
                                             :wobble-xz 0.03
                                             :wobble-y 0.04
                                             :phase-step 0.18
                                             :wobble-y-freq 1.4
                                             :wobble-y-phase-shift 0.8975979010256552
                                             :theta-spread-factor 0.45}}}}))

    ;; Block-body family.
    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_magmanip_block_body"
        {:entity-kind :scripted-block-body
         :category :misc
         :width 1.0
         :height 1.0
         :client-tracking-range 64
         :update-interval 1
         :properties {:block-body {:default-block-id "minecraft:stone"
                                   :gravity 0.04
                                   ;; Matches original's new MagManipEntityBlock(player, 10) — hardcoded,
                                   ;; not exp-scaled (the original's exp-lerp'd damage field was declared
                                   ;; but never actually passed to the entity).
                                   :damage 10.0
                                   ;; Original keeps placement disabled while
                                   ;; held and enables it only on release/abort.
                                   :place-when-collide? false
                                   :renderer-id "block-body"
                                   :hook :magmanip-block
                                   :behavior :mag-manip-damage}}}))

    (edsl/register-entity!
      (edsl/create-entity-spec
        "entity_silbarn"
        {:entity-kind :scripted-block-body
         :category :misc
         :width 0.4
         :height 0.4
         :client-tracking-range 64
         :update-interval 1
         :properties {:block-body {:default-block-id "minecraft:iron_block"
                                   :gravity 0.12
                                   :damage 0.0
                                   :place-when-collide? false
                                   ;; Not "block-body": the silbarn is a
                                   ;; behavior-driven OBJ model (upstream
                                   ;; EntitySilbarn$RenderSibarn), resolved per
                                   ;; entity by BehaviorObjRenderer from the
                                   ;; hook -> render-ns registry.
                                   :renderer-id "silbarn"
                                   :hook :silbarn
                                   :behavior :impact-detonation
                                   ;; Original's Rigidbody linearDrag 0.8 —
                                   ;; strong per-tick drag: the silbarn
                                   ;; glides ~5 blocks then hovers while the
                                   ;; 50-tick gravity delay (see
                                   ;; ScriptedBlockBodyEntity.getGravity)
                                   ;; keeps it airborne.
                                   :drag 0.8}}}))

    ;; Minimal scripted-mob for bundle pipeline /summon smoke (dev only).
    (edsl/register-entity!
      (edsl/create-entity-spec
        "scripted-test-mob"
        {:entity-kind :scripted-mob
         :category :monster
         :width 0.6
         :height 1.8
         :client-tracking-range 64
         :update-interval 3
         :properties {:mob {:mob-tick-fn (fn [_mob] nil)
                            :mob-hurt-fn (fn [_mob _src amt] amt)
                            :mob-death-fn (fn [_mob _src] nil)
                            :mob-loot-fn (fn [_mob _src _recent?] false)}}})))))

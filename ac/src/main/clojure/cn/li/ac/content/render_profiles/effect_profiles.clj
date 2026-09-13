(ns cn.li.ac.content.render-profiles.effect-profiles
  "AC-owned ScriptRender profile definitions.

  Keeps renderer semantics in AC while execution stays in mc1201 runtime."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.runtime.install :as install]
	    [cn.li.mcmod.client.render.script-render-abi :as script-abi]
	    [cn.li.mcmod.client.render.script-render-registry :as script-registry]))

(def ^:private v1-effect-profiles
  [{:id "effect-billboard"
    :kind :billboard-cross
    :state {:layer :translucent
	    :blend :alpha}
    :params {:size 0.6
	     :color-r 180 :color-g 220 :color-b 255}}

   ;; Diamond shield for LightShield (matching original EntityMdShield +
   ;; RenderMdShield: single translucent quad, oriented to the owner's
   ;; yaw/pitch, spinning around its own normal — never edge-on — SIZE 1.8
   ;; grown from 0.2x over 15 ticks, alpha fade-in over 6 ticks).
   ;; JetEngine's diamond shield reuses the same visual with its own texture.
   {:id "md-shield"
    :kind :spinning-shield
    :state {:layer :translucent
            :blend :alpha
            :cull :off}
    :params {:texture (modid/namespaced-path "textures/effects/md_shield/0.png")
             :scale 1.8}}

   ;; JetEngine's diamond shield (matching original EntityDiamondShield +
   ;; RenderDiamondShield): a 3D DIAMOND PYRAMID — four triangle faces from
   ;; the apex (0,0,1) to the rim of the diamond base, oriented to the
   ;; owner's yaw/pitch — NOT the flat spinning quad the light-shield uses.
   ;; RenderDiamondShield's scale is a flat glScalef(1.5); the 1.8 that was
   ;; here is EntityDiamondShield.SIZE, which only sizes the bounding box.
   {:id "diamond-shield"
    :kind :diamond-pyramid
    :state {:layer :translucent
            :blend :alpha
            :cull :off}
    :params {:texture (modid/namespaced-path "textures/effects/diamond_shield.png")
             :scale 1.5}}

   {:id "marker-billboard"
    :kind :billboard-cross
    :state {:layer :translucent
	    :blend :alpha}
    :params {:size 0.45
  	     :color-r 240 :color-g 255 :color-b 180}}

     ;; Tiered zigzag arcs for body intensify effect.
     ;; Matching original EntityIntensifyEffect (EntitySurroundArc THIN
     ;; with 7 height tiers, staggered delays, zigzag sub-arcs).
     {:id "intensify-arcs"
      :kind :intensify-arcs
      :state {:layer :lines
              :blend :alpha}
      :params {:life-ticks 15
               :arc-life-ticks 3
               :tier-heights [2.0 1.8 1.5 1.0 0.5 0.0 -0.1]
               :tier-delays [0 1 3 4 6 7 8]}}

     ;; RailgunHandEffect: arc_burst/0..39, one frame per 40 ms.
     {:id "railgun-charge-glow"
      :kind :animated-billboard
      :state {:layer :translucent
              :blend :alpha}
      :params {:texture-prefix
               (modid/namespaced-path "textures/effects/arc_burst/")
               :frame-count 40
               :frame-ms 40.0
               ;; Third-person RailgunHandEffect draws the billboard
               ;; createBillboard(-1,-1,1,1) with no scale at all — 2x2 units,
               ;; i.e. half-size 1.0. (Only the first-person branch scales it,
               ;; by 0.4, and that path is the hand quad in impl/railgun_shot.)
               :half-size 1.0
               :offset-y 0.8
               ;; Upstream's glTranslated(0, 1.8, -1) is in the player MODEL
               ;; space, which RenderLivingBase has already spun by
               ;; 180 - renderYawOffset — so -1 on Z there is one block IN
               ;; FRONT. This renderer's offset frame is the owner's own yaw
               ;; (+Z is where they are looking), so it needs +1: with -1 the
               ;; burst hung behind the caster's head.
               :offset-z 1.0}}

     ;; EntityCoinThrowing/RendererCoinThrowing: two-sided coin, player-relative
     ;; offset, 0.3 scale and a 300 ms full rotation around a per-entity axis.
     {:id "railgun-coin"
      :kind :spinning-double-sided
      :state {:layer :translucent
              :blend :alpha
              :cull :off}
      :params {:front-texture
               (modid/namespaced-path "textures/item/coin_front.png")
               :back-texture
               (modid/namespaced-path "textures/item/coin_back.png")
               :scale 0.3
               :offset-x -0.63
               :offset-y 1.0
               :offset-z 0.3
               :rotation-period-ms 300.0}}

   {:id "ray-composite"
      :kind :ray-composite
      :state {:layer :translucent
        :blend :alpha}
      :params {:length 15.0
         :inner-width 0.17
         :outer-width 0.22
         :glow-width 1.5
         :blend-in-ms 200.0
         :blend-out-ms 700.0
         :start-color 0xD8F8D8
         :end-color 0x6AF26A}}

     ;; MdBall (electron-bomb/electron-missile/scatter-bomb entity).
     ;; EntityMdBall.R: a soft glow quad (upstream size 0.7) behind a brighter
     ;; core quad (size 0.5), both flickering — see the md-ball renderer. The
     ;; generic animated-billboard kind draws one opaque quad on a fixed frame
     ;; cycle, which is neither.
     {:id "md-ball"
      :kind :md-ball
      :state {:layer :translucent
              :blend :alpha}
      :params {:texture-prefix (modid/namespaced-path "textures/effects/mdball/")
               :glow-texture (modid/namespaced-path "textures/effects/mdball/glow.png")
               :frame-count 5
               :glow-size-factor 0.35
               :core-size-factor 0.25
               ;; getAlpha(): 0 -> 0.6 over 0.3s, held, then a flare to full
               ;; over the 0.25s before death and a drop to nothing in the last
               ;; 0.15s. getSize(): holds at 1.0, swells to 1.5 over the 200ms
               ;; before the last 100ms, then collapses to 0 in those 100ms.
               ;; Only ElectronBomb's ball (20 ticks, or 5 once trained)
               ;; lives short enough to show the last two.
               :alpha-hold 0.6
               :alpha-attack-seconds 0.3
               :alpha-burst-seconds 0.4
               :alpha-blend-seconds 0.15}}

     ;; EntityBloodSplash (flesh-ripping hit splash): 10-frame splash,
     ;; matching the pre-refactor BloodSplashRenderer.
     {:id "blood-splash"
      :kind :animated-billboard
      :state {:layer :translucent
              :blend :alpha}
      :params {:texture-prefix (modid/namespaced-path "textures/effects/blood_splash/")
               :frame-count 10
               :frame-ms 50.0
               :half-size 0.4
               :offset-y 0.0
               :offset-z 0.0}}])

(def ^:private ac-effect-kinds
  #{:intensify-arcs :spinning-shield :diamond-pyramid :md-ball})

(defn init-render-profiles!
  []
  (install/framework-once! ::render-profiles-installed?
  (fn []
    (doseq [k ac-effect-kinds]
      (script-abi/register-scripted-effect-kind! k))
    ;; Map content-owned kind keywords to platform-neutral renderer keys
    ;; so the Minecraft component can dispatch without hardcoding content-specific strings.
    (script-abi/register-kind-renderer-key! :intensify-arcs :tiered-zigzag)
    (script-abi/register-kind-renderer-key! :spinning-shield :spinning-shield)
    (script-abi/register-kind-renderer-key! :diamond-pyramid :diamond-pyramid)
    (script-abi/register-kind-renderer-key! :md-ball :md-ball)
    (script-registry/register-profiles! v1-effect-profiles))))

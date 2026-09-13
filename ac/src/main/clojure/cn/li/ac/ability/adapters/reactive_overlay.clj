(ns cn.li.ac.ability.adapters.reactive-overlay
  "Reactive HUD overlay — native node tree + signals; no build-client-overlay-plan."
  (:require [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.ac.ability.client.hud :as hud]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.dsl :as dsl]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.ui.anim :as anim]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.signal ISigO]))

(defonce ^:private mode-switch-flags (boolean-array 2))
(defonce ^:private mode-switch-time (long-array 1))

;; Upstream CPBar.cpColors: red→orange→white as the CP bar fills.
(def ^:private cp-color-stops [[0.0 240 103 103] [0.35 255 174 68] [1.0 255 255 255]])
;; Upstream CPBar.overrideColors: the "normal state" overload preview tint
;; (transparent gray → tan → red), each stop's alpha (0-255) included.
(def ^:private overload-preview-stops
  [[0.0 10 223 223 223] [0.55 35 240 212 157] [1.0 80 245 100 100]])
(def ^:private overload-preview-color-stops
  ;; Same stops without alpha, consumed by the generic progress renderer.
  [[0.0 223 223 223] [0.55 240 212 157] [1.0 245 100 100]])

(defn- sample-argb-stops
  "3-stop [pos a r g b] lerp (upstream autoLerp, extended with alpha) for the
   overload-preview box tint. t in [0,1]."
  [stops ^double t]
  (let [n (count stops)]
    (loop [i 0]
      (if (>= i n)
        (let [[_ a r g b] (nth stops (dec n))] [(int a) (int r) (int g) (int b)])
        (let [[pos a r g b] (nth stops i)
              pos (double pos)]
          (if (>= pos t)
            (if (zero? i)
              [(int a) (int r) (int g) (int b)]
              (let [[pos0 a0 r0 g0 b0] (nth stops (dec i))
                    pos0 (double pos0)
                    span (- pos pos0)
                    f (if (zero? span) 0.0 (/ (- t pos0) span))]
                [(int (+ (double a0) (* (- (double a) (double a0)) f)))
                 (int (+ (double r0) (* (- (double r) (double r0)) f)))
                 (int (+ (double g0) (* (- (double g) (double g0)) f)))
                 (int (+ (double b0) (* (- (double b) (double b0)) f)))]))
            (recur (inc i))))))))

(defn on-mode-switch-key-state!
  "Track V-key hold for CP/OL numeric readout fade in/out."
  [is-down]
  (let [^booleans flags mode-switch-flags
        ^longs time-cell mode-switch-time
        was-down (aget flags 0)
        now (System/currentTimeMillis)]
    (cond
      (and (not was-down) is-down)
      (do
        (aset-boolean flags 1 true)
        (aset-long time-cell 0 now))

      (and was-down (not is-down))
      (do
        (aset-boolean flags 1 false)
        (aset-long time-cell 0 (if (> (- now (aget time-cell 0)) 400) now 0))))
    (aset-boolean flags 0 (boolean is-down)))
  nil)

(defn- local-player-uuid [] (bridge/local-player-uuid))

(defn- mask-vec [{:keys [r g b a]}]
  [(double (or r 0.0)) (double (or g 0.0)) (double (or b 0.0)) (double (or a 0.0))])

(defn- write-vignette-from-rgba-o!
  "Drive the :bg-mask vignette image from the smoothed [r g b a] color signal:
   rgb (0..1) → image tint (0..255), a → image alpha. The texture
   (effects/screen_mask.png, upstream AcademyCraft's screen mask) is a white
   edge glow with a transparent center, so tint×alpha yields the original
   'edge ring in category color' look instead of a full-screen solid fill."
  [^INode n source]
  (let [rgba (sig/sget-o ^ISigO source)
        a (double (nth rgba 3))]
    (when-not (== a (.getDSlot n 0))
      (.setDSlot n 0 a)
      (.setFlag n node/FLAG-RENDER-DIRTY))
    (let [nr (* 255.0 (double (nth rgba 0)))
          ng (* 255.0 (double (nth rgba 1)))
          nb (* 255.0 (double (nth rgba 2)))
          tint (.getOSlot n 1)]
      (when-not (and (vector? tint)
                     (== (double (nth tint 0)) nr)
                     (== (double (nth tint 1)) ng)
                     (== (double (nth tint 2)) nb))
        (.setOSlot n 1 [nr ng nb 255.0])
        (.setFlag n node/FLAG-RENDER-DIRTY)))))

(defn- rgb-vec->argb [[r g b] alpha]
  (bit-or (bit-shift-left (int (* 255.0 (double alpha))) 24)
          (bit-shift-left (int r) 16)
          (bit-shift-left (int g) 8)
          (int b)))

(defn- rgba-map->argb [{:keys [r g b a]}]
  (bit-or (bit-shift-left (int (or a 255)) 24)
          (bit-shift-left (int (or r 0)) 16)
          (bit-shift-left (int (or g 0)) 8)
          (int (or b 0))))

(defn- set-box-node-at! [_r ^INode n x y w h rgba]
  (when n
    (let [x* (double x)
          y* (double y)
          w* (double w)
          h* (double h)
          color (double (unchecked-int (rgba-map->argb rgba)))]
      (when (or (not (== x* (.getX n)))
                (not (== y* (.getY n)))
                (not (== w* (.getW n)))
                (not (== h* (.getH n)))
                (not (== color (.getDSlot n 0))))
        (.setX n x*)
        (.setY n y*)
        (.setW n w*)
        (.setH n h*)
        (.setDSlot n 0 color)
        (.setFlag n node/FLAG-LAYOUT-DIRTY)
        (.setFlag n node/FLAG-RENDER-DIRTY)))))

(defn- set-box-rgba! [r id rgba]
  (when-let [^INode n (ui/node r id)]
    (set-box-node-at! r n (.getX n) (.getY n) (.getW n) (.getH n) rgba)))

(defn- set-box-at! [r id x y w h rgba]
  (when-let [^INode n (ui/node r id)]
    (set-box-node-at! r n x y w h rgba)))

(defn- toast-template []
  (dsl/group {:id :toast :w 200 :h 32}
    (dsl/box {:id :bg :x 0 :y 0 :w 200 :h 32 :fill 0x77272727})
    (dsl/box {:id :border-t :x 0 :y 0 :w 200 :h 1 :fill 0xAAFFFFFF})
    (dsl/box {:id :border-b :x 0 :y 31 :w 200 :h 1 :fill 0xAAFFFFFF})
    (dsl/box {:id :border-l :x 0 :y 0 :w 1 :h 32 :fill 0xAAFFFFFF})
    (dsl/box {:id :border-r :x 199 :y 0 :w 1 :h 32 :fill 0xAAFFFFFF})
    (dsl/text {:id :msg :x 8 :y 9 :text "" :color 0xFFFFFFFF})))

;; Upstream KeyHintUI: the LIVE hud instance (not the editor-preview widget,
;; which is a separate 140×210@SCALE*2=0.46 object only used by the preset
;; editor) renders at scale(SCALE)=0.23. Shared by the list-node's own :scale
;; prop and the glow-line/cooldown-wipe dslot math below (those write already-
;; scaled screen pixels, not auto-scaled — see update fns).
(def ^:private skill-slot-scale 0.23)

;; Upstream KeyHintUI is an independent top-level widget — walign(RIGHT),
;; halign(CENTER), pos(0,30) on a declared 140×210 box — vertically centered
;; on the right screen edge, NOT anchored to the CP bar. drawSingle further
;; GL-translates each key-group by (-200 - availIdx*200, 0) before drawing;
;; the preset/preset-data model here only ever has one group (availIdx=0).
(defn- skill-slot-anchor [sw sh]
  (let [w (* 140.0 skill-slot-scale)
        h (* 210.0 skill-slot-scale)
        group-dx (* -200.0 skill-slot-scale)]
    [(+ (- sw w) group-dx)
     (+ (- (/ sh 2.0) (/ h 2.0)) 30.0)]))


(defn- key-cap-texture [key-label]
  ;; Upstream KeyHintUI.drawSingle: MOUSE_LEFT/MOUSE_RIGHT draw TEX_MOUSE_L/
  ;; TEX_MOUSE_R (no key character); any other key draws key_short/key_long
  ;; (Keyboard.getKeyName length <= 2 → key_short, else key_long) plus the
  ;; key character text.
  (modid/asset-path "textures"
                    (case key-label
                      :mouse-left "guis/key_hint/mouse_left.png"
                      :mouse-right "guis/key_hint/mouse_right.png"
                      (if (<= (count (str key-label)) 2)
                        "guis/key_hint/key_short.png"
                        "guis/key_hint/key_long.png"))))

;; Upstream KeyHintUI: `new FontOption(32, FontAlign.CENTER,
;; Colors.fromHexColor(0xff194246))` — the key character is a dark teal that
;; reads against the key-cap plate, never white.
(def ^:private key-label-color 0xFF194246)

;; While !canUseAbility or the skill is cooling down, upstream binds ShaderMono
;; (mono.frag: c = (r+g+b)/3) for the key cap + character, and the font's own
;; Colors.bindToGL(option.color) overrides the 0.7 grey glColor — so the dimmed
;; character is flat grey (0x19+0x42+0x46)/3 = 0x36, not a lighter teal.
(def ^:private key-label-color-dim 0xFF363636)

;; Font sizes below are authored in the same local design units as x/y/w/h:
;; render-text! multiplies :font-size by the node's cum-scale, exactly like the
;; box/image geometry. Pre-multiplying by skill-slot-scale (as this template
;; used to) therefore applied 0.23 twice and drew upstream's 32-unit glyphs at
;; 1.6px — an illegible smear that read as a plain white blob for letter keys,
;; and an unreadably small cooldown readout.
(def ^:private key-label-font-size 32.0)

(defn- skill-slot-template []
  ;; Upstream KeyHintUI.drawSingle raw local-unit offsets (auto-scaled via the
  ;; list's :scale 0.23, matching upstream's own pre-scale GL coordinate
  ;; space) — :h 92 matches the y+=92 per-key row step. drawSingle draws no
  ;; skill-name text at all, so there is no :label node here (upstream
  ;; element set: back plate, key-cap + key character, icon-back, icon,
  ;; cooldown wipe — nothing else).
  (dsl/group {:id :slot :h 92 :w 320}
    (dsl/image {:id :slot-back :x 122 :y 0 :w 185 :h 83
                :src (modid/asset-path "textures" "guis/key_hint/back.png")})
    (dsl/image {:id :key-cap :x 146 :y 10 :w 70 :h 70
                :src (key-cap-texture "")})
    ;; Upstream: font.draw(name, wx=180, wy=27, option) — x is the centre
    ;; anchor (FontAlign.CENTER), y the em-box top.
    (dsl/text {:id :key-label :x 180 :y 27 :text "" :color key-label-color
               :align :center :font-size key-label-font-size})
    (dsl/image {:id :icon-back :x 216 :y 5 :w 72 :h 72
                :src (modid/asset-path "textures" "guis/key_hint/icon_back.png")})
    (dsl/image {:id :icon :x 221 :y 10 :w 62 :h 62 :src ""})
    ;; Charge/active glow border (ACRenderingHelper.drawGlow port, no center
    ;; line) — geometry + color/sine-pulse set per-frame in update fn.
    (dsl/glow-line {:id :icon-glow :x 221 :y 10 :visible? false})
    ;; Cooldown wipe: upstream colorRect(221, 10+62*(1-prog), 62, 62*prog).
    (dsl/box {:id :cd-mask :x 221 :y 10 :w 62 :h 62 :fill 0x4D999999 :visible? false})
    ;; Remaining-cooldown readout. Upstream draws no number at all (the wipe
    ;; above is the whole cooldown cue), so this is the port's own addition —
    ;; centred on the icon box the wipe darkens (221+62/2, vertically centred),
    ;; where it has the room to be legible. Sharing the key cap with the key
    ;; character left it only ~21 design units of headroom, i.e. under 5px.
    (dsl/text {:id :cd-text :x 252 :y 27 :text "" :color 0xFFFFFFFF
               :align :center :font-size 28 :visible? false})))

;; Upstream CPBar.drawPresetHint: 4 numbered boxes, 52×52 texture-space units
;; @ the bar's 0.2 scale ≈ 10.4×10.4px, step 62×0.2≈12.4px between boxes.
(def ^:private preset-box-size 10.4)
(def ^:private preset-box-step 12.4)
(def ^:private preset-font-size 9.2)
(def ^:private preset-text-y-offset 1.0)

(defn- preset-box-id [idx suffix]
  (keyword (str "preset-box-" idx "-" suffix)))

(defn- preset-box-template [idx]
  (dsl/group {:id (preset-box-id idx "grp") :x (* idx preset-box-step) :y 0
              :w preset-box-size :h preset-box-size}
    (dsl/box {:id (preset-box-id idx "back") :x 0 :y 0
              :w preset-box-size :h preset-box-size :fill 0x00303030})
    (dsl/text {:id (preset-box-id idx "digit") :x 0 :y preset-text-y-offset
               :w preset-box-size :h preset-font-size
               :align :center :font-size preset-font-size
               :text (str (inc idx)) :color 0x00FFFFFF})
    ;; Glow border (ACRenderingHelper.drawGlow), shown only on the active preset.
    (dsl/glow-line {:id (preset-box-id idx "glow") :x 0 :y 0 :visible? false})))

(defn- coin-dot-template []
  ;; Round soft glow sprite instead of a square box — 36 of them sweep a
  ;; smooth golden arc around the ring instead of reading as clock ticks.
  (dsl/image {:id :dot :w 6 :h 6
              :src (modid/asset-path "textures" "effects/glow_circle.png")
              :alpha 0.0}))

(defn- vm-wave-template []
  (dsl/image {:id :wave :w 16 :h 16 :src (modid/asset-path "textures" "effects/glow_circle.png") :alpha 0.0}))

(defn- debug-line-template []
  (dsl/text {:id :line :text "" :color 0xFFFFFFFF}))

(defn- build-overlay-spec [sw sh]
  (let [[cp-dx cp-dy] (gameplay/hud-position :cpbar)
        [key-dx key-dy] (gameplay/hud-position :keyhint)
        [notif-dx notif-dy] (gameplay/hud-position :notification)
        cp-layout (hud/cpbar-layout sw)
        frame (:frame cp-layout)
        cp-lane (:cp-lane cp-layout)
        normal-ol (:overload-preview cp-layout)
        active-ol (:overload-active cp-layout)
        category-icon (:category-icon cp-layout)
        cp-numbers (:cp-numbers cp-layout)
        ol-numbers (:ol-numbers cp-layout)
        activation-hint (:activation-hint cp-layout)
        preset-row (:preset-row cp-layout)
        ;; Full-frame textures use Minecraft's integer blit path. Round only
        ;; at this final raster boundary; source-space offsets above retain the
        ;; exact original 0.2-scaled coordinates.
        bar-x (double (Math/round (double (:x frame))))
        bar-y (double (Math/round (double (:y frame))))
        bar-w (double (Math/round (double (:w frame))))
        bar-h (double (Math/round (double (:h frame))))]
    (dsl/group {:id :root :w sw :h sh}
      ;; Vignette (upstream AC screen mask): edge glow tinted by category
      ;; color; alpha/tint driven per-frame by the smoothed bg color signal.
      (dsl/image {:id :bg-mask :x 0 :y 0 :w sw :h sh
                  :src (modid/asset-path "textures" "effects/screen_mask.png")
                  :alpha 0.0})
      (dsl/list-node {:id :vm-waves :w sw :h sh :template (vm-wave-template)})
      (dsl/group {:id :charging-layer :w sw :h sh :visible? false}
        (dsl/box {:id :charging-dim :x 0 :y 0 :w sw :h sh :fill 0x00000000})
        ;; Upstream CurrentChargingHUD "blue mask" (em_intensify_mask.png,
        ;; full-screen, alpha = mAlpha) — the release/blend cinematic.
        (dsl/image {:id :charging-mask :x 0 :y 0 :w sw :h sh
                    :src (modid/asset-path "textures" "effects/em_intensify_mask.png")
                    :alpha 0.0})
        ;; Upstream SubArcHandler2D.drawAll: flickering arc sprites around
        ;; screen center (ambient ring while charging, denser burst on release).
        (dsl/list-node {:id :charging-arcs :w sw :h sh :template (vm-wave-template)})
        (dsl/box {:id :charging-bar-bg :w 140 :h 8 :fill 0x96081230})
        (dsl/box {:id :charging-bar-fill :w 2 :h 8 :fill 0xC85AD2FF})
        (dsl/text {:id :charging-label :text "" :color 0xFFFFFFFF})
        (dsl/box {:id :charging-mark-v :w 4 :h 16 :fill 0xC878DCFF})
        (dsl/box {:id :charging-mark-h :w 16 :h 4 :fill 0xC878DCFF}))
      (dsl/group {:id :coin-qte-layer :w sw :h sh :visible? false}
        (dsl/box {:id :coin-qte-bg :w 48 :h 48 :fill 0x6414120A})
        (dsl/list-node {:id :coin-qte-dots :w 48 :h 48 :template (coin-dot-template)})
        (dsl/box {:id :coin-qte-marker :w 4 :h 4 :fill 0xF0FFDC50})
        (dsl/text {:id :coin-qte-pct :text "" :color 0xFFFFD700}))
      ;; ===== CP Bar Area (right-aligned, matching upstream CPBar layout) =====
      ;; Upstream CPBar.initEvents applies its interference jitter
      ;; (GL11.glTranslated inside CPBar's own FrameEvent) to ONLY this
      ;; widget's own draw — not the whole HUD. Wrapped in one group so
      ;; apply-jitter! can offset just this subtree (see update-overlay-signals!).
      (dsl/group {:id :cpbar-jitter-group :x cp-dx :y cp-dy :w sw :h sh}
        ;; Bar frame background (full bar, switches on overload)
        (dsl/image {:id :cpbar-bg :x bar-x :y bar-y :w bar-w :h bar-h
                    :src (modid/asset-path "textures" "guis/cpbar/back_normal.png")
                    :alpha 0.8})
        ;; Overload fill (behind CP fill, scroll-animated)
        ;; Overload bar: upstream draws two DIFFERENT visuals depending on state
        ;; (only one visible at a time, toggled per-frame in update-overload-lane!):
        ;; - not overloaded (drawNormal, the common case): mask.png tinted by a
        ;;   3-stop gradient, growing from the RIGHT edge, no scroll/animation —
        ;;   :overload-preview below. It samples mask.png rather than substituting
        ;;   a solid rectangle: the original mask has a long alpha feather on its
        ;;   left edge, which is visible at medium/high overload.
        ;; - overloaded (drawOverload): animated scrolling front_overload.png —
        ;;   :overload-bar, NO color-stops (its own texture already carries the
        ;;   cyan→green→pink gradient; tinting it would double-color it).
        ;;   Upstream drawNormal preview lives in the thin mid-band (X0=0,Y0=21,
        ;;   W=943,H=104 → ×0.2), while drawOverload fills the WHOLE frame
        ;;   (x0=30,y=0,W=914,H=147 → ×0.2 = +6,+0,183,29) — so the two lanes
        ;;   have DIFFERENT geometry, matching upstream.
        (dsl/progress {:id :overload-preview
                       :x (Math/round (double (:x normal-ol)))
                       :y (Math/round (double (:y normal-ol)))
                       :w (Math/round (double (:w normal-ol)))
                       :h (Math/round (double (:h normal-ol)))
                       :fg-src (modid/asset-path "textures" "guis/cpbar/mask.png")
                       :color-stops overload-preview-color-stops
                       :uv-region [0.0 (/ 21.0 147.0) (/ 943.0 964.0) (/ 125.0 147.0)]
                       :anchor :right :visible? false})
        ;; Forge uses the original two-sampler mask shader. Other loaders keep
        ;; the same full-width overload layer through renderer's texture fallback.
        (dsl/shader-progress
          {:id :overload-bar
           :x (Math/round (double (:x active-ol)))
           :y (Math/round (double (:y active-ol)))
           :w (Math/round (double (:w active-ol)))
           :h (Math/round (double (:h active-ol)))
           :shader-props
           {:shader-id :cpbar-overload
            :texture-0 (modid/asset-path "textures" "guis/cpbar/front_overload.png")
            :texture-1 (modid/asset-path "textures" "guis/cpbar/mask.png")
            :uv-region [(/ 30.0 974.0) 0.0 (/ 944.0 974.0) 1.0]
            :highlight-alpha 0.0}
           :visible? false})
        ;; CP fill (diagonal cut + icon overlay). Consumption-hint "release" cue
        ;; (upstream CPBar: mAlpha*=0.2+0.1*(1+sin(t/80)) ghost of the CURRENT
        ;; level drawn first, then the PREDICTED post-cost level drawn solid on
        ;; top) — :cp-bar-ghost is the pulsing current-level bar (hidden unless a
        ;; skill's CP cost is being previewed), :cp-bar is the solid bar showing
        ;; either the predicted level (hint active) or the plain current level.
        (dsl/progress {:id :cp-bar-ghost
                       :x (Math/round (double (:x cp-lane)))
                       :y (Math/round (double (:y cp-lane)))
                       :w (Math/round (double (:w cp-lane)))
                       :h (Math/round (double (:h cp-lane)))
                       :corner 0.852
                       :fg-src (modid/asset-path "textures" "guis/cpbar/cp.png")
                       :color-stops cp-color-stops
                       :uv-region [(/ 47.0 964.0) (/ 30.0 147.0)
                                   (/ 930.0 964.0) (/ 114.0 147.0)]
                       ;; upstream drawCPBar: prog = 0.16 + prog*0.8 (min sliver, max 96%)
                       :fill-remap [0.16 0.8]
                       :anchor :right    ;; upstream fixes the right (icon-side) edge
                       :visible? false})
        (dsl/progress {:id :cp-bar
                       :x (Math/round (double (:x cp-lane)))
                       :y (Math/round (double (:y cp-lane)))
                       :w (Math/round (double (:w cp-lane)))
                       :h (Math/round (double (:h cp-lane)))
                       :corner 0.852    ;; 103*sin(44°)/84 — diagonal on left edge (matching upstream OFF/HEIGHT)
                       :fg-src (modid/asset-path "textures" "guis/cpbar/cp.png")
                       :color-stops cp-color-stops
                       :uv-region [(/ 47.0 964.0) (/ 30.0 147.0)
                                   (/ 930.0 964.0) (/ 114.0 147.0)]
                       :fill-remap [0.16 0.8]    ;; upstream drawCPBar prog = 0.16 + prog*0.8
                       :anchor :right            ;; upstream fixes the right (icon-side) edge
                       :icon-src ""     ;; set per-frame to category icon
                       :icon-cutout
                       {:x-offset (Math/round (double (:x-offset category-icon)))
                        :y-offset (Math/round (double (:y-offset category-icon)))
                        :w (Math/round (double (:w category-icon)))
                        :h (Math/round (double (:h category-icon)))}})
        ;; Overload highlight (pulsing overlay when overloaded)
        (dsl/image {:id :overload-highlight :x bar-x :y bar-y :w bar-w :h bar-h
                    :src (modid/asset-path "textures" "guis/cpbar/highlight_overload.png")
                    :visible? false :alpha 0.0})
        ;; ===== CP/OL Numbers (within bar area) =====
        (dsl/text {:id :cp-numbers
                   :x (Math/round (double (:x cp-numbers)))
                   :y (Math/round (double (:y cp-numbers)))
                   :font-size (:font-size cp-numbers)
                   :text "" :color 0xFFFFFFFF :visible? false})
        (dsl/text {:id :ol-numbers
                   :x (Math/round (double (:x ol-numbers)))
                   :y (Math/round (double (:y ol-numbers)))
                   :font-size (:font-size ol-numbers)
                   :text "" :color 0xFFFFFFFF :visible? false})
        ;; ===== Activation hint (within bar area, with background box) =====
        (dsl/group {:id :activation-hint-group :x bar-x :y bar-y
                    :w bar-w :h bar-h :visible? false}
          (dsl/box  {:id :activation-hint-bg :x 0 :y 0 :w 0 :h 0 :fill 0x46414141})
          ;; Glow border (ACRenderingHelper.drawGlow, upstream CRL_KH_GLOW =
          ;; white @ alpha 40/255). Its rectangle follows the measured hint
          ;; width, while visibility follows the parent group.
          (dsl/glow-line {:id :activation-hint-glow :x 0 :y 0})
          (dsl/text {:id :activation-hint
                     :x 0
                     :y (- (double (:text-y activation-hint)) bar-y)
                     :w (- (double (:text-right-x activation-hint)) bar-x)
                     :h (:font-size activation-hint)
                     :align :right :font-size (:font-size activation-hint)
                     :text "" :color 0xA0FFFFFF}))
        ;; ===== Preset indicators — upstream drawPresetHint is also a CPBar
        ;; method, called from the same jittered FrameEvent block. =====
        (dsl/group {:id :preset-row
                    :x (Math/round (double (:x preset-row)))
                    :y (Math/round (double (:y preset-row)))
                    :w (+ preset-box-size (* 3.0 preset-box-step)) :h preset-box-size
                    :visible? false}
          (preset-box-template 0) (preset-box-template 1)
          (preset-box-template 2) (preset-box-template 3)))
      ;; ===== Skill Slots (upstream KeyHintUI: independent widget, vertically
      ;; centred on the right screen edge — see skill-slot-anchor) =====
      (let [[ax ay] (skill-slot-anchor sw sh)]
        (dsl/list-node {:id :skill-slots :spacing 0 :w 320 :h 400 :scale skill-slot-scale
                        :x (+ ax key-dx) :y (+ ay key-dy)
                        :template (skill-slot-template)}))
      ;; StormWing's movement-key hint column — the KEY_GROUP delegates at
      ;; x = -200 - availIdx*200 widget units (upstream KeyHintUI), i.e. one
      ;; full key-hint column (~46px) to the LEFT of the skill slots, each
      ;; row 92 units apart using the same drawSingle layout.
      (let [[ax ay] (skill-slot-anchor sw sh)]
        (dsl/list-node {:id :movement-hints :spacing (* 92.0 skill-slot-scale)
                        :w 320 :h 400 :scale skill-slot-scale
                        :x (- (+ ax key-dx) (* 200.0 skill-slot-scale))
                        :y (+ ay key-dy)
                        :template (skill-slot-template) :visible? false}))
      (dsl/crosshair {:id :crosshair :x (int (/ sw 2)) :y (int (/ sh 2)) :visible? false})
      (dsl/list-node {:id :toasts :w sw :h 200 :template (toast-template)})
      (dsl/group {:id :tutorial-notif :x notif-dx :y notif-dy
                  :w sw :h 200 :visible? false}
        (dsl/image {:id :tut-bg :x 0 :y 15 :w 129 :h 43 :src "" :alpha 0.0})
        (dsl/image {:id :tut-icon :w 83 :h 83 :src "" :alpha 0.0})
        (dsl/text {:id :tut-title :text "" :color 0xFFFFFFFF})
        (dsl/text {:id :tut-content :text "" :color 0xFFFFFFFF}))
      (dsl/list-node {:id :debug-lines :w 300 :h 200 :template (debug-line-template)})
      (dsl/group {:id :overlay-app-layer :w sw :h sh :visible? false}
        (dsl/box {:id :overlay-app-panel :fill 0xC0202020})
        (dsl/text {:id :overlay-app-title :text "" :color 0xFFFFFFFF})
        (dsl/text {:id :overlay-app-subtitle :text "" :color 0xFF888888 :visible? false}))
      ;; Full-screen skill flash (e.g. jet-engine's trigger pulse) — last
      ;; child so it draws on top of everything else; transparent (alpha 0
      ;; fill) until update-skill-flash! drives it.
      (dsl/box {:id :skill-flash-screen :x 0 :y 0 :w sw :h sh :fill 0x00000000}))))

(defn- attach-overlay-bindings! [r]
  (let [clock (rt/clock-ms-sig r)
        bg-target (sig/signal-o [0.0 0.0 0.0 0.0])
        cp-target (sig/signal-d 0.0)
        cp-predicted-target (sig/signal-d 0.0)
        cp-hint-active (sig/signal-d 0.0)
        cp-low-mult (sig/signal-d 1.0)
        ol-target (sig/signal-d 0.0)
        ol-scroll (sig/signal-d 0.0)
        bg-smooth (anim/smoothed-color bg-target clock)
        cp-smooth (anim/smoothed cp-target clock 2.0)
        cp-predicted-smooth (anim/smoothed cp-predicted-target clock 2.0)
        ol-smooth (anim/smoothed ol-target clock 2.0)
        ;; CPBar bypasses bufferedCP while a consumption provider is alive:
        ;; both live and predicted layers snap to their current values.
        cp-display (sig/computed-d
                     [cp-hint-active cp-target cp-smooth]
                     (fn [hint-active target smooth]
                       (double (if (pos? (double hint-active)) target smooth))))
        cp-predicted-display (sig/computed-d
                               [cp-hint-active cp-predicted-target cp-predicted-smooth]
                               (fn [hint-active target smooth]
                                 (double (if (pos? (double hint-active)) target smooth))))
        ;; Upstream CPBar.drawOverload highlight: color4d(1,1,1, 0.3+0.35*(sin(time/200)+1))
        ;; → alpha in [0.3,1.0], period = 2π*200ms ≈ 1257ms (anim/breathe takes a
        ;; full-cycle period in ms, so this matches the upstream pulse speed).
        hl-alpha  (anim/breathe clock 1256.6 0.3 1.0)
        ;; Upstream consumption-hint ghost: mAlpha *= 0.2+0.1*(1+sin(t/80)) →
        ;; alpha in [0.2,0.4], period = 2π*80ms ≈ 502.65ms.
        cp-ghost-alpha (anim/breathe clock 502.65 0.2 0.4)
        cp-ghost-display-alpha
        (sig/computed-d [cp-ghost-alpha cp-low-mult]
                        (fn [pulse low-mult]
                          (* (double pulse) (double low-mult))))
        ;; Upstream CPBar interference jitter: 60 random keyframes (80-400ms,
        ;; direction (sinθ·n³·9·aspect, cosθ·n³·9)) built once; per frame the
        ;; offset holds the NEXT keyframe's direction, and the master alpha
        ;; flickers along a Catmull-Rom curve over the keyframe times.
        interf-frame (:frame (hud/cpbar-layout (rt/screen-w r)))
        interf-frames (anim/build-interference-keyframes
                        (/ (double (:w interf-frame)) (double (:h interf-frame))))
        interf-alpha-points (anim/build-interference-alpha-points interf-frames)
        jitter-x (sig/computed-d [clock]
                   (fn [now-ms] (double (first (anim/interference-offset (double now-ms) interf-frames)))))
        jitter-y (sig/computed-d [clock]
                   (fn [now-ms] (double (second (anim/interference-offset (double now-ms) interf-frames)))))
        ;; Plain signal — updated per frame by apply-jitter! (before the
        ;; binding flush), 1.0 when not interfered so the bound alphas are
        ;; unaffected outside interference.
        jitter-alpha (sig/signal-d 1.0)
        ^INode bg-mask (ui/node r :bg-mask)]
    ;; init-node-props! forces image alpha 0.0 → 1.0 (visibility default);
    ;; the vignette must START invisible — zero it here, the color binding
    ;; owns it from the first flush on.
    (.setDSlot bg-mask 0 0.0)
    (rt/put-user-signal! r :bg-target bg-target)
    (rt/put-user-signal! r :cp-target cp-target)
    (rt/put-user-signal! r :cp-predicted-target cp-predicted-target)
    (rt/put-user-signal! r :cp-hint-active cp-hint-active)
    (rt/put-user-signal! r :cp-low-mult cp-low-mult)
    (rt/put-user-signal! r :ol-target ol-target)
    (rt/put-user-signal! r :ol-scroll ol-scroll)
    (rt/put-user-signal! r :hl-alpha hl-alpha)
    (rt/put-user-signal! r :jitter-x jitter-x)
    (rt/put-user-signal! r :jitter-y jitter-y)
    (rt/put-user-signal! r :jitter-alpha jitter-alpha)
    ;; user-signal must hold a SIGNAL (callers deref it); a raw map breaks @.
    (rt/put-user-signal! r :interf-jitter-data
      (sig/signal-o {:frames interf-frames
                     :alpha-points interf-alpha-points}))
    ;; counts[0]=vm-waves [1]=toasts [2]=debug-lines [3]=coin-qte-dots [4]=charging-arcs
    (let [counts (int-array [-1 -1 -1 -1 -1])]
      (rt/put-user-signal! r :overlay-object-cache (object-array [[] ""]))
      (rt/put-user-signal! r :overlay-count-cache counts)
      ;; [0]=overloaded bg switch  [1]=overload highlight visible
      (rt/put-user-signal! r :overlay-flag-cache (boolean-array 2)))
    (let [b (sig/bind! bg-smooth bg-mask write-vignette-from-rgba-o! (rt/get-dirty-bindings-q r))]
      (rt/register-binding! r (.getIdx bg-mask) b))
    ;; CP bar: solid bar shows predicted-after-cost level when a consumption
    ;; hint is active, else the plain current level (see update-cp-lane!).
    ;; Ghost bar always tracks the plain current level, pulsing, shown only
    ;; while a hint is active.
    (ui/bind! r :cp-bar :progress cp-predicted-display)
    ;; Bar/ghost alphas carry the interference master-alpha flicker (upstream
    ;; mAlpha *= alphaCurve.valueAt — the curve value lands in jitter-alpha
    ;; before the flush, so the multiplied binding renders the flicker).
    (ui/bind! r :cp-bar :alpha
      (sig/computed-d [cp-low-mult jitter-alpha]
        (fn [m a] (* (double m) (double a)))))
    (ui/bind! r :cp-bar-ghost :progress cp-display)
    (ui/bind! r :cp-bar-ghost :alpha
      (sig/computed-d [cp-ghost-display-alpha jitter-alpha]
        (fn [g a] (* (double g) (double a)))))
    ;; Normal preview balances at 2 units/second. The overloaded shader always
    ;; draws its complete layer and uses :progress as texture-scroll phase.
    (ui/bind! r :overload-preview :progress ol-smooth)
    (ui/bind! r :overload-bar :progress ol-scroll)
    ;; Overload highlight: breathing alpha
    (ui/bind! r :overload-highlight :alpha hl-alpha)
    r))

(defn build-overlay-runtime
  [sw sh]
  (let [r (rt/create-runtime)]
    (rt/build! r (build-overlay-spec sw sh))
    (attach-overlay-bindings! r)))

(defn- overlay-input-state [player-uuid now-ms]
  (let [owner {:player-uuid player-uuid}]
    {:activated-override (bridge/client-overlay-activated-override owner)
     :showing-numbers? (aget ^booleans mode-switch-flags 1)
     :last-show-value-change-ms (aget ^longs mode-switch-time 0)
     :active-overlay-app (bridge/client-active-overlay-app owner)
     :now-ms now-ms}))

(defn- set-node-visible! [r ^INode n visible?]
  (when (and n (not= (boolean visible?) (.isVisible n)))
    (.setVisible n (boolean visible?))
    (.setFlag n node/FLAG-RENDER-DIRTY)
    ;; Visibility participates in tape flattening (layout/flatten-into! skips
    ;; invisible subtrees at REBUILD time), so a toggle must mark the tree
    ;; dirty or the change never reaches the render tape — the node keeps
    ;; drawing (or stays absent) forever. Guarded above: rebuild cost is paid
    ;; only on actual transition frames.
    (rt/mark-tree-dirty! r)))

(defn- set-visible! [r id visible?]
  (set-node-visible! r (ui/node r id) visible?))


(defn- update-overload-lane! [r snapshot _sw]
  ;; Upstream drawNormal vs drawOverload: only one of these two visuals is
  ;; ever on screen. Not overloaded (the common case) → :overload-preview,
  ;; the feathered mask texture tinted by the 3-stop overrideColors gradient
  ;; growing from the right edge. Overloaded → :overload-bar, the scrolling
  ;; front_overload.png texture (already bound to ol-smooth/ol-scroll).
  (let [activated? (boolean (:activated? snapshot))
        ob (:overload-bar snapshot)
        overloaded? (boolean (:overloaded ob))
        ol-pct (double (Math/max 0.0 (Math/min 1.0 (double (or (:percent ob) 0.0)))))]
    (set-visible! r :overload-bar (and activated? overloaded?))
    (set-visible! r :overload-preview (and activated? (not overloaded?)))
    (when (and activated? (not overloaded?))
      (let [[a _rr _gg _bb] (sample-argb-stops overload-preview-stops ol-pct)]
        (ui/set-prop! r :overload-preview :alpha (/ (double a) 255.0))))))

(defn- update-activation-indicator! [r snapshot sw]
  (let [ind       (:activation-indicator snapshot)
        activated (:activated snapshot)
        hint      (:hint ind)]
    ;; Show/hide the bar-area activation hint group
    (set-visible! r :cp-bar (:activated? snapshot))
    (set-visible! r :cpbar-bg (:activated? snapshot))
    (set-visible! r :activation-hint-group (boolean (and activated hint)))
    (when hint
      (let [layout (hud/cpbar-layout sw)
            text (str hint)
            font-size (get-in layout [:activation-hint :font-size])
            text-width (double
                         (or (bridge/font-text-width-optional {} text font-size)
                             (bridge/font-width-optional text)
                             (* 0.5 font-size (count text))))
            box (hud/cpbar-activation-hint-box sw text-width)
            frame (:frame layout)
            group-x (double (Math/round (double (:x frame))))
            group-y (double (Math/round (double (:y frame))))
            local-x (- (double (:x box)) group-x)
            local-y (- (double (:y box)) group-y)
            box-w (double (:w box))
            box-h (double (:h box))]
        (ui/set-prop! r :activation-hint :text text)
        (set-box-at! r :activation-hint-bg local-x local-y box-w box-h
                     {:r 65 :g 65 :b 65 :a 70})
        (when-let [^INode glow (ui/node r :activation-hint-glow)]
          (let [center-y (/ box-h 2.0)
                glow-size (double (:glow-size box))]
            (when (or (not= local-x (.getX glow))
                      (not= local-y (.getY glow))
                      (not= box-w (.getDSlot glow 1))
                      (not= center-y (.getDSlot glow 2))
                      (not= box-h (.getDSlot glow 3))
                      (not= glow-size (.getDSlot glow 4)))
              (.setX glow local-x)
              (.setY glow local-y)
              (.setDSlot glow 0 0.0)
              (.setDSlot glow 1 box-w)
              (.setDSlot glow 2 center-y)
              (.setDSlot glow 3 box-h)
              (.setDSlot glow 4 glow-size)
              (.setDSlot glow 5 (double (unchecked-int 0x28FFFFFF)))
              (.setDSlot glow 6 1.0)
              (.setFlag glow node/FLAG-LAYOUT-DIRTY)
              (.setFlag glow node/FLAG-RENDER-DIRTY))))))))

(defn- update-numbers! [r snapshot]
  (let [texts (:numbers-texts snapshot)]
    (if (seq texts)
      (doseq [[id idx] [[:cp-numbers 0] [:ol-numbers 1]]]
        (when-let [t (nth texts idx nil)]
          (set-visible! r id true)
          (ui/set-prop! r id :text (str (:text t)))
          (when-let [c (:color t)]
            (ui/set-prop! r id :color
                          (if (map? c)
                            (rgb-vec->argb [(:r c) (:g c) (:b c)] (/ (double (:a c)) 255.0))
                            c)))))
      (do
        (set-visible! r :cp-numbers false)
        (set-visible! r :ol-numbers false)))))

(defn- update-preset-box-glow! [r ^INode glow visible? intensity]
  (when glow
    (if visible?
      (do
        (set-node-visible! r glow true)
        (.setDSlot glow 0 0.0) (.setDSlot glow 1 preset-box-size)
        (.setDSlot glow 2 (/ preset-box-size 2.0)) (.setDSlot glow 3 preset-box-size)
        (.setDSlot glow 4 1.0)
        (.setDSlot glow 5 (double (unchecked-int
                                   (bit-or (bit-shift-left (int (* 200.0 intensity)) 24)
                                           0x00FFFFFF))))
        (.setDSlot glow 6 1.0)
        (.setFlag glow node/FLAG-RENDER-DIRTY))
      (set-node-visible! r glow false))))

(defn- update-preset-indicators! [r snapshot sw]
  ;; Upstream CPBar.drawPresetHint: 4 static numbered boxes, back rgb(48,48,48)
  ;; @ alpha*0.75, text white @ max(0.05, alpha*0.8), glow border on whichever
  ;; index is currently active. Whole row shown for ~2s after a preset switch.
  (let [indicators (:preset-indicators snapshot)
        curr (last indicators)
        visible? (boolean curr)]
    (when-let [^INode row (ui/node r :preset-row)]
      (let [{:keys [x y]} (:preset-row (hud/cpbar-layout sw))]
        (let [x (double (Math/round (double x)))
              y (double (Math/round (double y)))]
          (when (or (not= x (.getX row)) (not= y (.getY row)))
            (.setX row x)
            (.setY row y)
            (.setFlag row node/FLAG-LAYOUT-DIRTY)))))
    (set-node-visible! r (ui/node r :preset-row) visible?)
    (when curr
      (let [fade (double (or (:fade curr) 1.0))
            back-alpha (int (* 255.0 0.75 fade))
            text-alpha (int (* 255.0 (Math/max 0.05 (* 0.8 fade))))
            active-idx (:current curr)]
        (dotimes [i 4]
          (ui/set-node-prop! r (ui/node r (preset-box-id i "back")) :fill
                             (bit-or (bit-shift-left back-alpha 24) 0x00303030))
          (ui/set-node-prop! r (ui/node r (preset-box-id i "digit")) :color
                             (bit-or (bit-shift-left text-alpha 24) 0x00FFFFFF))
          (update-preset-box-glow! r (ui/node r (preset-box-id i "glow"))
                                   (= i active-idx) fade))))))

(defn- format-tenths-seconds
  "Format non-negative seconds as \"N.Ts\" (one decimal, rounded). Runs every
  frame for every skill slot on cooldown — avoids java.util.Formatter's
  locale-aware parsing (`(format \"%.1f\" ...)`) on that hot path."
  ^String [seconds]
  (let [tenths (long (Math/round (* (double seconds) 10.0)))
        whole (quot tenths 10)
        frac (mod tenths 10)]
    (str whole "." frac "s")))

(defn- sin-alpha
  "Upstream KeyHintUI: sinAlpha = 0.6 + (1 + sin((time%100)/50)) * 0.2 — a fast
   ~100ms shimmer applied to charge/active icon alpha and glow alpha."
  ^double [^double now-ms]
  (+ 0.6 (* 0.2 (+ 1.0 (Math/sin (/ (mod now-ms 100.0) 50.0))))))

(defn- update-skill-slot-glow! [r ^INode icon-glow visual glow this-sin-alpha]
  (when icon-glow
    (if (and glow (not= visual :idle))
      (let [icon-w (* 62.0 skill-slot-scale)   ;; upstream ICON_SIZE=62
            icon-h (* 62.0 skill-slot-scale)
            ;; upstream size=5 / ICON_SIZE=62 — the 5px edge glow reads as a
            ;; faint outline; 10px makes the :active state actually visible on
            ;; the skill bar (port enhancement; upstream has no active state).
            glow-sz (Math/max 1.0 (* icon-w (/ 10.0 62.0)))
            [gr gg gb ga] glow
            tint-alpha (int (* (double (or ga 255)) this-sin-alpha))
            argb (unchecked-int (bit-or (bit-shift-left tint-alpha 24)
                                         (bit-shift-left (int gr) 16)
                                         (bit-shift-left (int gg) 8)
                                         (int gb)))]
        (set-node-visible! r icon-glow true)
        (.setDSlot icon-glow 0 0.0)              ;; x0
        (.setDSlot icon-glow 1 icon-w)           ;; x1
        (.setDSlot icon-glow 2 (/ icon-h 2.0))   ;; y (box vertical center)
        (.setDSlot icon-glow 3 icon-h)           ;; line-w (= box height)
        (.setDSlot icon-glow 4 glow-sz)
        (.setDSlot icon-glow 5 (double argb))
        (.setDSlot icon-glow 6 1.0)              ;; no-center: drawGlow has no bright core line
        (.setFlag icon-glow node/FLAG-RENDER-DIRTY))
      (set-node-visible! r icon-glow false))))

(defn- update-skill-slot-item! [r item slot now-ms cant-use-ability?]
  (let [visual (:visual-state slot :idle)
        state-alpha (double (or (:alpha slot) 1.0))
        glow (:glow-color slot)
        sin-effect? (boolean (:sin-effect? slot))
        this-sin-alpha (if sin-effect? (sin-alpha (double now-ms)) 1.0)
        in-cd? (boolean (:in-cooldown slot))
        ;; Upstream KeyHintUI.drawSingle: alpha = 0.4 flat while on cooldown,
        ;; else state.alpha * (0.4 + sinAlpha*0.6). The sine trough dips to
        ;; 0.4 — DARKER than the :idle 0.7 — so an :active/:charge slot reads
        ;; dimmer than idle half the time and the activation is invisible on
        ;; the bar. Lift the floor to 0.85: pulsing states stay clearly
        ;; brighter than idle through the whole cycle, pulse shape unchanged.
        icon-alpha (if in-cd? 0.4 (* state-alpha (+ 0.85 (* this-sin-alpha 0.15))))
        key-label-raw (:key-label slot)
        ;; Upstream KeyHintUI.drawSingle draws no key character for
        ;; MOUSE_LEFT/MOUSE_RIGHT (the mouse icon texture stands alone).
        key-label (if (keyword? key-label-raw) "" (str key-label-raw))
        icon (ui/item-node item :icon)
        icon-glow (ui/item-node item :icon-glow)
        key-cap (ui/item-node item :key-cap)
        key-label-node (ui/item-node item :key-label)
        cd-mask (ui/item-node item :cd-mask)
        cd-text (ui/item-node item :cd-text)
        ;; Upstream: !canUseAbility || tickLeft>0 → color4d(0.7,0.7,0.7,1) dims
        ;; the key-cap plate + key character only (icon dimming is handled
        ;; separately above via icon-alpha). canUseAbility = activated &&
        ;; overloadFine && !interfering; activated is already implied (slots
        ;; are only shown while activated), so cant-use-ability? here is
        ;; overloaded? || interfered? — passed down from update-skill-slots!,
        ;; a per-player condition identical for every slot this frame.
        dim? (or in-cd? cant-use-ability?)]
    (ui/set-node-prop! r key-cap :src (key-cap-texture key-label-raw))
    (ui/set-node-prop! r key-label-node :text key-label)
    (ui/set-node-prop! r key-cap :tint (if dim? [178 178 178 255] [255 255 255 255]))
    (ui/set-node-prop! r key-label-node :color (if dim? key-label-color-dim key-label-color))
    (when-let [icon-src (:skill-icon slot)]
      (ui/set-node-prop! r icon :src icon-src))
    (ui/set-node-prop! r icon :alpha icon-alpha)
    ;; Upstream KeyHintUI.drawSingle: ShaderMono stays active through the icon
    ;; draw when !canUseAbility || on cooldown — the icon renders monochrome.
    ;; No mono shader in the reactive renderer; the 0.7 gray tint is the
    ;; equivalent desaturating wash.
    (ui/set-node-prop! r icon :tint (if dim? [178 178 178 255] [255 255 255 255]))
    (update-skill-slot-glow! r icon-glow visual glow this-sin-alpha)
    ;; Cooldown wipe: proportional bottom-up gray overlay on the icon box,
    ;; matching upstream colorRect(iconX, iconY+ICON_SIZE*(1-prog), ICON_SIZE, ICON_SIZE*prog).
    (set-node-visible! r cd-mask in-cd?)
    (when in-cd?
      (let [total (double (Math/max 1.0 (double (or (:cooldown-total slot) 1))))
            remaining (double (or (:cooldown-remaining slot) 0))
            prog (Math/max 0.0 (Math/min 1.0 (/ remaining total)))]
        (set-box-node-at! r cd-mask 221 (+ 10.0 (* 62.0 (- 1.0 prog))) 62 (* 62.0 prog)
                          {:r 153 :g 153 :b 153 :a 76})))
    (when cd-text
      (if in-cd?
        (do (set-node-visible! r cd-text true)
            (ui/set-node-prop! r cd-text :text (format-tenths-seconds (:cooldown-seconds slot 0.0))))
        (set-node-visible! r cd-text false)))))

(defn- same-skill-ids? [cached slots]
  (let [n (count slots)]
    (and (= n (count cached))
         (loop [i 0]
           (or (= i n)
               (and (= (nth cached i) (:skill-id (nth slots i)))
                    (recur (unchecked-inc-int i))))))))

;; ============================================================================
;; Movement-key hint column (upstream StormWing KEY_GROUP / KeyHintUI)
;; ============================================================================

(defn- update-movement-hint-item! [r item hint]
  (let [{:keys [key-label active? skill-icon]} hint
        key-cap (ui/item-node item :key-cap)
        key-label-node (ui/item-node item :key-label)
        icon (ui/item-node item :icon)
        icon-glow (ui/item-node item :icon-glow)]
    (ui/set-node-prop! r key-cap :src (key-cap-texture key-label))
    (ui/set-node-prop! r key-label-node :text (str key-label))
    (ui/set-node-prop! r key-cap :tint [255 255 255 255])
    (ui/set-node-prop! r key-label-node :color key-label-color)
    (when-let [icon-src skill-icon]
      (ui/set-node-prop! r icon :src icon-src))
    (ui/set-node-prop! r icon :alpha (if active? 1.0 0.55))
    ;; Upstream delegate ACTIVE state: blue glow around the held key's icon.
    (update-skill-slot-glow! r icon-glow (if active? :active :idle)
                             (when active? [70 179 255 255]) 1.0)))

(defn- update-movement-hints! [r snapshot sw sh]
  (let [hints (:movement-hints snapshot)
        [key-dx key-dy] (gameplay/hud-position :keyhint)]
    ;; Re-anchor every frame: one key-hint column (-200 widget units) LEFT of
    ;; the skill slots, matching the skill-slot anchor (tracks resize).
    (when-let [^INode list-node (ui/node r :movement-hints)]
      (let [[ax ay] (skill-slot-anchor sw sh)
            x (- (+ ax key-dx) (* 200.0 skill-slot-scale))
            y (+ ay key-dy)]
        (when (or (not= x (.getX list-node)) (not= y (.getY list-node)))
          (.setX list-node x)
          (.setY list-node y)
          (.setFlag list-node node/FLAG-LAYOUT-DIRTY))))
    (ui/list-set! r :movement-hints (or (:items hints) [])
                  update-movement-hint-item!)
    (set-visible! r :movement-hints (boolean (seq (:items hints))))))

(defn- update-skill-slots! [r snapshot now-ms sw sh]
  (let [slots (:skill-slots snapshot)
        ^objects cache (rt/user-signal r :overlay-object-cache)
        cached-ids (aget cache 0)
        ;; Upstream CPData.canUseAbility() = activated && overloadFine &&
        ;; !interfering; activated is already implied here (slots are only
        ;; populated while activated), so the remaining condition is just
        ;; overloaded? || interfered? — one player-wide value for the frame,
        ;; not per-slot.
        cant-use-ability? (boolean (or (:recovering (:overload-bar snapshot))
                                       (:interfered? snapshot)))]
    ;; Re-anchor every frame: upstream KeyHintUI is vertically centred on the
    ;; right screen edge (halign CENTER), so its position tracks window resize.
    (when-let [^INode list-node (ui/node r :skill-slots)]
      (let [[ax ay] (skill-slot-anchor sw sh)]
        (when (or (not= ax (.getX list-node)) (not= ay (.getY list-node)))
          (.setX list-node (double ax))
          (.setY list-node (double ay))
          (.setFlag list-node node/FLAG-LAYOUT-DIRTY))))
    (when-not (same-skill-ids? cached-ids slots)
      (aset cache 0 (mapv #(get % :skill-id) slots))
      (ui/list-set! r :skill-slots slots
                    (fn [rt item slot-data]
                      (update-skill-slot-item! rt item slot-data now-ms cant-use-ability?))))
    (when-let [^INode list-node (ui/node r :skill-slots)]
      (let [n (.getChildCount list-node)]
        (dotimes [i n]
          (when-let [slot (nth slots i nil)]
            (when-let [^INode item (.getChild list-node i)]
              (update-skill-slot-item! r item slot now-ms cant-use-ability?)))))))
    (set-visible! r :skill-slots (seq (:skill-slots snapshot))))

(defn- update-crosshair! [r snapshot]
  (if-let [ch (:crosshair snapshot)]
    (do
      (set-visible! r :crosshair true)
      (when-let [^INode n (ui/node r :crosshair)]
        (.setDSlot n 0 (double (:phase ch)))
        (.setDSlot n 1 (double (:intensity ch)))
        (.setFlag n node/FLAG-RENDER-DIRTY)))
    (set-visible! r :crosshair false)))

(defn- update-toast-item! [r item toast]
  (let [^INode grp item
        {:keys [x y w h bg borders text]} toast]
    (.setX grp (double x))
    (.setY grp (double y))
    (.setW grp (double w))
    (.setH grp (double h))
    (set-box-node-at! r (ui/item-node item :bg) 0 0 w h bg)
    (doseq [[bid border] [[:border-t (first borders)]
                         [:border-b (second borders)]
                         [:border-l (nth borders 2 nil)]
                         [:border-r (nth borders 3 nil)]]]
      (when border
        (set-box-node-at! r (ui/item-node item bid)
                          (- (:x border) x) (- (:y border) y)
                          (:w border) (:h border)
                          {:r 255 :g 255 :b 255 :a (:a border)})))
    (when-let [^INode msg (ui/item-node item :msg)]
      (.setX msg (double (- (:x text) x)))
      (.setY msg (double (- (:y text) y)))
      (ui/set-node-prop! r msg :text (str (:text text)))
      (ui/set-node-prop! r msg :color (rgba-map->argb (:color text))))))

(defn- update-toasts! [r snapshot]
  (let [toasts (:toasts snapshot)
        ^ints counts (rt/user-signal r :overlay-count-cache)
        n (count toasts)]
    (when (not= (aget counts 1) n)
      (aset-int counts 1 n)
      (ui/list-set! r :toasts toasts
                    (fn [rt item data] (update-toast-item! rt item data))))
    (when-let [^INode list-node (ui/node r :toasts)]
      (let [n (.getChildCount list-node)]
        (dotimes [i n]
          (when-let [toast (nth toasts i nil)]
            (when-let [^INode item (.getChild list-node i)]
              (update-toast-item! r item toast))))))))

(defn- update-vm-wave-item! [r item wave]
  (let [^INode img (ui/item-node item :wave)]
    (.setX img (double (:x wave)))
    (.setY img (double (:y wave)))
    (.setW img (double (:w wave)))
    (.setH img (double (:h wave)))
    (ui/set-node-prop! r img :src (:src wave))
    (ui/set-node-prop! r img :tint (:tint wave))
    (.setDSlot img 0 (double (:alpha wave)))
    (.setFlag img node/FLAG-RENDER-DIRTY)))

(defn- update-vm-waves! [r snapshot]
  (let [waves (or (:vm-waves snapshot) [])
        ^ints counts (rt/user-signal r :overlay-count-cache)
        n (count waves)]
    (when (not= (aget counts 0) n)
      (aset-int counts 0 n)
      (ui/list-set! r :vm-waves waves
                    (fn [rt item data] (update-vm-wave-item! rt item data))))
    (when-let [^INode list-node (ui/node r :vm-waves)]
      (let [n (.getChildCount list-node)]
        (dotimes [i n]
          (when-let [wave (nth waves i nil)]
            (when-let [^INode item (.getChild list-node i)]
              (update-vm-wave-item! r item wave))))))))

(defn- update-charging-layer! [r snapshot sw sh]
  (if-let [ch (:charging snapshot)]
    (let [{:keys [dim-a mask-alpha]} ch]
      (set-visible! r :charging-layer true)
      ;; Upstream CurrentChargingHUD Black Mask: glColor4d(0,0,0, 0.1*mAlpha).
      (set-box-rgba! r :charging-dim {:r 0 :g 0 :b 0 :a dim-a})
      (ui/set-prop! r :charging-mask :alpha (double (or mask-alpha 0.0)))
      ;; AcademyCraft's CurrentChargingHUD belongs to BodyIntensify and only
      ;; renders the dim/mask plus arc sprites. The progress bar, label and
      ;; crosshair were port-only additions and must stay hidden.
      (doseq [node-id [:charging-bar-bg :charging-bar-fill :charging-label
                       :charging-mark-v :charging-mark-h]]
        (set-visible! r node-id false)))
    (do
      (set-visible! r :charging-layer false)
      (ui/set-prop! r :charging-mask :alpha 0.0))))

(defn- update-charging-arcs! [r snapshot]
  (let [arcs (or (:charging-arcs snapshot) [])
        ^ints counts (rt/user-signal r :overlay-count-cache)
        n (count arcs)]
    (when (not= (aget counts 4) n)
      (aset-int counts 4 n)
      (ui/list-set! r :charging-arcs arcs
                    (fn [rt item data] (update-vm-wave-item! rt item data))))
    (when-let [^INode list-node (ui/node r :charging-arcs)]
      (let [n (.getChildCount list-node)]
        (dotimes [i n]
          (when-let [arc (nth arcs i nil)]
            (when-let [^INode item (.getChild list-node i)]
              (update-vm-wave-item! r item arc))))))))

(defn- update-coin-qte-dot! [r item dot]
  (let [^INode img (ui/item-node item :dot)]
    (.setX img (double (:x dot)))
    (.setY img (double (:y dot)))
    (.setW img (double (:w dot)))
    (.setH img (double (:h dot)))
    (ui/set-node-prop! r img :tint (or (:tint dot) 0xFFFFD700))
    (.setDSlot img 0 (double (or (:alpha dot) 0.0)))
    (.setFlag img node/FLAG-RENDER-DIRTY)))

(defn- update-coin-qte-layer! [r snapshot]
  (if-let [qte (:coin-qte snapshot)]
    (do
      (set-visible! r :coin-qte-layer true)
       (let [{:keys [bg-disc dots marker pct-text]} qte
             ^ints counts (rt/user-signal r :overlay-count-cache)
             n (count dots)]
        (set-box-node-at! r (ui/node r :coin-qte-bg)
                          (:x bg-disc) (:y bg-disc) (:w bg-disc) (:h bg-disc) (:color bg-disc))
         (when (not= (aget counts 3) n)
           (aset-int counts 3 n)
          (ui/list-set! r :coin-qte-dots dots
                        (fn [rt item dot] (update-coin-qte-dot! rt item dot))))
        (when-let [^INode list-node (ui/node r :coin-qte-dots)]
          (let [n (.getChildCount list-node)]
            (dotimes [i n]
              (when-let [dot (nth dots i nil)]
                (when-let [^INode item (.getChild list-node i)]
                  (update-coin-qte-dot! r item dot))))))
        (set-box-node-at! r (ui/node r :coin-qte-marker)
                          (:x marker) (:y marker) (:w marker) (:h marker) (:color marker))
        (when-let [^INode pct (ui/node r :coin-qte-pct)]
          (.setX pct (double (:x pct-text)))
          (.setY pct (double (:y pct-text)))
          (ui/set-node-prop! r pct :text (str (:text pct-text)))
          (ui/set-node-prop! r pct :color (rgba-map->argb (:color pct-text))))))
    (set-visible! r :coin-qte-layer false)))

(defn- update-tutorial-notif! [r snapshot]
  (if-let [n (:tutorial-notification snapshot)]
    (let [{:keys [bg icon title content]} n]
      (set-visible! r :tutorial-notif true)
      (when-let [^INode bg-n (ui/node r :tut-bg)]
        (.setX bg-n (double (:x bg)))
        (.setY bg-n (double (:y bg)))
        (.setW bg-n (double (:w bg)))
        (.setH bg-n (double (:h bg)))
        (ui/set-node-prop! r bg-n :src (:src bg))
        (.setDSlot bg-n 0 (double (:alpha bg))))
      (when-let [^INode icon-n (ui/node r :tut-icon)]
        (.setX icon-n (double (:x icon)))
        (.setY icon-n (double (:y icon)))
        (.setW icon-n (double (:w icon)))
        (.setH icon-n (double (:h icon)))
        (ui/set-node-prop! r icon-n :src (:src icon))
        (.setDSlot icon-n 0 (double (:alpha icon))))
      (when-let [^INode t (ui/node r :tut-title)]
        (.setX t (double (:x title)))
        (.setY t (double (:y title)))
        (ui/set-node-prop! r t :text (str (:text title)))
        (ui/set-node-prop! r t :color (rgba-map->argb (:color title))))
      (when-let [^INode c (ui/node r :tut-content)]
        (.setX c (double (:x content)))
        (.setY c (double (:y content)))
        (ui/set-node-prop! r c :text (str (:text content)))
        (ui/set-node-prop! r c :color (rgba-map->argb (:color content)))))
    (set-visible! r :tutorial-notif false)))

(defn- update-debug-lines! [r snapshot]
  (let [lines (:debug-lines snapshot)
        ^ints counts (rt/user-signal r :overlay-count-cache)
        n (count lines)]
    (when (not= (aget counts 2) n)
      (aset-int counts 2 n)
      (ui/list-set! r :debug-lines lines
                    (fn [rt item line]
                      (let [^INode n (ui/item-node item :line)]
                        (.setX n (double (:x line)))
                        (.setY n (double (:y line)))
                        (ui/set-node-prop! r n :text (str (:text line)))
                        (ui/set-node-prop! r n :color (long (:color line)))))))
    (when-let [^INode list-node (ui/node r :debug-lines)]
      (let [n (.getChildCount list-node)]
        (dotimes [i n]
          (when-let [line (nth lines i nil)]
            (when-let [^INode item (.getChild list-node i)]
              (let [^INode txt (ui/item-node item :line)]
                (.setX txt (double (:x line)))
                (.setY txt (double (:y line)))
                (ui/set-node-prop! r txt :text (str (:text line)))
                (ui/set-node-prop! r txt :color (long (:color line)))))))))
    (set-visible! r :debug-lines (seq lines))))

(defn- update-skill-flash! [r snapshot]
  (let [alpha (int (or (:screen-flash-alpha snapshot) 0))]
    (set-box-rgba! r :skill-flash-screen {:r 200 :g 220 :b 255 :a alpha})))

(defn- update-overlay-app! [r snapshot]
  (if-let [app-ui (:overlay-app-ui snapshot)]
    (let [{:keys [panel title subtitle]} app-ui]
      (set-visible! r :overlay-app-layer true)
      (set-box-at! r :overlay-app-panel (:x panel) (:y panel) (:w panel) (:h panel) (:color panel))
      (when-let [^INode t (ui/node r :overlay-app-title)]
        (.setX t (double (:x title)))
        (.setY t (double (:y title)))
        (ui/set-node-prop! r t :text (str (:text title)))
        (ui/set-node-prop! r t :color (long (:color title))))
      (if subtitle
        (do
          (set-visible! r :overlay-app-subtitle true)
          (when-let [^INode s (ui/node r :overlay-app-subtitle)]
            (.setX s (double (:x subtitle)))
            (.setY s (double (:y subtitle)))
            (ui/set-node-prop! r s :text (str (:text subtitle)))
            (ui/set-node-prop! r s :color (long (:color subtitle)))))
        (set-visible! r :overlay-app-subtitle false))
      (set-visible! r :cp-bar false)
      (set-visible! r :cp-bar-ghost false)
      (set-visible! r :overload-bar false)
      (set-visible! r :overload-preview false)
      (set-visible! r :skill-slots false)
      (set-visible! r :cpbar-bg false)
      (set-visible! r :overload-highlight false)
      (set-visible! r :activation-hint-group false)
      (set-visible! r :cp-numbers false)
      (set-visible! r :ol-numbers false)
      (set-visible! r :preset-row false))
    (set-visible! r :overlay-app-layer false)))

(defn- apply-jitter! [r interfered?]
  ;; Upstream CPBar.initEvents: the interference jitter is a GL11.glTranslated
  ;; inside CPBar's OWN FrameEvent only — it shakes just the CP bar (+ preset
  ;; hint, activate hint, CP/OL numbers, all part of the same widget), not the
  ;; whole HUD (skill slots, toasts, crosshair etc. stay put upstream too).
  ;; The master-alpha flicker (mAlpha *= alphaCurve.valueAt) is written into
  ;; :jitter-alpha before the binding flush, which multiplies the bar/ghost
  ;; alphas; the background and numbers are unbound, set directly.
  (when-let [^INode jitter-root (ui/node r :cpbar-jitter-group)]
    (let [[cp-dx cp-dy] (gameplay/hud-position :cpbar)
          jx (if interfered? (sig/sget-d (rt/user-signal r :jitter-x)) 0.0)
          jy (if interfered? (sig/sget-d (rt/user-signal r :jitter-y)) 0.0)
          x (+ (double cp-dx) jx)
          y (+ (double cp-dy) jy)
          flicker (if interfered?
                    (let [now-ms (sig/sget-l (rt/clock-ms-sig r))
                          {:keys [frames alpha-points]} (sig/sget-o (rt/user-signal r :interf-jitter-data))]
                      (anim/interference-alpha (double now-ms) frames alpha-points))
                    1.0)]
      (when (or (not= x (.getX jitter-root)) (not= y (.getY jitter-root)))
        (.setX jitter-root x)
        (.setY jitter-root y)
        (.setFlag jitter-root node/FLAG-LAYOUT-DIRTY))
      (when-let [ja (rt/user-signal r :jitter-alpha)]
        (sig/sset-d! ja (double flicker)))
      (ui/set-prop! r :cpbar-bg :alpha (* 0.8 (double flicker)))
      ;; Text nodes have no :alpha prop-writer — modulate the ARGB alpha byte.
      (ui/set-prop! r :cp-numbers :color
        (bit-or 0xFFFFFF (bit-shift-left (int (* 255.0 (double flicker))) 24))))))

(defn- apply-screen-size! [r sw sh]
  (doseq [id [:root :bg-mask]]
    (when-let [^INode n (ui/node r id)]
      (when (or (not= (double sw) (.getW n)) (not= (double sh) (.getH n)))
        (.setW n (double sw))
        (.setH n (double sh))
        (.setFlag n node/FLAG-LAYOUT-DIRTY)))))

(defonce ^:private hud-trace-state (object-array [::none]))

(defn update-overlay-signals!
  "Per-frame update. Called from overlay-host update-fn."
  [r]
  (when-let [player-uuid (local-player-uuid)]
    (let [now-ms (sig/sget-l (rt/clock-ms-sig r))
          sw (int (rt/screen-w r))
          sh (int (rt/screen-h r))
          snapshot (reactive-hud/build-snapshot player-uuid sw sh (overlay-input-state player-uuid now-ms))]
      ;; [HUD-TRACE] transition-only diagnostic: logs once at world entry and
      ;; once per activated? flip, with the session the render thread resolves.
      (let [act (boolean (:activated? snapshot))
            prev (aget ^objects hud-trace-state 0)]
        (when (not= act prev)
          (aset ^objects hud-trace-state 0 (Boolean/valueOf act))
          (log/debug "[HUD-TRACE] activated?"
                    {:to act
                     :session-id (runtime-hooks/client-session-id)
                     :uuid player-uuid})))
      (apply-screen-size! r sw sh)
      (when-let [bg-target (rt/user-signal r :bg-target)]
        (sig/sset-o! bg-target (mask-vec (:background-mask snapshot {:r 0.0 :g 0.0 :b 0.0 :a 0.0}))))
      (if (:overlay-app snapshot)
        (update-overlay-app! r snapshot)
        (do
          (when-let [cp-target (rt/user-signal r :cp-target)]
            (sig/sset-d! cp-target (if (:activated? snapshot)
                                     (double (or (:percent (:cp-bar snapshot)) 0.0))
                                     0.0)))
          (when-let [ol-target (rt/user-signal r :ol-target)]
            (sig/sset-d! ol-target (if (:activated? snapshot)
                                     (double (or (:percent (:overload-bar snapshot)) 0.0))
                                     0.0)))
          ;; Consumption-hint "release" cue: the solid :cp-bar shows the
          ;; predicted post-cost level while a hint is active, else the plain
          ;; current level; the pulsing :cp-bar-ghost (always plain current)
          ;; is shown only while the hint is active.
          (let [hint-pct (:hint-percent (:cp-bar snapshot))
                cur-pct (double (or (:percent (:cp-bar snapshot)) 0.0))]
            (when-let [cp-hint-active (rt/user-signal r :cp-hint-active)]
              (sig/sset-d! cp-hint-active (if (some? hint-pct) 1.0 0.0)))
            (when-let [cp-predicted-target (rt/user-signal r :cp-predicted-target)]
              (sig/sset-d! cp-predicted-target (if (:activated? snapshot)
                                                 (double (or hint-pct cur-pct))
                                                 0.0)))
            (set-visible! r :cp-bar-ghost (boolean (and (:activated? snapshot) hint-pct))))
          (when-let [ol-scroll (rt/user-signal r :ol-scroll)]
            (sig/sset-d! ol-scroll (double (or (:scroll-offset (:overload-bar snapshot)) 0.0))))
          ;; Category icon — only dirty when actually changed
          (when-let [^objects cache (rt/user-signal r :overlay-object-cache)]
            (let [icon-src (or (:category-icon (:cp-bar snapshot)) "")]
              (when (not= (aget cache 1) icon-src)
                (aset cache 1 icon-src)
                (when-let [^INode n (ui/node r :cp-bar)]
                  (.setOSlot n 3 icon-src)
                  (.setFlag n node/FLAG-RENDER-DIRTY)))))
          ;; Bar background — only switch on overload state change
          (when-let [^booleans flags (rt/user-signal r :overlay-flag-cache)]
            (let [overloaded? (boolean (:overloaded (:overload-bar snapshot)))]
              (when (not= (aget flags 0) overloaded?)
                (aset-boolean flags 0 overloaded?)
                (ui/set-prop! r :cpbar-bg :src
                              (if overloaded?
                                (modid/asset-path "textures" "guis/cpbar/back_overload.png")
                                (modid/asset-path "textures" "guis/cpbar/back_normal.png"))))))
          ;; Overload highlight — upstream draws TEX_OVERLOAD_HIGHLIGHT only inside
          ;; drawOverload (isOverloaded), so gate it on :overloaded alone, not on a
          ;; near-full overload threshold. Only toggle visibility on state change.
          (when-let [^booleans flags (rt/user-signal r :overlay-flag-cache)]
            (let [overloaded? (boolean (:overloaded (:overload-bar snapshot)))]
              (when (not= (aget flags 1) overloaded?)
                (aset-boolean flags 1 overloaded?)
                (set-visible! r :overload-highlight overloaded?))))
          ;; The original passes `low` to every CP draw, including both
          ;; consumption-hint layers.
          (when-let [cp-low-mult (rt/user-signal r :cp-low-mult)]
            (let [low? (boolean (or (:recovering (:overload-bar snapshot))
                                    (:interfered? snapshot)))]
              (sig/sset-d! cp-low-mult (if low? 0.3 1.0))))
          (update-activation-indicator! r snapshot sw)
          (update-overload-lane! r snapshot sw)
          (update-numbers! r snapshot)
          (update-preset-indicators! r snapshot sw)
          (update-skill-slots! r snapshot now-ms sw sh)
          (update-movement-hints! r snapshot sw sh)
          (update-crosshair! r snapshot)))
      (update-vm-waves! r snapshot)
      (update-charging-layer! r snapshot sw sh)
      (update-charging-arcs! r snapshot)
      (update-coin-qte-layer! r snapshot)
      (update-toasts! r snapshot)
      (update-tutorial-notif! r snapshot)
      (update-debug-lines! r snapshot)
      (update-skill-flash! r snapshot)
      (apply-jitter! r (:interfered? snapshot)))))

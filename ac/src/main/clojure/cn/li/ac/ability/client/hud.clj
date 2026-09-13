(ns cn.li.ac.ability.client.hud
  "HUD render data builder (AC layer - no Minecraft imports)."
  (:require [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.ability.client.combat-notice :as combat-notice]
            [cn.li.ac.ability.model.cooldown :as cd-data]
            [cn.li.ac.ability.rules.cooldown-rules :as cd-rules]
            [cn.li.ac.ability.client.delegate-state :as dstate]
            [cn.li.ac.config.gameplay :as gameplay]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- ability-key-label [idx]
  (let [key-code (gameplay/input-key (keyword (str "ability-key-" idx)))]
    (if (number? key-code)
      (case (long key-code)
        -100 :mouse-left
        -99 :mouse-right
        (or (bridge/call-adapter :settings-key-name key-code)
            (str "KEY_" key-code)))
      (or (bridge/call-adapter :settings-key-name key-code)
          (str "KEY_" key-code)))))

(def ^:private cpbar-scale 0.2)

(defn cpbar-layout
  "Return the original AcademyCraft CPBar geometry in GUI-scaled screen pixels.

   Upstream declares a 964x147 widget, scales it by 0.2, right-aligns it, then
   applies pos(-12, 12). Values stay as doubles until the render adapter's
   final pixel rounding."
  [screen-width]
  (let [s cpbar-scale
        x (+ (- (double screen-width) (* 964.0 s)) -12.0)
        y 12.0]
    {:origin [x y]
     :frame {:x x :y y :w (* 964.0 s) :h (* 147.0 s)}
     :overload-preview {:x x :y (+ y (* 21.0 s))
                        :w (* 943.0 s) :h (* 104.0 s)}
     :overload-active {:x (+ x (* 30.0 s)) :y y
                       :w (* 914.0 s) :h (* 147.0 s)}
     :cp-lane {:x (+ x (* 47.0 s)) :y (+ y (* 30.0 s))
               :w (* 883.0 s) :h (* 84.0 s)}
     :category-icon {:x-offset (* (- 857.0 47.0) s)
                     :y-offset (* (- 43.0 30.0) s)
                     :w (* 65.0 s) :h (* 65.0 s)}
     :cp-numbers {:x (+ x (* 110.0 s)) :y (+ y (* 55.0 s))
                  :font-size (* 40.0 s)}
     :ol-numbers {:x (+ x (* 110.0 s)) :y (+ y (* 85.0 s))
                  :font-size (* 40.0 s)}
     :activation-hint {:text-right-x (+ x (* 500.0 s))
                       :text-y (+ y (* 140.0 s))
                       :font-size (* 44.0 s)
                       :margin (* 8.0 s)
                       :box-y (+ y (* (- 140.0 8.0) s))
                       :box-h (* (+ 44.0 16.0) s)
                       :glow-size (* 5.0 s)}
     :preset-row {:x (+ x (* 580.0 s)) :y (+ y (* 136.0 s))
                  :box-size (* 52.0 s) :step (* 62.0 s)
                  :font-size (* 46.0 s)
                  :text-y-offset (* 5.0 s)}}))

(defn cpbar-activation-hint-box
  "Return the upstream activation-hint background/glow rectangle for a
   measured text width in screen pixels."
  [screen-width text-width]
  (let [{:keys [text-right-x margin box-y box-h glow-size]}
        (:activation-hint (cpbar-layout screen-width))
        text-width (max 0.0 (double text-width))]
    {:x (- text-right-x margin text-width)
     :y box-y
     :w (+ text-width (* 2.0 margin))
     :h box-h
     :glow-size glow-size}))

(defn build-cp-bar-render-data
  "Build CP bar render data matching original AcademyCraft CPBar:
   - bg-texture (back_normal.png) drawn at full width as the bar frame
   - fg-texture (cp.png) clipped via scissor to the filled percentage —
     the texture itself carries the red→yellow→white gradient
   - right-side 16×16 cutout for the category icon
   - red consumption-hint line showing predicted CP after active skill cost
   - pulsing white overlay when CP reaches 100%"
  [model]
  (let [{:keys [cur max]} (:cp model)
        percent (if (and max (pos? max)) (/ cur max) 0.0)
        consumption-hint (:consumption-hint model 0.0)
        hint-percent (if (and max (pos? max) (pos? consumption-hint))
                       (/ (- cur consumption-hint) max)
                       nil)]
    {:type :cp-bar
     :x 8 :y 8
     :width 100 :height 10
     :percent (+ 0.0 percent)        ;; force primitive double without AOT-unsafe double()
     :hint-percent (when hint-percent (+ 0.0 (clojure.core/max 0.0 hint-percent)))
     :icon-cutout {:x-offset 84 :w 16}
     :bg-texture (modid/asset-path "textures" "guis/cpbar/back_normal.png")
     :fg-texture (modid/asset-path "textures" "guis/cpbar/cp.png")
     :category-icon (:category-icon model)
     :full-glow? (>= percent 1.0)}))

(defn scroll-offset-for-now
  "Pure wall-clock U-scroll phase for the overload bar's fg-texture animation.
   Depends only on now-ms — no player-state dependency, so callers refreshing
   a cached overload-bar element can recompute just this field every frame."
  [now-ms]
  (let [t (+ 0.0 (or now-ms 0))]
    (+ 0.0 (mod (/ t 2000.0) 1.0))))

(defn build-overload-bar-render-data
  "Build overload bar render data matching original AcademyCraft:
   - bg-texture switches between back_normal.png (fine) and back_overload.png (overloaded)
   - fg-texture (front_overload.png) clipped via scissor, with scroll-offset
     driving a horizontal U-scroll animation — the texture itself carries
     the cyan→green→pink gradient
   - overloaded flag triggers CPU pulse highlight (cpbar_overload shader approximation)"
  [model now-ms]
  (let [{:keys [cur max overloaded recovering]} (:overload model)
        overloaded? (boolean overloaded)
        percent (if (and max (pos? max)) (/ cur max) 0.0)
        scroll-offset (scroll-offset-for-now now-ms)]
    {:type :overload-bar
     :x 8 :y 22
     :width 100 :height 10
     :percent (+ 0.0 percent)
     ;; :overloaded gates the "drawOverload" visual (isOverloaded, post-cap phase);
     ;; :recovering gates cant-use dimming (isOverloadRecovering, whole !fine tail).
     :overloaded overloaded?
     :recovering (boolean recovering)
     :scroll-offset scroll-offset
     :bg-texture (if overloaded?
                   (modid/asset-path "textures" "guis/cpbar/back_overload.png")
                   (modid/asset-path "textures" "guis/cpbar/back_normal.png"))
     :fg-texture (modid/asset-path "textures" "guis/cpbar/front_overload.png")}))

(defn build-skill-slot-shape
  "Per-slot identity/shape: skill lookup, icon, name, key-label, position,
  cooldown-total source. Depends only on the active-slots binding (preset-data) —
   no cooldown-data/context dependency, so callers can cache this and only
   rebuild when the player rebinds a skill slot."
  [model screen-width screen-height]
  (vec
   (keep-indexed
    (fn [idx slot]
      (when (and slot (vector? slot) (= 2 (count slot)))
        (let [[cat-id ctrl-id] slot
              skill-id (skill-query/get-skill-by-controllable cat-id ctrl-id)
              skill-spec (when skill-id (skill-registry/get-skill skill-id))]
          (when skill-id
            {:type :skill-slot
             :idx idx
             :x (- screen-width 120)
             :y (+ (- screen-height 100) (* idx 22))
             ;; Upstream ClientHandler.keyIDsInit default: MOUSE_LEFT,
             ;; MOUSE_RIGHT, R, F. Mouse slots carry a keyword (no text glyph
             ;; upstream draws a mouse icon instead — see KeyHintUI.drawSingle
             ;; TEX_MOUSE_L/TEX_MOUSE_R); keyboard slots carry the key name.
             :key-label (ability-key-label idx)
             :skill-id skill-id
             :skill-icon (skill-query/get-skill-icon-path skill-id)
             :skill-name (skill-query/skill-display-name skill-id)
             ;; Stand-in denominator for slots with no live cooldown to read a
             ;; recorded duration from — same declaration the apply paths use,
             ;; so the two can no longer disagree.
             :cooldown-total-spec (cd-rules/ticks-spec skill-spec)}))))
    (:active-slots model))))

(defn patch-skill-slot-cooldown
  "Refresh per-slot cooldown numeric fields from cooldown-data (plain map
   lookups, no registry/context calls) — cheap enough to run every frame."
  [slot-shapes cooldown-data {:keys [player-id skill-exps]}]
  (mapv (fn [s]
          (let [ctrl-id (:skill-id s)
                exp (double (or (get skill-exps ctrl-id) 0.0))
                in-cooldown (cd-data/in-cooldown? cooldown-data ctrl-id :main)
                remaining (when in-cooldown
                            (cd-data/get-remaining cooldown-data ctrl-id :main))
                ;; Upstream KeyHintUI: prog = tickLeft / maxTick — the duration
                ;; the cooldown was actually started with, carried in the data.
                ;; The spec estimate below is only a stand-in for slots that
                ;; aren't on cooldown (no recorded max to read); using it while
                ;; one is running drifts from the countdown whenever the applied
                ;; duration differs from the estimate.
                applied-max (cd-data/get-max cooldown-data ctrl-id :main)
                total (if (pos? applied-max)
                        applied-max
                        (cd-rules/resolve-ticks-value (:cooldown-total-spec s)
                                                      {:player-id player-id
                                                       :skill-id ctrl-id
                                                       :exp exp}))
                remaining-seconds (when remaining (/ remaining 20.0))]
            (assoc s
                   :cooldown-total total
                   :in-cooldown in-cooldown
                   :cooldown-remaining (or remaining 0)
                   :cooldown-seconds (or remaining-seconds 0.0))))
        slot-shapes))

(defn patch-skill-slot-visual
  "Refresh per-slot delegate-state visual fields from active-contexts —
   changes only on ability context lifecycle events, callers may cache
   this keyed on a context snapshot token. now-ms (optional) is forwarded
   to skill-specific delegate hooks that need a clock (e.g. railgun coin QTE)."
  [slot-shapes active-contexts player-uuid & [now-ms]]
  (mapv (fn [s]
          (let [visual (dstate/delegate-state-for-slot active-contexts (:skill-id s)
                                                       player-uuid now-ms)]
            (assoc s
                   :visual-state (:state visual)
                   :alpha (:alpha visual)
                   :glow-color (:glow-color visual)
                   :sin-effect? (:sin-effect? visual))))
        slot-shapes))

(defn build-activation-indicator-data
  "Build activation indicator render data matching original AcademyCraft:
   - centered status dot (green=activated, gray=inactive)
   - V-key hint text to the right of the dot
   - positioned near top-center of screen"
  [model activate-hint]
  {:type :activation-indicator
   :y 10
   :activated (:activated model)
   :hint activate-hint})

(defn build-combat-notice-data
  [combat-notice-component now-ms]
  ;; NB: client-session-id is a FUNCTION (hooks core 调用规范 #4) — invoke it.
  (when-let [session-id (when combat-notice-component
                          (runtime-hooks/client-session-id))]
    (when-let [{:keys [text color alpha]} (combat-notice/active-notice combat-notice-component
                                                                       session-id
                                                                       :teleporter-crit
                                                                       now-ms)]
      {:type :combat-notice
       :y 26
       :text text
       :color {:a (int (* 255.0 (+ 0.0 alpha)))
               :r (int (nth color 0 255))
               :g (int (nth color 1 255))
               :b (int (nth color 2 255))}})))

(defn build-preset-indicators-data
  "Preset-switch selection-indicator fade, based on wall-clock now-ms —
   only non-nil within the ~2s window after a preset switch."
  [preset-state now-ms]
  (when preset-state
    (let [remaining (- (:show-until-ms preset-state 0) now-ms)]
      (when (pos? remaining)
        (let [current-idx (:current-preset preset-state 0)
              previous-idx (:previous-preset preset-state 0)
              elapsed (- 2000.0 remaining)
              ;; Previous fades out over full 2s
              prev-fade (max 0.0 (- 1.0 (/ elapsed 2000.0)))
              ;; Current fades in over first 500ms
              curr-fade (min 1.0 (/ elapsed 500.0))]
          (vec
           (keep identity
            [(when (and (not= previous-idx current-idx) (pos? prev-fade))
               {:type :preset-indicator
                :current previous-idx :total 4 :fade prev-fade})
             {:type :preset-indicator
              :current current-idx :total 4 :fade curr-fade}])))))))

(defn build-numbers-texts-data
  "CP/OL numeric readout (hold V to show, fades in/out), based on wall-clock
   now-ms — only non-nil while showing or within the original 300ms fade-out
   window."
  [hud-model showing-numbers? last-show-value-change-ms now-ms]
  ;; Upstream CPBar: when isOverloaded(), the CP/OL readout alpha is forced to 0
  ;; (numbers hidden entirely) — the overload visual owns the bar area instead.
  (when (and hud-model (not (get-in hud-model [:overload :overloaded])))
    (let [last-change (long (or last-show-value-change-ms 0))
          dt (- (long now-ms) last-change)
          alpha (if showing-numbers?
                  ;; Fade in: 200ms delay, then 400ms ramp to full
                  (cond (zero? last-change) 0.0
                        (< dt 200) 0.0
                        (< dt 600) (/ (+ 0.0 (- dt 200)) 400.0)
                        :else 1.0)
                  ;; Upstream CPBar.stopDisplayNumbers + draw-data branch:
                  ;; release after a real hold fades from 1 -> 0 over 300ms.
                  (cond (zero? last-change) 0.0
                        (< dt 300) (- 1.0 (/ (+ 0.0 dt) 300.0))
                        :else 0.0))
          ;; Original FontOption alpha is 0.6 * mAlpha * transition alpha;
          ;; this builder supplies the steady-state 0.6 factor.
          a (int (* 255.0 0.6 alpha))]
      (when (pos? alpha)
        [{:kind :text
          :text (str "CP " (int (get-in hud-model [:cp :cur])) "/" (int (get-in hud-model [:cp :max])))
          :x 115 :y 14 :color {:r 255 :g 255 :b 255 :a a}}
         {:kind :text
          :text (str "OL " (int (get-in hud-model [:overload :cur])) "/" (int (get-in hud-model [:overload :max])))
          :x 115 :y 29 :color {:r 255 :g 255 :b 255 :a a}}]))))

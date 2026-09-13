(ns cn.li.mc1201.gui.cgui.font
  "CLIENT-ONLY CGui font bridge using MSDF shadow font (zero bundled assets).

  Font-size contract: :font-size N draws at N screen pixels.
  STB em height = DESIGN_PIXEL_HEIGHT (32 px); scale = N / 32."
  (:require [clojure.string :as str]
            [cn.li.mc1201.client.font.msdf-setup :as msdf-setup])
  (:import [net.minecraft.network.chat Component Style MutableComponent]
           [net.minecraft.client.gui Font GuiGraphics]
           [com.mojang.blaze3d.vertex PoseStack]
           [com.mojang.blaze3d.systems RenderSystem]
           [cn.li.mcbase.client MinecraftClientAccess]
           [cn.li.mc1201.client.font.msdf MsdfFontManager]))

(defonce ^:private registry (atom {}))
(defonce ^:private msdf-base-height (atom nil))

(defn- msdf-default-base-height []
  (double MsdfFontManager/CGUI_BASE_HEIGHT))

(defn set-msdf-base-height! [v]
  (reset! msdf-base-height (double v)))

(defn get-msdf-base-height []
  (or @msdf-base-height
      (reset! msdf-base-height (msdf-default-base-height))))

(defn register-font!
  "Register a CGui font keyword.
  `spec` keys: :bold? — italic style (optional), :monospace? — fixed-width mode"
  [name {:keys [bold? italic? monospace?]}]
  (swap! registry assoc name {:bold? (boolean bold?)
                              :italic? (boolean italic?)
                              :monospace? (boolean monospace?)})
  name)

(defn get-font [name]
  (get @registry name))

(defn font-exists? [name]
  (contains? @registry name))

;; ---- helpers ----

(defn- ensure-msdf-ready! []
  (msdf-setup/ensure-ready!))

(defn- msdf-active? []
  (ensure-msdf-ready!)
  (MsdfFontManager/isAvailable))

(defn- vanilla-font ^Font []
  (MinecraftClientAccess/getFont))

(defn- shadow-font ^Font []
  (MsdfFontManager/shadowFont))

(defn- build-style ^Style [{:keys [italic?]}]
  (if italic?
    (.withItalic Style/EMPTY true)
    Style/EMPTY))

(def ^:private argb-alpha-mask -16777216)

(defn normalize-color-int
  [color-int]
  (unchecked-int
   (bit-or (bit-and (long (or color-int 0)) 0xFFFFFF) argb-alpha-mask)))

(defn- codepoints [^String text]
  (.toArray (.codePoints text)))

(defn- msdf-glyph? [cp]
  (MsdfFontManager/hasGlyph (int cp)))

(defn- segment-runs
  "Split `text` into runs of consecutive same-family (MSDF/non-MSDF) codepoints.
  Single pass over the codepoint array; run boundaries are cut via `subs` on
  the original string (tracking the parallel char-index, correct across
  surrogate pairs) instead of the old per-codepoint `Character/toString` +
  `str` concatenation (which reallocated the run's text on every codepoint)."
  [^String text]
  (let [^ints cps (codepoints text)
        n (alength cps)]
    (if (or (not (MsdfFontManager/hasFontFace)) (zero? n))
      [{:msdf? false :text (or text "")}]
      (loop [i 0
             char-i 0
             run-start-char 0
             run-msdf? (msdf-glyph? (aget cps 0))
             acc (transient [])]
        (if (< i n)
          (let [cp (aget cps i)
                cw (Character/charCount cp)
                m? (msdf-glyph? cp)]
            (if (= m? run-msdf?)
              (recur (inc i) (+ char-i cw) run-start-char run-msdf? acc)
              (recur (inc i) (+ char-i cw) char-i m?
                     (conj! acc {:msdf? run-msdf? :text (subs text run-start-char char-i)}))))
          (persistent! (conj! acc {:msdf? run-msdf? :text (subs text run-start-char char-i)})))))))

(defn- scale-factor [font-size]
  (* (/ (double font-size) (get-msdf-base-height)) 1.0))

(def ^:private vanilla-line-height 9.0)

(defn- vanilla-scale-factor [font-size]
  ;; Vanilla font is 9px tall (8px glyphs + 1px line gap) at scale 1.0, so a
  ;; :font-size N fallback draw needs N/9 — the MSDF em's N/32 would shrink
  ;; every glyph to ~28% when the MSDF face is unavailable.
  (/ (double font-size) vanilla-line-height))

(defn- component-for-run [^String text font-desc]
  (let [^MutableComponent c (Component/literal (or text ""))]
    (.withStyle c ^Style (build-style (or font-desc {})))))

;; ---- MSDF shader reference (set by ForgeClientRenderRegistry / FabricClientRenderSetup) ----

(defonce ^:private msdf-shader (atom nil))

(defn set-msdf-shader! [shader]
  (reset! msdf-shader shader))

;; ---- draw ----

(defn- end-vanilla-draw!
  "Undo the depth state GuiGraphics leaves behind after a drawString.

  `GuiGraphics.drawString` flushes whenever the graphics is unmanaged — which
  is every Screen — and `GuiGraphics.flush` ends with glEnable(GL_DEPTH_TEST),
  because vanilla's own GUI pass wants the test back on. The reactive tape's
  contract is the opposite: depth test OFF unless a node opts in through
  :depth-func (see render.clj's pop-depth!), since blits carry no state of
  their own and every quad after this one inherits whatever we leave behind.

  Under the ortho GUI projection the difference is invisible — coplanar quads
  land on bit-identical depth, so LEQUAL passes everywhere. Under the
  terminal's perspective camera it is not: an app icon and the app_back plate
  it sits on are coplanar but built from different vertices, so their
  interpolated depths disagree by float rounding, LEQUAL starts rejecting
  fragments of the icon, and the icon tears apart and reassembles as the
  camera sways."
  []
  (RenderSystem/disableDepthTest))

(defn- draw-run! [^GuiGraphics gg ^Font font ^Component comp x y color shadow?]
  (RenderSystem/enableBlend)
  (RenderSystem/defaultBlendFunc)
  (RenderSystem/disableDepthTest)
  (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)
  (when-let [shader @msdf-shader]
    (RenderSystem/setShader (fn [] shader)))
  (.drawString gg font comp (int x) (int y) (unchecked-int color) (boolean shadow?))
  (end-vanilla-draw!))

;; ---- width ----

(defn- with-monospace [monospace? body-fn]
  (MsdfFontManager/setMonospace (boolean monospace?))
  (try (body-fn)
       (finally (MsdfFontManager/setMonospace false))))

;; ---- text bake cache ----
;; segment-runs + per-run Component + per-run width used to be recomputed
;; twice per draw-text! call (once for alignment width, once for drawing) and
;; every frame even for unchanged text. Baked result is font-size-independent
;; (scale is a pure multiplier applied downstream by callers), so it's cached
;; by [font-desc text] — structural map equality means callers passing a
;; fresh-but-equal font-desc map still hit; no key allocation on a cache hit,
;; only `get` against what the caller already passed in.
(defonce ^:private text-bake-cache (atom {}))
(defonce ^:private last-has-font-face? (atom false))
(defonce ^:private last-cache-clear-ms (atom 0))
(def ^:private cache-refresh-interval-ms
  "Bounds how long a text/style combo can stay pinned to a stale MSDF/vanilla
  run classification if a glyph's MSDF atlas entry finishes async pre-baking
  (triggered by MsdfFontManager/getGlyph) after this cache already classified
  it as non-MSDF. A few seconds is imperceptible but keeps the cache from
  being wrong indefinitely — the old uncached code re-segmented every frame
  and so self-corrected within ~1 frame; this bounds it to this interval
  instead."
  4000)

(defn- bake-text
  "{:runs [{:msdf? bool :text String :comp Component :width double} ...]
    :total-width double} — width is unscaled (em-space), matching the old
  segmented-width contract; multiply by `scale-factor` to get screen pixels."
  [font-desc ^String text]
  (let [monospace? (boolean (:monospace? font-desc))]
    (with-monospace monospace?
      (fn []
        (let [^Font shadow (shadow-font)
              ^Font vanilla (vanilla-font)
              use-shadow? (MsdfFontManager/hasFontFace)
              ^Font font (if use-shadow? shadow vanilla)]
          (loop [runs (segment-runs text) acc (transient []) total 0.0]
            (if (empty? runs)
              {:runs (persistent! acc) :total-width total}
              (let [{:keys [msdf? text]} (first runs)
                    ^Component comp (component-for-run text font-desc)
                    w (double
                       (if (and use-shadow? msdf? monospace?)
                         (* (count text) (MsdfFontManager/monospaceAdvance))
                         (.width font comp)))]
                (recur (rest runs)
                       (conj! acc {:msdf? msdf? :text text :comp comp :width w})
                       (+ total w))))))))))

(defn- baked-for
  [font-desc ^String text]
  ;; MSDF face can transition ready-state after this ns's first calls (async
  ;; load); invalidate on that transition. Also time-bounded refresh — see
  ;; `cache-refresh-interval-ms`.
  (let [ready? (boolean (MsdfFontManager/hasFontFace))
        now (System/currentTimeMillis)]
    (when (or (not= ready? @last-has-font-face?)
              (> (- now (long @last-cache-clear-ms)) cache-refresh-interval-ms))
      (reset! last-has-font-face? ready?)
      (reset! last-cache-clear-ms now)
      (reset! text-bake-cache {})))
  (let [by-text (get @text-bake-cache font-desc)]
    (or (get by-text text)
        (let [b (bake-text font-desc text)]
          (swap! text-bake-cache update font-desc (fnil assoc {}) text b)
          b))))

(defn- segmented-width [font-desc ^String text & {:keys [glyph-styles]}]
  (:total-width (baked-for font-desc text)))

(defn text-width
  ([font-desc ^String text font-size]
   (text-width font-desc text font-size nil))
  ([font-desc ^String text font-size glyph-styles]
   (ensure-msdf-ready!)
   (let [scale (scale-factor font-size)]
     (cond
       (str/blank? text) 0.0
       (or (msdf-active?) (MsdfFontManager/hasFontFace))
       (with-monospace (boolean (:monospace? font-desc))
         #(* scale (segmented-width font-desc text :glyph-styles glyph-styles)))
       :else
       (* (double (.width (vanilla-font) (Component/literal text)))
          (vanilla-scale-factor font-size))))))

;; ---- draw-msdf-runs! ----

(defn- aligned-x [align x total-w]
  (case align
    :center (- (double x) (/ total-w 2.0))
    :right (- (double x) total-w)
    (double x)))

(defn- draw-vanilla-run! [^GuiGraphics gg font-desc ^String text x y font-size color shadow?]
  (let [^Font mc-font (vanilla-font)
        ^Component comp (component-for-run text font-desc)
        scale (float (vanilla-scale-factor font-size))
        ^PoseStack ps (.pose gg)]
    (.pushPose ps)
    (try
      (.translate ps (double x) (double y) 0.0)
      (.scale ps scale scale 1.0)
      (.drawString gg mc-font comp 0 0 (unchecked-int color) (boolean shadow?))
      (finally
        (.popPose ps)))
    (.endBatch (.bufferSource gg))
    (end-vanilla-draw!)))

(defn- draw-msdf-runs! [^GuiGraphics gg font-desc ^String text x y font-size color shadow?
                       & {:keys [glyph-styles]}]
  ;; Reuses the same baked runs `segmented-width`/`text-width` already forced
  ;; a cache entry for — per-run Component + width are not recomputed here.
  (let [scale (scale-factor font-size)
        {:keys [runs]} (baked-for font-desc text)
        ^Font shadow (shadow-font)
        ^Font vanilla (vanilla-font)
        use-shadow? (or (msdf-active?) (MsdfFontManager/hasFontFace))
        ^Font font (if use-shadow? shadow vanilla)
        ^PoseStack ps (.pose gg)]
    (.pushPose ps)
    (try
      (.translate ps (double x) (double y) 0.0)
      (.scale ps (float scale) (float scale) 1.0)
      (loop [rs runs dx 0.0]
        (when (seq rs)
          (let [{:keys [^Component comp width]} (first rs)]
            (draw-run! gg font comp dx 0 color shadow?)
            (recur (rest rs) (+ dx (double width))))))
      (finally
        (.popPose ps)))
    (.endBatch (.bufferSource gg))))

(defn- draw-fallback-runs! [^GuiGraphics gg font-desc ^String text x y font-size color shadow?
                       & {:keys [glyph-styles]}]
  (draw-msdf-runs! gg font-desc text x y font-size color shadow? :glyph-styles glyph-styles))

(defn draw-text!
  "Draw `text` at (`x`,`y`) with MSDF shadow font (or vanilla fallback).
  `align` is :left, :center, or :right (relative to `x`)."
  ([^GuiGraphics gg font-desc ^String text x y font-size color align shadow?]
   (draw-text! gg font-desc text x y font-size color align shadow? nil))
  ([^GuiGraphics gg font-desc ^String text x y font-size color align shadow? glyph-styles]
   (when (seq text)
     (ensure-msdf-ready!)
     (let [total-w (text-width font-desc text font-size glyph-styles)
           x' (aligned-x align x total-w)]
       (with-monospace (boolean (:monospace? font-desc))
         #(cond
            (msdf-active?)
            (draw-msdf-runs! gg font-desc text x' y font-size color shadow?
                             :glyph-styles glyph-styles)
            (MsdfFontManager/hasFontFace)
            (draw-fallback-runs! gg font-desc text x' y font-size color shadow?
                                :glyph-styles glyph-styles)
            :else
            (draw-vanilla-run! gg font-desc text x' y font-size color shadow?)))))))

(def default-mc-font nil)

(defn get-cgui-font-base-height []
  (get-msdf-base-height))

(defn get-fallback-scale-factor []
  1.0)

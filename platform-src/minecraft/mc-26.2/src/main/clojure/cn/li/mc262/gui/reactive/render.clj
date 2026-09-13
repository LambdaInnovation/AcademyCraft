(ns cn.li.mc262.gui.reactive.render
  "Kind renderers for reactive UI on Minecraft 26.2."
  (:require [cn.li.mc262.client.texture-registry :as tex-registry]
            [cn.li.mc262.gui.cgui.font :as cgui-font]
            [cn.li.mc262.gui.reactive.clock :as clock]
            [cn.li.platform.neutral.client-runtime :as platform-bridge]
            [cn.li.platform.neutral.ui :as node]
            [cn.li.platform.neutral.ui :as ui-layout]
            [cn.li.mcmod.util.log :as log]
            [clojure.string :as str]
            [cn.li.mc262.runtime.registry :as registry])
  (:import [cn.li.mc262.client GuiGraphicsHelper]
           [cn.li.mc262.client.render GuiPerspectiveWarp ReactivePreviewRenderState]
           [com.mojang.blaze3d.pipeline RenderPipeline]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.gui GuiGraphicsExtractor]
           [net.minecraft.client.renderer.texture MissingTextureAtlasSprite]
           [net.minecraft.resources Identifier]
           [net.minecraft.world.item Item ItemStack Items]
           [net.minecraft.world.level.block Block Blocks]
           [net.minecraft.world.level.block.state BlockState]
           [org.joml Matrix3x2f Matrix3x2fStack]))

(declare draw-tape!)

(def ^:private SLOT-BOX-FILL      0)
(def ^:private SLOT-BOX-OUTLINE   1)
(def ^:private SLOT-BOX-OUTLINE-W 2)
(def ^:private SLOT-BOX-TINT      3)
(def ^:private SLOT-BOX-HOVER     4)

(def ^:private SLOT-IMG-SRC  0)
(def ^:private SLOT-IMG-BAKED-RL 2)
(def ^:private SLOT-IMG-ALPHA 0)
(def ^:private SLOT-IMG-U     1)
(def ^:private SLOT-IMG-V     2)
(def ^:private SLOT-IMG-TEX-W 3)
(def ^:private SLOT-IMG-TEX-H 4)

(def ^:private SLOT-TEXT-TEXT    0)
(def ^:private SLOT-TEXT-BAKED  8)

(def ^:private SLOT-PROG-PROGRESS 0)
(def ^:private SLOT-PROG-CORNER   1)
(def ^:private SLOT-PROG-SCROLL   3)
(def ^:private SLOT-PROG-ALPHA    4)
(def ^:private SLOT-PROG-BANDS   11)

(def ^:private SLOT-SHADER-PROPS 0)
(def ^:private SLOT-SHADER-PROGRESS 0)
(def ^:private SLOT-SQUAD-ALPHA 0)
(def ^:private SLOT-SHADER-TEX-0 4)
(def ^:private SLOT-SHADER-TEX-1 5)

(def ^:private SLOT-DM-SRC 0)
(def ^:private SLOT-DM-BAKED 1)

(def ^:private SLOT-NS-SRC 0)
(def ^:private SLOT-NS-LINE 1)
(def ^:private SLOT-NS-BAKED 2)
(def ^:private SLOT-NS-MARGIN 0)

(defn- resolve-tex-loc [tex-key]
  (cond
    (instance? Identifier tex-key) tex-key
    (keyword? tex-key) (tex-registry/resolve-texture tex-key)
    (string? tex-key) (ResourceLocations/parse tex-key)
    :else nil))

(defn- node-abs-x [^INode n] (.getAbsX n))
(defn- node-abs-y [^INode n] (.getAbsY n))
(defn- node-scale [^INode n] (.getCumScale n))
(defn- scaled-w [^INode n] (* (.getW n) (node-scale n)))
(defn- scaled-h [^INode n] (* (.getH n) (node-scale n)))

(defn- argb [^long a ^long r ^long g ^long b]
  (unchecked-int (bit-or (bit-shift-left a 24) (bit-shift-left r 16) (bit-shift-left g 8) b)))

(defn- unit-byte
  ^long [value]
  (long (Math/round (* 255.0 (max 0.0 (min 1.0 (double value)))))))

(defn- resolve-pipeline
  ^RenderPipeline [shader-id]
  (let [value (platform-bridge/resolve-shader shader-id)]
    (when (instance? RenderPipeline value)
      value)))

(defn- depth-layer-byte
  "Encode the two skill-tree mask layers consumed by gui_mask_depth_26.vsh.
   64 maps to approximately -0.5 NDC (plate), 96 to -0.25 (ring)."
  ^long [^INode node]
  (if (<= (double (:depth-z (.getStaticProps node) -1.0)) -1.5)
    64
    96))

(defn render-box! [^GuiGraphicsExtractor gg ^INode node]
  (let [x  (node-abs-x node)  y  (node-abs-y node)
        w  (scaled-w node)    h  (scaled-h node)
        ix (unchecked-int x)  iy (unchecked-int y)
        iw (unchecked-int (+ x w))  ih (unchecked-int (+ y h))]
    (let [fill-argb (unchecked-int (long (.getDSlot node SLOT-BOX-FILL)))]
      (when (not= fill-argb 0)
        (GuiGraphicsHelper/fill gg ix iy iw ih fill-argb)))
    (let [outline-argb (unchecked-int (long (.getDSlot node SLOT-BOX-OUTLINE)))
          outline-w    (.getDSlot node SLOT-BOX-OUTLINE-W)]
      (when (and (not= outline-argb 0) (> outline-w 0.0))
        (GuiGraphicsHelper/fill gg ix iy iw (unchecked-int (+ iy outline-w)) outline-argb)
        (GuiGraphicsHelper/fill gg ix (unchecked-int (- ih outline-w)) iw ih outline-argb)
        (GuiGraphicsHelper/fill gg ix iy (unchecked-int (+ ix outline-w)) ih outline-argb)
        (GuiGraphicsHelper/fill gg (unchecked-int (- iw outline-w)) iy iw ih outline-argb)))
    (let [hovered? (.hasFlag node node/FLAG-HOVERED)
          raw (if hovered?
                (let [ht (.getDSlot node SLOT-BOX-HOVER)]
                  (if (> ht 0.0) ht (.getDSlot node SLOT-BOX-TINT)))
                (.getDSlot node SLOT-BOX-TINT))
          alpha (if (> raw 1.0)
                  (unchecked-int (bit-and (bit-shift-right (long raw) 24) 0xFF))
                  (unchecked-int (* 255.0 raw)))]
      (when (pos? alpha)
        (GuiGraphicsHelper/fill gg ix iy iw ih (argb alpha 255 255 255))))))

(defn- resolve-rl
  ^Identifier [src]
  (when (and src (string? src) (not (str/blank? src)))
    (let [src (if (str/ends-with? src ".png") src (str src ".png"))]
      (try (ResourceLocations/parse src) (catch Exception _ nil)))))

(defn bake-image! [^INode node]
  (let [src (.getOSlot node SLOT-IMG-SRC)]
    (when (string? src)
      (.setOSlot node SLOT-IMG-BAKED-RL (resolve-rl src)))))

(defn- image-tint-rgba [^INode node]
  (let [raw (.getOSlot node 1)]
    (if (vector? raw)
      (let [[r g b a] raw]
        [(float (/ (double (or r 255.0)) 255.0))
         (float (/ (double (or g 255.0)) 255.0))
         (float (/ (double (or b 255.0)) 255.0))
         (float (/ (double (or a 255.0)) 255.0))])
      [1.0 1.0 1.0 1.0])))

(defonce ^:private ^java.util.HashSet seen-image-textures (java.util.HashSet.))

(defn- warn-missing-texture!
  [^INode node ^Identifier rl]
  (when (.add seen-image-textures rl)
    (try
      (let [tex (.getTexture (.getTextureManager (Minecraft/getInstance)) rl)
            missing-loc (MissingTextureAtlasSprite/getLocation)]
        (when (or (nil? tex)
                  (= (str rl) (str missing-loc)))
          (log/warn
            "UI image texture did not resolve: " (str rl)
            " (node " (str (.getId node)) ") — draws as a blank quad")))
      (catch Throwable _ nil))))

(defn render-image! [^GuiGraphicsExtractor gg ^INode node]
  (let [rl-obj (.getOSlot node SLOT-IMG-BAKED-RL)
        ^Identifier rl (if (instance? Identifier rl-obj)
                         rl-obj
                         (when-let [src (.getOSlot node SLOT-IMG-SRC)]
                           (when (string? src)
                             (let [resolved (resolve-rl src)]
                               (.setOSlot node SLOT-IMG-BAKED-RL resolved)
                               resolved))))]
    (when rl
      (warn-missing-texture! node rl)
      (let [x  (node-abs-x node)  y  (node-abs-y node)
            w  (scaled-w node)    h  (scaled-h node)
            ix (unchecked-int x)  iy (unchecked-int y)
            iw (unchecked-int w)  ih (unchecked-int h)
            alpha (float (max 0.0 (min 1.0 (.getDSlot node SLOT-IMG-ALPHA))))
            u (.getDSlot node SLOT-IMG-U)
            v (.getDSlot node SLOT-IMG-V)
            tex-w-raw (.getDSlot node SLOT-IMG-TEX-W)
            tex-h-raw (.getDSlot node SLOT-IMG-TEX-H)
            tex-w (if (pos? tex-w-raw) tex-w-raw 1.0)
            tex-h (if (pos? tex-h-raw) tex-h-raw 1.0)
            [tr tg tb ta] (image-tint-rgba node)
            depth-equal? (= :equal (:depth-func (.getStaticProps node)))
            ^RenderPipeline depth-pipeline
            (when depth-equal? (resolve-pipeline :depth-equal))]
        (when (pos? alpha)
          (if depth-pipeline
            (GuiGraphicsHelper/blitPipeline
              gg depth-pipeline rl nil
              ix iy (unchecked-int (+ x w)) (unchecked-int (+ y h))
              (float u) (float (+ u tex-w))
              (float v) (float (+ v tex-h))
              (argb (unit-byte (* alpha ta))
                    (unit-byte tr) (unit-byte tg) (unit-byte tb)))
            (if (and (zero? u) (zero? v) (== tex-w 1.0) (== tex-h 1.0))
              (GuiGraphicsHelper/blit gg rl ix iy iw ih)
              (GuiGraphicsHelper/blitTexturedQuad gg rl
                (float x) (float y) (float (+ x w)) (float (+ y h)) 0.0
                (float u) (float (+ u tex-w))
                (float v) (float (+ v tex-h))))))))))

(defn- display-text [^INode node raw]
  (let [s (str (or raw ""))]
    (if (boolean (get (.getStaticProps node) :masked?))
      (apply str (repeat (count s) "*"))
      s)))

(defn- text-font-desc [font-raw]
  (cond
    (map? font-raw) font-raw
    (keyword? font-raw) (or (cgui-font/get-font font-raw) {})
    (string? font-raw) (or (cgui-font/get-font (keyword font-raw)) {})
    :else {}))

(defn- text-align-kw [align-raw]
  (let [kw (cond
             (keyword? align-raw) align-raw
             (string? align-raw) (keyword align-raw)
             :else :left)]
    (case kw
      :center :center
      :right :right
      :left)))

(defn bake-text! [^INode node]
  (let [text (display-text node (or (.getOSlot node SLOT-TEXT-TEXT) ""))
        font-size-raw (.getDSlot node 0)
        font-size (if (pos? font-size-raw) font-size-raw 14.0)
        color-raw (.getOSlot node 1)
        raw-long (if (number? color-raw) (long color-raw) 0xFFFFFFFF)
        text-alpha (/ (double (bit-and (bit-shift-right raw-long 24) 0xFF)) 255.0)
        color (let [c (cgui-font/normalize-color-int raw-long)]
                (if (< text-alpha 1.0)
                  (unchecked-int (bit-or (bit-and (long c) 0x00FFFFFF)
                                         (bit-shift-left (long (* 255.0 text-alpha)) 24)))
                  c))
        font-raw (or (.getOSlot node 2) (get (.getStaticProps node) :font))
        align-raw (or (.getOSlot node 3) (get (.getStaticProps node) :align))]
    (.setOSlot node SLOT-TEXT-BAKED
               {:text text
                :font-size (double font-size)
                :color color
                :font-desc (text-font-desc font-raw)
                :align (text-align-kw align-raw)
                :x-off (double (.getDSlot node 1))
                :y-off (double (.getDSlot node 2))})))

(defn render-text! [^GuiGraphicsExtractor gg ^INode node]
  (let [baked (.getOSlot node SLOT-TEXT-BAKED)]
    (when baked
      (let [{:keys [text color font-size font-desc align x-off y-off]} baked
            editable? (boolean (get (.getStaticProps node) :editable?))
            focused? (.hasFlag node node/FLAG-FOCUSED)
            align-kw (or align :left)
            ;; font-size / text offsets are authored in local design units (same
            ;; as w/h). Images/boxes already multiply by cum-scale; text must too
            ;; or scaled roots (terminal fit-scale, skill-tree camera, the HUD
            ;; key-hint column at 0.23) draw glyphs that ignore the shrink.
            ;; Matches mc-1.20.1 / mc-1.21.1 render-text!.
            s (.getCumScale node)
            font-size (* (double font-size) s)
            x-off (* (double x-off) s)
            y-off (* (double y-off) s)
            node-h (* (.getH node) (.getCumScale node))
            v-off (case (int (.getAlignH node))
                    1 (/ (- node-h (double font-size)) 2.0)
                    2 (- node-h (double font-size))
                    0.0)
            node-w (* (.getW node) (.getCumScale node))
            h-off (case align-kw :center (/ node-w 2.0) :right node-w 0.0)
            x (+ (node-abs-x node) x-off h-off)
            y (+ (node-abs-y node) y-off v-off)]
        (cgui-font/draw-text! gg (or font-desc {}) ^String text
                      x y font-size color align-kw true)
          (when (and editable? focused?
                 (< (rem (System/currentTimeMillis) 600) 300))
            (let [text-w (cgui-font/text-width (or font-desc {}) ^String text font-size)
              cursor-x (case align-kw
                     :center (+ x (/ text-w 2.0))
                     :right x
                     (+ x text-w))]
            (cgui-font/draw-text! gg (or font-desc {}) "|"
                        cursor-x (- y 1.0) font-size color :left false)))))))

(defn bake-progress! [^INode node]
  (let [bg    (.getOSlot node 1)
        fg    (.getOSlot node 2)
        icon  (.getOSlot node 3)
        stops (.getOSlot node 0)]
    (when (string? bg)   (.setOSlot node 8  (resolve-rl bg)))
    (when (string? fg)   (.setOSlot node 9  (resolve-rl fg)))
    (when (string? icon) (.setOSlot node 10 (resolve-rl icon)))
    (when (vector? stops) (.setOSlot node SLOT-PROG-BANDS stops))))

(defn- icon-cutout [^INode node]
  (let [cut (get (.getStaticProps node) :icon-cutout)]
    (when (map? cut) cut)))

(defn- progress-uv
  "The :uv-region crop as [u0 v0 u1 v1], defaulting to the whole texture."
  [^INode node]
  (let [uv (get (.getStaticProps node) :uv-region)]
    (if (and (vector? uv) (= 4 (count uv)))
      [(double (nth uv 0)) (double (nth uv 1)) (double (nth uv 2)) (double (nth uv 3))]
      [0.0 0.0 1.0 1.0])))

(defn- sample-progress-color [bands ^double t]
  (if (nil? bands)
    [255 255 255]
    (let [n (count bands)]
      (loop [i 0]
        (if (>= i n)
          (let [[_ r g b] (nth bands (dec n))] [(int r) (int g) (int b)])
          (let [[pos r g b] (nth bands i)
                pos (double pos)]
            (if (>= pos t)
              (if (zero? i)
                [(int r) (int g) (int b)]
                (let [[pos0 r0 g0 b0] (nth bands (dec i))
                      pos0 (double pos0)
                      span (- pos pos0)
                      f (if (zero? span) 0.0 (/ (- t pos0) span))]
                  [(int (+ (double r0) (* (- (double r) (double r0)) f)))
                   (int (+ (double g0) (* (- (double g) (double g0)) f)))
                   (int (+ (double b0) (* (- (double b) (double b0)) f)))]))
              (recur (inc i)))))))))

(defn- draw-bar-segment!
  "One horizontal run of the fill texture, tinted by the colour ramp.

   `diag` > 0 reproduces the 1.21.1 trapezoid. GuiGraphicsExtractor only ever
   submits axis-aligned quads, so the slant comes from the pose instead: shear
   it about the bar's top edge so the run's bottom edge sits diag*bar-h further
   right, extend the quad by that much, and let a scissor cut the equally
   slanted right edge back to vertical. The u ramp is anchored to the bar (not
   the run), so a partial fill shows the matching slice of the texture rather
   than the whole thing squashed."
  [^GuiGraphicsExtractor gg ^Identifier fg-rl
   seg-start seg-end bar-x bar-y bar-w bar-h diag scroll uv tint]
  (when (and fg-rl (< (double seg-start) (double seg-end))
             (pos? (double bar-w)) (pos? (double bar-h)))
    (let [seg-start (double seg-start) seg-end (double seg-end)
          bar-x (double bar-x) bar-y (double bar-y)
          bar-w (double bar-w) bar-h (double bar-h)
          diag (double diag) scroll (double scroll)
          [u0 v0 u1 v1] uv
          rate (/ (- (double u1) (double u0)) bar-w)
          u-of (fn ^double [^double sx]
                 (+ (double u0) scroll (* (- sx bar-x) rate)))
          y0 (unchecked-int bar-y)
          y1 (unchecked-int (+ bar-y bar-h))
          x0 (unchecked-int seg-start)
          x1 (unchecked-int seg-end)
          color (unchecked-int (long tint))]
      (if (and (pos? diag) (GuiPerspectiveWarp/active))
        (let [lean (* diag bar-h)
              ^Matrix3x2fStack pose (.pose gg)
              shear (Matrix3x2f. (float 1.0) (float 0.0) (float diag)
                                 (float 1.0) (float 0.0) (float 0.0))]
          (.enableScissor gg x0 y0 x1 y1)
          (.pushMatrix pose)
          (try
            (.translate pose (float 0.0) (float bar-y))
            (.mul pose shear)
            (.translate pose (float 0.0) (float (- bar-y)))
            (GuiGraphicsHelper/blitTintedQuad
              gg fg-rl x0 y0 (unchecked-int (+ seg-end lean)) y1
              (float (u-of seg-start)) (float (u-of (+ seg-end lean)))
              (float v0) (float v1) color)
            (finally
              (.popMatrix pose)
              (.disableScissor gg))))
        (GuiGraphicsHelper/blitTintedQuad
          gg fg-rl x0 y0 x1 y1
          (float (u-of seg-start)) (float (u-of seg-end))
          (float v0) (float v1) color)))))

(defn render-progress!
  "Bar background, fill and icon overlay in the 1.21.1 shape: optional slanted
   left edge (:corner), multi-stop colour ramp, :uv-region cropping,
   :fill-remap extent, :anchor :right growth and an :icon-cutout gap."
  [^GuiGraphicsExtractor gg ^INode node]
  (let [x (node-abs-x node) y (node-abs-y node)
        w (scaled-w node) h (scaled-h node)
        percent (double (.getDSlot node SLOT-PROG-PROGRESS))
        diag (max 0.0 (double (.getDSlot node SLOT-PROG-CORNER)))
        scroll (double (.getDSlot node SLOT-PROG-SCROLL))
        raw-alpha (double (.getDSlot node SLOT-PROG-ALPHA))
        fill-alpha (if (pos? raw-alpha) (min 1.0 raw-alpha) 1.0)
        sp (.getStaticProps node)
        remap (get sp :fill-remap)
        anchor-right? (= :right (get sp :anchor))
        fill-pct (if remap
                   (+ (double (nth remap 0)) (* percent (double (nth remap 1))))
                   percent)
        filled-w (max 0.0 (* fill-pct w))
        uv (progress-uv node)
        ix (unchecked-int x) iy (unchecked-int y)
        iw (unchecked-int w) ih (unchecked-int h)
        ^Identifier bg-rl (.getOSlot node 8)
        ^Identifier fg-rl (.getOSlot node 9)
        ^Identifier icon-rl (.getOSlot node 10)
        bands (.getOSlot node SLOT-PROG-BANDS)
        ;; Upstream CPBar.autoLerp samples the RAW percent, before :fill-remap
        ;; stretches the drawn extent.
        [cr cg cb] (sample-progress-color bands (max 0.0 (min 1.0 percent)))
        tint (argb (unchecked-int (* 255.0 fill-alpha)) cr cg cb)
        cut (icon-cutout node)
        cut-x0 (when cut (+ x (double (:x-offset cut 0))))
        cut-w (when cut (double (:w cut 0)))
        cut-x1 (when (and cut-x0 cut-w) (+ (double cut-x0) (double cut-w)))
        bar-right (+ x w)]
    (when bg-rl
      (GuiGraphicsHelper/blit gg bg-rl ix iy iw ih))
    (when (pos? filled-w)
      (if fg-rl
        (if anchor-right?
          ;; Upstream drawCPBar pins the icon-side edge and grows leftward; draw
          ;; one run all the way to the right edge so the diagonal never has to
          ;; degenerate on the sliver beside the icon.
          (draw-bar-segment! gg fg-rl (max x (- bar-right filled-w)) bar-right
                             x y w h diag scroll uv tint)
          (let [bar-end (min bar-right (+ x filled-w))]
            (if (and cut-x0 cut-w (pos? (double cut-w)))
              (do (draw-bar-segment! gg fg-rl x (min bar-end (double cut-x0))
                                     x y w h diag scroll uv tint)
                  (draw-bar-segment! gg fg-rl (double cut-x1) bar-end
                                     x y w h diag scroll uv tint))
              (draw-bar-segment! gg fg-rl x bar-end x y w h diag scroll uv tint))))
        ;; No fill texture: flat colour run, still honouring the anchor.
        (let [x0 (if anchor-right? (max x (- bar-right filled-w)) x)
              x1 (if anchor-right? bar-right (min bar-right (+ x filled-w)))]
          (GuiGraphicsHelper/fill gg (unchecked-int x0) iy (unchecked-int x1) (+ iy ih) tint))))
    (when (and icon-rl cut-x0 cut-w (pos? (double cut-w)))
      (GuiGraphicsHelper/blit gg icon-rl
                              (unchecked-int (double cut-x0))
                              (unchecked-int (+ y (double (:y-offset cut 0))))
                              (unchecked-int (double cut-w))
                              (unchecked-int (double (:h cut h)))))))

(defn- bake-shader-node! [^INode node]
  (let [props (.getOSlot node SLOT-SHADER-PROPS)]
    (.setOSlot node SLOT-SHADER-TEX-0
               (resolve-tex-loc (or (:texture-0 props) (:tex-0 props))))
    (.setOSlot node SLOT-SHADER-TEX-1
               (resolve-tex-loc (or (:texture-1 props) (:tex-1 props))))))

(defn- shader-uv
  [props]
  (let [uv (:uv-region props)]
    (if (and (vector? uv) (= 4 (count uv)))
      (mapv double uv)
      [0.0 0.0 1.0 1.0])))

(defn- blit-uv!
  [^GuiGraphicsExtractor gg ^Identifier texture
   x0 y0 x1 y1 u0 v0 u1 v1]
  (when (and texture (< x0 x1) (< y0 y1))
    (GuiGraphicsHelper/blitTexturedQuad
      gg texture
      (float x0) (float y0) (float x1) (float y1) 0.0
      (float u0) (float u1) (float v0) (float v1))))

(defn- render-textured-quad!
  "Draw a baked texture with optional UV cropping. Extractor blits have no
   ColorModulator, so fractional alpha is represented by stable scanline
   dithering instead of being silently ignored."
  [^GuiGraphicsExtractor gg ^INode node ^Identifier texture props]
  (let [x (node-abs-x node) y (node-abs-y node)
        w (scaled-w node) h (scaled-h node)
        quad? (identical? :shader-quad (.getKind node))
        alpha (if quad?
                (max 0.0 (min 1.0 (.getDSlot node SLOT-SQUAD-ALPHA)))
                1.0)
        [u0 v0 u1 v1] (shader-uv props)
        ;; cpbar-overload used Progress as ScrollOffset in the old shader.
        scroll (if (= :cpbar-overload (:shader-id props))
                 (double (.getDSlot node SLOT-SHADER-PROGRESS))
                 0.0)
        u0 (+ u0 scroll)
        u1 (+ u1 scroll)]
    (when (and texture (pos? w) (pos? h) (pos? alpha))
      (if (>= alpha 0.999)
        (blit-uv! gg texture x y (+ x w) (+ y h) u0 v0 u1 v1)
        (let [rows (max 1 (int (Math/ceil h)))
              threshold (* alpha 8.0)]
          (dotimes [row rows]
            ;; 5 is coprime with 8: adjacent rows do not form a solid block.
            (when (< (mod (* row 5) 8) threshold)
              (let [fy0 (/ (double row) rows)
                    fy1 (/ (double (inc row)) rows)]
                (blit-uv! gg texture
                          x (+ y (* h fy0)) (+ x w) (+ y (* h fy1))
                          u0 (+ v0 (* (- v1 v0) fy0))
                          u1 (+ v0 (* (- v1 v0) fy1)))))))))))

(defn- radial-fraction
  "Clockwise angular fraction with zero at twelve o'clock."
  ^double [^double dx ^double dy]
  (let [a (Math/atan2 dx (- dy))]
    (/ (if (neg? a) (+ a (* 2.0 Math/PI)) a) (* 2.0 Math/PI))))

(defn- render-radial-texture!
  "Failure-only fallback when GuiRenderPipelines are unavailable: clip texture-0 into
   horizontal runs. The texture's own alpha keeps only the ring artwork, while
   the run boundary supplies the animated clockwise progress sector."
  [^GuiGraphicsExtractor gg ^INode node ^Identifier texture props]
  (let [x (node-abs-x node) y (node-abs-y node)
        w (scaled-w node) h (scaled-h node)
        progress (max 0.0 (min 1.0
                            (double (.getDSlot node SLOT-SHADER-PROGRESS))))
        [u0 v0 u1 v1] (shader-uv props)
        cols (max 1 (int (Math/ceil w)))
        rows (max 1 (int (Math/ceil h)))
        cx (/ cols 2.0)
        cy (/ rows 2.0)]
    (when (and texture (pos? progress) (pos? w) (pos? h))
      (if (>= progress 0.999)
        (blit-uv! gg texture x y (+ x w) (+ y h) u0 v0 u1 v1)
        (dotimes [row rows]
          (loop [col 0 run-start -1]
            (if (< col cols)
              (let [inside? (< (radial-fraction (- (+ col 0.5) cx)
                                                (- (+ row 0.5) cy))
                               progress)]
                (cond
                  (and inside? (neg? run-start))
                  (recur (inc col) col)

                  (and (not inside?) (not (neg? run-start)))
                  (let [fx0 (/ (double run-start) cols)
                        fx1 (/ (double col) cols)
                        fy0 (/ (double row) rows)
                        fy1 (/ (double (inc row)) rows)]
                    (blit-uv! gg texture
                              (+ x (* w fx0)) (+ y (* h fy0))
                              (+ x (* w fx1)) (+ y (* h fy1))
                              (+ u0 (* (- u1 u0) fx0))
                              (+ v0 (* (- v1 v0) fy0))
                              (+ u0 (* (- u1 u0) fx1))
                              (+ v0 (* (- v1 v0) fy1)))
                    (recur (inc col) -1))

                  :else
                  (recur (inc col) run-start)))
              (when (not (neg? run-start))
                (let [fx0 (/ (double run-start) cols)
                      fy0 (/ (double row) rows)
                      fy1 (/ (double (inc row)) rows)]
                  (blit-uv! gg texture
                            (+ x (* w fx0)) (+ y (* h fy0))
                            (+ x w) (+ y (* h fy1))
                            (+ u0 (* (- u1 u0) fx0))
                            (+ v0 (* (- v1 v0) fy0))
                            u1
                            (+ v0 (* (- v1 v0) fy1))))))))))))

(defn render-shader-progress-node!
  [^GuiGraphicsExtractor gg ^INode node]
  (let [props (or (.getOSlot node SLOT-SHADER-PROPS) {})
        ^Identifier texture-0 (or (.getOSlot node SLOT-SHADER-TEX-0)
                                  (resolve-tex-loc
                                    (or (:texture-0 props) (:tex-0 props))))
        ^Identifier texture-1 (or (.getOSlot node SLOT-SHADER-TEX-1)
                                  (resolve-tex-loc
                                    (or (:texture-1 props) (:tex-1 props))))
        kind (.getKind node)
        shader-id (or (:shader-id props) :ring-progbar)
        radial? (or (identical? kind :shader-ring)
                    (= :ring-progbar shader-id))
        ^RenderPipeline pipeline (resolve-pipeline shader-id)
        two-textures? (contains? #{:ring-progbar :skill-progbar :cpbar-overload}
                                shader-id)
        pipeline-ready? (and pipeline texture-0
                             (or (not two-textures?) texture-1))]
    (if pipeline-ready?
      (let [x (unchecked-int (node-abs-x node))
            y (unchecked-int (node-abs-y node))
            x1 (unchecked-int (+ (node-abs-x node) (scaled-w node)))
            y1 (unchecked-int (+ (node-abs-y node) (scaled-h node)))
            progress (max 0.0 (min 1.0
                                (double (.getDSlot node SLOT-SHADER-PROGRESS))))
            quad? (identical? kind :shader-quad)
            alpha (if quad?
                    (max 0.0 (min 1.0
                               (double (.getDSlot node SLOT-SQUAD-ALPHA))))
                    1.0)
            scroll (mod progress 1.0)
            highlight (max 0.0 (min 1.0
                                 (double (or (:highlight-alpha props) 0.0))))
            red (case shader-id
                  :cpbar-overload (unit-byte scroll)
                  (unit-byte progress))
            green (if (= :cpbar-overload shader-id)
                    (unit-byte highlight)
                    255)
            [u0 v0 u1 v1] (shader-uv props)]
        (GuiGraphicsHelper/blitPipeline
          gg pipeline texture-0 (when two-textures? texture-1)
          x y x1 y1
          (float u0) (float u1) (float v0) (float v1)
          (argb (unit-byte alpha) red green 255)))
      ;; Keep the previous CPU approximations as a resource/pipeline failure
      ;; fallback; successful 26.2 registration no longer takes this branch.
      (if radial?
        (render-radial-texture! gg node texture-0 props)
        (render-textured-quad! gg node texture-0 props)))))

(defn bake-depth-mask! [^INode node]
  (let [src (.getOSlot node SLOT-DM-SRC)]
    (when (string? src)
      (.setOSlot node SLOT-DM-BAKED (resolve-rl src)))))

(defn render-depth-mask!
  "Submit an alpha-discarded, colour-write-disabled depth stamp. Unlike the old
   immediate GL path, 26.2 stores cutoff and layer in vertex colour so the state
   remains valid when the extractor is rendered later."
  [^GuiGraphicsExtractor gg ^INode node]
  (when-let [^Identifier texture (.getOSlot node SLOT-DM-BAKED)]
    (when-let [^RenderPipeline pipeline (resolve-pipeline :alpha-discard)]
      (let [x (unchecked-int (node-abs-x node))
            y (unchecked-int (node-abs-y node))
            x1 (unchecked-int (+ (node-abs-x node) (scaled-w node)))
            y1 (unchecked-int (+ (node-abs-y node) (scaled-h node)))
            cutoff (unit-byte (:alpha-cutoff (.getStaticProps node) 0.3))
            layer (depth-layer-byte node)]
        (GuiGraphicsHelper/blitPipeline
          gg pipeline texture nil x y x1 y1
          0.0 1.0 0.0 1.0
          (argb 255 cutoff layer 255))))))

(defn render-shader-quad! [^GuiGraphicsExtractor gg ^INode node]
  (render-shader-progress-node! gg node))
(defn bake-shader-quad! [^INode node] (bake-shader-node! node))
(defn render-shader-progress! [^GuiGraphicsExtractor gg ^INode node]
  (render-shader-progress-node! gg node))
(defn bake-shader-progress! [^INode node] (bake-shader-node! node))
(defn render-shader-ring! [^GuiGraphicsExtractor gg ^INode node]
  (render-shader-progress-node! gg node))
(defn bake-shader-ring! [^INode node] (bake-shader-node! node))

(def ^:private SLOT-GRAD-ALPHA 0)
(def ^:private SLOT-GRAD-ANGLE 1)
(def ^:private SLOT-GRAD-STOPS 0)
(def ^:private SLOT-GRAD-BAKED 2)

(defn- pack-argb
  "ARGB as an unsigned long, so intermediate colour maths never trips over
   Clojure's signed-int boxing."
  [a r g b]
  (bit-or (bit-shift-left (bit-and (long a) 0xFF) 24)
          (bit-shift-left (bit-and (long r) 0xFF) 16)
          (bit-shift-left (bit-and (long g) 0xFF) 8)
          (bit-and (long b) 0xFF)))

(defn- stop-color->argb
  "A stop colour, either a packed ARGB number or an [r g b a] vector with 0-255
   channels and a 0..1 alpha."
  [c]
  (cond
    (number? c) (bit-and (long c) 0xFFFFFFFF)
    (vector? c) (let [[r g b a] c]
                  (pack-argb (long (* 255.0 (double (or a 1.0))))
                             (long (or r 255)) (long (or g 255)) (long (or b 255))))
    :else 0xFFFFFFFF))

(defn- channel-at [^long c ^long shift]
  (bit-and (bit-shift-right c shift) 0xFF))

(defn- interpolate-argb [^long c0 ^long c1 ^double t]
  (let [lerp (fn [^long shift]
               (let [v0 (double (channel-at c0 shift))
                     v1 (double (channel-at c1 shift))]
                 (long (+ v0 (* (- v1 v0) t)))))]
    (pack-argb (lerp 24) (lerp 16) (lerp 8) (lerp 0))))

(defn- gradient-color-at
  "Colour at position t in [0,1] over an ordered [[pos colour] ...] stop list."
  [stops ^double t]
  (let [n (count stops)
        [p-first c-first] (nth stops 0)
        [p-last c-last] (nth stops (dec n))]
    (cond
      (<= t (double p-first)) (stop-color->argb c-first)
      (>= t (double p-last)) (stop-color->argb c-last)
      :else
      (loop [i 1]
        (if (>= i n)
          (stop-color->argb c-last)
          (let [[p1 c1] (nth stops i)
                p1 (double p1)]
            (if (>= p1 t)
              (let [[p0 c0] (nth stops (dec i))
                    p0 (double p0)
                    span (- p1 p0)
                    f (if (zero? span) 0.0 (/ (- t p0) span))]
                (interpolate-argb (long (stop-color->argb c0))
                                  (long (stop-color->argb c1))
                                  f))
              (recur (inc i)))))))))

(defn- bake-gradient-bands
  [stops ^long n-bands]
  (let [bands (int-array n-bands)
        last-idx (double (max 1 (dec n-bands)))]
    (dotimes [i n-bands]
      (aset bands i (unchecked-int (long (gradient-color-at stops (/ (double i) last-idx))))))
    bands))

(defn bake-gradient!
  "Pre-compute evenly spaced bands so the render path never walks the stop list.
   Resolution follows the node's pixel size, as in 1.21.1."
  [^INode node]
  (when (nil? (.getOSlot node SLOT-GRAD-BAKED))
    (when-let [stops (.getOSlot node SLOT-GRAD-STOPS)]
      (when (and (vector? stops) (seq stops))
        (let [px-w (* (.getW node) (.getCumScale node))
              px-h (* (.getH node) (.getCumScale node))
              n-bands (long (max 16.0 (min 128.0 (max px-w px-h))))]
          (.setOSlot node SLOT-GRAD-BAKED (bake-gradient-bands stops n-bands)))))))

(defn- apply-band-alpha [^long c ^double alpha]
  (if (>= alpha 0.999)
    c
    (pack-argb (long (* alpha (double (channel-at c 24))))
               (channel-at c 16) (channel-at c 8) (channel-at c 0))))

(defn render-gradient!
  "Multi-stop gradient. A vertical ramp maps straight onto fillGradient, whose
   two colours are the top and bottom edges; a horizontal one has no such
   primitive in 26.2, so it is drawn as flat vertical strips at the baked band
   resolution."
  [^GuiGraphicsExtractor gg ^INode node]
  (when-let [bands (.getOSlot node SLOT-GRAD-BAKED)]
    (let [^ints bands bands
          n (alength bands)
          x (node-abs-x node) y (node-abs-y node)
          w (scaled-w node) h (scaled-h node)
          raw-alpha (double (.getDSlot node SLOT-GRAD-ALPHA))
          alpha (if (pos? raw-alpha) (min 1.0 raw-alpha) 1.0)
          angle (double (.getDSlot node SLOT-GRAD-ANGLE))
          horizontal? (< (Math/abs (- angle 90.0)) 45.0)
          segments (dec n)]
      (when (and (pos? segments) (pos? w) (pos? h))
        (if horizontal?
          (let [band-w (/ w (double segments))]
            (dotimes [i segments]
              (GuiGraphicsHelper/fill gg
                     (unchecked-int (+ x (* band-w i)))
                     (unchecked-int y)
                     (unchecked-int (Math/ceil (+ x (* band-w (inc i)))))
                     (unchecked-int (+ y h))
                     (unchecked-int (apply-band-alpha (aget bands i) alpha)))))
          (let [band-h (/ h (double segments))]
            (dotimes [i segments]
              (GuiGraphicsHelper/fillGradient gg
                             (unchecked-int x)
                             (unchecked-int (+ y (* band-h i)))
                             (unchecked-int (+ x w))
                             (unchecked-int (Math/ceil (+ y (* band-h (inc i)))))
                             (unchecked-int (apply-band-alpha (aget bands i) alpha))
                             (unchecked-int (apply-band-alpha (aget bands (inc i)) alpha))))))))))

(defn render-line! [^GuiGraphicsExtractor gg ^INode node]
  (let [scale (node-scale node)
        x1 (+ (node-abs-x node) (* scale (.getDSlot node 0)))
        y1 (+ (node-abs-y node) (* scale (.getDSlot node 1)))
        x2 (+ (node-abs-x node) (* scale (.getDSlot node 2)))
        y2 (+ (node-abs-y node) (* scale (.getDSlot node 3)))
        thick (max 1.0 (* scale (.getDSlot node 4)))
        alpha (.getDSlot node 5)
        a (unchecked-int (* 255.0 (if (pos? alpha) alpha 1.0)))
        raw-color (.getOSlot node 0)
        base-color (if (number? raw-color)
                     (unchecked-int (long raw-color))
                     0xFFFFFFFF)
        color (unchecked-int
                (bit-or (bit-and (long base-color) 0x00FFFFFF)
                        (bit-shift-left (long a) 24)))
        depth-notequal? (= :notequal (:depth-func (.getStaticProps node)))
        ^RenderPipeline pipeline
        (when depth-notequal? (resolve-pipeline :depth-notequal))
        ix1 (unchecked-int (min x1 x2))
        iy1 (unchecked-int (min y1 y2))
        ix2 (unchecked-int (max x1 x2))
        iy2 (unchecked-int (max y1 y2))
        fill! (fn [x0 y0 x3 y3]
                (if pipeline
                  (GuiGraphicsHelper/fillPipeline
                    ^GuiGraphicsExtractor gg ^RenderPipeline pipeline
                    (int x0) (int y0) (int x3) (int y3) (int color))
                  (GuiGraphicsHelper/fill ^GuiGraphicsExtractor gg
                         (int x0) (int y0) (int x3) (int y3) (int color))))]
    ;; Real diagonal quad first (1.21.1 drew the p1→p2 axis quad with ±normal
    ;; offsets sampling the tex-line gradient; the extractor's blits are
    ;; axis-aligned, so blitRotated submits a rotate/scale-pose BlitRenderState
    ;; instead). Falls back to the axis-aligned fills when the loader has no
    ;; element submitter.
    (if (GuiGraphicsHelper/blitRotated
          gg (or (resolve-tex-loc :tex-line) (resolve-tex-loc "tex-line")) color
          (double x1) (double y1) (double x2) (double y2) (double thick))
      nil
      (if (< (Math/abs (- x2 x1)) (Math/abs (- y2 y1)))
        (fill! (unchecked-int (- (/ (+ x1 x2) 2.0) (/ thick 2.0)))
               iy1
               (unchecked-int (+ (/ (+ x1 x2) 2.0) (/ thick 2.0)))
               iy2)
        (fill! ix1
               (unchecked-int (- (/ (+ y1 y2) 2.0) (/ thick 2.0)))
               ix2
               (unchecked-int (+ (/ (+ y1 y2) 2.0) (/ thick 2.0))))))))

(defn bake-nine-slice! [^INode node]
  (let [src (.getOSlot node SLOT-NS-SRC)]
    (when (string? src)
      (.setOSlot node SLOT-NS-BAKED (resolve-rl src))))
  (let [line (.getOSlot node SLOT-NS-LINE)]
    (when (string? line)
      (.setOSlot node SLOT-NS-LINE (resolve-rl line)))))

(defn render-nine-slice! [^GuiGraphicsExtractor gg ^INode node]
  (when-let [^Identifier rl (.getOSlot node SLOT-NS-BAKED)]
    (let [x (unchecked-int (node-abs-x node))
          y (unchecked-int (node-abs-y node))
          w (unchecked-int (scaled-w node))
          h (unchecked-int (scaled-h node))
          m (max 1 (unchecked-int (.getDSlot node SLOT-NS-MARGIN)))]
      ;; blend_quad tint: AcademyCraft draws it with Colors.monoBlend(0, 0.5) =
      ;; black @ 0.5 alpha (a translucent dark panel). White (0xFFFFFFFF) would
      ;; show the raw (light) texture — the "pure white" background bug.
      (GuiGraphicsHelper/blit9 gg rl x y w h 0 0 w h w h m m m m
                               (unchecked-int 0x80000000))
      ;; Top & bottom decorative lines (matching upstream lineTex rendering
      ;; and 1.20.1/1.21.1's render-nine-slice!): drawn at full white.
      (when-let [^Identifier line-tex (.getOSlot node SLOT-NS-LINE)]
        (let [lm 3.2
              lt -8.6 lh 12.0
              lb (- h 2.0) lbh 8.0]
          (GuiGraphicsHelper/blitTexturedQuad gg line-tex
            (float (- x lm)) (float (+ y lt)) (float (+ x w lm)) (float (+ y lt lh)) 0.0
            0.0 1.0 0.0 1.0)
          (GuiGraphicsHelper/blitTexturedQuad gg line-tex
            (float (- x lm)) (float (+ y lb)) (float (+ x w lm)) (float (+ y lb lbh)) 0.0
            0.0 1.0 0.0 1.0))))))

(defn render-glow-line! [^GuiGraphicsExtractor gg ^INode node]
  (render-line! gg node))

(def ^:private SLOT-PREVIEW-ITEM-ID 0)
(def ^:private SLOT-PREVIEW-BAKED 2)
(def ^:private SLOT-P3D-TYPE 0)
(def ^:private SLOT-P3D-BLOCK 1)
(def ^:private SLOT-P3D-ITEM 2)
(def ^:private SLOT-P3D-BAKED 4)
(def ^:private SLOT-P3D-BLOCK-STATE 5)
(def ^:private SLOT-P3D-SPEED 0)
(def ^:private SLOT-P3D-SCALE 1)
(def ^:private SLOT-P3D-YOFF 2)

(defn- item-stack-of
  ^ItemStack [^Identifier id]
  (let [^Item item (.getValue (registry/builtin "ITEM") id)]
    (when item
      (ItemStack. item))))

(defn- block-stack-of
  ^ItemStack [^Identifier id]
  (let [^Block block (.getValue (registry/builtin "BLOCK") id)]
    (when block
      (let [stack (ItemStack. (.asItem block))]
        (when-not (.isEmpty stack) stack)))))

(defn- block-state-of
  "Resolve the default BlockState of a block id (for real 3D block previews)."
  ^BlockState [^Identifier id]
  (let [^Block block (.getValue (registry/builtin "BLOCK") id)]
    (when block
      (.defaultBlockState block))))

(defn- resolve-preview-stack
  "Prefer the registry the caller named, but fall back to the other one: a
   :block view of something registered only as an item (or a block with no item
   form) would otherwise silently render nothing."
  ^ItemStack [id-value block?]
  (when (string? id-value)
    (try
      (let [id (ResourceLocations/parse id-value)]
        (if block?
          (or (block-stack-of id) (item-stack-of id))
          (or (item-stack-of id) (block-stack-of id))))
      (catch Throwable _ nil))))

(defn bake-preview-item! [^INode node]
  (when (nil? (.getOSlot node SLOT-PREVIEW-BAKED))
    (.setOSlot node SLOT-PREVIEW-BAKED
               (resolve-preview-stack (.getOSlot node SLOT-PREVIEW-ITEM-ID) false))))

(defn- turntable-phase
  "Upstream drawBlock spins at (millis / 80) degrees, so a full turn takes ~29s.
   Keeping the same period leaves 26.2 in step with the other versions."
  ^double [^double speed]
  (Math/toRadians (* speed (mod (/ (System/currentTimeMillis) 80.0) 360.0))))

(defn- render-stack!
  "Compatibility renderer used by :preview-item and only as the :preview-3d
   fallback when no PIP renderer was registered by the active loader."
  [^GuiGraphicsExtractor gg ^INode node ^ItemStack stack extra-scale spin y-off]
  (let [x (node-abs-x node)
        y (node-abs-y node)
        w (scaled-w node)
        h (scaled-h node)
        spin (double spin)
        scale (float (* (min (/ w 16.0) (/ h 16.0)) (double extra-scale)))
        ^Matrix3x2fStack pose (.pose gg)]
    (.pushMatrix pose)
    (try
      ;; Items are composited out of vanilla's GUI item atlas, so their quad is
      ;; built inside GuiRenderer where a warp cannot reach. Anchoring on the
      ;; warp's tangent plane at the item's centre still puts it on the panel at
      ;; the right place, scale and shear; only its own 16px extent stays
      ;; unforeshortened, which is far below noticing.
      (let [cx (float (+ x (/ w 2.0)))
            cy (float (+ y (/ h 2.0) (* (double y-off) scale)))]
        (when-not (GuiGraphicsHelper/anchorWarp gg cx cy)
          (.translate pose cx cy)))
      (when (pos? spin)
        (let [phase (turntable-phase spin)
              ;; The default GUI view is corner-on, so face-on is a quarter
              ;; turn away.
              side (+ phase (/ Math/PI 4.0))
              squeeze (/ (+ (Math/abs (Math/cos side)) (Math/abs (Math/sin side)))
                         (Math/sqrt 2.0))
              lean (* 0.08 (Math/sin (* 4.0 phase)))]
          (.mul pose (Matrix3x2f. (float 1.0) (float 0.0) (float lean)
                                  (float 1.0) (float 0.0) (float 0.0)))
          (.scale pose (float squeeze) (float 1.0))))
      (.scale pose scale scale)
      (.fakeItem gg stack -8 -8)
      (finally
        (.popMatrix pose)))))

(defn render-preview-item! [^GuiGraphicsExtractor gg ^INode node]
  (when-let [^ItemStack stack (.getOSlot node SLOT-PREVIEW-BAKED)]
    (render-stack! gg node stack 1.0 0.0 0.0)))

(defn bake-preview-3d! [^INode node]
  (when (nil? (.getOSlot node SLOT-P3D-BAKED))
    (let [block? (= :block (.getOSlot node SLOT-P3D-TYPE))
          id-value (.getOSlot node (if block? SLOT-P3D-BLOCK SLOT-P3D-ITEM))]
      (.setOSlot node SLOT-P3D-BAKED (resolve-preview-stack id-value block?))
      (when block?
        (.setOSlot node SLOT-P3D-BLOCK-STATE
                   (try
                     (block-state-of (ResourceLocations/parse ^String id-value))
                     (catch Throwable _ nil)))))))

(defn render-preview-3d! [^GuiGraphicsExtractor gg ^INode node]
  (when (.isVisible node)
    (let [block? (= :block (.getOSlot node SLOT-P3D-TYPE))
          requested-scale (double (.getDSlot node SLOT-P3D-SCALE))
          model-scale (if (pos? requested-scale) requested-scale 1.0)
          spin (max 0.0 (double (.getDSlot node SLOT-P3D-SPEED)))
          yaw-degrees (Math/toDegrees (turntable-phase spin))
          y-off (double (.getDSlot node SLOT-P3D-YOFF))
          ;; A picture-in-picture is rendered to an offscreen texture and
          ;; composited back axis-aligned, so a warp cannot reach inside it.
          ;; Anchoring on the tangent plane at the node's own corner still
          ;; lands it on the panel at the right place and size.
          ^Matrix3x2fStack pose (.pose gg)
          _ (.pushMatrix pose)
          warped? (GuiGraphicsHelper/anchorWarp gg
                                                (float (node-abs-x node))
                                                (float (node-abs-y node)))
          submitted?
          (try
            (if block?
              (when-let [^BlockState block-state (.getOSlot node SLOT-P3D-BLOCK-STATE)]
                (ReactivePreviewRenderState/submitBlock
                  gg block-state
                  (if warped? 0.0 (node-abs-x node))
                  (if warped? 0.0 (node-abs-y node))
                  (scaled-w node) (scaled-h node)
                  model-scale yaw-degrees y-off))
              (when-let [^ItemStack stack (.getOSlot node SLOT-P3D-BAKED)]
                (ReactivePreviewRenderState/submit
                  gg stack
                  (if warped? 0.0 (node-abs-x node))
                  (if warped? 0.0 (node-abs-y node))
                  (scaled-w node) (scaled-h node)
                  model-scale yaw-degrees y-off)))
            (finally
              (.popMatrix pose)))]
      (when-not submitted?
        (when-let [^ItemStack stack (.getOSlot node SLOT-P3D-BAKED)]
          (render-stack! gg node stack model-scale spin y-off))))))

(def ^:private crosshair-ring-unit-vecs
  (mapv (fn [idx]
          (let [a (/ (* 2.0 Math/PI idx) 24.0)]
            [(Math/cos a) (Math/sin a)]))
        (range 24)))

(defn render-crosshair! [^GuiGraphicsExtractor gg ^INode node]
  (let [cx (unchecked-int (node-abs-x node))
        cy (unchecked-int (node-abs-y node))
        p (double (.getDSlot node 0))
        amp (double (.getDSlot node 1))
        pulse (+ 1.0 (* 0.5 (Math/sin (* 2.0 Math/PI p))))
        radius (+ 11.0 (* 4.0 pulse amp))
        gap (+ 6 (int (* 2.0 pulse amp)))
        len (+ 8 (int (* 2.0 pulse amp)))
        line-color 0xB4E8F8FF
        ring-color 0x88DDF2FF]
    (GuiGraphicsHelper/fill gg (- cx len) (dec cy) (- cx gap) (inc cy) line-color)
    (GuiGraphicsHelper/fill gg (+ cx gap) (dec cy) (+ cx len) (inc cy) line-color)
    (GuiGraphicsHelper/fill gg (dec cx) (- cy len) (inc cx) (- cy gap) line-color)
    (GuiGraphicsHelper/fill gg (dec cx) (+ cy gap) (inc cx) (+ cy len) line-color)
    (doseq [[cos-a sin-a] crosshair-ring-unit-vecs]
      (let [rx (+ cx (int (Math/round (* radius cos-a))))
            ry (+ cy (int (Math/round (* radius sin-a))))]
        (GuiGraphicsHelper/fill gg (dec rx) (dec ry) (inc rx) (inc ry) ring-color)))))

(defn render-embedded-runtime!
  [^GuiGraphicsExtractor gg ^UiRt rt left top w h partial-ticks]
  (when (and rt (not (cn.li.platform.neutral.ui/disposed? rt)))
    (clock/tick! rt partial-ticks)
    (cn.li.platform.neutral.ui/resize! rt (double w) (double h))
    (cn.li.platform.neutral.ui/flush! rt)
    (ui-layout/ensure-layout! rt)
    (ui-layout/ensure-tape! rt)
    (draw-tape! gg rt (int left) (int top))))

(def kind-renderers
  {:box             {:render! render-box!             :bake! nil}
   :image           {:render! render-image!           :bake! bake-image!}
   :text            {:render! render-text!            :bake! bake-text!}
   :progress        {:render! render-progress!        :bake! bake-progress!}
   :depth-mask      {:render! render-depth-mask!      :bake! bake-depth-mask!}
   :shader-quad     {:render! render-shader-quad!     :bake! bake-shader-quad!}
   :shader-ring     {:render! render-shader-ring!     :bake! bake-shader-ring!}
   :shader-progress {:render! render-shader-progress! :bake! bake-shader-progress!}
   :gradient        {:render! render-gradient!        :bake! bake-gradient!}
   :line            {:render! render-line!            :bake! nil}
   :group           {:render! nil                     :bake! nil}
   :list            {:render! nil                     :bake! nil}
   :nine-slice      {:render! render-nine-slice!      :bake! bake-nine-slice!}
   :glow-line       {:render! render-glow-line!       :bake! nil}
   :preview-item    {:render! render-preview-item!    :bake! bake-preview-item!}
   :preview-3d      {:render! render-preview-3d!      :bake! bake-preview-3d!}
   :crosshair       {:render! render-crosshair!       :bake! nil}})

(defn draw-tape!
  "Render the flat tape via GuiGraphicsExtractor + Matrix3x2fStack."
  [^GuiGraphicsExtractor gg ^UiRt rt left top]
  (let [^objects tape (cn.li.platform.neutral.ui/get-tape-arr rt)
        n (alength tape)
        push-clip ui-layout/push-clip-sentinel
        pop-clip  ui-layout/pop-clip-sentinel
        push-xf   ui-layout/push-transform-sentinel
        pop-xf    ui-layout/pop-transform-sentinel]
    (when (pos? n)
      (let [^Matrix3x2fStack pose (.pose gg)]
        (.pushMatrix pose)
        (.translate pose (float left) (float top))
        (loop [i 0]
          (when (< i n)
            (let [entry (aget tape i)]
              (if (instance? INode entry)
                (let [^INode nd entry
                      kdef (get kind-renderers (.getKind nd))]
                  (when (.hasFlag nd node/FLAG-RENDER-DIRTY)
                    (when-let [bake-fn (:bake! kdef)]
                      (bake-fn nd))
                    (.clearFlag nd node/FLAG-RENDER-DIRTY))
                  (when-let [render-fn (:render! kdef)]
                    (render-fn gg nd)))
                (cond
                  (identical? push-clip entry)
                  (do
                    (.pushMatrix pose)
                    ;; Scissor to the following clip-group node's bounds
                    ;; (abs coords, pose already carries left/top + ancestor
                    ;; transforms; ScissorStack intersects nested clips).
                    (when (< (inc i) n)
                      (let [clip-nd (aget tape (inc i))]
                        (when (instance? INode clip-nd)
                          (let [^INode nd clip-nd
                                x0 (int (Math/floor (node-abs-x nd)))
                                y0 (int (Math/floor (node-abs-y nd)))
                                x1 (int (Math/ceil (+ (node-abs-x nd) (scaled-w nd))))
                                y1 (int (Math/ceil (+ (node-abs-y nd) (scaled-h nd))))]
                            (.enableScissor gg x0 y0 x1 y1))))))
                  (identical? pop-clip entry)
                  (do
                    (.disableScissor gg)
                    (.popMatrix pose))
                  (identical? push-xf entry) (.pushMatrix pose)
                  (identical? pop-xf entry)  (.popMatrix pose))))
            (recur (unchecked-inc-int i))))
        (.popMatrix pose)))))

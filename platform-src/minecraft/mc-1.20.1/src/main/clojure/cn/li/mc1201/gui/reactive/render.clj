(ns cn.li.mc1201.gui.reactive.render
  "Kind renderers (:render!/:bake!) — ported from CGUI renderer.clj zero-alloc techniques.
   All render fns take [^GuiGraphics gg ^INode node]."
  (:require [cn.li.mc1201.client.texture-registry :as tex-registry]
            [cn.li.mc1201.gui.cgui.font :as cgui-font]
            [cn.li.mc1201.gui.reactive.clock :as clock]
            [cn.li.platform.neutral.client-runtime :as platform-bridge]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.platform.neutral.ui :as node]
            [cn.li.platform.neutral.ui :as ui-layout]
            [clojure.string :as str]
            [cn.li.mcmod.util.log])
  (:import [cn.li.mc1201.client GuiGraphicsHelper]
           [cn.li.mcmod.ui.node INode]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcver.render ImmediateDraw ImmediateDraw$Mode ImmediateDraw$Format]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.gui GuiGraphics]
           [net.minecraft.client.renderer GameRenderer ShaderInstance]
           [net.minecraft.client.renderer.texture MissingTextureAtlasSprite]
           [net.minecraft.resources ResourceLocation]
           [com.mojang.blaze3d.vertex PoseStack PoseStack$Pose]
           [com.mojang.blaze3d.systems RenderSystem]
           [com.mojang.blaze3d.vertex VertexSorting]
           [org.lwjgl.opengl GL11 GL13]
           [org.joml Matrix4f Quaternionf]))

(declare draw-tape!)

;; Slot indices matching node.clj kind definitions
(def ^:private SLOT-BOX-FILL      0)
(def ^:private SLOT-BOX-OUTLINE   1)
(def ^:private SLOT-BOX-OUTLINE-W 2)
(def ^:private SLOT-BOX-TINT      3)
(def ^:private SLOT-BOX-HOVER     4)

(def ^:private SLOT-IMG-SRC  0)
(def ^:private SLOT-IMG-BAKED-RL 2)  ;; backend slot: resolved ResourceLocation
(def ^:private SLOT-IMG-ALPHA 0)
(def ^:private SLOT-IMG-U     1)
(def ^:private SLOT-IMG-V     2)
(def ^:private SLOT-IMG-TEX-W 3)
(def ^:private SLOT-IMG-TEX-H 4)

(def ^:private SLOT-TEXT-TEXT    0)
(def ^:private SLOT-TEXT-BAKED  8)  ;; backend slot: baked text runs

(def ^:private SLOT-PROG-PROGRESS 0)
(def ^:private SLOT-PROG-BANDS   11)  ;; backend: baked :color-stops vector

(def ^:private SLOT-LINE-X1 0)
(def ^:private SLOT-LINE-Y1 1)
(def ^:private SLOT-LINE-X2 2)
(def ^:private SLOT-LINE-Y2 3)
(def ^:private SLOT-LINE-THICK 4)
(def ^:private SLOT-LINE-ALPHA 5)
(def ^:private SLOT-SHADER-PROPS 0)
(def ^:private SLOT-SHADER-PROGRESS 0)
(def ^:private SLOT-SQUAD-ALPHA 0)   ;; :shader-quad reuses dslot 0 for alpha

(deftype StaticShaderSupplier [^ShaderInstance shader]
  java.util.function.Supplier
  (get [_] shader))

(defn- resolve-tex-loc [tex-key]
  (cond
    (instance? ResourceLocation tex-key) tex-key
    (keyword? tex-key) (tex-registry/resolve-texture tex-key)
    (string? tex-key) (ResourceLocation/tryParse tex-key)
    :else nil))

;; Pre-allocated buffers (zero-alloc render)
(defonce ^:private quad-xs (double-array 4))
(defonce ^:private quad-ys (double-array 4))
(defonce ^:private shared-quat (Quaternionf.))

;; ============================================================================
;; Helpers
;; ============================================================================

(defn- node-abs-x [^INode n] (.getAbsX n))
(defn- node-abs-y [^INode n] (.getAbsY n))
(defn- node-scale [^INode n] (.getCumScale n))

(defn- scaled-w [^INode n] (* (.getW n) (node-scale n)))
(defn- scaled-h [^INode n] (* (.getH n) (node-scale n)))

(defn- argb [^long a ^long r ^long g ^long b]
  (unchecked-int (bit-or (bit-shift-left a 24) (bit-shift-left r 16) (bit-shift-left g 8) b)))

;; ============================================================================
;; Depth layering (opt-in, static props)
;;
;; A node may declare `:depth-z` and `:depth-func` to take part in depth-buffer
;; layering — used by the skill tree to reproduce upstream SkillTree.scala's
;; mask trick: node plates/rings stamp the depth buffer, the icon draws with
;; GL_EQUAL so it is clipped to its plate, and connection lines draw with
;; GL_NOTEQUAL so they are cut where they would cross a node's ring.
;;
;; NOT `:z`: that is the layout layer's paint-order key (layout/flatten-into!
;; sorts each group's children by it), so putting a GL depth there silently
;; reorders the tape — a node icon at :z -2 sorts BEFORE the plate that is
;; supposed to sit behind it, and the plate then paints over the icon.
;;
;; These are static props, not slots: nothing animates them, so they cost one
;; map lookup and no dslot layout churn on the kinds that opt in. Nodes without
;; :depth-func render exactly as before — depth test off, painter's order.
;;
;; The GUI depth buffer is shared with the rest of the screen, so the skill tree
;; stamps NEGATIVE depth-z (behind the z=0 plane every other GUI element draws
;; at). Anything drawn afterwards with the usual LEQUAL test is nearer and still
;; passes; a positive value would silently punch holes in later overlays.
;;
;; The whole scheme rides on the alpha-discard shader that writes the stamps.
;; Where that is not registered — Fabric registers no custom shaders at all —
;; the layering degrades to painter's order for every participant together. A
;; GL_EQUAL test against a depth buffer nothing stamped rejects every fragment,
;; so leaving the test on would silently erase the icons instead.
;; ============================================================================

(defn- depth-func-gl [kw]
  (case kw
    :equal    GL11/GL_EQUAL
    :notequal GL11/GL_NOTEQUAL
    :always   GL11/GL_ALWAYS
    GL11/GL_LEQUAL))

(defn- depth-mask-shader
  "The shader the depth stamps are drawn with, or nil when this loader has none."
  ^ShaderInstance []
  (platform-bridge/resolve-shader :alpha-discard))

(defn- depth-func-of
  "A node's declared depth func, or nil when depth layering is unavailable."
  [^INode node]
  (when-let [f (:depth-func (.getStaticProps node))]
    (when (depth-mask-shader) f)))

(defn- push-depth!
  "Apply a node's declared depth state. Returns its z (0.0 when it declares
   none, which is also the no-depth-test case)."
  [^INode node]
  (let [sp (.getStaticProps node)]
    (when-let [f (depth-func-of node)]
      (RenderSystem/enableDepthTest)
      (RenderSystem/depthFunc (int (depth-func-gl f)))
      (RenderSystem/depthMask (boolean (:depth-write? sp false))))
    (float (double (:depth-z sp 0.0)))))

(defn- pop-depth!
  "Restore the shared default: no depth test, LEQUAL, depth writes on. Every
   depth-aware renderer must call this so the state never escapes the node."
  [^INode node]
  (when (depth-func-of node)
    (RenderSystem/depthMask true)
    (RenderSystem/depthFunc (int GL11/GL_LEQUAL))
    (RenderSystem/disableDepthTest)))

(defn- end-vanilla-draw!
  "Undo the depth state GuiGraphics leaves behind after a batched draw.

   `GuiGraphics.fill` / `fillGradient` / `drawString` flush whenever the
   graphics is unmanaged — which is every Screen — and `GuiGraphics.flush` ends
   with glEnable(GL_DEPTH_TEST), because vanilla's own GUI pass wants the test
   back on. That is the opposite of the default pop-depth! documents above, and
   blits carry no state of their own, so every quad drawn after this one
   inherits whatever we leave behind.

   Ortho hides the damage: coplanar quads land on bit-identical depth and
   LEQUAL passes everywhere. A perspective camera does not — two coplanar quads
   built from different vertices interpolate depths that disagree by float
   rounding, LEQUAL starts rejecting fragments, and the nearer quad tears apart
   and reassembles as the camera moves. cgui.font keeps the same contract on
   its own drawString path."
  []
  (RenderSystem/disableDepthTest))

;; ============================================================================
;; :box
;; ============================================================================

(defn- fill-quad!
  "One ARGB rectangle, drawn immediately in tape order.

   Not GuiGraphics.fill: that queues into RenderType.gui(), and
   BufferSource.endBatch() replays render types by iterating
   fixedBuffers.keySet() -- map order, not submission order -- while text
   flushes on its own with every drawString. A batched fill therefore gets
   re-sorted against text and a :box could never cover text drawn before it
   (the skill detail cover dimmed the art beneath it but left both layers'
   labels bright and overlapping).

   Flushing after each fill would fix the order but throw away the batching
   that made fills cheap, and pay a full endBatch per box on top. Drawing the
   quad ourselves costs one draw call, the same as every other immediate kind
   in this renderer -- gradient, line, nine-slice, glow all work this way."
  [^GuiGraphics gg x0 y0 x1 y1 argb-int]
  (let [a (float (/ (double (bit-and (bit-shift-right (int argb-int) 24) 0xFF)) 255.0))
        r (float (/ (double (bit-and (bit-shift-right (int argb-int) 16) 0xFF)) 255.0))
        g (float (/ (double (bit-and (bit-shift-right (int argb-int) 8) 0xFF)) 255.0))
        b (float (/ (double (bit-and (int argb-int) 0xFF)) 255.0))
        ^PoseStack ps (.pose gg)
        ^Matrix4f pm (.pose (.last ps))]
    (RenderSystem/enableBlend)
    (RenderSystem/defaultBlendFunc)
    (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionColorShader)))
    (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_COLOR)
    (-> (ImmediateDraw/vertex pm (float x0) (float y1) 0.0) (.color r g b a) (.endVertex))
    (-> (ImmediateDraw/vertex pm (float x1) (float y1) 0.0) (.color r g b a) (.endVertex))
    (-> (ImmediateDraw/vertex pm (float x1) (float y0) 0.0) (.color r g b a) (.endVertex))
    (-> (ImmediateDraw/vertex pm (float x0) (float y0) 0.0) (.color r g b a) (.endVertex))
    (ImmediateDraw/draw)))

(defn render-box! [^GuiGraphics gg ^INode node]
  (let [x  (node-abs-x node)  y  (node-abs-y node)
        w  (scaled-w node)    h  (scaled-h node)
        ix (unchecked-int x)  iy (unchecked-int y)
        iw (unchecked-int (+ x w))  ih (unchecked-int (+ y h))]
    ;; Fill. Go via long, not a direct double->int cast: fills are stored as
    ;; doubles and Java's double->int SATURATES at Integer.MAX_VALUE, so any
    ;; ARGB with alpha >= 0x80 (value > 2^31) would clamp to 0x7FFFFFFF (faint
    ;; white). double->long->int wraps correctly and preserves the ARGB bits.
    (let [fill-argb (unchecked-int (long (.getDSlot node SLOT-BOX-FILL)))]
      (when (not= fill-argb 0)
        (fill-quad! gg ix iy iw ih fill-argb)))
    ;; Outline (same double->long->int wrap as fill above)
    (let [outline-argb (unchecked-int (long (.getDSlot node SLOT-BOX-OUTLINE)))
          outline-w    (.getDSlot node SLOT-BOX-OUTLINE-W)]
      (when (and (not= outline-argb 0) (> outline-w 0.0))
        (fill-quad! gg ix iy iw (unchecked-int (+ iy outline-w)) outline-argb)
        (fill-quad! gg ix (unchecked-int (- ih outline-w)) iw ih outline-argb)
        (fill-quad! gg ix iy (unchecked-int (+ ix outline-w)) ih outline-argb)
        (fill-quad! gg (unchecked-int (- iw outline-w)) iy iw ih outline-argb)))
    ;; Tint overlay — both :tint and :hover-tint prop-writers store
    ;; raw ARGB doubles; extract the alpha byte so (* 255.0 raw) does
    ;; not overflow (0x33FFFFFF = 855M → alpha*255 overflows to garbage).
    (let [hovered? (.hasFlag node node/FLAG-HOVERED)
          raw (if hovered?
                        (let [ht (.getDSlot node SLOT-BOX-HOVER)]
                          (if (> ht 0.0) ht (.getDSlot node SLOT-BOX-TINT)))
                        (.getDSlot node SLOT-BOX-TINT))
          alpha (if (> raw 1.0)
                  ;; ARGB packed value → decode the alpha channel only
                  (unchecked-int (bit-and (bit-shift-right (long raw) 24) 0xFF))
                  (unchecked-int (* 255.0 raw)))]
      (when (pos? alpha)
        (fill-quad! gg ix iy iw ih (argb alpha 255 255 255))))
    (end-vanilla-draw!)))

(defn bake-box! [^INode _node] nil)

;; ============================================================================
;; :image
;; ============================================================================

(defn- resolve-rl
  "Parse a GUI-texture path string into a ResourceLocation. MC's SimpleTexture
   uses the RL verbatim as the resource file path, so it must include the
   '.png' extension. GUI textures are always .png; tolerate paths written
   without it (e.g. \"academy:textures/guis/blend_quad\") by appending it. Paths
   that already end in .png (image :src values) are left unchanged."
  ^ResourceLocation [src]
  (when (and src (string? src) (not (clojure.string/blank? src)))
    (let [src (if (clojure.string/ends-with? src ".png") src (str src ".png"))]
      (ResourceLocation/tryParse src))))

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

;; ---------------------------------------------------------------------------
;; A GUI texture that fails to resolve draws as a flat untextured quad and
;; nothing else says so, which reads as a renderer bug rather than a bad path.
;; Report each offending location once.
;; ---------------------------------------------------------------------------

;; Tagged so the .add below resolves statically — this runs per image node on
;; the render path, where a reflective call would allocate and box every frame.
(defonce ^:private ^java.util.HashSet seen-image-textures (java.util.HashSet.))

(defn- warn-missing-texture!
  [^INode node ^ResourceLocation rl]
  (when (.add seen-image-textures rl)
    (when (identical? (.getTexture (.getTextureManager (Minecraft/getInstance)) rl)
                      (MissingTextureAtlasSprite/getTexture))
      (cn.li.mcmod.util.log/warn
        "UI image texture did not resolve: " (str rl)
        " (node " (str (.getId node)) ") — draws as a blank quad"))))

(defn render-image! [^GuiGraphics gg ^INode node]
  (let [rl-obj (.getOSlot node SLOT-IMG-BAKED-RL)
        ;; Fallback: if bake-image! never ran (FLAG-RENDER-DIRTY wasn't set or
        ;; the node wasn't in the tape when the flag was checked), SLOT-IMG-BAKED-RL
        ;; is nil even though SLOT-IMG-SRC has a valid string. Resolve inline so
        ;; the image draws rather than silently disappearing.
        ^ResourceLocation rl (if (instance? ResourceLocation rl-obj)
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
            depth? (some? (depth-func-of node))
            [tr tg tb ta] (image-tint-rgba node)]
        (when (pos? alpha)
          ;; Enable blend for the alpha channel — otherwise a texture with
          ;; transparency (white glyph icons, the faint element_background) renders
          ;; opaque, filling its quad with solid RGB. :text and :nine-slice already
          ;; do this; images previously relied on whatever blend state the prior
          ;; tape node happened to leave, so alpha rendering was order-dependent.
          (RenderSystem/enableBlend)
          (RenderSystem/defaultBlendFunc)
          (RenderSystem/setShaderColor tr tg tb (* alpha ta))
          (if (and (zero? u) (zero? v) (== tex-w 1.0) (== tex-h 1.0) (not depth?))
            ;; Fast path: no sprite-sheet cropping requested — full-texture blit.
            ;; (GuiGraphics.blit pins z at 0, so a depth-layered image can't use it.)
            (.blit gg rl ix iy iw ih 0.0 0.0 iw ih iw ih)
            ;; Cropped path: sample a [u,v]→[u+tex-w,v+tex-h] fractional UV region
            ;; (matching the old CGUI comp/render-texture-region convention), e.g.
            ;; a single frame out of a vertically-stacked sprite-sheet. Pass `gg`
            ;; so the quad is transformed by the current PoseStack (draw-tape's
            ;; leftPos/topPos translate) instead of rendering at raw coordinates.
            (let [z (push-depth! node)]
              (GuiGraphicsHelper/blitTexturedQuad gg rl
                (float x) (float y) (float (+ x w)) (float (+ y h)) z
                (float u) (float (+ u tex-w))
                (float v) (float (+ v tex-h)))
              (pop-depth! node)))
          (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0))))))

;; ============================================================================
;; :text (MSDF font)
;; ============================================================================

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
  ;; text kind slots (node.clj): dslots {:font-size 0 :x-offset 1 :y-offset 2}
  ;; oslots {:text 0 :color 1 :font 2 :align 3}
  (let [text (display-text node (or (.getOSlot node SLOT-TEXT-TEXT) ""))
        font-size-raw (.getDSlot node 0)
        font-size (if (pos? font-size-raw) font-size-raw 14.0)
        color-raw (.getOSlot node 1)
        raw-long (if (number? color-raw) (long color-raw) 0xFFFFFFFF)
        ;; normalize-color-int forces argb-alpha-mask (0xFF) — extract the
        ;; per-node alpha first, then splice it back into the normalized color
        ;; so the MSDF shader (which reads the color param, not the global
        ;; shader color) receives it.
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

(defn render-text! [^GuiGraphics gg ^INode node]
  (let [baked (.getOSlot node SLOT-TEXT-BAKED)]
    (when baked
      (let [{:keys [text color font-size font-desc align x-off y-off]} baked
            editable? (boolean (get (.getStaticProps node) :editable?))
            focused? (.hasFlag node node/FLAG-FOCUSED)
            align-kw (or align :left)
            ;; font-size / text offsets are authored in local design units (same as
            ;; w/h). Images/boxes already multiply by cum-scale; text must too or
            ;; scaled roots (terminal fit-scale, skill-tree camera) draw oversized
            ;; glyphs that ignore the panel shrink.
            s (node-scale node)
            font-size (* (double font-size) s)
            x-off (* (double x-off) s)
            y-off (* (double y-off) s)
            ;; Vertical alignment: MSDF text draws with y as the em-box top, so a
            ;; node with align-h center/bottom otherwise renders top-aligned in its
            ;; box (reads as "too low" for a tall font in a short row). Offset y by
            ;; the em-box within the node's height to honor align-h (0 top / 1
            ;; middle / 2 bottom), matching how images/boxes already center.
            node-h (* (.getH node) s)
            v-off (case (int (.getAlignH node))
                    1 (/ (- node-h (double font-size)) 2.0)
                    2 (- node-h (double font-size))
                    0.0)
            ;; Horizontal alignment: draw-text!'s `align` treats x as the anchor
            ;; (left→text start, center→text center, right→text end). x here is the
            ;; node's LEFT edge, so center/right must anchor at the box center/right
            ;; edge — otherwise centered/right text draws off the node box. Matches
            ;; upstream TextBox.option.align (independent of the node's align-w).
            node-w (* (.getW node) s)
            h-off (case align-kw :center (/ node-w 2.0) :right node-w 0.0)
            x (+ (node-abs-x node) x-off h-off)
            y (+ (node-abs-y node) y-off v-off)]
        ;; Draw the text (masked if masked? already handled by display-text in bake-text!)
        (cgui-font/draw-text! gg (or font-desc {}) ^String text
                              x y font-size color align-kw true)
        ;; Blinking caret cursor | at the end of text (matching AcademyCraft TextBox:
        ;; font.draw("|", origin.x + sumLength(display, 0, localCaret), origin.y-1, option)
        ;; when w.isFocused() && allowEdit && GameTimer.getAbsTime() % 2 < 1).
        ;; Reactive text doesn't have a caret-position system yet, so the cursor
        ;; sits at the end of the text (caretPos == content.length). Blink alternates
        ;; each second, matching upstream's getAbsTime() % 2.
        (when (and editable? focused?
                   ;; Blink: 300ms on, 300ms off (faster than AcademyCraft's 1s cycle)
                   (< (rem (System/currentTimeMillis) 600) 300))
          (let [text-w (cgui-font/text-width (or font-desc {}) ^String text font-size)
                ;; cursor-x follows alignment (like upstream's origin.x + sumLength)
                cursor-x (case align-kw
                           :center (+ x (/ text-w 2.0))
                           :right  x
                           (+ x text-w))]
            (cgui-font/draw-text! gg (or font-desc {}) "|"
                                  cursor-x (- y 1.0) font-size color :left false)))))))

;; ============================================================================
;; :progress
;; ============================================================================

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
  (get (.getStaticProps node) :icon-cutout))

(defn- sample-progress-color
  "Multi-stop [pos r g b] color lerp — matches upstream CPBar.autoLerp. nil
   bands (no :color-stops set, e.g. the overload bar) means \"no tint\":
   passthrough white, so the fg texture's own baked colors show through."
  [bands ^double t]
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


(defn render-progress! [^GuiGraphics gg ^INode node]
  (let [x        (node-abs-x node)   y        (node-abs-y node)
        w        (scaled-w node)     h        (scaled-h node)
        percent  (double (.getDSlot node SLOT-PROG-PROGRESS))
        diag-tan (double (.getDSlot node 1))            ;; :corner dslot (0=rect, 0.695=44deg)
        scroll   (.getDSlot node 3)                      ;; :scroll-offset
        raw-alpha (.getDSlot node 4)                     ;; :alpha (0.0 = unset → default 1.0)
        fill-alpha (if (pos? raw-alpha) raw-alpha 1.0)
        ;; :fill-remap [base span] — upstream CPBar.drawCPBar does
        ;; prog = base + prog*span (0.16 + prog*0.8) so the CP fill keeps a
        ;; 16% minimum sliver and tops out at 96% (never reaching the diagonal
        ;; tip). This remaps only the fill EXTENT; the gradient color below still
        ;; samples the RAW percent (upstream calls autoLerp BEFORE this remap).
        remap    (get (.getStaticProps node) :fill-remap)
        uv-region (get (.getStaticProps node) :uv-region)
        u0       (double (if uv-region (nth uv-region 0) 0.0))
        v0       (double (if uv-region (nth uv-region 1) 0.0))
        u1       (double (if uv-region (nth uv-region 2) 1.0))
        v1       (double (if uv-region (nth uv-region 3) 1.0))
        fill-pct (if remap
                   (+ (double (nth remap 0)) (* percent (double (nth remap 1))))
                   percent)
        ;; :anchor :right — upstream CPBar fixes the bar's RIGHT edge (at the
        ;; icon side) and grows the fill leftward; default is left-anchored.
        anchor-right? (= :right (get (.getStaticProps node) :anchor))
        filled-w (int (* fill-pct w))
        ix (unchecked-int x) iy (unchecked-int y)
        iw (unchecked-int w) ih (unchecked-int (+ y h))
        ^ResourceLocation bg-rl    (.getOSlot node 8)
        ^ResourceLocation fg-rl    (.getOSlot node 9)
        ^ResourceLocation icon-rl  (.getOSlot node 10)   ;; baked icon
        bands      (.getOSlot node SLOT-PROG-BANDS)
        cutout     (icon-cutout node)
        cutout-x0  (when cutout (+ ix (int (:x-offset cutout 0))))
        cutout-w   (when cutout (int (:w cutout 0)))
        cutout-x1  (when cutout (+ cutout-x0 cutout-w))
        cutout-y0  (when cutout (+ iy (int (or (:y-offset cutout 0) 0))))
        cutout-h   (when cutout (int (or (:h cutout 0) (- ih iy))))

        ;; Draw a textured trapezoid via ImmediateDraw (1 draw call, zero alloc).
        ;; When diag-tan=0 draws a plain rectangle; when >0 the left edge tilts
        ;; inward such that the bottom-left corner is offset right by diag-tan*h.
        draw-trap!
        (fn draw-trap! ([seg-start seg-end]
                         (draw-trap! seg-start seg-end 255 255 255 1.0))
                        ([seg-start seg-end r g b a]
          (when (< seg-start seg-end)
            (let [apply-color! #(RenderSystem/setShaderColor
                                   (float (/ (double r) 255.0))
                                   (float (/ (double g) 255.0))
                                   (float (/ (double b) 255.0))
                                   (float (or a 1.0)))]
            (if (or (pos? diag-tan) uv-region)
              (let [^PoseStack ps (.pose gg)
                    ^Matrix4f pose (.pose (.last ps))
                    x0   (float seg-start)
                    x1   (float seg-end)
                    y0   (float iy)
                    y1   (float ih)
                    x0b  (float (+ seg-start (* diag-tan h)))  ;; bottom-left offset
                    u-at (fn [^double sx]
                           (float (+ u0 scroll
                                     (* (/ (- sx (double ix)) (double iw))
                                        (- u1 u0)))))]
                (RenderSystem/setShaderTexture 0 fg-rl)
                (RenderSystem/setShader GameRenderer/getPositionTexShader)
                (RenderSystem/enableBlend)
                (RenderSystem/defaultBlendFunc)
                (apply-color!)  ;; AFTER setShader
                (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
                (-> (ImmediateDraw/vertex pose x0  y0 0.0)  (.uv (u-at x0)  (float v0))  (.endVertex))
                (-> (ImmediateDraw/vertex pose x0b y1 0.0)  (.uv (u-at x0b) (float v1))  (.endVertex))
                (-> (ImmediateDraw/vertex pose x1  y1 0.0)  (.uv (u-at x1)  (float v1))  (.endVertex))
                (-> (ImmediateDraw/vertex pose x1  y0 0.0)  (.uv (u-at x1)  (float v0))  (.endVertex))
                (ImmediateDraw/draw))
              ;; Rectangular fill: existing scissor+blit path
              (let [uoff (float (* scroll iw))
                    hh (unchecked-int h)         ;; node HEIGHT — ih is the bottom edge (y+h)
                    fw (- (int seg-end) (int seg-start))]
                ;; Clip by UV, not scissor: GuiGraphics.enableScissor takes
                ;; absolute SCREEN coords, but node x/y are GUI-local — on a
                ;; centered container the bar fell outside the scissor rect
                ;; and vanished. Blit only the filled width (u 0..fw/iw).
                (apply-color!)
                (.blit gg fg-rl ix iy uoff 0.0 fw hh (float iw) (float hh))))))))]
    ;; ---- background ----
    (when bg-rl
      (.blit gg bg-rl ix iy 0 0 iw (unchecked-int h) iw (unchecked-int h)))
    ;; ---- upstream autoLerp: multi-stop color lerp (bands nil → white/no tint) ----
    (when (and fg-rl (pos? filled-w))
      (let [t       (double (max 0.0 (min 1.0 percent)))
            [r g b] (sample-progress-color bands t)]
        (if anchor-right?
          ;; Right-anchored (upstream drawCPBar): fixed right edge = node right,
          ;; fill grows leftward. Draw ONE trapezoid all the way to the right edge
          ;; (under the icon, which the icon overlay below redraws on top) so the
          ;; diagonal never degenerates on the thin sliver beside the icon.
          (let [bar-right (int (+ x w))
                bar-start (int (- bar-right filled-w))]
            (draw-trap! (max ix bar-start) bar-right r g b fill-alpha))
          (let [bar-end (int (+ x filled-w))
                ;; Absolute right edge of the node — `iw` is the node WIDTH
                ;; (relative), and the trap segments are absolute screen x.
                ;; (min bar-end iw) clipped a 90%-full bar to ~6px.
                node-right (int (+ x w))]
            (if (and cutout-x0 cutout-w (pos? cutout-w))
              (do (draw-trap! ix       (min bar-end cutout-x0) r g b fill-alpha)
                  (draw-trap! cutout-x1 (min bar-end node-right) r g b fill-alpha))
              (draw-trap! ix (min bar-end node-right) r g b fill-alpha))))))
    ;; ---- icon overlay ----
    (when (and cutout-x0 cutout-w (pos? cutout-w) icon-rl)
      (.blit gg icon-rl cutout-x0 cutout-y0 0 0 cutout-w cutout-h cutout-w cutout-h))
    ;; fill-alpha can be <1.0 (e.g. the CP-bar consumption-hint ghost bar) —
    ;; reset so a subsequent tape node never inherits a translucent tint.
    (when (< fill-alpha 1.0)
      (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0))))

;; ============================================================================
;; :shader-quad / :shader-ring / :shader-progress
;; ============================================================================

(defn- render-shader-progress-node! [^GuiGraphics gg ^INode node]
  (let [props (.getOSlot node SLOT-SHADER-PROPS)
        ;; :shader-quad has no :progress — dslot 0 is its alpha (node.clj).
        quad? (identical? :shader-quad (.getKind node))
        progress (float (if quad? 0.0 (.getDSlot node SLOT-SHADER-PROGRESS)))
        alpha (float (if quad?
                       (max 0.0 (min 1.0 (.getDSlot node SLOT-SQUAD-ALPHA)))
                       1.0))
        shader-id (or (:shader-id props) :ring-progbar)
        ^ResourceLocation tex-0 (resolve-tex-loc (or (:texture-0 props) (:tex-0 props)))
        ^ResourceLocation tex-1 (resolve-tex-loc (or (:texture-1 props) (:tex-1 props)))
        ^ShaderInstance si (platform-bridge/resolve-shader shader-id)
        uv-region (:uv-region props)
        u0 (float (if uv-region (nth uv-region 0) 0.0))
        v0 (float (if uv-region (nth uv-region 1) 0.0))
        u1 (float (if uv-region (nth uv-region 2) 1.0))
        v1 (float (if uv-region (nth uv-region 3) 1.0))]
    (when tex-0
      (try
        ;; tex-1 is optional — single-sampler shaders (mono) are as valid as the
        ;; two-sampler ring-progbar; only the shader itself is required.
        (if si
          (do
            ;; Samplers are named Sampler0/Sampler1 exactly as vanilla core
            ;; shaders are, so ShaderInstance.apply() fills them from the
            ;; RenderSystem texture slots set below — no manual setSampler.
            (RenderSystem/setShader (StaticShaderSupplier. si))
            (when-let [u (.safeGetUniform si "Progress")] (.set u progress))
            (when-let [u (.safeGetUniform si "ScrollOffset")] (.set u progress))
            (when-let [u (.safeGetUniform si "HighlightAlpha")]
              (.set u (float (or (:highlight-alpha props) 0.0)))))
          (do
            ;; Loader without this optional shader: retain the full overload
            ;; texture instead of dropping the state visual entirely.
            (RenderSystem/setShaderTexture 0 tex-0)
            (RenderSystem/setShader GameRenderer/getPositionTexShader)))
        (RenderSystem/enableBlend)
        (RenderSystem/defaultBlendFunc)
        ;; ColorModulator — read by ShaderInstance.apply() inside ImmediateDraw/draw.
        ;; Set unconditionally so the quad never inherits the previous node's tint.
        (RenderSystem/setShaderColor 1.0 1.0 1.0 alpha)
        (let [x (float (node-abs-x node)) y (float (node-abs-y node))
              x2 (float (+ (node-abs-x node) (scaled-w node)))
              y2 (float (+ (node-abs-y node) (scaled-h node)))
              ^PoseStack ps (.pose gg)
              ^PoseStack$Pose entry (.last ps)
              ^Matrix4f pose-matrix (.pose entry)]
          (RenderSystem/setShaderTexture (int 0) tex-0)
          (when tex-1 (RenderSystem/setShaderTexture (int 1) tex-1))
          (let [z (push-depth! node)]
            (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
            (-> (ImmediateDraw/vertex pose-matrix x y2 z) (.uv u0 v1) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix x2 y2 z) (.uv u1 v1) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix x2 y z) (.uv u1 v0) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix x y z) (.uv u0 v0) (.endVertex))
            (ImmediateDraw/draw)
            (pop-depth! node)))
        ;; ShaderInstance.apply() walks its samplers with activeTexture(GL_TEXTUREi)
        ;; and never puts the unit back, so a two-sampler shader (ring-progbar)
        ;; leaves unit 1 selected. The next AbstractTexture.bind() — notably the
        ;; MSDF glyph atlas — then lands on the wrong unit and Sampler0 keeps
        ;; whatever stale texture was there, which renders text as solid boxes.
        ;; Upstream restores it explicitly (SkillTree.scala: glActiveTexture(
        ;; GL_TEXTURE0) after the progress-bar draw).
        (RenderSystem/activeTexture GL13/GL_TEXTURE0)
        (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)
        ;; Release the custom program, but leave a *usable* shader bound: a nil
        ;; current shader NPEs the next ImmediateDraw node (draw dereferences it),
        ;; and every skill-tree node ends with this ring.
        (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionTexShader)))
        (catch Exception e
          (cn.li.mcmod.util.log/stacktrace "shader render failed" e))))))

;; ============================================================================
;; :depth-mask — colour-less depth stamp (upstream SkillTree.scala's
;; glColorMask(false…) + glAlphaFunc pass))
;; ============================================================================

(def ^:private SLOT-DM-SRC 0)
(def ^:private SLOT-DM-BAKED 1)

(defn bake-depth-mask! [^INode node]
  (let [src (.getOSlot node SLOT-DM-SRC)]
    (when (string? src)
      (.setOSlot node SLOT-DM-BAKED (resolve-rl src)))))

(defn render-depth-mask! [^GuiGraphics gg ^INode node]
  (let [^ResourceLocation rl (.getOSlot node SLOT-DM-BAKED)
        ^ShaderInstance si (depth-mask-shader)]
    ;; No alpha-discard shader on this loader → skip the stamp entirely rather
    ;; than write a full opaque quad. Layering degrades to painter's order.
    (when (and rl si)
      (try
        (let [sp (.getStaticProps node)
              z (float (double (:depth-z sp 0.0)))
              cutoff (float (double (:alpha-cutoff sp 0.3)))
              x (float (node-abs-x node)) y (float (node-abs-y node))
              x2 (float (+ (node-abs-x node) (scaled-w node)))
              y2 (float (+ (node-abs-y node) (scaled-h node)))
              ^PoseStack ps (.pose gg)
              ^Matrix4f pose-matrix (.pose (.last ps))]
          (RenderSystem/setShader (StaticShaderSupplier. si))
          (when-let [u (.safeGetUniform si "AlphaThreshold")] (.set u cutoff))
          (RenderSystem/setShaderTexture (int 0) rl)
          ;; GL_ALWAYS, not LEQUAL: the stamp sits behind the z=0 plane the rest
          ;; of the GUI (and its own depth writes) occupy, so a depth-ordered
          ;; test would reject it. Equality against these values is all the
          ;; icon/line passes care about.
          (RenderSystem/enableDepthTest)
          (RenderSystem/depthFunc (int GL11/GL_ALWAYS))
          (RenderSystem/depthMask true)
          (RenderSystem/colorMask false false false false)
          (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
          (-> (ImmediateDraw/vertex pose-matrix x y2 z) (.uv 0.0 1.0) (.endVertex))
          (-> (ImmediateDraw/vertex pose-matrix x2 y2 z) (.uv 1.0 1.0) (.endVertex))
          (-> (ImmediateDraw/vertex pose-matrix x2 y z) (.uv 1.0 0.0) (.endVertex))
          (-> (ImmediateDraw/vertex pose-matrix x y z) (.uv 0.0 0.0) (.endVertex))
          (ImmediateDraw/draw)
          (RenderSystem/colorMask true true true true)
          (RenderSystem/depthMask true)
          (RenderSystem/depthFunc (int GL11/GL_LEQUAL))
          (RenderSystem/disableDepthTest)
          (RenderSystem/activeTexture GL13/GL_TEXTURE0)
          (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionTexShader))))
        (catch Exception e
          ;; Never leave the colour mask off — a leaked one blanks the screen.
          (RenderSystem/colorMask true true true true)
          (RenderSystem/depthMask true)
          (RenderSystem/depthFunc (int GL11/GL_LEQUAL))
          (RenderSystem/disableDepthTest)
          (RenderSystem/activeTexture GL13/GL_TEXTURE0)
          (cn.li.mcmod.util.log/stacktrace "depth mask render failed" e))))))

(defn render-shader-quad! [^GuiGraphics gg ^INode node]
  (render-shader-progress-node! gg node))
(defn bake-shader-quad! [^INode _node] nil)
(def render-shader-ring! render-shader-quad!)
(def bake-shader-ring! bake-shader-quad!)
(defn render-shader-progress! [^GuiGraphics gg ^INode node]
  (render-shader-progress-node! gg node))
(defn bake-shader-progress! [^INode _node] nil)

;; ============================================================================
;; :gradient — pre-compute stops into baked bands, render via ImmediateDraw strips
;; ============================================================================

(def ^:private SLOT-GRAD-ALPHA 0)
(def ^:private SLOT-GRAD-ANGLE 1)
(def ^:private SLOT-GRAD-STOPS  0)    ;; oslot: vector of [pos color] pairs
(def ^:private SLOT-GRAD-BAKED  2)    ;; backend oslot: baked int-array bands

(defn- interpolate-color [c0 c1 t]
  "Interpolate between two ARGB int colors. t in [0,1]."
  (let [a0 (bit-and (bit-shift-right c0 24) 0xFF)
        r0 (bit-and (bit-shift-right c0 16) 0xFF)
        g0 (bit-and (bit-shift-right c0 8) 0xFF)
        b0 (bit-and c0 0xFF)
        a1 (bit-and (bit-shift-right c1 24) 0xFF)
        r1 (bit-and (bit-shift-right c1 16) 0xFF)
        g1 (bit-and (bit-shift-right c1 8) 0xFF)
        b1 (bit-and c1 0xFF)
        a (int (+ (double a0) (* (- (double a1) (double a0)) t)))
        r (int (+ (double r0) (* (- (double r1) (double r0)) t)))
        g (int (+ (double g0) (* (- (double g1) (double g0)) t)))
        b (int (+ (double b0) (* (- (double b1) (double b0)) t)))]
    (bit-or (bit-shift-left a 24) (bit-shift-left r 16) (bit-shift-left g 8) b)))

(defn- gradient-color-at [stops pos]
  "Get the interpolated ARGB color at position [0,1] from an ordered stops vector."
  (let [n (count stops)]
    (if (zero? n)
      0x00000000
      (let [first-stop (nth stops 0) last-stop (nth stops (dec n))]
        (if (<= pos (double (first first-stop)))
          (int (second first-stop))
          (if (>= pos (double (first last-stop)))
            (int (second last-stop))
            (loop [i 0]
              (let [s0 (nth stops i) s1 (nth stops (inc i))
                    p0 (double (first s0)) p1 (double (first s1))]
                (if (and (>= pos p0) (<= pos p1))
                  (let [t (/ (- pos p0) (- p1 p0))]
                    (interpolate-color (int (second s0)) (int (second s1)) t))
                  (if (< (inc i) (dec n))
                    (recur (inc i))
                    0x00000000))))))))))

(defn- bake-gradient-bands [stops n-bands]
  "Pre-compute n-bands evenly-spaced ARGB colors from stops."
  (let [bands (int-array n-bands)]
    (dotimes [i n-bands]
      (let [pos (/ (double i) (double (dec n-bands)))]
        (aset bands i (int (gradient-color-at stops pos)))))
    bands))

(defn bake-gradient! [^INode node]
  (when (nil? (.getOSlot node SLOT-GRAD-BAKED))
    (when-let [stops (.getOSlot node SLOT-GRAD-STOPS)]
      (when (and (vector? stops) (seq stops))
        (let [w (.getW node) h (.getH node)
              scale (.getCumScale node)
              pixel-w (* w scale) pixel-h (* h scale)
              n-bands (int (max 16 (min 128 (max pixel-w pixel-h))))]
          (.setOSlot node SLOT-GRAD-BAKED (bake-gradient-bands stops n-bands)))))))

(defn render-gradient! [^GuiGraphics gg ^INode node]
  (let [bands (.getOSlot node SLOT-GRAD-BAKED)]
    (when bands
      (let [n (alength ^ints bands)
            x (node-abs-x node) y (node-abs-y node)
            w (scaled-w node) h (scaled-h node)
            raw-alpha (.getDSlot node SLOT-GRAD-ALPHA)
            alpha (if (pos? (double raw-alpha)) (max 0.0 (min 1.0 (double raw-alpha))) 1.0)
            angle (double (.getDSlot node SLOT-GRAD-ANGLE))
            horizontal? (< (Math/abs (- angle 90.0)) 45.0)
            ^PoseStack ps (.pose gg)
            ^PoseStack$Pose entry (.last ps)
            ^Matrix4f pose-matrix (.pose entry)]
        (RenderSystem/enableBlend)
        (RenderSystem/defaultBlendFunc)
        ;; POSITION_COLOR buffer → bind position_color explicitly (ImmediateDraw/draw
        ;; otherwise reuses the previous node's shader).
        (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionColorShader)))
        (if horizontal?
          ;; Vertical bands for horizontal-ish gradient
          (let [band-w (/ (double w) (double (max 1 (dec n))))]
            (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_COLOR)
            (dotimes [i (dec n)]
              (let [x0 (+ x (* band-w (double i))) x1 (+ x0 band-w)
                    c0 (aget ^ints bands i) c1 (aget ^ints bands (inc i))
                    a0 (float (* alpha (/ (double (bit-and (bit-shift-right c0 24) 0xFF)) 255.0)))
                    r0 (float (/ (double (bit-and (bit-shift-right c0 16) 0xFF)) 255.0))
                    g0 (float (/ (double (bit-and (bit-shift-right c0 8) 0xFF)) 255.0))
                    b0 (float (/ (double (bit-and c0 0xFF)) 255.0))
                    a1 (float (* alpha (/ (double (bit-and (bit-shift-right c1 24) 0xFF)) 255.0)))
                    r1 (float (/ (double (bit-and (bit-shift-right c1 16) 0xFF)) 255.0))
                    g1 (float (/ (double (bit-and (bit-shift-right c1 8) 0xFF)) 255.0))
                    b1 (float (/ (double (bit-and c1 0xFF)) 255.0))]
                (-> (ImmediateDraw/vertex pose-matrix (float x0) (float y) 0.0) (.color r0 g0 b0 a0) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float x1) (float y) 0.0) (.color r1 g1 b1 a1) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float x1) (float (+ y h)) 0.0) (.color r1 g1 b1 a1) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float x0) (float (+ y h)) 0.0) (.color r0 g0 b0 a0) (.endVertex))))
            (ImmediateDraw/draw))
          ;; Horizontal bands for vertical-ish gradient
          (let [band-h (/ (double h) (double (max 1 (dec n))))]
            (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_COLOR)
            (dotimes [i (dec n)]
              (let [y0 (+ y (* band-h (double i))) y1 (+ y0 band-h)
                    c0 (aget ^ints bands i) c1 (aget ^ints bands (inc i))
                    a0 (float (* alpha (/ (double (bit-and (bit-shift-right c0 24) 0xFF)) 255.0)))
                    r0 (float (/ (double (bit-and (bit-shift-right c0 16) 0xFF)) 255.0))
                    g0 (float (/ (double (bit-and (bit-shift-right c0 8) 0xFF)) 255.0))
                    b0 (float (/ (double (bit-and c0 0xFF)) 255.0))
                    a1 (float (* alpha (/ (double (bit-and (bit-shift-right c1 24) 0xFF)) 255.0)))
                    r1 (float (/ (double (bit-and (bit-shift-right c1 16) 0xFF)) 255.0))
                    g1 (float (/ (double (bit-and (bit-shift-right c1 8) 0xFF)) 255.0))
                    b1 (float (/ (double (bit-and c1 0xFF)) 255.0))]
                (-> (ImmediateDraw/vertex pose-matrix (float x) (float y0) 0.0) (.color r0 g0 b0 a0) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float (+ x w)) (float y0) 0.0) (.color r0 g0 b0 a0) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float (+ x w)) (float y1) 0.0) (.color r1 g1 b1 a1) (.endVertex))
                (-> (ImmediateDraw/vertex pose-matrix (float x) (float y1) 0.0) (.color r1 g1 b1 a1) (.endVertex))))
            (ImmediateDraw/draw)))))))

;; ============================================================================
;; :line
;; ============================================================================

(defn render-line! [^GuiGraphics gg ^INode node]
  ;; Endpoints and thickness are authored in the parent's local units, so they
  ;; scale with it — a :line under a scaled camera group (the skill tree fits its
  ;; tree into the panel area at ~0.68x) otherwise draws at raw coordinates and
  ;; raw width, landing off its nodes and reading much thicker than the art
  ;; around it. Everything else in the tree goes through layout's cum-scale.
  (let [s (node-scale node)
        x1 (+ (node-abs-x node) (* s (.getDSlot node SLOT-LINE-X1)))
        y1 (+ (node-abs-y node) (* s (.getDSlot node SLOT-LINE-Y1)))
        x2 (+ (node-abs-x node) (* s (.getDSlot node SLOT-LINE-X2)))
        y2 (+ (node-abs-y node) (* s (.getDSlot node SLOT-LINE-Y2)))
        line-w (double (max 1.0 (* s (.getDSlot node SLOT-LINE-THICK))))
        alpha (double (max 0.0 (min 1.0 (.getDSlot node SLOT-LINE-ALPHA))))
        color-raw (.getOSlot node 0)
        color-int (if (number? color-raw) (unchecked-int (long color-raw)) 0xFFFFFFFF)
        ^ResourceLocation tex (or (resolve-tex-loc :tex-line) (resolve-tex-loc "tex-line"))
        dx (- x2 x1) dy (- y2 y1)
        norm (Math/sqrt (+ (* dx dx) (* dy dy)))]
    (when (and tex (> norm 0.5) (> alpha 0.0))
      (try
        ;; Quad straddles the p1→p2 axis by half the width on each side
        ;; (upstream ACRenderingHelper drawLine). The endpoints themselves are
        ;; not nudged — offsetting them along the normal, as an earlier version
        ;; did, pushes the whole line half its width off-axis.
        (let [half-w (/ line-w 2.0)
              nx (* (/ (- dy) norm) half-w) ny (* (/ dx norm) half-w)
              ca (float (* alpha (/ (double (bit-and (bit-shift-right color-int 24) 0xFF)) 255.0)))
              ^PoseStack ps (.pose gg)
              ^PoseStack$Pose entry (.last ps)
              ^Matrix4f pose-matrix (.pose entry)]
          (RenderSystem/enableBlend)
          (RenderSystem/defaultBlendFunc)
          ;; Bind position_tex explicitly — see render-nine-slice!: ImmediateDraw/draw
          ;; reuses whatever shader was last set, and the tree background / text
          ;; nodes drawn just before leave a different one active.
          (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionTexShader)))
          (RenderSystem/setShaderColor 1.0 1.0 1.0 ca)
          (RenderSystem/setShaderTexture (int 0) tex)
          (let [z (push-depth! node)]
            (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
            (-> (ImmediateDraw/vertex pose-matrix (float (- x1 nx)) (float (- y1 ny)) z) (.uv 0.0 0.0) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix (float (+ x1 nx)) (float (+ y1 ny)) z) (.uv 0.0 1.0) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix (float (+ x2 nx)) (float (+ y2 ny)) z) (.uv 1.0 1.0) (.endVertex))
            (-> (ImmediateDraw/vertex pose-matrix (float (- x2 nx)) (float (- y2 ny)) z) (.uv 1.0 0.0) (.endVertex))
            (ImmediateDraw/draw)
            (pop-depth! node))
          (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0))
        (catch Exception e
          (cn.li.mcmod.util.log/stacktrace "line render failed" e))))))
(defn bake-line! [^INode _node] nil)

;; ============================================================================
;; :group — PoseStack push/pop (handled by tape sentinels, no-op here)
;; ============================================================================

(defn render-group! [^GuiGraphics _gg ^INode _node] nil)
(defn bake-group! [^INode _node] nil)

;; ============================================================================
;; :list — parent of template items (no-op, children render individually)
;; ============================================================================

(defn render-list! [^GuiGraphics _gg ^INode _node] nil)
(defn bake-list! [^INode _node] nil)

;; ============================================================================
;; :nine-slice — 9-slice textured background (port of AcademyCraft BlendQuad)
;; ============================================================================

(def ^:private SLOT-NS-SRC 0)
(def ^:private SLOT-NS-LINE 1)
(def ^:private SLOT-NS-BAKED 2)  ;; oslot index 2 = backend slot (base 2)
(def ^:private SLOT-NS-MARGIN 0)

(defn bake-nine-slice! [^INode node]
  (let [src (.getOSlot node SLOT-NS-SRC)]
    (when (string? src)
      (.setOSlot node SLOT-NS-BAKED (resolve-rl src))))
  (let [line (.getOSlot node SLOT-NS-LINE)]
    (when (string? line)
      (.setOSlot node SLOT-NS-LINE (resolve-rl line)))))

(defn- render-nine-slice-quad!
  "Render a single textured quad via ImmediateDraw (matching upstream glBegin/glEnd)."
  [^Matrix4f pose-matrix
   x0 y0 x1 y1 u0 v0 u1 v1]
  (-> (ImmediateDraw/vertex pose-matrix (float x0) (float y1) 0.0) (.uv (float u0) (float v1)) (.endVertex))
  (-> (ImmediateDraw/vertex pose-matrix (float x1) (float y1) 0.0) (.uv (float u1) (float v1)) (.endVertex))
  (-> (ImmediateDraw/vertex pose-matrix (float x1) (float y0) 0.0) (.uv (float u1) (float v0)) (.endVertex))
  (-> (ImmediateDraw/vertex pose-matrix (float x0) (float y0) 0.0) (.uv (float u0) (float v0)) (.endVertex)))

(defn render-nine-slice! [^GuiGraphics gg ^INode node]
  (let [^ResourceLocation tex (.getOSlot node SLOT-NS-BAKED)]
    (when tex
      (let [raw-margin (.getDSlot node SLOT-NS-MARGIN)
            margin (max 1.0 (if (pos? raw-margin) raw-margin 4.0))
            x  (node-abs-x node)  y  (node-abs-y node)
            w  (scaled-w node)    h  (scaled-h node)
            ;; 3x3 grid UVs: each tile is 1/3 of texture
            step  (/ 1.0 3.0)
            ;; Destination: x coords [x-margin, x, x+w, x+w+margin]
            d-xs [(double (- x margin)) (double x) (double (+ x w)) (double (+ x w margin))]
            d-ys [(double (- y margin)) (double y) (double (+ y h)) (double (+ y h margin))]
            ^PoseStack ps (.pose gg)
            ^PoseStack$Pose entry (.last ps)
            ^Matrix4f pose-matrix (.pose entry)]
        (RenderSystem/enableBlend)
        (RenderSystem/defaultBlendFunc)
        ;; Bind the position_tex shader explicitly: ImmediateDraw/draw
        ;; uses the *currently set* shader, and text/other nodes rendered earlier
        ;; leave a different one active — without this the quad draws untextured
        ;; (pure white). Also reset the shader color so we're not tinted.
        (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionTexShader)))
        ;; blend_quad tint: AcademyCraft draws it with Colors.monoBlend(0, 0.5) =
        ;; black @ 0.5 alpha (a translucent dark panel). White would show the raw
        ;; (light) texture — that's the "pure white" background.
        (RenderSystem/setShaderColor 0.0 0.0 0.0 0.5)
        (RenderSystem/setShaderTexture 0 tex)
        (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
        (dotimes [i 3]
          (dotimes [j 3]
            (let [u0 (* i step)  u1 (+ u0 step)
                  v0 (* j step)  v1 (+ v0 step)]
              (render-nine-slice-quad! pose-matrix
                (d-xs i) (d-ys j) (d-xs (inc i)) (d-ys (inc j))
                u0 v0 u1 v1))))
        (ImmediateDraw/draw)
        ;; Top & bottom decorative lines (matching upstream lineTex rendering)
        (when-let [^ResourceLocation line-tex (.getOSlot node SLOT-NS-LINE)]
          (let [lm 3.2
                lt -8.6  lh 12.0
                lb (- h 2.0)  lbh 8.0]
            ;; lines drawn at full white (upstream resets glColor4d(1,1,1,1))
            (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)
            (RenderSystem/setShaderTexture 0 line-tex)
            (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
            ;; Top line
            (render-nine-slice-quad! pose-matrix
              (- x lm) (+ y lt) (+ x w lm) (+ y lt lh) 0.0 0.0 1.0 1.0)
            ;; Bottom line
            (render-nine-slice-quad! pose-matrix
              (- x lm) (+ y lb) (+ x w lm) (+ y lb lbh) 0.0 0.0 1.0 1.0)
            (ImmediateDraw/draw)))
        (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)))))

;; ============================================================================
;; :glow-line — matches upstream ACRenderingHelper.drawGlow + lineSegment
;; ============================================================================

(def ^:private SLOT-GL-X0 0) (def ^:private SLOT-GL-X1 1)
(def ^:private SLOT-GL-Y 2) (def ^:private SLOT-GL-LINEW 3)
(def ^:private SLOT-GL-GLOWSZ 4) (def ^:private SLOT-GL-TINT 5)
(def ^:private SLOT-GL-NOCENTER 6)

(defn- glow-line-tint-rgba
  "Decode the packed-ARGB :tint dslot. Untouched (0.0, the Node ctor's
   double-array default) means no caller ever set a color — keep the
   pre-existing behavior of drawing at ambient/ whatever shader color the
   prior tape node left (opaque white by convention)."
  [^INode node]
  (let [raw (long (.getDSlot node SLOT-GL-TINT))]
    (if (zero? raw)
      [1.0 1.0 1.0 1.0]
      (let [argb (unchecked-int raw)]
        [(float (/ (double (bit-and (bit-shift-right argb 16) 0xFF)) 255.0))
         (float (/ (double (bit-and (bit-shift-right argb 8) 0xFF)) 255.0))
         (float (/ (double (bit-and argb 0xFF)) 255.0))
         (float (/ (double (bit-and (bit-shift-right argb 24) 0xFF)) 255.0))]))))

(defn- bake-glow-line! [^INode node]
  (when (nil? (.getOSlot node 0))
    (.setOSlot node 0
                {:lu (resolve-rl (modid/asset-path "textures" "guis/glow_lu"))
                 :ru (resolve-rl (modid/asset-path "textures" "guis/glow_ru"))
                 :ld (resolve-rl (modid/asset-path "textures" "guis/glow_ld"))
                 :rd (resolve-rl (modid/asset-path "textures" "guis/glow_rd"))
                 :l  (resolve-rl (modid/asset-path "textures" "guis/glow_left"))
                 :r  (resolve-rl (modid/asset-path "textures" "guis/glow_right"))
                 :u  (resolve-rl (modid/asset-path "textures" "guis/glow_up"))
                 :d  (resolve-rl (modid/asset-path "textures" "guis/glow_down"))})))

(defn- glow-quad!
  "Emit one POSITION_TEX quad into the open ImmediateDraw mesh."
  [^Matrix4f pm x0 y0 x1 y1 u0 v0 u1 v1]
  (-> (ImmediateDraw/vertex pm (float x0) (float y1) 0.0) (.uv (float u0) (float v1)) (.endVertex))
  (-> (ImmediateDraw/vertex pm (float x1) (float y1) 0.0) (.uv (float u1) (float v1)) (.endVertex))
  (-> (ImmediateDraw/vertex pm (float x1) (float y0) 0.0) (.uv (float u1) (float v0)) (.endVertex))
  (-> (ImmediateDraw/vertex pm (float x0) (float y0) 0.0) (.uv (float u0) (float v0)) (.endVertex)))

(defn- glow-textured-quad!
  "One textured glow segment: bind texture then begin/draw (no mid-batch rebind)."
  [^Matrix4f pm ^ResourceLocation tex x0 y0 x1 y1]
  (RenderSystem/setShaderTexture 0 tex)
  (ImmediateDraw/begin ImmediateDraw$Mode/QUADS ImmediateDraw$Format/POSITION_TEX)
  (glow-quad! pm x0 y0 x1 y1 0.0 0.0 1.0 1.0)
  (ImmediateDraw/draw))

(defn render-glow-line! [^GuiGraphics gg ^INode node]
  (when (.isVisible node)
    (let [x0 (node-abs-x node)  y0 (node-abs-y node)
          gx0 (+ x0 (.getDSlot node SLOT-GL-X0))
          gx1 (+ x0 (.getDSlot node SLOT-GL-X1))
          gy  (+ y0 (.getDSlot node SLOT-GL-Y))
          line-w (max 1.0 (.getDSlot node SLOT-GL-LINEW))
          glow-sz (max 1.0 (.getDSlot node SLOT-GL-GLOWSZ))
          hw (/ line-w 2.0)
          texs (.getOSlot node 0)]
      (when texs
        (let [s (double glow-sz)
              glx0 (- gx0 s) glx1 (+ gx1 s)
              gly0 (- gy s)  gly1 (+ gy s)
              gy0 (- gy hw)  gy1 (+ gy hw)
              [tr tg tb ta] (glow-line-tint-rgba node)
              no-center? (not (zero? (.getDSlot node SLOT-GL-NOCENTER)))
              ^PoseStack ps (.pose gg)
              ^Matrix4f pm (.pose (.last ps))]
          (RenderSystem/enableBlend)
          (RenderSystem/defaultBlendFunc)
          (RenderSystem/setShader (StaticShaderSupplier. (GameRenderer/getPositionTexShader)))
          (RenderSystem/setShaderColor tr tg tb ta)
          ;; corners — separate begin/draw per texture (ImmediateDraw forbids mid-batch rebind)
          (glow-textured-quad! pm ^ResourceLocation (:lu texs) glx0 gly0 gx0 gy0)
          (glow-textured-quad! pm ^ResourceLocation (:ru texs) gx1 gly0 glx1 gy0)
          (glow-textured-quad! pm ^ResourceLocation (:ld texs) glx0 gy1 gx0 gly1)
          (glow-textured-quad! pm ^ResourceLocation (:rd texs) gx1 gy1 glx1 gly1)
          ;; edges
          (glow-textured-quad! pm ^ResourceLocation (:l texs) glx0 gy0 gx0 gy1)
          (glow-textured-quad! pm ^ResourceLocation (:r texs) gx1 gy0 glx1 gy1)
          (glow-textured-quad! pm ^ResourceLocation (:u texs) gx0 gly0 gx1 gy0)
          (glow-textured-quad! pm ^ResourceLocation (:d texs) gx0 gy1 gx1 gly1)
          ;; center bright line (ACRenderingHelper.lineSegmentGlow only —
          ;; drawGlow-style box glow, e.g. the skill-slot icon border, sets
          ;; :no-center to skip this so only the 8 edge/corner segments show)
          (when-not no-center?
            (glow-textured-quad! pm
              (resolve-rl (modid/asset-path "textures" "guis/line"))
              gx0 (- gy hw) gx1 (+ gy hw)))
          (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0))))))

;; ============================================================================
;; :preview-item — renders an ItemStack scaled to fill the node area
;; ============================================================================

(def ^:private SLOT-PREVIEW-ITEM-ID 0)
(def ^:private SLOT-PREVIEW-BAKED  2)  ;; backend oslot: ItemStack

(defn bake-preview-item! [^INode node]
  (when (nil? (.getOSlot node SLOT-PREVIEW-BAKED))
    (when-let [item-id-val (.getOSlot node SLOT-PREVIEW-ITEM-ID)]
      (when (string? item-id-val)
        (try
          (let [item-id (str item-id-val)
                rl (if (str/includes? item-id ":")
                     (let [parts (str/split item-id #":" 2)]
                       (ResourceLocation. (first parts) (second parts)))
                     (ResourceLocation. item-id))
                ^net.minecraft.world.item.Item item
                (.get net.minecraft.core.registries.BuiltInRegistries/ITEM rl)]
            (if (and item (not= item net.minecraft.world.item.Items/AIR))
              (.setOSlot node SLOT-PREVIEW-BAKED (net.minecraft.world.item.ItemStack. item))
              ;; Try block registry (for :block-3d views where block may not have a direct item)
              (let [^net.minecraft.world.level.block.Block block
                    (.get net.minecraft.core.registries.BuiltInRegistries/BLOCK rl)]
                (when (and block (not= block net.minecraft.world.level.block.Blocks/AIR))
                  (.setOSlot node SLOT-PREVIEW-BAKED
                    (net.minecraft.world.item.ItemStack. (.asItem block)))))))
          (catch Throwable _
            nil))))))

(defn render-preview-item! [^GuiGraphics gg ^INode node]
  (when-let [^net.minecraft.world.item.ItemStack stack (.getOSlot node SLOT-PREVIEW-BAKED)]
    (let [x (node-abs-x node) y (node-abs-y node)
          w (scaled-w node) h (scaled-h node)
          item-size 16.0
          scale-x (/ w item-size) scale-y (/ h item-size)
          scale (min scale-x scale-y)
          cx (+ x (/ w 2.0)) cy (+ y (/ h 2.0))
          ^PoseStack ps (.pose gg)]
      (.pushPose ps)
      (.translate ps cx cy 0.0)
      (.scale ps (float scale) (float scale) 1.0)
      (.renderFakeItem gg stack -8 -8)
      (.popPose ps))))

;; ============================================================================
;; :preview-3d — perspective 3D preview matching upstream AcademyCraft
;; Uses Minecraft's RenderSystem/PoseStack instead of raw GL.
;; ============================================================================

(def ^:private SLOT-P3D-TYPE  0)  ;; oslot: :block or :item
(def ^:private SLOT-P3D-BLOCK 1)  ;; oslot: block id string
(def ^:private SLOT-P3D-ITEM  2)  ;; oslot: item id string
(def ^:private SLOT-P3D-BAKED 4)  ;; backend: resolved ItemStack
(def ^:private SLOT-P3D-SPEED 0)  ;; dslot: rotation speed (1=blocks, 0=items)
(def ^:private SLOT-P3D-SCALE 1)  ;; dslot: uniform scale
(def ^:private SLOT-P3D-YOFF  2)  ;; dslot: Y offset

(defn bake-preview-3d! [^INode node]
  (when (nil? (.getOSlot node SLOT-P3D-BAKED))
    (let [rtype (.getOSlot node SLOT-P3D-TYPE)
          id-str (if (= rtype :block)
                   (.getOSlot node SLOT-P3D-BLOCK)
                   (.getOSlot node SLOT-P3D-ITEM))]
      (when (string? id-str)
        (try
          (let [rl (ResourceLocation/tryParse ^String id-str)
                ^net.minecraft.world.item.Item item
                (.get net.minecraft.core.registries.BuiltInRegistries/ITEM rl)]
            (if (and item (not= item net.minecraft.world.item.Items/AIR))
              (.setOSlot node SLOT-P3D-BAKED (net.minecraft.world.item.ItemStack. item))
              ;; Try block registry fallback
              (let [^net.minecraft.world.level.block.Block block
                    (.get net.minecraft.core.registries.BuiltInRegistries/BLOCK rl)]
                (when (and block (not= block net.minecraft.world.level.block.Blocks/AIR))
                  (.setOSlot node SLOT-P3D-BAKED
                    (net.minecraft.world.item.ItemStack. (.asItem block)))))))
          (catch Throwable _ nil))))))

(defn render-preview-3d! [^GuiGraphics gg ^INode node]
  (when (.isVisible node)
    (when-let [^net.minecraft.world.item.ItemStack stack (.getOSlot node SLOT-P3D-BAKED)]
      (let [rot-speed (.getDSlot node SLOT-P3D-SPEED)
            uni-scale (let [s (.getDSlot node SLOT-P3D-SCALE)] (if (pos? s) s 1.0))
            rtype (.getOSlot node SLOT-P3D-TYPE)
            x (node-abs-x node) y (node-abs-y node)
            w (scaled-w node) h (scaled-h node)
            ^net.minecraft.client.Minecraft mc (net.minecraft.client.Minecraft/getInstance)
            ^com.mojang.blaze3d.platform.Window win (.getWindow mc)
            gui-scale (.getGuiScale win)
            fb-w (.getWidth win)
            fb-h (.getHeight win)
            ;; Real GL viewport scoped to this node's screen rect (in physical
            ;; framebuffer pixels — GL viewport Y is bottom-up, GUI Y is top-down)
            ;; instead of trying to fold the node's absolute screen-pixel
            ;; position into the modelview translate: that mixed pixel-scale
            ;; XY with a near=1/far=100 perspective frustum, and — combined
            ;; with a -200 Z translate that sits *beyond* the far plane — made
            ;; the block get clipped away entirely (this is why it rendered as
            ;; nothing). A real viewport lets the camera use upstream's own
            ;; small eye-space distances and puts the near/far clip range back
            ;; where the object actually is.
            vp-x (int (Math/round (* x gui-scale)))
            vp-w (int (max 1 (Math/round (* w gui-scale))))
            vp-h (int (max 1 (Math/round (* h gui-scale))))
            vp-y (int (Math/round (- fb-h (* (+ y h) gui-scale))))
            aspect (float (if (pos? h) (/ w h) 1.0))
            ;; Upstream builds its projection as glScaled(scale, -scale*aspect, -0.5)
            ;; *followed by* gluPerspective(50,1,1,100), i.e. P = S · Perspective.
            ;; The viewport above already does S's x/y magnitude job, but not its
            ;; two sign flips, and both of those carry meaning:
            ;;   -y  cancels the modelview's own scale(.75,-.75,.75) so screen-space
            ;;       winding comes out unreversed and back-face culling keeps the
            ;;       cube's front faces instead of its far ones;
            ;;   -0.5 remaps clip z so the block lands at depth ~0.36. Without it it
            ;;       sits at ~0.77, while every 2D GUI quad drawn before it wrote
            ;;       0.5 (the ortho GUI pass puts pose z=0 exactly mid-range), so
            ;;       LEQUAL rejected every fragment — the block was submitted and
            ;;       flushed correctly, just never visible.
            persp (-> (Matrix4f.)
                      (.scaling (float 1.0) (float -1.0) (float -0.5))
                      (.perspective (float (Math/toRadians 50.0)) aspect
                                    (float 1.0) (float 100.0)))
            ;; Use RenderSystem modelview stack (separate from GuiGraphics PoseStack)
            ^PoseStack mv (RenderSystem/getModelViewStack)
            saved-proj (RenderSystem/getProjectionMatrix)]
        ;; Flush any pending 2D GUI geometry queued under the normal ortho
        ;; projection before switching state for this 3D preview.
        (.flush gg)
        (RenderSystem/viewport vp-x vp-y vp-w vp-h)
        (RenderSystem/setProjectionMatrix persp VertexSorting/DISTANCE_TO_ORIGIN)
        ;; Modelview: upstream's TWO-stage transform. GuiTutorial.showArea's
        ;; outer camera (shared by every preview widget): translate(0,0,-4),
        ;; translate(.55,.55,.5), scale(.75,-.75,.75), fixed glRotated(-20,1,0,0.1)
        ;; tilt — wraps ViewGroups.drawsBlockImpl's own inner transform: local
        ;; offset (0.15,0.1,-1), Y-spin, scale .8, recenter (-.5,-.5,-.5). The
        ;; viewport above now stands in for the NDC remap upstream builds into
        ;; its projection matrix, so the camera chain below is otherwise 1:1.
        (.pushPose mv)
        ;; The GUI pass seeds this stack with translate(0, 0, 1000 - guiFarPlane)
        ;; = -10000 to suit vanilla's ortho depth range. Left in place it sits
        ;; ~10000 beyond the far plane of the near=1/far=100 frustum above and
        ;; the model is clipped away entirely, so start the camera at the eye.
        (.setIdentity mv)
        (.translate mv 0.0 0.0 -4.0)
        (.translate mv 0.55 0.55 0.5)
        (.scale mv 0.75 -0.75 0.75)
        (.mulPose mv (doto (Quaternionf.)
                       (.rotateAxis (float (Math/toRadians -20.0)) 1.0 0.0 0.1)))
        (.translate mv 0.15 0.1 -1.0)  ;; upstream inner glTranslated(0.15,0.1,-1) pre-rotation offset
        ;; Auto-rotation Y (matching upstream drawBlock time/80°)
        (when (pos? rot-speed)
          (.mulPose mv (doto (Quaternionf.)
                         (.rotateY (float (Math/toRadians
                                            (mod (/ (System/currentTimeMillis) 80.0) 360.0)))))))
        (.scale mv (float uni-scale) (float uni-scale) (float uni-scale))
        (.translate mv -0.5 -0.5 -0.5)  ;; center block/item on all three axes
        (RenderSystem/applyModelViewMatrix)
        ;; Render the block/item with the 3D-aware renderer
        (if (= rtype :block)
          (let [^net.minecraft.client.renderer.block.BlockRenderDispatcher brd
                (.getBlockRenderer mc)
                block-id (.getOSlot node SLOT-P3D-BLOCK)]
            (when (string? block-id)
              (if-let [^net.minecraft.world.level.block.Block block
                       (try (.get net.minecraft.core.registries.BuiltInRegistries/BLOCK
                                  (ResourceLocation/tryParse ^String block-id))
                            (catch Throwable _ nil))]
                (let [state (.defaultBlockState block)
                      buffer (.bufferSource gg)]
                  (RenderSystem/enableDepthTest)
                  (.renderSingleBlock brd state (.pose gg) buffer 15728880
                                     net.minecraft.client.renderer.texture.OverlayTexture/NO_OVERLAY)
                  ;; renderSingleBlock only QUEUES vertices into gg's buffered
                  ;; MultiBufferSource — without flushing here, they'd draw
                  ;; later under whatever projection/viewport is active by
                  ;; then (already restored to the normal 2D GUI ortho), i.e.
                  ;; never under the perspective camera set up above. This
                  ;; missing flush was the actual reason the block never
                  ;; appeared at all.
                  (.flush gg)
                  (RenderSystem/disableDepthTest))
                  nil)))
          ;; :item — GuiGraphics.renderItem bakes translate(0,0,150) + scale(16,-16,16)
          ;; into gg's own pose, sized and offset for vanilla's ortho GUI. Under
          ;; this perspective camera the 150 lands the model far behind the eye
          ;; (the frustum only reaches 100), so undo both here — upstream's
          ;; drawsItemImpl does the same with its own glScaled(-1/16,-1/16,1).
          ;; Cancelling renderItem's -16 also leaves winding matching the block
          ;; path, so the projection's -y flip keeps the item upright.
          (let [^PoseStack ps (.pose gg)]
            (.pushPose ps)
            (.scale ps (float (/ 1.0 16.0)) (float (/ -1.0 16.0)) (float (/ 1.0 16.0)))
            (.translate ps 0.0 0.0 -150.0)
            (.renderFakeItem gg stack -8 -8)
            (.flush gg)
            (.popPose ps)))
        ;; Restore
        (.popPose mv)
        (RenderSystem/applyModelViewMatrix)
        (RenderSystem/setProjectionMatrix saved-proj VertexSorting/DISTANCE_TO_ORIGIN)
        (RenderSystem/viewport 0 0 fb-w fb-h)))))

;; ============================================================================
;; :crosshair
;; ============================================================================

;; ============================================================================
;; :crosshair
;; ============================================================================

(def ^:private crosshair-ring-unit-vecs
  "Precomputed [cos sin] for the 24-point ring — the reflection crosshair only
  scales this fixed unit circle by `radius` each frame; no need to recompute
  Math/cos + Math/sin 24x every frame it's active."
  (mapv (fn [idx]
          (let [a (/ (* 2.0 Math/PI idx) 24.0)]
            [(Math/cos a) (Math/sin a)]))
        (range 24)))

(defn render-crosshair! [^GuiGraphics gg ^INode node]
  (let [cx (unchecked-int (node-abs-x node))
        cy (unchecked-int (node-abs-y node))
        p (double (.getDSlot node 0))
        amp (max 0.0 (min 1.0 (double (.getDSlot node 1))))
        pulse (+ 1.0 (* 0.5 (Math/sin (* 2.0 Math/PI p))))
        radius (+ 11.0 (* 4.0 pulse amp))
        gap (int (* 2.0 pulse amp))
        len (int (* 2.0 pulse amp))
        ;; ARGB literals above 0x7FFFFFFF are longs in Clojure; GuiGraphics.fill
        ;; takes an int color, and the interop cast (toIntExact) overflowed —
        ;; unchecked-int reproduces the signed-ARGB bit pattern the original
        ;; Java call passes.
        line-color (unchecked-int 0xB4E8F8FF)
        ring-color (unchecked-int 0x88DDF2FF)]
    (.fill gg (- cx len) (dec cy) (- cx gap) (inc cy) line-color)
    (.fill gg (+ cx gap) (dec cy) (+ cx len) (inc cy) line-color)
    (.fill gg (dec cx) (- cy len) (inc cx) (- cy gap) line-color)
    (.fill gg (dec cx) (+ cy gap) (inc cx) (+ cy len) line-color)
    (doseq [[cos-a sin-a] crosshair-ring-unit-vecs]
      (let [rx (unchecked-int (+ cx (Math/round (* radius cos-a))))
            ry (unchecked-int (+ cy (Math/round (* radius sin-a))))]
        (.fill gg (dec rx) (dec ry) (inc rx) (inc ry) ring-color)))
    (end-vanilla-draw!)))

(defn bake-crosshair! [^INode _node] nil)

(defn render-embedded-runtime!
  "Render a UiRt embedded at screen offset (CGUI widget host)."
  [^GuiGraphics gg ^UiRt rt left top w h partial-ticks]
  (when (and rt (not (cn.li.platform.neutral.ui/disposed? rt)))
    (clock/tick! rt partial-ticks)
    (cn.li.platform.neutral.ui/resize! rt (double w) (double h))
    (cn.li.platform.neutral.ui/flush! rt)
    (ui-layout/ensure-layout! rt)
    (ui-layout/ensure-tape! rt)
    (draw-tape! gg rt (int left) (int top))))

;; ============================================================================
;; Kind fn-map (installed via install-adapter! into [:platform :ui-kinds])
;; ============================================================================

(def kind-renderers
  {:box             {:render! render-box!             :bake! bake-box!}
   :image           {:render! render-image!           :bake! bake-image!}
   :text            {:render! render-text!            :bake! bake-text!}
   :progress        {:render! render-progress!        :bake! bake-progress!}
   :depth-mask      {:render! render-depth-mask!      :bake! bake-depth-mask!}
   :shader-quad     {:render! render-shader-quad!     :bake! bake-shader-quad!}
   :shader-ring     {:render! render-shader-ring!     :bake! bake-shader-ring!}
   :shader-progress {:render! render-shader-progress! :bake! bake-shader-progress!}
   :gradient        {:render! render-gradient!        :bake! bake-gradient!}
   :line            {:render! render-line!            :bake! bake-line!}
   :group           {:render! render-group!           :bake! bake-group!}
   :list            {:render! render-list!            :bake! bake-list!}
   :nine-slice      {:render! render-nine-slice!      :bake! bake-nine-slice!}
   :glow-line       {:render! render-glow-line!       :bake! bake-glow-line!}
   :preview-item    {:render! render-preview-item!    :bake! bake-preview-item!}
   :preview-3d      {:render! render-preview-3d!      :bake! bake-preview-3d!}
   :crosshair       {:render! render-crosshair!       :bake! bake-crosshair!}})

;; ============================================================================
;; draw-tape! — flat tape render loop
;; ============================================================================

(defn- clip-node-screen-rect
  "Screen-space AABB for a clip group (abs layout + host left/top)."
  [^INode nd ^double left ^double top]
  (let [x0 (+ left (.getAbsX nd))
        y0 (+ top (.getAbsY nd))
        x1 (+ x0 (* (.getW nd) (.getCumScale nd)))
        y1 (+ y0 (* (.getH nd) (.getCumScale nd)))]
    [(int (Math/floor x0)) (int (Math/floor y0))
     (int (Math/ceil x1)) (int (Math/ceil y1))]))

(defn- intersect-scissor
  [[ax0 ay0 ax1 ay1] [bx0 by0 bx1 by1]]
  [(max ax0 bx0) (max ay0 by0) (min ax1 bx1) (min ay1 by1)])

(defn draw-tape!
  "Render the flat tape: iterate Object[].
   Node with RENDER_DIRTY → run :bake! first (resolve textures / bake text /
   pre-compute gradient bands into backend slots), clear the flag, then :render!
   reads only cached fields. Sentinels → PoseStack push/pop; push-clip also
   enables GuiGraphics scissor to the following clip-group node's bounds
   (nested clips intersect).

   Node abs coords are 0-rooted; `left`/`top` are the GUI's on-screen origin
   (container leftPos/topPos, standalone screen offset, or an embedded widget's
   offset). We translate the PoseStack by (left,top) so the tree lands where the
   input layer already assumes it is (input/* subtracts left/top before
   hit-testing) and where vanilla renders the container slots (leftPos+slot.x)."
  [^GuiGraphics gg ^UiRt rt left top]
  (let [^objects tape (cn.li.platform.neutral.ui/get-tape-arr rt)
        n (alength tape)
        push-clip ui-layout/push-clip-sentinel
        pop-clip  ui-layout/pop-clip-sentinel
        push-xf   ui-layout/push-transform-sentinel
        pop-xf    ui-layout/pop-transform-sentinel
        left (double left)
        top (double top)]
    (when (pos? n)
      (let [^PoseStack pose (.pose gg)
            ;; Nested scissor stack: each entry is [x0 y0 x1 y1] in screen space.
            clip-stack (java.util.ArrayDeque.)]
        (.pushPose pose)
        (.translate pose left top 0.0)
        (loop [i 0]
          (when (< i n)
            (let [entry (aget tape i)]
              (if (instance? INode entry)
                (let [^INode nd entry
                      kdef (get kind-renderers (.getKind nd))]
                  ;; Bake on dirty: only recompute caches when a binding wrote the node
                  (when (.hasFlag nd node/FLAG-RENDER-DIRTY)
                    (when-let [bake-fn (:bake! kdef)]
                      (bake-fn nd))
                    (.clearFlag nd node/FLAG-RENDER-DIRTY))
                  (when-let [render-fn (:render! kdef)]
                    (render-fn gg nd)))
                ;; Sentinel dispatch
                (cond
                  (identical? push-clip entry)
                  (do
                    (.pushPose pose)
                    ;; flatten-into! emits push-clip then the clip group node.
                    (when (< (inc i) n)
                      (let [clip-nd (aget tape (inc i))]
                        (when (instance? INode clip-nd)
                          (let [rect (clip-node-screen-rect ^INode clip-nd left top)
                                rect (if (.isEmpty clip-stack)
                                       rect
                                       (intersect-scissor (.peek clip-stack) rect))
                                [x0 y0 x1 y1] rect]
                            (.push clip-stack rect)
                            (when (and (< x0 x1) (< y0 y1))
                              (.enableScissor gg x0 y0 x1 y1)))))))

                  (identical? pop-clip entry)
                  (do
                    (when-not (.isEmpty clip-stack)
                      (.pop clip-stack))
                    (if (.isEmpty clip-stack)
                      (.disableScissor gg)
                      (let [[x0 y0 x1 y1] (.peek clip-stack)]
                        (when (and (< x0 x1) (< y0 y1))
                          (.enableScissor gg x0 y0 x1 y1))))
                    (.popPose pose))

                  (identical? push-xf entry) (.pushPose pose)
                  (identical? pop-xf entry)  (.popPose pose))))
            (recur (unchecked-inc-int i))))
        (.popPose pose)
        (when-not (.isEmpty clip-stack)
          (.disableScissor gg))))))

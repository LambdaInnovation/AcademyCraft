(ns cn.li.mc1211.client.overlay.elements
  "Shared overlay element renderer for legacy plan-based tests.
   Not used by reactive overlay."
  (:require [clojure.string :as str]
            [cn.li.platform.neutral.config :as modid]
            [cn.li.mcmod.util.log :as log])
  (:import [com.mojang.blaze3d.systems RenderSystem]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.gui GuiGraphics Font]
           [net.minecraft.resources ResourceLocation]))

(defn- draw-string! [^GuiGraphics graphics ^String text x y color]
  (let [^Minecraft mc (Minecraft/getInstance)
        ^Font font (.-font mc)]
    (.drawString graphics font text (int x) (int y) (unchecked-int color))))

(defn- normalize-texture-path [path]
  (when (and path (not (str/blank? path)))
    (cond
      (str/includes? path ":") path
      (str/starts-with? path "textures/") (str modid/mod-id ":" path)
      :else (str modid/mod-id ":textures/" path))))

(defn- argb-map [{:keys [a r g b]}]
  (bit-or (bit-shift-left (int (or a 255)) 24)
          (bit-shift-left (int (or r 255)) 16)
          (bit-shift-left (int (or g 255)) 8)
          (int (or b 255))))

(defn- rgb-vec->argb [[r g b] alpha]
  (bit-or (bit-shift-left (int (* 255 (double alpha))) 24)
          (bit-shift-left (int r) 16)
          (bit-shift-left (int g) 8)
          (int b)))

(defn- element-color [color]
  (if (map? color) (argb-map color) (unchecked-int (or color 0xFFFFFFFF))))

(defn render-bar!
  [^GuiGraphics graphics {:keys [x y width height percent bg-texture fg-texture bar-color hint-percent icon-cutout scroll-offset]}]
  (let [filled-width (int (* (double (or percent 0.0)) (int width)))
        bg-path (normalize-texture-path bg-texture)
        fg-path (normalize-texture-path fg-texture)
        cutout-start (when icon-cutout (+ (int x) (int (:x-offset icon-cutout 0))))
        cutout-width (when icon-cutout (int (:w icon-cutout 0)))
        cutout-end (when icon-cutout (+ cutout-start cutout-width))]
    (when-let [bg-loc (and bg-path (ResourceLocation/tryParse bg-path))]
      (.blit graphics bg-loc (int x) (int y) 0 0 (int width) (int height) (int width) (int height)))
    (when (and hint-percent (pos? hint-percent) (< hint-percent percent))
      (let [hint-x (int (+ x (* (double hint-percent) width)))]
        (.fill graphics hint-x (int y) (+ hint-x 1) (int (+ y height))
               (unchecked-int 0x80FF4444))))
    (letfn [(draw-segment! [seg-start seg-end]
              (when (< seg-start seg-end)
                (if bar-color
                  (let [color-int (argb-map bar-color)]
                    (.enableScissor graphics (int seg-start) (int y) (int seg-end) (int (+ y height)))
                    (.fill graphics (int seg-start) (int y) (int seg-end) (int (+ y height)) (unchecked-int color-int))
                    (.disableScissor graphics))
                  (when-let [fg-loc (and fg-path (ResourceLocation/tryParse fg-path))]
                    (let [uoff (float (* (double (or scroll-offset 0.0)) width))]
                      (.enableScissor graphics (int seg-start) (int y) (int seg-end) (int (+ y height)))
                      (.blit graphics fg-loc (int x) (int y) uoff 0.0 (int width) (int height) (float width) (float height))
                      (.disableScissor graphics))))))]
      (when (and fg-path (pos? filled-width))
        (let [bar-start (int x)
              bar-end (int (+ x filled-width))]
          (if (and cutout-start cutout-width (pos? cutout-width))
            (do
              (draw-segment! bar-start (min bar-end cutout-start))
              (draw-segment! (max bar-start cutout-end) bar-end))
            (draw-segment! bar-start bar-end)))))))

(defn- render-skill-slot!
  [^GuiGraphics graphics {:keys [x y key-label skill-icon skill-name content-icon content-label
                                 in-cooldown disabled? cooldown-seconds status-seconds
                                 visual-state alpha glow-color sin-effect?]}]
  (let [icon (or content-icon skill-icon)
        label (or content-label skill-name)
        in-cd? (or disabled? in-cooldown)
        cd-secs (or status-seconds cooldown-seconds)
        effective-alpha (double (or alpha 1.0))
        bg-color (case visual-state
                   :charge (rgb-vec->argb (or glow-color [255 173 55]) (* 0.4 effective-alpha))
                   :active (rgb-vec->argb (or glow-color [70 179 255]) (* 0.4 effective-alpha))
                   (unchecked-int (rgb-vec->argb [0 0 0] (* 0.5 effective-alpha))))]
    (.fill graphics x y (+ x 20) (+ y 20) (unchecked-int bg-color))
    (when (and glow-color (not= visual-state :idle))
      (let [border-alpha (if sin-effect?
                           (* 0.6 effective-alpha (+ 0.5 (* 0.5 (Math/sin (* (/ (System/currentTimeMillis) 300.0) Math/PI)))))
                           (* 0.6 effective-alpha))
            border-color (rgb-vec->argb glow-color border-alpha)]
        (.fill graphics (dec x) (dec y) (+ x 21) y (unchecked-int border-color))
        (.fill graphics (dec x) (+ y 20) (+ x 21) (+ y 21) (unchecked-int border-color))
        (.fill graphics (dec x) y x (+ y 20) (unchecked-int border-color))
        (.fill graphics (+ x 20) y (+ x 21) (+ y 20) (unchecked-int border-color))))
    (draw-string! graphics (str key-label) (+ x 2) (+ y 2) 0xFFFFFF)
    (when icon
      (when-let [icon-loc (ResourceLocation/tryParse (normalize-texture-path icon))]
        (.blit graphics icon-loc (+ x 25) y 0 0 16 16 16 16)))
    (draw-string! graphics (str label) (+ x 45) (+ y 6) 0xFFFFFF)
    (when in-cd?
      (.fill graphics x y (+ x 20) (+ y 20) -1073741824)
      (when (pos? (double (or cd-secs 0.0)))
        (draw-string! graphics (format "%.1fs" (double cd-secs)) (+ x 3) (+ y 10) 0xFFFFFF)))))

(defn- render-crosshair!
  [^GuiGraphics graphics {:keys [x y phase intensity]}]
  (let [cx (int (or x 0))
        cy (int (or y 0))
        p (double (or phase 0.0))
        amp (double (or intensity 1.0))
        pulse (+ 1.0 (* 0.5 (Math/sin (* 2.0 Math/PI p))))
        radius (+ 11.0 (* 4.0 pulse amp))
        gap (+ 6 (int (* 2.0 pulse amp)))
        len (+ 8 (int (* 2.0 pulse amp)))
        line-color 0xB4E8F8FF
        ring-color 0x88DDF2FF]
    (.fill graphics (- cx len) (dec cy) (- cx gap) (inc cy) line-color)
    (.fill graphics (+ cx gap) (dec cy) (+ cx len) (inc cy) line-color)
    (.fill graphics (dec cx) (- cy len) (inc cx) (- cy gap) line-color)
    (.fill graphics (dec cx) (+ cy gap) (inc cx) (+ cy len) line-color)
    (doseq [idx (range 24)]
      (let [a (/ (* 2.0 Math/PI idx) 24.0)
            rx (+ cx (int (Math/round (* radius (Math/cos a)))))
            ry (+ cy (int (Math/round (* radius (Math/sin a)))))]
        (.fill graphics (dec rx) (dec ry) (inc rx) (inc ry) ring-color)))))

(defn- render-preset-indicator!
  [^GuiGraphics graphics {:keys [x y current total fade]}]
  (let [square-size 8
        gap 3
        total-width (+ (* total square-size) (* (dec total) gap))
        start-x (- x (int (/ total-width 2)))
        alpha-byte (int (* 255 (double (or fade 1.0))))]
    (doseq [i (range total)]
      (let [sx (+ start-x (* i (+ square-size gap)))
            active? (= i current)
            color (if active?
                    (bit-or (bit-shift-left alpha-byte 24) 0x00FFAA00)
                    (bit-or (bit-shift-left (int (* alpha-byte 0.5)) 24) 0x00666666))]
        (.fill graphics sx y (+ sx square-size) (+ y square-size) (unchecked-int color))
        (when active?
          (let [glow (bit-or (bit-shift-left alpha-byte 24) 0x00FFD700)]
            (.fill graphics (dec sx) (dec y) (+ sx square-size 1) y (unchecked-int glow))
            (.fill graphics (dec sx) (+ y square-size) (+ sx square-size 1) (+ y square-size 1) (unchecked-int glow))
            (.fill graphics (dec sx) y sx (+ y square-size) (unchecked-int glow))
            (.fill graphics (+ sx square-size) y (+ sx square-size 1) (+ y square-size) (unchecked-int glow))))))))

(defn- render-blit-texture!
  [^GuiGraphics graphics {:keys [texture x y w h alpha u v tex-w tex-h tint]}]
  (when (and texture (pos? (int (or w 0))) (pos? (int (or h 0))))
    (when-let [loc (ResourceLocation/tryParse (normalize-texture-path texture))]
      (let [a (float (double (or alpha 1.0)))
            [tr tg tb] (or tint [1.0 1.0 1.0])
            u (float (double (or u 0.0)))
            v (float (double (or v 0.0)))
            tw (float (double (or tex-w w)))
            th (float (double (or tex-h h)))]
        (RenderSystem/setShaderColor (float tr) (float tg) (float tb) a)
        (.blit graphics loc (int x) (int y) u v (int w) (int h) tw th)
        (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)))))

(defn- render-movement-hints!
  "Upstream KeyHintUI key-group column: one box per movement sub-key, the
  held key highlighted like an active skill slot."
  [^GuiGraphics graphics {:keys [x y skill-icon items]}]
  (doseq [[idx {:keys [key-label label active?]}] (map-indexed vector items)]
    (let [ix (int (+ (long x) 0))
          iy (int (+ (long y) (* (long idx) 22)))
          active? (boolean active?)
          bg-color (if active?
                     (rgb-vec->argb [70 179 255] 0.4)
                     (unchecked-int (rgb-vec->argb [0 0 0] 0.5)))]
      (.fill graphics ix iy (+ ix 20) (+ iy 20) bg-color)
      (when active?
        (let [border-color (rgb-vec->argb [70 179 255] 0.6)]
          (.fill graphics (dec ix) (dec iy) (+ ix 21) iy border-color)
          (.fill graphics (dec ix) (+ iy 20) (+ ix 21) (+ iy 21) border-color)
          (.fill graphics (dec ix) iy ix (+ iy 20) border-color)
          (.fill graphics (+ ix 20) iy (+ ix 21) (+ iy 20) border-color)))
      (draw-string! graphics (str key-label) (+ ix 2) (+ iy 2) 0xFFFFFF)
      (when skill-icon
        (when-let [icon-loc (ResourceLocation/tryParse (normalize-texture-path skill-icon))]
          (.blit graphics icon-loc (+ ix 25) iy 0 0 16 16 16 16)))
      (draw-string! graphics (str label) (+ ix 45) (+ iy 6) 0xFFFFFF))))

(defn render-element!
  [^GuiGraphics graphics element screen-width screen-height]
  (case (:kind element)
    :bar (render-bar! graphics element)
    :activation-indicator (when (:activated element)
                            (draw-string! graphics "*" (:x element) (:y element) 0x00FF00)
                            (when-let [hint (:hint element)]
                              (draw-string! graphics (str hint) (+ (:x element) 12) (:y element) 0xCCCCCC)))
    :movement-hints (render-movement-hints! graphics element)
    :content-slot (render-skill-slot! graphics element)
    :skill-slot (render-skill-slot! graphics element)
    :content-crosshair (render-crosshair! graphics element)
    :vec-reflection-crosshair (render-crosshair! graphics element)
    :selection-indicator (render-preset-indicator! graphics element)
    :preset-indicator (render-preset-indicator! graphics element)
    :blit-texture (render-blit-texture! graphics element)
    :overload-pulse (let [intensity (double (or (:intensity element) 0.0))
                          alpha (int (* 40 intensity))]
                      (when (pos? alpha)
                        (.fill graphics 0 0 (int screen-width) (int screen-height)
                               (unchecked-int (bit-or (bit-shift-left alpha 24) 0x00FF2200)))))
    :text (draw-string! graphics (str (:text element)) (:x element) (:y element)
                        (element-color (:color element)))
    :fill (.fill graphics (:x element) (:y element) (+ (:x element) (:w element)) (+ (:y element) (:h element))
                 (element-color (:color element)))
    :fullscreen-fill (.fill graphics 0 0 (int screen-width) (int screen-height)
                           (element-color (:color element)))
    nil))

(defn render-elements!
  [^GuiGraphics graphics elements screen-width screen-height]
  (try
    (doseq [element elements]
      (render-element! graphics element screen-width screen-height))
    (catch Exception e
      (log/debug "Error rendering overlay elements:" (ex-message e)))))

(ns cn.li.mc262.gui.reactive.terminal-render
  "Install TerminalUI's camera on 26.2's extracted GUI pipeline.

   1.20.1 and 1.21.1 host the camera by swapping RenderSystem's projection and
   modelview for the duration of the panel. 26.2 extracts the whole GUI into
   render states and draws them later, so there is no live matrix to swap and no
   point in the frame at which one would apply — every element carries its own
   2D affine pose instead, which cannot foreshorten.

   What it does expose is a public GuiElementRenderState. So the same camera
   (cn.li.mcbase.gui.reactive.terminal-camera) is collapsed into a homography and
   the drawing helpers project their own geometry through it — see
   GuiPerspectiveWarp and PerspectiveQuadRenderState."
  (:require [cn.li.mcbase.gui.reactive.terminal-camera :as camera]
            [cn.li.platform.neutral.client-runtime :as bridge]
            [cn.li.platform.neutral.ui :as rt])
  (:import [cn.li.mc262.client GuiGraphicsHelper]
           [cn.li.mc262.client.render GuiPerspectiveWarp]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.gui GuiGraphicsExtractor]
           [com.mojang.blaze3d.platform Window]
           [org.lwjgl.glfw GLFW]))

(defonce ^:private cursor-id
  (ResourceLocations/of "academy" "textures/guis/data_terminal/cursor.png"))

;; Alpha 0x66 is upstream's glColor4d(1, 1, 1, .4) reticle tint.
(def ^:private cursor-tint 0x66FFFFFF)

(defn apply-perspective!
  "Install the camera as a projective warp for this frame's extraction.

   `render-cursor!` clears it once the panel and reticle are submitted, so
   nothing else in the frame is warped."
  [^GuiGraphicsExtractor _gg ^UiRt rt mx my _pt]
  (let [^doubles fd (rt/user-signal rt :terminal-fd)
        ^objects state (rt/user-signal rt :terminal-render-state)]
    (when (and fd state)
      ;; The extractor works in GUI-scaled units, so the whole camera is built
      ;; in that space rather than in framebuffer pixels; the aspect ratio is
      ;; the same either way.
      ;; UiRt is a neutral Java API.  Read its dimensions directly so this
      ;; platform renderer does not pull the Clojure UI runtime into AOT.
      (let [screen-w (max 1.0 (double (.getScreenW rt)))
            screen-h (max 1.0 (double (.getScreenH rt)))
            cam (camera/camera-matrix (/ screen-w screen-h) fd (camera/game-seconds))]
        (GuiPerspectiveWarp/set (camera/homography cam screen-w screen-h))
        (aset state 0 Boolean/TRUE)))))

(defn render-cursor!
  [^GuiGraphicsExtractor gg ^UiRt rt _mx _my _pt]
  (let [^doubles fd (rt/user-signal rt :terminal-fd)
        ^ints fi (rt/user-signal rt :terminal-fi)
        ^objects state (rt/user-signal rt :terminal-render-state)]
    (when (and fd fi state)
      (let [{:keys [center-x center-y size]} (camera/cursor-geometry
                                               fd fi (camera/game-seconds))
            half (/ (double size) 2.0)]
        ;; MOJANG_LOGO is vanilla's textured SRC_ALPHA/ONE pipeline, so the
        ;; additive blend belongs to the submitted state instead of being
        ;; mutated globally while the tape is recorded. The helper also picks up
        ;; the warp installed above, putting the reticle on the panel's surface.
        (GuiGraphicsHelper/blitAdditive gg cursor-id
                                        (int (- (double center-x) half))
                                        (int (- (double center-y) half))
                                        (int size) (int size) cursor-tint)
        (when (aget state 0)
          (GuiPerspectiveWarp/clear)
          (aset state 0 nil))))))

(defn hide-cursor! []
  (let [^Window window (.getWindow (Minecraft/getInstance))]
    (GLFW/glfwSetInputMode (.handle window) GLFW/GLFW_CURSOR GLFW/GLFW_CURSOR_DISABLED)))

(defn show-cursor! []
  ;; Runs on terminal close. Clearing here too means a frame that died between
  ;; apply-perspective! and render-cursor! cannot leave the warp installed and
  ;; skew every GUI drawn afterwards.
  (GuiPerspectiveWarp/clear)
  (let [^Window window (.getWindow (Minecraft/getInstance))]
    (GLFW/glfwSetInputMode (.handle window) GLFW/GLFW_CURSOR GLFW/GLFW_CURSOR_NORMAL)))

(defn install-terminal-render-bridge! []
  (bridge/merge-client-bridge!
    {:terminal-apply-perspective! apply-perspective!
     :terminal-render-cursor!     render-cursor!
     :terminal-cursor-hide!       hide-cursor!
     :terminal-cursor-show!       show-cursor!}))

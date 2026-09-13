(ns cn.li.mc1211.gui.reactive.terminal-render
  "Install TerminalUI's camera on 1.21.1's GuiGraphics Screen.

   Identical in shape to the 1.20.1 shell — see cn.li.mc1201's docstring for why
   RenderSystem's *global* modelview has to be reset alongside the projection,
   or the panel is clipped away entirely. Two things differ here: 1.21.1's
   modelview stack is a JOML Matrix4fStack rather than a PoseStack, and the GUI
   pass seeds it with `translation(0, 0, 10000 - guiFarPlane)` = -1000 instead of
   -10000. Both still sit far beyond a near=1/far=100 frustum."
  (:require [cn.li.mcbase.gui.reactive.terminal-camera :as camera]
            [cn.li.platform.neutral.client-runtime :as bridge]
            [cn.li.platform.neutral.ui :as rt])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcver ResourceLocations]
           [net.minecraft.client Minecraft]
           [net.minecraft.client.gui GuiGraphics]
           [com.mojang.blaze3d.vertex PoseStack PoseStack$Pose VertexSorting]
           [com.mojang.blaze3d.systems RenderSystem]
           [com.mojang.blaze3d.platform Window GlStateManager$SourceFactor
            GlStateManager$DestFactor]
           [org.joml Matrix3f Matrix4f Matrix4fStack]
           [org.lwjgl.glfw GLFW]))

(defonce ^:private cursor-rl
  (ResourceLocations/of "academy" "textures/guis/data_terminal/cursor.png"))

(defn- window-aspect
  "Upstream reads mc.displayWidth / displayHeight — physical framebuffer pixels."
  ^double []
  (let [^Window window (.getWindow (Minecraft/getInstance))]
    (/ (double (.getWidth window)) (double (.getHeight window)))))

(defn apply-perspective!
  [^GuiGraphics gg ^UiRt rt mx my _pt]
  (let [^doubles fd (rt/user-signal rt :terminal-fd)
        ^objects saved (rt/user-signal rt :terminal-render-state)]
    (when (and fd saved)
      (let [aspect (window-aspect)
            ^Matrix4f cam (camera/camera-matrix aspect fd (camera/game-seconds))
            ^PoseStack pose (.pose gg)
            ^Matrix4fStack modelview (RenderSystem/getModelViewStack)]
        ;; Anything already queued belongs to the Screen's own projection.
        (.flush gg)
        (aset saved 0 (RenderSystem/getProjectionMatrix))
        (RenderSystem/setProjectionMatrix (camera/projection-matrix aspect)
                                          VertexSorting/DISTANCE_TO_ORIGIN)
        (.pushMatrix modelview)
        (.identity modelview)
        (RenderSystem/applyModelViewMatrix)
        (RenderSystem/disableDepthTest)
        (RenderSystem/enableBlend)
        (RenderSystem/defaultBlendFunc)
        (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)
        ;; The Screen hands us an identity pose, so the camera *is* the pose.
        ;; The normal matrix stays identity: only blits and glyphs are drawn
        ;; under this camera and neither reads it.
        (.pushPose pose)
        (let [^PoseStack$Pose top (.last pose)]
          (.set ^Matrix4f (.pose top) cam)
          (.identity ^Matrix3f (.normal top)))))))

(defn render-cursor!
  [^GuiGraphics gg ^UiRt rt _mx _my _pt]
  (let [^doubles fd (rt/user-signal rt :terminal-fd)
        ^ints fi (rt/user-signal rt :terminal-fi)
        ^objects saved (rt/user-signal rt :terminal-render-state)]
    (when (and fd fi saved)
      (let [{:keys [center-x center-y size]} (camera/cursor-geometry
                                               fd fi (camera/game-seconds))
            half (/ (double size) 2.0)
            ix (int (- (double center-x) half))
            iy (int (- (double center-y) half))
            is (int size)
            ^PoseStack pose (.pose gg)]
        (RenderSystem/enableBlend)
        (RenderSystem/blendFunc GlStateManager$SourceFactor/SRC_ALPHA
                                GlStateManager$DestFactor/ONE)
        (RenderSystem/setShaderColor 1.0 1.0 1.0 0.4)
        ;; Upstream nudges the reticle 2 units towards the viewer in design space
        ;; before drawing it, keeping it clear of the panel it sits on.
        (.pushPose pose)
        (.translate pose 0.0 0.0 -2.0)
        (.blit gg cursor-rl ix iy 0 0 is is is is)
        ;; GuiGraphics batches textured quads: flush while the camera is still
        ;; installed or the reticle would be drawn under the restored Screen
        ;; matrices instead.
        (.flush gg)
        (.popPose pose)
        (RenderSystem/defaultBlendFunc)
        (RenderSystem/setShaderColor 1.0 1.0 1.0 1.0)
        (when-let [projection (aget saved 0)]
          (.popPose pose)
          (.popMatrix ^Matrix4fStack (RenderSystem/getModelViewStack))
          (RenderSystem/applyModelViewMatrix)
          (RenderSystem/setProjectionMatrix projection VertexSorting/DISTANCE_TO_ORIGIN)
          ;; Upstream ends its draw with glEnable(GL_DEPTH_TEST); leaving it off
          ;; would follow the terminal into vanilla's remaining GUI passes.
          (RenderSystem/enableDepthTest)
          (aset saved 0 nil))))))

(defn hide-cursor!
  []
  (let [^Window window (.getWindow (Minecraft/getInstance))]
    (GLFW/glfwSetInputMode (.getWindow window) GLFW/GLFW_CURSOR GLFW/GLFW_CURSOR_DISABLED)))

(defn show-cursor!
  []
  (let [^Window window (.getWindow (Minecraft/getInstance))]
    (GLFW/glfwSetInputMode (.getWindow window) GLFW/GLFW_CURSOR GLFW/GLFW_CURSOR_NORMAL)))

(defn install-terminal-render-bridge!
  []
  (bridge/merge-client-bridge!
    {:terminal-apply-perspective! apply-perspective!
     :terminal-render-cursor!     render-cursor!
     :terminal-cursor-hide!       hide-cursor!
     :terminal-cursor-show!       show-cursor!}))

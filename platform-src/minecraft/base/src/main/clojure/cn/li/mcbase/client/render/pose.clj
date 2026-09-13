(ns cn.li.mcbase.client.render.pose
  "CLIENT-ONLY shared pose stack and vertex consumer helpers."
  (:import [com.mojang.blaze3d.vertex PoseStack VertexConsumer]
           [cn.li.mcver RenderInterop]))

(defn rotate-y
  [^PoseStack pose-stack angle]
  (.mulPose pose-stack (.rotationDegrees com.mojang.math.Axis/YP (float angle))))

(defn rotate-x
  [^PoseStack pose-stack angle]
  (.mulPose pose-stack (.rotationDegrees com.mojang.math.Axis/XP (float angle))))

(defn rotate-z
  [^PoseStack pose-stack angle]
  (.mulPose pose-stack (.rotationDegrees com.mojang.math.Axis/ZP (float angle))))

(defn rotate-axis
  "Rotate around an arbitrary (non-unit-length OK) axis, matching legacy GL11.glRotated semantics."
  [^PoseStack pose-stack angle ax ay az]
  (let [len (Math/sqrt (+ (* (double ax) ax) (* (double ay) ay) (* (double az) az)))
        [ax ay az] (if (< len 1.0e-9) [1.0 0.0 0.0] [(/ ax len) (/ ay len) (/ az len)])]
    (.mulPose pose-stack
              (.rotationDegrees (com.mojang.math.Axis/of (org.joml.Vector3f. (float ax) (float ay) (float az)))
                                 (float angle)))))

(defn push-pose
  [^PoseStack pose-stack]
  (.pushPose pose-stack))

(defn pop-pose
  [^PoseStack pose-stack]
  (.popPose pose-stack))

(defn translate
  [^PoseStack pose-stack x y z]
  (.translate pose-stack (double x) (double y) (double z)))

(defn scale
  [^PoseStack pose-stack x y z]
  (.scale pose-stack (float x) (float y) (float z)))

(defn get-pose-matrix
  [^PoseStack pose-stack]
  (.pose (.last pose-stack)))

(defn submit-vertex
  "Submit one vertex using PoseStack's current pose + normal matrices."
  [^VertexConsumer vc ^PoseStack pose-stack x y z r g b a u v overlay uv2 nx ny nz]
  (RenderInterop/submitVertex vc pose-stack
                              (float x) (float y) (float z)
                              (float r) (float g) (float b) (float a)
                              (float u) (float v)
                              (int overlay) (int uv2)
                              (float nx) (float ny) (float nz)))

(defn submit-vertex-no-overlay
  "Submit one vertex to a POSITION_COLOR_TEX_LIGHTMAP render type, which has no
  overlay and no normal element."
  [^VertexConsumer vc ^PoseStack pose-stack x y z r g b a u v uv2]
  (RenderInterop/submitVertexNoOverlay vc pose-stack
                                       (float x) (float y) (float z)
                                       (float r) (float g) (float b) (float a)
                                       (float u) (float v)
                                       (int uv2)))

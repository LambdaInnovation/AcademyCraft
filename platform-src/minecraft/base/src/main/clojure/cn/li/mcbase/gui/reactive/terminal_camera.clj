(ns cn.li.mcbase.gui.reactive.terminal-camera
  "AcademyCraft TerminalUI's camera and reticle, shared by every Minecraft version.

   Upstream draws the terminal as an AuxGui behind a real perspective camera:

     GL_PROJECTION  loadIdentity, gluPerspective(50, aspect, 1, 100)
     GL_MODELVIEW   loadIdentity
                    translate(.35 * aspect, 1.2, -4)
                    translate(1, -1.8, 0)
                    rotate(-1.6, Z)
                    rotate(-18 - 4 * (buffX/MAX_MX - .5) + sin(t/1000), Y)
                    rotate(7 + 4 * (buffY/MAX_MY - .5), X)
                    translate(-1, 1.8, 0)
                    scale(1/310, -1/310, 1/310)

   Every supported version reproduces exactly that; what differs is only how the
   matrices reach the GPU. So the camera, its pointer response and idle sway, the
   reticle's pulse, and the layout of the per-frame arrays all live here, and the
   version namespaces are left with nothing but their own graphics API.

   Nothing here touches Minecraft — it is arithmetic over JOML."
  (:import [org.joml Matrix4f]))

;; ============================================================================
;; Upstream constants (TerminalUI)
;; ============================================================================

(def max-mx "TerminalUI.MAX_MX" 605.0)
(def max-my "TerminalUI.MAX_MY" 740.0)

(def ^:private panel-scale (/ 1.0 310.0))
(def ^:private fov-radians (Math/toRadians 50.0))
(def ^:private z-near 1.0)
(def ^:private z-far 100.0)

;; ============================================================================
;; Per-frame arrays
;;
;; Allocated and integrated by cn.li.ac.terminal.client.shell-reactive; read
;; here so the index layout is written down exactly once.
;;   fd: [0]mouse-x [1]mouse-y [2]buff-x [3]buff-y [4]last-px [5]last-py
;;       [6]last-frame-ms [7]create-time-ms [8]aspect
;;
;; [4]/[5] are the previous frame's *raw* pointer, in physical pixels, owned
;; and written by shell-reactive alone — see the delta it takes from them.
;;   fi: [0]scroll [1]selection [2]last-selected-app-index [3]installed-count
;; ============================================================================

(defn buff-x
  "Smoothed pointer X. Upstream tilts the camera by buffX, not the raw mouseX."
  ^double [^doubles fd]
  (aget fd 2))

(defn buff-y
  ^double [^doubles fd]
  (aget fd 3))

(defn app-selected?
  "Whether the 3x3 cell under the reticle holds an installed app.

   Upstream keys the reticle's 1.3x swell off `getSelectedApp() != null`."
  [^ints fi]
  (let [index (+ (* (aget fi 0) 3) (aget fi 1))]
    (and (>= index 0) (< index (aget fi 3)))))

;; ============================================================================
;; Time
;; ============================================================================

(defonce ^:private client-epoch-ms (System/currentTimeMillis))

(defn game-seconds
  "Upstream's clock, LambdaLib2 `GameTimer.getTime()`: *seconds* elapsed since
   the client started, not epoch milliseconds. The sway's `/ 1000` and the
   reticle's `/ 300` are divisors against that scale — feed them epoch ms and
   both run about a thousand times fast."
  ^double []
  (/ (- (double (System/currentTimeMillis)) (double client-epoch-ms)) 1000.0))

;; ============================================================================
;; Camera
;; ============================================================================

(defn projection-matrix
  "gluPerspective(50, aspect, 1, 100)."
  ^Matrix4f [^double aspect]
  (doto (Matrix4f.)
    (.setPerspective (float fov-radians) (float aspect) (float z-near) (float z-far))))

(defn camera-matrix
  "TerminalUI's GL_MODELVIEW chain, mapping terminal design space to eye space.

   JOML post-multiplies exactly as glTranslated/glRotated/glScaled do, so the
   calls below read in upstream's own order."
  ^Matrix4f [^double aspect ^doubles fd ^double t-sec]
  (let [yaw (+ -18.0
               (* -4.0 (- (/ (buff-x fd) max-mx) 0.5))
               (Math/sin (/ t-sec 1000.0)))
        pitch (+ 7.0 (* 4.0 (- (/ (buff-y fd) max-my) 0.5)))]
    (doto (Matrix4f.)
      (.translate (float (* 0.35 aspect)) (float 1.2) (float -4.0))
      (.translate (float 1.0) (float -1.8) (float 0.0))
      (.rotateZ (float (Math/toRadians -1.6)))
      (.rotateY (float (Math/toRadians yaw)))
      (.rotateX (float (Math/toRadians pitch)))
      (.translate (float -1.0) (float 1.8) (float 0.0))
      (.scale (float panel-scale) (float (- panel-scale)) (float panel-scale)))))

(defn homography
  "Collapse the whole camera into a row-major 3x3 design -> screen matrix:
   `[Xw Yw w] = H * [x y 1]`, screen = `(Xw/w, Yw/w)`.

   The terminal is a plane and gluPerspective is a pinhole, so the projection
   restricted to that plane *is* a homography — this is exact, not a fit. Only
   versions that cannot install a real camera need it, but it is pure geometry,
   so it belongs beside the camera rather than in a version namespace.

   Design space reaches eye space affinely, so its three eye components are each
   linear in (x, y, 1) — read straight off the camera's x-axis, y-axis and
   origin columns. Dividing by -z is what makes the result projective, and is
   precisely what a 2D affine GUI pose cannot carry."
  ^floats [^Matrix4f camera ^double screen-w ^double screen-h]
  (let [aspect (/ screen-w screen-h)
        focal (/ 1.0 (Math/tan (/ fov-radians 2.0)))
        fx (/ focal aspect)
        kx (* 0.5 screen-w)
        ky (* 0.5 screen-h)
        ;; Eye-space x/y/z coefficients for design x, design y and the origin.
        ex0 (.m00 camera) ex1 (.m10 camera) ex2 (.m30 camera)
        ey0 (.m01 camera) ey1 (.m11 camera) ey2 (.m31 camera)
        ;; Eye z is negative in front of the camera; the perspective divisor is
        ;; its negation.
        w0 (- (.m02 camera)) w1 (- (.m12 camera)) w2 (- (.m32 camera))]
    (float-array
      [(* kx (+ (* fx ex0) w0)) (* kx (+ (* fx ex1) w1)) (* kx (+ (* fx ex2) w2))
       (* ky (- w0 (* focal ey0))) (* ky (- w1 (* focal ey1))) (* ky (- w2 (* focal ey2)))
       w0 w1 w2])))

;; ============================================================================
;; Reticle
;; ============================================================================

(defn cursor-geometry
  "Upstream's reticle: a 20px disc breathing by ±2px, swelling 1.3x over an app,
   centred on the smoothed pointer and pushed 120px down the panel.

   Returned in terminal design space, like every other coordinate here."
  [^doubles fd ^ints fi ^double t-sec]
  (let [size (* (if (app-selected? fi) 1.3 1.0)
                (+ 20.0 (* 2.0 (Math/sin (/ t-sec 300.0)))))]
    {:center-x (buff-x fd)
     :center-y (+ (buff-y fd) 120.0)
     :size size}))

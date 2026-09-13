(ns cn.li.mcmod.client.render.buffer
  "Render buffer API for model rendering via Framework function map.

   Buffer ops stored at [:platform :render-buffer-ops]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def buffer-ops-keys #{:solid :translucent :cutout-no-cull :submit-vertex :triangle-vertex-order})

(defn- buffer-op [k]
  (get-in @(fw/fw-atom) [:platform :render-buffer-ops k]))

(defn install-render-buffer-ops!
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) buffer-ops-keys))]
      (swap! fw-atom assoc-in [:platform :render-buffer-ops] ops-map)
      (log/debug "Buffer ops installed:" (pr-str (keys ops-map)))
      (when missing
        (log/error "Buffer ops MISSING required keys:" (pr-str missing))))
    (log/error "Buffer ops install FAILED: Framework atom nil")))

(defn render-buffer-ops-available? []
  (boolean (get-in @(fw/fw-atom) [:platform :render-buffer-ops])))

(defn- require-buffer-fn [buffer-fn kind]
  (or buffer-fn
      (throw (ex-info (str "Render buffer function not initialized: " kind)
                      {:kind kind :hint "Call install-render-buffer-ops! before renderer registration"}))))

(defn get-solid-buffer [buffer-source texture]
  ((require-buffer-fn (buffer-op :solid) :solid) buffer-source texture))

(defn get-translucent-buffer [buffer-source texture]
  ((require-buffer-fn (buffer-op :translucent) :translucent) buffer-source texture))

(defn get-cutout-no-cull-buffer [buffer-source texture]
  ((require-buffer-fn (buffer-op :cutout-no-cull) :cutout-no-cull) buffer-source texture))

;; See-through translucent: no depth test, no depth write, no cull — the state
;; legacy renderers got from glDisable(GL_DEPTH_TEST) + glDepthMask(false) +
;; glDisable(GL_CULL_FACE). Its vertex format is version-specific, so vertices
;; must go through `submit-vertex-no-overlay`, which owns that difference.
;; Kept outside `buffer-ops-keys` (i.e. optional rather than required) so a
;; loader that cannot express the state degrades to the depth-tested buffer
;; instead of erroring; callers must check availability first.
(defn translucent-see-through-available? []
  (boolean (and (buffer-op :translucent-see-through)
                (buffer-op :submit-vertex-no-overlay))))

(defn get-translucent-see-through-buffer [buffer-source texture]
  (when-let [f (buffer-op :translucent-see-through)]
    (f buffer-source texture)))

;; See-through translucent that renders into the translucent render target —
;; the fluid surface lives there and is blitted over the main buffer at the
;; end of the level pass, covering any main-target draw beneath it. The
;; imag-phase flash needs this variant to composite over the pool.
(defn translucent-see-through-target-available? []
  (boolean (and (buffer-op :translucent-see-through-target)
                (buffer-op :submit-vertex-no-overlay))))

(defn get-translucent-see-through-target-buffer [buffer-source texture]
  (when-let [f (buffer-op :translucent-see-through-target)]
    (f buffer-source texture)))

;; Additive translucent QUADS (SRC_ALPHA/ONE — light ADDS to whatever is
;; behind) with depth TESTED (LEQUAL) and never written. The imag-phase
;; :surface-flash mode uses it so the flash reads over the opaque black pool
;; surface while terrain still occludes from the side. Optional like the
;; see-through ops: a loader without it degrades to the depth-tested buffer.
(defn additive-buffer-available? []
  (boolean (and (buffer-op :additive-buffer)
                (buffer-op :submit-vertex-no-overlay))))

(defn get-additive-buffer [buffer-source texture]
  (when-let [f (buffer-op :additive-buffer)]
    (f buffer-source texture)))

(defn submit-vertex-no-overlay [vertex-consumer pose-stack x y z r g b a u v uv2]
  (let [submit-fn (or (buffer-op :submit-vertex-no-overlay)
                      (throw (ex-info "No platform submit-vertex-no-overlay function bound"
                                      {:hint "Guard with translucent-see-through-available?"})))]
    (submit-fn vertex-consumer pose-stack x y z r g b a u v uv2)))

(defn triangle-vertex-order []
  ((require-buffer-fn (buffer-op :triangle-vertex-order) :triangle-vertex-order)))

(defn submit-vertex [vertex-consumer pose-stack x y z r g b a u v overlay uv2 nx ny nz]
  (let [submit-fn (or (buffer-op :submit-vertex)
                      (throw (ex-info "No platform submit-vertex function bound"
                                      {:hint "Call install-render-buffer-ops! during client init"})))]
    (submit-fn vertex-consumer pose-stack x y z r g b a u v overlay uv2 nx ny nz)))

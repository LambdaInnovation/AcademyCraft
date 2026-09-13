(ns cn.li.mcmod.client.render.tesr-api
  "Universal TileEntity Special Renderer API

  Provides a platform-agnostic interface for block entity rendering.
  Platform-specific TESR implementations dispatch to these methods.

  Renderer objects are plain maps with:
  - :render-tile (fn [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay] -> nil)"
  (:require [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.runtime.install :as install]))

;; ============================================================================
;; Renderer Registry & Dispatcher
;; ============================================================================

(defn create-tesr-runtime
  []
  {:renderer-registry {}
   :scripted-renderer-registry {}})

(def ^:private renderer-registry
  {})

(def ^:private scripted-renderer-registry
  {})

(defn renderer-registry-snapshot
  []
  renderer-registry)

(defn scripted-renderers-snapshot
  []
  scripted-renderer-registry)

(defn register-tile-renderer!
  "Register a renderer for a specific TileEntity type
  
  Args:
    tile-class: Class object or symbol for the TileEntity type
    renderer-fn: Function that implements ITileEntityRenderer protocol
  
  Example:
    (register-tile-renderer! TileMatrix my-renderer-obj)"
  [tile-class renderer-fn]
  (log/debug "Registering TileEntity renderer for" tile-class)
  (install/install-root! #'renderer-registry (assoc renderer-registry tile-class renderer-fn))
  nil)

;; Scripted block entities: dispatch by block-id (string). Platform registers
;; one BER for ScriptedBlockEntity and calls render-scripted-tile-entity.

(defn register-scripted-tile-renderer!
  "Register a renderer for a scripted block entity by block-id.
  Used when the BE is ScriptedBlockEntity; platform BER gets block-id and calls
  render-scripted-tile-entity(block-id, be, x, y, z)."
  [block-id renderer-obj]
  (log/debug "Registering scripted TileEntity renderer for" block-id)
  (install/install-root! #'scripted-renderer-registry (assoc scripted-renderer-registry block-id renderer-obj))
  nil)

(defn get-scripted-tile-renderer [block-id]
  (get (scripted-renderers-snapshot) block-id))

(defn render-scripted-tile-entity
  "Render a scripted block entity by block-id. Called from platform BER when
  the BE is ScriptedBlockEntity."
  [block-id tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
  (when-let [renderer (get-scripted-tile-renderer block-id)]
    (try
      ((:render-tile renderer) tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay)
      (catch Exception e
        (log/debug "Error rendering scripted tile entity" block-id (ex-message e))
        (log/stacktrace "Error rendering scripted tile entity" e)))))

(defn- get-block-id [tile-entity]
  ;; Resolves block-id via `platform.be/get-block-id` (dynamic fn when bound).
  (when tile-entity
    (let [v (platform-be/get-block-id tile-entity)]
      (when (string? v) v))))

(defn get-tile-renderer
  "Get the renderer for a TileEntity instance.
  For scripted BEs (with getBlockId), looks up scripted-renderer-registry by block-id first."
  [tile-entity]
  (when tile-entity
    (or (when-let [block-id (get-block-id tile-entity)]
          (get-scripted-tile-renderer block-id))
        (get (renderer-registry-snapshot) (class tile-entity)))))

(defn render-tile-entity
  "Universal dispatcher for rendering any TileEntity

  Args:
    tile-entity: The TileEntity to render
    partial-ticks: interpolation float
    pose-stack: PoseStack passed from platform
    buffer-source: VertexConsumer provider
    packed-light: lighting
    packed-overlay: overlay

  Returns: nil

  Dispatches to appropriate renderer based on TileEntity type."
  [tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay]
(when-let [renderer (get-tile-renderer tile-entity)]
    (try
      ((:render-tile renderer) tile-entity partial-ticks pose-stack buffer-source packed-light packed-overlay)
      (catch Exception e
        (log/debug "Error rendering tile entity:" (ex-message e))
        (log/stacktrace "Error rendering tile entity" e)))))

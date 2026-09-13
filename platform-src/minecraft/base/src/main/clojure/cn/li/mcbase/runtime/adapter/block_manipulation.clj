(ns cn.li.mcbase.runtime.adapter.block-manipulation
  "Shared IBlockManipulation adapter factory.

  Platform namespaces provide the server lookup and break guard. Shared
  Minecraft-version-specific code owns the protocol implementation and delegates
  the actual block operations to block-manipulation-core."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]))

(defonce ^:private block-core-atom (atom nil))

(defn install-block-core!
  "Install map of core fns: break-block! set-block! get-block get-block-hardness
   block-collidable? can-break-block? requires-high-tier-tool? find-blocks-in-line
   liquid-block? farmland-block?."
  [m]
  (reset! block-core-atom m)
  m)

(defn- core []
  (let [m @block-core-atom]
    (when (nil? m)
      (throw (IllegalStateException. "block-manipulation-core not installed")))
    m))

(defn create-block-manipulation
  [server-fn break-guard-fn]
  {:break-block! (fn
                   ([player-id world-id x y z drop?]
                    ((:break-block! (core)) (server-fn) player-id world-id x y z drop? break-guard-fn))
                   ([player-id world-id x y z drop? fortune-level]
                    ((:break-block! (core)) (server-fn) player-id world-id x y z drop? fortune-level break-guard-fn)))
   :set-block! (fn [world-id x y z block-id]
                 ((:set-block! (core)) (server-fn) world-id x y z block-id))
   :get-block (fn [world-id x y z]
                ((:get-block (core)) (server-fn) world-id x y z))
   :get-block-hardness (fn [world-id x y z]
                         ((:get-block-hardness (core)) (server-fn) world-id x y z))
   :block-collidable? (fn [world-id x y z]
                        ((:block-collidable? (core)) (server-fn) world-id x y z))
   :can-break-block? (fn [player-id world-id x y z]
                       (boolean ((:can-break-block? (core)) (server-fn) player-id world-id x y z break-guard-fn)))
   :requires-high-tier-tool? (fn [world-id x y z]
                               (boolean ((:requires-high-tier-tool? (core)) (server-fn) world-id x y z)))
   :find-blocks-in-line (fn [world-id x1 y1 z1 dx dy dz max-distance]
                          ((:find-blocks-in-line (core)) (server-fn) world-id x1 y1 z1 dx dy dz max-distance))
   :liquid-block? (fn [world-id x y z]
                    (boolean ((:liquid-block? (core)) (server-fn) world-id x y z)))
   :farmland-block? (fn [world-id x y z]
                      (boolean ((:farmland-block? (core)) (server-fn) world-id x y z)))})

(defn install-block-manipulation!
  [block-manipulation label]
  (when-let [fw-atom (fw/fw-atom)]
    (platform/install-adapter! fw-atom :block-manipulation block-manipulation))
  nil)

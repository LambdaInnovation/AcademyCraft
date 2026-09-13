(ns cn.li.mcmod.platform.block-manipulation
  (:require [cn.li.mcmod.framework :as fw]))

(defn available?
  []
  (boolean (get-in @(fw/fw-atom) [:platform :block-manipulation])))

(defn current
  []
  (get-in @(fw/fw-atom) [:platform :block-manipulation]))

(defn- call
  [k & args]
  (let [ops (current)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required block-manipulation operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn install-destroy-gate!
  [pred]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in [:platform :block-destroy-gate] pred))
  nil)

(defn destroy-allowed?
  "Global destroy gate. world-id lets the gate consult the upstream-style
   dimension whitelist (worldsWhitelistedDestroyingBlocks); the 0-arity form
   stays permissive-default for callers without a world context."
  ([] (destroy-allowed? nil))
  ([world-id]
   (if-let [pred (get-in @(fw/fw-atom) [:platform :block-destroy-gate])]
     (boolean (pred world-id))
     true)))

(defn break-block!
  ([player-id world-id x y z drop?]
   (when (destroy-allowed? world-id)
     (call :break-block! player-id world-id x y z drop?)))
  ([player-id world-id x y z drop? fortune-level]
   (when (destroy-allowed? world-id)
     (call :break-block! player-id world-id x y z drop? fortune-level))))

(defn set-block!
  [world-id x y z block-id]
  (call :set-block! world-id x y z block-id))

(defn get-block
  [world-id x y z]
  (call :get-block world-id x y z))

(defn get-block-hardness
  [world-id x y z]
  (call :get-block-hardness world-id x y z))

(defn block-collidable?
  [world-id x y z]
  (boolean (call :block-collidable? world-id x y z)))

(defn can-break-block?
  [player-id world-id x y z]
  (call :can-break-block? player-id world-id x y z))

(defn requires-high-tier-tool?
  [world-id x y z]
  (boolean (call :requires-high-tier-tool? world-id x y z)))

(defn find-blocks-in-line
  [world-id x1 y1 z1 dx dy dz max-distance]
  (call :find-blocks-in-line world-id x1 y1 z1 dx dy dz max-distance))

(defn liquid-block?
  [world-id x y z]
  (call :liquid-block? world-id x y z))

(defn farmland-block?
  [world-id x y z]
  (call :farmland-block? world-id x y z))

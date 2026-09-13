(ns cn.li.mcmod.platform.raycast
  (:require [cn.li.mcmod.framework :as fw]))

(defn available?
  []
  (boolean (get-in @(fw/fw-atom) [:platform :raycast])))

(defn current
  []
  (get-in @(fw/fw-atom) [:platform :raycast]))

(defn- call
  [k & args]
  (let [ops (current)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required raycast operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn raycast-blocks
  [world-id sx sy sz dx dy dz max-dist]
  (call :raycast-blocks world-id sx sy sz dx dy dz max-dist))

(defn raycast-blocks-matching
  "Trace through non-matching blocks and return the first accepted block hit."
  [world-id sx sy sz dx dy dz max-dist accepted-block-ids]
  (call :raycast-blocks-matching
        world-id sx sy sz dx dy dz max-dist accepted-block-ids))

(defn raycast-collidable-blocks-or-water
  "Trace blocks with a collision shape plus water, passing through other
  non-collidable blocks and fluids."
  [world-id sx sy sz dx dy dz max-dist]
  (call :raycast-collidable-blocks-or-water
        world-id sx sy sz dx dy dz max-dist))

(defn raycast-entities
  [world-id sx sy sz dx dy dz max-dist]
  (call :raycast-entities world-id sx sy sz dx dy dz max-dist))

(defn raycast-combined
  [world-id sx sy sz dx dy dz max-dist]
  (call :raycast-combined world-id sx sy sz dx dy dz max-dist))

(defn raycast-combined-excluding
  [world-id sx sy sz dx dy dz max-dist excluded-entity-uuid]
  (call :raycast-combined-excluding
        world-id sx sy sz dx dy dz max-dist excluded-entity-uuid))

(defn raycast-combined-all
  "Return the nearest block or any collidable entity, including non-living
  entities."
  [world-id sx sy sz dx dy dz max-dist]
  (call :raycast-combined-all world-id sx sy sz dx dy dz max-dist))

(defn raycast-combined-from-player
  "Return the nearest block or entity along a player's eye ray, excluding the
  player itself."
  [player-uuid max-dist living-only?]
  (call :raycast-combined-from-player player-uuid max-dist living-only?))

(defn player-look-vector
  [player-uuid]
  (call :get-player-look-vector player-uuid))

(defn player-position
  "Return {:x :y :z :eye-y} for a player UUID, or nil if not found.
  :x/:y/:z are body position (feet); :eye-y is eye-height Y."
  [player-uuid]
  (call :get-player-position player-uuid))

(defn raycast-from-player
  [player-uuid max-dist living-only?]
  (call :raycast-from-player player-uuid max-dist living-only?))

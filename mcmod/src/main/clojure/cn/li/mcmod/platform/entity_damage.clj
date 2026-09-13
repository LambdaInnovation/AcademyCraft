(ns cn.li.mcmod.platform.entity-damage
  (:require [cn.li.mcmod.framework :as fw]))

(defn available?
  []
  (boolean (get-in @(fw/fw-atom) [:platform :entity-damage])))

(defn current
  []
  (get-in @(fw/fw-atom) [:platform :entity-damage]))

(defn- call
  [k & args]
  (let [ops (current)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required entity-damage operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn install-pvp-gate!
  [pred]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in [:platform :entity-damage-pvp-gate] pred))
  nil)

(defn pvp-allowed?
  []
  (if-let [pred (get-in @(fw/fw-atom) [:platform :entity-damage-pvp-gate])]
    (boolean (pred))
    true))

(defn install-scripted-block-body-hit-handler!
  "Install a content-owned collision damage handler for one scripted
  block-body behavior. The handler receives
  [world-id attacker-uuid target-uuid raw-damage]."
  [behavior-id handler]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in
           [:platform :scripted-block-body-hit-handlers (name behavior-id)]
           handler))
  nil)

(defn handle-scripted-block-body-hit!
  "Dispatch a scripted block-body collision. Returns nil when no content
  handler is installed, allowing the entity to use its generic fallback.
  Any non-nil result means the collision was handled, including false when a
  permission gate deliberately rejected the hit."
  [behavior-id world-id attacker-uuid target-uuid raw-damage]
  (when-let [handler (get-in @(fw/fw-atom)
                             [:platform
                              :scripted-block-body-hit-handlers
                              (name behavior-id)])]
    (boolean
      (handler world-id attacker-uuid target-uuid (double raw-damage)))))

(defn apply-direct-damage!
  "opts (optional) supports :reset-invulnerable-time? — when true, clears the
  target's post-hit invulnerability window before applying damage, for
  skills whose original deliberately does this (e.g. ElectronMissile's
  hurtResistantTime = -1) so rapid successive hits always land full damage."
  ([world-id entity-uuid damage source-type]
   (apply-direct-damage! world-id entity-uuid damage source-type nil))
  ([world-id entity-uuid damage source-type opts]
   (call :apply-direct-damage! world-id entity-uuid damage source-type opts)))

(defn direct-source-entity-id
  "Return the UUID of a native damage source's direct/immediate entity.
  This differs from the causing entity for projectile damage."
  [damage-source]
  (call :direct-source-entity-id damage-source))

(defn reflection-target-entity-id
  "Return the UUID of the stable combat owner for reflected damage.
  The causing entity is preferred over the direct projectile, and multipart
  entities are normalized to their parent before the UUID is returned."
  [damage-source]
  (call :reflection-target-entity-id damage-source))

(defn vec-reflection-damage-source?
  "True when damage-source was created by VecReflection itself."
  [damage-source]
  (boolean (call :vec-reflection-damage-source? damage-source)))

(defn apply-aoe-damage!
  [world-id x y z radius damage source-type falloff?]
  (call :apply-aoe-damage! world-id x y z radius damage source-type falloff?))

(defn apply-reflection-damage!
  [world-id entity-uuid damage source-type reflection-count max-reflections]
  (call :apply-reflection-damage! world-id entity-uuid damage source-type reflection-count max-reflections))

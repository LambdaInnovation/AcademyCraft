(ns cn.li.mcmod.platform.entity
  "Entity/Player operations via Framework function map — pure relay layer, no MC dependencies."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def entity-ops-keys
  #{:entity-distance-to-sqr :entity-get-x :entity-get-y :entity-get-z
    :player-get-level :player-creative? :player-spectator? :player-get-name :player-get-uuid
    :player-get-horizontal-facing
    :player-get-main-hand-item-id :player-get-main-hand-item-count
    :player-get-main-hand-item-stack :player-main-hand-placeable-block?
    :player-place-main-hand-block-at-hit! :player-consume-main-hand-item!
    :player-drop-main-hand-item-at! :player-spawn-main-hand-item-copy-at!
    :player-count-item-by-id :player-consume-item-by-id!
    :player-give-item-stack! :player-spawn-entity-by-id! :player-spawn-tracked-entity-by-id!
    :player-raytrace-block :player-get-container-menu
    :inventory-get-player :menu-get-container-id})

(defn install-entity-ops!
  [ops-map _label]
  (if-let [fw-atom (fw/fw-atom)]
    (let [missing (seq (remove (set (keys ops-map)) entity-ops-keys))]
      (swap! fw-atom assoc-in [:platform :entity-ops] ops-map)
      (when missing
        (log/error "Entity ops MISSING required keys:" (pr-str missing))))
    (log/error "Entity ops install FAILED: Framework atom nil")))

(defn available? [] (boolean (get-in @(fw/fw-atom) [:platform :entity-ops])))
(defn current-ops           [] (get-in @(fw/fw-atom) [:platform :entity-ops]))

(defn- call [k & args]
  (let [ops (current-ops)
        f (get ops k)]
    (when-not f
      (throw (ex-info "Required entity operation is not installed"
                      {:operation k :installed (keys ops)})))
    (apply f args)))

(defn entity-distance-to-sqr [e x y z] (call :entity-distance-to-sqr e x y z))
(defn entity-get-x           [e]       (call :entity-get-x e))
(defn entity-get-y           [e]       (call :entity-get-y e))
(defn entity-get-z           [e]       (call :entity-get-z e))
(defn player-get-level               [p]       (call :player-get-level p))
(defn player-creative?               [p]       (call :player-creative? p))
(defn player-spectator?              [p]       (call :player-spectator? p))
(defn player-get-name                [p]       (call :player-get-name p))
(defn player-get-uuid                [p]       (call :player-get-uuid p))
(defn player-get-horizontal-facing   [p]       (call :player-get-horizontal-facing p))

(defn opposite-horizontal-facing
  "Opposite of a horizontal facing name (\"north\" ↔ \"south\", \"east\" ↔
  \"west\"). Upstream machines set their FACING to the opposite of the placer's
  horizontal facing (onBlockPlacedBy), so the front faces the player."
  [facing]
  (get {"north" "south" "south" "north" "east" "west" "west" "east"} facing facing))
(defn player-get-main-hand-item-id   [p]       (call :player-get-main-hand-item-id p))
(defn player-get-main-hand-item-count [p]      (call :player-get-main-hand-item-count p))
(defn player-get-main-hand-item-stack [p]      (call :player-get-main-hand-item-stack p))
(defn player-main-hand-placeable-block? [p]   (call :player-main-hand-placeable-block? p))
(defn player-place-main-hand-block-at-hit! [p world-id x y z face] (call :player-place-main-hand-block-at-hit! p world-id x y z face))
(defn player-consume-main-hand-item! [p n]   (call :player-consume-main-hand-item! p n))
(defn player-drop-main-hand-item-at! [p n x y z] (call :player-drop-main-hand-item-at! p n x y z))
(defn player-spawn-main-hand-item-copy-at! [p n x y z]
  (call :player-spawn-main-hand-item-copy-at! p n x y z))
(defn player-count-item-by-id        [p id]  (call :player-count-item-by-id p id))
(defn player-consume-item-by-id!     [p id n](call :player-consume-item-by-id! p id n))
(defn player-give-item-stack!        [p s]   (call :player-give-item-stack! p s))
(defn player-spawn-entity-by-id!     [p eid sp] (call :player-spawn-entity-by-id! p eid sp))
(defn player-spawn-tracked-entity-by-id!
  ([p eid sp] (call :player-spawn-tracked-entity-by-id! p eid sp nil))
  ([p eid sp life] (call :player-spawn-tracked-entity-by-id! p eid sp life)))
(defn player-raytrace-block          [p r f?](call :player-raytrace-block p r f?))
(defn player-get-container-menu      [p]     (call :player-get-container-menu p))

;; Menu/inventory ops
(defn inventory-get-player  [inv] (call :inventory-get-player inv))
(defn menu-get-container-id [menu] (call :menu-get-container-id menu))

;; Entity type ID lookup (was protocol method with [world-id entity-uuid] arity)
(defn get-type-id [world-id entity-uuid]
  (call :entity-get-type-id-fn world-id entity-uuid))

;; Entity type ID — separate path
(defn install-entity-type-id-fn! [f _label]
  (when-let [fw-atom (fw/fw-atom)] (swap! fw-atom assoc-in [:platform :entity-ops :entity-get-type-id-fn] f)) nil)
(defn entity-type-id-fn-available? []
  (boolean (get-in @(fw/fw-atom) [:platform :entity-ops :entity-get-type-id-fn])))

(ns cn.li.ac.ability.client.surround-arc
  "EntitySurroundArc equivalent: orbiting arc particles around a target.

  Matching original AcademyCraft EntitySurroundArc:
    NORMAL: 2-3 orbiting arc points around block/tile (Current Charging block mode)
    THIN: 1 orbiting arc point around player (Current Charging item mode)

  Each surround point generates short zigzag arcs pointing toward the center,
  rotating around the target at a fixed angular speed."
  (:require [cn.li.ac.ability.client.effects.rv3 :as v]
            [cn.li.ac.ability.client.render-util :as ru]))

;; ============================================================================
;; Surround configuration
;; ============================================================================

(def surround-configs
  {:normal
   {:point-count 3
    :orbit-radius 1.2
    :orbit-speed 0.157              ;; 2π/40 ticks
    :arc-length 0.8
    :arc-pattern :weak
    :arc-color {:r 110 :g 200 :b 255}
    :alpha 160}

   :thin
   {:point-count 1
    :orbit-radius 0.8
    :orbit-speed 0.157
    :arc-length 0.6
    :arc-pattern :weak
    :arc-color {:r 160 :g 220 :b 255}
    :alpha 140}})

;; ============================================================================
;; Surround arc ops
;; ============================================================================

(defn- orbit-position
  "Compute the world-space position of an orbiting point around center.
  index: which orbiting point (0, 1, 2, ...)
  total: total number of points
  angle: current base angle (radians, incremented per frame)
  radius: orbit radius"
  [center index total angle radius]
  (let [phase (* index (/ (* 2.0 Math/PI) (double total)))
        a (+ angle phase)
        dx (* radius (Math/cos a))
        dz (* radius (Math/sin a))
        ;; Slight vertical wobble
        dy (* radius 0.3 (Math/sin (* a 1.7)))]
    (v/v+ center (v/v3 dx dy dz))))

(defn surround-arc-ops
  "Generate render ops for EntitySurroundArc equivalent.

  Params:
    cam-pos   — camera position {:x :y :z}
    center    — world center of the surround effect {:x :y :z}
    surround-type — :normal or :thin
    life-ratio — 0.0-1.0 (used for arc fade)
    tick       — current world tick (for orbit animation)"
  [cam-pos center {:keys [surround-type life-ratio tick]
                   :or {surround-type :normal life-ratio 0.5 tick 0}}]
  (let [config (get surround-configs surround-type (get surround-configs :normal))
        point-count (int (:point-count config))
        radius (double (:orbit-radius config))
        speed (double (:orbit-speed config))
        arc-len (double (:arc-length config))
        pattern (:arc-pattern config)
        angle (* (double tick) speed)
        alpha (int (* (:alpha config) (max 0.0 (min 1.0 (double life-ratio)))))
        color (let [{:keys [r g b]} (:arc-color config {:r 255 :g 255 :b 255})]
                (unchecked-int (bit-or (bit-shift-left (int alpha) 24)
                                       (bit-shift-left (int r) 16)
                                       (bit-shift-left (int g) 8)
                                       (int b))))
        arc-ops
        (mapcat (fn [i]
                  (let [orbit-pos (orbit-position center i point-count angle radius)
                        ;; Arc pointing inward toward center
                        dir-to-center (v/vnorm (v/v- center orbit-pos))
                        arc-end (v/v+ orbit-pos (v/v* dir-to-center arc-len))]
                    (ru/zigzag-arc-ops cam-pos orbit-pos arc-end
                                       {:arc-pattern pattern
                                        :life-ratio life-ratio
                                        :wiggle-offset (* i 0.5)})))
                (range point-count))]
    (vec arc-ops)))

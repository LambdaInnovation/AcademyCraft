(ns cn.li.ac.ability.util.targeting
  "Entity targeting and AOE utilities for abilities.

  Pure functions for finding entities, calculating damage falloff, etc.
  No Minecraft imports.")

(defn distance-3d
  "Calculate 3D Euclidean distance between two points."
  [x1 y1 z1 x2 y2 z2]
  (Math/sqrt (+ (* (- x2 x1) (- x2 x1))
                (* (- y2 y1) (- y2 y1))
                (* (- z2 z1) (- z2 z1)))))

(defn calculate-aoe-falloff
  "Calculate damage falloff for AOE based on distance from center.

  Args:
    base-damage: base damage at center
    distance: distance from center
    max-radius: maximum AOE radius
    falloff?: if true, damage decreases linearly with distance; if false, constant damage

  Returns: scaled damage as double"
  [base-damage distance max-radius falloff?]
  (if falloff?
    (let [ratio (max 0.0 (- 1.0 (/ (double distance) (double max-radius))))]
      (* (double base-damage) ratio))
    (double base-damage)))

(defn find-entities-in-cone
  "Filter entities that are within a cone from origin in direction.

  Args:
    entities: seq of entity maps with :x :y :z coordinates
    origin-x, origin-y, origin-z: cone origin
    dir-x, dir-y, dir-z: cone direction (normalized)
    max-distance: maximum cone distance
    cone-angle: cone half-angle in radians (e.g., Math/PI / 6 for 30 degrees)

  Returns: filtered seq of entities within cone"
  [entities origin-x origin-y origin-z dir-x dir-y dir-z max-distance cone-angle]
  (filter
   (fn [entity]
     (let [dx (- (:x entity) origin-x)
           dy (- (:y entity) origin-y)
           dz (- (:z entity) origin-z)
           dist (Math/sqrt (+ (* dx dx) (* dy dy) (* dz dz)))]
       (when (<= dist max-distance)
         ;; Calculate angle between direction and entity vector
         (let [dot (+ (* dir-x dx) (* dir-y dy) (* dir-z dz))
               cos-angle (/ dot dist)]
           (>= cos-angle (Math/cos cone-angle))))))
   entities))

(defn find-nearest-entity
  "Find the nearest entity to a position.

  Args:
    entities: seq of entity maps with :x :y :z coordinates
    x, y, z: reference position

  Returns: nearest entity map or nil if empty"
  [entities x y z]
  (when (seq entities)
    (apply min-key
           (fn [entity]
             (distance-3d x y z (:x entity) (:y entity) (:z entity)))
           entities)))

(defn filter-by-distance
  "Filter entities within max-distance of position.

  Args:
    entities: seq of entity maps with :x :y :z coordinates
    x, y, z: reference position
    max-distance: maximum distance

  Returns: filtered seq of entities"
  [entities x y z max-distance]
  (filter
   (fn [entity]
     (<= (distance-3d x y z (:x entity) (:y entity) (:z entity))
         max-distance))
   entities))

(defn sort-by-distance
  "Sort entities by distance from position (nearest first).

  Args:
    entities: seq of entity maps with :x :y :z coordinates
    x, y, z: reference position

  Returns: sorted seq of entities"
  [entities x y z]
  (sort-by
   (fn [entity]
     (distance-3d x y z (:x entity) (:y entity) (:z entity)))
   entities))

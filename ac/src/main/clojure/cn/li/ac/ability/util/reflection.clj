(ns cn.li.ac.ability.util.reflection
  "Damage reflection mechanics for beam/projectile abilities.

  Reflection state tracks hit entities to prevent infinite loops.
  No Minecraft imports.")

(defn init-reflection-state
  "Initialize reflection tracking state.

  Args:
    max-reflections: maximum number of reflection bounces

  Returns: reflection state map"
  [max-reflections]
  {:hit-entities #{}
   :reflection-count 0
   :max-reflections max-reflections})

(defn add-reflection-hit
  "Record an entity hit for reflection tracking.

  Args:
    reflection-state: current reflection state map
    entity-uuid: UUID string of hit entity

  Returns: updated reflection state map"
  [reflection-state entity-uuid]
  (-> reflection-state
      (update :hit-entities conj entity-uuid)
      (update :reflection-count inc)))

(defn can-reflect?
  "Check if reflection can continue.

  Args:
    reflection-state: current reflection state map

  Returns: true if more reflections allowed"
  [reflection-state]
  (< (:reflection-count reflection-state)
     (:max-reflections reflection-state)))

(defn is-entity-hit?
  "Check if entity was already hit (prevent double-hit).

  Args:
    reflection-state: current reflection state map
    entity-uuid: UUID string to check

  Returns: true if entity already hit"
  [reflection-state entity-uuid]
  (contains? (:hit-entities reflection-state) entity-uuid))

(defn calculate-reflection-damage
  "Calculate reflected damage (50% of original per bounce).

  Args:
    base-damage: original damage amount
    reflection-count: current reflection depth

  Returns: reflected damage as double"
  [base-damage reflection-count]
  (* (double base-damage)
     (Math/pow 0.5 reflection-count)))

(defn get-reflection-targets
  "Filter entities that can be reflection targets.

  Args:
    entities: seq of entity maps with :uuid
    reflection-state: current reflection state map

  Returns: filtered seq of entities not yet hit"
  [entities reflection-state]
  (filter
   (fn [entity]
     (not (is-entity-hit? reflection-state (:uuid entity))))
   entities))

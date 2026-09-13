(ns cn.li.ac.ability.model.cooldown
  "Pure-data functions for CooldownData.

  Cooldown key = [ctrl-id sub-id] (both keywords).
  Main cooldown uses sub-id :main (equivalent to old subID=0).
  Sub-cooldowns use user-defined keywords.

  Map schema: {[ctrl-id sub-id] {:ticks remaining :max applied}}

  `:max` mirrors upstream CooldownData.SkillCooldown.maxTick: the duration this
  cooldown was actually started with, kept as ticks count down. The HUD's
  unavailable-wipe is tickLeft/maxTick, and recomputing that denominator from
  the skill spec instead (as this model's tick-only schema forced) drifts from
  the real countdown whenever the applied duration differs from the estimate —
  exp-scaled policies, the two apply paths disagreeing about
  :cooldown-policy/:ticks vs :cooldown-ticks, or set-cooldown merging a longer
  cooldown over a shorter one.")

;; ============================================================================
;; Constructors
;; ============================================================================

(defn new-cooldown-data [] {})

;; ============================================================================
;; Queries
;; ============================================================================

(defn get-remaining
  [d ctrl-id sub-id]
  (long (:ticks (get d [ctrl-id sub-id]) 0)))

(defn get-max
  "Ticks this cooldown was started with (upstream SkillCooldown.getMaxTick).
   0 when the skill is not on cooldown."
  [d ctrl-id sub-id]
  (long (:max (get d [ctrl-id sub-id]) 0)))

(defn in-cooldown?
  [d ctrl-id sub-id]
  (pos? (get-remaining d ctrl-id sub-id)))

;; ============================================================================
;; Mutation (pure, returns new map)
;; ============================================================================

(defn set-cooldown
  "Set cooldown to max(existing, ticks). Cooldowns never decrease.
  Upstream CooldownData.doSet raises tickLeft and maxTick independently, so a
  longer cooldown landing on top of a running one also extends the denominator
  the HUD wipe divides by."
  [d ctrl-id sub-id ticks]
  (let [k [ctrl-id sub-id]
        ticks (long ticks)
        cur (get d k)]
    (assoc d k (if cur
                 {:ticks (max (long (:ticks cur 0)) ticks)
                  :max (max (long (:max cur 0)) ticks)}
                 {:ticks ticks :max ticks}))))

(defn tick-cooldowns
  "Decrement all cooldowns by 1; remove entries at 0. :max is left alone — it
  records the applied duration, not a countdown.
  Returns d unchanged (identical?) when already empty, so idle-player ticks
  don't allocate a fresh map or break command_runtime's not= dirty check."
  [d]
  (if (empty? d)
    d
    (persistent!
     (reduce-kv
      (fn [acc k v]
        (let [nv (dec (long (:ticks v 0)))]
          (if (pos? nv)
            (assoc! acc k (assoc v :ticks nv))
            acc)))
      (transient {})
      d))))

;; ============================================================================
;; Serialization helpers
;; ============================================================================

(defn cooldown-data->vec
  "Serialize to vector of [ctrl-key sub-key ticks max] for NBT."
  [d]
  (mapv (fn [[[ctrl-id sub-id] {:keys [ticks max]}]]
          [ctrl-id sub-id ticks max])
        d))

(defn vec->cooldown-data
  "Deserialize from vector produced by cooldown-data->vec. A missing max (an
  older 3-element row) falls back to the remaining ticks, the same assumption
  upstream makes when it constructs SkillCooldown(cd) from a single value."
  [coll]
  (->> coll
       (reduce (fn [acc [ctrl-id sub-id ticks max]]
                 (assoc acc [(keyword ctrl-id) (keyword sub-id)]
                        {:ticks (int ticks) :max (int (or max ticks))}))
               {})))

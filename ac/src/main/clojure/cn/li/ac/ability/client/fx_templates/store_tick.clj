(ns cn.li.ac.ability.client.fx-templates.store-tick
  "Low-allocation tick helpers for FX owner→items / owner→state maps.

  Client FX ticks run at 20 Hz for every active effect. Rebuilding those maps
  with `into {}` + `map`/`filter`/`vec` allocates heavily; these helpers use
  transients and a single pass instead."
  (:refer-clojure :exclude []))

(defn tick-ttl-vec
  "Decrement `:ttl` on each item in a vector; drop expired. Transient build."
  [items]
  (if (or (nil? items) (empty? items))
    (or items [])
    (persistent!
     (reduce (fn [acc item]
               (let [ttl (dec (long (or (:ttl item) 0)))]
                 (if (pos? ttl)
                   (conj! acc (assoc item :ttl ttl))
                   acc)))
             (transient [])
             items))))

(defn tick-ttl-items-by-owner
  "Decrement `:ttl` on each item under each owner-key; drop expired items and
  empty owners. Returns `by-owner` unchanged when empty."
  [by-owner]
  (if (or (nil? by-owner) (empty? by-owner))
    (or by-owner {})
    (persistent!
     (reduce-kv
      (fn [acc owner-key items]
        (let [live (tick-ttl-vec items)]
          (if (seq live)
            (assoc! acc owner-key live)
            acc)))
      (transient {})
      by-owner))))

(defn tick-ttl-states-by-owner
  "Decrement `:ttl` on each owner state map; drop expired owners.
  When `inc-ticks?` is true, also increments `:ticks` on survivors."
  ([by-owner]
   (tick-ttl-states-by-owner by-owner false))
  ([by-owner inc-ticks?]
   (if (or (nil? by-owner) (empty? by-owner))
     (or by-owner {})
     (persistent!
      (reduce-kv
       (fn [acc owner-key st]
         (let [ttl (dec (long (or (:ttl st) 0)))]
           (if (pos? ttl)
             (assoc! acc owner-key
                     (cond-> (assoc st :ttl ttl)
                       inc-ticks? (assoc :ticks (inc (long (or (:ticks st) 0))))))
             acc)))
       (transient {})
       by-owner)))))

(defn keep-active-inc-ticks
  "Retain `:active?` owner states and increment `:ticks` via transient."
  [by-owner]
  (if (or (nil? by-owner) (empty? by-owner))
    (or by-owner {})
    (persistent!
     (reduce-kv
      (fn [acc owner-key st]
        (if (:active? st)
          (assoc! acc owner-key (update st :ticks (fnil inc 0)))
          acc))
      (transient {})
      by-owner))))

(defn keep-active
  "Retain only `:active?` owner states (no tick bump)."
  [by-owner]
  (if (or (nil? by-owner) (empty? by-owner))
    (or by-owner {})
    (persistent!
     (reduce-kv
      (fn [acc owner-key st]
        (cond-> acc
          (:active? st) (assoc! owner-key st)))
      (transient {})
      by-owner))))

(defn map-active-states
  "For each `:active?` state, call `(f owner-key st)` → next state (or nil to drop).
  Built with a transient accumulator."
  [by-owner f]
  (if (or (nil? by-owner) (empty? by-owner))
    (or by-owner {})
    (persistent!
     (reduce-kv
      (fn [acc owner-key st]
        (if-not (:active? st)
          acc
          (if-let [st' (f owner-key st)]
            (assoc! acc owner-key st')
            acc)))
      (transient {})
      by-owner))))


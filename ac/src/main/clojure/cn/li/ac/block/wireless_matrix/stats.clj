(ns cn.li.ac.block.wireless-matrix.stats
  "Wireless matrix stats computation, slot layout, and state queries.
   Extracted from logic.clj to break circular dependencies between
   logic.clj, mat_core.clj, and capability.clj."
  (:require [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wireless-matrix.schema :as matrix-schema]
            [cn.li.ac.wireless.config :as matrix-config]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]))

;; ============================================================================
;; Slot schema identity
;; ============================================================================

(def ^:private matrix-slot-schema-id :wireless-matrix)

;; ============================================================================
;; Required plate count (structural, derived from slot schema)
;; ============================================================================

(defn required-plate-count
  "Return the number of plate slots in the matrix slot schema."
  []
  (count (slot-schema/slot-indexes-by-type matrix-slot-schema-id :plate)))

;; ============================================================================
;; Stats formulas
;; ============================================================================

(defn stats-for-counts
  "Return Matrix capacity/bandwidth/range for a core and plate count.

  `required-plate-count` is passed in by the block logic because the plate slot
  layout is structural, not a player config value."
  [required-plate-count core-level plate-count]
  (let [core-lv (int core-level)
        plates (int plate-count)
        required (int required-plate-count)]
    (if (and (> core-lv 0)
             (> required 0)
             (= plates required))
      {:capacity (int (* (matrix-config/capacity-per-core-level) core-lv))
       :bandwidth (double (* core-lv core-lv (matrix-config/bandwidth-factor)))
       :range (double (* (matrix-config/range-base) (Math/sqrt core-lv)))}
      {:capacity 0 :bandwidth 0.0 :range 0.0})))

(defn matrix-stats-for-counts
  "Convenience wrapper that fills in required-plate-count from the slot schema."
  [core-level plate-count]
  (stats-for-counts (required-plate-count) core-level plate-count))

;; ============================================================================
;; State lifecycle
;; ============================================================================

(def ^:private matrix-rt
  (machine-runtime/schema-runtime matrix-schema/unified-matrix-schema))

(def matrix-default-state (:default-state matrix-rt))
(def matrix-scripted-load-fn (:load-fn matrix-rt))
(def matrix-scripted-save-fn (:save-fn matrix-rt))

(defn safe-state
  [be]
  (machine-runtime/state-or-default be matrix-default-state))

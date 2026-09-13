(ns cn.li.ac.block.wireless-matrix.logic
  "Wireless matrix tick, container, block events, state lifecycle, inventory, and stats."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.block.machine.container :as machine-container]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.wireless-matrix.schema :as matrix-schema]
            [cn.li.ac.block.wireless-matrix.stats :as stats]
            [cn.li.ac.item.constraint-plate :as plate]
            [cn.li.ac.item.mat-core :as core]
            [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.wireless.config :as matrix-config]
            [cn.li.mcmod.block.state-schema :as schema]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.item :as pitem]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.util.log :as log]))

(defn safe-state
  [be]
  (stats/safe-state be))

(defn resolve-controller-be
  [be]
  (if-not be
    nil
    (let [state (stats/safe-state be)]
      (if (zero? (long (:sub-id state 0)))
        be
        (let [world-obj (platform-be/be-get-world-safe be)
              cx (:controller-pos-x state)
              cy (:controller-pos-y state)
              cz (:controller-pos-z state)]
          (if (and world-obj (number? cx) (number? cy) (number? cz))
            (or (world/get-tile-entity world-obj (pos/create-block-pos (long cx) (long cy) (long cz)))
                be)
            be))))))

;; ============================================================================
;; Inventory / slot schema (from inventory.clj)
;; ============================================================================

(def ^:private matrix-slot-schema-id :wireless-matrix)

(def ^:private matrix-slot-schema-config
  {:schema-id matrix-slot-schema-id
   :slots [{:id :plate-a :type :plate :x 78 :y 11}
           {:id :plate-b :type :plate :x 53 :y 60}
           {:id :plate-c :type :plate :x 104 :y 60}
           {:id :core :type :core :x 78 :y 36}]})

(defonce ^:private matrix-slot-schema-registration
  (delay
    (slot-schema/register-slot-schema! matrix-slot-schema-config)))

(defn ensure-matrix-slot-schema!
  []
  @matrix-slot-schema-registration
  matrix-slot-schema-id)

(defn plate-slot-indexes
  []
  (slot-schema/slot-indexes-by-type matrix-slot-schema-id :plate))

(defn core-slot-index
  []
  (slot-schema/slot-index matrix-slot-schema-id :core))

(defn all-slot-indexes
  []
  (slot-schema/all-slot-indexes matrix-slot-schema-id))

(defn slot-count
  []
  (slot-schema/tile-slot-count matrix-slot-schema-id))

(defn- slot-has-stack?
  [stk]
  (and stk (try (pos? (long (pitem/stack-count stk))) (catch Exception _ true))))

(defn recalculate-counts
  [state]
  (let [plate-count (count (for [slot (plate-slot-indexes)
                                 :let [stk (get-in state [:inventory slot])]
                                 :when (slot-has-stack? stk)]
                             slot))
        core-stack (get-in state [:inventory (core-slot-index)])
        core-level (if (slot-has-stack? core-stack)
                     (inc (int (max 0 (pitem/damage core-stack))))
                     0)]
    (assoc state :plate-count plate-count :core-level core-level)))

(defn is-working?
  [state]
  (and (> (:core-level state 0) 0)
       (= (:plate-count state 0) (stats/required-plate-count))))

;; (stats formulas moved to cn.li.ac.block.wireless-matrix.stats)

;; ============================================================================
;; Convenience accessors
;; ============================================================================

(defn get-plate-count
  [be]
  (if-let [ctrl (resolve-controller-be be)]
    (get (stats/safe-state ctrl) :plate-count 0)
    0))

(defn get-core-level
  [be]
  (if-let [ctrl (resolve-controller-be be)]
    (get (stats/safe-state ctrl) :core-level 0)
    0))

;; ============================================================================
;; Ownership helpers (aligned with wireless-node pattern)
;; ============================================================================

(defn placer-name
  "Normalize matrix placer from tile custom state map.
   Returns empty string when placer cannot be resolved.
   (Analogous to node-logic/owner-name)"
  [state]
  (str (get (or state {}) :placer-name "")))

(defn placer-uuid
  "Return the canonical placer UUID string from tile custom state map, or nil."
  [state]
  (get (or state {}) :placer-uuid))

(defn owner-authorized?
  "True when player is allowed to edit matrix owner-protected fields.
   Empty stored UUID means the block is not initialized yet and remains editable."
  [state player]
  (let [stored-uuid (str (or (placer-uuid state) ""))
        player-uuid (str (or (uuid/player-uuid player) ""))]
    (or (str/blank? stored-uuid)
        (and (not (str/blank? player-uuid))
             (= stored-uuid player-uuid)))))

;; ============================================================================
;; Tick
;; ============================================================================

(defn- matrix-sync-payload
  [state pos]
  (let [stats' (stats/matrix-stats-for-counts (:core-level state) (:plate-count state))]
    (-> (schema/schema->sync-payload matrix-schema/unified-matrix-schema state pos)
        (assoc :is-working (is-working? state)
               :capacity (:capacity stats')
               :bandwidth (:bandwidth stats')
               :range (:range stats')))))

(defn matrix-tick-state
  [state _level pos _block-state _be]
  (let [ticker (machine-runtime/advance-tick! state)
        state1 state]
    (if (and (zero? (:sub-id state1 0))
             (zero? (mod ticker (matrix-config/gui-sync-interval))))
      (try
        (matrix-sync-payload state1 pos)
        state1
        (catch Exception e
          (log/debug "Matrix sync skipped:" (ex-message e))
          state1))
      state1)))

(def matrix-scripted-tick-fn
  (machine-runtime/make-tick-fn
    {:default-state stats/matrix-default-state
     :tick-state matrix-tick-state
     }))

;; ============================================================================
;; Container
;; ============================================================================

(def matrix-container-fns
  (machine-container/make-inventory-container-fns
    {:default-state stats/matrix-default-state
     :slot-count slot-count
     :transform-state recalculate-counts
     :slots-for-face (fn [_be _face] (int-array (all-slot-indexes)))
     :can-place? (fn [_be slot item _face]
                   (cond
                     (contains? (set (plate-slot-indexes)) slot)
                     (plate/is-constraint-plate? item)
                     (= slot (core-slot-index))
                     (core/is-mat-core? item)
                     :else false))
     :can-take? (fn [_be _slot _item _face] true)}))

;; ============================================================================
;; Block events
;; ============================================================================

(def handle-matrix-right-click
  (machine-runtime/make-open-gui-handler :matrix))

(defn handle-matrix-place
  []
  (fn [player world pos _block-id]
    (when-let [be (world/get-tile-entity world pos)]
      (let [player-uuid (uuid/player-uuid player)
            player-name (try (str (or (entity/player-get-name player) ""))
                             (catch Exception _ ""))
            state (stats/safe-state be)
            state' (-> state
                       (assoc :placer-uuid player-uuid)
                       (assoc :placer-name player-name))]
        (machine-runtime/commit-state! be world pos state state')))))

(defn handle-matrix-break
  []
  (fn [world pos _block-id]
    ;; Inventory items are dropped automatically by SharedScriptedBlock.onRemove
    ;; via Containers.dropContents.
    (try
      (wireless-api/destroy-network-at!
        world (pos/pos-x pos) (pos/pos-y pos) (pos/pos-z pos))
      (catch Exception e
        (log/warn "[wireless-matrix] break cleanup failed at" pos ":" (ex-message e))))
    {:break-handled true}))

(ns cn.li.ac.tutorial.conditions
  "Condition-based tutorial unlock system.

  Original AcademyCraft TutorialInit defined conditions via itemObtained()
  (OR of itemCrafted + itemPickup + itemSmelted).  Each tutorial could have
  multiple conditions, all OR'd — fulfilling any single condition activates
  the tutorial.

  Design:
    - Global condition index: deduplicated vector of all conditions across
      all registered tutorials, each assigned a stable numeric index.
    - Condition: {:type :item-crafted|:item-smelted|:item-pickup
                  :item-id \"academy:...\"}
    - Tutorial registry stores condition indices per tutorial.
    - When an item event fires, matching condition indices are marked in
      player state.  Tutorials whose any condition index matches become
      newly activated.

  All functions are pure — no platform imports."
  (:require [cn.li.ac.config.modid :as modid]
            [cn.li.ac.tutorial.model :as model]
            [clojure.string :as str]))

;; ============================================================================
;; Terminal app condition extension (must precede condition-index init)
;; ============================================================================

(def ^:private non-preinstalled-app-ids
  "App IDs that require an installer item (not pre-installed with terminal).
  Matching upstream TutorialInit.java line 77-82: for each non-pre-installed
  app, an itemObtained condition is added for its installer item.
  Convention: app_<id-with-underscores> — see app_installers.clj."
  #{:skill-tree :freq-transmitter :media-player})

(defn- app-installer-item-id
  "Convert an app keyword id to its installer item id string.
  E.g. :skill-tree → \"academy:app_skill_tree\""
  [app-id]
  (modid/namespaced-path (str "app_" (str/replace (name app-id) "-" "_"))))

(defn extend-terminal-conditions
  "Add item-obtained conditions for non-pre-installed terminal apps to
  the :terminal tutorial entry.  Returns updated tutorials vector."
  [tutorials]
  (mapv (fn [tut]
          (if (= (:id tut) :terminal)
            (let [extra-conds (mapv (fn [app-id]
                                      {:type :item-obtained
                                       :item-id (app-installer-item-id app-id)})
                                    non-preinstalled-app-ids)]
              (update tut :conditions into extra-conds))
            tut))
        tutorials))

;; ============================================================================
;; Condition index
;; ============================================================================

(defn- dedupe-conditions
  "Extract unique conditions from a tutorial entry."
  [tutorial]
  (:conditions tutorial))

(defn build-condition-index
  "Build the global ordered vector of unique conditions across all tutorials.
  Each condition's position in the returned vector is its stable index.

  Returns: [{:type :item-obtained :item-id \"...\"} ...]"
  [tutorials]
  (->> tutorials
       (mapcat dedupe-conditions)
       (distinct)
       vec))

(def ^:private condition-index-cache
  "Cached condition index built once from the registry at init time."
  (atom nil))

(defn ensure-condition-index!
  "Initialize the global condition index from the tutorial registry.
  Idempotent — builds the index once and caches it.
  Extends terminal tutorial conditions dynamically (matching upstream)."
  [tutorials]
  (when-not @condition-index-cache
    (reset! condition-index-cache
            (-> tutorials
                (extend-terminal-conditions)
                (build-condition-index))))
  @condition-index-cache)

(defn condition-index
  "Look up the index of a specific condition in the global index.
  Returns the integer index or nil if not found."
  [condition]
  (when-let [idx @condition-index-cache]
    (let [^java.util.List idx idx
          i (.indexOf idx condition)]
      (when (>= i 0) i))))

;; ============================================================================
;; Tutorial condition resolution
;; ============================================================================

(defn- condition-indices-for-tutorial
  "Resolve all condition indices for a single tutorial entry."
  [tutorial]
  (keep condition-index (:conditions tutorial)))

(defn build-tutorial-condition-map
  "Build a map from tutorial-id to vector of condition indices.
  Only includes condition-based tutorials (default-installed? = false)."
  [tutorials]
  (into {}
        (keep (fn [tut]
                (when-not (:default-installed? tut)
                  (when-let [indices (seq (condition-indices-for-tutorial tut))]
                    [(:id tut) indices]))))
        tutorials))

;; ============================================================================
;; Activation logic
;; ============================================================================

(defn check-new-activations
  "Given the current player state and all tutorials, return a set of
  tutorial ids that should be newly activated (conditions now met but
  not yet in activated-tuts).

  `tutorial-cond-map` is the result of `build-tutorial-condition-map`."
  [tutorial-state tutorial-cond-map]
  (let [activated (:activated-tuts tutorial-state)]
    (reduce-kv (fn [acc tut-id cond-indices]
                 (if (or (contains? activated tut-id)
                         (not (model/any-condition-met? tutorial-state cond-indices)))
                   acc
                   (conj acc tut-id)))
               #{}
               tutorial-cond-map)))

;; ============================================================================
;; Item event → condition matching
;; ============================================================================

(def ^:private obtained-event-types #{:item-crafted :item-smelted :item-pickup})

(defn- match-condition?
  "Check if `condition` matches the given `item-id` and `event-type`.
  event-type is one of :item-crafted, :item-smelted, :item-pickup.

  :item-obtained acts as an OR of the three concrete event types,
  matching upstream Conditions.itemObtained() semantics."
  [condition item-id event-type]
  (and (= (:item-id condition) item-id)
       (or (= (:type condition) event-type)
           (and (= (:type condition) :item-obtained)
                (contains? obtained-event-types event-type)))))

(defn find-matching-conditions
  "Find all condition indices that match an item event.
  Returns a vector of integer indices."
  [item-id event-type]
  (let [idx @condition-index-cache]
    (when idx
      (keep-indexed (fn [i cond]
                      (when (match-condition? cond item-id event-type)
                        i))
                    idx))))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn reset-condition-index!
  "Reset cached condition index.  For test isolation only."
  []
  (reset! condition-index-cache nil)
  nil)

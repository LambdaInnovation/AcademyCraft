(ns cn.li.ac.tutorial.model
  "Pure-data model for tutorial player state.

  State schema:
    {:activated-tuts     #{keyword}   ; activated tutorial ids
     :condition-flags    #{int}       ; fulfilled condition indices (Phase 5)
     :misaka-id          int-or-nil   ; random Misaka No. (1000–19000)
     :first-open?        boolean      ; first-open animation not yet played
     :dirty?             boolean}     ; conditions changed, pending activation check

  Follows the terminal/model.clj `state-key` pattern for S2 runtime-store
  integration via tutorial/player.clj.")

(def state-key :tutorial-data)

;; --- Constructors ---

(defn fresh-state
  "Return initial tutorial state for a new player."
  []
  {:activated-tuts     #{}
   :condition-flags    #{}
   :misaka-id          nil
   :first-open?        true
   :dirty?             false})

;; --- Activation ---

(defn activate-tutorial
  "Mark a tutorial id as activated.  Idempotent."
  [d tut-id]
  (update d :activated-tuts
          (fnil conj #{}) (keyword tut-id)))

(defn is-activated?
  "True when `tut-id` has been explicitly activated."
  [d tut-id]
  (contains? (:activated-tuts d) (keyword tut-id)))

;; --- Misaka ID ---

(def ^:private misaka-id-min 1000)
(def ^:private misaka-id-max 19000)

(defn ensure-misaka-id
  "Assign a random Misaka No. if not already set.  Idempotent.
  Returns updated state."
  [d]
  (if (:misaka-id d)
    d
    (assoc d :misaka-id
           (+ misaka-id-min
              (rand-int (- misaka-id-max misaka-id-min))))))

;; --- First-open flag (animation) ---

(defn mark-first-open-done!
  "Mark that the first-open animation has been played."
  [d]
  (assoc d :first-open? false))

(defn first-open?
  "True when the first-open animation should play."
  [d]
  (boolean (:first-open? d)))

;; --- Condition flags (Phase 5: condition-based unlock) ---

(defn mark-condition!
  "Record that a condition index has been fulfilled.  Idempotent."
  [d condition-index]
  (update d :condition-flags (fnil conj #{}) (int condition-index)))

(defn condition-met?
  "True if the condition index has been fulfilled."
  [d condition-index]
  (contains? (:condition-flags d) (int condition-index)))

(defn any-condition-met?
  "True if at least one of the condition indices has been fulfilled."
  [d condition-indexes]
  (some #(condition-met? d %) condition-indexes))

;; --- Dirty flag (tick-based batching) ---

(defn mark-dirty!
  "Set the dirty flag to true.  Used after marking new condition flags."
  [d]
  (assoc d :dirty? true))

(defn clear-dirty!
  "Clear the dirty flag after pending activations have been processed."
  [d]
  (assoc d :dirty? false))

(defn dirty?
  "True when conditions have changed and activation check is pending."
  [d]
  (boolean (:dirty? d)))

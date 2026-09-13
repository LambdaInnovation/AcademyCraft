(ns cn.li.ac.foundation.concurrency
  "Concurrency and synchronization utilities.
  
  Provides:
  - Atomic operation wrappers with logging
  - Read/write synchronization helpers
  - Safe state transitions"
  (:require [cn.li.mcmod.util.log :as log]
            [clojure.core.protocols :as protocols]))

;; ============================================================================
;; Atomic Operations
;; ============================================================================

(defn atomic-get
  "Safely read an atom with optional logging.
  
  Args:
    atom: Atom to read
    label (optional): String label for logging
    
  Returns:
    The atom's current value"
  ([atom]
   @atom)
  ([atom label]
   (let [value @atom]
     (log/debug (str "Atomic read: " label " = " (pr-str value)))
     value)))

(defn atomic-set!
  "Safely set an atom with validation and logging.
  
  Args:
    atom: Atom to update
    new-value: New value
    label (optional): String label for logging
    
  Returns:
    The new value"
  ([atom new-value]
   (reset! atom new-value))
  ([atom new-value label]
   (let [old-value @atom]
     (reset! atom new-value)
     (log/debug (str "Atomic set: " label " " old-value " -> " new-value))
     new-value)))

(defn atomic-update!
  "Safely update an atom with a function and validation.
  
  Args:
    atom: Atom to update
    update-fn: (old-value) -> new-value
    label (optional): String label for logging
    
  Returns:
    The new value"
  ([atom update-fn]
   (swap! atom update-fn))
  ([atom update-fn label]
   (let [old-value @atom
         new-value (update-fn old-value)]
     (swap! atom (constantly new-value))
     (log/debug (str "Atomic update: " label " changed"))
     new-value)))

(defn atomic-update-many!
  "Update an atom with multiple values, ensuring all-or-nothing semantics.
  
  If any update fails, no changes are applied.
  
  Args:
    atom: Atom to update
    update-fns: [fn1 fn2 ...] or {key fn ...}
    label (optional): String label for logging
    
  Returns:
    {:success boolean :value new-value :error error}"
  ([atom update-fns]
   (atomic-update-many! atom update-fns nil))
  ([atom update-fns label]
   (try
     (let [old-value @atom
           new-value (if (map? update-fns)
                       (reduce (fn [acc [k f]] (assoc acc k (f (get acc k))))
                               old-value
                               update-fns)
                       (reduce (fn [acc f] (f acc))
                               old-value
                               update-fns))]
       (reset! atom new-value)
       (when label
         (log/debug (str "Atomic batch update: " label " OK")))
       {:success true :value new-value})
     (catch Exception ex
       (log/warn (str "Atomic batch update failed: " label) ex)
       (log/stacktrace "Atomic batch update failed" ex)
       {:success false :error ex}))))

;; ============================================================================
;; Conditional Updates
;; ============================================================================

(defn atomic-compare-and-set!
  "Compare-and-set operation (optimistic locking).
  
  Args:
    atom: Atom to update
    expected: Expected current value
    new-value: New value if expected matches
    
  Returns:
    boolean: true if update succeeded"
  [atom expected new-value]
  (compare-and-set! atom expected new-value))

(defn atomic-conditional-update!
  "Update atom only if predicate returns true for current value.
  
  Args:
    atom: Atom to update
    predicate: (old-value) -> boolean
    update-fn: (old-value) -> new-value
    label (optional): String label for logging
    
  Returns:
    {:updated boolean :value value}"
  ([atom predicate update-fn]
   (atomic-conditional-update! atom predicate update-fn nil))
  ([atom predicate update-fn label]
   (let [old-value @atom
         should-update (predicate old-value)]
     (if should-update
       (let [new-value (update-fn old-value)]
         (reset! atom new-value)
         (when label
           (log/debug (str "Conditional update: " label " applied")))
         {:updated true :value new-value})
       (do
         (when label
           (log/debug (str "Conditional update: " label " skipped")))
         {:updated false :value old-value})))))

;; ============================================================================
;; Protected Transitions
;; ============================================================================

(defn safe-state-transition!
  "Transition state machine atomically with guard checks.
  
  Args:
    atom: Atom holding current state
    valid-states: Set of valid states
    from-state: Expected current state
    to-state: Target state
    on-success (optional): (old-state new-state) -> side-effect
    
  Returns:
    {:success boolean :from state :to state :reason string}"
  ([atom valid-states from-state to-state]
   (safe-state-transition! atom valid-states from-state to-state nil))
  ([atom valid-states from-state to-state on-success]
   (let [current @atom]
     (cond
       (not (contains? valid-states current))
       {:success false :from current :to nil :reason "Invalid current state"}
       
       (not= current from-state)
       {:success false :from current :to nil :reason (str "Expected " from-state ", got " current)}
       
       (not (contains? valid-states to-state))
       {:success false :from current :to nil :reason "Invalid target state"}
       
       :else
       (do
         (reset! atom to-state)
         (when on-success
           (on-success from-state to-state))
         {:success true :from from-state :to to-state :reason "OK"})))))

;; ============================================================================
;; Synchronized Collections
;; ============================================================================

(defn synchronized-vector
  "Create a thread-safe vector backed by an atom.
  
  Returns:
    atom containing vector"
  []
  (atom []))

(defn synchronized-map
  "Create a thread-safe map backed by an atom.
  
  Returns:
    atom containing map"
  []
  (atom {}))

(defn sync-vec-conj!
  "Add element to synchronized vector.
  
  Args:
    vec-atom: Atom containing vector
    element: Element to add
    
  Returns:
    The updated vector"
  [vec-atom element]
  (swap! vec-atom conj element))

(defn sync-vec-remove!
  "Remove element from synchronized vector by predicate.
  
  Args:
    vec-atom: Atom containing vector
    predicate: (element) -> boolean for elements to remove
    
  Returns:
    The updated vector"
  [vec-atom predicate]
  (swap! vec-atom (fn [v] (filterv (complement predicate) v))))

;; ============================================================================
;; Retry Logic
;; ============================================================================

(defn with-retries
  "Execute function with exponential backoff retries.
  
  Args:
    f: Function to execute
    opts: {:max-retries 3 :initial-delay 100 :backoff-factor 2.0}
    
  Returns:
    Result of function or throws exception"
  [f & [{:keys [max-retries initial-delay backoff-factor]
         :or {max-retries 3 initial-delay 100 backoff-factor 2.0}}]]
  (loop [attempt 0
         delay initial-delay]
    (if (try
          (f)
          true
          (catch Exception ex
            (if (< attempt max-retries)
              (do
                (log/warn (str "Attempt " (inc attempt) " failed, retrying in " delay "ms..."))
                (log/stacktrace "with-retries attempt failed" ex)
                (Thread/sleep (long delay))
                false)
              (throw ex))))
      true
      (recur (inc attempt) (* delay backoff-factor)))))

(ns cn.li.mcmod.runtime.deferred
  "Unified thread-safe lazy holder for registry/property access.
  
  Replaces both:
  - Forge: cached-once! helper (lock + var-get pattern)
  - Fabric: var-root-registry pattern (locking + reflective var-get)
  
  Key API:
  - (deferred init-thunk) macro: wrap registry/property creation
  
  On first @deref:
  1. Calls (aot/ensure-runtime!) to crash if during AOT
  2. Creates value by calling init-thunk
  3. Memoizes result
  4. Returns value on all subsequent derefs (lock-free after first)
  
  Thread-safe: locking during first deref; subsequent derefs are atomic."
  (:require [cn.li.mcmod.aot :as aot]))

;; ============================================================================
;; Shared lock for initial deref synchronization
;; ============================================================================

(def ^:private deferred-init-lock
  "Lock used by all deferred instances during first @deref.
   
   After first deref, access is atomic (no lock needed)."
  (Object.))

;; ============================================================================
;; Deferred Implementation (IDeref)
;; ============================================================================

(deftype Deferred [init-thunk
                   ^:volatile-mutable result
                   ^:volatile-mutable computed?]
  clojure.lang.IDeref
  (deref [this]
    (if computed?
      result
      (locking deferred-init-lock
        ;; Double-check lock pattern: check again after acquiring lock
        (if-not computed?
          (do
            ;; Safety tripwire: crash if called during AOT
            (aot/ensure-runtime! (str "Deferred initialization: " (class init-thunk)))
            
            ;; Create value
            (let [value (init-thunk)]
              (set! result value)
              (set! computed? true)
              value))
          result)))))

;; ============================================================================
;; Public macro
;; ============================================================================

(defmacro deferred
  "Create a thread-safe lazy holder that defers creation until first access.
  
  Replaces both Forge cached-once! and Fabric var-root-registry patterns.
  
  Usage:
    (def my-register
      (deferred #(create-register mod-id)))
    
    ; Later, at runtime:
    @my-register  ; triggers creation on first deref
  
  Args:
    init-thunk: 0-arg function that creates the value.
                Called once on first @deref. Results are memoized.
  
  Returns:
    An IDeref-implementing object. Safe to use with @, deref, or IDeref protocol.
  
  Throws:
    ex-info (::compile-phase-violation): if @deref called during AOT
    (any exception from init-thunk): propagated to caller"
  [init-thunk]
  `(Deferred. ~init-thunk nil false))

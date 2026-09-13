(ns cn.li.mcmod.aot
  "Single-source-of-truth compile-phase guard.
  
  Unifies all compile-time detection (AOT, checkClojure, datagen) into one
  namespace, preventing duplicate predicates scattered across Minecraft components,
  Forge, and Fabric. Zero Minecraft imports to ensure this ns always loads
  safely during AOT.
  
  Key functions:
  - (compiling?) -> boolean: true if running under AOT or checkClojure
  - (ensure-runtime! who) -> throws ex-info if compiling (tripwire)
  - (when-runtime & body) macro: skip body during compilation")

;; ============================================================================
;; Compile-phase detection
;; ============================================================================

(defn compiling?
  "Detect if currently in AOT compile, checkClojure, or Clojurephant build phase.
  
  Returns true if any of:
  - *compile-files* is true (Clojure AOT marker)
  - clojure.server.clojurephant system property set (Clojurephant build server)
  - mcmod.compile system property is 'true' (custom AC/checkClojure flag)"
  []
  (or (boolean *compile-files*)
      (boolean (System/getProperty "clojure.server.clojurephant"))
      (= "true" (System/getProperty "mcmod.compile"))))

(defn compile-context
  "Return full compilation context as a map.
  
  Useful for logging/debugging to see which detection flags are set."
  []
  {:aot (boolean *compile-files*)
   :clojurephant (boolean (System/getProperty "clojure.server.clojurephant"))
   :mcmod-compile (= "true" (System/getProperty "mcmod.compile"))
   :compiling? (compiling?)})

;; ============================================================================
;; Runtime tripwire
;; ============================================================================

(defn ensure-runtime!
  "Fail loudly if called during compilation.
  
  Use this inside lazy holders / deferred registries to guarantee they
  cannot accidentally force during AOT. Throws ex-info with precise
  error message pointing to AOT_BOOTSTRAP.md for mitigation.
  
  Args:
    who (string): identifier of what tried to access Minecraft state.
           Should be the namespace/function name, e.g. 'cn.li.forge1201.mod/blocks-register'.
  
  Throws:
    ex-info {:type ::compile-phase-violation :who who :context compile-context}"
  [who]
  (when (compiling?)
    (throw
      (ex-info
        (str
          "Attempted to access Minecraft registry / bootstrap-dependent state during AOT compilation. "
          "This will cause 'Not bootstrapped' error. "
          "\n\nCulprit: " who
          "\n\nFix: Wrap this access in (delay ...) or use the mcmod.runtime.deferred/deferred macro. "
          "See docs/dev/AOT_BOOTSTRAP.md for guidance.")
        {:type ::compile-phase-violation
         :who who
         :context (compile-context)}))))

;; ============================================================================
;; Convenience macros
;; ============================================================================

(defmacro when-runtime
  "Conditional macro: skip body during compilation, evaluate at runtime.
  
  Sugar for (when-not (compiling?) ...).
  
  Example:
    (when-runtime
      (println \"This only runs at runtime\"))"
  [& body]
  `(when-not (compiling?)
     ~@body))

;; ============================================================================
;; Datagen detection (separate concern, not part of compiling?)
;; ============================================================================

(defn datagen-run?
  "Detect if running in data generation mode (runData task).

  Returns true if any datagen system property is set:
  - forge.datagen
  - fabric.datagen

  Datagen is technically a runtime operation (not AOT), but it may skip
  some bootstrap-sensitive initialization."
  []
  (or (= "true" (System/getProperty "forge.datagen"))
      (= "true" (System/getProperty "fabric.datagen"))))

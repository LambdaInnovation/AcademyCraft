(ns bad-top-level-registry
  "Intentional AOT bootstrap violation for verifyAotBootstrapSafetyNegativeFixture.")

;; Top-level registry access — must be caught by aot_safety.clj
(def bad-stone Blocks/STONE)

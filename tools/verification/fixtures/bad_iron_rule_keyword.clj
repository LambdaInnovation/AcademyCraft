(ns bad-iron-rule-fixture)

;; Intentional iron-law-1 violation for verifyAotIronRulesNegativeFixture.
(defn bad-keyword-as-ifn [rows]
  (map :key rows))

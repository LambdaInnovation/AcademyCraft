(ns cn.li.mcmod.integration.energy-conversion
  "Platform-neutral energy conversion definitions.

  This namespace defines conversion formulas between different energy systems:
  - content energy unit - content-owned internal energy unit
  - FE (Forge Energy) - Forge mod's energy standard
  - EU (Energy Units) - Industrial Craft 2's energy standard

  Conversion rates are configuration-based and platform-agnostic.
  No Minecraft or platform-specific imports allowed.

  These constants and functions are used by both Forge and Fabric
  platform layers to implement energy adapter logic.")

;; ============================================================================
;; Conversion Rate Configuration
;; ============================================================================

(def default-fe-to-content-rate
  "Default Forge Energy to content energy conversion rate.
  
  1 FE (Forge Energy) = X content energy units.
  
  This constant defines the external rate: FE per 1 content energy unit.
  
  Default: 4.0 (1 content energy unit = 4 FE)."
  4.0)

(def default-eu-to-content-rate
  "Default IC2 Energy Unit to content energy conversion rate.
  
  1 EU (Energy Unit) = X content energy units.
  
  This constant defines the rate: EU per 1 content energy unit.
  
  Default: 1.0 (1 content energy unit = 1 EU)."
  1.0)

;; ============================================================================
;; Conversion Functions (Pure Arithmetic)
;; ============================================================================

(defn content-to-fe
  "Convert content energy units to Forge Energy.
  
  Args:
    content-amount: Amount in content energy units
    fe-rate: Conversion rate (FE per 1 content energy unit) - use default-fe-to-content-rate if nil
  
  Returns:
    Amount in FE (Forge Energy), as integer"
  [content-amount & {:keys [rate] :or {rate default-fe-to-content-rate}}]
  (int (* (double content-amount) (double rate))))

(defn fe-to-content
  "Convert Forge Energy to content energy units.
  
  Args:
    fe-amount: Amount in FE (Forge Energy)
    fe-rate: Conversion rate (FE per 1 content energy unit) - use default-fe-to-content-rate if nil
  
  Returns:
    Amount in content energy units, as integer"
  [fe-amount & {:keys [rate] :or {rate default-fe-to-content-rate}}]
  (int (/ (double fe-amount) (double rate))))

(defn content-to-eu
  "Convert content energy units to IC2 Energy Units.
  
  Args:
    content-amount: Amount in content energy units
    eu-rate: Conversion rate (EU per 1 content energy unit) - use default-eu-to-content-rate if nil
  
  Returns:
    Amount in EU (Energy Units), as double"
  [content-amount & {:keys [rate] :or {rate default-eu-to-content-rate}}]
  (* (double content-amount) (double rate)))

(defn eu-to-content
  "Convert IC2 Energy Units to content energy units.
  
  Args:
    eu-amount: Amount in EU (Energy Units)
    eu-rate: Conversion rate (EU per 1 content energy unit) - use default-eu-to-content-rate if nil
  
  Returns:
    Amount in content energy units, as double"
  [eu-amount & {:keys [rate] :or {rate default-eu-to-content-rate}}]
  (/ (double eu-amount) (double rate)))

;; ============================================================================
;; Rate Validation (Pure Validation)
;; ============================================================================

(defn validate-conversion-rate
  "Validate a conversion rate value.
  
  Args:
    rate: The rate to validate
  
  Returns:
    true if valid (positive number), false otherwise"
  [rate]
  (and (number? rate) (pos? rate)))

(defn clamp-energy
  "Clamp an energy value to a valid range.
  
  Args:
    energy: Current energy
    min-energy: Minimum allowed (inclusive)
    max-energy: Maximum allowed (inclusive)
  
  Returns:
    Energy clamped to [min-energy, max-energy]"
  [energy min-energy max-energy]
  (let [e (double energy)
        min-e (double min-energy)
        max-e (double max-energy)]
    (max min-e (min e max-e))))

;; ============================================================================
;; Rate Calculation Helpers
;; ============================================================================

(defn calculate-transfer-amount
  "Calculate how much energy can be transferred given source and target constraints.
  
  This is used by adapters to determine transfer amounts that satisfy
  both source and destination constraints.
  
  Args:
    requested: Amount requested to transfer
    source-available: Amount available in source
    target-space: Amount of space in target
    rate-multiplier: Conversion multiplier (default 1.0 for same-unit transfers)
  
  Returns:
    Amount that can safely be transferred, as integer"
  [requested source-available target-space & {:keys [rate-multiplier] :or {rate-multiplier 1.0}}]
  (int (min (double requested)
            (double source-available)
            (/ (double target-space) (double rate-multiplier)))))

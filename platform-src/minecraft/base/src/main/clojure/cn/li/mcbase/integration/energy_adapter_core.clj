(ns cn.li.mcbase.integration.energy-adapter-core
  "Shared energy adapter core - implements platform-agnostic energy interface adapters.

  This namespace provides ~75% of energy adapter logic that can be shared
  between Forge and Fabric. It implements the common patterns used by both
  ForgeEnergyAdapter and potential Fabric energy adapters.

  Platform-specific parts (Forge capability registration, Fabric event listeners)
  remain in platform layers."
  (:require [cn.li.platform.neutral.integration-runtime :as energy-conversion]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.energy IEnergyCapable]))

;; ============================================================================
;; Energy Adapter State (Platform-Agnostic)
;; ============================================================================

(defn create-energy-adapter-state
  "Create the state map for an energy adapter.
  
  This encapsulates the adapter's configuration and allows platform-specific
  code to wrap it in their respective interface implementations.
  
  Args:
    content-energy: IEnergyCapable implementation supplied by content
    conversion-rate: Conversion rate (external units per 1 internal unit, e.g., 4.0)
    direction: Optional direction filtering (:receive, :extract, or nil for both)
  
  Returns:
    State map with {:content-energy, :conversion-rate, :direction}"
  [^IEnergyCapable content-energy conversion-rate & {:keys [direction]}]
  (when (energy-conversion/validate-conversion-rate conversion-rate)
    {:content-energy content-energy
     :conversion-rate (double conversion-rate)
     :direction direction}))

;; ============================================================================
;; Energy Query Functions (Platform-Agnostic)
;; ============================================================================

(defn get-energy-stored
  "Get current stored energy converted to target units.
  
  Converts from content internal units to FE (Forge Energy).
  
  Args:
    state: Adapter state map
  
  Returns:
    Energy stored in FE, as integer"
  [state]
  (when-let [content-energy (:content-energy state)]
    (let [content-stored (.getEnergyStored ^IEnergyCapable content-energy)
          conversion-rate (:conversion-rate state 1.0)]
      (energy-conversion/content-to-fe content-stored :rate conversion-rate))))

(defn get-max-energy-stored
  "Get maximum energy capacity converted to target units.
  
  Converts from content internal units to FE (Forge Energy).
  
  Args:
    state: Adapter state map
  
  Returns:
    Maximum energy capacity in FE, as integer"
  [state]
  (when-let [content-energy (:content-energy state)]
    (let [content-max (.getMaxEnergyStored ^IEnergyCapable content-energy)
          conversion-rate (:conversion-rate state 1.0)]
      (energy-conversion/content-to-fe content-max :rate conversion-rate))))

(defn can-receive?
  "Check if adapter can receive energy.
  
  Args:
    state: Adapter state map
  
  Returns:
    true if can receive, false otherwise"
  [state]
  (when-let [content-energy (:content-energy state)]
    (.canReceive ^IEnergyCapable content-energy)))

(defn can-extract?
  "Check if adapter can extract energy.
  
  Args:
    state: Adapter state map
  
  Returns:
    true if can extract, false otherwise"
  [state]
  (when-let [content-energy (:content-energy state)]
    (.canExtract ^IEnergyCapable content-energy)))

;; ============================================================================
;; Energy Transfer Functions (Platform-Agnostic)
;; ============================================================================

(defn receive-energy
  "Receive energy from external source, converting units as needed.
  
  Converts from FE (Forge Energy) to content internal units.
  Respects the adapter's direction constraints.
  
  Args:
    state: Adapter state map
    max-receive: Maximum FE to receive
    simulate: Boolean - if true, don't actually transfer
  
  Returns:
    Amount of FE actually received (or would be received if simulated), as integer"
  [state max-receive simulate]
  (when (and (can-receive? state) (not= :extract (:direction state)))
        (let [content-energy (:content-energy state)
          conversion-rate (:conversion-rate state 1.0)
          content-amount (energy-conversion/fe-to-content max-receive :rate conversion-rate)
          content-received (.receiveEnergy ^IEnergyCapable content-energy (int content-amount) simulate)
          fe-received (energy-conversion/content-to-fe content-received :rate conversion-rate)]
      (int fe-received))))

(defn extract-energy
  "Extract energy to external consumer, converting units as needed.
  
  Converts from FE (Forge Energy) to content internal units.
  Respects the adapter's direction constraints.
  
  Args:
    state: Adapter state map
    max-extract: Maximum FE to extract
    simulate: Boolean - if true, don't actually transfer
  
  Returns:
    Amount of FE actually extracted (or would be extracted if simulated), as integer"
  [state max-extract simulate]
  (when (and (can-extract? state) (not= :receive (:direction state)))
        (let [content-energy (:content-energy state)
          conversion-rate (:conversion-rate state 1.0)
          content-amount (energy-conversion/fe-to-content max-extract :rate conversion-rate)
          content-extracted (.extractEnergy ^IEnergyCapable content-energy (int content-amount) simulate)
          fe-extracted (energy-conversion/content-to-fe content-extracted :rate conversion-rate)]
      (int fe-extracted))))

;; ============================================================================
;; Capacity Calculation Helpers
;; ============================================================================

(defn calculate-receivable-energy
  "Calculate how much energy can be received (space available).
  
  Args:
    state: Adapter state map
  
  Returns:
    Maximum FE that can be received (max capacity - current stored), as integer"
  [state]
  (when (can-receive? state)
    (- (get-max-energy-stored state)
       (get-energy-stored state))))

(defn calculate-extractable-energy
  "Calculate how much energy can be extracted (amount stored).
  
  Args:
    state: Adapter state map
  
  Returns:
    Maximum FE that can be extracted (current stored), as integer"
  [state]
  (when (can-extract? state)
    (get-energy-stored state)))

;; ============================================================================
;; Validation and Error Handling
;; ============================================================================

(defn validate-adapter-state
  "Validate an adapter state map for correctness.
  
  Args:
    state: State map to validate
  
  Returns:
    Boolean - true if valid, false otherwise"
  [state]
  (when (map? state)
    (and (some? (:content-energy state))
         (number? (:conversion-rate state))
         (energy-conversion/validate-conversion-rate (:conversion-rate state)))))

(defn safe-receive-energy
  "Safely receive energy with error handling.
  
  Wraps receive-energy with exception handling and logging.
  
  Args:
    state: Adapter state map
    max-receive: Maximum FE to receive
    simulate: Boolean
  
  Returns:
    Amount received, or 0 on error"
  [state max-receive simulate]
  (try
    (or (receive-energy state max-receive simulate) 0)
    (catch Exception e
      (log/stacktrace "Error receiving energy:" e)
      0)))

(defn safe-extract-energy
  "Safely extract energy with error handling.
  
  Wraps extract-energy with exception handling and logging.
  
  Args:
    state: Adapter state map
    max-extract: Maximum FE to extract
    simulate: Boolean
  
  Returns:
    Amount extracted, or 0 on error"
  [state max-extract simulate]
  (try
    (or (extract-energy state max-extract simulate) 0)
    (catch Exception e
      (log/stacktrace "Error extracting energy:" e)
      0)))

(ns cn.li.ac.energy.domain.container
  "Pure energy container data model.
  
  Provides immutable energy container records with pure operations.
  No atoms, no side effects - fully testable."
  (:require [cn.li.ac.foundation.validation :as validation]))

;; ============================================================================
;; Energy Container Record
;; ============================================================================

(defrecord EnergyContainer
  [^double current      ; Current energy amount
   ^double max-capacity ; Maximum capacity
   ^double transfer-rate ; Max transfer per operation
   ^double efficiency   ; Transfer efficiency (0.0 - 1.0)
   ^long last-updated]) ; Timestamp of last change

(defn energy-container
  "Create a new energy container.
  
  Args:
    max-capacity: Maximum capacity (required)
    current: Current energy (default 0.0)
    transfer-rate: Max transfer per op (default capacity/10)
    efficiency: Transfer efficiency (default 1.0)
    
  Returns:
    EnergyContainer"
  ([max-capacity]
   (energy-container max-capacity 0.0))
  ([max-capacity current]
   (energy-container max-capacity current (/ max-capacity 10.0)))
  ([max-capacity current transfer-rate]
   (energy-container max-capacity current transfer-rate 1.0))
  ([max-capacity current transfer-rate efficiency]
   (when-not (and (pos? max-capacity)
                  (>= current 0.0)
                  (<= current max-capacity)
                  (pos? transfer-rate)
                  (> efficiency 0.0)
                  (<= efficiency 1.0))
     (throw (IllegalArgumentException. "energy-container: invalid max-capacity/current/transfer-rate/efficiency")))
   (->EnergyContainer
     (double current)
     (double max-capacity)
     (double transfer-rate)
     (double efficiency)
     (System/currentTimeMillis))))

(defn energy-container?
  "Check if value is an energy container.
  
  Args:
    v: Value to check
    
  Returns:
    boolean"
  [v]
  (instance? EnergyContainer v))

;; ============================================================================
;; Query Operations (Pure)
;; ============================================================================

(defn get-current
  "Get current energy amount.
  
  Args:
    container: EnergyContainer
    
  Returns:
    double"
  [^EnergyContainer container]
  (:current container))

(defn get-capacity
  "Get maximum capacity.
  
  Args:
    container: EnergyContainer
    
  Returns:
    double"
  [^EnergyContainer container]
  (:max-capacity container))

(defn get-remaining
  "Get remaining capacity (max - current).
  
  Args:
    container: EnergyContainer
    
  Returns:
    double"
  [^EnergyContainer container]
  (- (:max-capacity container) (:current container)))

(defn get-percentage
  "Get energy percentage (0.0 - 1.0).
  
  Args:
    container: EnergyContainer
    
  Returns:
    double"
  [^EnergyContainer container]
  (double (/ (:current container) (:max-capacity container))))

(defn is-full?
  "Check if container is full.
  
  Args:
    container: EnergyContainer
    
  Returns:
    boolean"
  [^EnergyContainer container]
  (>= (:current container) (:max-capacity container)))

(defn is-empty?
  "Check if container is empty.
  
  Args:
    container: EnergyContainer
    
  Returns:
    boolean"
  [^EnergyContainer container]
  (<= (:current container) 0.0))

(defn has-energy?
  "Check if container has any energy.
  
  Args:
    container: EnergyContainer
    amount: Required amount (optional, default 1.0)
    
  Returns:
    boolean"
  ([^EnergyContainer container]
   (> (:current container) 0.0))
  ([^EnergyContainer container ^double amount]
   (>= (:current container) amount)))

(defn can-accept?
  "Check if container can accept energy.
  
  Args:
    container: EnergyContainer
    amount: Amount to check (optional, default 1.0)
    
  Returns:
    boolean"
  ([^EnergyContainer container]
   (< (:current container) (:max-capacity container)))
  ([^EnergyContainer container ^double amount]
   (<= (+ (:current container) amount) (:max-capacity container))))

;; ============================================================================
;; Modification Operations (Pure - Return New Container)
;; ============================================================================

(defn receive-energy
  "Add energy to container (clamp at max).
  
  Args:
    container: EnergyContainer
    amount: Amount to add (must be positive)
    
  Returns:
    [new-container actually-received]
    
  Example:
    (receive-energy container 100.0)
    ; => [updated-container 95.0]  ; If some was lost
  "
  [^EnergyContainer container ^double amount]
  (when-not (pos? amount)
    (throw (IllegalArgumentException. "amount must be positive")))
  (let [current (:current container)
        max-cap (:max-capacity container)
        available-space (- max-cap current)
        received (min amount available-space)]
    [(assoc container :current (+ current received)
                      :last-updated (System/currentTimeMillis))
     received]))

(defn extract-energy
  "Remove energy from container (clamp at 0).
  
  Args:
    container: EnergyContainer
    amount: Amount to extract (must be positive)
    
  Returns:
    [new-container actually-extracted]
    
  Example:
    (extract-energy container 100.0)
    ; => [updated-container 75.0]  ; If only 75 available
  "
  [^EnergyContainer container ^double amount]
  (when-not (pos? amount)
    (throw (IllegalArgumentException. "amount must be positive")))
  (let [current (:current container)
        extracted (min amount current)]
    [(assoc container :current (- current extracted)
                      :last-updated (System/currentTimeMillis))
     extracted]))

(defn set-energy
  "Set energy to exact amount.
  
  Args:
    container: EnergyContainer
    amount: New energy amount (clamped to [0, capacity])
    
  Returns:
    new-container
    
  Example:
    (set-energy container 50.0)
  "
  [^EnergyContainer container ^double amount]
  (let [clamped (max 0.0 (min amount (:max-capacity container)))]
    (assoc container :current clamped
                     :last-updated (System/currentTimeMillis))))

(defn drain-completely
  "Remove all energy from container.
  
  Args:
    container: EnergyContainer
    
  Returns:
    [new-container amount-drained]
    
  Example:
    (drain-completely container)
    ; => [empty-container 50.0]
  "
  [^EnergyContainer container]
  (let [drained (:current container)]
    [(assoc container :current 0.0
                      :last-updated (System/currentTimeMillis))
     drained]))

(defn update-properties
  "Update container properties (transfer-rate, efficiency).
  
  Args:
    container: EnergyContainer
    updates: Map of {:transfer-rate :efficiency :max-capacity}
    
  Returns:
    new-container or validation error"
  [^EnergyContainer container updates]
  (let [result (merge container updates)]
    (if (and (pos? (:transfer-rate result))
             (> (:efficiency result) 0.0)
             (<= (:efficiency result) 1.0)
             (pos? (:max-capacity result)))
      (assoc result :last-updated (System/currentTimeMillis))
      {:valid false :errors ["Invalid energy container properties"]})))

;; ============================================================================
;; Transfer Operations (Pure)
;; ============================================================================

(defrecord EnergyTransfer
  [source-id dest-id amount lost efficiency timestamp])

(defn transfer-energy
  "Transfer energy between containers.
  
  Args:
    source: Source EnergyContainer
    dest: Destination EnergyContainer
    amount: Amount to transfer (optional, use max transfer-rate)
    
  Returns:
    {:source new-source
     :dest new-dest
     :transferred amount-actually-transferred
     :lost amount-lost-to-efficiency
     :transfer-record EnergyTransfer}
     
  Example:
    (transfer-energy source dest 100.0)
    ; => {:source ... :dest ... :transferred 100.0 :lost 0.0}
  "
  ([^EnergyContainer source ^EnergyContainer dest]
   (transfer-energy source dest (:transfer-rate source)))
  ([^EnergyContainer source ^EnergyContainer dest ^double amount]
   (when-not (pos? amount)
    (throw (IllegalArgumentException. "amount must be positive")))
   (let [;; Amount that can be extracted from source
         extractable (min amount (:current source))
         ;; Amount that can be received by dest
         available-space (- (:max-capacity dest) (:current dest))
         ;; Apply efficiency loss
         efficiency (:efficiency dest)
         transferable (min extractable (/ available-space efficiency))
         ;; Calculate what actually arrives (with efficiency loss)
         arrives (Math/floor (* transferable efficiency))
         lost (- transferable arrives)
         
         new-source (assoc source :current (- (:current source) transferable)
                                  :last-updated (System/currentTimeMillis))
         new-dest (assoc dest :current (+ (:current dest) arrives)
                              :last-updated (System/currentTimeMillis))
         
         transfer-rec (->EnergyTransfer
                        (str (java.util.UUID/randomUUID)) ; placeholder ID
                        (str (java.util.UUID/randomUUID))
                        transferable
                        lost
                        efficiency
                        (System/currentTimeMillis))]
     
     {:source new-source
      :dest new-dest
      :transferred arrives
      :lost lost
      :transfer-record transfer-rec})))

;; ============================================================================
;; Serialization
;; ============================================================================

(defn container->map
  "Convert container to plain map for serialization.
  
  Args:
    container: EnergyContainer
    
  Returns:
    {:current :max-capacity :transfer-rate :efficiency :last-updated}"
  [^EnergyContainer container]
  {:current (:current container)
   :max-capacity (:max-capacity container)
   :transfer-rate (:transfer-rate container)
   :efficiency (:efficiency container)
   :last-updated (:last-updated container)})

(defn map->container
  "Convert map to container (from serialization).
  
  Args:
    m: Map with energy container data
    
  Returns:
    EnergyContainer or validation error"
  [m]
  (if (and (:max-capacity m) (:current m))
    (try
      (->EnergyContainer
        (double (:current m))
        (double (:max-capacity m))
        (double (or (:transfer-rate m) (/ (:max-capacity m) 10.0)))
        (double (or (:efficiency m) 1.0))
        (long (or (:last-updated m) (System/currentTimeMillis))))
      (catch Exception e
        {:valid false :errors [(str "Failed to deserialize container: " (.getMessage e))]}))
    {:valid false :errors ["Missing required fields: :max-capacity :current"]}))

(defn validate-container
  "Validate energy container consistency.
  
  Args:
    container: EnergyContainer
    
  Returns:
    {:valid boolean :errors [string]}"
  [^EnergyContainer container]
  (let [errors (cond-> []
                 (not (instance? EnergyContainer container))
                 (conj "Not an EnergyContainer")
                 
                 (not (pos? (:max-capacity container)))
                 (conj "Max capacity must be positive")
                 
                 (< (:current container) 0.0)
                 (conj "Current energy cannot be negative")
                 
                 (> (:current container) (:max-capacity container))
                 (conj "Current energy exceeds max capacity")
                 
                 (not (pos? (:transfer-rate container)))
                 (conj "Transfer rate must be positive")
                 
                 (or (< (:efficiency container) 0.0)
                     (> (:efficiency container) 1.0))
                 (conj "Efficiency must be 0.0-1.0"))]
    {:valid (empty? errors) :errors errors}))

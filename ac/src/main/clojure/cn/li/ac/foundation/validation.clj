(ns cn.li.ac.foundation.validation
  "Data validation utilities for AC module.
  
  Centralizes validation logic for:
  - Position ranges
  - Energy values
  - Network state
  - Configuration"
  (:require [cn.li.ac.foundation.position :as pos]
            [cn.li.ac.foundation.vblock :as vb]))

;; ============================================================================
;; Position Validation
;; ============================================================================

(defn valid-position?
  "Check if position coordinates are valid.
  
  Args:
    x, y, z: Coordinates
    
  Returns:
    boolean: true if all are finite numbers"
  [x y z]
  (pos/valid-position? x y z))

(defn valid-world-height?
  "Check if Y coordinate is within Minecraft world bounds.
  
  Minecraft 1.18+ uses dynamic height: Y in [-64, 320).
  This function uses the general Minecraft bounds.
  
  Args:
    y (int): Y coordinate
    
  Returns:
    boolean"
  [y]
  (and (>= y -64) (< y 320)))

(defn clamp-coordinates
  "Clamp coordinates to world bounds.
  
  Args:
    x, y, z: Coordinates
    
  Returns:
    [x y z]: Clamped coordinates"
  [x y z]
  [x
   (-> y (max -64) (min 319))
   z])

;; ============================================================================
;; Energy Validation
;; ============================================================================

(defn valid-energy?
  "Check if energy value is valid (non-negative number).
  
  Args:
    energy (number): Energy amount
    
  Returns:
    boolean"
  [energy]
  (and (number? energy)
       (>= energy 0)))

(defn valid-energy-capacity?
  "Check if energy capacity is valid (positive number).
  
  Args:
    capacity (number): Energy capacity
    
  Returns:
    boolean"
  [capacity]
  (and (number? capacity)
       (> capacity 0)))

(defn valid-energy-bounds?
  "Check if current energy is within capacity.
  
  Args:
    current (number): Current energy
    capacity (number): Max energy capacity
    
  Returns:
    boolean"
  [current capacity]
  (and (valid-energy? current)
       (valid-energy-capacity? capacity)
       (<= current capacity)))

(defn clamp-energy
  "Clamp energy to valid bounds [0, capacity].
  
  Args:
    current (number): Current energy
    capacity (number): Max energy capacity
    
  Returns:
    number: Clamped energy"
  [current capacity]
  (-> current (max 0) (min capacity)))

;; ============================================================================
;; VBlock Validation
;; ============================================================================

(defn valid-vblock?
  "Check if VBlock has valid structure.
  
  Args:
    vblock: Object to check
    
  Returns:
    boolean"
  [vblock]
  (and (vb/vblock? vblock)
       (valid-position? (:x vblock) (:y vblock) (:z vblock))
       (keyword? (:block-type vblock))
       (boolean? (:ignore-chunk vblock))))

(defn valid-vblock-type?
  "Check if VBlock type is one of the known types.
  
  Args:
    vblock: VBlock record
    
  Returns:
    boolean"
  [vblock]
  (contains? #{:matrix :node :node-conn :generator :receiver}
             (:block-type vblock)))

;; ============================================================================
;; String Validation
;; ============================================================================

(defn valid-ssid?
  "Check if SSID string is valid.
  
  Args:
    ssid (string): Network SSID
    
  Returns:
    boolean"
  [ssid]
  (and (string? ssid)
       (< 0 (count ssid) 32)))

(defn valid-password?
  "Check if password string is valid.
  
  Args:
    password (string): Network password
    
  Returns:
    boolean: true if empty or 6-32 chars"
  [password]
  (or (empty? password)
      (and (string? password)
           (>= (count password) 6)
           (<= (count password) 32))))

;; ============================================================================
;; Composite Validation
;; ============================================================================

(defn validate-coordinates
  "Validate coordinates and return error details if invalid.
  
  Args:
    x, y, z: Coordinates
    
  Returns:
    {:valid boolean
     :errors [string]}: Validation result"
  [x y z]
  (let [errors (cond-> []
                  (not (valid-position? x y z))
                  (conj "Coordinates are not finite numbers")
                  (not (valid-world-height? y))
                  (conj (str "Y coordinate " y " is outside world bounds [-64, 320)")))]
    {:valid (empty? errors)
     :errors errors}))

(defn validate-energy
  "Validate energy values.
  
  Args:
    current (number): Current energy
    capacity (number): Max capacity
    
  Returns:
    {:valid boolean
     :errors [string]}: Validation result"
  [current capacity]
  (let [errors (cond-> []
                  (not (valid-energy? current))
                  (conj "Current energy is negative or non-numeric")
                  (not (valid-energy-capacity? capacity))
                  (conj "Capacity must be positive")
                  (and (valid-energy? current) (valid-energy-capacity? capacity)
                       (not (valid-energy-bounds? current capacity)))
                  (conj "Current energy exceeds capacity"))]
    {:valid (empty? errors)
     :errors errors}))

(defn validate-vblock
  "Full validation of a VBlock.
  
  Args:
    vblock: Object to validate
    
  Returns:
    {:valid boolean
     :errors [string]}: Validation result"
  [vblock]
  (let [errors (cond-> []
                  (not (vb/vblock? vblock))
                  (conj "Not a VBlock record")
                  (vb/vblock? vblock)
                  (fn [errs]
                    (let [coords-result (validate-coordinates (:x vblock) (:y vblock) (:z vblock))]
                      (into errs (:errors coords-result))))
                  (not (valid-vblock-type? vblock))
                  (conj (str "Unknown block type: " (:block-type vblock))))]
    {:valid (empty? errors)
     :errors errors}))

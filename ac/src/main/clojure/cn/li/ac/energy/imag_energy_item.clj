(ns cn.li.ac.energy.imag-energy-item
  "ImagEnergyItem contract — Clojure version of the original Java interface.

  Defines the contract for items that can store and transfer energy.
  Energy item configs are plain function maps with:
    :get-max-energy  — (fn [] -> double)
    :get-bandwidth   — (fn [] -> double)"
  (:require [cn.li.mcmod.util.log :as log]))

;; ============================================================================
;; Key set documentation
;; ============================================================================

(def imag-energy-item-keys
  "Keys required by an ImagEnergyItem function map."
  [:get-max-energy :get-bandwidth])

;; ============================================================================
;; Wrapper functions
;; ============================================================================

(defn get-max-energy
  "Get the maximum energy capacity of this item.
  Returns: double - maximum energy in IF"
  [item]
  ((:get-max-energy item)))

(defn get-bandwidth
  "Get the energy transfer bandwidth (max per operation).
  Returns: double - bandwidth in IF per operation"
  [item]
  ((:get-bandwidth item)))

;; ============================================================================
;; Type check
;; ============================================================================

(defn imag-energy-item?
  "Check if object satisfies ImagEnergyItem contract
  (has the required keys with callable values)."
  [obj]
  (and (map? obj)
       (fn? (:get-max-energy obj))
       (fn? (:get-bandwidth obj))))

;; ============================================================================
;; Helper Functions
;; ============================================================================

(defn validate-item-energy
  "Validate energy item parameters"
  [max-energy bandwidth]
  (when (<= max-energy 0.0)
    (throw (IllegalArgumentException. "Max energy must be positive")))
  (when (<= bandwidth 0.0)
    (throw (IllegalArgumentException. "Bandwidth must be positive")))
  true)

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-imag-energy-item! []
  (log/debug "ImagEnergyItem protocol initialized"))

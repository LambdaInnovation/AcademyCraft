(ns cn.li.ac.block.energy-converter.wireless-impl
  "Wireless energy integration for energy converters.

  Allows energy converters to act as wireless generators (provide energy)
  or wireless receivers (receive energy) when linked to wireless nodes."
  (:require [cn.li.ac.wireless.api :as wireless-api]
            [cn.li.ac.integration.block.energy-converter.config :as ec-config]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessGenerator IWirelessReceiver]))


;; ============================================================================
;; Wireless Generator Implementation
;; ============================================================================

(defn create-wireless-generator
  "Create IWirelessGenerator implementation for energy converter.

  Args:
    _tile-entity - The ScriptedBlockEntity instance (unused, kept for API consistency)
    get-state-fn - Function to get current state map
    set-state-fn - Function to update state map

  Returns:
    IWirelessGenerator implementation"
  [_tile-entity get-state-fn set-state-fn]
  (reify IWirelessGenerator
    (getEnergy [_]
      (let [state (get-state-fn)]
        (double (get state :energy 0.0))))

    (setEnergy [_ energy]
      (let [state (get-state-fn)]
          (let [prev (double (get state :energy 0.0))
                delta (- (double energy) prev)]
            (set-state-fn
              (cond-> (assoc state :energy (double energy))
                ;; Track how much was extracted from this generator
                (neg? delta) (update :wireless-out-accumulator #(+ (or % 0.0) (- delta))))))))

    (getProvidedEnergy [_ req]
      ;; Provide energy from internal buffer and deduct the provided amount
      (let [state (get-state-fn)
            current-energy (double (get state :energy 0.0))
            bandwidth (double (ec-config/transfer-bandwidth))
            can-provide (min current-energy bandwidth req)]
        (when (pos? can-provide)
          (set-state-fn (assoc state :energy (- current-energy can-provide))))
        (double can-provide)))

    (getGeneratorBandwidth [_]
      (let [state (get-state-fn)]
        (double (ec-config/transfer-bandwidth))))))

;; ============================================================================
;; Wireless Receiver Implementation
;; ============================================================================

(defn create-wireless-receiver
  "Create IWirelessReceiver implementation for energy converter.

  Args:
    _tile-entity - The ScriptedBlockEntity instance (unused, kept for API consistency)
    get-state-fn - Function to get current state map
    set-state-fn - Function to update state map
    opts - optional map:
      :after-inject! — (fn [^double accepted-if]) called after a positive wireless inject (for UI sync rate).

  Returns:
    IWirelessReceiver implementation"
  ([tile-entity get-state-fn set-state-fn]
   (create-wireless-receiver tile-entity get-state-fn set-state-fn nil))
  ([_tile-entity get-state-fn set-state-fn opts]
   (let [after-inject! (when (map? opts) (:after-inject! opts))
         max-energy-fn (:max-energy opts)
         bandwidth-fn (:bandwidth opts)]
     (reify IWirelessReceiver
       (getRequiredEnergy [_]
         ;; How much energy we need
         (let [state (get-state-fn)
               current-energy (double (get state :energy 0.0))
               max-energy (double (or (when max-energy-fn (max-energy-fn))
                                      (ec-config/energy-capacity)))
               space (max 0.0 (- max-energy current-energy))
               bandwidth (double (or (when bandwidth-fn (bandwidth-fn))
                                     (ec-config/transfer-bandwidth)))]
           (double (min space bandwidth))))

       (injectEnergy [_ amt]
         ;; Receive energy into internal buffer
         (let [state (get-state-fn)
               current-energy (double (get state :energy 0.0))
               max-energy (double (or (when max-energy-fn (max-energy-fn))
                                      (ec-config/energy-capacity)))
               space (max 0.0 (- max-energy current-energy))
               to-inject (min amt space)
               leftover (- amt to-inject)]
          (set-state-fn (-> state
                            (assoc :energy (+ current-energy to-inject))
                            (update :wireless-in-accumulator #(+ (or % 0.0) to-inject))))
           (when (and after-inject! (pos? to-inject))
             (try (after-inject! (double to-inject)) (catch Exception _ nil)))
           (double leftover)))

       (pullEnergy [_ amt]
         ;; Pull energy from internal buffer
         (let [state (get-state-fn)
               current-energy (double (get state :energy 0.0))
               to-pull (min amt current-energy)]
           (set-state-fn (assoc state :energy (- current-energy to-pull)))
           (double to-pull)))

       (getReceiverBandwidth [_]
         (let [state (get-state-fn)]
           (double (or (when bandwidth-fn (bandwidth-fn))
                       (ec-config/transfer-bandwidth)))))))))

;; ============================================================================
;; Wireless Integration Helpers
;; ============================================================================

(defn get-wireless-mode
  "Get wireless mode: 'generator' or 'receiver'."
  [state]
  (get state :wireless-mode "generator"))

(defn link-to-node!
  "Link converter to a wireless node.

  Args:
    tile-entity - The converter tile entity
    node-tile - The wireless node tile entity
    password - Node password (optional)
    state - Current converter state

  Returns:
    true if successfully linked"
  [tile-entity node-tile password state]
  (try
    (let [mode (get-wireless-mode state)
          need-auth false]
      (case mode
        "generator"
        (wireless-api/link-generator-to-node! tile-entity node-tile password need-auth)

        "receiver"
        (wireless-api/link-receiver-to-node! tile-entity node-tile password need-auth)

        false))
    (catch Exception e
      (log/stacktrace "Failed to link converter to wireless node" e)
      false)))

(defn unlink-from-node!
  "Unlink converter from its wireless node.

  Args:
    tile-entity - The converter tile entity
    state - Current converter state

  Returns:
    true if successfully unlinked"
  [tile-entity state]
  (try
    (let [mode (get-wireless-mode state)]
      (case mode
        "generator"
        (wireless-api/unlink-generator-from-node! tile-entity)

        "receiver"
        (wireless-api/unlink-receiver-from-node! tile-entity)

        false))
    (catch Exception e
      (log/stacktrace "Failed to unlink converter from wireless node" e)
      false)))

(defn is-linked?
  "Check if converter is linked to a wireless node.

  Args:
    tile-entity - The converter tile entity
    state - Current converter state

  Returns:
    true if linked"
  [tile-entity state]
  (let [mode (get-wireless-mode state)]
    (case mode
      "generator"
      (wireless-api/is-generator-linked? tile-entity)

      "receiver"
      (wireless-api/is-receiver-linked? tile-entity)

      false)))

(defn get-wireless-transfer-rate
  "Get current wireless energy transfer rate.

  Args:
    tile-entity - The converter tile entity
    state - Current converter state

  Returns:
    Transfer rate in IF/tick"
  [tile-entity state]
  (if (is-linked? tile-entity state)
    (let [mode (get-wireless-mode state)]
      (case mode
        "generator"
        ;; For generators, check how much energy was provided last tick
        (double (get state :wireless-transfer-rate 0.0))

        "receiver"
        ;; For receivers, check how much energy was received last tick
        (double (get state :wireless-transfer-rate 0.0))

        0.0))
    0.0))

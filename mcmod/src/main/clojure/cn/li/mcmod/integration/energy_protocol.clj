(ns cn.li.mcmod.integration.energy-protocol
  "Platform-neutral energy protocols for external mod integration.

  These protocols abstract external energy systems (Forge Energy, IC2 EU, etc.)
  so that content code can work with them without importing platform-specific classes.

  Expected map keys:
  - IExternalEnergyProvider: :can-provide-energy? (fn [] -> bool), :extract-external-energy (fn [amount simulate?] -> extracted)
  - IExternalEnergyReceiver: :can-receive-energy? (fn [] -> bool), :receive-external-energy (fn [amount simulate?] -> received)
  - IExternalEnergyStorage: :get-external-energy-stored (fn [] -> stored), :get-external-max-energy (fn [] -> max), :get-conversion-rate (fn [] -> rate)")

;; Wrapper functions for IExternalEnergyProvider

(defn can-provide-energy?
  [provider]
  ((:can-provide-energy? provider)))

(defn extract-external-energy
  [provider amount simulate?]
  ((:extract-external-energy provider) amount simulate?))

;; Wrapper functions for IExternalEnergyReceiver

(defn can-receive-energy?
  [receiver]
  ((:can-receive-energy? receiver)))

(defn receive-external-energy
  [receiver amount simulate?]
  ((:receive-external-energy receiver) amount simulate?))

;; Wrapper functions for IExternalEnergyStorage

(defn get-external-energy-stored
  [storage]
  ((:get-external-energy-stored storage)))

(defn get-external-max-energy
  [storage]
  ((:get-external-max-energy storage)))

(defn get-conversion-rate
  [storage]
  ((:get-conversion-rate storage)))

(ns cn.li.ac.block.role-impls
  "Clojure deftype implementations of mcmod Java interfaces.

  Each deftype wraps a ScriptedBlockEntity and delegates to its customState map.
  The customState map is the single source of truth for tile data (Design-3).

  WirelessGeneratorImpl → implements IWirelessGenerator
  WirelessReceiverImpl → implements IWirelessReceiver

  Note: WirelessMatrixImpl, WirelessNodeImpl, and ClojureEnergyImpl are defined
  in their respective block files (wireless_matrix/block.clj, wireless_node/block.clj)
  to avoid duplication."
  (:require [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.position :as pos])
  (:import [cn.li.acapi.wireless IWirelessGenerator IWirelessReceiver]))

;; ============================================================================
;; WirelessGeneratorImpl
;; Implements IWirelessGenerator for ScriptedBlockEntity whose customState stores :energy.
;; ============================================================================

(defn- assoc-energy
  "Return state with :energy set. Arity [energy state] for `(partial assoc-energy v)` in commit-transform! hot paths."
  [energy state]
  (assoc state :energy (double energy)))

(deftype WirelessGeneratorImpl [be]
  IWirelessGenerator

  (getEnergy [_]
    (let [state (or (platform-be/get-custom-state be) {})]
      (double (get state :energy 0.0))))

  (setEnergy [_ energy]
    (machine-runtime/commit-transform! be {} (partial assoc-energy energy)))

  (getProvidedEnergy [_ req]
    (let [state (or (platform-be/get-custom-state be) {})
          cur (double (get state :energy 0.0))
          bw (double (get state :gen-speed 0.0))
          max-out (if (pos? bw) bw req)
          actual (min (double req) cur max-out)]
      (when (pos? actual)
        (machine-runtime/commit-transform! be {} (partial assoc-energy (- cur actual))))
      (double actual)))

  (getGeneratorBandwidth [_]
    (let [state (or (platform-be/get-custom-state be) {})]
      (double (max 0.0 (double (get state :gen-speed 0.0))))))

  Object
  (toString [_]
    (str "WirelessGeneratorImpl@" (pos/block-pos be))))

(defn wireless-generator-factory
  "Named capability factory — avoids anonymous fn literal in init options,
  preventing local-var leakage into AOT compilation scopes."
  [be _side]
  (->WirelessGeneratorImpl be))

;; ============================================================================
;; WirelessReceiverImpl
;; Implements IWirelessReceiver for ScriptedBlockEntity whose customState stores :energy.
;; Used by ImagFusor and similar energy-consuming machines.
;; ============================================================================

(deftype WirelessReceiverImpl [be max-energy-fn bandwidth-fn]
  IWirelessReceiver

  (getRequiredEnergy [_]
    (let [state (or (platform-be/get-custom-state be) {})
          cur (double (get state :energy 0.0))
          max-e (double (max-energy-fn state))]
      (max 0.0 (- max-e cur))))

  (injectEnergy [_ amt]
    (let [state (or (platform-be/get-custom-state be) {})
          cur (double (get state :energy 0.0))
          max-e (double (max-energy-fn state))
          req (max 0.0 (double amt))
          space (- max-e cur)
          give (min req space)]
      (when (pos? give)
        (machine-runtime/commit-transform! be {} (partial assoc-energy (+ cur give))))
      (- req give)))

  (pullEnergy [_ amt]
    (let [state (or (platform-be/get-custom-state be) {})
          cur (double (get state :energy 0.0))
          req (max 0.0 (double amt))
          give (min req cur)]
      (when (pos? give)
        (machine-runtime/commit-transform! be {} (partial assoc-energy (- cur give))))
      (double give)))

  (getReceiverBandwidth [_]
    (let [state (or (platform-be/get-custom-state be) {})]
      (double (max 0.0 (double (bandwidth-fn state))))))

  Object
  (toString [_]
    (str "WirelessReceiverImpl@" (pos/block-pos be))))

(defn wireless-receiver-factory
  "Named capability factory — avoids anonymous fn literal in init options.
  Returns (fn [be _side] -> IWirelessReceiver) compatible with capability factory contract."
  [max-energy-fn bandwidth-fn]
  (fn [be _side]
    (->WirelessReceiverImpl be max-energy-fn bandwidth-fn)))

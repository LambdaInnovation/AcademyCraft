(ns cn.li.neoforgebase.setup.capability-wiring
  "Shared NeoForge capability listener wiring. Version loaders install Java bridges."
  (:require [cn.li.neoforgebase.setup.consumer-support :as consumer-support]
            [cn.li.mcmod.capability.registry :as cap-registry])
  (:import [net.neoforged.neoforge.capabilities RegisterCapabilitiesEvent]
           [net.neoforged.bus.api IEventBus]))

(defonce ^:private capability-bridge-atom
  (atom nil))

(defn install-capability-bridge!
  "Install {:get-block :register-block! :get-or-create-block! :block-cap-for-type :register-all!}."
  [bridge]
  (reset! capability-bridge-atom bridge)
  bridge)

(defn- bridge! []
  (let [b @capability-bridge-atom]
    (when (nil? b)
      (throw (IllegalStateException. "capability bridge not installed")))
    b))

(defn- add-listener!
  [^IEventBus mod-bus ^Class listener-class f]
  (consumer-support/add-normal-listener! mod-bus listener-class f))

(defn- ensure-block-capability-token!
  [key ^Class java-type]
  (let [b (bridge!)
        k (name key)]
    (when-not ((:get-block b) k)
      (if-let [provided ((:block-cap-for-type b) java-type)]
        ((:register-block! b) k provided)
        ((:get-or-create-block! b) k java-type)))
    nil))

(defn- sync-declared-capability-tokens!
  []
  (doseq [[key {:keys [java-type]}] (cap-registry/capability-type-registry-snapshot)]
    (when java-type
      (ensure-block-capability-token! key java-type)))
  nil)

(defn register-capability-listener!
  [^IEventBus mod-bus]
  (let [b (bridge!)]
    (add-listener! mod-bus RegisterCapabilitiesEvent
                   (fn [event]
                     (sync-declared-capability-tokens!)
                     ((:register-all! b) event))))
  nil)

(ns cn.li.neoforge262.capability.fluid-handler
  "Forge-specific fluid capability registration for AC machines.

  26.2: expose Capabilities.Fluid.BLOCK through a native ResourceHandler."
  (:require [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.capability.registry :as cap-registry]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.neoforge262.capability CapabilityRegistry
            ForgeProvidedCapabilitySupport]
           [cn.li.neoforge262.shim UniversalFluidHandler]
           [net.minecraft.core.registries BuiltInRegistries]
           [net.minecraft.resources Identifier]
           [net.minecraft.world.level.material Fluid]
           [net.neoforged.neoforge.fluids FluidStack]
           [net.neoforged.neoforge.transfer ResourceHandler]
           [net.neoforged.neoforge.transfer.fluid FluidResource]))

(def ^:private phase-fluid-lock (Object.))
(def ^:private ^:dynamic *phase-fluid* nil)

(defn- resolve-phase-fluid
  []
  (or *phase-fluid*
      (locking phase-fluid-lock
        (or *phase-fluid*
            (let [{:keys [mod-id path]} (cap-registry/get-tile-fluid-spec "phase-gen")
                  rl (when (and mod-id path)
                       (Identifier/fromNamespaceAndPath ^String mod-id ^String path))
                  fluid (when rl
                          (try (.getValue BuiltInRegistries/FLUID rl)
                               (catch Exception e (log/debug "Fluid registry lookup failed:" (ex-message e)) nil)))]
              (alter-var-root #'*phase-fluid* (constantly fluid))
              fluid)))))

(defn- create-phase-gen-fluid-handler
  "Return a native ResourceHandler backed by the phase-gen BE's custom state."
  ^ResourceHandler
  [be]
  (let [^Fluid fluid (resolve-phase-fluid)]
    (UniversalFluidHandler.
      (fn [] 1)
      (fn [_tank]
        (let [state (platform-be/get-custom-state be)
              amount (int (get state :liquid-amount 0))]
          (if (and fluid (pos? amount))
            (FluidResource/of (FluidStack. fluid amount))
            FluidResource/EMPTY)))
      (fn [_tank]
        (long (get (platform-be/get-custom-state be) :liquid-amount 0)))
      (fn [_tank _resource]
        (let [state (platform-be/get-custom-state be)]
          (long (get state :tank-size 8000))))
      (fn [_tank resource]
        (let [^FluidResource resource resource]
          (boolean (and fluid resource (not (.isEmpty resource))
                        (.matches resource (FluidStack. fluid 1))))))
      (fn [_tank resource amount]
        (let [^FluidResource resource resource
              amount (int amount)]
          (if (and fluid resource (.matches resource (FluidStack. fluid 1)))
            (let [state (platform-be/get-custom-state be)
                  current (int (get state :liquid-amount 0))
                  capacity (int (get state :tank-size 8000))
                  can-fill (max 0 (min amount (- capacity current)))]
              (when (pos? can-fill)
                (let [new-state (assoc state :liquid-amount (+ current can-fill))]
                  (platform-be/set-custom-state! be new-state)
                  (try (platform-be/set-changed! be) (catch Exception e (log/debug "set-changed! failed for fluid fill" (ex-message e)) nil))
                  (try (platform-be/sync-to-client! be) (catch Exception e (log/debug "sync-to-client! failed for fluid fill" (ex-message e)) nil))))
              (int can-fill))
            0)))
      (fn [_tank resource amount]
        (let [^FluidResource resource resource
              amount (int amount)]
          (if (and fluid resource (.matches resource (FluidStack. fluid 1)))
            (let [state (platform-be/get-custom-state be)
                  current (int (get state :liquid-amount 0))
                  can-drain (min current amount)]
              (if (pos? can-drain)
                (do
                  (let [new-state (assoc state :liquid-amount (- current can-drain))]
                    (platform-be/set-custom-state! be new-state)
                    (try (platform-be/set-changed! be) (catch Exception e (log/debug "set-changed! failed for fluid drain" (ex-message e)) nil))
                    (try (platform-be/sync-to-client! be) (catch Exception e (log/debug "sync-to-client! failed for fluid drain" (ex-message e)) nil)))
                  (int can-drain))
                0))
            0))))))

(defn register!
  "Register Capabilities.Fluid.BLOCK for AC machines."
  []
  (CapabilityRegistry/registerBlock "fluid-handler" (ForgeProvidedCapabilitySupport/fluidBlock))
  (when-not (cap-registry/get-capability-entry :fluid-handler)
    (cap-registry/declare-capability!
      :fluid-handler ResourceHandler
      (fn [be _side] (create-phase-gen-fluid-handler be))))
  (log/debug "Registered NeoForge Capabilities.Fluid.BLOCK for AC machines"))

(defn init! [& _] (register!))

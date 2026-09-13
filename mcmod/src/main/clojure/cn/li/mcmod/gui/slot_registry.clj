(ns cn.li.mcmod.gui.slot-registry
  "Slot validator registry for platform GUI slot creation.

   Game/content modules register predicate functions (stack -> boolean)
   for each slot category/type. Platform adapters then use those validators
   to implement vanilla Slot.mayPlace/mayPickup logic.

   State stored in Framework [:registry :slots]."
  (:require [cn.li.mcmod.framework :as fw]))

(def ^:private slot-path [:registry :slots])

(defn- slot-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom slot-path {:slot-validators {}})
    {:slot-validators {}}))

(defn set-slot-validators!
  "Replace all registered slot validators. Intended for tests."
  [validators]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom assoc-in (conj slot-path :slot-validators) validators))
  nil)

(defn register-slot-validator!
  "Register a slot validator function for a slot category/type keyword."
  [slot-type validator-fn]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in slot-path
           (fn [current]
             (let [base (or current {:slot-validators {}})]
               (assoc-in base [:slot-validators slot-type] validator-fn)))))
  nil)

(defn get-slot-validator
  "Get the registered slot validator for a slot category/type keyword."
  [slot-type]
  (get-in (slot-snapshot) [:slot-validators slot-type]))

(defn get-all-slot-validators
  "Return the full slot validators map."
  []
  (:slot-validators (slot-snapshot)))

(ns cn.li.ac.energy.imaginary-energy-impl
  "Default imaginary-energy implementation for AC items."
  (:require [cn.li.ac.energy.energy-type-interface :as energy-type]
            [cn.li.ac.energy.service.item-manager :as item-manager]))

;; ============================================================================
;; ImaginaryEnergyType — plain function map
;; ============================================================================

(defn make-imaginary-energy-type
  "Create the default imaginary energy type function map."
  []
  {:energy-type-id :imaginary-energy
   :energy-type-name "Imaginary Energy"
   :supports-item? (fn [item-stack]
                     (item-manager/is-energy-item-supported? item-stack))
   :get-energy* (fn [item-stack]
                  (item-manager/get-item-energy item-stack))
   :get-capacity* (fn [item-stack]
                    (item-manager/get-item-capacity item-stack))
   :get-bandwidth* (fn [item-stack]
                     (item-manager/get-item-bandwidth item-stack))
   :set-energy*! (fn [item-stack amount]
                   (item-manager/set-item-energy! item-stack amount))
   :charge-item*! (fn [item-stack amount ignore-bandwidth]
                    (item-manager/charge-energy-to-item item-stack amount ignore-bandwidth))
   :discharge-item*! (fn [item-stack amount ignore-bandwidth]
                       (item-manager/pull-energy-from-item item-stack amount ignore-bandwidth))})

(def ^:private imaginary-energy-type-lock
  (Object.))

(def ^:private ^:dynamic *default-imaginary-energy-type*
  nil)

(defn register-default-energy-type!
  []
  (or (var-get #'*default-imaginary-energy-type*)
      (locking imaginary-energy-type-lock
        (or (var-get #'*default-imaginary-energy-type*)
            (let [energy-type (energy-type/register-energy-type! (make-imaginary-energy-type))]
              (alter-var-root #'*default-imaginary-energy-type* (constantly energy-type))
              energy-type)))))

(defn imaginary-energy-type
  []
  (register-default-energy-type!))

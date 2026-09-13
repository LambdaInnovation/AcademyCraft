(ns cn.li.ac.item.constraint-plate
  "Constraint Plate - component for Wireless Matrix

  Required for Matrix to function (need 3 plates)."
  (:require [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.platform.item :as item]
            [clojure.string :as str]))

(def ^:private constraint-plate-id "constraint_plate")

(defn init-constraint-plate! []
  (install/framework-once! ::constraint-plate-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        constraint-plate-id
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["用于无线矩阵的限制板"
                                "需要3个才能激活矩阵"]
                      :model-texture "constraint_plate"}
         :on-use (fn [_event-data]
                   (log/debug "Using constraint plate"))}))
    (log/debug "Constraint Plate initialized"))))

(defn is-constraint-plate?
  "Check whether a platform ItemStack is a constraint plate."
  [item-stack]
  (when item-stack
    (let [item-obj (try (item/object item-stack) (catch Throwable _ nil))
          registry-name (when item-obj
                          (try (item/registry-name item-obj) (catch Throwable _ nil)))
          desc (when item-obj
                 (try (str (item/description-id item-obj)) (catch Throwable _ nil)))
          item-id (or registry-name (when desc (last (str/split desc #"\\."))))
          result (boolean
                   (or (= item-id constraint-plate-id)
                       (and (string? desc) (str/includes? desc constraint-plate-id))))]
      (try
        (log/debug "is-constraint-plate?" {:stack-class (class item-stack)
                                            :desc desc
                                            :derived-id item-id
                                            :result result})
        (catch Throwable _))
      result)))

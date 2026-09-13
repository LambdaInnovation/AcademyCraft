(ns cn.li.platform.neutral.integration-runtime
  "Installed direct function bindings for platform-neutral integration domain code."
  (:require [clojure.string :as str]))

(def ^:private operations
  [:content-to-fe :fe-to-content :validate-conversion-rate
   :forge-energy-conversion-rate :ic2-energy-conversion-rate
   :jei-get-all-categories :jei-get-recipes :jei-format-recipe
   :get-jei-nbt-subtype-item-ids :msg-id
   :on-item-event! :process-pending-activations!
   :register-tutorial-activated-hook!])

(defn- unavailable [operation]
  (throw (IllegalStateException.
           (str "Integration runtime provider unavailable: " operation))))

(def content-to-fe (fn [& _] (unavailable :content-to-fe)))
(def fe-to-content (fn [& _] (unavailable :fe-to-content)))
(def validate-conversion-rate (fn [& _] (unavailable :validate-conversion-rate)))
(def forge-energy-conversion-rate (fn [& _] (unavailable :forge-energy-conversion-rate)))
(def ic2-energy-conversion-rate (fn [& _] (unavailable :ic2-energy-conversion-rate)))
(def jei-get-all-categories (fn [& _] (unavailable :jei-get-all-categories)))
(def jei-get-recipes (fn [& _] (unavailable :jei-get-recipes)))
(def jei-format-recipe (fn [& _] (unavailable :jei-format-recipe)))
(def get-jei-nbt-subtype-item-ids (fn [& _] (unavailable :get-jei-nbt-subtype-item-ids)))
(def msg-id (fn [& _] (unavailable :msg-id)))
(def on-item-event! (fn [& _] (unavailable :on-item-event!)))
(def process-pending-activations! (fn [& _] (unavailable :process-pending-activations!)))
(def register-tutorial-activated-hook! (fn [& _] (unavailable :register-tutorial-activated-hook!)))
(doseq [operation operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))

(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.integration-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))

(defn install! [provided]
  (when (or (not= (set operations) (set (keys provided)))
            (some (complement ifn?) (vals provided)))
    (throw (ex-info "Integration runtime provider contract mismatch"
                    {:actual (sort (keys provided))})))
  (doseq [operation operations]
    (alter-var-root (facade-var operation)
                    (constantly (get provided operation))))
  nil)

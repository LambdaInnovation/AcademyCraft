(ns cn.li.mcbase.datagen.provider-registration
  "Shared datagen provider registration loop.

  Loader setup namespaces provide the ordered provider entries and the
  Loader/API-specific registration callback."
  (:require [clojure.string :as str]))

(defn- registering-message
  [mod-id {:keys [label]}]
  (str "[" mod-id "] Registering " label " DataGenerator..."))

(defn- summary-message
  [mod-id target-label providers]
  (let [summary-labels (->> providers
                            (map #(get % :summary-label))
                            distinct
                            (str/join "+"))]
    (str "[" mod-id "] " target-label
         " DataGenerator setup registered " summary-labels " providers.")))

(defn register-providers!
  "Register ordered logical datagen providers.

  Options:
  - `:mod-id` string/keyword used in log messages.
  - `:target-label` human-readable target label used in summary output.
  - `:providers` ordered vector of provider manifest entries.
  - `:register-provider!` function called with each provider manifest entry.

  Returns the vector of provider entries that were registered."
  [{:keys [mod-id target-label providers register-provider!]}]
  (when-not register-provider!
    (throw (ex-info "Datagen provider registration requires :register-provider!"
                    {:target target-label})))
  (when-not (seq providers)
    (throw (ex-info "Datagen provider registration requires :providers"
                    {:target target-label})))
  (let [mod-id (str mod-id)
        target-label (str target-label)]
    (doseq [provider providers]
      (println (registering-message mod-id provider))
      (register-provider! provider))
    (println (summary-message mod-id target-label providers))
    providers))

(ns cn.li.ac.ability.discovery-descriptor
  "Descriptor source for bundled ability discovery providers."
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]))

(def ^:private bundled-descriptor-resource
  "ac/ability/providers.edn")

(defn bundled-provider-descriptors
  []
  (if-let [res (io/resource bundled-descriptor-resource)]
    (with-open [rdr (java.io.PushbackReader. (io/reader res))]
      (edn/read rdr))
    []))

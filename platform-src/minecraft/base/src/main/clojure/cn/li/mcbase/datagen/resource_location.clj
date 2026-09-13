(ns cn.li.mcbase.datagen.resource-location
  "Loader-agnostic id parsing helper for datagen outputs."
  (:require [clojure.string :as str])
  (:import [cn.li.mcver ResourceLocations]))

(defn parse-resource-location
  ([s] (parse-resource-location s nil))
  ([s default-namespace]
   (let [value (str s)]
     (if (str/includes? value ":")
       (let [[namespace path] (str/split value #":" 2)]
         (ResourceLocations/of namespace path))
       (when default-namespace
         (ResourceLocations/of default-namespace value))))))

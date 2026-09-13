(ns cn.li.platform.target
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io])
  (:import [java.io PushbackReader]))

(def ^:private target-resource "META-INF/academy-target.edn")
(defn current-target! []
  (if-let [resource (io/resource target-resource)]
    (with-open [reader (io/reader resource)]
      (edn/read {:readers *data-readers*} (PushbackReader. reader)))
    (throw (ex-info "Platform target metadata missing" {:resource target-resource}))))
(defn current-target-key! [] (keyword (:id (current-target!))))
(defn current-loader! [] (:loader (current-target!)))

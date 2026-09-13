(ns cn.li.mcbase.datagen.json-util
  "Loader-agnostic JSON serialization helper for datagen outputs."
  (:require [clojure.string]))

(defn- escape-json-string [s]
  (-> (str s)
      (.replace "\\" "\\\\")
      (.replace "\"" "\\\"")
      (.replace "\b" "\\b")
      (.replace "\f" "\\f")
      (.replace "\n" "\\n")
      (.replace "\r" "\\r")
      (.replace "\t" "\\t")))

(declare write-json)

(defn- write-json-map [m]
  (str "{"
       (clojure.string/join
        ","
        (map (fn [[k v]]
               (str "\"" (escape-json-string (if (keyword? k) (name k) (str k))) "\":"
                    (write-json v)))
             m))
       "}"))

(defn- write-json-coll [xs]
  (str "["
       (clojure.string/join "," (map write-json xs))
       "]"))

(defn write-json
  "Serialize Clojure data to JSON string.

  Supports maps, vectors/lists/sets, strings, numbers, booleans, nil,
  keywords, and symbols."
  [x]
  (cond
    (nil? x) "null"
    (string? x) (str "\"" (escape-json-string x) "\"")
    (keyword? x) (str "\"" (escape-json-string (name x)) "\"")
    (symbol? x) (str "\"" (escape-json-string (str x)) "\"")
    (number? x) (str x)
    (true? x) "true"
    (false? x) "false"
    (map? x) (write-json-map x)
    (or (vector? x) (list? x) (set? x) (seq? x)) (write-json-coll x)
    :else (str "\"" (escape-json-string (str x)) "\"")))
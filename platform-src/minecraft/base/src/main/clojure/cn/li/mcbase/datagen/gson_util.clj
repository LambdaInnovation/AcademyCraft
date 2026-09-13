(ns cn.li.mcbase.datagen.gson-util
  "Shared Gson utilities for datagen JSON generation.
  
  Provides standard Gson builder configuration used consistently across all datagen providers."
  (:import [com.google.gson GsonBuilder]))

(defn create-pretty-gson
  "Create a standard Gson instance for datagen use.
   
   Returns: com.google.gson.Gson configured with:
     - Pretty printing enabled
     - HTML escaping disabled (allows raw JSON objects in output)"
  []
  (-> (GsonBuilder.)
      (.setPrettyPrinting)
      (.disableHtmlEscaping)
      (.create)))

(defn normalize-json
  "Recursively convert Clojure data into Gson-friendly JSON structures.

  - map keys are converted to plain strings (keyword keys become `name`)
  - vectors/lists/seqs are normalized recursively
  - scalar values are passed through unchanged"
  [x]
  (cond
    (map? x)
    (into {}
          (map (fn [[k v]]
                 [(cond
                    (keyword? k) (name k)
                    (string? k) k
                    :else (str k))
                  (normalize-json v)])
          x))

    (vector? x) (mapv normalize-json x)
    (sequential? x) (mapv normalize-json x)
    :else x))

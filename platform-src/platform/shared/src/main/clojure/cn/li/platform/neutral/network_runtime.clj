(ns cn.li.platform.neutral.network-runtime
  (:require [clojure.string :as str]))
(def ^:private operations [:encode :decode :list-descriptors :handle-request])
(defn- unavailable [operation] (throw (IllegalStateException. (str "Network runtime provider unavailable: " operation))))
(def encode (fn [& _] (unavailable :encode)))
(def decode (fn [& _] (unavailable :decode)))
(def list-descriptors (fn [& _] (unavailable :list-descriptors)))
(def handle-request (fn [& _] (unavailable :handle-request)))
(doseq [operation operations] (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))
(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.network-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))
(defn install! [provided]
  (when (or (not= (set operations) (set (keys provided))) (some (complement ifn?) (vals provided)))
    (throw (ex-info "Network runtime provider contract mismatch" {:actual (sort (keys provided))})))
  (doseq [operation operations] (alter-var-root (facade-var operation) (constantly (get provided operation)))) nil)

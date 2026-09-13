(ns cn.li.platform.neutral.block-runtime
  (:require [clojure.string :as str]))

(def ^:private operations
  [:get-block-spec :list-all-blocks :identify-block-from-full-name :is-part-block?
   :has-block-event-handler? :snapshot-tiles-by-id :register-tile-capability-keys!
   :merge-tile-kind-defaults :create-property-registry :register-block-properties!
   :get-property :get-all-properties])

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Block runtime provider unavailable: " operation))))
(def get-block-spec (fn [& _] (unavailable :get-block-spec)))
(def list-all-blocks (fn [& _] (unavailable :list-all-blocks)))
(def identify-block-from-full-name (fn [& _] (unavailable :identify-block-from-full-name)))
(def is-part-block? (fn [& _] (unavailable :is-part-block?)))
(def has-block-event-handler? (fn [& _] (unavailable :has-block-event-handler?)))
(def snapshot-tiles-by-id (fn [& _] (unavailable :snapshot-tiles-by-id)))
(def register-tile-capability-keys! (fn [& _] (unavailable :register-tile-capability-keys!)))
(def merge-tile-kind-defaults (fn [& _] (unavailable :merge-tile-kind-defaults)))
(def create-property-registry (fn [& _] (unavailable :create-property-registry)))
(def register-block-properties! (fn [& _] (unavailable :register-block-properties!)))
(def get-property (fn [& _] (unavailable :get-property)))
(def get-all-properties (fn [& _] (unavailable :get-all-properties)))
(doseq [operation operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))

(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.block-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))

(defn install! [provided]
  (let [expected (set operations)]
    (when (or (not= expected (set (keys provided)))
              (some (complement ifn?) (vals provided)))
      (throw (ex-info "Block runtime provider contract mismatch"
                      {:actual (sort (keys provided))})))
    (doseq [operation operations]
      (alter-var-root (facade-var operation)
                      (constantly (get provided operation)))))
  nil)

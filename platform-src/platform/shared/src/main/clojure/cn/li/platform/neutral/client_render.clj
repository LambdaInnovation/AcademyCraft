(ns cn.li.platform.neutral.client-render
  (:require [clojure.string :as str]))
(def ^:private operations [:initial-button-state :handle-button-state! :validate-profile! :kind-renderer-key :register-scripted-effect-kind! :resolve-kind-renderer-key :supported-kinds :get-profile :snapshot])
(defn- unavailable [operation] (throw (IllegalStateException. (str "Client render provider unavailable: " operation))))
(def initial-button-state (fn [& _] (unavailable :initial-button-state)))
(def handle-button-state! (fn [& _] (unavailable :handle-button-state!)))
(def validate-profile! (fn [& _] (unavailable :validate-profile!)))
(def kind-renderer-key (fn [& _] (unavailable :kind-renderer-key)))
(def register-scripted-effect-kind! (fn [& _] (unavailable :register-scripted-effect-kind!)))
(def resolve-kind-renderer-key (fn [& _] (unavailable :resolve-kind-renderer-key)))
(def supported-kinds (fn [& _] (unavailable :supported-kinds)))
(def get-profile (fn [& _] (unavailable :get-profile)))
(def snapshot (fn [& _] (unavailable :snapshot)))
(doseq [operation operations] (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))
(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.client-render)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))
(defn install! [provided]
  (when (or (not= (set operations) (set (keys provided))) (some (complement ifn?) (vals provided)))
    (throw (ex-info "Client render provider contract mismatch" {:actual (sort (keys provided))})))
  (doseq [operation operations] (alter-var-root (facade-var operation) (constantly (get provided operation)))) nil)

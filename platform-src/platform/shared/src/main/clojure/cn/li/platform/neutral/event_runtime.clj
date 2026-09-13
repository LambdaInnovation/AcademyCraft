(ns cn.li.platform.neutral.event-runtime
  (:require [clojure.string :as str]))

(def ^:private operations
  [:on-block-right-click :on-block-place :on-block-break
   :interaction-consumed? :gui-open-result?
   :dispatch-world-load :dispatch-world-unload :dispatch-world-save :dispatch-world-tick
   :remember-saved-data! :consume-saved-data! :clear-world-saved-data!
   :set-on-world-state-changed-fn! :world-key])

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Event runtime provider unavailable: " operation))))
(def on-block-right-click (fn [& _] (unavailable :on-block-right-click)))
(def on-block-place (fn [& _] (unavailable :on-block-place)))
(def on-block-break (fn [& _] (unavailable :on-block-break)))
(def interaction-consumed? (fn [& _] (unavailable :interaction-consumed?)))
(def gui-open-result? (fn [& _] (unavailable :gui-open-result?)))
(def dispatch-world-load (fn [& _] (unavailable :dispatch-world-load)))
(def dispatch-world-unload (fn [& _] (unavailable :dispatch-world-unload)))
(def dispatch-world-save (fn [& _] (unavailable :dispatch-world-save)))
(def dispatch-world-tick (fn [& _] (unavailable :dispatch-world-tick)))
(def remember-saved-data! (fn [& _] (unavailable :remember-saved-data!)))
(def consume-saved-data! (fn [& _] (unavailable :consume-saved-data!)))
(def clear-world-saved-data! (fn [& _] (unavailable :clear-world-saved-data!)))
(def set-on-world-state-changed-fn! (fn [& _] (unavailable :set-on-world-state-changed-fn!)))
(def world-key (fn [& _] (unavailable :world-key)))
(doseq [operation operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))

(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.event-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))

(defn install! [provided]
  (let [expected (set operations)]
    (when (or (not= expected (set (keys provided)))
              (some (complement ifn?) (vals provided)))
      (throw (ex-info "Event runtime provider contract mismatch"
                      {:actual (sort (keys provided))})))
    (doseq [operation operations]
      (alter-var-root (facade-var operation)
                      (constantly (get provided operation)))))
  nil)

(ns cn.li.platform.neutral.gui-runtime
  (:require [clojure.string :as str]))

(def ^:private operations
  [:get-all-gui-ids :get-registry-name :get-display-name :get-slot-layout :get-slot-range
   :get-screen-factory-fn :get-screen-factory-fn-kw :get-gui-handler :get-server-container
   :get-container-for-menu :owner-from-container :register-menu-container! :unregister-menu-container!
   :get-gui-id-for-container :safe-close! :safe-validate :server-menu-sync! :slot-can-place?
   :slot-changed! :slot-count :slot-get-item :slot-set-item! :clamp-int :get-slot-validator])
(defn- unavailable [operation]
  (throw (IllegalStateException. (str "GUI runtime provider unavailable: " operation))))
(def get-all-gui-ids (fn [& _] (unavailable :get-all-gui-ids)))
(def get-registry-name (fn [& _] (unavailable :get-registry-name)))
(def get-display-name (fn [& _] (unavailable :get-display-name)))
(def get-slot-layout (fn [& _] (unavailable :get-slot-layout)))
(def get-slot-range (fn [& _] (unavailable :get-slot-range)))
(def get-screen-factory-fn (fn [& _] (unavailable :get-screen-factory-fn)))
(def get-screen-factory-fn-kw (fn [& _] (unavailable :get-screen-factory-fn-kw)))
(def get-gui-handler (fn [& _] (unavailable :get-gui-handler)))
(def get-server-container (fn [& _] (unavailable :get-server-container)))
(def get-container-for-menu (fn [& _] (unavailable :get-container-for-menu)))
(def owner-from-container (fn [& _] (unavailable :owner-from-container)))
(def register-menu-container! (fn [& _] (unavailable :register-menu-container!)))
(def unregister-menu-container! (fn [& _] (unavailable :unregister-menu-container!)))
(def get-gui-id-for-container (fn [& _] (unavailable :get-gui-id-for-container)))
(def safe-close! (fn [& _] (unavailable :safe-close!)))
(def safe-validate (fn [& _] (unavailable :safe-validate)))
(def server-menu-sync! (fn [& _] (unavailable :server-menu-sync!)))
(def slot-can-place? (fn [& _] (unavailable :slot-can-place?)))
(def slot-changed! (fn [& _] (unavailable :slot-changed!)))
(def slot-count (fn [& _] (unavailable :slot-count)))
(def slot-get-item (fn [& _] (unavailable :slot-get-item)))
(def slot-set-item! (fn [& _] (unavailable :slot-set-item!)))
(def clamp-int (fn [& _] (unavailable :clamp-int)))
(def get-slot-validator (fn [& _] (unavailable :get-slot-validator)))
(doseq [operation operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))
(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.gui-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))
(defn install! [provided]
  (let [expected (set operations)]
    (when (or (not= expected (set (keys provided))) (some (complement ifn?) (vals provided)))
      (throw (ex-info "GUI runtime provider contract mismatch" {:actual (sort (keys provided))})))
    (doseq [operation operations]
      (alter-var-root (facade-var operation) (constantly (get provided operation)))))
  nil)

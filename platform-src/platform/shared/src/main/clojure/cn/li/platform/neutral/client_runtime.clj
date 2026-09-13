(ns cn.li.platform.neutral.client-runtime
  (:require [clojure.string :as str]))

(def ^:private operations
  [:merge-client-bridge! :call-adapter :resolve-shader :open-screen!
   :reactive-overlay-build :reactive-overlay-update :reactive-overlay-mode-switch!
   :run-client-tick-hooks! :create-widget
   :register-default-renderer-init-fns! :register-all-renderers!
   :get-scripted-tile-renderer :scripted-renderers-snapshot
   :install-pose-ops! :install-render-buffer-ops!
   :register-texture! :get-texture-path :reset-texture-registry-for-test!])

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Client runtime provider unavailable: " operation))))

(def merge-client-bridge! (fn [& _] (unavailable :merge-client-bridge!)))
(def call-adapter (fn [& _] (unavailable :call-adapter)))
(def resolve-shader (fn [& _] (unavailable :resolve-shader)))
(def open-screen! (fn [& _] (unavailable :open-screen!)))
(def reactive-overlay-build (fn [& _] (unavailable :reactive-overlay-build)))
(def reactive-overlay-update (fn [& _] (unavailable :reactive-overlay-update)))
(def reactive-overlay-mode-switch! (fn [& _] (unavailable :reactive-overlay-mode-switch!)))
(def run-client-tick-hooks! (fn [& _] (unavailable :run-client-tick-hooks!)))
(def create-widget (fn [& _] (unavailable :create-widget)))
(def register-default-renderer-init-fns! (fn [& _] (unavailable :register-default-renderer-init-fns!)))
(def register-all-renderers! (fn [& _] (unavailable :register-all-renderers!)))
(def get-scripted-tile-renderer (fn [& _] (unavailable :get-scripted-tile-renderer)))
(def scripted-renderers-snapshot (fn [& _] (unavailable :scripted-renderers-snapshot)))
(def install-pose-ops! (fn [& _] (unavailable :install-pose-ops!)))
(def install-render-buffer-ops! (fn [& _] (unavailable :install-render-buffer-ops!)))
(def register-texture! (fn [& _] (unavailable :register-texture!)))
(def get-texture-path (fn [& _] (unavailable :get-texture-path)))
(def reset-texture-registry-for-test! (fn [& _] (unavailable :reset-texture-registry-for-test!)))
(doseq [operation operations]
  (intern *ns* (symbol (name operation)) (fn [& _] (unavailable operation))))

(defn- facade-var [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.client-runtime)
        operation-symbol (symbol (name operation))]
    (or (ns-resolve facade-ns operation-symbol)
        (intern facade-ns operation-symbol (fn [& _] (unavailable operation))))))

(defn install! [provided]
  (let [expected (set operations)]
    (when (or (not= expected (set (keys provided)))
              (some (complement ifn?) (vals provided)))
      (throw (ex-info "Client runtime provider contract mismatch"
                      {:actual (sort (keys provided))})))
    (doseq [operation operations]
      (alter-var-root (facade-var operation)
                      (constantly (get provided operation)))))
  nil)

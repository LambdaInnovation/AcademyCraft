(ns cn.li.ac.registry.hooks
  "Hook registry system for explicit network handler and client renderer setup.

  Blocks/items/GUIs should register hooks from explicit init functions. The AC
  lifecycle invokes the collected hooks during initialization/client setup.

  Registry stored in Framework [:registry :hooks :ac]."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def ^:private hooks-path [:registry :hooks :ac])

(defn- hook-registry-state-snapshot []
  (if-let [fw-atom (fw/fw-atom)]
    (get-in @fw-atom hooks-path {:network-handlers [] :client-renderers [] :frozen? false})
    {:network-handlers [] :client-renderers [] :frozen? false}))

(defn- update-hook-registry-state! [f & args]
  (when-let [fw-atom (fw/fw-atom)]
    (swap! fw-atom update-in hooks-path
           (fn [current] (apply f (or current {:network-handlers [] :client-renderers [] :frozen? false}) args))))
  nil)

(defn- assert-registry-open! []
  (when (:frozen? (hook-registry-state-snapshot))
    (throw (ex-info "AC hook registry is frozen" {}))))

(defn hook-registry-snapshot [] (hook-registry-state-snapshot))

(defn reset-hook-registry-for-test!
  ([] (reset-hook-registry-for-test! {}))
  ([{:keys [network-handlers client-renderers frozen?]
     :or {network-handlers [] client-renderers [] frozen? false}}]
   (when-let [fw-atom (fw/fw-atom)]
     (swap! fw-atom assoc-in hooks-path {:network-handlers (vec network-handlers)
                                          :client-renderers (vec client-renderers)
                                          :frozen? frozen?}))
   nil))

(defn freeze-hook-registry! []
  (update-hook-registry-state! assoc :frozen? true) nil)

(defn- dedupe-conj [items item]
  (if (some #(= % item) items) items (conj items item)))

(defn get-network-handlers [] (:network-handlers (hook-registry-state-snapshot)))
(defn get-client-renderers [] (:client-renderers (hook-registry-state-snapshot)))
(defn reset-registries! [] (reset-hook-registry-for-test!))

(defn register-network-handler! [handler-fn]
  (assert-registry-open!)
  (update-hook-registry-state! update :network-handlers dedupe-conj handler-fn))

(defn register-client-renderer! [renderer-ns-sym]
  (assert-registry-open!)
  (update-hook-registry-state! update :client-renderers dedupe-conj renderer-ns-sym))

(defn call-all-network-handlers-with-report! []
  (reduce (fn [{:keys [ok failed] :as acc} handler]
            (try (handler) (assoc acc :ok (conj ok handler))
                 (catch Throwable t (assoc acc :failed (conj failed {:handler handler :error (ex-message t)})))))
          {:ok [] :failed []} (get-network-handlers)))

(defn call-all-network-handlers! []
  (let [{:keys [failed] :as report} (call-all-network-handlers-with-report!)]
    (when (seq failed)
      (log/error "Network handler registration failed:" failed)
      (throw (ex-info "Network handler registration failed" {:report report}))) nil))

(defn load-all-client-renderers-with-report! []
  (reduce (fn [{:keys [ok failed] :as acc} renderer-ns]
            (try (if-let [init-fn (requiring-resolve renderer-ns)]
                   (do (init-fn) (assoc acc :ok (conj ok renderer-ns)))
                   (assoc acc :failed (conj failed {:renderer renderer-ns :error "init function not found"})))
                 (catch Throwable t (assoc acc :failed (conj failed {:renderer renderer-ns :error (ex-message t)})))))
          {:ok [] :failed []} (get-client-renderers)))

(defn load-all-client-renderers! []
  (let [{:keys [failed] :as report} (load-all-client-renderers-with-report!)]
    (when (seq failed)
      (log/error "Client renderer loading failed:" failed)
      (throw (ex-info "Client renderer loading failed" {:report report}))) nil))

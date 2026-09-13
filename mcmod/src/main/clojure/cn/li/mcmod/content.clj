(ns cn.li.mcmod.content
  "Explicit suite content registration driven by target-supplied provider data.

   This namespace must remain platform-neutral: target metadata is read by the
   platform layer and passed in explicitly, so content registration never
   requires cn.li.platform.target."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.runtime.provider :as provider]))

(def ^:private provider-sides #{:common :server :client :datagen})

(defn- side-key [side]
  (cond
    (keyword? side) side
    (string? side) (keyword side)
    :else (throw (ex-info "Provider manifest side must be a keyword or string" {:side side}))))

(defn- side-modules [provider-manifests side]
  (let [side (side-key side)]
    (when-not (contains? provider-sides side)
      (throw (ex-info "Unsupported provider manifest side" {:side side})))
    (when-not (map? provider-manifests)
      (throw (ex-info "Provider manifests must be a side-keyed map"
                      {:provider-manifests provider-manifests})))
    (let [modules (get provider-manifests side [])]
      (when-not (sequential? modules)
        (throw (ex-info "Provider manifest side must contain a sequence"
                        {:side side :modules modules})))
      modules)))

(defn register-content! [content-module target-model]
  "Load one verified neutral provider and invoke its optional content op.

   content-module is generated target data with :id, :side, :namespace,
   :function and :provides. The factory is allowed to be source-loaded only
   because the build verifies its complete dependency closure is neutral."
  (let [{:keys [id namespace function provides side]} content-module
        fw-atom (or (fw/fw-atom)
                    (throw (ex-info "Content registration requires Framework injection"
                                    {:module content-module})))]
    (when-not (and id namespace function side (seq provides))
      (throw (ex-info "Invalid content provider metadata" {:module content-module})))
    (provider/load-provider! fw-atom content-module {:target target-model})
    (let [operations (into {}
                           (map (fn [operation]
                                  [(keyword operation)
                                   (provider/provider-op! fw-atom id operation)]))
                           provides)]
      (when-let [register-content (:register-content! operations)]
        (register-content))
      [(keyword id) operations])))

(defn available-content-ids
  "Return content ids for one target-owned provider-manifest side."
  [provider-manifests side]
  (mapv :id (side-modules provider-manifests side)))

(defn register-provider-side!
  "Load every provider declared for one initialization side.

   The side is data outside each descriptor, so a provider manifest contains
   symbols and operation contracts only; it never embeds a platform Var, Class
   or object."
  [provider-manifests side target-model]
  (let [side (side-key side)]
    (into {}
          (map #(register-content! (assoc % :side side) target-model))
          (side-modules provider-manifests side))))

(defn register-all-content!
  "Register common providers during normal content initialization."
  [provider-manifests target-model]
  (register-provider-side! provider-manifests :common target-model))

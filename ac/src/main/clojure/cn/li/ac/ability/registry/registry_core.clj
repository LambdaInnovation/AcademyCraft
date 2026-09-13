(ns cn.li.ac.ability.registry.registry-core
  "Shared Framework-backed frozen-registry engine.

  Both registry/skill.clj and registry/category.clj are 'register once at
  bootstrap, freeze, read forever after' stores with identical
  snapshot/update/freeze/reset/register shape. This namespace is the single
  implementation; the per-domain namespaces are thin wrappers that add their
  own validation/normalization before delegating here."
  (:require [clojure.string :as str]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(defn- capitalize-first [s]
  (if (seq s) (str (str/upper-case (subs s 0 1)) (subs s 1)) s))

(defn make-registry-ops
  "Build a standard Framework-backed frozen-registry operation set.

  fw-path — Framework path vector, e.g. [:registry :skills]
  opts:
    :label            string used in error/log messages, e.g. \"skill\"
    :conflict-key-fn  fn spec -> comparable value. Two registrations under the
                      same id are treated as an idempotent no-op when this fn
                      returns equal values for both; otherwise a conflict is
                      thrown. Defaults to identity (full spec equality).
    :validate!        optional fn spec -> void, called first inside
                      :register! and expected to throw on an invalid spec.

  Returns a map of operation fns:
    :snapshot         [] -> {id spec}
    :freeze!          [] -> nil
    :reset-for-test!  ([] [registry-map]) -> nil
    :register!        [spec] -> stored-spec (spec must contain :id)
    :get              [id] -> spec or nil
    :get-all          [] -> [spec ...]"
  [fw-path {:keys [label conflict-key-fn validate!]
            :or {conflict-key-fn identity}}]
  (let [default-state {:registry {} :frozen? false}]
    (letfn [(state-snapshot []
              (if-let [fw-atom (fw/fw-atom)]
                (get-in @fw-atom fw-path default-state)
                default-state))
            (update-state! [f & args]
              (when-let [fw-atom (fw/fw-atom)]
                (swap! fw-atom update-in fw-path
                       (fn [current] (apply f (or current default-state) args))))
              nil)]
      {:snapshot (fn [] (:registry (state-snapshot)))
       :freeze! (fn [] (update-state! assoc :frozen? true) nil)
       :reset-for-test! (fn
                          ([] (update-state! (constantly default-state)))
                          ([registry]
                           (when-let [fw-atom (fw/fw-atom)]
                             (swap! fw-atom assoc-in fw-path
                                    {:registry (or registry {}) :frozen? false}))
                           nil))
       :register! (fn [{:keys [id] :as spec}]
                    (when validate! (validate! spec))
                    (if-let [existing (get (:registry (state-snapshot)) id)]
                      (if (= (conflict-key-fn existing) (conflict-key-fn spec))
                        existing
                        (throw (ex-info (str "Conflicting " label " id")
                                        {:id id
                                         :existing (conflict-key-fn existing)
                                         :new (conflict-key-fn spec)})))
                      (do
                        (when (:frozen? (state-snapshot))
                          (throw (ex-info (str (capitalize-first label) " registry is frozen") {})))
                        (update-state! assoc-in [:registry id] spec)
                        (log/debug "Registered" label id)
                        spec)))
       :get (fn [id] (get (:registry (state-snapshot)) id))
       :get-all (fn [] (vals (:registry (state-snapshot))))})))

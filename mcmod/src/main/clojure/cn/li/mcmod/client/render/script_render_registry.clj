(ns cn.li.mcmod.client.render.script-render-registry
  "Shared ScriptRender profile registry.

  Registry semantics:
  - first registration for a profile id wins
  - duplicate registrations are ignored
  - duplicate warning is emitted at most once per profile id
  - registry can be frozen after initialization"
  (:require [cn.li.mcmod.client.render.script-render-abi :as abi]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn create-script-render-runtime
  []
  {:profile-registry {}
   :registry-frozen? false
   :duplicate-warned-ids #{}})


(def ^:private profile-registry
  {})

(def ^:private registry-frozen?
  false)

(def ^:private duplicate-warned-ids
  #{})

(defn profile-registry-snapshot
  []
  profile-registry)

(defn frozen?
  []
  registry-frozen?)

(defn freeze!
  []
  (install/install-root! #'registry-frozen? true)
  nil)

(defn unfreeze!
  []
  (install/install-root! #'registry-frozen? false)
  nil)

(defn clear!
  []
  (install/install-root! #'profile-registry {})
  (install/install-root! #'duplicate-warned-ids #{})
  nil)

(defn register-profile!
  [profile]
  (when (frozen?)
    (throw (ex-info "ScriptRender profile registry is frozen"
                    {:id (:id profile)})))
  (let [validated (abi/validate-profile! profile)
        profile-id (:id validated)
        existing (get profile-registry profile-id)]
    (if existing
      (do
        (when-not (contains? duplicate-warned-ids profile-id)
          (install/install-root! #'duplicate-warned-ids (conj duplicate-warned-ids profile-id))
          (log/warn "Duplicate ScriptRender profile ignored: " profile-id))
        existing)
      (do
        (install/install-root! #'profile-registry (assoc profile-registry profile-id validated))
        validated))))

(defn register-profiles!
  [profiles]
  (doseq [profile profiles]
    (register-profile! profile))
  nil)

(defn get-profile
  [profile-id]
  (get (profile-registry-snapshot) profile-id))

(defn list-profile-ids
  []
  (keys (profile-registry-snapshot)))

(defn snapshot
  []
  (profile-registry-snapshot))

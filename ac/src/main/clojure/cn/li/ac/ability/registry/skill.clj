(ns cn.li.ac.ability.registry.skill
  "Canonical AC skill registry storage and registration API.

  Registry stored in Framework [:registry :skills]. Standard
  snapshot/freeze/register/reset plumbing lives in registry-core; this
  namespace only adds skill-specific validation, normalization, and the
  apply-skill-overrides cache."
  (:require [cn.li.ac.ability.registry.registry-core :as registry-core]
            [cn.li.ac.ability.registry.skill-spec :as skill-spec]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.mcmod.config.registry :as config-reg]
            [cn.li.mcmod.framework :as fw]))

(def ^:private skill-path [:registry :skills])

(defn- stable-skill-identity [spec]
  (select-keys spec [:id :category-id :level :ctrl-id :pattern]))

(def ^:private ops
  (registry-core/make-registry-ops skill-path
                                   {:label "skill"
                                    :conflict-key-fn stable-skill-identity}))

;; apply-skill-overrides recomputation cache — Framework [:service :skill-spec-cache].
;; Invalidated wholesale on config-generation bump (any config mutation anywhere),
;; not per-skill, since scoping to a domain would require threading the skill's
;; config domain through here — a global generation is simpler and this cache
;; only ever needs to survive within one generation on the hot tick path.
(def ^:private spec-cache-path [:service :skill-spec-cache])

(defn- spec-cache-atom
  ^clojure.lang.IAtom []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom spec-cache-path)
        (get-in (swap! fw-atom update-in spec-cache-path #(or % (atom {:gen -1 :specs {}})))
                spec-cache-path))
    (atom {:gen -1 :specs {}})))

(defn- reset-spec-cache!
  "Invalidate the per-skill override cache. Called whenever the underlying
  skill registry contents are replaced wholesale (test resets) so a stale
  spec can never be served for a skill id re-registered with different data."
  []
  (reset! (spec-cache-atom) {:gen -1 :specs {}})
  nil)

(defn create-skill-registry-runtime
  "Composition-root factory: an isolated {registry frozen?} state atom that
  runtime-container can install/reinstall as a unit (production reset paths
  and test fixtures)."
  ([]
   {::skill-registry-runtime true
    :state* (atom {:registry {} :frozen? false})})
  ([{:keys [state*] :or {state* (atom {:registry {} :frozen? false})}}]
   {::skill-registry-runtime true :state* state*}))

(defn install-skill-registry-runtime! [runtime]
  (when-let [state* (:state* runtime)]
    ((:reset-for-test! ops) (:registry @state*)))
  (reset-spec-cache!)
  runtime)

(defn skill-registry-snapshot []
  ((:snapshot ops)))

(defn reset-skill-registry-for-test!
  ([]
   (reset-skill-registry-for-test! {}))
  ([snapshot]
   ((:reset-for-test! ops) snapshot)
   (reset-spec-cache!)
   nil))

(defn freeze-skill-registry! []
  ((:freeze! ops)))

(defn- inject-configured-fields
  "defskill-declared skills carry no :level/:controllable? (the defskill macro
  rejects them at compile time) — fill both in from
  skill-config/skill-definitions-by-id, the single source of truth for those
  two fields, and cross-check :category-id agrees.

  Callers that already supply :level directly (generic/course-chain passive
  skills built outside the defskill DSL, and any future external/third-party
  skill provider) are trusted as-is and skipped — they have no
  skill-definitions entry by design."
  [{:keys [id category-id] :as spec}]
  (if (contains? spec :level)
    spec
    (if-let [defn-entry (get skill-config/skill-definitions-by-id id)]
      (do
        (when (not= category-id (:category-id defn-entry))
          (throw (ex-info "register-skill!: :category-id disagrees with skill-config/skill-definitions"
                          {:id id
                           :defskill-category-id category-id
                           :skill-definitions-category-id (:category-id defn-entry)})))
        (merge spec (select-keys defn-entry [:level :controllable?])))
      (throw (ex-info "register-skill!: no skill-config/skill-definitions entry for this skill id"
                      {:id id})))))

(defn register-skill!
  "Validate, normalize, and register a skill spec."
  [spec]
  (let [{:keys [id category-id level] :as spec} (inject-configured-fields spec)]
    (when-not (and (keyword? id) (keyword? category-id) (integer? level))
      (throw (IllegalArgumentException. "register-skill!: id & category-id must be keywords, level must be integer")))
    ((:register! ops) (skill-spec/normalize-skill-spec spec))))

(defn raw-skill [skill-id]
  ((:get ops) skill-id))

(defn raw-skills []
  ((:get-all ops)))

(defn raw-skill-entries []
  ((:snapshot ops)))

(defn get-skill [skill-id]
  (when-let [raw (raw-skill skill-id)]
    (let [gen (config-reg/config-generation)
          cache* (spec-cache-atom)
          {cached-gen :gen specs :specs} @cache*]
      (if (and (= gen cached-gen) (contains? specs skill-id))
        (get specs skill-id)
        (let [spec (skill-config/apply-skill-overrides raw)]
          (swap! cache*
                 (fn [c]
                   (if (= (:gen c) gen)
                     (update c :specs assoc skill-id spec)
                     {:gen gen :specs {skill-id spec}})))
          spec)))))

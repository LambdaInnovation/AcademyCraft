(ns cn.li.ac.ability.skill-config
  "Per-skill ability configuration descriptors and effective-spec helpers.

  This namespace is the SSoT for player-facing skill balance config. It stays
  platform-neutral: AC owns descriptors/defaults/getters, while Forge/Fabric
  expose the domains as TOML/JSON through the generic mcmod config bridge."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.config-common :as ability-config-common]
            [cn.li.ac.ability.skill-config.common :as common]
            [cn.li.ac.ability.skill-config.electromaster :as electromaster]
            [cn.li.ac.ability.skill-config.meltdowner :as meltdowner]
            [cn.li.ac.ability.skill-config.teleporter :as teleporter]
            [cn.li.ac.ability.skill-config.vecmanip :as vecmanip]
            [cn.li.ac.config.common :as config-common]
            [cn.li.mcmod.config.registry :as config-reg]))

(def category-ids
  common/category-ids)

(def skill-definitions
  common/skill-definitions)

(def all-skill-ids
  common/all-skill-ids)

(def skill-definitions-by-id
  (into {} (map #(vector (get % :id) %) skill-definitions)))

(def skills-by-category
  (into {}
        (map (fn [category-id]
               [category-id (vec (filter #(= category-id (:category-id %)) skill-definitions))])
             category-ids)))

(def field-definitions
  common/field-definitions)

;; One entry per category module — adding a category means adding one entry
;; here (plus the :require above) instead of touching two separate concat
;; forms.
(def ^:private category-modules
  [{:tunable electromaster/skill-tunable-definitions :internal electromaster/internal-tunable-definitions}
   {:tunable meltdowner/skill-tunable-definitions :internal meltdowner/internal-tunable-definitions}
   {:tunable teleporter/skill-tunable-definitions :internal teleporter/internal-tunable-definitions}
   {:tunable vecmanip/skill-tunable-definitions :internal vecmanip/internal-tunable-definitions}])

(def skill-tunable-definitions
  (vec (mapcat #(get % :tunable) category-modules)))

(def ^:private internal-tunable-definitions
  (vec (mapcat #(get % :internal) category-modules)))

(def field-definitions-by-id
  (into {} (map #(vector (get % :id) %) field-definitions)))

(def skill-tunable-definitions-by-skill
  (group-by #(get % :skill-id) skill-tunable-definitions))

(def skill-tunable-definitions-by-category
  (into {}
        (map (fn [category-id]
               [category-id
                (vec (filter (fn [{:keys [skill-id]}]
                               (= category-id (get-in skill-definitions-by-id [skill-id :category-id])))
                             skill-tunable-definitions))])
             category-ids)))

(def ^:private skill-tunable-definitions-by-skill-field
  (into {}
        (map (fn [[skill-id definitions]]
               [skill-id (into {} (map #(vector (get % :id) %) definitions))])
             skill-tunable-definitions-by-skill)))

(def ^:private internal-tunable-definitions-by-skill-field
  (into {}
        (map (fn [[skill-id definitions]]
               [skill-id (into {} (map #(vector (get % :id) %) definitions))])
             (group-by #(get % :skill-id) internal-tunable-definitions))))

(defn config-key
  [skill-id field-id]
  (keyword (str (name skill-id) "." (name field-id))))

(defn- skill-field-default
  [skill-def {:keys [id spec-key default]}]
  (get skill-def (or spec-key id) default))

(defn- descriptor-for
  [skill-def {:keys [id path section-suffix type min max list-count comment] :as field-def}]
  (cond-> {:key (config-key (:id skill-def) id)
           :path (str (name (:id skill-def)) "." path)
           :section (keyword (str (name (:id skill-def)) "." section-suffix))
           :type type
           :default (skill-field-default skill-def field-def)
           :comment comment}
    (some? min) (assoc :min min)
    (some? max) (assoc :max max)
    (some? list-count) (assoc :list-count list-count)))

(defn- field-definitions-for-skill
  [skill-id]
  (concat field-definitions
          (get skill-tunable-definitions-by-skill skill-id [])))

(defn- descriptors-for-category
  [category-id]
  ;; mapcat + map instead of 2-binding `for` (shorter AOT class names).
  (into []
        (mapcat (fn [skill-def]
                  (map (fn [field-def]
                         (descriptor-for skill-def field-def))
                       (field-definitions-for-skill (:id skill-def))))
                (get skills-by-category category-id))))

(def descriptors-by-category
  (into {} (map (fn [category-id]
                  [category-id (descriptors-for-category category-id)])
                category-ids)))

(def descriptors-by-domain
  (into {} (map (fn [category-id]
                  [(config-common/ability-skill-category-domain category-id)
                   (get descriptors-by-category category-id)])
                category-ids)))

(def default-values-by-category
  (into {} (map (fn [category-id]
                  [category-id
                   (into {}
                         (map #(vector (get % :key) (get % :default))
                              (get descriptors-by-category category-id)))])
                category-ids)))

(def default-values-by-domain
  (into {} (map (fn [category-id]
                  [(config-common/ability-skill-category-domain category-id)
                   (get default-values-by-category category-id)])
                category-ids)))

(defn skill-configured?
  [skill-id]
  (contains? skill-definitions-by-id skill-id))

(defn category-domain
  [category-id]
  (config-common/ability-skill-category-domain category-id))

(defn- skill-domain
  [skill-id]
  (some-> (get-in skill-definitions-by-id [skill-id :category-id]) category-domain))

(defn- public-field-definition
  [skill-id field-id]
  (or (get field-definitions-by-id field-id)
      (get-in skill-tunable-definitions-by-skill-field [skill-id field-id])))

(defn- internal-field-definition
  [skill-id field-id]
  (get-in internal-tunable-definitions-by-skill-field [skill-id field-id]))

(defn- field-definition
  [skill-id field-id]
  (or (public-field-definition skill-id field-id)
      (internal-field-definition skill-id field-id)))

(defn- field-default
  [skill-id field-id]
  (let [skill-def (get skill-definitions-by-id skill-id)
        field-def (field-definition skill-id field-id)]
    (skill-field-default skill-def field-def)))

(defn raw-value
  [skill-id field-id]
  (let [domain (skill-domain skill-id)
        k (config-key skill-id field-id)
        fallback (field-default skill-id field-id)]
    (if (and domain (public-field-definition skill-id field-id))
      (get (config-reg/get-config-values domain) k fallback)
      fallback)))

(defn- non-negative-double
  [skill-id field-id]
  (let [default (double (field-default skill-id field-id))
        ^double value (ability-config-common/finite-double (raw-value skill-id field-id) default)]
    (if (neg? value) default value)))

(defn- positive-double
  [skill-id field-id]
  (let [default (double (field-default skill-id field-id))
        ^double value (ability-config-common/finite-double (raw-value skill-id field-id) default)]
    (if (pos? value) value default)))

(defn- int-in-range
  [skill-id field-id]
  (let [{lower-bound :min upper-bound :max} (field-definition skill-id field-id)
        default (int (field-default skill-id field-id))
      rounded-input (double (ability-config-common/finite-double (raw-value skill-id field-id) default))
      value (Math/round rounded-input)]
    (cond-> value
      (some? lower-bound) (max lower-bound)
      (some? upper-bound) (min upper-bound))))

(defn- within-bounds?
  [{lower-bound :min upper-bound :max} value]
  (and (or (nil? lower-bound) (<= (double lower-bound) (double value)))
       (or (nil? upper-bound) (<= (double value) (double upper-bound)))))

(defn tunable-double
  "Read a skill-specific action tunable as a bounded double.

  Invalid runtime values fall back to the descriptor default. This keeps bad
  server config edits from leaking NaN/Infinity/negative geometry into skill
  execution."
  [skill-id field-id]
  (let [field-def (field-definition skill-id field-id)
        default (double (field-default skill-id field-id))
      value (ability-config-common/finite-double (raw-value skill-id field-id) default)]
    (if (within-bounds? field-def value)
      value
      default)))

(defn tunable-int
  "Read a skill-specific action tunable as a bounded integer.

  Unlike core level config, action tunables fall back instead of clamping so an
  accidental out-of-range edit cannot silently reshape mechanics."
  [skill-id field-id]
  (let [field-def (field-definition skill-id field-id)
        default (int (field-default skill-id field-id))
      rounded-input (double (ability-config-common/finite-double (raw-value skill-id field-id) default))
      value (Math/round rounded-input)]
    (if (within-bounds? field-def value)
      value
      default)))

(defn tunable-double-list
  "Read a fixed-length list of bounded doubles for a skill action tunable.

  Length mismatches fall back to the descriptor default. Individual invalid
  entries fall back to their corresponding default entry."
  [skill-id field-id]
  (let [{:keys [list-count] :as field-def} (field-definition skill-id field-id)
        fallback (vec (field-default skill-id field-id))
        raw (raw-value skill-id field-id)]
    (if (and (ability-config-common/list-like? raw)
             (or (nil? list-count) (= (int list-count) (count raw))))
      (mapv (fn [value default]
              (let [d (ability-config-common/finite-double value default)]
                (if (within-bounds? field-def d)
                  d
                  (double default))))
            raw
            fallback)
      fallback)))

(defn tunable-int-list
  "Read a fixed-length list of bounded integers for a skill action tunable."
  [skill-id field-id]
  (let [{:keys [list-count] :as field-def} (field-definition skill-id field-id)
        fallback (vec (field-default skill-id field-id))
        raw (raw-value skill-id field-id)]
    (if (and (ability-config-common/list-like? raw)
             (or (nil? list-count) (= (int list-count) (count raw))))
      (mapv (fn [value default]
          (let [rounded-input (double (ability-config-common/finite-double value default))
            i (Math/round rounded-input)]
                (if (within-bounds? field-def i)
                  i
                  (int default))))
            raw
            fallback)
      fallback)))

(defn tunable-string-list
  "Read a list of strings for a skill action tunable.

  Blank entries are ignored. Non-list runtime edits fall back to defaults."
  [skill-id field-id]
  (let [fallback (vec (field-default skill-id field-id))
        raw (raw-value skill-id field-id)]
    (if (ability-config-common/list-like? raw)
      (let [values (->> raw
                        (map str)
                        (map str/trim)
                        (remove str/blank?)
                        vec)]
        (if (seq values) values fallback))
      fallback)))

(defn lerp-double
  [skill-id field-id exp]
  (let [[from to] (tunable-double-list skill-id field-id)]
    (+ (double from) (* (- (double to) (double from)) (double exp)))))

(defn lerp-int
  [skill-id field-id exp]
  (let [value (double (lerp-double skill-id field-id exp))]
    (int (Math/round value))))

(defn probability
  [skill-id field-id]
  (max 0.0 (min 1.0 (tunable-double skill-id field-id))))

(defn- boolean-value
  [skill-id field-id]
  (ability-config-common/boolean-value
    (raw-value skill-id field-id)
    (field-default skill-id field-id)))

(defn tunable-boolean
  [skill-id field-id]
  (boolean-value skill-id field-id))

(defn skill-enabled?
  [skill-id]
  (boolean-value skill-id :enabled))

(defn skill-controllable?
  [skill-id]
  (boolean-value skill-id :controllable))

(defn skill-level
  [skill-id]
  (int-in-range skill-id :level))

(defn destroy-blocks-enabled?
  [skill-id]
  (boolean-value skill-id :destroy-blocks))

(defn damage-scale
  [skill-id]
  (non-negative-double skill-id :damage-scale))

(defn cp-consume-speed
  [skill-id]
  (non-negative-double skill-id :cp-consume-speed))

(defn overload-consume-speed
  [skill-id]
  (non-negative-double skill-id :overload-consume-speed))

(defn exp-incr-speed
  [skill-id]
  (positive-double skill-id :exp-incr-speed))

(defn cooldown-scale
  [skill-id]
  (non-negative-double skill-id :cooldown-scale))

(defn cost-cp-scale
  [skill-id]
  (non-negative-double skill-id :cost-cp-scale))

(defn cost-overload-scale
  [skill-id]
  (non-negative-double skill-id :cost-overload-scale))

(defn- scale-value
  [value scale]
  (cond
    (fn? value)
    ;; Cost/cooldown fns are called either with a ctx map (current convention)
    ;; or legacy positional (player-id skill-id exp) — the scaled wrapper must
    ;; accept both, delegating to whichever arity `value` implements.
    (fn scaled
      ([ctx]
       (* (double scale)
          (double (or (try
                        (value ctx)
                        (catch clojure.lang.ArityException _
                          (value (:player-id ctx) (:skill-id ctx) (double (or (:exp ctx) 0.0)))))
                      0.0))))
      ([player-id skill-id exp]
       (* (double scale)
          (double (or (try
                        (value player-id skill-id exp)
                        (catch clojure.lang.ArityException _
                          (value {:player-id player-id :skill-id skill-id :exp exp})))
                      0.0)))))

    (number? value)
    (* (double scale) (double value))

    :else
    value))

(defn- scale-cost-stage
  [cost-stage cp-scale overload-scale]
  (cond-> cost-stage
    (contains? cost-stage :cp) (update :cp scale-value cp-scale)
    (contains? cost-stage :overload) (update :overload scale-value overload-scale)))

(defn- scale-cost
  [cost cp-scale overload-scale]
  (if (map? cost)
    (into {}
          (map (fn [[stage cost-stage]]
                 [stage (if (map? cost-stage)
                          (scale-cost-stage cost-stage cp-scale overload-scale)
                          cost-stage)]))
          cost)
    cost))

(defn- scale-cooldown-policy
  [policy cooldown-scale]
  (if (and (map? policy) (contains? policy :ticks))
    (update policy :ticks scale-value cooldown-scale)
    policy))

(defn apply-skill-overrides
  "Return a skill spec with the current per-skill config overlaid.

  The base registry keeps immutable skill definitions. This function is called
  when specs are read, so Forge config reloads are visible without re-registering
  content namespaces."
  [{:keys [id] :as spec}]
  (if-not (skill-configured? id)
    spec
    (let [cp-scale (cost-cp-scale id)
          overload-scale (cost-overload-scale id)
          cd-scale (cooldown-scale id)]
      (cond-> spec
        true (assoc :enabled (skill-enabled? id)
                    :controllable? (skill-controllable? id)
                    :level (skill-level id)
                    :destroy-blocks? (destroy-blocks-enabled? id)
                    :damage-scale (damage-scale id)
                    :cp-consume-speed (cp-consume-speed id)
                    :overload-consume-speed (overload-consume-speed id)
                    :exp-incr-speed (exp-incr-speed id))
        (contains? spec :cost) (update :cost scale-cost cp-scale overload-scale)
        (contains? spec :cooldown-ticks) (update :cooldown-ticks scale-value cd-scale)
        (contains? spec :cooldown-policy) (update :cooldown-policy scale-cooldown-policy cd-scale)))))

(defn- collect-config-errors
  []
  (vec
    (mapcat (fn [skill-id]
              (let [level-error
                    (when-not (<= 1 (skill-level skill-id) 5)
                      [(str (name skill-id) ".general.level must be between 1 and 5")])
                    non-negative-errors
                    (for [field-id [:damage-scale :cp-consume-speed :overload-consume-speed
                                    :cooldown-scale :cost-cp-scale :cost-overload-scale]
                          :let [value (ability-config-common/finite-double (raw-value skill-id field-id) -1.0)]
                          :when (neg? value)]
                      (str (name skill-id) "." (name field-id) " must be non-negative"))
                    exp-speed (ability-config-common/finite-double (raw-value skill-id :exp-incr-speed) 0.0)
                    exp-error (when-not (pos? exp-speed)
                                [(str (name skill-id) ".progression.exp-incr-speed must be positive")])]
                (concat level-error non-negative-errors exp-error)))
            all-skill-ids)))

(defn validate-config!
  []
  (let [errors (collect-config-errors)]
    (when (seq errors)
      (throw (ex-info "Invalid ability skill configuration" {:errors errors})))
    nil))
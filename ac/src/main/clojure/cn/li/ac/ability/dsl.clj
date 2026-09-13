(ns cn.li.ac.ability.dsl
  "Usage:
    (defcategory electrokinesis
      :id       :electrokinesis
      :name-key \"category.electrokinesis\"
      :level-matcher (fn [level] :normal)
      :prog-incr-rate 1.0)

    (defskill thunderbolt
      :id         :thunderbolt
      :category   :electrokinesis
      :name-key   \"skill.thunderbolt\"
      :max-level  3
      :can-control? true
      :conditions [(constantly true)])

  Macros only declare data. Runtime registration is performed by explicit
  content bootstrap functions."
  (:require [cn.li.ac.ability.definition-core :as definition-core]))

;; ============================================================================
;; defcategory
;; ============================================================================

(defmacro defcategory
  "Declare and register an ability category.
  Keys: :id :name-key :enabled? :level-matcher :prog-incr-rate :metadata"
  [sym & opts]
  (let [category-spec (definition-core/build-category-spec sym (apply hash-map opts))
        id           (:id category-spec)
        rest-map     (dissoc category-spec :id)]
    `(let [category-map# (assoc ~rest-map :id ~id :ac/content-type :category)]
       (def ~sym category-map#))))

;; ============================================================================
;; defskill
;; ============================================================================

(defmacro defskill
  "Declare a skill map without registration.

  :level and :controllable? must NOT be declared here — they're sourced from
  skill-config/skill-definitions (the single source of truth for those two
  fields; see registry/skill.clj register-skill!). Declaring them in defskill
  would be dead data that can silently drift out of sync with the config
  entry actually used at runtime (this happened in practice — thunder-bolt's
  defskill said level 4 while skill-definitions said level 2)."
  [sym & opts]
  (let [kv-map (apply hash-map opts)
        offending (filterv #(contains? kv-map %) [:level :controllable?])]
    (when (seq offending)
      (throw (ex-info (str "defskill " sym ": " offending " must not be declared here — "
                           "add/edit the entry for :" (name (definition-core/definition-id sym kv-map))
                           " in cn.li.ac.ability.skill-config.common/skill-definitions instead")
                      {:skill sym :offending-keys offending})))
    (let [skill-map (definition-core/build-skill-spec sym kv-map)]
      `(let [skill-map# (assoc ~skill-map :ac/content-type :skill)]
         (def ~sym skill-map#)))))

;; ============================================================================
;; def-skill-config-ops
;; ============================================================================

(defmacro def-skill-config-ops
  "Declare standard per-skill config and exp helper fns for content skills."
  [skill-id]
  `(do
     (defn- ~'cfg-double [field-id#]
       (cn.li.ac.ability.skill-config/tunable-double ~skill-id field-id#))
     (defn- ~'cfg-int [field-id#]
       (cn.li.ac.ability.skill-config/tunable-int ~skill-id field-id#))
     (defn- ~'cfg-boolean [field-id#]
       (cn.li.ac.ability.skill-config/tunable-boolean ~skill-id field-id#))
     (defn- ~'cfg-lerp [field-id# exp#]
       (cn.li.ac.ability.skill-config/lerp-double ~skill-id field-id# exp#))
     (defn- ~'cfg-lerp-int [field-id# exp#]
       (cn.li.ac.ability.skill-config/lerp-int ~skill-id field-id# exp#))
     (defn- ~'cfg-double-list [field-id#]
       (cn.li.ac.ability.skill-config/tunable-double-list ~skill-id field-id#))
     (defn- ~'cfg-string-set [field-id#]
       (set (cn.li.ac.ability.skill-config/tunable-string-list ~skill-id field-id#)))
     (defn- ~'cfg-probability [field-id#]
       (cn.li.ac.ability.skill-config/probability ~skill-id field-id#))
     (defn- ~'cfg-progression [field-id# exp#]
       (let [[base# scale#] (cn.li.ac.ability.skill-config/tunable-double-list ~skill-id field-id#)]
         (+ (double base#) (* (double scale#) (double exp#)))))
     (defn- ~'skill-exp [player-id#]
       (cn.li.ac.ability.service.skill-effects/skill-exp player-id# ~skill-id))))

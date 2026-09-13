(ns cn.li.ac.ability.registry.category
  "Categories group skills into thematic sets (e.g., esper, radiation, mist).
  Each category definition is a plain map stored in category-registry.

  Schema per category:
    {:id            keyword        ; unique identifier, e.g. :esper
     :name-key      string         ; i18n key
     :icon          string         ; resource path to icon texture
     :color         [r g b a]      ; RGBA floats for UI tinting
     :prog-incr-rate float         ; experience gain rate for this category
     :enabled       bool}

  Registry stored in Framework [:registry :categories]. Standard
  snapshot/freeze/register/reset plumbing lives in registry-core."
  (:require [cn.li.ac.ability.registry.registry-core :as registry-core]))

(def ^:private cat-path [:registry :categories])

(defn- validate-category! [{:keys [id] :as spec}]
  (when-not (and (keyword? id) (string? (:name-key spec)))
    (throw (IllegalArgumentException. "register-category!: id must be keyword, :name-key must be string"))))

(def ^:private ops
  (registry-core/make-registry-ops cat-path
                                   {:label "category"
                                    :validate! validate-category!}))

(defn create-category-registry-runtime
  "Composition-root factory: an isolated {registry frozen?} state atom that
  runtime-container can install/reinstall as a unit."
  ([]
   {::category-registry-runtime true
    :state* (atom {:registry {} :frozen? false})})
  ([{:keys [state*] :or {state* (atom {:registry {} :frozen? false})}}]
   {::category-registry-runtime true :state* state*}))

(defn install-category-registry-runtime!
  [runtime]
  (when-let [state* (:state* runtime)]
    ((:reset-for-test! ops) (:registry @state*)))
  runtime)

(defn category-registry-snapshot []
  ((:snapshot ops)))

(defn reset-category-registry-for-test!
  ([]
   (reset-category-registry-for-test! {}))
  ([snapshot]
   ((:reset-for-test! ops) snapshot)))

(defn freeze-category-registry! []
  ((:freeze! ops)))

(defn register-category!
  "Register a category spec with idempotent duplicate handling."
  [spec]
  ((:register! ops) spec))

(defn get-category [cat-id]
  ((:get ops) cat-id))

(defn get-all-categories []
  ((:get-all ops)))

(defn category-enabled? [cat-id]
  (boolean (:enabled (get-category cat-id))))

(defn get-prog-incr-rate [cat-id]
  (get-in ((:snapshot ops)) [cat-id :prog-incr-rate] 1.0))

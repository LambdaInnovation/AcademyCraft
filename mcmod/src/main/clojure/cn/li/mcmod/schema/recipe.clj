(ns cn.li.mcmod.schema.recipe
	"Malli contracts for low-frequency datagen recipe and achievement metadata."
	(:require [clojure.string :as str]
						[cn.li.mcmod.schema.core :as schema-core]))

(def ^:private resource-id-regex
	#"^[a-z0-9._-]+:[a-z0-9._/-]+$")

(defn- ^:private non-blank-str?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [v]
  (not (str/blank? v)))

(defn- ^:private non-empty-str?
  "Named predicate for Malli schema — avoids anon-fn AOT symbol leak."
  [s]
  (pos? (count s)))

(def ^:private non-blank-string-schema
	[:and string? [:fn non-blank-str?]])

(def ingredient-schema
	[:or
	 [:map [:item [:re resource-id-regex]]]
	 [:map [:tag [:re resource-id-regex]]]])

(def result-schema
	[:map
	 [:item [:re resource-id-regex]]
	 [:count {:optional true} [:and int? [:>= 1]]]])

(def shaped-recipe-schema
	[:map
	 [:id non-blank-string-schema]
	 [:type [:= :shaped]]
	 [:pattern [:sequential [:and string? [:fn non-empty-str?]]]]
	 [:key [:map-of char? ingredient-schema]]
	 [:result result-schema]])

(def shapeless-recipe-schema
	[:map
	 [:id non-blank-string-schema]
	 [:type [:= :shapeless]]
	 [:ingredients [:sequential ingredient-schema]]
	 [:result result-schema]])

(def smelting-recipe-schema
	[:map
	 [:id non-blank-string-schema]
	 [:type [:= :smelting]]
	 [:ingredient ingredient-schema]
	 [:result result-schema]
	 [:experience [:and number? [:>= 0.0]]]
	 [:cooking-time [:and int? [:>= 1]]]])

(def imag-fusor-recipe-schema
	[:map
	 [:id non-blank-string-schema]
	 [:type [:= :imag-fusor]]
	 [:input ingredient-schema]
	 [:output result-schema]
	 [:consume-liquid {:optional true} [:and int? [:>= 0]]]
	 [:time {:optional true} [:and int? [:>= 1]]]])

(def metal-former-recipe-schema
	[:map
	 [:id non-blank-string-schema]
	 [:type [:= :metal-former]]
	 [:input ingredient-schema]
	 [:output result-schema]
	 [:mode [:enum "plate" "incise" "etch" "refine"]]])

(def custom-process-recipe-schema
  [:map
   [:id non-blank-string-schema]
   [:type [:= :custom-process]]
   [:input ingredient-schema]
   [:output result-schema]
   [:consume-liquid {:optional true} [:and int? [:>= 0]]]
   [:time {:optional true} [:and int? [:>= 1]]]])

(def custom-mode-recipe-schema
  [:map
   [:id non-blank-string-schema]
   [:type [:= :custom-mode]]
   [:input ingredient-schema]
   [:output result-schema]
   [:mode [:enum "plate" "incise" "etch" "refine"]]])

(def recipe-schema
	[:or shaped-recipe-schema shapeless-recipe-schema smelting-recipe-schema
	     imag-fusor-recipe-schema metal-former-recipe-schema
     custom-process-recipe-schema custom-mode-recipe-schema])

(def recipes-schema
	[:sequential recipe-schema])

(def achievement-tab-schema
	[:map
	 [:id keyword?]
	 [:background [:re resource-id-regex]]])

(def achievement-tabs-schema
	[:sequential achievement-tab-schema])

(def criterion-schema
	[:or
	 [:map [:type [:= :custom]] [:criterion-id non-blank-string-schema]]
	 [:map [:type [:= :inventory-changed]] [:items [:sequential [:re resource-id-regex]]]]])

(def achievement-schema
	[:map
	 [:id non-blank-string-schema]
	 [:tab keyword?]
	 [:criteria [:sequential criterion-schema]]
	 [:translation [:map
									[:en_us [:map-of string? string?]]
									[:zh_cn [:map-of string? string?]]]]
	 [:icon {:optional true} [:re resource-id-regex]]
	[:parent {:optional true} [:or nil? non-blank-string-schema]]
	 [:frame {:optional true} [:enum :task :goal :challenge]]
	[:trigger-key {:optional true} [:or nil? map?]]
	 [:hidden? {:optional true} boolean?]])

(def achievements-schema
	[:sequential achievement-schema])

(def translations-schema
	[:map
	 [:en_us [:map-of string? string?]]
	 [:zh_cn [:map-of string? string?]]
	 [:zh_tw [:map-of string? string?]]
	 [:ja_jp [:map-of string? string?]]
	 [:ko_kr [:map-of string? string?]]
	 [:ru_ru [:map-of string? string?]]])

;; Validators use schema-core/lazy-validator to avoid calling schema/validator
;; (and therefore Malli) during namespace loading / AOT compilation.
;; NOTE: `delay` is deliberately NOT used — see `schema/lazy-validator`.

(def ^:private recipes-validator
	(schema-core/lazy-validator recipes-schema))

(def ^:private achievement-tabs-validator
	(schema-core/lazy-validator achievement-tabs-schema))

(def ^:private achievements-validator
	(schema-core/lazy-validator achievements-schema))

(def ^:private translations-validator
	(schema-core/lazy-validator translations-schema))

(defn require-recipes!
	[contract value]
	(schema-core/require-valid recipes-schema (recipes-validator) contract value))

(defn require-achievement-tabs!
	[contract value]
	(schema-core/require-valid achievement-tabs-schema (achievement-tabs-validator) contract value))

(defn require-achievements!
	[contract value]
	(schema-core/require-valid achievements-schema (achievements-validator) contract value))

(defn require-translations!
	[contract value]
	(schema-core/require-valid translations-schema (translations-validator) contract value))

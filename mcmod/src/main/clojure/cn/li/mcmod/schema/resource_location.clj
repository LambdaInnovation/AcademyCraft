(ns cn.li.mcmod.schema.resource-location
	"Platform-neutral ResourceLocation string validation/parsing helpers."
	(:require [clojure.string :as str]
						[cn.li.mcmod.schema.core :as schema-core]))

(def ^:private namespace-pattern
	#"^[a-z0-9._-]+$")

(def ^:private path-pattern
	#"^[a-z0-9._/-]+$")

(def namespace-schema
	[:and
	 string?
	 [:re namespace-pattern]])

(def path-schema
	[:and
	 string?
	 [:re path-pattern]])

(def resource-location-schema
	[:and
	 string?
	 [:re #"^[a-z0-9._-]+:[a-z0-9._/-]+$"]])

;; Validators use schema-core/lazy-validator to avoid calling schema/validator
;; (and therefore Malli) during namespace loading / AOT compilation.
;; NOTE: `delay` is deliberately NOT used — see `schema/lazy-validator`.

(def ^:private namespace-validator
	(schema-core/lazy-validator namespace-schema))

(def ^:private path-validator
	(schema-core/lazy-validator path-schema))

(def ^:private resource-location-validator
	(schema-core/lazy-validator resource-location-schema))

(defn valid-namespace?
	[value]
	(schema-core/valid? (namespace-validator) value))

(defn valid-path?
	[value]
	(schema-core/valid? (path-validator) value))

(defn valid-resource-location?
	[value]
	(schema-core/valid? (resource-location-validator) value))

(defn- invalid-resource-location!
	[value default-namespace]
	(throw (ex-info "Invalid resource location string"
									{:value value
									 :default-namespace default-namespace})))

(defn parse-resource-location
	([value] (parse-resource-location value nil))
	([value default-namespace]
	 (let [s (str value)]
		 (if (str/includes? s ":")
			 (let [[namespace path] (str/split s #":" 2)]
				 (if (and (valid-namespace? namespace)
									(valid-path? path))
					 {:namespace namespace :path path}
					 (invalid-resource-location! value default-namespace)))
			 (if (and (valid-namespace? default-namespace)
								(valid-path? s))
				 {:namespace (str default-namespace)
					:path s}
				 (invalid-resource-location! value default-namespace))))))

(defn normalize-resource-location
	([value] (normalize-resource-location value nil))
	([value default-namespace]
	 (let [{:keys [namespace path]} (parse-resource-location value default-namespace)]
		 (str namespace ":" path))))
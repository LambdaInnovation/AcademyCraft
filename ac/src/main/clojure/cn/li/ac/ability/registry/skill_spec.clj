(ns cn.li.ac.ability.registry.skill-spec
	"Skill spec defaults, validation, and normalization.

	This namespace is pure with respect to registry storage: it does not read or
	write the skill-registry atom. Keeping spec normalization independent avoids
	registry/skill <-> registry/skill-spec require cycles."
	(:require [cn.li.ac.ability.domain.developer :as developer]
					[cn.li.ac.ability.registry.skill-schema :as schema]))

(defn build-defaults
	"Build default fields derived from the minimal skill spec."
	[{:keys [level]}]
	{:controllable?          true
	 :damage-scale           1.0
	 :cp-consume-speed       1.0
	 :overload-consume-speed 1.0
	 :cooldown-ticks         nil
	 :exp-incr-speed         1.0
	 :destroy-blocks?        true
	 :enabled                true
	 :prerequisites          []
	 :conditions             []
	 :developer-type         (developer/min-for-level level)
	 :cooldown               {:mode :default}
	 :targeting {}
	 :transitions {}
	 :exp-policy {}
	 :cooldown-policy {}
	 :state {}
	 :ops {}
	 :perform []
	 :aim {}
	 :exp {}})

(defn- resolve-fn-ref [v]
	(cond
		(fn? v) v
		(var? v) (var-get v)
		:else v))

(defn- normalize-op [id op]
	(when-not (and (vector? op)
						 (= 2 (count op))
						 (keyword? (first op))
						 (map? (second op)))
		(throw (ex-info "Each effect op must be [keyword map]"
								{:skill-id id :op op})))
	op)

(defn normalize-merged
	"Normalize callback vars, effect ops, and FX payload refs in a merged skill map."
	[{:keys [id] :as merged}]
	(let [actions* (into {} (map (fn [[k v]] [k (resolve-fn-ref v)])) (or (:actions merged) {}))
			ops*     (into {} (map (fn [[stage ops]] [stage (mapv #(normalize-op id %) (or ops []))]))
							 (or (:ops merged) {}))
			perform* (mapv #(normalize-op id %) (or (:perform merged) []))
			fx*      (into {} (map (fn [[k evt]]
											 [k (if (map? evt)
													(update evt :payload resolve-fn-ref)
													evt)]))
							 (or (:fx merged) {}))]
		(-> merged
				(assoc :actions actions*)
				(assoc :ops ops*)
				(assoc :perform perform*)
				(assoc :fx fx*))))

(defn normalize-skill-spec
	"Validate and normalize a skill spec for storage in the skill registry."
	[spec]
	(schema/validate! spec)
	(-> (merge (build-defaults spec) spec)
			normalize-merged))

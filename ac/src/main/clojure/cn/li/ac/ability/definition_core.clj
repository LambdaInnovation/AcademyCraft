(ns cn.li.ac.ability.definition-core
	"Pure ability definition helpers used by DSL and bootstrap layers.")

(defn normalize-prereqs
	"Normalize prerequisite declarations into canonical vector form."
	[value]
	(cond
		(map? value)
		(mapv (fn [[skill-id min-exp]]
						{:skill-id skill-id :min-exp (double min-exp)})
					value)

		(vector? value)
		value

		:else
		[]))

(defn definition-id
	"Resolve a stable identifier from DSL options."
	[sym kv-map]
	(or (:id kv-map)
			(keyword (name sym))))

(defn build-category-spec
	"Build a normalized category spec map from DSL input."
	[sym kv-map]
	(assoc (dissoc kv-map :id)
				 :id (definition-id sym kv-map)))

(defn build-skill-spec
	"Build a normalized skill spec map from DSL input."
	[sym kv-map]
	(let [id (definition-id sym kv-map)
				category-id (or (:category-id kv-map) (:category kv-map))
				prerequisites (normalize-prereqs (or (:prerequisites kv-map)
																						 (:prereqs kv-map)))]
		(cond-> (-> kv-map
								(dissoc :id :category :prereqs)
								(assoc :id id)
								(assoc :prerequisites prerequisites))
			category-id
			(assoc :category-id category-id))))
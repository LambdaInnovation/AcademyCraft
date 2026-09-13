(ns cn.li.ac.ability.util.resource-check
	"Shared predicates for ability resource usability checks.")

(defn can-use-resource-data?
	"True when activated && not in overload recovery && no interference."
	[resource-data]
	(and (boolean (:activated resource-data))
			 (:overload-fine resource-data true)
			 (empty? (:interferences resource-data #{}))))
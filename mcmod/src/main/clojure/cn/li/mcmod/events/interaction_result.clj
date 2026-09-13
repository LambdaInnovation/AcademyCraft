(ns cn.li.mcmod.events.interaction-result
	"Predicates for right-click block handler return values.

	Handlers may return:
		nil                          - no handler / not consumed (vanilla proceeds)
		{:consume? true}             - explicitly consumed, cancel vanilla
		{:consume? true :messages …} - consumed + feedback messages
		{:gui-id … :player … …}     - old-style GUI-open signal (also consumed)
		any other truthy value       - consumed (side-effect already performed)")

(defn interaction-consumed?
	"Returns true if the right-click handler result indicates the interaction was
	consumed and vanilla block/item processing should be cancelled."
	[ret]
	(boolean ret))

(defn gui-open-result?
	"Returns true if the right-click handler result is a GUI-open signal map.
	The map must contain a :gui-id key (and typically :player, :world, :pos)."
	[ret]
	(and (map? ret)
			 (contains? ret :gui-id)))
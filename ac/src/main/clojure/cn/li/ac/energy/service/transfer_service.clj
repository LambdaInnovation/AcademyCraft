(ns cn.li.ac.energy.service.transfer-service
	"Energy transfer service for Phase C.

	Centralizes transfer math so callers no longer duplicate ad-hoc formulas.
	The current wireless transfer policy is explicit:
	- wireless transfer applies a default 10% loss model
	- callers can override loss-rate when needed")

(def default-wireless-loss-rate 0.10)

(defn clamp-loss-rate
	"Clamp an arbitrary loss rate into [0.0, 1.0]."
	[loss-rate]
	(-> (double (or loss-rate 0.0))
			(max 0.0)
			(min 1.0)))

(defn transfer-loss
	"Compute transfer loss amount for a given amount/loss-rate."
	([amount]
	 (transfer-loss amount default-wireless-loss-rate))
	([amount loss-rate]
	 (let [safe-amount (max 0.0 (double (or amount 0.0)))
				 rate (clamp-loss-rate loss-rate)]
		 (* safe-amount rate))))

(defn transfer-result
	"Return a normalized transfer result map.

	Keys:
	- :requested   requested amount
	- :transferred amount delivered after loss
	- :lost        amount lost during transfer
	- :loss-rate   normalized loss rate"
	([amount]
	 (transfer-result amount default-wireless-loss-rate))
	([amount loss-rate]
	 (let [requested (max 0.0 (double (or amount 0.0)))
				 rate (clamp-loss-rate loss-rate)
				 lost (transfer-loss requested rate)]
		 {:requested requested
			:transferred (- requested lost)
			:lost lost
			:loss-rate rate})))

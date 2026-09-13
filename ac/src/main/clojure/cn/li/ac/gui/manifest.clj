(ns cn.li.ac.gui.manifest
	"Central AC GUI manifest data.

	AC content namespaces still own behavior (containers, screens, handlers), while
	this namespace owns cross-cutting GUI catalog data such as wireless message
	domains. Keeping this data here avoids scattered registration calls in block
	initializers."
	(:require [clojure.set :as set]))

(def message-domain-actions
	"Wireless GUI message actions keyed by GUI/message domain.

	These domains are consumed by cn.li.ac.wireless.gui.message.bootstrap during
	content startup and by tests/guards that need one source of truth."
	{:matrix [:gather-info :init :change-ssid :change-password]
	 :node [:change-name :change-password :list-networks :connect :disconnect]
	 :generator [:list-nodes :connect :disconnect]
	 :metal-former [:alternate]
	 :developer [:start-development :stop-development :list-nodes :connect :disconnect]
	 :ability-interferer [:change-range :toggle-enabled :set-whitelist :add-to-whitelist :remove-from-whitelist :list-nodes :connect :disconnect]})

(def message-domain-contracts
	"Registry-phase handler contracts for wireless GUI message domains.

	Validated when domains are registered and again after runtime handler wiring."
	(into {}
	      (map (fn [domain] [domain {:owner-spec :server :payload-routing :sync-routing}]))
	      (keys message-domain-actions)))

(def gui-definitions
	"Static AC GUI registration metadata keyed by content GUI key.

	GUI implementation namespaces own behavior callbacks (container/screen/sync/slot
	functions). This manifest owns stable IDs and metadata that must remain unique
	and platform-visible."
	{:wireless-node {:gui-name "wireless-node"
					 :gui-id 0
					 :display-name "Wireless Node"
					 :gui-type :node
					 :registry-name "wireless_node_gui"
					 :screen-factory-fn-kw :create-node-screen
					 :slot-schema-id :wireless-node
					 :message-domain :node
}
	 :wireless-matrix {:gui-name "wireless-matrix"
					 :gui-id 1
					 :display-name "Wireless Matrix"
					 :gui-type :matrix
					 :registry-name "wireless_matrix_gui"
					 :screen-factory-fn-kw :create-matrix-screen
					 :slot-schema-id :wireless-matrix
					 :message-domain :matrix
}
	 :solar-gen {:gui-name "solar-gen"
			 :gui-id 2
			 :display-name "Solar Generator"
			 :gui-type :solar
			 :registry-name "solar_gen_gui"
			 :screen-factory-fn-kw :create-solar-screen
			 :slot-schema-id :solar-gen
			 :message-domain :generator
}
	 :wind-gen-main {:gui-name "wind-gen-main"
				 :gui-id 3
				 :display-name "Wind Generator Main"
				 :gui-type :wind-gen-main
				 :registry-name "wind_gen_main_gui"
				 :screen-factory-fn-kw :create-wind-main-screen
				 :slot-schema-id :wind-gen-main
}
	 :wind-gen-base {:gui-name "wind-gen-base"
				 :gui-id 4
				 :display-name "Wind Generator Base"
				 :gui-type :wind-gen-base
				 :registry-name "wind_gen_base_gui"
				 :screen-factory-fn-kw :create-wind-base-screen
				 :slot-schema-id :wind-gen-base
}
	 :imag-fusor {:gui-name "imag-fusor"
			  :gui-id 5
			  :display-name "Imag Fusor"
			  :gui-type :imag-fusor
			  :registry-name "imag_fusor_gui"
			  :screen-factory-fn-kw :create-imag-fusor-screen
			  :slot-schema-id :imag-fusor
}
	 :metal-former {:gui-name "metal-former"
				 :gui-id 6
				 :display-name "Metal Former"
				 :gui-type :metal-former
				 :registry-name "metal_former_gui"
				 :screen-factory-fn-kw :create-metal-former-screen
				 :slot-schema-id :metal-former
				 :message-domain :metal-former
}
	 :phase-gen {:gui-name "phase-gen"
			 :gui-id 7
			 :display-name "Phase Generator"
			 :gui-type :phase-gen
			 :registry-name "phase_gen_gui"
			 :screen-factory-fn-kw :create-phase-gen-screen
			 :slot-schema-id :phase-gen
}
	 :developer {:gui-name "developer"
			 :gui-id 13
			 :display-name "Ability Developer"
			 :gui-type :developer
			 :registry-name "developer_gui"
			 :screen-factory-fn-kw :create-developer-screen
			 :slot-schema-id :developer-gui
			 :message-domain :developer
}
	 :energy-converter {:gui-name "energy-converter"
				    :gui-id 14
				    :display-name "Energy Converter"
				    :gui-type :energy-converter
				    :registry-name "energy_converter_gui"
				    :screen-factory-fn-kw :create-energy-converter-screen
				    :slot-schema-id :energy-converter
				    :message-domain nil
}
	 :ability-interferer {:gui-name "ability-interferer"
				       :gui-id 15
				       :display-name "Ability Interferer"
				       :gui-type :ability-interferer
				       :registry-name "ability_interferer_gui"
				       :screen-factory-fn-kw :create-ability-interferer-screen
				       :slot-schema-id :ability-interferer
				       :message-domain :ability-interferer
}})

(def gui-registration-keys
	[:gui-id :display-name :gui-type :registry-name :screen-factory-fn-kw :slot-schema-id])

(defn gui-definition
	"Return the full static manifest entry for a GUI key."
	[gui-key]
	(get gui-definitions gui-key))

(defn require-gui-definition
	"Return a manifest entry or fail fast with the unknown GUI key."
	[gui-key]
	(or (gui-definition gui-key)
		(throw (ex-info "Unknown AC GUI manifest key" {:gui-key gui-key}))))

(defn gui-name
	"Return the content GUI name used by mcmod.gui parser/registry."
	[gui-key]
	(:gui-name (require-gui-definition gui-key)))

(defn gui-id
	"Return the stable integer GUI id for a GUI key."
	[gui-key]
	(:gui-id (require-gui-definition gui-key)))

(defn gui-definition-for-type
	"Return the manifest entry whose business GUI type matches gui-type."
	[gui-type]
	(some (fn [[_ entry]]
			(when (= gui-type (:gui-type entry))
				entry))
		  gui-definitions))

(defn gui-id-for-type
	"Return the stable integer GUI id for a business GUI type keyword."
	[gui-type]
	(:gui-id (gui-definition-for-type gui-type)))

(defn gui-registration
	"Return the static metadata map consumed by ac.block.gui.registration/register-block-gui!."
	[gui-key]
	(select-keys (require-gui-definition gui-key) gui-registration-keys))

(defn message-domains
	"Return all known message domains."
	[]
	(keys message-domain-actions))

(defn message-actions
	"Return the declared action vector for a message domain."
	[domain]
	(get message-domain-actions domain))

(defn message-domain-contract
	"Return the registry contract map for a wireless GUI message domain."
	[domain]
	(or (get message-domain-contracts domain)
		(throw (ex-info "Unknown wireless GUI message domain contract" {:domain domain}))))

(defn missing-message-domains
	"Return required domains that are absent from the manifest."
	[required-domains]
	(set/difference (set required-domains) (set (message-domains))))
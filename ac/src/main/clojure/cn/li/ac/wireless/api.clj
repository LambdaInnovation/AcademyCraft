(ns cn.li.ac.wireless.api
		"Canonical public API for wireless queries and topology commands."
		(:require [cn.li.ac.wireless.core.vblock :as vb]
						[cn.li.ac.wireless.core.capability-resolver :as resolver]
						[cn.li.ac.wireless.data.network-state :as network-state]
						[cn.li.ac.wireless.data.store :as store]
						[cn.li.ac.wireless.service.commands :as commands]
						[cn.li.ac.wireless.service.queries :as queries]
						[cn.li.ac.wireless.data.network-lookup :as lookup]
						[cn.li.ac.wireless.data.world-registry :as world-registry]
						[cn.li.mcmod.platform.be :as platform-be]
						[cn.li.mcmod.framework :as fw]
						[cn.li.mcmod.framework.platform :as platform]
						[cn.li.mcmod.util.log :as log])
		(:import [cn.li.acapi.wireless
							IWirelessGenerator
							IWirelessMatrix
							IWirelessNode
							IWirelessReceiver]))

(def network-snapshot network-state/snapshot)
(def network-ssid network-state/get-ssid)
(def network-password network-state/get-password)
(def network-load network-state/get-load)
(def network-active? network-state/active?)

(defn fire-event!
	"Post an event object to the installed platform event bus. No-op when the
	platform does not provide an event bridge."
	[event]
	(when-let [fw-atom (fw/fw-atom)]
		(when event
			(try
							(platform/call-adapter-optional fw-atom :event-bus :fire! event)
				(catch Exception e
					(log/warn "Event dispatch failed:" (ex-message e)))))))

(defn get-wireless-net-by-matrix
		[matrix-tile]
		(queries/find-network-by-matrix matrix-tile))

(defn get-wireless-net-by-node
		[node-tile]
		(queries/find-network-by-node node-tile))

(defn get-wireless-net-by-ssid
		[world ssid]
		(queries/find-network-by-ssid world ssid))

(defn is-node-linked?
		[node-tile]
		(some? (get-wireless-net-by-node node-tile)))

(defn is-matrix-active?
		[matrix-tile]
		(some? (get-wireless-net-by-matrix matrix-tile)))

(defn get-nets-in-range
		[world x y z range max-results]
		(queries/find-networks-in-range world x y z range max-results))

(defn get-node-conn-by-node
		[node-tile]
		(queries/find-node-connection-by-node node-tile))

(defn get-node-conn-by-generator
		[gen-tile]
		(queries/find-node-connection-by-generator gen-tile))

(defn get-node-conn-by-receiver
		[rec-tile]
		(queries/find-node-connection-by-receiver rec-tile))

(defn is-receiver-linked?
		[rec-tile]
		(some? (get-node-conn-by-receiver rec-tile)))

(defn is-generator-linked?
		[gen-tile]
		(some? (get-node-conn-by-generator gen-tile)))

(defn get-nodes-in-range-at
		[world x y z]
		(queries/find-available-nodes-at world x y z))

(defn get-nodes-in-range
		[world pos]
		(queries/find-available-nodes world pos))

(declare destroy-network!)

(defn create-network!
		[matrix-tile ssid password]
		(let [world (platform-be/be-get-world-safe matrix-tile)
					world-data (world-registry/get-world-data world)
					matrix-vb (vb/create-vmatrix matrix-tile)
	        ;; Functional parity: recreating a network on the same matrix replaces the old one.
	        _ (when (some? (lookup/get-network-by-matrix world-data matrix-vb))
	            (destroy-network! matrix-tile))
					result (commands/create-network! world-data matrix-vb ssid password)]
			(when (:success result)
					(when-let [matrix-cap (resolver/matrix-capability matrix-tile)]
					(fire-event!
						{:kind :topology/network
						 :action :created
						 :ssid ssid
						 :matrix ^IWirelessMatrix matrix-cap})))
			result))

(defn destroy-network!
		[matrix-tile]
		(when-let [network-item (get-wireless-net-by-matrix matrix-tile)]
			(let [world (platform-be/be-get-world-safe matrix-tile)
						world-data (world-registry/get-world-data world)
						ssid (network-state/get-ssid network-item)
						result (commands/destroy-network! world-data network-item)]
				(when (:success result)
						(when-let [matrix-cap (resolver/matrix-capability matrix-tile)]
						(fire-event!
							{:kind :topology/network
							 :action :destroyed
							 :ssid ssid
							 :matrix ^IWirelessMatrix matrix-cap})))
				result)))

(defn destroy-network-at!
	"Destroy the network whose matrix sits at (x,y,z) — matrix-break cleanup.
	Works without a live BlockEntity; the :destroyed topology event fires
	best-effort while the matrix capability still resolves."
	[world x y z]
	(when-let [world-data (world-registry/get-world-data-non-create world)]
		(let [matrix-vb (vb/create-vmatrix x y z)]
			(when-let [network-item (lookup/get-network-by-matrix world-data matrix-vb)]
				(let [ssid (network-state/get-ssid network-item)
							matrix-cap (resolver/resolve-matrix-cap world matrix-vb)
							result (commands/destroy-network! world-data network-item)]
					(when (and (:success result) matrix-cap)
						(fire-event!
							{:kind :topology/network
							 :action :destroyed
							 :ssid ssid
							 :matrix ^IWirelessMatrix matrix-cap}))
					result)))))

(defn cleanup-node-at!
	"Node-break cleanup by position: untrack from the spatial index, unlink
	from its network, and destroy its node connection. Works without a live
	BlockEntity; topology events fire best-effort while caps still resolve."
	[world x y z]
	(when-let [world-data (world-registry/get-world-data-non-create world)]
		(let [node-vb (vb/create-vnode x y z)]
			(store/remove-spatial-vblock! world-data node-vb)
			(when-let [network-item (lookup/get-network-by-node world-data node-vb)]
				(let [result (commands/unlink-node-from-network! network-item node-vb)]
					(when (:success result)
						(when-let [node-cap (resolver/resolve-node-cap world node-vb)]
							(when-let [matrix-cap (resolver/resolve-matrix-cap world (:matrix network-item))]
								(fire-event!
									{:kind :topology/node
									 :action :disconnected
									 :matrix ^IWirelessMatrix matrix-cap
									 :node ^IWirelessNode node-cap}))))))
			(when-let [conn (lookup/get-node-connection world-data node-vb)]
				(commands/destroy-node-connection! world-data conn))
			nil)))

(defn link-node-to-network!
		[node-tile matrix-tile password]
		(when-let [network-item (get-wireless-net-by-matrix matrix-tile)]
			(let [world (platform-be/be-get-world-safe matrix-tile)
						world-data (world-registry/get-world-data world)
						node-vb (vb/create-vnode node-tile)
						result (commands/link-node-to-network! world-data network-item node-vb password world)]
				(when (:success result)
					(when-let [matrix-cap (resolver/matrix-capability matrix-tile)]
						(when-let [node-cap (resolver/node-capability node-tile)]
							(fire-event!
								{:kind :topology/node
								 :action :connected
								 :matrix ^IWirelessMatrix matrix-cap
								 :node ^IWirelessNode node-cap}))))
				result)))

(defn unlink-node-from-network!
		[node-tile]
		(when-let [network-item (get-wireless-net-by-node node-tile)]
			(let [world (platform-be/be-get-world-safe node-tile)
						matrix-tile (when-let [matrix-vb (:matrix network-item)]
											(vb/vblock-get matrix-vb world))
						node-vb (vb/create-vnode node-tile)
						result (commands/unlink-node-from-network! network-item node-vb)]
				(when (:success result)
						(when-let [node-cap (resolver/node-capability node-tile)]
							(when-let [matrix-cap (some-> matrix-tile resolver/matrix-capability)]
							(fire-event!
								{:kind :topology/node
								 :action :disconnected
								 :matrix ^IWirelessMatrix matrix-cap
								 :node ^IWirelessNode node-cap}))))
				result)))

(defn connect-node-to-ssid!
		[world node-tile ssid password]
		(let [network (get-wireless-net-by-ssid world ssid)
				matrix-tile (when-let [matrix-vb (:matrix network)]
									(vb/vblock-get matrix-vb world))]
			(if (and network matrix-tile)
				(link-node-to-network! node-tile matrix-tile password)
				{:success false :reason :not-found})))

;; ============================================================================
;; Shared link / unlink plumbing
;; ============================================================================

(def ^:private device-ops
  "Per-device-type capability and command dispatch — eliminates copy-paste
  across the four link/unlink functions. Values are vars so runtime rebinding
  (tests, tooling) is honored."
  {:generator {:resolve-cap   #'resolver/generator-capability
               :create-vb     #'vb/create-vgenerator
               :link-cmd      #'commands/link-generator-to-connection!
               :unlink-cmd    #'commands/unlink-generator-from-connection!
               :get-conn      #'get-node-conn-by-generator
               :link-action   :generator-linked
               :unlink-action :generator-unlinked
               :event-key     :generator
               :not-device    :not-a-generator}
   :receiver  {:resolve-cap   #'resolver/receiver-capability
               :create-vb     #'vb/create-vreceiver
               :link-cmd      #'commands/link-receiver-to-connection!
               :unlink-cmd    #'commands/unlink-receiver-from-connection!
               :get-conn      #'get-node-conn-by-receiver
               :link-action   :receiver-linked
               :unlink-action :receiver-unlinked
               :event-key     :receiver
               :not-device    :not-a-receiver}})

(defn- link-device!
  "Shared implementation for link-generator-to-node! / link-receiver-to-node!.
  Order: node capability → password auth → device capability → link."
  [device-tile node-tile password need-auth device-type]
  (let [{:keys [resolve-cap create-vb link-cmd link-action event-key not-device]}
        (get device-ops device-type)]
    (if-let [node-cap (resolver/node-capability node-tile)]
      (if-not (or (not need-auth)
                  (= (str password) (str (.getPassword ^IWirelessNode node-cap))))
        (do (log/info "[link-device!]" device-type "password mismatch")
            {:success false :reason :password})
        (if-let [dev-cap (resolve-cap device-tile)]
          (let [world      (platform-be/be-get-world-safe node-tile)
                world-data (world-registry/get-world-data world)
                node-vb    (vb/create-vnode-conn node-tile)
                conn       (commands/ensure-node-connection! world-data node-vb)
                dev-vb     (create-vb device-tile)
                result     (link-cmd world-data conn dev-vb world)]
            (when (:success result)
              (fire-event!
                {:kind :topology/node :action link-action
                 :node ^IWirelessNode node-cap
                 event-key dev-cap}))
            result)
          (do (log/info "[link-device!]" device-type "device " (name device-type) " has no capability. block-id=" (some-> device-tile platform-be/get-block-id))
              {:success false :reason not-device})))
      (do (log/info "[link-device!]" device-type "target has no IWirelessNode. block-id=" (some-> node-tile platform-be/get-block-id))
          {:success false :reason :not-a-node}))))

(defn- unlink-device!
  "Shared implementation for unlink-generator-from-node! / unlink-receiver-from-node!."
  [device-tile device-type]
  (let [{:keys [resolve-cap create-vb unlink-cmd get-conn unlink-action event-key]}
        (get device-ops device-type)]
    (when-let [conn (get-conn device-tile)]
      (let [world  (platform-be/be-get-world-safe device-tile)
            dev-vb (create-vb device-tile)
            result (unlink-cmd conn dev-vb)]
        (when (:success result)
          (when-let [dev-cap (resolve-cap device-tile)]
            (when-let [node-cap (resolver/resolve-node-cap world (:node conn))]
              (fire-event!
                {:kind :topology/node :action unlink-action
                 :node ^IWirelessNode node-cap
                 event-key dev-cap}))))
        result))))

;; ============================================================================
;; Public link / unlink API
;; ============================================================================

(defn link-generator-to-node!
  [gen-tile node-tile password need-auth]
  (link-device! gen-tile node-tile password need-auth :generator))

(defn link-receiver-to-node!
  [rec-tile node-tile password need-auth]
  (link-device! rec-tile node-tile password need-auth :receiver))

(defn unlink-generator-from-node!
  [gen-tile]
  (unlink-device! gen-tile :generator))

(defn unlink-receiver-from-node!
  [rec-tile]
  (unlink-device! rec-tile :receiver))

(defn change-network-ssid!
		[network new-ssid]
		(commands/change-network-ssid! network new-ssid))

(defn change-network-password!
		[network new-password]
		(commands/reset-network-password! network new-password))

(defn destroy-node-connection-for-node!
		"Remove the node connection registered for a wireless node tile, if any."
		[node-tile]
		(when-let [conn (get-node-conn-by-node node-tile)]
			(let [world (platform-be/be-get-world-safe node-tile)
						world-data (world-registry/get-world-data world)]
				(commands/destroy-node-connection! world-data conn))))

(defn register-node-spatial!
		"Track a placed wireless node in the world spatial index."
		[world node-vblock]
		(let [world-data (world-registry/get-world-data world)]
			(store/add-spatial-vblock! world-data node-vblock)))

(defn unregister-node-spatial!
		"Remove a wireless node from the world spatial index."
		[world node-vblock]
		(when-let [world-data (world-registry/get-world-data-non-create world)]
			(store/remove-spatial-vblock! world-data node-vblock)))

(ns cn.li.ac.wireless.service.commands
  "Application-level wireless topology commands.

  Owns admission rules (ssid uniqueness, password, capacity, range — the
  checks that need lookups or capability IO) and delegates the actual state
  transitions to `wireless.data.store` primitives."
  (:require [cn.li.ac.wireless.core.vblock :as vb]
            [cn.li.ac.wireless.data.entity-commit :as entity-commit]
            [cn.li.ac.wireless.data.network-lookup :as lookup]
            [cn.li.ac.wireless.data.network-state :as network-state]
            [cn.li.ac.wireless.data.node-conn :as node-conn]
            [cn.li.ac.wireless.data.store :as store]
            [cn.li.ac.wireless.domain.model :as model]
            [cn.li.ac.wireless.domain.topology :as topology]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessMatrix]))

;; ============================================================================
;; Admission rules
;; ============================================================================

(defn- can-create-network?
  [world-data ssid matrix-vblock]
  (and (nil? (lookup/get-network-by-ssid world-data ssid))
       (nil? (lookup/get-network-by-matrix world-data matrix-vblock))))

(defn- ssid-available?
  [world-data network new-ssid]
  (let [existing (lookup/get-network-by-ssid world-data new-ssid)]
    (or (nil? existing)
        (= (vb/pos-of (:matrix existing)) (vb/pos-of (:matrix network))))))

(defn- password-valid?
  [network password-attempt]
  (= password-attempt (network-state/get-password network)))

(defn- network-has-node-capacity?
  [network world]
  (model/network-has-capacity?
    {:nodes (network-state/get-nodes network)}
    (network-state/get-capacity network world)))

(defn- matrix-in-range?
  [network node-vblock world]
  (if-let [matrix (network-state/get-matrix network world)]
    (let [range (.getMatrixRange ^IWirelessMatrix matrix)
          dist-sq (vb/dist-sq node-vblock (:matrix network))]
      (<= dist-sq (* range range)))
    false))

(defn validate-add-node
  [network node-vblock password-attempt world]
  (cond
    (not (password-valid? network password-attempt))
    {:ok false :reason :password}

    (not (network-has-node-capacity? network world))
    {:ok false :reason :capacity}

    (not (matrix-in-range? network node-vblock world))
    {:ok false :reason :range}

    :else
    {:ok true}))

;; ============================================================================
;; Network commands
;; ============================================================================

(defn create-network!
  [world-data matrix-vblock ssid password]
  (if-not (can-create-network? world-data ssid matrix-vblock)
    {:success false :reason :ssid-exists}
    (do
      (store/register-network!
        world-data
        (network-state/create-wireless-net world-data matrix-vblock ssid password))
      (log/info (format "Created network: SSID='%s'" ssid))
      {:success true})))

(defn destroy-network!
  [world-data item]
  (store/unregister-network! world-data item)
  {:success true})

(defn change-network-ssid!
  [network-item new-ssid]
  (let [world-data (:world-data network-item)
        network-item (entity-commit/resolve-network world-data network-item)
        old-ssid (network-state/get-ssid network-item)]
    (cond
      (= old-ssid new-ssid)
      {:success true}

      (not (ssid-available? world-data network-item new-ssid))
      (do
        (log/info (format "SSID change rejected: target SSID '%s' already exists" new-ssid))
        {:success false :reason :ssid-taken})

      :else
      (do
        (store/rename-network!
          world-data
          (network-state/set-state-value network-item :ssid new-ssid)
          old-ssid new-ssid)
        {:success true}))))

(defn reset-network-password!
  [network-item new-password]
  (let [network-item (entity-commit/resolve-network (:world-data network-item) network-item)
        updated (network-state/set-state-value! network-item :password new-password)]
    (log/info (format "Network '%s' password changed" (network-state/get-ssid updated)))
    {:success true}))

;; ============================================================================
;; Node membership commands
;; ============================================================================

(defn unlink-node-from-network!
  [network-item node-vb]
  (if (store/unlink-node! (:world-data network-item) network-item node-vb)
    {:success true}
    {:success false}))

(defn link-node-to-network!
  [world-data net node-vblock password-attempt world]
  (let [net (entity-commit/resolve-network world-data net)]
    (when-let [old-net (lookup/get-network-by-node world-data node-vblock)]
      (when-not (= (vb/pos-of (:matrix old-net)) (vb/pos-of (:matrix net)))
        (store/unlink-node! world-data old-net node-vblock)))
    (let [validation (validate-add-node net node-vblock password-attempt world)]
      (if-not (:ok validation)
        (do
          (log/info (format "Node add failed: reason=%s for '%s'"
                            (:reason validation)
                            (network-state/get-ssid net)))
          {:success false :reason (:reason validation)})
        (do
          (store/link-node!
            world-data
            (topology/add-node-to-network net node-vblock)
            node-vblock)
          {:success true})))))

;; ============================================================================
;; Connection commands
;; ============================================================================

(defn create-node-connection!
  [world-data node-vblock]
  (if (lookup/get-node-connection world-data node-vblock)
    {:success false :reason :already-exists}
    {:success true
     :connection (store/register-connection!
                   world-data
                   (node-conn/create-node-conn world-data node-vblock))}))

(defn destroy-node-connection!
  [world-data item]
  (store/unregister-connection! world-data item)
  {:success true})

(defn ensure-node-connection!
  [world-data node-vblock]
  (or (lookup/get-node-connection world-data node-vblock)
      (let [result (create-node-connection! world-data node-vblock)]
        (when (:success result)
          (:connection result)))))

(defn link-generator-to-connection!
  [world-data conn generator-vblock world]
  (when-let [old-conn (lookup/get-node-connection world-data generator-vblock)]
    (node-conn/remove-generator! old-conn generator-vblock))
  {:success (boolean (node-conn/add-generator! conn generator-vblock world))})

(defn unlink-generator-from-connection!
  [conn gen-vb]
  {:success (boolean (node-conn/remove-generator! conn gen-vb))})

(defn link-receiver-to-connection!
  [world-data conn receiver-vblock world]
  (when-let [old-conn (lookup/get-node-connection world-data receiver-vblock)]
    (node-conn/remove-receiver! old-conn receiver-vblock))
  {:success (boolean (node-conn/add-receiver! conn receiver-vblock world))})

(defn unlink-receiver-from-connection!
  [conn rec-vb]
  {:success (boolean (node-conn/remove-receiver! conn rec-vb))})

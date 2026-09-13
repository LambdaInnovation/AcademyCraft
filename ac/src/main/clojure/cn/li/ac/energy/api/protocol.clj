(ns cn.li.ac.energy.api.protocol
  "Energy system public contracts.

  Defines all energy management contracts without protocol dispatch.
  Uses Framework function map lookup for dispatch.

  Framework paths:
    [:service :energy :manager]  — IEnergyManager methods
    [:service :energy :network]  — IEnergyNetwork + IEnergyNode + IEnergyTransfer methods
    [:service :energy :storage]  — IEnergyStorage + IEnergyItem methods
    [:service :energy :admin]    — IEnergyValidator + IEnergyAdmin methods"
  (:require [cn.li.mcmod.framework :as fw]))

;; ============================================================================
;; Key set documentation
;; ============================================================================

(def manager-keys
  "Keys required by [:service :energy :manager] function maps.
  IEnergyManager: get-energy get-capacity set-energy transfer-energy
                   drain-energy subscribe-to-changes unsubscribe-from-changes
                   list-energy-providers"
  [:get-energy :get-capacity :set-energy :transfer-energy
   :drain-energy :subscribe-to-changes :unsubscribe-from-changes
   :list-energy-providers])

(def item-keys
  "Keys required by [:service :energy :storage] function maps — item operations.
  IEnergyItem: get-item-energy set-item-energy get-item-capacity get-item-bandwidth
                is-energy-item? charge-item discharge-item"
  [:get-item-energy :set-item-energy :get-item-capacity :get-item-bandwidth
   :is-energy-item? :charge-item :discharge-item])

(def node-keys
  "Keys required by [:service :energy :network] function maps — node operations.
  IEnergyNode: get-node-energy set-node-energy get-node-capacity inject-energy
                extract-node-energy"
  [:get-node-energy :set-node-energy :get-node-capacity :inject-energy
   :extract-node-energy])

(def network-keys
  "Keys required by [:service :energy :network] function maps — network operations.
  IEnergyNetwork: get-network-energy get-network-capacity set-network-energy
                   distribute-energy balance-network-energy"
  [:get-network-energy :get-network-capacity :set-network-energy
   :distribute-energy :balance-network-energy])

(def transfer-keys
  "Keys required by [:service :energy :network] function maps — transfer operations.
  IEnergyTransfer: transfer-wireless batch-transfer schedule-transfer
                    cancel-scheduled-transfer"
  [:transfer-wireless :batch-transfer :schedule-transfer
   :cancel-scheduled-transfer])

(def storage-keys
  "Keys required by [:service :energy :storage] function maps — persistence.
  IEnergyStorage: save-energy-to-nbt load-energy-from-nbt backup-energy-state
                   restore-energy-from-backup"
  [:save-energy-to-nbt :load-energy-from-nbt :backup-energy-state
   :restore-energy-from-backup])

(def validator-keys
  "Keys required by [:service :energy :admin] function maps — validation.
  IEnergyValidator: validate-energy-amount validate-transfer
                     validate-network-consistency repair-inconsistency"
  [:validate-energy-amount :validate-transfer
   :validate-network-consistency :repair-inconsistency])

(def admin-keys
  "Keys required by [:service :energy :admin] function maps — admin.
  IEnergyAdmin: admin-dump-state admin-set-energy-unsafe
                 admin-reset-energy-system admin-simulate-loss"
  [:admin-dump-state :admin-set-energy-unsafe
   :admin-reset-energy-system :admin-simulate-loss])

;; ============================================================================
;; Internal dispatch helpers
;; ============================================================================

(def ^:private mgr-path [:service :energy :manager])
(def ^:private net-path [:service :energy :network])
(def ^:private stor-path [:service :energy :storage])
(def ^:private admin-path [:service :energy :admin])

(defn- call-from
  "Look up a function at `path` with key `k` and apply `args`."
  [path k args]
  (if-let [m (get-in @(fw/fw-atom) path)]
    (if-let [f (get m k)]
      (apply f args)
      (throw (ex-info "Function not found in installed service"
                      {:path path :key k})))
    (throw (ex-info "Service not installed"
                    {:path path :key k}))))

;; ============================================================================
;; Install functions
;; ============================================================================

(defn install-energy-manager!
  "Install an IEnergyManager implementation at [:service :energy :manager].
  Impl-map must contain keys: get-energy, get-capacity, set-energy, transfer-energy,
  drain-energy, subscribe-to-changes, unsubscribe-from-changes, list-energy-providers."
  [impl-map]
  (when-let [a (fw/fw-atom)]
    (swap! a assoc-in mgr-path impl-map))
  nil)

(defn install-energy-network!
  "Install IEnergyNetwork + IEnergyNode + IEnergyTransfer implementation
  at [:service :energy :network]."
  [impl-map]
  (when-let [a (fw/fw-atom)]
    (swap! a assoc-in net-path impl-map))
  nil)

(defn install-energy-storage!
  "Install IEnergyStorage + IEnergyItem implementation
  at [:service :energy :storage]."
  [impl-map]
  (when-let [a (fw/fw-atom)]
    (swap! a assoc-in stor-path impl-map))
  nil)

(defn install-energy-admin!
  "Install IEnergyValidator + IEnergyAdmin implementation
  at [:service :energy :admin]."
  [impl-map]
  (when-let [a (fw/fw-atom)]
    (swap! a assoc-in admin-path impl-map))
  nil)

;; ============================================================================
;; IEnergyManager — Core Energy Management
;; ============================================================================

(defn get-energy
  "Get current energy amount for entity id.
  Returns double (amount) or nil if not found."
  [system id]
  (call-from mgr-path :get-energy [system id]))

(defn get-capacity
  "Get maximum energy capacity for entity id.
  Returns double (capacity) or nil if not found."
  [system id]
  (call-from mgr-path :get-capacity [system id]))

(defn set-energy
  "Set energy to exact amount (clamped).
  Returns {:success boolean :reason string}."
  [system id amount]
  (call-from mgr-path :set-energy [system id amount]))

(defn transfer-energy
  "Transfer energy between containers.
  Returns {:transferred amount :lost amount :reason string}."
  ([system source dest amount]
   (transfer-energy system source dest amount nil))
  ([system source dest amount callback]
   (call-from mgr-path :transfer-energy [system source dest amount callback])))

(defn drain-energy
  "Extract energy from container.
  Returns [success amount-extracted]."
  [system id amount]
  (call-from mgr-path :drain-energy [system id amount]))

(defn subscribe-to-changes
  "Subscribe to energy changes for entity id.
  callback: fn(old-amount new-amount) called on change.
  Returns subscription-id (for unsubscribe)."
  [system id callback]
  (call-from mgr-path :subscribe-to-changes [system id callback]))

(defn unsubscribe-from-changes
  "Unsubscribe from energy changes.
  Returns nil."
  [system subscription-id]
  (call-from mgr-path :unsubscribe-from-changes [system subscription-id]))

(defn list-energy-providers
  "List all energy-capable entities.
  Returns [id id id ...]."
  [system]
  (call-from mgr-path :list-energy-providers [system]))

;; ============================================================================
;; IEnergyItem — Item-Specific Energy
;; ============================================================================

(defn get-item-energy
  "Get energy in item-stack.
  Returns double."
  [system item-stack]
  (call-from stor-path :get-item-energy [system item-stack]))

(defn set-item-energy
  "Set energy in item-stack (modifies in place).
  Returns nil."
  [system item-stack amount]
  (call-from stor-path :set-item-energy [system item-stack amount]))

(defn get-item-capacity
  "Get item max capacity.
  Returns double."
  [system item-stack]
  (call-from stor-path :get-item-capacity [system item-stack]))

(defn get-item-bandwidth
  "Get max transfer rate.
  Returns double."
  [system item-stack]
  (call-from stor-path :get-item-bandwidth [system item-stack]))

(defn is-energy-item?
  "Check if item stores energy.
  Returns boolean."
  [system item-stack]
  (call-from stor-path :is-energy-item? [system item-stack]))

(defn charge-item
  "Charge item energy.
  Returns amount-charged."
  [system item-stack amount]
  (call-from stor-path :charge-item [system item-stack amount]))

(defn discharge-item
  "Discharge item energy.
  Returns amount-discharged."
  [system item-stack amount]
  (call-from stor-path :discharge-item [system item-stack amount]))

;; ============================================================================
;; IEnergyNode — Node Energy (Wireless Nodes)
;; ============================================================================

(defn get-node-energy
  "Get node energy.
  Returns double."
  [system node-vblock]
  (call-from net-path :get-node-energy [system node-vblock]))

(defn set-node-energy
  "Set node energy.
  Returns nil."
  [system node-vblock amount]
  (call-from net-path :set-node-energy [system node-vblock amount]))

(defn get-node-capacity
  "Get node max capacity.
  Returns double."
  [system node-vblock]
  (call-from net-path :get-node-capacity [system node-vblock]))

(defn inject-energy
  "Add energy to node.
  Returns amount-added."
  [system node-vblock amount]
  (call-from net-path :inject-energy [system node-vblock amount]))

(defn extract-node-energy
  "Remove energy from node.
  Returns amount-extracted."
  [system node-vblock amount]
  (call-from net-path :extract-node-energy [system node-vblock amount]))

;; ============================================================================
;; IEnergyNetwork — Network Energy Coordination
;; ============================================================================

(defn get-network-energy
  "Get total energy in network.
  Returns double."
  [system network-id]
  (call-from net-path :get-network-energy [system network-id]))

(defn get-network-capacity
  "Get total capacity.
  Returns double."
  [system network-id]
  (call-from net-path :get-network-capacity [system network-id]))

(defn set-network-energy
  "Set network energy to amount (distributed equally).
  Returns nil."
  [system network-id amount]
  (call-from net-path :set-network-energy [system network-id amount]))

(defn distribute-energy
  "Distribute energy using strategy.
  Strategies: :equal, :proportional, :load-balanced, :priority
  Returns {:distributed amount :strategy strategy}."
  ([system network-id strategy]
   (distribute-energy system network-id strategy nil))
  ([system network-id strategy amount]
   (call-from net-path :distribute-energy [system network-id strategy amount])))

(defn balance-network-energy
  "Auto-balance energy across all nodes.
  Returns {:moved amount :reason string}."
  [system network-id]
  (call-from net-path :balance-network-energy [system network-id]))

;; ============================================================================
;; IEnergyTransfer — High-Level Transfers
;; ============================================================================

(defn transfer-wireless
  "Transfer via wireless connection.
  Returns {:transferred :lost :range :reason}."
  [system source-node dest-node amount]
  (call-from net-path :transfer-wireless [system source-node dest-node amount]))

(defn batch-transfer
  "Execute multiple transfers atomically.
  transfers: [{:source :dest :amount} ...]
  Returns {:total-transferred :failed []}."
  [system transfers]
  (call-from net-path :batch-transfer [system transfers]))

(defn schedule-transfer
  "Schedule recurring transfer.
  interval: Transfer every N ticks.
  Returns schedule-id."
  [system source dest amount interval]
  (call-from net-path :schedule-transfer [system source dest amount interval]))

(defn cancel-scheduled-transfer
  "Cancel scheduled transfer.
  Returns nil."
  [system schedule-id]
  (call-from net-path :cancel-scheduled-transfer [system schedule-id]))

;; ============================================================================
;; IEnergyStorage — Persistent Storage
;; ============================================================================

(defn save-energy-to-nbt
  "Save energy data to NBT.
  Returns updated-nbt."
  [system id nbt-tag]
  (call-from stor-path :save-energy-to-nbt [system id nbt-tag]))

(defn load-energy-from-nbt
  "Load energy data from NBT.
  Returns {:energy amount :capacity capacity}."
  [system nbt-tag]
  (call-from stor-path :load-energy-from-nbt [system nbt-tag]))

(defn backup-energy-state
  "Create backup of all energy.
  Returns backup-id."
  [system timestamp]
  (call-from stor-path :backup-energy-state [system timestamp]))

(defn restore-energy-from-backup
  "Restore from backup.
  Returns {:success boolean :reason string}."
  [system backup-id]
  (call-from stor-path :restore-energy-from-backup [system backup-id]))

;; ============================================================================
;; IEnergyValidator — Consistency & Validation
;; ============================================================================

(defn validate-energy-amount
  "Check if amount is valid.
  Returns {:valid boolean :errors []}."
  [system amount]
  (call-from admin-path :validate-energy-amount [system amount]))

(defn validate-transfer
  "Validate transfer operation.
  Returns {:valid boolean :reason string}."
  [system source dest amount]
  (call-from admin-path :validate-transfer [system source dest amount]))

(defn validate-network-consistency
  "Validate network energy consistency.
  Returns {:consistent boolean :errors []}."
  [system network-id]
  (call-from admin-path :validate-network-consistency [system network-id]))

(defn repair-inconsistency
  "Attempt to repair inconsistency.
  Returns {:repaired boolean :changes}."
  [system network-id]
  (call-from admin-path :repair-inconsistency [system network-id]))

;; ============================================================================
;; IEnergyAdmin — Debugging & Admin
;; ============================================================================

(defn admin-dump-state
  "Dump all energy state.
  Returns state-map."
  [system]
  (call-from admin-path :admin-dump-state [system]))

(defn admin-set-energy-unsafe
  "Forcefully set energy (with logging).
  reason: Why this was forced.
  Returns {:success boolean}."
  [system id amount reason]
  (call-from admin-path :admin-set-energy-unsafe [system id amount reason]))

(defn admin-reset-energy-system
  "Reset all energy to defaults.
  Returns nil."
  [system]
  (call-from admin-path :admin-reset-energy-system [system]))

(defn admin-simulate-loss
  "Simulate energy loss (for testing).
  Returns {:lost amount}."
  [system network-id amount-lost reason]
  (call-from admin-path :admin-simulate-loss [system network-id amount-lost reason]))

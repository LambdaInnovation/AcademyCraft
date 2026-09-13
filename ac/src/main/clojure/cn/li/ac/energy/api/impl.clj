(ns cn.li.ac.energy.api.impl
  "Default Phase C energy API implementation.

  This namespace bridges the API layer to the concrete item/node service
  implementations. Runtime stores require an explicit server/world owner.

  Implements the contracts defined in cn.li.ac.energy.api.protocol as
  plain function maps installed into Framework."
  (:require [cn.li.ac.energy.api.protocol :as proto]
            [cn.li.ac.energy.service.provider-registry :as provider-registry]
            [cn.li.ac.energy.service.subscription :as subscription]
            [cn.li.ac.energy.service.transfer-executor :as transfer-executor]
            [cn.li.ac.energy.service.item-manager :as item-manager]
            [cn.li.ac.energy.service.node-manager :as node-manager]
            [cn.li.ac.energy.domain.container :as container]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.util.log :as log]))

(def ^:private WORLD-PROVIDER-OWNER :world)

(defn- require-owner-value
  [owner label value]
  (when-not value
    (throw (ex-info (str "Energy owner requires " label)
                    {:owner owner})))
  value)

(defn energy-owner-key
  "Return the owner key for an energy system instance."
  [owner]
  (let [server-session-id (or (:server-session-id owner) (:session-id owner))
        world-id (or (:world-id owner) (:dimension-id owner))]
    [(require-owner-value owner ":server-session-id" server-session-id)
     (require-owner-value owner ":world-id" world-id)
     (or (:provider-owner owner) (:owner-id owner) WORLD-PROVIDER-OWNER)]))

;; ============================================================================
;; System state factory — per-owner state map
;; ============================================================================

(defn- new-energy-system
  "Create a fresh per-owner energy system state map."
  []
  {:providers (atom {})
   :subscriptions (atom {})
   :scheduled-transfers (atom {})})

;; ============================================================================
;; Implementation function maps — installed into Framework via proto/install-*
;; ============================================================================

(defn- create-manager-impl
  "Create the IEnergyManager implementation function map.
  Methods access state via (:providers system), (:subscriptions system), etc."
  []
  {:get-energy (fn [system id]
                 (some-> (provider-registry/resolve-provider (:providers system) id)
                         provider-registry/provider-energy))

   :get-capacity (fn [system id]
                   (some-> (provider-registry/resolve-provider (:providers system) id)
                           provider-registry/provider-capacity))

   :set-energy (fn [system id amount]
                 (if-let [{:keys [kind value] :as provider}
                          (provider-registry/resolve-provider (:providers system) id)]
                   (let [old-value (provider-registry/provider-energy provider)]
                     (case kind
                       :item (do
                               (item-manager/set-item-energy! value amount)
                               (subscription/notify! (:subscriptions system) id
                                                    old-value (item-manager/get-item-energy value))
                               {:success true :reason "ok"})
                       :node (do
                               (node-manager/set-node-energy! value amount)
                               (subscription/notify! (:subscriptions system) id
                                                    old-value (node-manager/get-node-energy value))
                               {:success true :reason "ok"})
                       :container (if-let [[before after]
                                          (provider-registry/update-container-ref!
                                            provider container/set-energy amount)]
                                    (do
                                      (subscription/notify! (:subscriptions system) id
                                                           (container/get-current before)
                                                           (container/get-current after))
                                      {:success true :reason "ok"})
                                    {:success false
                                     :reason "container is immutable unless registered with an atom ref"})
                       {:success false :reason "unsupported provider"}))
                   {:success false :reason "provider not found"}))

   :transfer-energy (fn [system source dest amount callback]
                      (transfer-executor/transfer!
                        (provider-registry/resolve-provider (:providers system) source)
                        (provider-registry/resolve-provider (:providers system) dest)
                        amount
                        callback))

   :drain-energy (fn [system id amount]
                   (let [provider (provider-registry/resolve-provider (:providers system) id)
                         old-value (some-> provider provider-registry/provider-energy)
                         [success extracted] (transfer-executor/drain! provider amount)
                         new-value (some-> provider provider-registry/provider-energy)]
                     (when provider
                       (subscription/notify! (:subscriptions system) id old-value new-value))
                     [success extracted]))

   :subscribe-to-changes (fn [system id callback]
                           (subscription/subscribe! (:subscriptions system) id callback))

   :unsubscribe-from-changes (fn [system sid]
                               (subscription/unsubscribe! (:subscriptions system) sid))

   :list-energy-providers (fn [system]
                            (provider-registry/provider-ids (:providers system)))})

(defn- create-item-impl
  "Create the IEnergyItem implementation function map."
  []
  {:get-item-energy (fn [_system item-stack]
                      (item-manager/get-item-energy item-stack))

   :set-item-energy (fn [_system item-stack amount]
                      (item-manager/set-item-energy! item-stack amount)
                      nil)

   :get-item-capacity (fn [_system item-stack]
                        (item-manager/get-item-capacity item-stack))

   :get-item-bandwidth (fn [_system item-stack]
                         (item-manager/get-item-bandwidth item-stack))

   :is-energy-item? (fn [_system item-stack]
                      (item-manager/is-energy-item-supported? item-stack))

   :charge-item (fn [_system item-stack amount]
                  (let [leftover (item-manager/charge-energy-to-item item-stack amount false)]
                    (- (double amount) (double leftover))))

   :discharge-item (fn [_system item-stack amount]
                     (item-manager/pull-energy-from-item item-stack amount false))})

(defn- create-node-impl
  "Create the IEnergyNode implementation function map."
  []
  {:get-node-energy (fn [_system node-vblock]
                      (node-manager/get-node-energy node-vblock))

   :set-node-energy (fn [_system node-vblock amount]
                      (node-manager/set-node-energy! node-vblock amount)
                      nil)

   :get-node-capacity (fn [_system node-vblock]
                        (node-manager/get-node-capacity node-vblock))

   :inject-energy (fn [_system node-vblock amount]
                    (let [leftover (node-manager/charge-node node-vblock amount false)]
                      (- (double amount) (double leftover))))

   :extract-node-energy (fn [_system node-vblock amount]
                          (node-manager/pull-from-node node-vblock amount false))})

(defn- create-admin-impl
  "Create the combined IEnergyValidator + IEnergyAdmin implementation map."
  []
  {:validate-energy-amount (fn [_system amount]
                             {:valid (and (number? amount)
                                         (Double/isFinite (double amount))
                                         (>= (double amount) 0.0))
                              :errors (cond-> []
                                        (not (number? amount))
                                        (conj "amount must be numeric")
                                        (and (number? amount)
                                             (not (Double/isFinite (double amount))))
                                        (conj "amount must be finite")
                                        (and (number? amount) (< (double amount) 0.0))
                                        (conj "amount must be non-negative"))})

   :validate-transfer (fn [system source dest amount]
                        (let [amount-validation (proto/validate-energy-amount system amount)]
                          {:valid (and (:valid amount-validation)
                                       (some? (provider-registry/resolve-provider
                                                (:providers system) source))
                                       (some? (provider-registry/resolve-provider
                                                (:providers system) dest)))
                           :reason (cond
                                     (not (:valid amount-validation))
                                     (first (:errors amount-validation))
                                     (nil? (provider-registry/resolve-provider
                                             (:providers system) source))
                                     "source not found"
                                     (nil? (provider-registry/resolve-provider
                                             (:providers system) dest))
                                     "destination not found"
                                     :else "ok")}))

   :validate-network-consistency (fn [_system _network-id]
                                   {:consistent true :errors []})

   :repair-inconsistency (fn [_system _network-id]
                           {:repaired false :changes []})

   :admin-dump-state (fn [system]
                       (provider-registry/dump-state (:providers system)))

   :admin-set-energy-unsafe (fn [system id amount reason]
                              (log/warn (str "Unsafe energy set for " id ": "
                                             amount " because " reason))
                              (proto/set-energy system id amount))

   :admin-reset-energy-system (fn [system]
                                (provider-registry/clear! (:providers system))
                                (subscription/clear! (:subscriptions system))
                                (reset! (:scheduled-transfers system) {})
                                nil)

   :admin-simulate-loss (fn [_system _network-id amount-lost reason]
                          (log/warn (str "Simulated energy loss: " amount-lost
                                         " because " reason))
                          {:lost amount-lost})})

;; ============================================================================
;; Bootstrap — install all default implementations
;; ============================================================================

(defn install-default-impls!
  "Install all default energy implementation function maps into Framework.
  Should be called during system initialization."
  []
  (proto/install-energy-manager! (create-manager-impl))
  (proto/install-energy-storage! (create-item-impl))
  (proto/install-energy-network! (create-node-impl))
  (proto/install-energy-admin! (create-admin-impl))
  nil)

;; ============================================================================
;; Energy system runtime — Framework [:service :energy-system]
;; ============================================================================

(def ^:private es-path [:service :energy-system])

(defn- systems-atom []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom es-path)
        (let [a (atom {})]
          (swap! fw-atom assoc-in es-path a)
          a))
    (atom {})))

(defn- systems-snapshot []
  @(systems-atom))

(defn energy-system
  "Return the default energy system state map for the given owner."
  [owner]
  (let [key (energy-owner-key owner)
        systems* (systems-atom)
        created* (atom nil)]
    (or (get (systems-snapshot) key)
        (do
          (swap! systems*
                 (fn [systems]
                   (if-let [existing (get systems key)]
                     (do
                       (reset! created* existing)
                       systems)
                     (let [created (new-energy-system)]
                       (reset! created* created)
                       (assoc systems key created)))))
          @created*))))

(defn energy-systems-snapshot
  []
  (systems-snapshot))

(defn reset-energy-systems-for-test!
  []
  (reset! (systems-atom) {})
  nil)

;; ============================================================================
;; Runtime helpers for test isolation
;; ============================================================================

(defn create-energy-system-runtime
  "Create an isolated energy system runtime for testing.
  Returns a fresh atom that can be used with call-with-energy-system-runtime."
  []
  (atom {}))

(defn call-with-energy-system-runtime
  "Temporarily replace the systems atom at [:service :energy-system]
  with `runtime` for the duration of `f`."
  [runtime f]
  (let [fw-atom (fw/fw-atom)]
    (if fw-atom
      (let [prev (get-in @fw-atom es-path)]
        (try
          (swap! fw-atom assoc-in es-path runtime)
          (f)
          (finally
            (swap! fw-atom assoc-in es-path prev))))
      (f))))

;; ============================================================================
;; Public API — registration
;; ============================================================================

(defn register-provider!
  "Register a provider under a stable id.

  The value may be:
  - an item stack
  - a wireless node tile entity
  - an EnergyContainer
  - an atom holding an EnergyContainer"
  [owner id value]
  (provider-registry/register-provider! (:providers (energy-system owner)) id value))

(defn unregister-provider!
  "Unregister a previously registered provider id."
  [owner id]
  (provider-registry/unregister-provider! (:providers (energy-system owner)) id))

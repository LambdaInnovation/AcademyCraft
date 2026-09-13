(ns cn.li.ac.energy.service.transfer-executor
  "Concrete transfer/drain execution for resolved energy providers."
  (:require [cn.li.ac.energy.service.item-manager :as item-manager]
            [cn.li.ac.energy.service.node-manager :as node-manager]))

(defn extract-from-provider
  "Extract up to `amount` from a provider.

  Container providers are immutable snapshots, so direct extraction is unsupported."
  [{:keys [kind value]} amount]
  (case kind
    :item (item-manager/pull-energy-from-item value amount false)
    :node (node-manager/pull-from-node value amount false)
    :container 0.0
    0.0))

(defn refund-to-provider!
  "Return leftover energy to the source provider when a destination cannot accept it."
  [{:keys [kind value]} leftover]
  (when (pos? (double leftover))
    (case kind
      :item (item-manager/charge-energy-to-item value leftover false)
      :node (node-manager/charge-node value leftover false)
      nil))
  nil)

(defn insert-into-provider!
  "Insert energy into a destination provider and return leftover energy."
  [{:keys [kind value]} amount]
  (case kind
    :item (item-manager/charge-energy-to-item value amount false)
    :node (node-manager/charge-node value amount false)
    :container amount
    amount))

(defn transfer!
  "Execute a transfer between two already-resolved providers."
  [source-provider dest-provider amount callback]
  (cond
    (not (and source-provider dest-provider))
    {:transferred 0.0 :lost 0.0 :reason "provider not found"}

    (not (pos? (double amount)))
    {:transferred 0.0 :lost 0.0 :reason "amount must be positive"}

    :else
    (let [extracted (extract-from-provider source-provider amount)
          leftover (insert-into-provider! dest-provider extracted)
          transferred (- extracted leftover)
          result {:transferred transferred :lost 0.0 :reason "ok"}]
      (when (and (pos? (double leftover)) (pos? (double extracted)))
        (refund-to-provider! source-provider leftover))
      (when callback
        (callback result))
      result)))

(defn drain!
  "Drain energy from a resolved provider. Returns `[success extracted]`."
  [provider amount]
  (if provider
    (let [extracted (extract-from-provider provider amount)]
      [(pos? (double extracted)) extracted])
    [false 0.0]))

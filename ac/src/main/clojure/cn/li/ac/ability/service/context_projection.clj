(ns cn.li.ac.ability.service.context-projection
  "Read-only context projections from authoritative runtime-store player state.

  Transport metadata (listeners, message-buffer, route wiring) lives in
  context-dispatcher; business fields (status, input-state, skill-state,
  keepalive) are read from [:context-registry] in runtime-store only."
  (:require [cn.li.ac.ability.service.context-domain :as context-domain]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.mcmod.runtime.owner :as owner]))

(defn- store-context
  [session-id player-uuid ctx-id]
  (when (and session-id player-uuid ctx-id)
    (get-in (store/get-player-state session-id player-uuid)
            [:context-registry ctx-id])))

(defn merge-store-projection
  "Overlay authoritative store fields onto a transport context map."
  [transport-ctx]
  (if-not (map? transport-ctx)
    transport-ctx
    (let [session-id (owner/transport-store-session-id transport-ctx)
          player-uuid (:player-uuid transport-ctx)
          ctx-id (:id transport-ctx)
          projected (store-context session-id player-uuid ctx-id)]
      (if-not projected
        transport-ctx
        (merge transport-ctx
               (select-keys projected [:status :input-state :skill-state
                                       :last-keepalive-ms :terminated-reason
                                       :skill-id]))))))

(defn get-store-context
  [session-id player-uuid ctx-id]
  (store-context session-id player-uuid ctx-id))

(defn context-owned-by?
  [session-id player-uuid ctx-id]
  (when-let [ctx (store-context session-id player-uuid ctx-id)]
    (= player-uuid (:player-uuid ctx))))

(defn active-store-context?
  [ctx]
  (context-domain/active-context? ctx))

(defn snapshot-store-contexts
  [session-id player-uuid]
  (->> (or (get-in (store/get-player-state session-id player-uuid)
                   [:context-registry])
           {})
       vals
       (filter map?)
       vec))

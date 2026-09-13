(ns cn.li.mcbase.client.session
  "Client-local owner/session helpers for client-owned runtime state."
  (:require [cn.li.platform.neutral.hooks :as runtime-hooks])
  (:import [net.minecraft.client Minecraft]
           [net.minecraft.client.multiplayer ClientPacketListener]))

(defn current-connection
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (.getConnection mc)))

(defn client-session-id
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [^ClientPacketListener connection (.getConnection mc)]
      [:client-session
       (System/identityHashCode mc)
       (System/identityHashCode connection)])))

(defn local-player-uuid
  []
  (when-let [^Minecraft mc (Minecraft/getInstance)]
    (when-let [player (.player mc)]
      (str (.getUUID player)))))

(defn owner-for-player-uuid
  [player-uuid]
  (when-let [session-id (client-session-id)]
    (when player-uuid
      {:logical-side :client
       :client-session-id session-id
       :player-uuid (str player-uuid)})))

(defn current-local-player-owner
  []
  (owner-for-player-uuid (local-player-uuid)))

(defn connection-key
  []
  (when-let [session-id (client-session-id)]
    (if-let [player-uuid (local-player-uuid)]
      [:connection session-id player-uuid]
      [:connection session-id])))

(defn payload-player-uuid
  [payload]
  (some-> (or (:uuid payload)
              (:player-uuid payload)
              (get-in payload [:payload :uuid])
              (get-in payload [:payload :player-uuid]))
          str))

(defn with-current-client-session
  "Set client session context in Framework [:service :client-ctx] for duration of f.
  Uses Framework atom instead of ThreadLocal binding — compatible with
  client-session-id defn migration to Framework-based reader."
  [f]
  (runtime-hooks/with-client-ctx-fn {:session-id (client-session-id)} f))

(defn with-bound-client-owner
  "Set client session + player owner in Framework [:service :client-ctx] for duration of f.
  Uses Framework atom instead of ThreadLocal binding — compatible with
  client-session-id / player-state-owner defn migration."
  [owner f]
  (let [owner* (or owner {})
        session-id (:client-session-id owner*)
        player-uuid (some-> (:player-uuid owner*) str)]
    (when-not session-id
      (throw (ex-info "Client owner requires :client-session-id"
                      {:owner owner})))
    (runtime-hooks/with-client-ctx-fn
      {:session-id session-id
       :player-owner (cond-> {:logical-side :client
                              :client-session-id session-id}
                       player-uuid (assoc :player-uuid player-uuid))}
      f)))

(defn with-current-client-owner
  ([f]
   (if-let [owner (current-local-player-owner)]
     (with-bound-client-owner owner f)
     (with-current-client-session f)))
  ([player-uuid f]
   (if-let [owner (owner-for-player-uuid player-uuid)]
     (with-bound-client-owner owner f)
     (with-current-client-session f))))

(defn owner-key
  [owner]
  (let [session-id (:client-session-id owner)
        player-uuid (:player-uuid owner)]
    (when-not session-id
      (throw (ex-info "Client owner requires :client-session-id" {:owner owner})))
    (when-not player-uuid
      (throw (ex-info "Client owner requires :player-uuid" {:owner owner})))
    [session-id (str player-uuid)]))

(defn require-local-player-owner
  []
  (or (current-local-player-owner)
      (throw (ex-info "Client local player owner unavailable"
                      {:client-session-id (client-session-id)
                       :player-uuid (local-player-uuid)}))))

(defn init-default-owner-resolver!
  "Register the default client owner hook so platform-agnostic modules (ac)
  can resolve a client owner without importing Minecraft classes. Called
  from platform client bootstrap (forge/fabric client init), not at
  namespace load — reset! is naturally idempotent so no exactly-once guard
  is needed here."
  []
  (runtime-hooks/set-default-client-owner-fn! current-local-player-owner)
  nil)

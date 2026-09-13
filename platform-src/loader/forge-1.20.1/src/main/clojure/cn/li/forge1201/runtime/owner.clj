(ns cn.li.forge1201.runtime.owner
  "Canonical runtime owner helpers for Forge event boundaries."
  (:require [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as runtime-owner])
  (:import [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world.entity.player Player]))

(def ^:private client-session-id-fn
  (atom (fn []
          (throw (ex-info "Forge client session id function is not installed"
                          {:namespace 'cn.li.forge1201.runtime.owner})))))

(def ^:private with-bound-client-owner-fn
  (atom (fn [_ _]
          (throw (ex-info "Forge client owner binding function is not installed"
                          {:namespace 'cn.li.forge1201.runtime.owner})))))

(defn install-client-owner-functions!
  [{:keys [client-session-id with-bound-client-owner]}]
  (reset! client-session-id-fn client-session-id)
  (reset! with-bound-client-owner-fn with-bound-client-owner)
  nil)

(defn player-uuid
  [^Player player]
  (some-> player .getUUID str))

(defn server-session-id
  [^ServerPlayer player]
  (when-let [server (.getServer player)]
    [:server (System/identityHashCode server)]))

(defn server-owner
  [^ServerPlayer player]
  (let [session-id (server-session-id player)
        uuid (player-uuid player)]
    (when-not session-id
      (throw (ex-info "Forge server player owner requires server session"
                      {:player player
                       :player-uuid uuid})))
    (when-not uuid
      (throw (ex-info "Forge server player owner requires player UUID"
                      {:player player
                       :server-session-id session-id})))
    (runtime-owner/require-server-owner
     {:logical-side :server
      :server-session-id session-id
      :player-uuid uuid})))

(defn client-owner
  [^Player player]
  (let [session-id (@client-session-id-fn)
        uuid (player-uuid player)]
    (when-not session-id
      (throw (ex-info "Forge client player owner requires client session"
                      {:player player
                       :player-uuid uuid})))
    (when-not uuid
      (throw (ex-info "Forge client player owner requires player UUID"
                      {:player player
                       :client-session-id session-id})))
    (runtime-owner/require-client-owner
     {:logical-side :client
      :client-session-id session-id
      :player-uuid uuid})))

(defn owner-for-player
  [^Player player side]
  (case side
    :server (server-owner ^ServerPlayer player)
    :client (client-owner player)
    (throw (ex-info "Forge player owner requires logical side"
                    {:player player
                     :side side}))))

(defn with-player-owner
  [^Player player side f]
  (let [owner (owner-for-player player side)]
    (if (= :client (:logical-side owner))
      (@with-bound-client-owner-fn owner f)
      ;; 完美契合：直接把 f 作为一个完整的函数对象传递进去
      (runtime-hooks/with-player-state-owner-fn owner f))))

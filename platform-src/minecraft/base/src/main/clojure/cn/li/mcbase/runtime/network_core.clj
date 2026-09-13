(ns cn.li.mcbase.runtime.network-core
  "Loader-agnostic runtime network registration helpers.

  Platforms provide transport functions; shared core wires runtime route/send
  integration with mcmod power-runtime and runtime message registry."
  (:require [cn.li.mcbase.runtime.entity-query-core :as query-core]
            [cn.li.mcbase.runtime.spi.network-transport :as transport-spi]
            [cn.li.mcbase.runtime.spi.server-context :as server-context-spi]
            [cn.li.platform.neutral.hooks :as network-hooks]
            [cn.li.platform.neutral.integration-runtime :as messages]
            [cn.li.platform.neutral.network-runtime :as content-registry]
            [cn.li.platform.neutral.client-network :as net-client]
            [cn.li.mcmod.util.log :as log])
  (:import [net.minecraft.server MinecraftServer]
           [net.minecraft.server.players PlayerList]
           [net.minecraft.server.level ServerPlayer]))

(def ^:private default-except-local-radius 64.0)

(defn- msg-id [message-key]
  (messages/msg-id message-key))

(defn- target-player-not-found
  [uuid extra]
  (ex-info (str "Runtime network target player not found: " uuid)
           (merge {:reason :target-player-not-found
                   :target-player-uuid uuid}
                  extra)))

(defn- require-target-player!
  [find-player-by-uuid uuid extra]
  (or (find-player-by-uuid uuid)
      (throw (target-player-not-found uuid extra))))

(defn- normalize-direct-send-result
  [result]
  (cond
    (map? result) (if (contains? result :sent)
                    result
                    (assoc result :sent 1))
    (nil? result) {:sent 1}
    :else {:sent 1
           :transport-result result}))

(declare init-runtime-network!)

(defn- sync-message-descriptors
  []
  (->> (content-registry/list-descriptors :sync)
       (sort-by (fn [x] [(long (or (:order x) 0)) (str (:id x))]))))

(defn- sync-message-payload
  "Build one domain's sync message, or nil to skip it entirely.

  `runtime-payload` only contains keys for domains build-sync-payload decided
  to include (all of them for a full sync, only the dirty ones for a delta
  sync) -- contains? (not just truthy get) is what makes the skip work, since
  an included domain's value can itself legitimately be nil."
  [uuid runtime-payload {:keys [message-id message-key payload-key]}]
  (when (and (or message-id message-key) payload-key
             (contains? runtime-payload payload-key))
    {:msg-id (or message-id (msg-id message-key))
     :payload {:uuid uuid
               payload-key (get runtime-payload payload-key)}}))

(defn sync-message-payloads
  [uuid payload]
  (keep #(sync-message-payload uuid payload %) (sync-message-descriptors)))

(defn create-targeted-client-sender
  "Create a player-targeted send fn from player lookup + push transport functions."
  [find-player-by-uuid push-to-client!]
  (fn [uuid msg-id payload]
    (let [player (require-target-player! find-player-by-uuid
                                         uuid
                                         {:operation :targeted-client-send
                                          :msg-id msg-id})]
      (push-to-client! player msg-id payload)
      {:sent 1
       :msg-id msg-id
       :target-player-uuid uuid})))

(defn create-sync-sender
  "Create the single-packet runtime v2 sync sender."
  [find-player-by-uuid push-to-client!]
  (fn [uuid payload]
    (let [player (require-target-player! find-player-by-uuid
                                         uuid
                                         {:operation :runtime-sync-v2-send})
          message-id (msg-id :sync-v2)]
      (push-to-client! player message-id payload)
      {:sent 1
       :msg-id message-id
       :target-player-uuid uuid})))

(defn default-send-to-server!
  [msg-id payload]
  (net-client/send-to-server msg-id payload))

(defn default-find-player-by-uuid
  [uuid-str]
  (query-core/get-player-by-uuid (server-context-spi/require-current-server) uuid-str))

(defn- same-dimension?
  "ResourceKey<Level> is an interned singleton per dimension (same instance for
  every player in the same dimension) -- identical? avoids building two
  dimension-id-string conversions per candidate player on this
  every-player-in-the-list scan."
  [^ServerPlayer a-player ^ServerPlayer b-player]
  (identical? (some-> a-player .level .dimension)
              (some-> b-player .level .dimension)))

(defn- within-radius?
  [^ServerPlayer source-player ^ServerPlayer target-player radius]
  (let [dx (- (.getX target-player) (.getX source-player))
        dy (- (.getY target-player) (.getY source-player))
        dz (- (.getZ target-player) (.getZ source-player))
        distance-sqr (+ (* dx dx) (* dy dy) (* dz dz))]
    (<= distance-sqr (* radius radius))))

(defn default-find-nearby-player-uuids
  [source-player-uuid radius]
  (try
    (let [^MinecraftServer server (server-context-spi/require-current-server)
          ^ServerPlayer source-player (query-core/get-player-by-uuid server source-player-uuid)]
      (if (nil? source-player)
        []
        (let [^PlayerList player-list (.getPlayerList server)]
          (->> (.getPlayers player-list)
               (filter (fn [^ServerPlayer target-player]
                       (and (not= (str (.getUUID target-player)) source-player-uuid)
                            (same-dimension? source-player target-player)
                            (within-radius? source-player target-player radius))))
               (map (fn [^ServerPlayer target-player] (str (.getUUID target-player))))
               (into [])))))
    (catch Exception e
      (log/warn "Failed to resolve nearby players for except-local route:" source-player-uuid (ex-message e))
      [])))

(defn create-except-local-context-sender
  "Create a context channel sender that broadcasts to nearby players except source player."
  [find-nearby-player-uuids send-to-client-fn]
  (fn [ctx-id channel payload ctx-map]
    (if-let [source-player-uuid (or (:player-uuid ctx-map)
                                    (network-hooks/get-context-player-uuid ctx-id))]
      (let [target-player-uuids (vec (remove #{source-player-uuid}
                                             (find-nearby-player-uuids source-player-uuid default-except-local-radius)))]
        (if (empty? target-player-uuids)
          {:sent 0
           :reason :no-nearby-targets
           :ctx-id ctx-id
           :channel channel
           :source-player-uuid source-player-uuid}
          (let [result
                (reduce (fn [acc target-player-uuid]
                          (let [send-result (try
                                              (normalize-direct-send-result
                                                (send-to-client-fn target-player-uuid
                                                                   (msg-id :ctx-channel)
                                                                   {:ctx-id ctx-id :channel channel :payload payload}))
                                              (catch clojure.lang.ExceptionInfo e
                                                (if (= :target-player-not-found (:reason (ex-data e)))
                                                  (do
                                                    (log/warn "Skip except-local send due to missing target player:" target-player-uuid)
                                                    {:sent 0
                                                     :missing-target-player-uuid target-player-uuid})
                                                  (throw e))))]
                            (cond-> (update acc :sent + (long (or (:sent send-result) 0)))
                              (pos? (long (or (:sent send-result) 0)))
                              (update :target-player-uuids conj target-player-uuid)

                              (:missing-target-player-uuid send-result)
                              (update :missing-target-player-uuids (fnil conj []) (:missing-target-player-uuid send-result)))))
                        {:sent 0
                         :ctx-id ctx-id
                         :channel channel
                         :source-player-uuid source-player-uuid
                         :target-player-uuids []}
                        target-player-uuids)]
            (cond
              (pos? (:sent result)) result
              (seq (:missing-target-player-uuids result))
              (assoc result :reason :target-players-unavailable)
              :else result))))
      (do
        (log/debug "Skip except-local send due to missing context owner:" ctx-id)
        {:sent 0
         :reason :missing-context-player-uuid
         :ctx-id ctx-id
         :channel channel}))))

(def send-sync-to-client!
  (create-sync-sender transport-spi/find-player-by-uuid transport-spi/send-push-to-client!))

(def send-to-client!
  (create-targeted-client-sender transport-spi/find-player-by-uuid transport-spi/send-push-to-client!))

(defn install-runtime-network-transport!
  [{:keys [label install-server-context! send-to-server! send-push-to-client! find-player-by-uuid find-nearby-player-uuids]
    :or {label "runtime network"
         install-server-context! server-context-spi/install-server-context!
         send-to-server! default-send-to-server!
         find-player-by-uuid default-find-player-by-uuid
         find-nearby-player-uuids default-find-nearby-player-uuids}}]
  (install-server-context!)
  (transport-spi/register-transport-impl! {:send-to-server! send-to-server!
                                           :send-push-to-client! send-push-to-client!
                                           :find-player-by-uuid find-player-by-uuid
                                           :find-nearby-player-uuids find-nearby-player-uuids})
  (let [send-to-except-local-fn
        (create-except-local-context-sender find-nearby-player-uuids send-to-client!)]
    (init-runtime-network! {:send-to-server-fn send-to-server!
                            :send-to-client-fn send-to-client!
                            :send-to-except-local-fn send-to-except-local-fn}))
  (log/debug label "runtime network initialized"))

(defn init-runtime-network!
  [{:keys [send-to-server-fn send-to-client-fn send-to-except-local-fn]}]
  (let [send-to-except-local-fn (or send-to-except-local-fn (fn [_ctx-id _channel _payload _ctx-map] nil))
        send-context-channel-to-server!
        (fn [ctx-id channel payload _ctx-map]
          (send-to-server-fn (msg-id :ctx-channel)
                             {:ctx-id ctx-id :channel channel :payload payload})
          {:sent 1
           :ctx-id ctx-id
           :channel channel
           :msg-id (msg-id :ctx-channel)})
        send-context-channel-to-client!
        (fn [ctx-id channel payload ctx-map]
          (if-let [player-uuid (or (:player-uuid ctx-map)
                                   (network-hooks/get-context-player-uuid ctx-id))]
            (merge {:ctx-id ctx-id
                    :channel channel}
                   (normalize-direct-send-result
                     (send-to-client-fn player-uuid
                                        (msg-id :ctx-channel)
                                        {:ctx-id ctx-id :channel channel :payload payload})))
            (do
              (log/debug "Skip context send to client due to missing context owner:" ctx-id channel)
              {:sent 0
               :reason :missing-context-player-uuid
               :ctx-id ctx-id
               :channel channel})))]
    (network-hooks/register-network-handlers!)
    (network-hooks/register-context-route-fns! {:to-server send-context-channel-to-server!
                                                :to-client send-context-channel-to-client!
                                                :to-except-local send-to-except-local-fn})
    (network-hooks/register-context-send-fns! {:to-server send-to-server-fn
                                               :to-client send-to-client-fn})))

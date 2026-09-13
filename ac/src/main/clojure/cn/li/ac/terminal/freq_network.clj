(ns cn.li.ac.terminal.freq-network
  "Server protocol for the original Frequency Transmitter workflow."
  (:require [cn.li.ac.wireless.api :as wireless]
            [cn.li.ac.wireless.core.capability-resolver :as cap-resolver]
            [cn.li.ac.wireless.feedback :as feedback]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.position :as pos]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessNode]))

;; Strings, not integers -- see cn.li.ac.media.network for why.
(def freq-scan-msg "freq-transmitter:scan")
(def freq-config-msg "freq-transmitter:config")

(defn- resolve-hit-target [player]
  (when-let [hit (entity/player-raytrace-block player 4.0 false)]
    (let [world (entity/player-get-level player)
          hx (:x (:hit-pos hit))
          hy (:y (:hit-pos hit))
          hz (:z (:hit-pos hit))
          block-pos (pos/create-block-pos hx hy hz)]
      {:world world
       :pos {:x hx :y hy :z hz}
       :tile (platform-be/get-block-entity world block-pos)})))

(defn- resolve-target [player remembered-pos]
  (if remembered-pos
    (let [world (entity/player-get-level player)
          block-pos (pos/create-block-pos (:x remembered-pos)
                                          (:y remembered-pos)
                                          (:z remembered-pos))]
      {:world world
       :pos remembered-pos
       :tile (platform-be/get-block-entity world block-pos)})
    (resolve-hit-target player)))

(defn- device-info [tile position]
  (cond
    (cap-resolver/matrix-capability tile)
    (when-let [network (wireless/get-wireless-net-by-matrix tile)]
      {:type :matrix :pos position :ssid (wireless/network-ssid network)})

    (cap-resolver/node-capability tile)
    {:type :node
     :pos position
     :node-name (.getNodeName ^IWirelessNode
                              (cap-resolver/node-capability tile))}

    (cap-resolver/generator-capability tile)
    {:type :generator :pos position}

    (cap-resolver/receiver-capability tile)
    {:type :receiver :pos position}

    :else nil))

(defn- handle-scan [_payload player]
  (try
    (if-let [{:keys [pos tile]} (resolve-hit-target player)]
      (if-let [device (device-info tile pos)]
        {:success true :device device}
        {:success false :reason :unsupported
         :error "Target block is not a wireless device"})
      {:success false :reason :not-found :error "No block targeted"})
    (catch Throwable e
      (log/stacktrace "Error in frequency transmitter scan" e)
      {:success false :error (ex-message e)})))

(defn- authorize-source
  [player {:keys [source-pos source-type password]}]
  (if-let [{:keys [tile]} (resolve-target player source-pos)]
    (case source-type
      :matrix
      (if-let [network (wireless/get-wireless-net-by-matrix tile)]
        (if (= (str password) (str (wireless/network-password network)))
          {:success true}
          {:success false :reason :password})
        {:success false :reason :not-found
         :error "Matrix does not own a network"})

      :node
      (if-let [node-cap (cap-resolver/node-capability tile)]
        (if (= (str password) (str (.getPassword ^IWirelessNode node-cap)))
          {:success true}
          {:success false :reason :password})
        {:success false :reason :not-a-node})

      {:success false :reason :unsupported})
    {:success false :reason :not-found
     :error "Source device is no longer available"}))

(defn- result-response [device-type result]
  (assoc result :messages (feedback/result->messages device-type result)))

(defn- link-target
  [player {:keys [source-pos source-type password]}]
  (let [source (resolve-target player source-pos)
        target (resolve-hit-target player)]
    (cond
      (nil? source)
      {:success false :reason :not-found
       :error "Source device is no longer available"}

      (nil? target)
      {:success false :reason :not-found :error "No block targeted"}

      (= source-type :matrix)
      (if (cap-resolver/node-capability (:tile target))
        (result-response
          :node
          (wireless/link-node-to-network!
            (:tile target) (:tile source) password))
        {:success false :reason :not-a-node
         :error "Target is not a wireless node"})

      (= source-type :node)
      (cond
        (cap-resolver/generator-capability (:tile target))
        (result-response
          :generator
          (wireless/link-generator-to-node!
            (:tile target) (:tile source) password false))

        (cap-resolver/receiver-capability (:tile target))
        (result-response
          :receiver
          (wireless/link-receiver-to-node!
            (:tile target) (:tile source) password false))

        :else
        {:success false :reason :not-a-wireless-user
         :error "Target is not a wireless generator or receiver"})

      :else
      {:success false :reason :unsupported})))

(defn- handle-configure [payload player]
  (try
    (case (:operation payload)
      :authorize (authorize-source player payload)
      :link-target (link-target player payload)
      {:success false :reason :unsupported
       :error "Unknown transmitter operation"})
    (catch Throwable e
      (log/stacktrace "Error in frequency transmitter command" e)
      {:success false :error (ex-message e)})))

(defn register-handlers! []
  ;; Freq transmitter apps are launched from the terminal overlay; requests are
  ;; not scoped to an open block container.
  (let [contract {:owner-spec :server :payload-routing :none}]
    (net-server/register-handler freq-scan-msg handle-scan contract)
    (net-server/register-handler freq-config-msg handle-configure contract))
  (log/debug "Frequency transmitter handlers registered"))

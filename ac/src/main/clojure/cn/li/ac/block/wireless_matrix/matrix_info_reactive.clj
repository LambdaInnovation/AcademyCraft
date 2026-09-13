(ns cn.li.ac.block.wireless-matrix.matrix-info-reactive
  "Reactive info-area for Wireless Matrix — SSID/password editing + INIT form.
   Network-info policy/messaging lives here since this namespace is its only
   consumer."
  (:require [cn.li.ac.block.wireless-matrix.logic :as matrix-logic]
            [cn.li.ac.gui.info-area-reactive :as info-area]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessMatrix]
           [cn.li.mcmod.uipojo.runtime UiRt]
           [cn.li.mcmod.ui.node INode]))

;; ============================================================================
;; Data — network info
;; ============================================================================

(defrecord MatrixNetworkData
  [ssid password owner load max-capacity range bandwidth initialized])

(defn network-initialized? [data]
  (:initialized data))

(defn matrix-info-area-policy
  "Return Matrix info-area interaction policy.

  Keys:
  - :show-init?          show initialization form
  - :show-noinit?        show not-initialized message only
  - :editable-ssid?      whether SSID is editable
  - :editable-password?  whether password is editable"
  [initialized? is-owner?]
  (let [owner? (true? is-owner?)]
    (cond
      (true? initialized?)
      {:show-init? false
       :show-noinit? false
       :editable-ssid? owner?
       :editable-password? owner?}

      (and (or (false? initialized?) (nil? initialized?)) owner?)
      {:show-init? true
       :show-noinit? false
       :editable-ssid? false
       :editable-password? false}

      (or (false? initialized?) (nil? initialized?))
      {:show-init? false
       :show-noinit? true
       :editable-ssid? false
       :editable-password? false}

      :else
      {:show-init? false
       :show-noinit? false
       :editable-ssid? false
       :editable-password? false})))

;; ============================================================================
;; Network Messages
;; ============================================================================

(defn- msg
  "Generate message ID for matrix actions (must match server DSL / underscores)."
  [action]
  (msg-registry/msg :matrix action))

(defn- current-client-owner
  "Resolve a client owner for GUI-initiated server requests.
  Uses the platform-registered hook to get the client owner,
  which matches the source used by with-client-response-owner during response dispatch."
  []
  (runtime-hooks/default-client-owner))

(defn send-gather-info
  "Query network information from server."
  [container callback]
  (try
    (if-let [owner (current-client-owner)]
      (net-client/send-to-server owner
        (msg :gather-info)
        (action-payload/action-payload container {})
        (fn [response]
          (try
            (let [data (map->MatrixNetworkData
                         {:ssid (get response :ssid)
                          :password (get response :password)
                          :owner (get response :owner "Unknown")
                          :load (get response :load 0)
                          :max-capacity (get response :max-capacity 16)
                          :range (get response :range 64)
                          :bandwidth (get response :bandwidth 100)
                          :initialized (boolean (if (contains? response :initialized)
                                                  (get response :initialized)
                                                  (get response :ssid)))})]
              (callback data))
            (catch Exception e
              (log/stacktrace "Error processing gather-info response" e)))))
      (log/debug "[send-gather-info] Skip: current-client-owner returned nil"))
    (catch Exception e
      (log/stacktrace "Error sending gather-info" e))))

(defn send-init-network
  [container ssid password callback]
  (try
    (if-let [owner (current-client-owner)]
      (net-client/send-to-server owner
        (msg :init)
        (action-payload/action-payload container {:ssid ssid :password password})
        (fn [response]
          (try
            (callback (get response :success false))
            (catch Exception e
              (log/stacktrace "Error processing init response" e)))))
      (log/debug "[send-init-network] Skip: current-client-owner returned nil"))
    (catch Exception e
      (log/stacktrace "Error sending init" e))))

(defn send-change-ssid
  [container new-ssid]
  (try
    (if-let [owner (current-client-owner)]
      (net-client/send-to-server owner
        (msg :change-ssid)
        (action-payload/action-payload container {:new-ssid new-ssid})
        nil)
      (log/debug "Skip change-ssid: no client session bound"))
    (catch Exception e
      (log/stacktrace "Error sending change-ssid" e))))

(defn send-change-password
  [container new-password]
  (try
    (if-let [owner (current-client-owner)]
      (net-client/send-to-server owner
        (msg :change-password)
        (action-payload/action-payload container {:new-password new-password})
        nil)
      (log/debug "Skip change-password: no client session bound"))
    (catch Exception e
      (log/stacktrace "Error sending change-password" e))))

;; rebuild! is referenced by INIT response helpers defined above it.
(declare rebuild!)

(defn- refresh-after-init!
  [rt container player]
  (fn [success]
    (when success
      (send-gather-info container
        (fn [new-data] (rebuild! rt container player new-data))))))

(defn- make-init-button-handler
  [rt container player ^INode ssid-n ^INode pass-n]
  (fn []
    (let [ssid (str (or (.getOSlot ssid-n 0) ""))
          pass (str (or (.getOSlot pass-n 0) ""))]
      (log/debug "Matrix INIT ssid=" ssid)
      (send-init-network container ssid pass
        (refresh-after-init! rt container player)))))

(defn rebuild!
  [^UiRt rt container player data]
  (try
    (let [tile (:tile-entity container)
          state (matrix-logic/safe-state tile)
          placer (or (try (.getPlacerName ^IWirelessMatrix tile) (catch Exception _ nil))
                     (matrix-logic/placer-name state)
                     (:owner data "Unknown"))
          is-owner? (matrix-logic/owner-authorized? state player)
          policy (matrix-info-area-policy
                   (boolean (network-initialized? data)) is-owner?)
          ctx (info-area/clear-area! rt)
          load-fn (fn [] (double (:load data)))
          max-capacity (max 1.0 (double (:max-capacity data)))]
      (info-area/add-histogram!
        ctx
        [{:label "capacity"
          :color 0xFFFF6C00
          :value-fn load-fn
          :max max-capacity
          :desc-fn (fn [] (str (long (load-fn)) "/" (long max-capacity)))}])
      (info-area/add-sepline! ctx "info")
      (info-area/add-property! ctx "owner" placer)
      (info-area/add-property! ctx "range" (format "%.0f" (double (:range data))))
      (info-area/add-property! ctx "bandwidth" (str (:bandwidth data) " IF/T"))
      (cond
        (network-initialized? data)
        ;; Upstream GuiMatrix: owner sees ssid -> change_pass sepline ->
        ;; password; non-owner sees ssid + password WITHOUT the sepline.
        (do
          (info-area/add-sepline! ctx "wireless_info")
          (if (:editable-ssid? policy)
            (do
              (info-area/add-property! ctx "ssid" (:ssid data)
                :editable? true
                :on-change #(send-change-ssid container %))
              (info-area/add-sepline! ctx "change_pass")
              (info-area/add-property! ctx "password" (:password data)
                :editable? true
                :masked? true
                :on-change #(send-change-password container %)))
            (do
              (info-area/add-property! ctx "ssid" (:ssid data))
              (info-area/add-property! ctx "password" (:password data)
                :masked? true))))

        (:show-init? policy)
        (let [_ (info-area/add-sepline! ctx "wireless_init")
              ssid-row (info-area/add-property! ctx "ssid" ""
                          :editable? true :color-change? false)
              pass-row (info-area/add-property! ctx "password" ""
                          :editable? true :masked? true :color-change? false)
              ^INode ssid-n (:value-node ssid-row)
              ^INode pass-n (:value-node pass-row)]
          ;; Upstream blank(1) between the password field and the INIT button.
          (info-area/add-blank! ctx)
          (info-area/add-button! ctx "INIT"
            (make-init-button-handler rt container player ssid-n pass-n)))

        (:show-noinit? policy)
        (info-area/add-sepline! ctx "wireless_noinit")
        :else nil))
    (catch Exception e
      (log/stacktrace "matrix-info-reactive rebuild failed" e)
      nil)))

(defn attach!
  [^UiRt rt container player]
  (info-area/ensure-shell! rt)
  (send-gather-info container
    (fn [data] (rebuild! rt container player data))))

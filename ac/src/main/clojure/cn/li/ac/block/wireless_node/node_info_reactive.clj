(ns cn.li.ac.block.wireless-node.node-info-reactive
  "Reactive info-area for Wireless Node — editable node name + password."
  (:require [cn.li.ac.block.wireless-node.logic :as node-logic]
            [cn.li.ac.gui.info-area-reactive :as info-area]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.acapi.wireless IWirelessNode]
           [cn.li.mcmod.uipojo.runtime UiRt]))

(def ^:private gui-type :node)
(defn- msg [action] (msg-registry/msg gui-type action))

(defn- node-info-area-policy
  "Compute editable policy for node info-area fields."
  [is-owner?]
  {:editable-node-name? (boolean is-owner?)
   :editable-password? (boolean is-owner?)})

(defn- send-owner
  "Resolve a client owner for GUI-initiated server requests (same pattern as
  matrix_info_reactive). Falls back to nil when no client session is bound."
  []
  (runtime-hooks/default-client-owner))

;; Network-send helpers log action-payload/send failures and keep UI state consistent.
;; On success, update the container atom directly so the value is correct on next rebuild.
(defn send-change-name
  [container new-name]
  (try
    (if-let [owner (send-owner)]
      (let [payload (action-payload/action-payload container {:node-name new-name})]
        (log/debug "[NodeGUI] Sending change-name:" {:new-name new-name :payload payload})
        (net-client/send-to-server owner
          (msg :change-name)
          payload
          (fn [resp]
            (log/debug "[NodeGUI] change-name response:" resp)
            (when (:success resp)
              (reset! (:ssid container) new-name)))))
      (log/debug "Skip change-name: no client session bound"))
    (catch Exception e
      (log/stacktrace "Error sending change-name" e))))

(defn send-change-password
  [container new-pass]
  (try
    (if-let [owner (send-owner)]
      (let [payload (action-payload/action-payload container {:password new-pass})]
        (log/debug "[NodeGUI] Sending change-password:" {:new-pass-len (count new-pass) :payload payload})
        (net-client/send-to-server owner
          (msg :change-password)
          payload
          (fn [resp]
            (log/debug "[NodeGUI] change-password response:" resp)
            (when (:success resp)
              (reset! (:password container) new-pass)))))
      (log/debug "Skip change-password: no client session bound"))
    (catch Exception e
      (log/stacktrace "Error sending change-password" e))))

(defn rebuild!
  [^UiRt rt container player]
  (try
    (let [tile (:tile-entity container)
          tile-state (common/get-tile-state tile)
          owner-name (node-logic/owner-name tile-state)
          is-owner? (node-logic/owner-authorized? tile-state player)
          policy (node-info-area-policy is-owner?)
          node-range (fn []
                       (try (str (.getRange ^IWirelessNode tile))
                            (catch Exception _ "0.0")))
          ctx (info-area/clear-area! rt)]
      (info-area/add-histogram! ctx
        [{:label "Energy" :color 0xFF25C4FF
          :value-fn (fn [] (double (or @(:energy container) 0.0)))
          :max-fn (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
          :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))}
         {:label "capacity" :color 0xFFFF6C00
          :value-fn (fn [] (double (or @(:capacity container) 0.0)))
          ;; :max-fn not :max — :max-capacity arrives via DataSlot sync after
          ;; the info-area is built (gui-init default 0); a captured static max
          ;; of 1.0 drew the bar at 100% while the text row showed the synced
          ;; "2/8".
          :max-fn (fn [] (max 1.0 (double (or @(:max-capacity container) 1.0))))
          :desc-fn (fn [] (str (long (or @(:capacity container) 0))
                               "/" (long (max 1.0 (double (or @(:max-capacity container) 1.0))))))}])
      (info-area/add-sepline! ctx "Info")
      (info-area/add-property! ctx "Range" node-range)
      (info-area/add-property! ctx "Owner" owner-name)
      (if (:editable-node-name? policy)
        (do
          (info-area/add-property! ctx "Node Name" (or @(:ssid container) "")
            :editable? true
            :on-change (fn [new-name] (send-change-name container new-name)))
          (info-area/add-property! ctx "Password" (or @(:password container) "")
            :editable? (:editable-password? policy)
            :masked? true
            :on-change (fn [new-pass] (send-change-password container new-pass))))
        (info-area/add-property! ctx "Node Name" (or @(:ssid container) "")))
      nil)
    (catch Exception e
      (log/stacktrace "node-info-reactive rebuild failed" e)
      nil)))

(defn attach!
  [^UiRt rt container player]
  (info-area/ensure-shell! rt)
  (rebuild! rt container player))

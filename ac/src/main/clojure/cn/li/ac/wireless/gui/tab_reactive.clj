(ns cn.li.ac.wireless.gui.tab-reactive
  "Reactive Wireless Tab — network list + connect/disconnect on embedded page_wireless."
  (:require [cn.li.ac.client.toast :as toast]
            [cn.li.ac.wireless.gui.tab.role-config :as role-config]
            [cn.li.ac.wireless.gui.tab.view-reactive :as view]
            [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcmod.uipojo.runtime UiRt]))

(defn- panel-network-owner [container]
  (container-state/owner-from-container container))

(defn- show-response-toasts!
  [r]
  (doseq [m (:messages r)]
    (toast/show-toast! {:message-key (:key m) :args (:args m)})))

(defn- encrypted-target?
  [t]
  (boolean (:is-encrypted? t)))

(defn- handle-mutation-response!
  "Shared connect/disconnect response path — named to avoid nested callback classes."
  [rebuild! r]
  (show-response-toasts! r)
  (rebuild!))

(defn- disconnect-handler
  [owner disconnect-msg routing-payload rebuild!]
  (fn [_linked]
    (net-client/send-to-server
      owner (disconnect-msg) routing-payload
      (fn [r] (handle-mutation-response! rebuild! r)))))

(defn- connect-handler
  [owner connect-msg connect-payload-fn routing-payload rebuild!]
  (fn [target pass]
    (net-client/send-to-server
      owner
      (connect-msg)
      (connect-payload-fn routing-payload target pass)
      (fn [r] (handle-mutation-response! rebuild! r)))))

(defn- handle-list-response!
  [rt owner cfg routing-payload rebuild! connected-row-logo-path resp]
  (let [{:keys [disconnect-msg connect-msg name-fn connect-payload-fn]} cfg]
    (log/debug "[wireless-reactive] list response" (pr-str (select-keys resp [:linked :avail])))
    (view/rebuild-page!
      rt
      {:linked (:linked resp)
       :avail (vec (:avail resp []))
       :name-fn name-fn
       :encrypted?-fn encrypted-target?
       :disconnect-fn (disconnect-handler owner disconnect-msg routing-payload rebuild!)
       :connect-fn (connect-handler owner connect-msg connect-payload-fn routing-payload rebuild!)})
    (view/set-connected-row-logo! rt connected-row-logo-path)))

(defn- list-response-handler
  [rt owner cfg routing-payload rebuild!* connected-row-logo-path]
  (fn [resp]
    (handle-list-response!
      rt owner cfg routing-payload @rebuild!* connected-row-logo-path resp)))

(defn attach-panel!
  "Wire wireless panel on an existing runtime that already contains page_wireless.xml.
   Returns rebuild! fn (idempotent). Call once per screen; safe to call on first tab switch."
  [^UiRt rt {:keys [role container menu tab-logo-path connected-row-logo-path
                    defer-initial-rebuild?]}]
  (let [cfg (get role-config/role-config role)
        _ (when-not cfg
            (throw (ex-info "Unknown wireless panel role" {:role role})))
        container (cond-> container
                    menu (assoc :minecraft-container menu))
        owner (panel-network-owner container)
        routing-payload (action-payload/action-payload container {})
        {:keys [list-msg]} cfg
        _ (rt/put-user-signal! rt :wireless-scroll (sig/signal-d 0.0))
        _ (rt/put-user-signal! rt :wireless-avail-count (sig/signal-l 0))
        _ (view/attach-scroll-buttons! rt)
        _ (view/setup-panel-logo! rt cfg tab-logo-path)
        ;; Atom breaks self-reference without nesting rebuild! inside response callbacks.
        rebuild!* (atom nil)
        rebuild! (fn []
                   (log/debug "[wireless-reactive] list request" (list-msg) routing-payload)
                   (net-client/send-to-server
                     owner
                     (list-msg)
                     routing-payload
                     (list-response-handler
                       rt owner cfg routing-payload rebuild!* connected-row-logo-path)))]
    (reset! rebuild!* rebuild!)
    (when-not defer-initial-rebuild?
      (rebuild!))
    rebuild!))

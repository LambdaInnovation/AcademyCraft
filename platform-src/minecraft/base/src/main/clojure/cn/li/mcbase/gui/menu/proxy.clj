(ns cn.li.mcbase.gui.menu.proxy
  "Shared CMenuBridge proxy builder for platform adapters.

  Platform namespaces provide small callback/config differences (slot add callback,
  loader-specific remove semantics, player inventory mode defaults), while all menu
  proxy and slot/data-slot sync logic lives here."
  (:require [cn.li.platform.neutral.gui-runtime :as gui-reg]
            [cn.li.platform.neutral.gui-runtime :as cs]
            [cn.li.platform.neutral.gui-runtime :as platform]
            [cn.li.platform.neutral.tabbed-gui :as tabbed]
            [cn.li.mcbase.gui.slots.sync :as slots-sync]
            [cn.li.platform.neutral.hooks :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as runtime-owner]
            [cn.li.mcmod.util.log :as log])
  (:import [cn.li.mcbase.gui CMenuBridge]
           [cn.li.mcbase.shim DelegatingCMenuBridgeBase]
           [net.minecraft.server.level ServerPlayer]
           [net.minecraft.world.entity.player Player]
           [net.minecraft.world.inventory AbstractContainerMenu Slot]
           [net.minecraft.world.item ItemStack]))

(defonce ^:private new-menu-bridge-atom
  (atom nil))

(defn install-new-menu-bridge!
  "Install (fn [menu-type window-id] DelegatingCMenuBridgeBase)."
  [f]
  (reset! new-menu-bridge-atom f)
  f)

(defn- new-menu-bridge
  ^DelegatingCMenuBridgeBase [menu-type window-id]
  (let [f @new-menu-bridge-atom]
    (when (nil? f)
      (throw (IllegalStateException. "menu-bridge factory not installed")))
    ^DelegatingCMenuBridgeBase (f menu-type window-id)))

(defn- owner-map-for-player-context
  "Build canonical menu owner from resolved session/player fields (testable without MC Player)."
  [{:keys [player player-uuid server-session-id client-session-id]}]
  (runtime-owner/require-owner
   (cond
     (and server-session-id player-uuid)
     {:logical-side :server
      :server-session-id server-session-id
      :player-uuid player-uuid
      :player player}

     (and client-session-id player-uuid)
     {:logical-side :client
      :client-session-id client-session-id
      :player-uuid player-uuid
      :player player}

     :else
     (throw (ex-info "GUI menu owner requires session id and player UUID"
                     {:player player
                      :server-session-id server-session-id
                      :client-session-id client-session-id
                      :player-uuid player-uuid})))))

(defn- owner-for-player
  [^Player player]
  (let [player-uuid (some-> player .getUUID str)
        ^ServerPlayer server-player (when (instance? ServerPlayer player) player)
        server-session-id (try
                            (when-let [server (some-> server-player .level .getServer)]
                              [:server (System/identityHashCode server)])
                            (catch Throwable _ nil))
        client-session-id (when (nil? server-session-id)
                            (runtime-hooks/client-session-id))]
    (owner-map-for-player-context
     {:player player
      :player-uuid player-uuid
      :server-session-id server-session-id
      :client-session-id client-session-id})))

(defn- enrich-container-owner
  [clj-container owner]
  (cond-> (assoc clj-container :owner owner)
    (:server-session-id owner) (assoc :server-session-id (:server-session-id owner))
    (:client-session-id owner) (assoc :client-session-id (:client-session-id owner))
    (:player-uuid owner) (assoc :player-uuid (:player-uuid owner))
    (:player owner) (assoc :player (:player owner))))

(defn- remove-menu!
  [this clj-container player {:keys [call-super-removed?
                                     log-message]
                              :or {call-super-removed? false
                                   log-message "Menu closed for player"}}]
  ;; Unregister from atom-based lookup, clear field, close container
  (cs/unregister-menu-container! this)
  (set! (.cljContainer ^CMenuBridge this) nil)
  (platform/safe-close! clj-container)
  (when call-super-removed?
    (let [^CMenuBridge s this]
      (.callSuperRemoved s player)))
  (log/debug log-message (str player)))

(defn- broadcast-menu-changes!
  [this clj-container]
  (platform/server-menu-sync! clj-container)
  (let [^CMenuBridge s this]
    (.callSuperBroadcastChanges s)))

(defn- quick-move-stack
  [this clj-container player slot-index error-prefix]
  (try
    (let [^Slot slot (.getSlot ^AbstractContainerMenu this slot-index)]
      (if (and slot (.hasItem slot))
        (let [^ItemStack stack (.getItem slot)
              moved (.copy stack)
              gui-id (platform/get-gui-id-for-container clj-container)
              [tile-start tile-end] (if gui-id
                                      (gui-reg/get-slot-range gui-id :tile)
                                      [0 -1])
              [player-main-start player-main-end] (if gui-id
                                                    (gui-reg/get-slot-range gui-id :player-main)
                                                    [0 -1])
              [player-hotbar-start player-hotbar-end] (if gui-id
                                                        (gui-reg/get-slot-range gui-id :player-hotbar)
                                                        [0 -1])
              player-start (if (and (<= 0 player-main-start) (<= 0 player-hotbar-start))
                             (min player-main-start player-hotbar-start)
                             player-main-start)
              player-end (max player-main-end player-hotbar-end)
              tile-end-exclusive (inc tile-end)
              player-end-exclusive (inc player-end)
              moved? (cond
                       (and (<= tile-start slot-index) (<= slot-index tile-end)
                            (<= 0 player-start) (<= player-start player-end))
                       (.callSuperMoveItemStackTo ^CMenuBridge this stack player-start player-end-exclusive true)

                       (and (<= player-start slot-index) (<= slot-index player-end)
                            (<= 0 tile-start) (<= tile-start tile-end))
                       (.callSuperMoveItemStackTo ^CMenuBridge this stack tile-start tile-end-exclusive false)

                       :else false)]
          (if-not moved?
            ItemStack/EMPTY
            (if (= (.getCount stack) (.getCount moved))
              ItemStack/EMPTY
              (do
                (if (.isEmpty stack)
                  (.setByPlayer slot ItemStack/EMPTY)
                  (.setChanged slot))
                (.onTake slot player stack)
                moved))))
        ItemStack/EMPTY))
    (catch Exception e
      (log/stacktrace error-prefix e)
      ItemStack/EMPTY)))

(defn- finalize-menu-registration!
  "Register clj-container in the container-state atom for lookups.
   Also stores on the CMenuBridge field for direct access where available."
  [menu _window-id clj-container _owner]
  (cs/register-menu-container! menu clj-container)
  (set! (.cljContainer ^CMenuBridge menu) clj-container)
  menu)

(defn menu-proxy-opts
  "Return shared CMenuBridge options.

  Loader components own any loader-specific overrides and pass them as opts."
  ([] (menu-proxy-opts nil))
  ([opts]
   (merge
    {:get-slot-layout gui-reg/get-slot-layout
     :default-player-inventory-mode :full
     :call-super-removed? false
     :remove-log-message "Menu closed for player"
     :quick-move-error-prefix "Error in quickMoveStack:"}
    opts)))

(defn quick-move-allowed?
  "Return true when quick-move/shift-click is allowed for the current tab state."
  [menu clj-container]
  (or (not (tabbed/tabbed-container? clj-container))
      (tabbed/slots-active-for-menu? menu clj-container)))

(defn create-menu-proxy
  "Create a CMenuBridge proxy around a Clojure container.

  Optional opts:
  - :get-slot-layout          fn [gui-id] -> layout map (default gui/get-slot-layout)
  - :default-player-inventory-mode  :full/:hotbar-only/:none (default :full)
  - :player                   player owning this menu, when available
  - :call-super-removed?      boolean
  - :remove-log-message       string
  - :quick-move-error-prefix  string"
  [window-id menu-type clj-container {:keys [get-slot-layout
                                             default-player-inventory-mode
                                             player
                                             call-super-removed?
                                             remove-log-message
                                             quick-move-error-prefix]
                                      :or {get-slot-layout gui-reg/get-slot-layout
                                           default-player-inventory-mode :full
                                           call-super-removed? false
                                           remove-log-message "Menu closed for player"
                                           quick-move-error-prefix "Error in quickMoveStack:"}}]
  (let [owner (owner-for-player player)
        clj-container (enrich-container-owner clj-container owner)
        ^DelegatingCMenuBridgeBase menu
        (doto (new-menu-bridge menu-type (int window-id))
               (.withStillValid (fn [_this player]
                 (boolean (platform/safe-validate clj-container player))))

               (.withRemoved (fn [this player]
                 (remove-menu!
                  this
                  clj-container
                  player
                  {:call-super-removed? call-super-removed?
                   :log-message remove-log-message})))

               (.withBroadcastChanges (fn [this]
                 (broadcast-menu-changes! this clj-container)))

               (.withClicked (fn [this slot-index button click-type player]
                 (when (or (not (tabbed/tabbed-container? clj-container))
                           (tabbed/slots-active-for-menu? this clj-container))
                   (let [^DelegatingCMenuBridgeBase s this]
                     (.callSuperClicked s slot-index button click-type player)))))

               (.withQuickMoveStack (fn [this player slot-index]
                 (if-not (quick-move-allowed? this clj-container)
                   ItemStack/EMPTY
                   (quick-move-stack this clj-container player slot-index quick-move-error-prefix)))))]
    (slots-sync/setup-menu-slots! menu clj-container nil {:get-slot-layout get-slot-layout
                                                        :default-player-inventory-mode default-player-inventory-mode})
    (finalize-menu-registration! menu window-id clj-container owner)))

(defn create-menu-proxy-with-defaults
  "Create a menu proxy with shared defaults plus loader-provided opts."
  [window-id menu-type clj-container opts]
  (create-menu-proxy
   window-id
   menu-type
   clj-container
   (menu-proxy-opts opts)))

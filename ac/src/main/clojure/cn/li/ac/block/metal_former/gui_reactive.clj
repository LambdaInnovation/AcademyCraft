(ns cn.li.ac.block.metal-former.gui-reactive
  "Reactive GUI registration for the Metal Former.
   Owns container wiring, slots, registration, network actions, and reactive rendering."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.signal :as sig]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.block.metal-former.config :as cfg]
            [cn.li.ac.block.metal-former.recipes :as recipes]
            [cn.li.ac.block.metal-former.schema :as former-schema]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.mcmod.gui.container.action-payload :as action-payload]
            [cn.li.ac.wireless.gui.message.registry :as msg-registry]
            [cn.li.ac.energy.operations :as energy]
            [clojure.string :as str]
            [cn.li.mcmod.ui.events]))

;; ============================================================================
;; Schema + container
;; ============================================================================

(def ^:private former-slot-schema-id :metal-former)
(def ^:private former-gui-type :metal-former)
(def ^:private former-gui-schema
  (mapv (fn [field]
          (if (= (:key field) :mode)
            (assoc field
                   :gui-init (fn [state] (recipes/normalize-mode (get state :mode (:default field))))
                   :gui-coerce recipes/normalize-mode)
            field))
        former-schema/metal-former-schema))
(def ^:private former-sync (gui-sync/schema-sync-fns former-gui-schema))

(defn- msg [action] (msg-registry/msg former-gui-type action))

(defn- create-container [tile player]
  (gui-sync/create-schema-container former-gui-schema tile player former-gui-type
                                    {:gui-id (gui-manifest/gui-id :metal-former)}))

(defn- get-slot-count [_] (slot-schema/tile-slot-count former-slot-schema-id))
(defn- get-slot-item [c i] (common/get-slot-item-be c i))
(defn- set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
(defn- slot-changed! [_ _] nil)
(defn- can-place-item? [_ i s]
  (case (int i) 0 (recipes/is-valid-input-item? s) 1 false 2 (energy/is-energy-item-supported? s) false))
(defn- still-valid? [_ _] true)
(def ^:private server-menu-sync! (:server-menu-sync! former-sync))
(def ^:private on-close (:on-close former-sync))
(defn- handle-button-click! [_ _ _] nil)

(def ^:private quick-move-config
  (delay (slot-schema/build-quick-move-config former-slot-schema-id
           {:inventory-pred (fn [i s] (>= i s))
            :rules [{:accept? energy/is-energy-item-supported? :slot-ids [:energy]}
                    {:accept? recipes/is-valid-input-item? :slot-ids [:input]}]})))
(defn- quick-move-stack [c i s] (move-common/quick-move-with-rules c i s @quick-move-config))

;; ============================================================================
;; Network actions
;; ============================================================================

(defn- request-alternate! [container dir]
  ;; Explicit owner: this fires from a button click, and the ThreadLocal
  ;; send-to-server would otherwise fall back to is not bound there.
  ;; default-client-owner reads the live connection instead.
  (net-client/send-to-server (runtime-hooks/default-client-owner) (msg :alternate)
    (action-payload/action-payload container {:dir (int dir)})
    (fn [resp] (when (and (:success resp) (:mode resp))
                 (reset! (:mode container) (recipes/normalize-mode (:mode resp)))))))

;; ============================================================================
;; Reactive rendering (replaces on-frame polling + find-widget + set-texture!)
;; ============================================================================

(defn- attach-binds!
  "Reactive replacement for bind-progress! + bind-mode-icon! + bind-buttons!"
  [r container menu _player _signals]
  ;; Merge menu into container so action-payload can resolve container-id
  ;; (click handlers in closures capture container — see send-link-query! pattern).
  (let [container (assoc container :minecraft-container menu)
        clock (rt/clock-ms-sig r)
        ;; Work progress — upstream GuiMetalFormer drives the XML progress bar
        ;; each frame from tile.getWorkProgress. Must be BOUND (ui/bind!) to the
        ;; :progress node; a bare put-user-signal! is never consumed.
        progress-sig (sig/computed-d [clock]
                       (fn [_] (if @(:working container)
                                 (max 0.0 (min 1.0 (/ (double @(:work-counter container))
                                                      (double cfg/work-ticks))))
                                 0.0)))
        ;; Mode icon — upstream updates icon_mode's texture on mode change /
        ;; sync response (updateModeTexture); bind the src so switching modes
        ;; (or the server returning a new mode) repaints the icon.
        mode-sig (sig/computed-o [clock]
                   (fn [_] (recipes/mode->icon-texture @(:mode container))))]
    (ui/bind! r :progress :progress progress-sig)
    (ui/bind! r :icon_mode :src mode-sig)
    ;; Mode switching buttons (replaces bind-buttons! + on-left-click)
    (cn.li.mcmod.ui.events/on! r :btn_left :left-click
      (fn [_rt _n _e] (request-alternate! container -1)))
    (cn.li.mcmod.ui.events/on! r :btn_right :left-click
      (fn [_rt _n _e] (request-alternate! container 1)))))

(defn create-screen [container menu player]
  (let [safe-val #(some-> % deref)]
    (bgui/create-screen
      {:page-xml "guis/rework/new/page_metalformer.xml"
       :texture-name "metalformer"
       :container container :menu menu
       ;; Upstream GuiMetalFormer info page: histEnergy (blue, "%.0f IF").
      :histograms [(bgui/hist-buffer (fn [] (double (or @(:energy container) 0.0)))
                                      (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
                                      {:label "Energy" :color 0xFF25C4FF
                                       :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})]
       :properties {:mode (fn [] (str/upper-case (recipes/mode->string @(:mode container))))
                    :progress (fn [] (if @(:working container)
                                      (format "%d/%d" @(:work-counter container) cfg/work-ticks)
                                      "IDLE"))}
       :wireless? true :wireless-role :receiver
       :custom-bind! attach-binds!})))

(def update! bgui/update-signals!)
(def open! bgui/open!)

;; ============================================================================
;; Registration
;; ============================================================================

(defn- former-container? [c]
  (and (map? c) (= (:container-type c) former-gui-type)
       (contains? c :tile-entity) (contains? c :mode) (contains? c :energy)))

(defn init-metal-former-reactive!
  []
  (install/framework-once! ::metal-former-reactive-installed?
  (fn []
    (slot-schema/register-slot-schema!
      {:schema-id former-slot-schema-id
       :slots [{:id :input :type :input :x 13 :y 49}
               {:id :output :type :output :x 143 :y 49}
               {:id :energy :type :energy :x 42 :y 80}]})
    (gui-reg/register-block-gui!
      (gui-manifest/gui-name :metal-former)
      (merge (gui-manifest/gui-registration :metal-former)
             {:container-predicate former-container?
              :container-fn create-container
              :screen-fn create-screen
              :server-menu-sync-fn server-menu-sync!
              :validate-fn still-valid?
              :close-fn on-close
              :button-click-fn handle-button-click!
              :slot-count-fn get-slot-count
              :slot-get-fn get-slot-item
              :slot-set-fn set-slot-item!
              :slot-can-place-fn can-place-item?
              :slot-changed-fn slot-changed!
              :quick-move-fn quick-move-stack}))
    (log/debug "Metal Former GUI initialized (reactive)"))))

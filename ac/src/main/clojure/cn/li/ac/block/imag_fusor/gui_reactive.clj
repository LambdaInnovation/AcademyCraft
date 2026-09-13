(ns cn.li.ac.block.imag-fusor.gui-reactive
  "Reactive GUI registration for the Imag Fusor."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg] [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.platform.item :as pitem] [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.util.log :as log] [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.core :as ui]
            [cn.li.mcmod.ui.node :as node]
            [cn.li.mcmod.ui.runtime :as rt] [cn.li.mcmod.ui.signal :as sig]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.ac.block.imag-fusor.config :as cfg]
            [cn.li.ac.block.imag-fusor.recipes :as recipes]
            [cn.li.ac.block.imag-fusor.schema :as fusor-schema]))

(def fusor-gui-type :imag-fusor) (def fusor-slot-schema-id :imag-fusor)
(def ^:private fusor-sync (gui-sync/schema-sync-fns fusor-schema/imag-fusor-schema))
(defn- phase-liquid-unit? [stack] (and stack (= (recipes/item-id-from-stack stack) cfg/matter-unit-item-id)))
(defn- create-container [tile player] (gui-sync/create-schema-container fusor-schema/imag-fusor-schema tile player fusor-gui-type {:gui-id (gui-manifest/gui-id :imag-fusor)}))
(defn- get-slot-count [_] (slot-schema/tile-slot-count fusor-slot-schema-id))
(defn- get-slot-item [c i] (common/get-slot-item-be c i))
(defn- set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
;; Slot indices mirror upstream TileImagFusor: 0 crystal input, 1 crystal
;; output, 2 phase-liquid unit input, 3 energy item, 4 empty-unit output.
;; The GUI previously registered 4 slots in a different order, so the input
;; slot actually fed the crystal OUTPUT index and the liquid input was
;; unreachable (its slot was marked output).
(defn- crystal-input? [s] (boolean (recipes/find-recipe s)))
(defn- can-place-item? [_ i s]
  (case (int i)
    0 (crystal-input? s)
    1 false
    2 (phase-liquid-unit? s)
    3 (energy/is-energy-item-supported? s)
    4 false
    false))
(defn- still-valid? [_ _] true) (def server-menu-sync! (:server-menu-sync! fusor-sync))
(def on-close (:on-close fusor-sync)) (defn- handle-button-click! [_ _ _] nil)
(def ^:private inventory-pred (fn [i s] (>= i s)))
(def ^:private quick-move-config (delay (slot-schema/build-quick-move-config fusor-slot-schema-id {:inventory-pred inventory-pred :rules [{:accept? crystal-input? :slot-ids [:crystal-input]} {:accept? phase-liquid-unit? :slot-ids [:imag-input]} {:accept? energy/is-energy-item-supported? :slot-ids [:energy]}]})))
(defn- quick-move-stack [c i s] (move-common/quick-move-with-rules c i s @quick-move-config))

(defn- attach-binds! [r container _menu _player _signals]
  ;; Upstream GuiImagFusor drives the XML progress bar each frame from
  ;; tile.getWorkProgress and the requirement text from getCurrentRecipe
  ;; (IDLE when no recipe). Bind both instead of leaving static text.
  (let [clock (rt/clock-ms-sig r)
        progress-sig (sig/computed-d [clock]
                       (fn [_] (double (or @(:work-progress container) 0.0))))
        liquid-sig (sig/computed-o [clock]
                     (fn [_]
                       (let [need (int (or @(:current-recipe-liquid container) 0))]
                         (if (pos? need) (str need) "IDLE"))))]
    (ui/bind! r :progress :progress progress-sig)
    (ui/bind! r :text_imagneeded :text liquid-sig)))

(defn create-screen [container menu player]
  (let [safe-val #(some-> % deref)]
    (bgui/create-screen
      {:page-xml "guis/rework/new/page_imagfusor.xml" :texture-name "imagfusor"
       :container container :menu menu
       ;; Upstream GuiImagFusor info page: energy + phase-liquid histograms.
      :histograms [(bgui/hist-buffer (fn [] (double @(:energy container)))
                                     (fn [] (max 1.0 (double @(:max-energy container))))
                                     {:label "Energy" :color 0xFF25C4FF
                                      :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})
                    (bgui/hist-buffer (fn [] (double (or @(:liquid-amount container) 0)))
                                     (fn [] (max 1.0 (double (or @(:tank-size container) 1))))
                                     {:label "Liquid" :color 0xFF7680DE
                                      :desc-fn (fn [] (format "%.0f mB" (double (or @(:liquid-amount container) 0))))})]
       :properties {:status (fn [] (or (safe-val (:status container)) "IDLE"))}
       :wireless? true :wireless-role :machine :custom-bind! attach-binds!})))

(def update! bgui/update-signals!) (def open! bgui/open!)
(defn- fusor-container? [c] (and (map? c) (= (:container-type c) fusor-gui-type) (contains? c :tile-entity) (contains? c :energy)))
(defn init-imag-fusor-reactive! []
  (install/framework-once! ::imag-fusor-reactive-installed?
  (fn []
    ;; Slot positions match upstream ContainerImagFusor (vanilla container
    ;; coords on the ui_imagfusor texture): crystal in (13,49), crystal out
    ;; (143,49), liquid in (13,10), energy (42,80), empty unit out (143,10).
    ;; :can-place restricts mayPlace at the slot level (upstream SlotCrystal /
    ;; SlotMatterUnit) — without it a crystal could enter the liquid slot,
    ;; where the machine logic then consumed it.
    (slot-schema/register-slot-schema! {:schema-id fusor-slot-schema-id :slots [{:id :crystal-input :type :input :x 13 :y 49 :can-place crystal-input?} {:id :crystal-output :type :output :x 143 :y 49} {:id :imag-input :type :input :x 13 :y 10 :can-place phase-liquid-unit?} {:id :energy :type :energy :x 42 :y 80} {:id :imag-output :type :output :x 143 :y 10}]})
    (gui-reg/register-block-gui! (gui-manifest/gui-name :imag-fusor) (merge (gui-manifest/gui-registration :imag-fusor) {:container-predicate fusor-container? :container-fn create-container :screen-fn create-screen :server-menu-sync-fn server-menu-sync! :validate-fn still-valid? :close-fn on-close :button-click-fn handle-button-click! :slot-count-fn get-slot-count :slot-get-fn get-slot-item :slot-set-fn set-slot-item! :slot-can-place-fn can-place-item? :slot-changed-fn (fn [_ _] nil) :quick-move-fn quick-move-stack}))
    (log/debug "Imag Fusor GUI initialized (reactive)"))))

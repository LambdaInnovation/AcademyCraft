(ns cn.li.ac.block.phase-gen.gui-reactive
  "Reactive GUI registration for the Phase Generator."
  (:refer-clojure :exclude [sync])
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg] [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.ac.energy.operations :as energy]
            [cn.li.mcmod.util.log :as log] [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.container.move :as move-common]
            [cn.li.ac.block.machine.matter-unit :as matter-unit]
            [cn.li.ac.block.phase-gen.schema :as phase-schema]
            [cn.li.ac.block.phase-gen.config :as phase-config]))

(def ^:private slot-schema-id :phase-gen) (def ^:private gui-type :phase-gen)
(def ^:private sync (gui-sync/schema-sync-fns phase-schema/phase-gen-schema))

(defn- phase-liquid-unit? [s] (matter-unit/phase-liquid-unit? s phase-config/matter-unit-item-id))

(defn create-container [tile player] (gui-sync/create-schema-container phase-schema/phase-gen-schema tile player gui-type {:gui-id (gui-manifest/gui-id :phase-gen)}))
(defn get-slot-count [_] (slot-schema/tile-slot-count slot-schema-id))
(defn get-slot-item [c i] (common/get-slot-item-be c i))
(defn set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
(defn can-place-item? [_ i s] (case (int i) 0 (phase-liquid-unit? s) 1 false 2 (energy/is-energy-item-supported? s) false))
(defn still-valid? [_ _] true) (def server-menu-sync! (:server-menu-sync! sync))
(def on-close (:on-close sync)) (defn handle-button-click! [_ _ _] nil)
(def ^:private inventory-pred (fn [i s] (>= i s)))
(def ^:private quick-move-config
  (delay (slot-schema/build-quick-move-config
          slot-schema-id
          {:inventory-pred inventory-pred
           :rules [{:accept? energy/is-energy-item-supported? :slot-ids [:energy]}
                   {:accept? phase-liquid-unit? :slot-ids [:liquid-in]}]})))
(defn- quick-move-stack [c i s] (move-common/quick-move-with-rules c i s @quick-move-config))

(defn create-screen [container menu player]
  (let [safe-val #(some-> % deref)]
    (bgui/create-screen
      {:page-xml "guis/rework/new/page_phasegen.xml" :texture-name "phasegen"
       :container container :menu menu
       ;; Upstream GuiPhaseGen info page: energy (histEnergy) + liquid
       ;; histogram labeled "IF" (0xffb983fb) with "%d mB" description.
       :histograms [(bgui/hist-buffer (fn [] (double (or @(:energy container) 0.0)))
                                      (fn [] (max 1.0 (double @(:max-energy container))))
                                      {:label "Energy" :color 0xFF25C4FF
                                       :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})
                    (bgui/hist-buffer (fn [] (double (or @(:liquid-amount container) 0)))
                                      (fn [] (max 1.0 (double (or @(:tank-size container) 1))))
                                      {:label "IF" :color 0xFFB983FB
                                       :desc-fn (fn [] (format "%d mB" (int (or @(:liquid-amount container) 0))))})]
       :properties {:status (fn [] (or (safe-val (:status container)) "IDLE"))}
       :wireless? true :wireless-role :generator})))

(def update! bgui/update-signals!) (def open! bgui/open!)
(defn- container? [c] (and (map? c) (= (:container-type c) gui-type) (contains? c :tile-entity) (contains? c :energy)))
(defn init-phase-gen-reactive! []
  (install/framework-once! ::phase-gen-reactive-installed?
  (fn []
    ;; Slot positions match upstream ContainerPhaseGen on the ui_phasegen
    ;; texture: liquid in (45,12), liquid out (112,51), energy (42,80).
    ;; :can-place restricts mayPlace at the slot level (upstream
    ;; SlotMatterUnit) — without it any item could enter the liquid input.
    (slot-schema/register-slot-schema! {:schema-id slot-schema-id :slots [{:id :liquid-in :type :input :x 45 :y 12 :can-place phase-liquid-unit?} {:id :liquid-out :type :output :x 112 :y 51} {:id :energy :type :energy :x 42 :y 80}]})
    (gui-reg/register-block-gui! (gui-manifest/gui-name :phase-gen) (merge (gui-manifest/gui-registration :phase-gen) {:container-predicate container? :container-fn create-container :screen-fn create-screen :server-menu-sync-fn server-menu-sync! :validate-fn still-valid? :close-fn on-close :button-click-fn handle-button-click! :slot-count-fn get-slot-count :slot-get-fn get-slot-item :slot-set-fn set-slot-item! :slot-can-place-fn can-place-item? :slot-changed-fn (fn [_ _] nil) :quick-move-fn quick-move-stack}))
    (log/debug "Phase Generator GUI initialized (reactive)"))))

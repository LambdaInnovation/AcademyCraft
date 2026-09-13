(ns cn.li.ac.block.solar-gen.gui-reactive
  "Reactive GUI registration for the Solar Generator."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg] [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.ac.energy.operations :as energy] [cn.li.mcmod.util.log :as log]
            [cn.li.ac.gui.manifest :as gui-manifest] [cn.li.ac.gui.block-gui-reactive :as bgui]
            [cn.li.mcmod.ui.runtime :as rt] [cn.li.mcmod.ui.core :as ui] [cn.li.mcmod.ui.signal :as sig]
            [cn.li.ac.block.gui.sync :as gui-sync] [cn.li.ac.config.modid :as modid]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.block.solar-gen.schema :as solar-schema])
  (:import [cn.li.mcmod.ui.node INode]))

(def solar-gen-id :solar-gen)
(def ^:private solar-sync (gui-sync/schema-sync-fns solar-schema/unified-solar-schema))
(defn create-container [tile player] (gui-sync/create-schema-container solar-schema/unified-solar-schema tile player :solar {:gui-id (gui-manifest/gui-id :solar-gen)}))
(defn get-slot-count [_] (slot-schema/tile-slot-count solar-gen-id))
(defn can-place-item? [_ _ s] (energy/is-energy-item-supported? s))
(defn get-slot-item [c i] (common/get-slot-item-be c i))
(defn set-slot-item! [c i s] (common/set-slot-item-be! c i s {:inventory [nil]} identity))
(defn slot-changed! [_ _] nil) (defn still-valid? [_ _] true)
(def server-menu-sync! (:server-menu-sync! solar-sync)) (def on-close (:on-close solar-sync))
(defn handle-button-click! [_ _ _] nil)

(def ^:private effect-solar-texture (modid/asset-path "textures" "guis/effect/effect_solar.png"))
(defn- solar-status->frame [status] (case status "STRONG" 0.0 "WEAK" (/ 2.0 3.0) (/ 1.0 3.0)))

(defn- attach-anim-bind! [r container _menu _player _signals]
  ;; "anim_frame" is a group defined in page_solar.xml (id="anim_frame")
  ;; — now keywordized by parse-ui-element.
  (when-let [^INode anim-frame (rt/node-by-id r :anim_frame)]
    (rt/build-child! r
      {:kind :image
       :props {:id :solar-anim-img :x 0.0 :y 0.0 :w 104.0 :h 70.0 :alpha 1.0
               :src effect-solar-texture :tex-h (/ 1.0 3.0)}}
      anim-frame)
    (ui/bind! r :solar-anim-img :v
      (sig/computed-d [(rt/clock-ms-sig r)]
        (fn [_] (solar-status->frame (or @(:status container) "STOPPED")))))))

(defn create-screen [container menu player]
  (let [safe-val #(some-> % deref)]
    (bgui/create-screen
      {:page-xml "guis/rework/new/page_solar.xml" :texture-name "windbase"
       :container container :menu menu
       ;; Upstream GuiSolarGen info page: histEnergy (blue, "%.0f IF").
       :histograms [(bgui/hist-buffer (fn [] (double (or @(:energy container) 0.0)))
                                      (fn [] (max 1.0 (double (or @(:max-energy container) 1.0))))
                                      {:label "Energy" :color 0xFF25C4FF
                                       :desc-fn (fn [] (format "%.0f IF" (double (or @(:energy container) 0.0))))})]
       :properties {:gen_speed (fn [] (format "%.2fIF/T" (double (or @(:gen-speed container) 0.0))))
                    :status (fn [] (or @(:status container) "STOPPED"))}
       :wireless? true :wireless-role :generator :custom-bind! attach-anim-bind!})))

(def update! bgui/update-signals!) (def open! bgui/open!)
(defn- solar-container? [c] (and (map? c) (contains? c :tile-entity) (contains? c :energy) (contains? c :status)))
(defn init-solar-reactive! []
  (install/framework-once! ::solar-gui-reactive-installed?
  (fn []
    (slot-schema/register-slot-schema! {:schema-id solar-gen-id :slots [{:id :energy :type :energy :x 42 :y 81}]})
    (gui-reg/register-block-gui! (gui-manifest/gui-name :solar-gen) (merge (gui-manifest/gui-registration :solar-gen) {:container-predicate solar-container? :container-fn create-container :screen-fn create-screen :server-menu-sync-fn server-menu-sync! :validate-fn still-valid? :close-fn on-close :button-click-fn handle-button-click! :slot-count-fn get-slot-count :slot-get-fn get-slot-item :slot-set-fn set-slot-item! :slot-can-place-fn can-place-item? :slot-changed-fn slot-changed!}))
    (log/debug "Solar Generator GUI initialized (reactive)"))))

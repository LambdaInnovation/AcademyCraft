(ns cn.li.ac.integration.block.energy-converter.gui-reactive
  "Complete reactive replacement for energy_converter/gui.clj (deleted).
   Container/slot/sync/derived-mode logic was ported verbatim — none of it
   ever touched CGUI, only the old create-screen/create-wireless-page did.

   The old screen showed only the wireless panel (no inventory tab, no
   histogram — the block has zero I/O slots), so this does not go through
   cn.li.ac.gui.block-gui-reactive (which always builds an inv+wireless
   tab pair); it builds a bare page_wireless.xml runtime instead, matching
   the old single-page layout exactly."
  (:require [cn.li.mcmod.runtime.install :as install]
            [cn.li.ac.config.modid :as modid]
            [cn.li.mcmod.gui.spec :as gui-reg]
            [cn.li.mcmod.gui.slot-schema :as slot-schema]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.wireless.gui.tab-reactive :as wireless-tab]
            [cn.li.mcmod.platform.be :as platform-be]
            [cn.li.mcmod.ui.runtime :as rt]
            [cn.li.mcmod.ui.xml :as ui-xml]
            [cn.li.ac.integration.block.energy-converter.config :as ec-config]
            [cn.li.ac.integration.block.energy-converter.schema :as ec-schema]))

(def ^:private converter-slot-schema-id :energy-converter)
(def ^:private converter-gui-type :energy-converter)

;; ============================================================================
;; Container — derived generator/receiver mode + schema sync (ported verbatim
;; from energy_converter/gui.clj)
;; ============================================================================

(defn- output-block?
  [block-id]
  (ec-config/output-block? block-id))

(defn- block-wireless-mode
  [block-id]
  (if (output-block? block-id) :generator :receiver))

(defn- reset-derived-mode!
  [container]
  (let [mode (block-wireless-mode (:block-id container))]
    (reset! (:wireless-mode container) mode)
    (reset! (:status container) (if (= mode :generator) "OUTPUT" "INPUT"))))

(def ^:private converter-sync
  (gui-sync/schema-sync-fns ec-schema/energy-converter-gui-schema
    {:after-sync! reset-derived-mode!}))

(defn create-container
  [tile player]
  (let [state (or (common/get-tile-state tile) {})
        block-id (str (platform-be/get-block-id tile))]
    (gui-sync/create-schema-container ec-schema/energy-converter-gui-schema
      tile
      player
      converter-gui-type
      {:gui-id (gui-manifest/gui-id :energy-converter)
       :state state
       :base {:block-id block-id
              :wireless-mode (atom (block-wireless-mode block-id))
              :status (atom (if (output-block? block-id) "OUTPUT" "INPUT"))}})))

(defn get-slot-count [_container]
  (slot-schema/tile-slot-count converter-slot-schema-id))

(defn get-slot-item [_container _slot-index] nil)
(defn set-slot-item! [_container _slot-index _item-stack] nil)
(defn slot-changed! [_container _slot-index] nil)
(defn can-place-item? [_container _slot-index _item-stack] false)
(defn still-valid? [_container _player] true)

(def server-menu-sync! (:server-menu-sync! converter-sync))
(def on-close (:on-close converter-sync))
(defn handle-button-click! [_container _button-id _player] nil)

(defn converter-container?
  [container]
  (and (map? container)
       (= (:container-type container) converter-gui-type)
       (contains? container :tile-entity)
       (contains? container :energy)))

;; ============================================================================
;; Reactive screen — bare wireless panel, role derived from block-id
;; ============================================================================

(defn create-screen
  [container menu _player]
  (let [r (rt/create-runtime)
        spec (ui-xml/load-spec (modid/namespaced-path "guis/rework/new/page_wireless.xml"))
        _ (rt/build! r spec)
        role (if (= @(:wireless-mode container) :generator) :generator :receiver)]
    (wireless-tab/attach-panel! r {:role role :container container :menu menu})
    {:type :reactive-container-screen
     :runtime r
     :container container
     :menu menu
     :size-dx 0
     :size-dy 21}))

;; ============================================================================
;; Registration
;; ============================================================================

(defn register-converter-guis-reactive!
  []
  (install/framework-once! ::converter-gui-reactive-installed?
  (fn []
    (slot-schema/register-slot-schema!
      {:schema-id :energy-converter
       :slots []})
    (gui-reg/register-block-gui!
      (gui-manifest/gui-name :energy-converter)
      (merge (gui-manifest/gui-registration :energy-converter)
        {:container-predicate converter-container?
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
         :slot-changed-fn slot-changed!}))
    (log/debug "Energy Converter GUI initialized (reactive: wireless panel, gui-id 14)"))))

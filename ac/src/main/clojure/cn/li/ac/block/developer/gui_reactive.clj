(ns cn.li.ac.block.developer.gui-reactive
  "Reactive GUI registration for the Developer block.
   Owns container, slot, sync, and validation wiring. Screen content
   (developer layout, console, skill-tree area, cover overlays) lives in
   panel-reactive.clj."
  (:require [clojure.string :as str]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.gui.spec :as gui-reg]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.gui.manifest :as gui-manifest]
            [cn.li.ac.wireless.gui.container.common :as common]
            [cn.li.ac.block.developer.schema :as dev-schema]
            [cn.li.ac.block.developer.logic :as dev-logic]
            [cn.li.ac.block.developer.session :as dev-session]
            [cn.li.ac.block.developer.panel-reactive :as panel-reactive]
            [cn.li.ac.block.developer.console-reactive :as console-reactive]
            [cn.li.ac.block.machine.runtime :as machine-runtime]
            [cn.li.ac.block.gui.sync :as gui-sync]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.world :as world]
            [cn.li.mcmod.platform.be :as platform-be]))

;; ============================================================================
;; Container — internal inventory (2 slots, not user-visible) + state sync
;; ============================================================================

(def ^:private developer-sync
  (gui-sync/schema-sync-fns dev-schema/unified-developer-schema))

(defn create-container [tile player]
  (let [state (or (common/get-tile-state tile) {})]
    (gui-sync/create-schema-container dev-schema/unified-developer-schema
                                      tile player :developer
                                      {:gui-id (gui-manifest/gui-id :developer)
                                       :state state})))

(defn get-slot-count [_container]
  0)  ;; No visible slots — inventory is internal-only for automation

(defn can-place-item? [_container _slot-index _item-stack]
  true)

(defn get-slot-item [container slot-index]
  (common/get-slot-item-be container slot-index))

(defn set-slot-item! [container slot-index item-stack]
  (common/set-slot-item-be! container slot-index item-stack
                            dev-logic/dev-default-state
                            (fn [state]
                              (let [v (vec (take 2 (concat (vec (:inventory state [])) (repeat nil))))]
                                (assoc state :inventory v))))
  (when-let [tile (:tile-entity container)]
    (try (platform-be/set-changed! tile) (catch Exception _ nil)))
  nil)

(defn slot-changed! [_container _slot-index] nil)

(defn still-valid? [container player]
  (and (common/still-valid? container player)
       (let [tile (:tile-entity container)
             st (or (common/get-tile-state tile) {})
             pid (uuid/player-uuid player)
             holder (str (:user-uuid st ""))]
         (or (str/blank? holder) (= holder pid)))))

(def server-menu-sync! (:server-menu-sync! developer-sync))

(defn handle-button-click! [_container _button-id _player] nil)

(defn on-close [container]
  ((:on-close developer-sync) container)
  (when-let [tile (:tile-entity container)]
    (when-let [pl (:player container)]
      (let [lvl (entity/player-get-level pl)]
        (when (and lvl (not (world/client-side? lvl)))
          (try
            (machine-runtime/commit-transform! tile dev-logic/dev-default-state
              #(-> % (assoc :user-uuid "" :user-name "") dev-session/clear-session))
            (catch Exception e
              (log/debug "Developer on-close tile update:" (ex-message e)))))))))

(defn developer-container? [container]
  (and (map? container)
       (contains? container :tile-entity)
       (= :developer (:container-type container))))

;; ============================================================================
;; Screen + registration
;; ============================================================================

(defn create-screen [container menu player]
  (panel-reactive/create-screen container menu player))

(defn init-developer-reactive!
  []
  (install/framework-once! ::developer-gui-reactive-installed?
  (fn []
    (console-reactive/register-builtin-commands!)
    (gui-reg/register-block-gui!
      (gui-manifest/gui-name :developer)
      (merge (gui-manifest/gui-registration :developer)
        {:container-predicate developer-container?
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
    (log/debug "Ability Developer GUI initialized (reactive: classic layout + console + skill-tree area)"))))

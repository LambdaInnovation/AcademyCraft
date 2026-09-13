(ns cn.li.ac.tutorial.item
  "Tutorial item (MisakaCloud Terminal) registration and right-click behavior.

  Separated from cn.li.ac.item.components because the tutorial item carries
  tutorial-specific business logic (terminal GUI open, achievement trigger)."
  (:require [cn.li.mcmod.item.dsl :as idsl]
            [cn.li.mcmod.util.log :as log]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.achievement.dispatcher :as achievement-dispatcher]
            [cn.li.ac.terminal.client.actions :as terminal-actions]
            [cn.li.mcmod.runtime.install :as install]))

;; ============================================================================
;; Right-click handler
;; ============================================================================

(def ^:private tutorial-on-right-click
  (fn [event-data]
    (let [{:keys [player side]} event-data]
      ;; Client side: open tutorial GUI (original AC GuiTutorial).
      ;; No raytrace guard needed — Forge's event chain naturally prevents
      ;; double-open: RightClickBlock fires first; if a block handler consumes
      ;; it (AC blocks) or vanilla Block.use succeeds (chests etc.), useItem is
      ;; set to DENY and the item-use callback is not entered.  The tutorial GUI only
      ;; opens when the block interaction is PASS (dirt, air, etc.).
      (when (= side :client)
        (terminal-actions/open-tutorial! player))
      ;; Server side: trigger open_misaka_cloud achievement
      ;; (original AC MSG_TRIGGER → ACAdvancements.trigger)
      (when (= side :server)
        (try
          (achievement-dispatcher/trigger-custom-event! (uuid/player-uuid player) "open_misaka_cloud")
          (catch Throwable e
            (log/warn "Failed to trigger open_misaka_cloud achievement:" (ex-message e))
            (log/stacktrace "Failed to trigger open_misaka_cloud achievement:" e)
            nil)))
      ;; Item IS consumed (matches original AC: EnumActionResult.SUCCESS).
      ;; Returning :consume? true tells the native Item.use() callback to
      ;; return InteractionResult.SUCCESS, which short-circuits the Minecraft
      ;; interaction chain and prevents any block GUI from opening.
      {:consume? true})))

;; ============================================================================
;; Initialization
;; ============================================================================

(defn init-tutorial-item!
  []
  (install/framework-once! ::tutorial-item-installed?
  (fn []
    (idsl/register-item!
      (idsl/create-item-spec
        "tutorial"
        {:max-stack-size 64
         :creative-tab :misc
         :properties {:tooltip ["教程物品"]
                      :model-texture "tutorial"}
         :on-right-click tutorial-on-right-click}))
    (log/debug "Tutorial item (MisakaCloud Terminal) registered"))))

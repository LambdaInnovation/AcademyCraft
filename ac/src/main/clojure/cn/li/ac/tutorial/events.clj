(ns cn.li.ac.tutorial.events
  "Server-side tutorial item event handlers.

  When a player crafts, picks up, or smelts an item, this namespace checks
  whether any tutorial conditions are now met and activates those tutorials.
  Receives ServerPlayer from the platform layer, passes directly to player.clj
  for NBT-based persistence."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.ac.tutorial.auto-give :as auto-give]
            [cn.li.ac.tutorial.player :as tut-player]
            [cn.li.ac.tutorial.registry :as tut-registry]
            [cn.li.ac.tutorial.model :as model]
            [cn.li.ac.tutorial.conditions :as conds]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.hooks.tutorial-events :as tutorial-hooks]
            [cn.li.mcmod.util.log :as log]))

(defn install-tutorial-activated-hook!
  "Register a callback that fires on tutorial activation."
  [hook-fn]
  (tutorial-hooks/register-tutorial-activated-hook! hook-fn))

;; Cache the tutorial-condition-map after first build
(def ^:private tutorial-cond-map*
  (atom nil))

(defn- ensure-tutorial-cond-map!
  []
  (when-not @tutorial-cond-map*
    (reset! tutorial-cond-map*
            (-> (tut-registry/all-tutorials)
                (conds/extend-terminal-conditions)
                (conds/build-tutorial-condition-map))))
  @tutorial-cond-map*)

(defn on-item-event!
  "Called when a player acquires an item (craft/smelt/pickup).
  Args: player — ServerPlayer, item-id, event-type keyword.
  Marks matching condition flags and sets the dirty flag."
  [player item-id event-type]
  (let [matching (conds/find-matching-conditions item-id event-type)]
    (when (seq matching)
      (tut-player/mark-conditions-and-set-dirty! player matching))))

(defn process-pending-activations!
  "Called periodically (every 3 server ticks) to check for new activations.
  Args: player — ServerPlayer."
  [player]
  (when-let [acts (tut-player/process-pending! player (ensure-tutorial-cond-map!))]
    (when (seq acts)
      (let [uuid-str (uuid/player-uuid player)]
        (doseq [tut-id acts]
          (log/debug "Tutorial activated by condition (batched)"
                    {:player uuid-str :tutorial (name tut-id)})
          (try (tutorial-hooks/notify-tutorial-activated! uuid-str tut-id)
               (catch Throwable e (log/warn "Tutorial activation processing failed:" (ex-message e)))))))))

(defn register-platform-handlers!
  "Register tutorial business handlers with the mcmod platform bridge."
  []
  (tutorial-hooks/register-tutorial-handlers!
   {:on-item-event! on-item-event!
    :process-pending-activations! process-pending-activations!})
  (runtime-hooks/register-server-player-login-hook! auto-give/auto-give-on-login!)
  nil)

(ns cn.li.mcmod.gui.container.sync-routing
  "Runtime validation for container-scoped C2S GUI actions."
  (:require [cn.li.mcmod.gui.container-state :as container-state]
            [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.platform.entity :as entity]))

(defn player-open-menu
  [player]
  (try
    (entity/player-get-container-menu player)
    (catch Exception _ nil)))

(defn require-open-container!
  "Resolve the player's open Clojure container from payload :container-id.

  Throws when menu is missing, container-id mismatches, or container cannot be resolved."
  [payload player]
  (registry-contract/require-sync-routing payload)
  (let [expected-id (int (:container-id payload))
        menu (player-open-menu player)]
    (when-not menu
      (throw (ex-info "No open container menu"
                      {:container-id expected-id})))
    (let [actual-id (container-state/get-menu-container-id menu)]
      (when-not (= expected-id actual-id)
        (throw (ex-info "Container id mismatch"
                        {:expected expected-id
                         :actual actual-id})))
      (or (container-state/get-container-for-menu menu)
          (throw (ex-info "Open menu has no Clojure container"
                          {:container-id expected-id}))))))

(defn with-open-container
  "Invoke handler-fn with resolved open container when routing succeeds."
  [payload player handler-fn]
  (let [container (require-open-container! payload player)]
    (handler-fn container payload player)))

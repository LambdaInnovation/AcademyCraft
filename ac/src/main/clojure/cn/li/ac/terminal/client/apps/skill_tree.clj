(ns cn.li.ac.terminal.client.apps.skill-tree
  "CLIENT-ONLY: skill tree terminal app launcher."
  (:require [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.client.platform-bridge :as client-bridge]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.util.log :as log]))

(defn open!
  ([player] (open! player nil))
  ([player learn-context]
   (log/debug "Opening skill tree from terminal for player:" (entity/player-get-name player))
   (client-bridge/open-screen! :ac/skill-tree
                               {:player-uuid (uuid/player-uuid player)
                                :learn-context learn-context})))

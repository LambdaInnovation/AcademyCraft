(ns cn.li.ac.ability.effects.network-handler
  "Effect handler for :network-send effects.

  Marks a player dirty so the platform auto-sync loop pushes the
  updated state to the client on the next tick.

  Effect shape:
    {:effect/type  :network-send
     :player-uuid  string-uuid
     :channel      keyword
     :payload      map}"
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.mcmod.hooks.core :as runtime-hooks]
[cn.li.mcmod.util.log :as log]))

(defn- mark-runtime-dirty-in-session!
  [session-id player-uuid]
  (store/mark-player-dirty! session-id player-uuid))

(defn execute-network-send!
  ([{:keys [player-uuid channel]}]
   (execute-network-send! nil {:player-uuid player-uuid :channel channel}))
  ([session-id {:keys [player-uuid channel]}]
  (when player-uuid
    (mark-runtime-dirty-in-session! (or session-id
                                        (runtime-hooks/require-player-state-session-id "network-send effect"))
                                    player-uuid)
    (log/debug "network-send effect queued sync" player-uuid channel))
   nil))


(ns cn.li.ac.ability.effects.persistence-handler
  "Effect handler for :persist-state effects.

  Marks the player dirty so the platform NBT save cycle serializes
  the updated state.

  Effect shape:
    {:effect/type  :persist-state
     :player-uuid  string-uuid
     :domain       keyword}"
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.mcmod.hooks.core :as runtime-hooks]
[cn.li.mcmod.util.log :as log]))

(defn- mark-runtime-dirty-in-session!
  [session-id player-uuid]
  (store/mark-player-dirty! session-id player-uuid))

(defn execute-persist-state!
  ([{:keys [player-uuid domain]}]
   (execute-persist-state! nil {:player-uuid player-uuid :domain domain}))
  ([session-id {:keys [player-uuid domain]}]
  (when player-uuid
    (mark-runtime-dirty-in-session! (or session-id
                                        (runtime-hooks/require-player-state-session-id "persist-state effect"))
                                    player-uuid)
    (log/debug "persist-state effect queued save" player-uuid domain))
   nil))


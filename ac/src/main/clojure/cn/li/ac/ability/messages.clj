(ns cn.li.ac.ability.messages
  "AC-owned runtime network message catalog.

  Protocol v2 is intentionally incompatible with old clients and servers."
  (:require [cn.li.mcmod.hooks.messages :as message-registry]))

(def message-ids
  {:ctx-begin-link "ability:ctx/begin-link"
   :ctx-establish "ability:ctx/establish"
   :ctx-keepalive "ability:ctx/keepalive"
   :ctx-terminate "ability:ctx/terminate"
   :ctx-terminated "ability:ctx/terminated"
   :ctx-channel "ability:ctx/channel"

   :slot-key-down "ability:skill/key-down"
   :slot-key-tick "ability:skill/key-tick"
   :slot-key-up "ability:skill/key-up"
   :slot-key-abort "ability:skill/key-abort"

   :sync-v2 "ability:sync/runtime-v2"

   :req-learn-node "ability:req/learn-skill"
   :req-level-up "ability:req/level-up"
   :req-portable-dev-start "ability:req/portable-dev-start"
   :req-set-preset "ability:req/set-preset"
   :req-switch-preset "ability:req/switch-preset"
   :req-set-activated "ability:req/set-activated"
   :req-saved-pos-query "ability:req/location-teleport/query"
   :req-saved-pos-add "ability:req/location-teleport/add"
   :req-saved-pos-remove "ability:req/location-teleport/remove"
   :req-saved-pos-perform "ability:req/location-teleport/perform"})

(def MSG-CTX-BEGIN-LINK (:ctx-begin-link message-ids))
(def MSG-CTX-ESTABLISH (:ctx-establish message-ids))
(def MSG-CTX-KEEPALIVE (:ctx-keepalive message-ids))
(def MSG-CTX-TERMINATE (:ctx-terminate message-ids))
(def MSG-CTX-TERMINATED (:ctx-terminated message-ids))
(def MSG-CTX-CHANNEL (:ctx-channel message-ids))

(def MSG-SLOT-KEY-DOWN (:slot-key-down message-ids))
(def MSG-SLOT-KEY-TICK (:slot-key-tick message-ids))
(def MSG-SLOT-KEY-UP (:slot-key-up message-ids))
(def MSG-SLOT-KEY-ABORT (:slot-key-abort message-ids))

(def MSG-SYNC-V2 (:sync-v2 message-ids))

(def MSG-REQ-LEARN-NODE (:req-learn-node message-ids))
(def MSG-REQ-LEVEL-UP (:req-level-up message-ids))
(def MSG-REQ-PORTABLE-DEV-START (:req-portable-dev-start message-ids))
(def MSG-REQ-SET-PRESET (:req-set-preset message-ids))
(def MSG-REQ-SWITCH-PRESET (:req-switch-preset message-ids))
(def MSG-REQ-SET-ACTIVATED (:req-set-activated message-ids))
(def MSG-REQ-SAVED-POS-QUERY (:req-saved-pos-query message-ids))
(def MSG-REQ-SAVED-POS-ADD (:req-saved-pos-add message-ids))
(def MSG-REQ-SAVED-POS-REMOVE (:req-saved-pos-remove message-ids))
(def MSG-REQ-SAVED-POS-PERFORM (:req-saved-pos-perform message-ids))

(def all-messages
  (set (vals message-ids)))

(defn valid-msg-id?
  [message-id]
  (contains? all-messages message-id))

(defn install!
  "Install AC runtime network message ids into the generic mcmod registry."
  []
  (message-registry/register-messages! message-ids))

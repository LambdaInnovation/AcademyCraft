(ns cn.li.ac.ability.client.api
  "Client request API for ability runtime and GUI.

  All requests require an explicit canonical client owner (see mcmod.runtime.owner)."
  (:require [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.runtime.owner :as owner]
            [cn.li.ac.ability.messages :as catalog]))

(defn- require-client-owner! [owner]
  (owner/require-client-owner owner))

(defn req-learn-skill!
  ([owner skill-id callback]
   (req-learn-skill! owner skill-id nil callback))
  ([owner skill-id extra callback]
   (net-client/send-to-server (require-client-owner! owner)
                              catalog/MSG-REQ-LEARN-NODE
                              (merge {:skill-id skill-id} (or extra {}))
                              callback)))

(defn req-level-up! [owner callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-LEVEL-UP {}
                             callback))

(defn req-portable-dev-start!
  "Start a timed development session on the held portable developer.
   action: :learn-skill | :level-up (server turns a category-less :level-up
   into an awaken, matching the block developer)."
  [owner action skill-id callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-PORTABLE-DEV-START
                             (cond-> {:action (name action)}
                               skill-id (assoc :skill-id (name skill-id)))
                             callback))

(defn req-set-activated! [owner activated callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SET-ACTIVATED
                             {:activated (boolean activated)}
                             callback))

(defn req-set-preset-slot!
  [owner preset-idx key-idx cat-id ctrl-id callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SET-PRESET
                             {:preset-idx preset-idx
                              :key-idx key-idx
                              :cat-id cat-id
                              :ctrl-id ctrl-id}
                             callback))

(defn req-switch-preset! [owner preset-idx callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SWITCH-PRESET
                             {:preset-idx preset-idx}
                             callback))

(defn req-location-teleport-query! [owner callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SAVED-POS-QUERY {}
                             callback))

(defn req-location-teleport-add!
  [owner location-name callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SAVED-POS-ADD
                             {:name location-name}
                             callback))

(defn req-location-teleport-remove!
  [owner location-name callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SAVED-POS-REMOVE
                             {:name location-name}
                             callback))

(defn req-location-teleport-perform!
  [owner location-name callback]
  (net-client/send-to-server (require-client-owner! owner)
                             catalog/MSG-REQ-SAVED-POS-PERFORM
                             {:name location-name}
                             callback))

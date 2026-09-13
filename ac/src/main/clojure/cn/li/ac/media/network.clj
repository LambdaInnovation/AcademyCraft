(ns cn.li.ac.media.network
  "Server-side network handler for the Media Player app — the only
  server-authoritative piece is which internal tracks a player has acquired.
  Playback/volume/external-track scanning are entirely client-local, matching
  upstream (media_player volume is a client Forge config, not synced)."
  (:require [cn.li.ac.media.acquire :as acquire]
            [cn.li.mcmod.network.server :as net-server]
            [cn.li.mcmod.util.log :as log]))

;; Message ids are strings: register-handler documents that contract, and the
;; loader transports declare `^String msg-id` (ClojureNetwork/sendToServer), so
;; an integer id threw ClassCastException before it ever reached the wire.
(def media-get-state-msg "media:get-state")

(defn- media->wire
  [media]
  {:id (name (:id media))
   :name (:name media)
   :desc (:desc media)
   :external? (boolean (:external? media))})

(defn- handle-get-state
  [_payload player]
  (try
    {:success true
     :medias (mapv media->wire (acquire/installed-medias player))}
    (catch Throwable e
      (log/stacktrace "Error in media get-state handler" e)
      {:success false :error (ex-message e)})))

(defn register-handlers!
  []
  ;; Media player state is player-scoped, not an open container GUI session.
  (net-server/register-handler media-get-state-msg handle-get-state
                               {:owner-spec :server :payload-routing :none})
  (log/debug "Media player network handlers registered"))

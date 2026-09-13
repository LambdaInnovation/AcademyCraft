(ns cn.li.ac.ability.fx
  "Unified server-side ability FX transport.

  Skill specs declare:
    :fx {:start   {:topic kw :mode :start :payload (fn [evt] map) :to :client}
         :update  {:topic kw ...}
         :perform {:topic kw ...}
         :end     {:topic kw ...}}

  Hand-written skill callbacks call `send!` with the same entry shape."
  (:require [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.mcmod.util.log :as log]))

(defn- safe-payload [payload-fn evt]
  (try
    (let [p (when (fn? payload-fn) (payload-fn evt))]
      (when (map? p) p))
    (catch Exception e
      (log/warn "FX payload fn failed" (ex-message e))
      nil)))

(defn- dispatch!
  [ctx-id topic body to]
  (case (or to :client)
    :client (ctx/ctx-send-to-client! ctx-id topic body)
    :self (ctx/ctx-send-to-self! ctx-id topic body)
    :except-local (ctx/ctx-send-to-except-local! ctx-id topic body)
    (ctx/ctx-send-to-client! ctx-id topic body)))

(defn send!
  "Send one ability FX event.

  `fx-entry` — `{:topic kw :mode kw :to :client|:self|:except-local :payload fn?}`
  `evt`      — optional context event map for meta/payload-fn
  `payload`  — optional map merged last"
  ([ctx-id fx-entry]
   (send! ctx-id fx-entry nil nil))
  ([ctx-id fx-entry evt]
   (send! ctx-id fx-entry evt nil))
  ([ctx-id fx-entry evt payload]
   (when (and (map? fx-entry) (keyword? (:topic fx-entry)))
     (let [ctx-id* (or ctx-id (:ctx-id evt))
           entry-payload (safe-payload (:payload fx-entry) evt)
           extra (when (map? payload) payload)
           mode (or (:mode extra) (:mode fx-entry) (:mode entry-payload))
           base (cond-> {:ctx-id ctx-id*}
                   mode (assoc :mode mode)
                   (:skill-id evt) (assoc :skill-id (:skill-id evt))
                   (:player-id evt) (assoc :player-id (:player-id evt)))
           body (merge base entry-payload extra)]
       (dispatch! ctx-id* (:topic fx-entry) body (:to fx-entry))))
   nil))

(defn send-local-and-nearby!
  "Fan-out one ability FX to the owning client and nearby players (except local).

  Replaces per-skill duplicate `send-fx-to-local-and-nearby!` helpers."
  [ctx-id fx-entry evt payload]
  (send! ctx-id (assoc fx-entry :to :client) evt payload)
  (send! ctx-id (assoc fx-entry :to :except-local) evt payload))

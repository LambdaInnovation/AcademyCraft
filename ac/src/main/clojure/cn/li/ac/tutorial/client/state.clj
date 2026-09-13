(ns cn.li.ac.tutorial.client.state
  "CLIENT-ONLY: Tutorial state cache — lightweight atom holding the player's
  tutorial activation state on the client side.

  State lives in Framework [:service :tutorial-client :by-session <session>],
  session-keyed via runtime-hooks/default-client-owner so state from one
  world/server session can never leak into a different session loaded later
  in the same client JVM (see docs/dev/AGENT_AND_TOOLING.md P5)."
  (:require [cn.li.ac.tutorial.messages :as tut-msg]
            [cn.li.ac.tutorial.client.notification :as notification]
            [cn.li.mcmod.network.client :as net-client]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.util.log :as log]
            [clojure.set]))

;; --- State atom ---

(def ^:private default-client-state
  {:activated-tuts #{}       ; set of activated tutorial ids
   :misaka-id      nil       ; int or nil
   :first-open?    true      ; first-open animation flag
   :ready?         false})   ; true after first sync

(def ^:private state-path [:service :tutorial-client :by-session])
(def ^:private sync-throttle-path [:service :tutorial-client :last-sync-ms])

(defn- session-key []
  (:client-session-id (runtime-hooks/default-client-owner)))

(defn- client-state-atom []
  (let [fw-atom (or (fw/fw-atom)
                     (throw (ex-info "Tutorial client-state accessed before framework injection" {})))
        path (conj state-path (session-key))]
    (or (get-in @fw-atom path)
        (let [a (atom default-client-state)]
          (swap! fw-atom assoc-in path a)
          a))))

(declare apply-sync!)

;; --- Public API ---

(defn ensure-client-state!
  "Ensure the client state atom is initialized.  Idempotent."
  [_player-uuid]
  (let [state-atom (client-state-atom)]
    (when-not (:ready? @state-atom)
      (reset! state-atom default-client-state)))
  nil)

(defn request-sync!
  "Send a :tutorial/request-sync message to the server.
  `owner` is the owner map used by the network layer (from client-side runtime)."
  [owner]
  (net-client/send-to-server
    owner
    (tut-msg/msg-id :tutorial/request-sync)
    {}
    (fn [response]
      (when response
        (apply-sync! response))))
  nil)

(defn apply-sync!
  "Update the client state cache from a server sync response.
  Detects newly activated tutorials and shows a toast notification
  (matching upstream NotifyUI behavior)."
  [data]
  (let [state-atom (client-state-atom)
        old-activated (:activated-tuts @state-atom)
        new-activated (set (:activated-tuts data))
        new-ids (clojure.set/difference new-activated old-activated)]
    (swap! state-atom merge
           {:activated-tuts new-activated
            :misaka-id      (:misaka-id data)
            :first-open?    (boolean (:first-open? data))
            :ready?         true})
    (log/debug "Tutorial client state synced"
               {:activated (count new-activated)
                :misaka (:misaka-id data)})
    ;; Show toast for newly activated tutorials (matching upstream NotifyUI)
    (when (and (:ready? @state-atom) (seq new-ids))
      (notification/show-activation-toasts! new-ids)))
  nil)

(defn is-activated?
  "Check if a tutorial is activated from the client cache."
  [_player-uuid tut-id]
  (contains? (:activated-tuts @(client-state-atom)) (keyword tut-id)))

(defn get-misaka-id
  "Return the player's Misaka No. from client cache, or a deterministic local
  fallback (based on UUID hash) so the tutorial always shows a real number
  even before the first server sync completes."
  [player-uuid]
  (or (:misaka-id @(client-state-atom))
      (let [h (mod (Math/abs (long (hash (str player-uuid)))) 18001)]
        (+ 1000 h))))

(defn first-open?
  "Return the first-open flag from the client cache."
  [_player-uuid]
  (:first-open? @(client-state-atom)))

(defn ready?
  "True after at least one successful sync."
  []
  (:ready? @(client-state-atom)))

;; --- Periodic background sync ---
;; Keeps client state current without requiring the GUI to be open,
;; enabling real-time activation notifications matching upstream NotifyUI.

(def ^:private sync-interval-ms 5000)

(defn tick-background-sync!
  "Call this from the client tick handler.  Sends a nil-owner request-sync
  every sync-interval-ms (default 5s) to keep tutorial state current.
  Activation notifications are shown automatically when apply-sync! detects
  newly activated tutorial ids."
  []
  (try
    (when-let [fw-atom (fw/fw-atom)]
      (when-let [sk (session-key)]
        (let [now (System/currentTimeMillis)
              throttle-path (conj sync-throttle-path sk)]
          (when (>= (- now (get-in @fw-atom throttle-path 0)) sync-interval-ms)
            (swap! fw-atom assoc-in throttle-path now)
            (net-client/send-to-server
              (runtime-hooks/default-client-owner)
              (tut-msg/msg-id :tutorial/request-sync)
              {}
              (fn [response]
                (when response
                  (apply-sync! response))))))))
    (catch Throwable _
      nil)))

(defn reset-state!
  "Reset client state to default.  For test isolation only."
  []
  (reset! (client-state-atom) default-client-state)
  nil)

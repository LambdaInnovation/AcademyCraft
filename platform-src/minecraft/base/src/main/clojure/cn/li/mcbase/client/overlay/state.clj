(ns cn.li.mcbase.client.overlay.state
  (:require [cn.li.mcbase.client.session :as client-session]))

(defn create-overlay-state-runtime
  []
  {::runtime ::overlay-state-runtime
   :client-activated-overlay* (atom {})
   :active-overlay-app* (atom {})})

(def ^:private overlay-state-runtime-atom (atom (create-overlay-state-runtime)))

(defn- overlay-state-runtime?
  [runtime]
  (and (map? runtime)
       (= ::overlay-state-runtime (::runtime runtime))
       (some? (:client-activated-overlay* runtime))))

(defn call-with-overlay-state-runtime
  "Set the overlay state runtime for the current context (primarily for testing)."
  [runtime f]
  (when-not (overlay-state-runtime? runtime)
    (throw (ex-info "Expected overlay state runtime"
                    {:runtime runtime})))
  (let [saved @overlay-state-runtime-atom]
    (try
      (reset! overlay-state-runtime-atom runtime)
      (f)
      (finally
        (reset! overlay-state-runtime-atom saved)))))

(defmacro with-overlay-state-runtime
  [runtime & body]
  `(call-with-overlay-state-runtime ~runtime (fn [] ~@body)))

(defn- current-overlay-state-runtime
  []
  @overlay-state-runtime-atom)

(defn- client-activated-overlay-atom
  []
  (:client-activated-overlay* (current-overlay-state-runtime)))

(defn get-client-activated
  [owner]
  (get @(client-activated-overlay-atom) (client-session/owner-key owner)))

(defn set-client-activated!
  [owner v]
  (swap! (client-activated-overlay-atom) assoc (client-session/owner-key owner) (boolean v))
  nil)

(defn clear-client-activated!
  [owner]
  (swap! (client-activated-overlay-atom) dissoc (client-session/owner-key owner))
  nil)

;; ============================================================================
;; Overlay App Switching (passOn equivalent)
;; ============================================================================

(defn- active-overlay-app-atom
  []
  (:active-overlay-app* (current-overlay-state-runtime)))

(defn get-active-overlay-app
  "Return the currently active overlay app for owner, or nil."
  [owner]
  (get @(active-overlay-app-atom) (client-session/owner-key owner)))

(defn set-active-overlay-app!
  "Set the active overlay app for owner. app-kw should be a keyword like :freq-tx."
  [owner app-kw]
  (swap! (active-overlay-app-atom) assoc (client-session/owner-key owner) app-kw)
  nil)

(defn clear-active-overlay-app!
  "Clear the active overlay app for owner, returning to normal HUD."
  [owner]
  (swap! (active-overlay-app-atom) dissoc (client-session/owner-key owner))
  nil)

;; ============================================================================
;; Session cleanup
;; ============================================================================

(defn clear-client-overlay-session!
  [client-session-id]
  (swap! (client-activated-overlay-atom)
         (fn [states]
           (into {}
                 (remove (fn [[[entry-session-id _player-uuid] _value]]
                           (= client-session-id entry-session-id))
                         states))))
  (swap! (active-overlay-app-atom)
         (fn [states]
           (into {}
                 (remove (fn [[[entry-session-id _player-uuid] _value]]
                           (= client-session-id entry-session-id))
                         states))))
  nil)

(defn overlay-state-snapshot
  []
  @(client-activated-overlay-atom))

(defn reset-client-activated-for-test!
  ([]
   (reset-client-activated-for-test! {}))
  ([snapshot]
   (reset! (client-activated-overlay-atom) (or snapshot {}))
   (reset! (active-overlay-app-atom) {})
   nil))

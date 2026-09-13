(ns cn.li.mcmod.network.server
  "Server-side RPC handler registry for GUI/network logic"
  (:require [cn.li.mcmod.gui.registry-contract :as registry-contract]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]))

(definterface INetworkServerRuntime
  (handlersMap [])
  (^boolean frozenState [])
  (setFrozen [^boolean value]))

(deftype NetworkServerRuntime [^HashMap handlers
                               ^:unsynchronized-mutable ^boolean frozen]
  INetworkServerRuntime
  (handlersMap [_] handlers)
  (frozenState [_] frozen)
  (setFrozen [_ value] (set! frozen value)))

(defn create-network-server-runtime []
  (NetworkServerRuntime. (HashMap.) false))

(defonce ^:private runtime-slot
  (object-array [(create-network-server-runtime)]))

(defn- current-runtime ^NetworkServerRuntime []
  (aget ^objects runtime-slot 0))

(defn call-with-network-server-runtime [runtime thunk]
  (let [previous (aget ^objects runtime-slot 0)]
    (aset ^objects runtime-slot 0 runtime)
    (try (thunk)
         (finally (aset ^objects runtime-slot 0 previous)))))

(defn- assert-not-frozen!
  []
  (when (.frozenState ^INetworkServerRuntime (current-runtime))
    (throw (ex-info "Network server handlers are frozen" {}))))

(defn register-handler
  "Register a request handler with an explicit registry contract.

  Args:
  - msg-id: string message identifier
  - handler-fn: (fn [payload player] response-map)
  - contract: optional map with :owner-spec and :payload-routing"
  ([msg-id handler-fn]
   (register-handler msg-id handler-fn (registry-contract/default-server-gui-handler-contract)))
  ([msg-id handler-fn contract]
   (registry-contract/validate-handler-fn! msg-id handler-fn)
   (let [entry (registry-contract/handler-entry handler-fn contract)]
     (assert-not-frozen!)
     (let [^HashMap handlers (.handlersMap ^INetworkServerRuntime (current-runtime))]
       (if-let [existing (.get handlers msg-id)]
         (when-not (and (identical? (registry-contract/registered-handler-fn existing) handler-fn)
                        (registry-contract/contracts-compatible?
                         (registry-contract/registered-handler-contract existing)
                         (:contract entry)))
           (throw (ex-info "Conflicting network handler id"
                           {:msg-id msg-id
                            :existing (registry-contract/registered-handler-contract existing)
                            :new (:contract entry)})))
         (.put handlers msg-id entry)))
     (log/debug "Registered network handler for" msg-id
                "owner-spec=" (:owner-spec (:contract entry)))
     nil)))

(defn freeze-handlers!
  []
  (.setFrozen ^INetworkServerRuntime (current-runtime) true)
  nil)

(defn reset-handlers-for-test!
  []
  (.clear ^HashMap (.handlersMap ^INetworkServerRuntime (current-runtime)))
  (.setFrozen ^INetworkServerRuntime (current-runtime) false)
  nil)

(defn handlers-snapshot
  []
  (let [runtime (current-runtime)]
    {:handlers (into {} (.handlersMap ^INetworkServerRuntime runtime))
     :frozen? (.frozenState ^INetworkServerRuntime runtime)}))

(defn- validate-payload-routing!
  [contract payload]
  (when (= :sync-routing (:payload-routing contract))
    (registry-contract/require-sync-routing payload))
  payload)

(defn handle-request
  "Handle an incoming request and send a response if needed.

  Args:
  - msg-id: string message identifier
  - request-id: int
  - payload: map
  - player: player entity
  - respond-fn: (fn [request-id response-map]) or nil"
  [msg-id request-id payload player respond-fn]
  (if-let [entry (.get ^HashMap (.handlersMap ^INetworkServerRuntime (current-runtime)) msg-id)]
    (let [handler (registry-contract/registered-handler-fn entry)
          contract (registry-contract/registered-handler-contract entry)]
      (try
        (let [payload* (validate-payload-routing! contract payload)
              response (handler payload* player)]
          (when (and respond-fn (>= request-id 0))
            (respond-fn request-id (or response {}))))
        (catch Exception e
          (log/stacktrace (str "Error handling request " msg-id) e)
          (log/stacktrace "Error handling request" e)
          (when (and respond-fn (>= request-id 0))
            (respond-fn request-id {:success false :error (ex-message e)})))))
    (do
      (log/debug "No handler registered for" msg-id)
      (when (and respond-fn (>= request-id 0))
        (respond-fn request-id {:success false :error "no-handler"})))))

(ns cn.li.ac.ability.service.context-transport
  "Transport port for ability context server/client messaging.

  Owns send-function registration and provides transport operations used by
  lifecycle/runtime services. Runtime access is direct and allocation-free."
  )

(defn default-context-transport-runtime-state []
  {:to-client nil :to-server nil :frozen? false})

(definterface IContextTransportRuntime
  (toClient [])
  (toServer [])
  (^boolean frozenState [])
  (setToClient [value])
  (setToServer [value])
  (setFrozen [^boolean value]))

(deftype ContextTransportRuntime [^:unsynchronized-mutable to-client
                                  ^:unsynchronized-mutable to-server
                                  ^:unsynchronized-mutable ^boolean frozen]
  IContextTransportRuntime
  (toClient [_] to-client)
  (toServer [_] to-server)
  (frozenState [_] frozen)
  (setToClient [_ value] (set! to-client value))
  (setToServer [_ value] (set! to-server value))
  (setFrozen [_ value] (set! frozen value)))

(defonce ^:private ^ContextTransportRuntime runtime
  (ContextTransportRuntime. nil nil false))

(defn- assert-transport-open! []
  (when (.frozenState ^IContextTransportRuntime runtime)
    (throw (ex-info "Context transport is frozen" {}))))

(defn context-transport-snapshot []
  {:to-client (.toClient ^IContextTransportRuntime runtime)
   :to-server (.toServer ^IContextTransportRuntime runtime)
   :frozen? (.frozenState ^IContextTransportRuntime runtime)})

(defn reset-context-transport-for-test!
  ([] (reset-context-transport-for-test! {}))
  ([{:keys [to-client to-server frozen?] :or {to-client nil to-server nil frozen? false}}]
   (.setToClient ^IContextTransportRuntime runtime to-client)
   (.setToServer ^IContextTransportRuntime runtime to-server)
   (.setFrozen ^IContextTransportRuntime runtime (boolean frozen?))
   nil))

(defn freeze-context-transport! [] (.setFrozen ^IContextTransportRuntime runtime true) nil)

(defn register-send-fns! [{:keys [to-client to-server]}]
  (assert-transport-open!)
  (.setToClient ^IContextTransportRuntime runtime to-client)
  (.setToServer ^IContextTransportRuntime runtime to-server)
  nil)

(defn send-to-client! [player-uuid msg-id payload]
  (when-let [f (.toClient ^IContextTransportRuntime runtime)] (f player-uuid msg-id payload)))

(defn send-to-server! [msg-id payload]
  (when-let [f (.toServer ^IContextTransportRuntime runtime)] (f msg-id payload)))

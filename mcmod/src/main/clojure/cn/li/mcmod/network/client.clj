(ns cn.li.mcmod.network.client
  "Client-side RPC request/response support for GUI/network logic"
  (:require [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.platform.dispatch :as platform-dispatch]
            [cn.li.mcmod.runtime.owner :as runtime-owner]
            [cn.li.mcmod.util.log :as log])
  (:import [java.util HashMap]))

(def ^:private CLIENT-RPC-CHANNEL :client-rpc)
(def ^:private DEFAULT-REQUEST-TIMEOUT-MS 30000)

(defn- initial-client-network-session-state
  []
  {:request-counter {}
   :pending-requests {}
   :push-handlers {}})

(defn- require-owner-value
  [owner label value]
  (if (some? value)
    value
    (throw (ex-info (format "Client network owner requires %s" label)
                    {:owner owner
                     :required label}))))

(defn- client-session-id
  [owner]
  (if (some? owner)
    (:client-session-id (runtime-owner/require-client-owner owner))
    (require-owner-value owner ":client-session-id"
                         (runtime-hooks/client-session-id))))

(defn- maybe-client-session-id
  [owner]
  (or (some-> owner :client-session-id)
      (runtime-hooks/client-session-id)))

(defn- client-channel-id
  "RPC channel for pending-request / push scoping.

  Only `:channel-id` participates — not `:screen-id`. S2C responses rebuild
  the owner via with-client-response-owner without screen-id, so keys that
  include screen-id never match (terminal Left-Alt: 'No pending request')."
  [owner]
  (or (:channel-id owner)
      CLIENT-RPC-CHANNEL))

(defn client-owner-key
  [owner]
  [(client-session-id owner)
   (client-channel-id owner)])

(defn- push-owner-key
  [owner]
  (client-owner-key owner))

(defn- now-ms []
  (System/currentTimeMillis))

(defn- request-key [owner-key request-id]
  ;; Normalize the id type: registration stores Long counters while platform
  ;; response decoding hands back Integer. Pending entries live in a
  ;; java.util.HashMap whose vector keys compare elements with .equals,
  ;; and Long(2).equals(Integer(2)) is false — without (long ...) every
  ;; response lookup misses ("No pending request for response N").
  [owner-key (long request-id)])

(defn create-client-network-session
  ([]
   (create-client-network-session {}))
  ([{:keys [now-ms-fn]
     :or {now-ms-fn now-ms}}]
   (let [^HashMap request-counter (HashMap.)
         ^HashMap pending-requests (HashMap.)
         ^HashMap push-handlers (HashMap.)]
     (letfn [(snapshot []
               {:request-counter (into {} request-counter)
                :pending-requests (into {} pending-requests)
                :push-handlers (into {} push-handlers)})
             (dispose! []
               (.clear request-counter)
               (.clear pending-requests)
               (.clear push-handlers)
               nil)
             (next-request-id! [owner-key]
               (let [next-id (unchecked-inc (long (or (.get request-counter owner-key) 0)))]
                 (.put request-counter owner-key next-id)
                 next-id))
             (register-callback! [owner-key request-id callback timeout-ms]
               (when callback
                 (.put pending-requests (request-key owner-key request-id)
                       {:owner-key owner-key
                        :request-id request-id
                        :callback callback
                        :created-at-ms (now-ms-fn)
                        :timeout-ms (long (or timeout-ms DEFAULT-REQUEST-TIMEOUT-MS))}))
               nil)
             (find-pending-request-entry [owner-key request-id]
               (let [key (request-key owner-key request-id)]
                 (when-let [entry (.get pending-requests key)]
                   [key entry])))
             (clear-owner! [owner-key]
               (let [pending-it (.iterator (.keySet pending-requests))]
                 (while (.hasNext pending-it)
                   (let [[entry-owner _request-id] (.next pending-it)]
                     (when (= owner-key entry-owner) (.remove pending-it)))))
               (let [push-it (.iterator (.keySet push-handlers))]
                 (while (.hasNext push-it)
                   (let [[handler-owner _msg-id] (.next push-it)]
                     (when (= owner-key handler-owner) (.remove push-it)))))
               (.remove request-counter owner-key)
               nil)
             (expire-pending! [current-ms]
               (let [expired (transient [])
                     iterator (.iterator (.entrySet pending-requests))]
                 (while (.hasNext iterator)
                   (let [entry (.next iterator)
                         key (.getKey ^java.util.Map$Entry entry)
                         {:keys [created-at-ms timeout-ms]} (.getValue ^java.util.Map$Entry entry)]
                     (when (>= (- (long current-ms) (long created-at-ms)) (long timeout-ms))
                       (conj! expired key)
                       (.remove iterator))))
                 (persistent! expired)))
             (handle-response! [owner-key request-id response]
               (if-let [[pending-key {:keys [callback]}] (find-pending-request-entry owner-key request-id)]
                 (do
                   (.remove pending-requests pending-key)
                   (try
                     (callback response)
                     (catch Exception e
                       (log/stacktrace (str "Error in response callback (request-id=" request-id ")") e)
                       (log/stacktrace "Error in response callback" e))))
                 (log/debug "No pending request for response" request-id))
               nil)
             (register-owner-push-handler! [owner-key msg-id handler-fn]
               (.put push-handlers [owner-key msg-id] handler-fn)
               nil)
             (unregister-owner-push-handler! [owner-key msg-id]
               (.remove push-handlers [owner-key msg-id])
               nil)
             (has-owner-push-handler-msg-id? [msg-id]
               (let [iterator (.iterator (.keySet push-handlers))]
                 (loop []
                   (if (.hasNext iterator)
                     (let [[_owner-key entry-msg-id] (.next iterator)]
                       (if (= msg-id entry-msg-id) true (recur)))
                     false))))
             (handle-owner-push! [owner-key msg-id payload]
               (if-let [handler (.get push-handlers [owner-key msg-id])]
                 (try
                   (handler payload)
                   true
                   (catch Exception e
                     (log/stacktrace (str "Error in push handler " msg-id) e)
                     (log/stacktrace "Error in push handler" e)
                     true))
                 false))]
       {:kind ::client-network-session
        :snapshot snapshot
        :dispose! dispose!
        :next-request-id! next-request-id!
        :register-callback! register-callback!
        :clear-owner! clear-owner!
        :expire-pending! expire-pending!
        :handle-response! handle-response!
        :register-owner-push-handler! register-owner-push-handler!
        :unregister-owner-push-handler! unregister-owner-push-handler!
        :has-owner-push-handler-msg-id? has-owner-push-handler-msg-id?
        :handle-owner-push! handle-owner-push!}))))

(defn client-network-session?
  [value]
  (= ::client-network-session (:kind value)))

(defn create-client-runtime
  ([]
   (create-client-runtime {}))
  ([{:keys [create-session-fn]
     :or {create-session-fn create-client-network-session}}]
   (let [^HashMap global-push-handlers (HashMap.)
         ^HashMap installed-sessions (HashMap.)]
     (letfn [(snapshot []
               {:global-push-handlers (into {} global-push-handlers)
                :installed-sessions (into {} installed-sessions)})
             (global-push-handler [msg-id]
               (.get global-push-handlers msg-id))
             (register-global-push-handler! [msg-id handler-fn]
               (.put global-push-handlers msg-id handler-fn)
               nil)
             (unregister-global-push-handler! [msg-id]
               (.remove global-push-handlers msg-id)
               nil)
             (installed-client-network-session [session-id]
               (.get installed-sessions session-id))
             (ensure-installed-client-network-session! [session-id]
               (or (.get installed-sessions session-id)
                   (let [created (create-session-fn)]
                     (.put installed-sessions session-id created)
                     created)))
             (clear-client-session-state! [session-id]
               (when-let [session (installed-client-network-session session-id)]
                 ((:dispose! session)))
               (.remove installed-sessions session-id)
               nil)
             (dispose! []
               (doseq [session (.values installed-sessions)]
                 ((:dispose! session)))
               (.clear installed-sessions)
               (.clear global-push-handlers)
               nil)]
       {:kind ::client-runtime
        :snapshot snapshot
        :installed-sessions-values #(.values installed-sessions)
        :dispose! dispose!
        :global-push-handler global-push-handler
        :register-global-push-handler! register-global-push-handler!
        :unregister-global-push-handler! unregister-global-push-handler!
        :installed-client-network-session installed-client-network-session
        :ensure-installed-client-network-session! ensure-installed-client-network-session!
        :clear-client-session-state! clear-client-session-state!}))))

(defonce ^:private client-runtime-slot
  (object-array [(create-client-runtime)]))

(defn- ensure-client-runtime [] (aget ^objects client-runtime-slot 0))

(defn call-with-client-runtime
  "Run thunk with an explicit client-thread runtime. Used by lifecycle tests
  and integrated-server session boundaries; it does not create a ThreadLocal."
  [runtime thunk]
  (let [previous (aget ^objects client-runtime-slot 0)]
    (aset ^objects client-runtime-slot 0 runtime)
    (try
      (thunk)
      (finally
        (aset ^objects client-runtime-slot 0 previous)))))

(defn client-runtime?
  [value]
  (= ::client-runtime (:kind value)))

(defn current-client-runtime
  []
  (ensure-client-runtime))

(defn client-runtime-state-snapshot
  []
  ((:snapshot (current-client-runtime))))

(declare unregister-push-handler!)

(defn- owner-client-network-session
  [owner]
  (when-let [session (:client-network-session (or owner {}))]
    (if (client-network-session? session)
      session
      (throw (ex-info "Client network owner provided invalid :client-network-session"
                      {:owner owner
                       :value session})))))

(defn- installed-client-network-session
  [session-id]
  ((:installed-client-network-session (current-client-runtime)) session-id))

(defn- ensure-installed-client-network-session!
  [session-id]
  ((:ensure-installed-client-network-session! (current-client-runtime)) session-id))

(defn- resolve-client-network-session
  ([owner]
   (resolve-client-network-session owner {}))
  ([owner {:keys [allow-install?]
           :or {allow-install? false}}]
   (or (owner-client-network-session owner)
       (when-let [session-id (maybe-client-session-id owner)]
         (if allow-install?
           (ensure-installed-client-network-session! session-id)
           (installed-client-network-session session-id))))))

(defn- runtime-global-push-handler
  [msg-id]
  ((:global-push-handler (current-client-runtime)) msg-id))

(defn clear-owner-state!
  [owner]
  (let [owner-key (client-owner-key owner)]
    (when-let [session (resolve-client-network-session owner {:allow-install? false})]
      ((:clear-owner! session) owner-key))
    nil))

(defn clear-client-session-state!
  [client-session-id]
  ((:clear-client-session-state! (current-client-runtime)) client-session-id))

(defn expire-pending-requests!
  ([]
   (expire-pending-requests! (now-ms)))
  ([current-ms]
   (vec (mapcat #((:expire-pending! %) current-ms)
                ((:installed-sessions-values (current-client-runtime))))))
  ([session-or-owner current-ms]
   (let [session (if (client-network-session? session-or-owner)
                   session-or-owner
                   (resolve-client-network-session session-or-owner {:allow-install? false}))]
     (if session
       (vec ((:expire-pending! session) current-ms))
       []))))

(defn reset-client-state-for-test!
  []
  ((:dispose! (current-client-runtime)))
  nil)

(defn client-state-snapshot
  ([]
   (client-state-snapshot nil))
  ([session-or-owner]
   (let [session (if (client-network-session? session-or-owner)
                   session-or-owner
                   (resolve-client-network-session session-or-owner {:allow-install? false}))
         session-snapshot (if session
                            ((:snapshot session))
                            (initial-client-network-session-state))
         {:keys [global-push-handlers installed-sessions]} (client-runtime-state-snapshot)]
     (assoc session-snapshot
            :global-push-handlers global-push-handlers
            :installed-sessions (vec (keys installed-sessions))))))

(defn handle-response
  "Handle a response from server.

  Args:
  - request-id: int
  - response: map returned by server handler"
  ([request-id response]
   (handle-response nil request-id response))
  ([owner request-id response]
   (let [owner-key (client-owner-key owner)]
     (if-let [session (resolve-client-network-session owner {:allow-install? false})]
       ((:handle-response! session) owner-key request-id response)
       (log/debug "No pending request for response" request-id)))))

(defn register-push-handler!
  "Register one-way client push handler.

  Args:
  - msg-id: string message identifier
  - handler-fn: (fn [payload])"
  ([msg-id handler-fn]
   ((:register-global-push-handler! (current-client-runtime)) msg-id handler-fn)
   (fn []
     (unregister-push-handler! msg-id)))
  ([owner msg-id handler-fn]
   (let [session (resolve-client-network-session owner {:allow-install? true})
         owner-key (push-owner-key owner)]
     ((:register-owner-push-handler! session) owner-key msg-id handler-fn)
     (fn []
       (unregister-push-handler! owner msg-id)))))

(defn unregister-push-handler!
  ([msg-id]
   ((:unregister-global-push-handler! (current-client-runtime)) msg-id))
  ([owner msg-id]
   (when-let [session (resolve-client-network-session owner {:allow-install? false})]
     ((:unregister-owner-push-handler! session) (push-owner-key owner) msg-id))
   nil))

(defn- owner-requires-scoped-push-handler?
  [owner]
  (or (:screen-id owner)
      (:channel-id owner)))

(defn handle-push
  "Handle one-way server push message.

  Args:
  - msg-id: string message identifier
  - payload: map"
  ([msg-id payload]
   (if-let [handler (runtime-global-push-handler msg-id)]
     (try
       (handler payload)
       (catch Exception e
         (log/stacktrace (str "Error in push handler " msg-id) e)
         (log/stacktrace "Error in push handler" e)))
     (log/debug "No push handler registered for" msg-id)))
  ([owner msg-id payload]
   (if-let [session (resolve-client-network-session owner {:allow-install? false})]
     (if ((:handle-owner-push! session) (push-owner-key owner) msg-id payload)
       nil
       (if (or (owner-requires-scoped-push-handler? owner)
               ((:has-owner-push-handler-msg-id? session) msg-id))
         (log/debug "No push handler registered for" msg-id)
         (handle-push msg-id payload)))
     (if (owner-requires-scoped-push-handler? owner)
       (log/debug "No push handler registered for" msg-id)
       (handle-push msg-id payload)))))

(defonce ^:private ^HashMap request-transports (HashMap.))

(defn register-request-transport!
  "Install one frozen loader transport during startup."
  [platform-version transport-fn]
  (.put request-transports platform-version transport-fn)
  nil)

(defn send-request
  [msg-id payload request-id]
  (let [version (platform-dispatch/current-platform-version)]
    (if-let [transport (.get request-transports version)]
      (transport msg-id payload request-id)
      (throw (ex-info "No network transport for version" {:version version})))))

(defn send-to-server
  "Send a request to server and optionally receive a response.

  Args:
  - msg-id: string message identifier
  - payload: map of data
  - callback: (fn [response]) or nil"
  ([msg-id payload]
   (send-to-server msg-id payload nil))
  ([msg-id payload callback]
   (send-to-server nil msg-id payload callback))
  ([owner msg-id payload callback]
   (let [owner* (when owner (runtime-owner/require-client-owner owner))
         session (resolve-client-network-session owner* {:allow-install? true})
         owner-key (client-owner-key owner*)
         request-id (if callback
                      ((:next-request-id! session) owner-key)
                      -1)]
     ((:register-callback! session) owner-key request-id callback (when (map? owner*) (:timeout-ms owner*)))
     (send-request msg-id payload request-id))))

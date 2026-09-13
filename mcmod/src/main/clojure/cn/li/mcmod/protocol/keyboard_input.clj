(ns cn.li.mcmod.protocol.keyboard-input
  "Universal keyboard input protocol (platform-independent).

   This namespace provides the core keybinding registration and dispatch mechanism
   that is shared across all platforms (Forge, Fabric, etc)."
  (:require [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.registry :as registry]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.util.log :as log]))

(defn register-input-id!
  "Register an input ID and its handler function.

   Args:
   - input-id: keyword (e.g., :content/slot-0, :content/toggle-primary-state)
   - handler-fn: function or symbol
     - Signature: (fn [context] ...)
     - context := {:player-uuid string
     -             :client-session-id string
     -             :logical-side :client}

   Called during AC bootstrap to register all available inputs."
  [input-id handler-fn]
  (assert (keyword? input-id) "input-id must be a keyword")
  (assert (or (fn? handler-fn) (symbol? handler-fn) (var? handler-fn))
          "handler-fn must be a function, symbol, or var")
  (registry/register! (fw/fw-atom) :input input-id handler-fn)
  (log/debug "Registered input handler" {:input-id input-id})
  nil)

(defn get-input-id-handler
  "Query the registered handler for an input ID.

   Returns: function or symbol, or nil if not registered"
  [input-id]
  (when-let [fw-atom (fw/fw-atom)]
    (registry/get-spec fw-atom :input input-id)))

(defn get-all-input-ids
  "Get all registered input IDs (as a sequence of keywords)"
  []
  (when-let [fw-atom (fw/fw-atom)]
    (keys (get-in @fw-atom [:registry :input]))))

(defn- call-with-input-ctx
  "Rebuild the per-thread client ctx from the input context before dispatch.

   Input dispatch is a context boundary (hooks.core 调用规范 #2/#3): platform
   event threads carry no bound ctx, so handlers that resolve the session via
   runtime-hooks/client-session-id would otherwise read nil even though the
   context map itself carries :client-session-id."
  [{:keys [client-session-id player-uuid]} thunk]
  (if (some? client-session-id)
    (runtime-hooks/with-client-ctx-fn
      {:session-id client-session-id
       :player-owner (cond-> {:logical-side :client
                              :client-session-id client-session-id}
                       player-uuid (assoc :player-uuid (str player-uuid)))}
      thunk)
    (thunk)))

(defn emit-keyboard-input!
  "Dispatch a keyboard input event to its registered handler.

   Invocation chain:
   1. Platform polling / Forge-Fabric event routing decide the gesture timing
      (e.g. short press vs. long hold)
   2. emit-keyboard-input! (this function) dispatches the filtered event
   3. AC handler execution consumes the input and runs business logic

   Args:
   - input-id: keyword identifying the input
   - context: map with :player-uuid, :client-session-id, :logical-side

   Returns: nil (side effects only)
   Logs errors but does not propagate exceptions to avoid disrupting other inputs"
  [input-id context]
  (if-let [handler (get-input-id-handler input-id)]
    (try
      ;; If handler is a symbol, resolve it to a function
      (let [handler-fn (if (symbol? handler) (resolve handler) handler)]
        (if (ifn? handler-fn)
          (call-with-input-ctx context #(handler-fn context))
          (log/warn "Resolved handler is not a function"
                   {:input-id input-id :handler handler})))
      (catch Exception e
        ;; Log error but don't propagate - allow other inputs to continue
        (log/warn e "Keyboard input handler failed"
                  {:input-id input-id
                   :context context})))
    (log/debug "No handler registered for input-id" {:input-id input-id}))
  nil)

(defn reset-handlers-for-test!
  "Clear all registered handlers (for testing only)"
  []
  (when-let [fw-atom (fw/fw-atom)]
    (registry/reset-domain-for-test! fw-atom :input))
  nil)

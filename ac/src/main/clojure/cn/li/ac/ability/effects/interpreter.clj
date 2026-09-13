(ns cn.li.ac.ability.effects.interpreter
  "Effect interpreter �?the Imperative Shell that executes effect plans.

  Receives the {:effects [...]} list produced by the reducer and
  dispatches each {:effect/type ...} plan to the appropriate handler.

  All handlers have side effects; none return meaningful values.
  The interpreter is the ONLY place in the ability system that talks
  to external systems (network, platform, persistence, events).

  Supported effect types:
    :network-send    �?mark player dirty, triggering auto-sync to client
    :platform-call   �?call a registered platform function via hooks registry
    :persist-state   �?mark player dirty, triggering NBT save
    :fire-event      �?fire an ability lifecycle event
    :radiation-index-sync �?resync the derived radiation-mark read index

  Unrecognised effect types are logged as warnings and skipped."
  (:require [cn.li.ac.ability.effects.network-handler    :as net]
            [cn.li.ac.ability.effects.platform-handler   :as platform]
            [cn.li.ac.ability.effects.persistence-handler :as persist]
            [cn.li.ac.ability.registry.event             :as evt]
            [cn.li.ac.ability.service.radiation-mark-index :as rad-index]
            [cn.li.mcmod.hooks.core                      :as runtime-hooks]
            [cn.li.mcmod.util.log                        :as log]))

;; ============================================================================
;; Event handler
;; ============================================================================

(defn- execute-fire-event!
  "Handle :fire-event effect.

  Effect shape:
    {:effect/type  :fire-event
     :event        map}   ; passed directly to evt/fire-ability-event!"
  [{:keys [event]}]
  (when event
    (evt/fire-ability-event! event))
  nil)

;; ============================================================================
;; Main Dispatcher
;; ============================================================================

(defn execute-effect!
  "Dispatch and execute a single effect plan."
  ([effect]
  (execute-effect! (runtime-hooks/require-player-state-session-id "effect-interpreter")
                    effect))
  ([session-id effect]
   (log/debug "ability-effect.execute"
              {:type (:effect/type effect)
               :player-uuid (:player-uuid effect)
               :ctx-id (:ctx-id effect)})
   (case (:effect/type effect)
     :network-send  (net/execute-network-send! session-id effect)
     :platform-call (platform/execute-platform-call! effect)
     :persist-state (persist/execute-persist-state! session-id effect)
     :fire-event    (execute-fire-event! effect)
     :radiation-index-sync (rad-index/execute-index-sync! session-id effect)
     (log/warn "Unknown ability effect type" (:effect/type effect) effect))))

(defn execute-effects!
  "Execute a sequence of effect plans produced by the reducer.

  Called from the imperative shell after applying commands to the store.
  Each effect is executed in order; exceptions in one do not prevent
  subsequent effects from running."
  ([effects]
    (execute-effects! (runtime-hooks/require-player-state-session-id "effect-interpreter")
                      effects))
  ([session-id effects]
  (doseq [effect effects]
    (try
      (execute-effect! session-id effect)
      (catch Exception e
        (log/stacktrace (str "Effect execution failed " (:effect/type effect)) e))))
   nil))

;; ============================================================================
;; Convenience: execute reducer result
;; ============================================================================

(defn execute-reducer-result!
  "Fire all events and execute all effects from a reducer result map.

  Accepts the map returned by reducer/apply-command or
  reducer/apply-commands:
    {:state ... :events [...] :effects [...]}

  Events are fired first (via evt/fire-ability-event!), then effects."
  [{:keys [events effects]}]
  (let [session-id (runtime-hooks/require-player-state-session-id "effect-interpreter")
        event-effects (mapv (fn [event]
                              {:effect/type :fire-event
                               :event event
                               :event-type (:event/type event)
                               :player-uuid (or (:player-uuid event)
                                                (:uuid event))
                               :ctx-id (:ctx-id event)
                               :session-id session-id})
                            (or events []))
        all-effects (into event-effects (or effects []))]
    (execute-effects! session-id all-effects))
  nil)
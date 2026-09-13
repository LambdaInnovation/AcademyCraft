(ns cn.li.ac.ability.server.damage.handler
  "Damage handler registration and management for skills.

  Skills can register damage handlers to intercept and modify incoming damage.
  This module manages the registration and provides utilities for common patterns.

  No Minecraft imports."
  (:require
            [cn.li.ac.ability.server.damage.runtime :as damage-runtime]
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.ac.ability.util.toggle :as toggle]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.mcmod.util.log :as log]
            [cn.li.mcmod.framework :as fw]
            [cn.li.mcmod.framework.platform :as platform]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- damage-interception-available? []
  (boolean (when-let [fw-atom (fw/fw-atom)]
             (platform/get-adapter fw-atom :damage-interception))))

(defn- damage-interception-call [fn-key & args]
  (when-let [fw-atom (fw/fw-atom)]
    (apply platform/call-adapter fw-atom :damage-interception fn-key args)))

(defn- runtime-player-state
  [player-id]
  (store/get-player-state (runtime-hooks/require-player-state-session-id "damage.handler")
                           player-id))

(defn- runtime-player-state-in-session
  [session-id player-id]
  (store/get-player-state session-id player-id))

(defn register-toggle-damage-handler!
  "Register a damage handler for a toggle skill.
  The handler will only be called if the toggle skill is active.

  Parameters:
  - handler-id: keyword identifier (e.g. :vec-deviation-damage)
  - skill-id: keyword skill identifier (e.g. :vec-deviation)
  - handler-fn: (fn [player-id attacker-id damage damage-source] -> [modified-damage metadata])
  - priority: int (lower = earlier, default 100)

  Returns: true if registered successfully"
  ([handler-id skill-id handler-fn]
   (register-toggle-damage-handler! handler-id skill-id handler-fn 100))
  ([handler-id skill-id handler-fn priority]
   ;; Registered directly into the AC damage registry (the same store the
   ;; :damage-interception adapter's :register-damage-handler! forwards into),
   ;; NOT via the adapter: content init! runs during mod-constructor
   ;; runtime-activation, before the platform adapter installs in common
   ;; setup, so damage-interception-available? was false there and the old
   ;; registration silently no-oped — vec-deviation/vec-reflection never
   ;; reduced damage. The session is resolved at damage time instead of
   ;; captured at registration: the pipeline binds the victim's server
   ;; session around each handler call (damage-process-result's
   ;; with-damaged-player-owner), while registration runs with no player
   ;; bound at all.
   (damage-runtime/register-damage-handler!
    handler-id
    (fn [player-id attacker-id damage damage-source]
      ;; Check if toggle skill is active by looking for active contexts
      (if (runtime-player-state player-id)
        ;; Try to find an active context with this toggle skill
        (let [active-contexts (ctx/get-all-contexts)
              player-contexts (filter (fn [[_ctx-id ctx-data]]
                                        (= (:player-uuid ctx-data) player-id))
                                      active-contexts)
              has-active-toggle? (some (fn [[_ctx-id ctx-data]]
                                         (toggle/is-toggle-active? ctx-data skill-id))
                                       player-contexts)]
          (if has-active-toggle?
            ;; Toggle is active - call handler
            (try
              (handler-fn player-id attacker-id damage damage-source)
              (catch Exception e
                (log/warn "Toggle damage handler" handler-id "failed:" (ex-message e))
                [damage nil]))
            ;; Toggle not active - pass through
            [damage nil]))
        ;; No player state - pass through
        [damage nil]))
    priority)))

(defn unregister-damage-handler!
  "Unregister a damage handler.

  Parameters:
  - handler-id: keyword identifier

  Returns: true if unregistered successfully"
  [handler-id]
  (damage-interception-call :unregister-damage-handler! handler-id))

;; ============================================================================
;; Attack-cancel-check registry
;; ============================================================================

;; ============================================================================
;; Attack-cancel-check registry
;; ============================================================================

(defn default-attack-check-registries-runtime-state
  []
  {:cancel-checks {}
   :precheck-side-effects {}
   :frozen? false})

;; Attack check registries — Framework [:service :attack-check-registries]

(def ^:private ac-path [:service :attack-check-registries])

(defn- attack-check-registries-state-atom []
  (if-let [fw-atom (fw/fw-atom)]
    (or (get-in @fw-atom ac-path)
        (let [a (atom (default-attack-check-registries-runtime-state))]
          (swap! fw-atom assoc-in ac-path a) a))
    (atom (default-attack-check-registries-runtime-state))))


(defn- attack-check-registries-state-snapshot
  []
  @(attack-check-registries-state-atom))

(defn- update-attack-check-registries-state!
  [f & args]
  (apply swap! (attack-check-registries-state-atom) f args))

(defn- assert-registries-open!
  []
  (when (:frozen? (attack-check-registries-state-snapshot))
    (throw (ex-info "Attack check registries are frozen" {}))))

(defn attack-check-registries-snapshot
  []
  (attack-check-registries-state-snapshot))

(defn reset-attack-check-registries-for-test!
  ([] (reset-attack-check-registries-for-test! {}))
  ([{:keys [cancel-checks precheck-side-effects frozen?]
     :or {cancel-checks {} precheck-side-effects {} frozen? false}}]
   (reset! (attack-check-registries-state-atom)
           {:cancel-checks cancel-checks
            :precheck-side-effects precheck-side-effects
            :frozen? frozen?})
   nil))

(defn freeze-attack-check-registries!
  []
  (update-attack-check-registries-state! assoc :frozen? true)
  nil)

(defn register-attack-cancel-check!
  "Register a predicate that decides whether an attack should be cancelled.
  Predicate signature: [player-id attacker-id damage damage-source]."
  [check-id check-fn]
  (when-not (contains? (:cancel-checks (attack-check-registries-state-snapshot)) check-id)
    (assert-registries-open!)
    (update-attack-check-registries-state! assoc-in [:cancel-checks check-id] check-fn))
  nil)

(defn should-cancel-attack?
  "Returns true if any registered check says the attack should be cancelled."
  [player-id attacker-id damage damage-source]
  (boolean
    (some (fn [[_id check-fn]]
            (try
              (check-fn player-id attacker-id damage damage-source)
              (catch Exception e
                (log/warn "Attack cancel check failed:" (ex-message e))
                false)))
          (:cancel-checks (attack-check-registries-state-snapshot)))))

(defn register-attack-precheck-side-effect!
  "Register a side-effect callback executed during attack precheck.
  This is used when a platform event cancels attack before damage-stage handlers run.

  side-effect-fn signature:
  (fn [player-id attacker-id damage damage-source] -> any)
  "
  [effect-id side-effect-fn]
  (when-not (contains? (:precheck-side-effects (attack-check-registries-state-snapshot)) effect-id)
    (assert-registries-open!)
    (update-attack-check-registries-state! assoc-in [:precheck-side-effects effect-id] side-effect-fn))
  nil)

(defn run-attack-precheck-side-effects!
  "Run all registered precheck side-effects.
  Returns true if at least one callback completed without exception."
  [player-id attacker-id damage damage-source]
  (boolean
    (seq
      (keep (fn [[effect-id effect-fn]]
              (try
                 (when (effect-fn player-id attacker-id damage damage-source)
                   effect-id)
                (catch Exception e
                  (log/warn "Attack precheck side-effect failed:" effect-id (ex-message e))
                  nil)))
            (:precheck-side-effects (attack-check-registries-state-snapshot))))))

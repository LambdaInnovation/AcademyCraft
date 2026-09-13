(ns cn.li.platform.neutral.hooks
  "Installed hooks facade for AOT/remapped platform code.

   Vars are rebound exactly once during common bootstrap.  Event, tick and
   render paths invoke those concrete IFns directly; they do not traverse the
   provider map or resolve namespaces."
  (:refer-clojure :exclude [resolve]))

(def ^:private operation-symbols
  '[freeze-runtime-hooks register-runtime-hook-keys! client-session-id player-state-owner
    current-player-state-owner current-context-owner player-state-session-id
    require-player-state-session-id player-state-server-session-id
    require-player-state-server-session-id player-state-client-session-id
    context-player-state-session-id require-context-player-state-session-id
    with-client-ctx-fn with-player-state-owner-fn push-session-context!
    pop-session-context! clear-session-context! push-player-state-owner!
    pop-client-context! register-power-runtime-hooks! register-action! dispatch-action!
    register-sync-descriptor! list-sync-descriptors apply-sync!
    register-player-persistence-descriptor! list-player-persistence-descriptors
    register-player-state-domain! list-player-state-domains register-server-player-login-hook!
    run-server-player-login-hooks! on-player-login! on-player-logout! on-server-stop!
    on-player-clone! on-player-death! on-player-dimension-change! on-server-tick-start!
    on-player-tick! on-server-tick-end! init-damage-handlers! list-player-uuids
    build-sync-payload player-state-dirty? mark-player-clean! get-player-state
    runtime-activated? sync-player-state! ensure-player-state! fresh-player-state
    register-network-handlers! subscribe-achievement-trigger! register-context-route-fns!
    register-context-send-fns! get-context-player-uuid register-damage-handler!
    unregister-damage-handler! get-active-damage-handlers process-damage-interception
    should-cancel-attack-interception? run-attack-precheck-side-effects!
    resolve-item-use-action on-runtime-item-action! build-item-use-plan compute-aoe-damage
    select-reflection-target compute-reflected-damage get-reflection-search-radius
    client-new-context client-register-context! client-get-context client-terminate-context!
    client-transition-to-alive! client-send-context-local! client-build-overlay-plan
    set-client-overlay-activated! client-poll-particle-effects client-poll-sound-effects
    client-tick-start! client-font-init! client-tick-keys! client-active-contexts
    client-latest-sync client-register-push-handlers! client-notify-visual-event!
    client-show-combat-notice! client-enqueue-level-effect! client-build-level-effect-plan
    client-level-effects-active? client-tick-level-effects! client-level-effect-fov-offset
    client-slot-visual-state client-visual-state client-on-slot-key-down!
    client-on-slot-key-tick! client-on-slot-key-up! client-on-slot-key-abort!
    client-on-movement-key-down! client-on-movement-key-tick! client-on-movement-key-up!
    client-on-slot-wheel! client-clear-owner-state! client-abort-all! client-tick!
    client-tick-hand-effects! client-drain-camera-pitch-deltas! client-current-hand-transform
    toggle-debug-overlay-state! set-default-client-owner-fn! default-client-owner])

;; Keep the runtime-generated facade visible to static analyzers as well.  The
;; macro below is still the single runtime definition site; these declarations
;; only describe the audited public contract and do not create a second
;; implementation or alter the install/rebind behavior.
(declare freeze-runtime-hooks register-runtime-hook-keys! client-session-id player-state-owner
         current-player-state-owner current-context-owner player-state-session-id
         require-player-state-session-id require-player-state-server-session-id
         player-state-client-session-id context-player-state-session-id
         require-context-player-state-session-id with-client-ctx-fn with-player-state-owner-fn
         push-session-context! pop-session-context! clear-session-context!
         push-player-state-owner! pop-client-context! register-power-runtime-hooks!
         register-action! dispatch-action! register-sync-descriptor! list-sync-descriptors
         apply-sync! register-player-persistence-descriptor! list-player-persistence-descriptors
         register-player-state-domain! list-player-state-domains register-server-player-login-hook!
         run-server-player-login-hooks! on-player-login! on-player-logout! on-server-stop!
         on-player-clone! on-player-death! on-player-dimension-change! on-server-tick-start!
         on-player-tick! on-server-tick-end! init-damage-handlers! list-player-uuids
         build-sync-payload player-state-dirty? mark-player-clean! get-player-state
         runtime-activated? sync-player-state! ensure-player-state! fresh-player-state
         register-network-handlers! subscribe-achievement-trigger! register-context-route-fns!
         register-context-send-fns! get-context-player-uuid register-damage-handler!
         unregister-damage-handler! get-active-damage-handlers process-damage-interception
         should-cancel-attack-interception? run-attack-precheck-side-effects!
         resolve-item-use-action on-runtime-item-action! build-item-use-plan compute-aoe-damage
         select-reflection-target compute-reflected-damage get-reflection-search-radius
         client-new-context client-register-context! client-get-context client-terminate-context!
         client-transition-to-alive! client-send-context-local! client-build-overlay-plan
         set-client-overlay-activated! client-poll-particle-effects client-poll-sound-effects
         client-tick-start! client-font-init! client-tick-keys! client-active-contexts
         client-latest-sync client-register-push-handlers! client-notify-visual-event!
         client-show-combat-notice! client-enqueue-level-effect! client-build-level-effect-plan
         client-level-effects-active? client-tick-level-effects! client-level-effect-fov-offset
         client-slot-visual-state client-visual-state client-on-slot-key-down!
         client-on-slot-key-tick! client-on-slot-key-up! client-on-slot-key-abort!
         client-on-movement-key-down! client-on-movement-key-tick! client-on-movement-key-up!
         client-on-slot-wheel! client-clear-owner-state! client-abort-all! client-tick!
         client-tick-hand-effects! client-drain-camera-pitch-deltas! client-current-hand-transform
         toggle-debug-overlay-state! set-default-client-owner-fn! default-client-owner)

(defn- unavailable [operation]
  (throw (IllegalStateException. (str "Hooks provider is unavailable: " operation))))

;; AOT callers must link against real Vars.  A top-level `intern` is evaluated
;; while compiling but does not reliably create those Vars when the generated
;; __init class is loaded, so expand explicit `def`s from the audited list.
(defmacro define-operation-facade-vars []
  (cons `do
        (map (fn [operation]
               `(def ~operation (fn [& _#] (unavailable '~operation))))
             operation-symbols)))

(define-operation-facade-vars)

(defn- facade-var
  "Return the local facade Var, recreating its unavailable sentinel when an
   AOT loader has initialized the namespace without retaining an interned Var.
   This preserves the static facade boundary; provider installation still
   replaces every Var with the validated concrete IFn exactly once."
  [operation]
  (let [facade-ns (the-ns 'cn.li.platform.neutral.hooks)]
    (or (ns-resolve facade-ns operation)
        (intern facade-ns operation (fn [& _] (unavailable operation))))))

(defn install!
  [operations]
  (let [expected (set (map #(keyword (name %)) operation-symbols))]
    (when (or (not= expected (set (keys operations)))
              (some (complement ifn?) (vals operations)))
      (throw (ex-info "Hooks provider contract mismatch"
                      {:expected (sort expected) :actual (sort (keys operations))})))
    (doseq [operation operation-symbols]
      (alter-var-root (facade-var operation)
                      (constantly (get operations (keyword (name operation)))))))
  nil)

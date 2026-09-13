(ns cn.li.mcmod.runtime.hooks-provider
  "Neutral provider exposing the stable hooks facade to platform AOT code."
  (:require [cn.li.mcmod.hooks.core :as hooks]))

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

(defn runtime-provider
  "Return the audited, complete hooks facade as concrete IFn values.

   The symbol list is intentionally fixed.  It is both the provider contract
   and the allow-list for the platform shim; consumers cannot resolve arbitrary
   vars through this SPI."
  [_]
  (into {}
        (map (fn [operation]
               [(keyword (name operation))
                (or (ns-resolve 'cn.li.mcmod.hooks.core operation)
                    (throw (ex-info "Hooks facade operation is missing"
                                    {:operation operation})))])
             operation-symbols)))

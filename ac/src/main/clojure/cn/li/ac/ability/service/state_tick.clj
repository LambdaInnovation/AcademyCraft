(ns cn.li.ac.ability.service.state-tick
  "Server tick orchestration for player-state runtime updates."
  (:require [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.model.develop :as ddata]
            [cn.li.ac.ability.registry.event :as evt]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.platform-hooks :as platform-hooks]
            [cn.li.ac.ability.service.reducer :as reducer]
            [cn.li.ac.ability.service.runtime-store :as store]))

(def ^:private fn-pull-portable-dev-energy :ability/pull-portable-dev-energy!)

(defn- fire-recover-speed-event!
  "Extracted to top-level defn- so AOT emits one static class.
   Eliminates per-tick {:uuid uuid-str} map allocation inside doseq context."
  [event-type base-speed uuid-str]
  (evt/fire-calc-event! event-type base-speed {:uuid uuid-str}))

(defn- drain-portable-develop-energy!
  "Pull this tick's develop energy (CPS/TPS) from the held portable developer
   before :server-tick advances the session — upstream DevelopData.tick calls
   developer.tryPullEnergy each tick and fails the development on shortfall
   (battery empty or item no longer in the main hand)."
  [session-id uuid-str state]
  (let [dd (:develop-data state)]
    (when (and dd (ddata/developing? dd) (= :portable (:developer-type dd)))
      (let [ept (ddata/energy-per-tick :portable)
            pulled? (boolean
                     (when (platform-hooks/platform-fn-registered? fn-pull-portable-dev-energy)
                       ((platform-hooks/get-platform-fn fn-pull-portable-dev-energy)
                        uuid-str ept)))]
        (when-not pulled?
          (command-rt/run-command-in-session! session-id uuid-str
                                              {:command :develop-fail}))))))

(defn server-tick-player-in-session!
  "Per-player per-tick state driver.
   Optimized: removed (vec) array clone — doseq handles any seq natively;
   added when guard so idle ticks skip iterator allocation entirely.
   Fully idle players (CP/overload at rest, no cooldowns, not developing)
   skip the :server-tick command dispatch entirely — zero commands, zero
   events, zero speed-calc allocations."
  [session-id uuid-str _sync-fn]
  (when-let [state (store/get-player-state session-id uuid-str)]
    (when-not (reducer/server-tick-noop? state)
      (drain-portable-develop-energy! session-id uuid-str state)
      (let [cp-speed (fire-recover-speed-event! evt/CALC-CP-RECOVER-SPEED
                                                (cfg/cp-recover-speed)
                                                uuid-str)
            ov-speed (fire-recover-speed-event! evt/CALC-OVERLOAD-RECOVER-SPEED
                                                (cfg/overload-recover-speed)
                                                uuid-str)
            tick-result (command-rt/run-command-in-session! session-id
                                                            uuid-str
                                                            {:command :server-tick
                                                             :uuid uuid-str
                                                             :player-uuid uuid-str
                                                             :cp-speed cp-speed
                                                             :ol-speed ov-speed})
            raw-events (:events tick-result)]
        (when raw-events
          (doseq [e raw-events] (evt/fire-ability-event! e)))
        {:events (or raw-events [])
         :state (or (:state tick-result) state)}))))

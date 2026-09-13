(ns cn.li.ac.test.support.hud-render-data
  "Test-only composition helpers preserving the legacy build-hud-render-data /
   build-skill-slot-render-data umbrella shape. Production code (client_ui_hooks.clj's
   build-client-overlay-plan) now calls hud.clj's split builder functions directly
   with a reactive cache in front of the registry/context lookups — these umbrella
   functions have no remaining production caller, only tests that want the
   pre-split, one-call shape."
  (:require [cn.li.ac.ability.client.hud :as hud]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(defn- player-contexts
  [player-uuid]
  (read-model/get-player-contexts-for-player (str player-uuid)
                                             runtime-hooks/client-session-id
                                             :hud))

(defn build-skill-slot-render-data
  "Build skill slot render data with cooldown info and delegate visual state."
  [model screen-width screen-height cooldown-data player-uuid]
  (let [active-contexts (when player-uuid
                          (player-contexts player-uuid))
        skill-exps (or (:skill-exps model) {})]
    (-> (hud/build-skill-slot-shape model screen-width screen-height)
        (hud/patch-skill-slot-cooldown cooldown-data {:player-id player-uuid
                                                      :skill-exps skill-exps})
        (hud/patch-skill-slot-visual active-contexts player-uuid))))

(defn build-hud-render-data
  "Main function to build complete HUD render data."
  [hud-model screen-width screen-height cooldown-data
   & {:keys [player-uuid activate-hint preset-state now-ms combat-notice-component
             showing-numbers? last-show-value-change-ms]}]
  (let [now (or now-ms (System/currentTimeMillis))
        combat-notice (hud/build-combat-notice-data combat-notice-component now)
        preset-indicators (hud/build-preset-indicators-data preset-state now)
        ;; Flatten for backward compat: always pick the CURRENT preset (last element,
        ;; since the vector is built as [previous current] during transitions).
        preset-indicator (last preset-indicators)
        numbers-texts (hud/build-numbers-texts-data hud-model showing-numbers? last-show-value-change-ms now)]
    (when (and hud-model (or (:activated hud-model) combat-notice preset-indicator
                             showing-numbers?
                             ;; During fade-out: showing-numbers? is false but numbers
                             ;; still need to render with decaying alpha.
                             (pos? (long (or last-show-value-change-ms 0)))))
      {:cp-bar (when (:activated hud-model)
                 (hud/build-cp-bar-render-data hud-model))
       :overload-bar (when (:activated hud-model)
                       (hud/build-overload-bar-render-data hud-model now))
       :skill-slots (when (:activated hud-model)
                      (build-skill-slot-render-data hud-model screen-width screen-height
                                                    cooldown-data player-uuid))
       :activation-indicator (when (:activated hud-model)
                               (hud/build-activation-indicator-data hud-model activate-hint))
       :combat-notice combat-notice
       :preset-indicator preset-indicator
       :preset-indicators preset-indicators
       :numbers-texts numbers-texts})))

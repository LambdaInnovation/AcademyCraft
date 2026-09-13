(ns cn.li.ac.ability.adapters.client-effect-hooks
  "Client FX/effect hook composition for AC ability platform bridge."
  (:require [cn.li.ac.ability.client.effects.particles :as client-particles]
            [cn.li.ac.ability.client.effects.sounds :as client-sounds]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.client.font-init :as font-init]))

(defn runtime-client-effect-hooks
  []
  {:client-poll-particle-effects
   (fn [owner]
     (client-particles/poll-particle-effects! owner))

   :client-poll-sound-effects
   (fn [owner]
     (client-sounds/poll-sound-effects! owner))

   :client-enqueue-level-effect!
   (fn [effect-id ctx-id channel payload & opts]
     (apply level-effects/enqueue-level-effect! effect-id ctx-id channel payload opts))

   :client-build-level-effect-plan
   (fn [camera-pos hand-center-pos tick query-nearby-blocks-fn]
     (level-effects/build-level-effect-plan camera-pos hand-center-pos tick query-nearby-blocks-fn))

   :client-level-effects-active?
   (fn [] (level-effects/any-level-effect-active?))

   :client-tick-level-effects!
   (fn []
     (level-effects/tick-level-effects!))

   :client-level-effect-fov-offset
   (fn [player-uuid]
     (level-effects/current-fov-offset player-uuid))

   :client-tick-start!
   (fn [get-player-uuid-fn]
     ;; START-of-tick vanilla-input suppression: must run before handleKeybinds
     ;; reads the KeyMappings — the END-phase call cannot stop skill-owned
     ;; movement keys because KeyboardHandler re-reads them from GLFW.
     (binding [cn.li.ac.ability.client.keybinds/*get-player-uuid-fn* get-player-uuid-fn]
       (client-keybinds/sync-vanilla-input-overrides!)))

   :client-font-init!
   (fn []
     ;; The content client-init hook (RegisterRenderers) can fire before the
     ;; platform bridge is installed — the platform calls this again after
     ;; its bridge merge so the MSDF fonts register with the real ops.
     (font-init/init-fonts!))

   :client-tick-keys!
   (fn [key-state-fn get-player-uuid-fn screen-open?]
     (binding [cn.li.ac.ability.client.keybinds/*get-player-uuid-fn* get-player-uuid-fn]
       (client-keybinds/tick-keys! key-state-fn screen-open?)))

   :client-tick-hand-effects!
   (fn []
     (hand-effects/tick-hand-effects!))

   :client-drain-camera-pitch-deltas!
   (fn [owner]
     (hand-effects/drain-camera-pitch-deltas! owner))

   :client-current-hand-transform
   (fn []
     (hand-effects/current-hand-transform))})

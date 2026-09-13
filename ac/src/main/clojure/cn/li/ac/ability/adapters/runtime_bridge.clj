(ns cn.li.ac.ability.adapters.runtime-bridge
  "Installs AC ability runtime hooks into the platform-neutral mcmod bridge."
  (:require [cn.li.ac.ability.adapters.client-effect-hooks :as client-effects]
            [cn.li.ac.ability.adapters.client-ui-hooks :as client-ui]
            [cn.li.ac.ability.adapters.server-hooks :as server-hooks]
            [cn.li.ac.ability.client.debug-overlay :as debug-overlay]
            [cn.li.ac.ability.client.keybinds :as client-keybinds]
            [cn.li.ac.ability.runtime-container :as runtime-container]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.util.log :as log]))

(def ^:private sync-descriptors
  [{:id :ac/sync-v2
    :content-id "ac"
    :message-key :sync-v2
    :order 0}])

(def ^:private player-persistence-descriptors
  [{:id :ac/player-runtime-state
    :content-id "ac"
    :kind :runtime-state
    :format :compound-tag
    :nbt-key "ac_runtime_v2"
    :order 0}
   {:id :ac/saved-locations
    :content-id "ac"
    :kind :content-owned-compound
    :format :compound-tag
    :host-key :named-world-position-store
    :nbt-key "SavedLocations"
    :clone? true
    :order 10}])

(def ^:private ac-runtime-hook-keys
  #{:get-skills-for-category
    :client-get-skill-by-controllable
    :client-update-ability-data!
    :client-update-resource-data!
    :client-update-cooldown-data!
    :client-update-preset-data!
    :client-req-learn-skill!
    :client-req-level-up!
    :client-req-set-activated!
    :client-req-set-preset-slot!
    :client-req-switch-preset!
    :client-trigger-mode-switch!
    :client-trigger-preset-switch!
    :toggle-debug-overlay-state!})

(def ^:private debug-overlay-hooks
  {:toggle-debug-overlay-state!
   (fn [] (debug-overlay/toggle-debug-state!))})

(defn- install-sync-descriptors!
  []
  (doseq [descriptor sync-descriptors]
    (runtime-hooks/register-sync-descriptor! descriptor))
  nil)

(defn- install-player-persistence-descriptors!
  []
  (doseq [descriptor player-persistence-descriptors]
    (runtime-hooks/register-player-persistence-descriptor! descriptor))
  (runtime-hooks/register-player-state-domain! {:domain-key :ability-data :nbt-key "ac_ability"})
  (runtime-hooks/register-player-state-domain! {:domain-key :resource-data :nbt-key "ac_resource"})
  (runtime-hooks/register-player-state-domain! {:domain-key :preset-data :nbt-key "ac_preset"})
  (runtime-hooks/register-player-state-domain! {:domain-key :develop-data :nbt-key "ac_develop"})
  (runtime-hooks/register-player-state-domain! {:domain-key :cheats-data :nbt-key "ac_cheats"})
  nil)

(defn- install-runtime-components!
  [runtime-components]
  (runtime-container/install-ability-runtime-container!
    (runtime-container/create-ability-runtime-container runtime-components))
  nil)

(defn install-runtime-hooks!
  "Register AC runtime hooks exactly once.

  This bridges loader lifecycle/network code to the AC ability implementation;
  without it, mcmod.hooks.core stays on its no-op defaults."
  ([]
   (install-runtime-hooks! nil))
  ([runtime-components]
  (install/framework-once! ::runtime-hooks-installed?
  (fn []
    (install-runtime-components! runtime-components)
    (server-hooks/register-platform-functions!)
    (server-hooks/register-lifecycle-subscriptions!)
    (client-keybinds/install-default-handlers!)
    (install-sync-descriptors!)
    (install-player-persistence-descriptors!)
    ;; NOTE: Client input is now dispatched solely through mcmod.protocol.keyboard-input
    ;; (System A). input_ids.clj handlers call keybinds/trigger-mode-switch! and
    ;; keybinds/switch-preset! directly. The old register-client-input-descriptor!
    ;; path (System B) was dead code (emit-client-input! was never called).
    (runtime-hooks/register-runtime-hook-keys! ac-runtime-hook-keys)
    (runtime-hooks/register-power-runtime-hooks!
      (merge (server-hooks/runtime-server-hooks)
             (client-ui/runtime-client-ui-hooks)
             (client-effects/runtime-client-effect-hooks)
             debug-overlay-hooks))
    (log/debug "AC ability runtime hooks installed")))))

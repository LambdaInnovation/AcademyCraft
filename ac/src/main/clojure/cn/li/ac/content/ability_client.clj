(ns cn.li.ac.content.ability-client
  "Client-side ability content bootstrap.

  Explicitly initializes discovered _fx.clj namespaces so they register their FX
  channels, level effects, and hand effects only during client FX init.

  This namespace must ONLY be required from client-side code paths
  (e.g. platform client entry points), never from dedicated-server code."
  (:require [cn.li.ac.ability.discovery :as discovery]
            [cn.li.ac.discovery.core :as discovery-core]
            [cn.li.ac.ability.client.fx-registry :as fx-registry]
            [cn.li.ac.ability.client.fx-templates.arc-beam :as arc-beam]
            [cn.li.ac.ability.client.hand-effects :as hand-effects]
            [cn.li.ac.ability.client.keybinds :as keybinds]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(defn- init-fx-namespace! [ns-sym]
  (let [ns-sym (discovery-core/ns-symbol ns-sym)]
    (require ns-sym)
    (when-let [init-var (ns-resolve ns-sym 'init!)]
      (when (bound? init-var)
        (init-var)))))

(defn- init-discovered-fx! []
  (doseq [ns-sym (discovery/discovered-fx-namespaces)]
    (init-fx-namespace! ns-sym)))

(defn init-client-fx!
  "Ensure all client FX registrations have been loaded.
  Safe to call multiple times."
  []
  (install/framework-once! ::fx-initialized?
  (fn []
    (init-discovered-fx!)
    ;; Validate multimethod arities BEFORE freezing — catches the class of
    ;; bug where a defmethod with wrong arity silently corrupts the dispatch
    ;; table for ALL effects (see groundshock fix).
    (arc-beam/validate-fx-multimethods!)
    (fx-registry/freeze-fx-registry!)
    (keybinds/freeze-keybind-registries!)
    (level-effects/freeze-level-effect-registry!)
    (hand-effects/freeze-hand-effect-registry!)
    (log/debug "Ability client FX content initialized"))))

(defn reset-client-fx-for-test!
  "Test-only: clear the client-FX install guard so init-client-fx! can rerun
   within the same Framework lifetime."
  []
  (install/reset-framework-once-flag-for-test! ::fx-initialized?)
  nil)

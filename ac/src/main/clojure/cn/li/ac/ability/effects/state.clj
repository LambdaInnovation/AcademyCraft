(ns cn.li.ac.ability.effects.state
  (:require [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner]))

(defn- resolve-session-id
  []
  (or (some-> ctx/context-owner owner/store-session-id)
      (runtime-hooks/player-state-session-id)
      (runtime-hooks/require-player-state-session-id "effects-state")))

(defn execute-assoc-state!
  [ctx-id player-id {:keys [k v]}]
  (command-rt/run-command-in-session!
   (resolve-session-id)
   player-id
   {:command :context-assoc-skill-state
    :ctx-id ctx-id
    :k k
    :v v}))

(defn execute-charge-tick!
  [ctx-id player-id {:keys [k max]}]
  (let [state-key (or k :charge-ticks)
        result (command-rt/run-command-in-session!
                (resolve-session-id)
                player-id
                {:command :context-increment-skill-state
                 :ctx-id ctx-id
                 :k state-key
                 :max max})]
    (long (or (get-in result [:state :context-registry ctx-id :skill-state state-key]) 0))))

(defn execute-terminate!
  [ctx-id _player-id _params]
  (ctx/terminate-context! ctx-id nil))

(defn execute-overload-floor!
  "Enforce a minimum overload value for the player.
  :floor may be a number or (fn [player-id skill-id exp] ...)."
  [ctx-id player-id skill-id exp {:keys [floor]}]
  (let [floor-val (double (if (fn? floor)
                            (try
                              (floor player-id skill-id exp)
                              (catch clojure.lang.ArityException _
                                (floor {:player-id player-id :skill-id skill-id :exp exp})))
                            (or floor 0.0)))]
    (command-rt/run-command-in-session!
     (resolve-session-id)
     player-id
     {:command :enforce-overload-floor
      :floor-value floor-val})))

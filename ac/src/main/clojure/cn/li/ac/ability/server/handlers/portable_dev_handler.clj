(ns cn.li.ac.ability.server.handlers.portable-dev-handler
  "Portable developer timed-development request handler.

   Upstream: MSG_START_SKILL / MSG_START_LEVEL with PortableDevData as the
   IDeveloper — the same DevelopData state machine as the block developer,
   hosted in player-state :develop-data and advanced by :server-tick.
   Item access goes through platform-hooks registered fns (see
   ability.adapters.server-hooks), keeping ability.server free of
   ac.item / ac.energy requires."
  (:require [cn.li.ac.ability.model.develop :as ddata]
            [cn.li.ac.ability.server.handlers.common :as common]
            [cn.li.ac.ability.service.command-runtime :as command-rt]
            [cn.li.ac.ability.service.platform-hooks :as platform-hooks]
            [cn.li.ac.ability.util.uuid :as uuid]
            [cn.li.mcmod.util.log :as log]))

(def ^:private fn-held-energy :ability/held-portable-dev-energy)
(def ^:private fn-awaken-category :ability/resolve-awaken-category!)

(defn- held-portable-energy [player-uuid]
  (when (platform-hooks/platform-fn-registered? fn-held-energy)
    ((platform-hooks/get-platform-fn fn-held-energy) player-uuid)))

(defn- resolve-awaken-category! [player]
  (when (platform-hooks/platform-fn-registered? fn-awaken-category)
    ((platform-hooks/get-platform-fn fn-awaken-category) player)))

(defn handle-portable-dev-start-request
  [payload player]
  (let [player-uuid (uuid/player-uuid player)
        session-id (common/current-server-session-id)
        state (common/get-state player-uuid)
        ad (:ability-data state)
        action (keyword (:action payload))
        energy (held-portable-energy player-uuid)]
    (cond
      (nil? energy)
      (log/debug "portable-dev-start rejected (no portable developer in hand)" player-uuid)

      (< (double energy) (ddata/energy-per-tick :portable))
      (log/debug "portable-dev-start rejected (energy)" player-uuid energy)

      :else
      (let [cmd (case action
                  :learn-skill {:command :develop-start :action :learn-skill
                                :developer-type :portable
                                :skill-id (some-> (:skill-id payload) keyword)}
                  :level-up (if (:category-id ad)
                              {:command :develop-start :action :level-up
                               :developer-type :portable}
                              ;; Awaken: decide the category NOW (induction
                              ;; factor consumed here — same timing as the
                              ;; block developer session).
                              (when-let [cat (resolve-awaken-category! player)]
                                {:command :develop-start :action :awaken
                                 :developer-type :portable :target-category cat}))
                  nil)]
        (if cmd
          (command-rt/run-command-in-session! session-id player-uuid cmd)
          (log/debug "portable-dev-start rejected (action)" player-uuid action))))))

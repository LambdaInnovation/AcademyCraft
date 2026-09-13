(ns cn.li.ac.ability.service.skill-callback
  "Positional skill action callback contract (Iron Rule / GC-friendly dispatch).

  All :actions callbacks use unified arity:
  [ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]"
  (:require [cn.li.ac.ability.model.ability :as adata]
            [cn.li.ac.ability.service.runtime-store :as store]
            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.runtime.owner :as owner]))

(defn resolve-action-key
  "Map lifecycle callback + skill pattern to :actions key."
  [pattern cb-key]
  (case cb-key
    :on-key-down  (case pattern
                    (:instant :hold-charge-release) :perform!
                    :toggle                        :activate!
                    :release-cast                  :down!
                    :hold-channel                  :down!
                    :charge-window                 :down!
                    :passive                       nil
                    :down!)
    :on-key-tick  :tick!
    :on-key-up    (case pattern
                    :toggle           :deactivate!
                    :release-cast     :up!
                    :hold-channel     :up!
                    :charge-window    :up!
                    :down!)
    :on-key-abort :abort!
    nil))

(defn cost-stage-for-cb-key
  [cb-key]
  (case cb-key
    :on-key-down :down
    :on-key-tick :tick
    :on-key-up   :up
    nil))

(defn- resolved-session-id [owner]
  (or (some-> owner owner/store-session-id)
      (runtime-hooks/require-player-state-session-id "skill-callback")))

(defn skill-exp-for
  [owner player-id skill-id]
  (double (adata/get-skill-exp (get-in (store/get-player-state (resolved-session-id owner) player-id)
                                       [:ability-data])
                               skill-id)))

(defn extract-invoke-args
  "Extract positional callback args from context map and network payload."
  [owner ctx-map payload {:keys [cost-ok? cost-stage]}]
  (let [player-id (:player-uuid ctx-map)
        skill-id (:skill-id ctx-map)
        exp (skill-exp-for owner player-id skill-id)
        hold-ticks (long (or (:hold-ticks payload) 0))
        player-ref (:player payload)]
    [(:id ctx-map) player-id skill-id exp (boolean cost-ok?) hold-ticks cost-stage player-ref]))

(defn invoke-action!
  "Invoke a skill :actions callback with positional args."
  [callback-fn ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref]
  (when (fn? callback-fn)
    (callback-fn ctx-id player-id skill-id exp cost-ok? hold-ticks cost-stage player-ref)))

(defn invoke-cost-fail!
  "Invoke optional :cost-fail! action when resource payment fails."
  [spec ctx-id player-id skill-id exp cost-stage player-ref]
  (when-let [cost-fail-fn (get-in spec [:actions :cost-fail!])]
    (invoke-action! cost-fail-fn ctx-id player-id skill-id exp false 0 cost-stage player-ref)))

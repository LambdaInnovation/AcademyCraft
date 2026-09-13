(ns cn.li.ac.content.ability.vecmanip.arbitration
  "Shared projectile arbitration for vecmanip toggle skills.

  Ensures one projectile is handled by at most one vecmanip skill per tick."
  (:require [clojure.string :as str]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.player-runtime-commands :as prt-cmd]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.util.toggle :as toggle]))

(def ^:private arbitration-config-skill-id :vec-reflection)
(def ^:private arbitration-config-field-id :interaction.projectile-arbitration-priority)

(defn- current-tick []
  (quot (System/currentTimeMillis) 50))

(defn projectile-locks-snapshot
  [player-id]
  (prt-cmd/projectile-claims player-id))

(defn reset-projectile-locks-for-test!
  ([player-id]
   (prt-cmd/run-for-player!
    player-id
    {:command :replace-projectile-claims
     :claims {:tick -1 :owners {}}}))
  ([player-id snapshot]
   (prt-cmd/run-for-player!
    player-id
    {:command :replace-projectile-claims
     :claims snapshot})))

(defn clear-player-projectile-locks!
  [player-id]
  (prt-cmd/run-for-player!
   player-id
   {:command :clear-player-projectile-claims})
  nil)

(defn preferred-skill-id
  []
  (let [v (some-> (skill-config/tunable-string-list arbitration-config-skill-id arbitration-config-field-id)
                  first
                  str
                  str/lower-case
                  str/trim)]
    (if (contains? #{"deviation-first" "vec-deviation" "deviation"} v)
      :vec-deviation
      :vec-reflection)))

(defn skill-allowed-in-dual-active?
  [skill-id]
  (= skill-id (preferred-skill-id)))

(defn dual-active?
  [player-id]
  (let [contexts (ctx/get-all-contexts)]
    (boolean
      (and (some (fn [[_ctx-id ctx-data]]
                   (and (= (:player-uuid ctx-data) player-id)
                        (toggle/is-toggle-active? ctx-data :vec-reflection)))
                 contexts)
           (some (fn [[_ctx-id ctx-data]]
                   (and (= (:player-uuid ctx-data) player-id)
                        (toggle/is-toggle-active? ctx-data :vec-deviation)))
                 contexts)))))

(defn claim-projectile!
  [player-id skill-id projectile-id]
  (if (and player-id projectile-id)
    (boolean
      (:granted?
       (prt-cmd/run-for-player!
        player-id
        {:command :claim-projectile
         :skill-id skill-id
         :projectile-id projectile-id
         :tick (current-tick)})))
    false))

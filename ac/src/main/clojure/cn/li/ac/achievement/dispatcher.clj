(ns cn.li.ac.achievement.dispatcher
  "AC-side achievement dispatcher (game-concept aware)."
  (:require 
            [cn.li.ac.ability.service.runtime-store :as store]
[cn.li.mcmod.hooks.core :as runtime-hooks]
[cn.li.ac.achievement.registry :as ach-reg]
            [cn.li.ac.achievement.trigger :as ach-trigger]
            [cn.li.ac.ability.registry.event :as evt]            [cn.li.mcmod.runtime.install :as install]
            [cn.li.mcmod.util.log :as log]))

(declare trigger-custom-event!
         player-category-in-session)

(defn- player-category
  [uuid]
  (player-category-in-session (runtime-hooks/require-player-state-session-id "achievement.dispatcher")
                              uuid))

(defn- player-category-in-session
  [session-id uuid]
  (get-in (store/get-player-state session-id uuid) [:ability-data :category-id]))

(defn- fire-by-trigger!
  [kind payload uuid]
  (doseq [achievement-id (ach-reg/find-by-trigger kind payload)]
    (ach-trigger/trigger-achievement! uuid achievement-id)))

(defn trigger-custom-event!
  "Manually map a custom game event key to one or more achievements."
  [uuid event-id]
  (fire-by-trigger! :custom {:event-id event-id} uuid))

(defn init-dispatcher!
  []
  (install/framework-once! ::installed?
  (fn []
    (evt/subscribe-ability-event!
      evt/EVT-LEVEL-CHANGE
      (fn [{:keys [uuid new-level]}]
        (when-let [category (player-category uuid)]
          (fire-by-trigger! :level-change {:category category :level new-level} uuid))))

    (evt/subscribe-ability-event!
      evt/EVT-SKILL-LEARN
      (fn [{:keys [uuid skill-id]}]
        (fire-by-trigger! :skill-learn {:skill-id skill-id} uuid)))

    (evt/subscribe-ability-event!
      evt/EVT-SKILL-PERFORM
      (fn [{:keys [uuid skill-id]}]
        (fire-by-trigger! :skill-perform {:skill-id skill-id} uuid)))

    (log/debug "Achievement dispatcher initialized"))))



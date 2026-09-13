(ns cn.li.ac.content.ability.teleporter.penetrate-teleport

  "PenetrateTP skill - preview and teleport through an obstacle.



  Pattern: :release-cast

  Server behavior:

  - Resolve destination from player look vector and requested distance

  - Clamp requested distance by max range and available CP

  - Recompute final destination on key-up server-side only



  No Minecraft imports."

  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]

            [cn.li.ac.ability.fx :as fx]

            [cn.li.ac.ability.service.context-dispatcher :as ctx]

            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]

                        [cn.li.ac.achievement.dispatcher :as ach-dispatcher]

            [cn.li.ac.ability.service.skill-effects :as skill-effects]

            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.content.ability.teleporter.penetrate-dest :as pdest]
            [cn.li.ac.content.ability.teleporter.release-cast-base :as release-cast]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.platform.entity :as entity]

                        [cn.li.mcmod.util.log :as log]))



;; ---------------------------------------------------------------------------

;; Constants and helpers

;; ---------------------------------------------------------------------------



(def-skill-config-ops :penetrate-teleport)

(def ^:private penetrate-teleport-skill-id :penetrate-teleport)

(def ^:private min-distance 0.5)


(def ^:private distance-channel :penetrate-tp/set-distance)



(defn- clamp [v lo hi]

  (max (double lo) (min (double hi) (double v))))



(defn- resolve-exp [player-id]

  (skill-exp player-id))



(defn- cp-per-block [exp]

  (pdest/cp-per-block exp))



(defn- max-distance [exp]

  (pdest/max-distance exp))



(defn- overload-cost [exp]

  (cfg-lerp :cost.up.overload exp))



(defn- cooldown-ticks [exp]

  (cfg-lerp-int :cooldown.ticks exp))



(defn- exp-per-distance []

  (cfg-double :progression.exp-per-distance))



(defn- solid? [world-id bx by bz]

  (when (bm/available?)
    (bm/block-collidable? world-id bx by bz)))



(defn- clamp-distance-by-cp
  [desired-distance current-cp exp]
  (pdest/clamp-distance-by-cp desired-distance current-cp exp))

(defn- resolve-destination
  [player-id look-vec distance]
  (when (and look-vec (bm/available?))
    (let [world-id (geom/world-id-of player-id)
          player-pos (helper/player-position player-id)]
      (when (and world-id player-pos)
        (pdest/destination
          {:x (:x player-pos) :y (:y player-pos) :z (:z player-pos)
           :look-vec look-vec
           :distance distance
           :collidable? (fn [bx by bz] (solid? world-id bx by bz))})))))

(defn- resolve-preview

  [player-id desired-distance]

  (let [exp (resolve-exp player-id)

        cp (skill-effects/current-cp player-id)

        distance (clamp-distance-by-cp desired-distance cp exp)

        look-vec (helper/player-look-vec player-id)

        dest (resolve-destination player-id look-vec distance)]

    {:exp exp

     :desired-distance desired-distance

     :distance distance

     :cp-per-block (cp-per-block exp)

     :dest dest

     :available? (boolean (:available? dest))}))



(defn- default-desired-distance

  [player-id]

  (max-distance (resolve-exp player-id)))



(defn- update-distance

  [player-id distance-state payload]

  (let [exp (resolve-exp player-id)

        lo min-distance

        hi (max-distance exp)

        current (double (or (:desired-distance distance-state) hi))

        next-distance (cond

                        (number? (:distance payload))

                        (clamp (double (:distance payload)) lo hi)



                        (number? (:delta payload))

                        (clamp (+ current (double (:delta payload))) lo hi)



                        :else

                        current)]

    (assoc distance-state :desired-distance next-distance)))



(defn- preview-payload

  [ctx-id]

  (let [ctx-data (ctx-skill/get-context ctx-id)

        preview (get-in ctx-data [:skill-state :preview])

        dest (:dest preview)]

    {:distance (double (or (:distance preview) 0.0))

     ;; The CP-clamped march length for this tick. getDest reads CP, which the
     ;; server owns; sending it lets the client re-march the wall itself every
     ;; frame the way l_updateMark does, without a second copy of the resource
     ;; rules.
     :march-distance (double (or (:distance preview) 0.0))

     :available? (boolean (:available? preview))

     :x (:x dest)

     :y (:y dest)

     :z (:z dest)}))



(defn- update-skill-state-root!

  [ctx-id f & args]

  (apply ctx-skill/update-skill-state-root! ctx-id f args))



(defn- ensure-up-resolve!

  [ctx-id player-id]

  (let [ctx-data (ctx-skill/get-context ctx-id)

        existing (get-in ctx-data [:skill-state :up-resolve])

        desired-distance (double (or (get-in ctx-data [:skill-state :desired-distance])

                                     (default-desired-distance player-id)))]

    (if (and existing

             (= desired-distance (double (or (:desired-distance existing) 0.0))))

      existing

      (let [preview (resolve-preview player-id desired-distance)]

        (update-skill-state-root! ctx-id #(assoc % :up-resolve preview))

        preview))))



(defn- active-ctx-id [player-id skill-id]
  (some->> (ctx/active-contexts player-id)
           (filter #(= skill-id (:skill-id %)))
           first
           :id))

(defn up-cost-cp

  [player-id skill-id _exp]

  ;; Upstream s_execute terminates before consume when the destination is
  ;; unavailable — return 0 so the framework deducts nothing.
  (if-let [ctx-id (active-ctx-id player-id skill-id)]
    (let [resolved (ensure-up-resolve! ctx-id player-id)]
      (if (:available? resolved)
        (* (double (:distance resolved)) (double (:cp-per-block resolved)))
        0.0))
    0.0))



(defn up-cost-overload

  [player-id skill-id _exp]

  (if-let [ctx-id (active-ctx-id player-id skill-id)]
    (let [resolved (ensure-up-resolve! ctx-id player-id)]
      (if (:available? resolved)
        (double (overload-cost (:exp resolved)))
        0.0))
    0.0))



(defn- up-cost-creative?
  [_player-id _skill-id _exp player-ref]
  (boolean (and player-ref (entity/player-creative? player-ref))))



;; ---------------------------------------------------------------------------

;; Actions

;; ---------------------------------------------------------------------------



(defn- penetrate-tp-down-impl!

  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]

  (when cost-ok?

    (let [desired (default-desired-distance player-id)

          preview (resolve-preview player-id desired)]

      (ctx-skill/replace-skill-state! ctx-id {:hold-ticks 0

                                     :desired-distance desired

                                     :preview preview

                                     :up-resolve nil})

      ;; Upstream l_spawnMark spawns EntityTPMarking on MSG_MADEALIVE; send
      ;; the first preview so the mark appears on key-down.
      (fx/send! ctx-id {:topic :penetrate-teleport/fx-start :mode :start} nil

                (preview-payload ctx-id))

      (ctx/ctx-on! ctx-id distance-channel

                   (fn [payload]

                     (update-skill-state-root! ctx-id

                                               (fn [st]

                                                 (let [base (or st {:desired-distance desired})

                                                       updated (update-distance player-id base payload)

                                                       resolved (resolve-preview player-id (:desired-distance updated))]

                                                   (-> updated

                                                       (assoc :preview resolved)

                                                       (assoc :up-resolve nil))))))))))



(defn- penetrate-tp-tick-impl!

  [ctx-id player-id _skill-id _exp _cost-ok? hold-ticks _cost-stage _player-ref]

  (let [ctx-data (ctx-skill/get-context ctx-id)

        desired (double (or (get-in ctx-data [:skill-state :desired-distance])

                            (default-desired-distance player-id)))

        preview (resolve-preview player-id desired)]

    (update-skill-state-root! ctx-id

                              (fn [st]

                                (-> (or st {})

                                    (assoc :hold-ticks (long hold-ticks))

                                    (assoc :desired-distance desired)

                                    (assoc :preview preview)

                                    (assoc :up-resolve nil))))

    ;; Upstream l_updateMark moves EntityTPMarking every client tick.
    (fx/send! ctx-id {:topic :penetrate-teleport/fx-update :mode :update} nil

              (preview-payload ctx-id))))



(defn- penetrate-tp-up-impl!

  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage _player-ref]

  (try

    (let [resolved (ensure-up-resolve! ctx-id player-id)

          dest (:dest resolved)

          exp (:exp resolved)]

      ;; AcademyCraft plays the local release sound before server validation,
      ;; including unavailable destinations and failed resource settlement.
      (fx/send! ctx-id {:topic :penetrate-teleport/fx-perform :mode :perform}
                nil
                {:to-x (:x dest)

                 :to-y (:y dest)

                 :to-z (:z dest)})

      ;; Upstream s_execute calls terminate() when the destination is
      ;; unavailable but has no `return` after it, and LambdaLib's terminate()
      ;; only marks the context dead -- so it goes on to consume, teleport,
      ;; grant exp and set the cooldown anyway, dropping the player inside the
      ;; wall. That is a missing return, not a design, and is not reproduced.
      (if (and cost-ok?
               (:available? resolved)

               dest

               (helper/teleport-to! player-id

                                    (geom/world-id-of player-id)

                                    (:x dest) (:y dest) (:z dest)))

        (do

          (skill-effects/add-skill-exp! player-id penetrate-teleport-skill-id

                                        (* (exp-per-distance)

                                           (double (:distance resolved))))

          (ach-dispatcher/trigger-custom-event! player-id "teleporter.ignore_barrier")

          (skill-effects/set-main-cooldown! player-id penetrate-teleport-skill-id (cooldown-ticks exp))

          nil)

        (log/debug "PenetrateTP: execute failed" {:cost-ok? cost-ok?

                                                   :available? (:available? resolved)})))

    (catch Exception e

      (log/warn "PenetrateTP up! failed:" (ex-message e)))))





;; ---------------------------------------------------------------------------

;; Skill registration

;; ---------------------------------------------------------------------------




(defn- penetrate-tp-abort-impl!

  "Key-abort: clear the hold state and drop the mark (upstream l_onKeyAbort
  terminates; c_endEffect kills EntityTPMarking)."

  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (ctx-skill/clear-skill-state! ctx-id)

  (fx/send! ctx-id {:topic :penetrate-teleport/fx-end :mode :end} nil))



(def ^:private release-cast-ops
  (release-cast/build-ops
    {:down! penetrate-tp-down-impl!
     :tick! penetrate-tp-tick-impl!
     :up! penetrate-tp-up-impl!
     :abort! penetrate-tp-abort-impl!}))

(defn penetrate-tp-down! [& args] (apply release-cast/down! release-cast-ops args))
(defn penetrate-tp-tick! [& args] (apply release-cast/tick! release-cast-ops args))
(defn penetrate-tp-up! [& args] (apply release-cast/up! release-cast-ops args))
(defn penetrate-tp-abort! [& args] (apply release-cast/abort! release-cast-ops args))

(defskill penetrate-teleport-skill

  :id             :penetrate-teleport

  :category-id    :teleporter

  :name-key       "ability.skill.teleporter.penetrate_teleport"

  :description-key "ability.skill.teleporter.penetrate_teleport.desc"

  :icon           "textures/abilities/teleporter/skills/penetrate_teleport.png"

  :ui-position    [60 46]



  :ctrl-id        :penetrate-teleport

  :cp-consume-speed 0.0

  :overload-consume-speed 0.0

  :pattern        :release-cast

  :cost           {:up {:cp up-cost-cp

                        :overload up-cost-overload

                        :creative? up-cost-creative?}}

  :cooldown       {:mode :manual}

  :actions        {:down!  penetrate-tp-down!

                   :tick!  penetrate-tp-tick!

                   :up!    penetrate-tp-up!

                   :abort! penetrate-tp-abort!}

  :fx             {:start {:topic :penetrate-teleport/fx-start :payload (fn [_] {})}

                   :update {:topic :penetrate-teleport/fx-update :payload (fn [{:keys [ctx-id]}]

                                                                      (preview-payload ctx-id))}

                   :end   {:topic :penetrate-teleport/fx-end   :payload (fn [_] {})}}

  :prerequisites  [{:skill-id :threatening-teleport :min-exp 0.5}])




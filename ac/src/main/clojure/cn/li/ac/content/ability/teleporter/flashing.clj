(ns cn.li.ac.content.ability.teleporter.flashing

  "Flashing skill - toggle main state + movement sub-keys (W/A/S/D).



  Main slot key:

  - key-down toggles flashing active/inactive.



  Movement sub-key events (forward/back/left/right):

  - down/tick: update preview destination.

  - up: perform one authority teleport with resource settlement.



  No Minecraft imports."

  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]

            [cn.li.ac.ability.fx :as fx]

            [cn.li.ac.ability.service.context-dispatcher :as ctx]

            [cn.li.ac.ability.service.context-manager :as ctx-mgr]

            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]

                        [cn.li.ac.ability.service.skill-effects :as skill-effects]

            [cn.li.ac.ability.effects.geom :as geom]

            [cn.li.ac.achievement.dispatcher :as ach-dispatcher]

            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.content.ability.teleporter.flashing-dest :as fdest]

            [cn.li.mcmod.hooks.core :as runtime-hooks]
            [cn.li.mcmod.platform.block-manipulation :as bm]
            [cn.li.mcmod.platform.entity :as entity]
            [cn.li.mcmod.platform.raycast :as raycast]

            [cn.li.mcmod.util.log :as log]))




(def-skill-config-ops :flashing)
(def ^:private flashing-skill-id :flashing)

(def ^:private move-down-channel :flashing/move-down)

(def ^:private move-tick-channel :flashing/move-tick)

(def ^:private move-up-channel :flashing/move-up)






(defn- now-ms []

  (System/currentTimeMillis))



(defn- set-skill-state!

  [ctx-id k v]

  (ctx-skill/assoc-skill-state! ctx-id k v))



(defn- update-skill-state-root!

  [ctx-id f & args]

  (apply ctx-skill/update-skill-state-root! ctx-id f args))



(defn- movement-key->direction [movement-key]

  (case movement-key

    :forward :forward

    :back :back

    :left :left

    :right :right

    nil))



(defn- destination-head-blocked?

  [world-id x y z]

  (boolean
    (bm/get-block world-id
                  (int (double x))
                  (int (+ (double y) 1.0))
                  (int (double z)))))



(defn- resolve-preview

  [player-id direction exp]

  (let [dist (fdest/blink-distance exp)

        pos (or (when (raycast/available?)
                  (raycast/player-position player-id))
                (helper/player-position player-id))

        look (helper/player-look-vec player-id)]

    (when (and pos look)

      (let [world-id (:world-id pos)

            resolved (fdest/destination
                       {:x (:x pos) :y (:y pos) :z (:z pos)
                        :eye-y (or (:eye-y pos) (+ (double (:y pos)) 1.62))
                        :look-vec look
                        :direction direction
                        :dist dist
                        :raycast (when (raycast/available?)
                                   (fn [sx sy sz dx dy dz max-dist]
                                     (raycast/raycast-combined-excluding
                                       world-id sx sy sz dx dy dz max-dist player-id)))
                        :head-blocked? (partial destination-head-blocked? world-id)})]

        {:direction direction

         :distance (double dist)

         :world-id world-id

         :from-x (:from-x resolved)

         :from-y (:from-y resolved)

         :from-z (:from-z resolved)

         :to-x (double (:to-x resolved))

         :to-y (double (:to-y resolved))

         :to-z (double (:to-z resolved))}))))



(defn- clear-preview!

  [ctx-id]

  (update-skill-state-root! ctx-id

                            (fn [st]

                              (-> (or st {})

                                  (dissoc :direction)

                                  (dissoc :preview)))))



(defn- active-context

  [ctx-id ctx-data]

  (when (get-in ctx-data [:skill-state :active?])
    ctx-data))

(defn- active-flashing-ctx-id

  "First flashing context of `player-id` whose active? flag is set,

  optionally excluding `exclude-ctx-id`."

  ([player-id]

   (active-flashing-ctx-id player-id nil))

  ([player-id exclude-ctx-id]

   (->> (ctx/get-all-contexts)

        (filter (fn [[ctx-id ctx-data]]

                  (and (not= ctx-id exclude-ctx-id)

                       (= (:player-uuid ctx-data) player-id)

                       (get-in ctx-data [:skill-state :active?]))))

        first

        first)))



(defn- flashing-cooldown-ticks

  [ctx-id player-id]

  (let [exp (double (or (get-in (ctx-skill/get-context ctx-id)

                                [:skill-state :active-exp])

                        (skill-exp player-id)))]

    (cfg-lerp-int :cooldown.deactivate-ticks exp)))



(defn- deactivate-and-terminate!

  "Original serverTerminated: cooldown on termination. Notify the client so

  its mirror context and preview marking are cleaned up."

  [ctx-id player-id reason]

  (fx/send! ctx-id {:topic :flashing/fx-state-end} nil {})

  (skill-effects/set-main-cooldown! player-id flashing-skill-id

                                    (flashing-cooldown-ticks ctx-id player-id))

  (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!)

  (log/debug "Flashing: Deactivated" reason)

  nil)



(defn- perform-flash!

  [player-id ctx-id direction]

  (let [ctx-data (ctx-skill/get-context ctx-id)
        exp (double (or (get-in ctx-data [:skill-state :active-exp])
                        (skill-exp player-id)))
        creative? (boolean (get-in ctx-data [:skill-state :creative?]))

        cp-cost (cfg-lerp :cost.blink.cp exp)

        overload-cost (cfg-lerp :cost.blink.overload exp)
        resource-result (skill-effects/perform-resource!
                          player-id
                          (double overload-cost)
                          (double cp-cost)
                          creative?)]

    (when (:success? resource-result)
      (when-let [preview (resolve-preview player-id direction exp)]
        (let [world-id (geom/world-id-of player-id)]
          (when (helper/teleport-to! player-id world-id
                                     (:to-x preview)
                                     (:to-y preview)
                                     (:to-z preview))

          (let [protect-ticks (cfg-lerp-int :timing.post-blink-fall-protect-ticks exp)

                protect-ticks (long (max 0 protect-ticks))]

            (set-skill-state! ctx-id [:fall-protect-ticks] protect-ticks)
            (helper/reset-fall-damage! player-id))

          (skill-effects/add-skill-exp! player-id flashing-skill-id

                                        (cfg-double :progression.exp-blink))

          (ach-dispatcher/trigger-custom-event! player-id "teleporter.flashing")

          ;; Original's serverPerform sendToClient(MSG_PERFORM) plays the
          ;; blink sound for owner + nearby unconditionally (only the
          ;; anti-fall-damage GravityCancellor hack is isLocal-gated).
          (fx/send-local-and-nearby! ctx-id {:topic :flashing/fx-perform :mode :perform} nil preview)

            true))))))



(defn- update-preview!

  [ctx-id player-id direction mode]

  (let [ctx-data (ctx-skill/get-context ctx-id)
        exp (double (or (get-in ctx-data [:skill-state :active-exp])
                        (skill-exp player-id)))
        creative? (boolean (get-in ctx-data [:skill-state :creative?]))
        cp-cost (cfg-lerp :cost.blink.cp exp)]
    (if (or creative?
            (>= (skill-effects/current-cp player-id) (double cp-cost)))
      (when-let [preview (resolve-preview player-id direction exp)]

        (update-skill-state-root! ctx-id

                                  (fn [st]

                                    (-> (or st {})

                                        (assoc :active? true)

                                        (assoc :direction direction)

                                        (assoc :preview preview))))

        (fx/send! ctx-id {:topic mode} nil preview))
      (do
        (clear-preview! ctx-id)
        (fx/send! ctx-id {:topic :flashing/fx-preview-end}
                  nil
                  {:direction direction})))))



(defn- register-movement-listeners!

  [ctx-id]

  (when-not (get-in (ctx-skill/get-context ctx-id) [:skill-state :listeners-installed?])

    (ctx/ctx-on! ctx-id move-down-channel

                 (fn [{:keys [key]}]

                   (when-let [ctx-data (ctx-skill/get-context ctx-id)]

                     (when-let [active-ctx (active-context ctx-id ctx-data)]

                       (when-let [direction (movement-key->direction key)]

                         (update-preview! ctx-id (:player-uuid active-ctx) direction :flashing/fx-preview-start))))))

    (ctx/ctx-on! ctx-id move-tick-channel

                 (fn [{:keys [key]}]

                   (when-let [ctx-data (ctx-skill/get-context ctx-id)]

                     (when-let [active-ctx (active-context ctx-id ctx-data)]

                       (when-let [direction (movement-key->direction key)]

                         (update-preview! ctx-id (:player-uuid active-ctx) direction :flashing/fx-preview-update))))))

    (ctx/ctx-on! ctx-id move-up-channel

                 (fn [{:keys [key]}]

                   (when-let [ctx-data (ctx-skill/get-context ctx-id)]

                     (when-let [active-ctx (active-context ctx-id ctx-data)]

                       (when-let [direction (movement-key->direction key)]

                         (perform-flash! (:player-uuid active-ctx) ctx-id direction)

                         (clear-preview! ctx-id)

                         (fx/send! ctx-id {:topic :flashing/fx-preview-end} nil {:direction direction}))))))

              (set-skill-state! ctx-id [:listeners-installed?] true)))



(defn flashing-activate!

  "Press-to-toggle like the original Flashing onKeyDown: the client's slot

  ctx-id is cleared at key-up, so the second press arrives on a NEW context

  - deactivate the still-active context of the previous press (and this

  one); otherwise activate."

  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage player-ref]

  (if-let [active-ctx-id (active-flashing-ctx-id player-id ctx-id)]

    (do

      (deactivate-and-terminate! active-ctx-id player-id :manual)

      (ctx/terminate-context! ctx-id ctx-mgr/send-terminated-context!))

    (if cost-ok?

    (let [exp (skill-exp player-id)

          max-active-ticks (cfg-lerp-int :timing.max-active-ticks exp)

          overload-floor (double (or (skill-effects/player-path player-id [:resource-data :cur-overload] 0.0) 0.0))
          creative? (boolean (and player-ref (entity/player-creative? player-ref)))]

      (update-skill-state-root! ctx-id

                                (fn [st]

                                  (-> (or st {})

                                      (assoc :active? true)

                                      (assoc :active-exp exp)
                                      (assoc :active-ticks 0)
                                      (assoc :max-active-ticks max-active-ticks)

                                      (assoc :overload-floor overload-floor)
                                      (assoc :creative? creative?)
                                      (assoc :fall-protect-ticks 0)

                                      (dissoc :direction)

                                      (dissoc :preview))))

      (register-movement-listeners! ctx-id)

      ;; Upstream localMakeAlive: when the context goes alive the client
      ;; registers the WASD sub-keys (and their hint icons). The port's
      ;; :state-start channel spawns the tp-marking and creates the level fx
      ;; state the movement-hint column reads — without this dispatch the fx
      ;; state only materialized lazily on the first preview, so the marking
      ;; never appeared and the hint column had nothing to read.
      (fx/send! ctx-id {:topic :flashing/fx-state-start :mode :state-start} nil {}))
    (ctx/terminate-context! ctx-id nil))))



(defn flashing-tick!

  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (when (get-in ctx-data [:skill-state :active?])
      (let [ticks (long (or (get-in ctx-data [:skill-state :active-ticks]) 0))
            max-ticks (long (or (get-in ctx-data [:skill-state :max-active-ticks]) 0))
            overload-floor (double (or (get-in ctx-data [:skill-state :overload-floor]) 0.0))
            protect-ticks (long (or (get-in ctx-data [:skill-state :fall-protect-ticks]) 0))]
        (skill-effects/enforce-overload-floor! player-id overload-floor)
        (when (pos? protect-ticks)
          (helper/reset-fall-damage! player-id))
        (if (> ticks max-ticks)

          (deactivate-and-terminate! ctx-id player-id :max-time)
          (update-skill-state-root!
            ctx-id
            (fn [st]
              (-> st
                  (assoc :active-ticks (inc ticks))
                  (assoc :fall-protect-ticks (max 0 (dec protect-ticks)))))))))))



(defn flashing-deactivate!

  "Release does nothing - the original onKeyUp has no handler; the second

  press (or max-time) deactivates."

  [_ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  nil)




(defn flashing-abort!

  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (fx/send! ctx-id {:topic :flashing/fx-state-end} nil {})

  (update-skill-state-root! ctx-id

                            (fn [st]

                              (-> (or st {})

                                  (assoc :active? false)

                                  (dissoc :direction)

                                  (dissoc :preview))))

  (let [exp (double (or (get-in (ctx-skill/get-context ctx-id)
                                 [:skill-state :active-exp])
                        (skill-exp player-id)))]
    (skill-effects/set-main-cooldown! player-id flashing-skill-id
                                      (cfg-lerp-int :cooldown.deactivate-ticks exp))))



(defskill flashing-skill

  :id              :flashing

  :category-id     :teleporter

  :name-key        "ability.skill.teleporter.flashing"

  :description-key "ability.skill.teleporter.flashing.desc"

  :icon            "textures/abilities/teleporter/skills/flashing.png"

  :ui-position     [220 20]



  :ctrl-id         :flashing

  :cp-consume-speed 0.0

  :overload-consume-speed 0.0

  :pattern         :release-cast

  :input-policy    {:terminate-on-key-up? false

                    :keep-active-on-key-up? true}

  :cost            {:down {:overload (fn [player-id _skill-id _exp]

                                       (cfg-lerp :cost.down.overload

                                                        (skill-exp player-id)))

                           :cp (fn [player-id _skill-id _exp]

                                 (cfg-lerp :cost.down.cp

                                                  (skill-exp player-id)))
                           :creative? (fn [_player-id _skill-id _exp player-ref]
                                      (boolean
                                        (and player-ref
                                             (entity/player-creative? player-ref))))}}

  :cooldown        {:mode :manual}

  :actions         {:down!  flashing-activate!

                    :tick!  flashing-tick!

                    :up!    flashing-deactivate!

                    :abort! flashing-abort!}

  :fx              {:start {:topic :flashing/fx-state-start :payload (fn [_] {})}

                    :end   {:topic :flashing/fx-state-end :payload (fn [_] {})}}

  :prerequisites   [{:skill-id :shift-teleport :min-exp 0.8}])




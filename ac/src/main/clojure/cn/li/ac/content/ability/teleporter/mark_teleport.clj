(ns cn.li.ac.content.ability.teleporter.mark-teleport

  "MarkTeleport skill - teleport to look direction target.



  Original-aligned mechanics:

  - Hold key to extend range by 2 blocks per tick up to min(max-range, current-cp/cpb)

  - Max range: lerp(25,60,exp)

  - CP consume per block: lerp(12,4,exp)

  - Overload: lerp(40,20,exp)

  - Cooldown: lerp(30,0,exp)

  - Minimum valid distance: 3 blocks

  - Missed raycasts still target look-direction endpoint

  - Release teleports, dismounts riding entities, resets fall damage

  - Experience gain: 0.00018 * distance

  - Client-side destination marker with looping teleport particles and execute sound



  No Minecraft imports."

  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]

            [cn.li.ac.ability.fx :as fx]

            [cn.li.ac.ability.service.context-dispatcher :as ctx]

            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]

                        [cn.li.ac.ability.service.skill-effects :as skill-effects]

                        [cn.li.mcmod.platform.entity :as entity]

            [cn.li.mcmod.platform.position :as pos]

            [cn.li.mcmod.platform.raycast :as raycast]

            [cn.li.ac.ability.effects.motion :as motion-effects]

            [cn.li.mcmod.platform.world :as world]

            [cn.li.mcmod.util.log :as log]

            [cn.li.ac.content.ability.teleporter.tp-skill-helper :as helper]
            [cn.li.ac.content.ability.teleporter.mark-teleport-dest :as dest]
            [cn.li.ac.content.ability.teleporter.release-cast-base :as release-cast]))



(def-skill-config-ops :mark-teleport)

(def ^:private mark-teleport-skill-id :mark-teleport)



(defn- current-cp [player-id]

  (skill-effects/current-cp player-id)

  )



(defn- cp-per-block [exp]

  (dest/cp-per-block exp))



(defn- overload-cost [exp]

  (cfg-lerp :cost.up.overload exp))



(defn- cooldown-ticks [exp]

  (cfg-lerp-int :cooldown.ticks exp))



(defn- max-distance [exp cp ticks]

  (dest/max-distance exp cp ticks))



(defn- add-exp! [player-id amount]

  (skill-effects/add-skill-exp! player-id mark-teleport-skill-id (double amount)))



(defn- build-target-fx-payload

  "The fx payload carries the teleport dest itself, which is where upstream
  puts the EntityTPMarking. MarkRender hangs the humanoid from that point
  rather than standing it on it, and the ambient particles are offset from it
  too, so both come out right without a separate landing-spot anchor."

  [target]

  (when target

    {:target {:x (double (:target-x target))

              :y (double (:target-y target))

              :z (double (:target-z target))}

     ;; getMaxDist for this tick. Upstream's client recomputes it from its own
     ;; CPData; sending it keeps the client from needing a second copy of the
     ;; resource rules while still letting it re-solve the AIM every frame,
     ;; which is the part that has to track the crosshair.
     :dist (double (or (:dist target) 0.0))

     :distance (double (:distance target))}))



(defn- destination-head-blocked?

  [player x y z]

  (when player

    (let [level (entity/player-get-level player)]

      (when level

        (let [block-pos (pos/create-block-pos (int x) (int (+ y 1.0)) (int z))

              block-state (world/get-block-state level block-pos)]

          (not (world/block-state-is-air block-state)))))))



(defn- resolve-hit-destination

  [player hit]

  (dest/hit-destination hit (partial destination-head-blocked? player)))



(defn- resolve-destination

  [player-id player hold-ticks]

  (let [exp (double (or (skill-exp player-id) 0.0))

        cp (current-cp player-id)

        player-pos (or (when (raycast/available?)
                         (raycast/player-position player-id))
                       (when (motion-effects/teleportation-available?)
                         (motion-effects/player-position player-id)))

        look-vec (when (raycast/available?)

                   (raycast/player-look-vector player-id))]

    (when (and player-pos look-vec)

      (let [{:keys [world-id x y z]} player-pos

            dist (max-distance exp cp hold-ticks)

            start-y (double (or (:eye-y player-pos)
                                (+ (double y) (cfg-double :targeting.eye-height))))

            hit (when (raycast/available?)
                  (raycast/raycast-combined-from-player player-id dist true))

            resolved (if hit

                       (resolve-hit-destination player hit)

                       (dest/miss-destination x start-y z look-vec dist))

            distance (dest/distance-from x y z resolved)]

        (merge resolved

               {:world-id world-id

                :dist dist

                :distance distance

                :hold-ticks (long hold-ticks)

                :exp exp

          :cp cp})))))



(defn- cached-or-resolved-target

  [player-id ctx-id player]

  (when-let [ctx-data (ctx-skill/get-context ctx-id)]

    (let [hold-ticks (long (or (get-in ctx-data [:skill-state :hold-ticks]) 0))]

      (or (when (get-in ctx-data [:skill-state :has-target])
            (select-keys (:skill-state ctx-data)
                         [:world-id :target-x :target-y :target-z :dist :distance :exp]))
          (resolve-destination player-id player hold-ticks)))))



(defn mark-teleport-fx-update-payload

  [{:keys [ctx-id]}]

  (when-let [ctx-data (ctx-skill/get-context ctx-id)]

    (build-target-fx-payload (:skill-state ctx-data))))



(defn mark-teleport-fx-perform-payload

  [{:keys [ctx-id]}]

  (mark-teleport-fx-update-payload {:ctx-id ctx-id}))



(defn- active-ctx-id [player-id skill-id]
  (some->> (ctx/active-contexts player-id)
           (filter #(= skill-id (:skill-id %)))
           first
           :id))

(defn mark-teleport-cost-up-cp

  [player-id skill-id _exp]

  (if-let [target (when-let [ctx-id (active-ctx-id player-id skill-id)]
                     (cached-or-resolved-target player-id ctx-id nil))]

    (let [distance (double (:distance target))]

      (if (>= distance (cfg-double :targeting.min-distance))

        (* distance (cp-per-block (double (or (:exp target) (skill-exp player-id) 0.0))))

        0.0))

    0.0))



(defn mark-teleport-cost-up-overload

  [player-id skill-id _exp]

  (if-let [target (when-let [ctx-id (active-ctx-id player-id skill-id)]
                     (cached-or-resolved-target player-id ctx-id nil))]

    (let [distance (double (:distance target))]

      (if (>= distance (cfg-double :targeting.min-distance))

        (overload-cost (double (or (:exp target) (skill-exp player-id) 0.0)))

        0.0))

    0.0))



(defn mark-teleport-cost-creative?
  [_player-id _skill-id _exp player-ref]
  (boolean (and player-ref (entity/player-creative? player-ref))))





(defn- mark-teleport-on-key-down-impl!

  "Key-down: seed the hold state and spawn the destination mark immediately
  (upstream l_start spawns EntityTPMarking on MSG_MADEALIVE)."

  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage player-ref]

  (ctx-skill/replace-skill-state! ctx-id {:hold-ticks 0 :has-target false})

  (when-let [target (resolve-destination player-id player-ref 0)]

    (ctx-skill/replace-skill-state! ctx-id

                           (merge {:hold-ticks 0}

                                  (assoc target :hold-ticks 0 :has-target true)))

    (fx/send! ctx-id {:topic :mark-teleport/fx-start :mode :start} nil

              (build-target-fx-payload target))))



(defn- mark-teleport-on-key-tick-impl!

  "Update destination marker while key is held."

  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage player-ref]

  (when-let [ctx (ctx-skill/get-context ctx-id)]

    (let [next-ticks (inc (long (or (get-in ctx [:skill-state :hold-ticks]) 0)))]

      (if-let [target (resolve-destination player-id player-ref next-ticks)]

        (do

          (ctx-skill/replace-skill-state! ctx-id

                                 (merge (:skill-state ctx)

                                        (assoc target :hold-ticks next-ticks :has-target true)))

          (fx/send! ctx-id {:topic :mark-teleport/fx-update :mode :update} nil

                    (build-target-fx-payload target)))

        (ctx-skill/replace-skill-state! ctx-id

                               (merge (:skill-state ctx)

                                      {:hold-ticks next-ticks :has-target false}))))))



(defn- mark-teleport-on-key-abort-impl!

  "Key-abort: clear the hold state and drop the mark (upstream l_onKeyAbort
  terminates; the client kills EntityTPMarking on MSG_TERMINATED)."

  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]

  (ctx-skill/clear-skill-state! ctx-id)

  (fx/send! ctx-id {:topic :mark-teleport/fx-end :mode :end} nil))



(defn- mark-teleport-on-key-up-impl!

  "Execute teleport when key released."

  [ctx-id player-id _skill-id _exp cost-ok? _hold-ticks _cost-stage player-ref]

  (when-let [ctx (ctx-skill/get-context ctx-id)]

    (let [hold-ticks (long (or (get-in ctx [:skill-state :hold-ticks]) 0))

          target (or (when (get-in ctx [:skill-state :has-target])
                       (select-keys (:skill-state ctx)
                                    [:world-id :target-x :target-y :target-z :dist :distance :exp]))
                     (resolve-destination player-id player-ref hold-ticks))]

      (if target

        (ctx-skill/replace-skill-state! ctx-id

                               (merge (:skill-state ctx)

                                      (assoc target :hold-ticks hold-ticks :has-target true)))

        (ctx-skill/replace-skill-state! ctx-id {:hold-ticks hold-ticks :has-target false}))

      (when (and target (motion-effects/teleportation-available?))

        (let [distance (double (:distance target))

              exp (double (or (:exp target) (skill-exp player-id) 0.0))]

          (when (and cost-ok? (>= distance (cfg-double :targeting.min-distance)))

            ;; Upstream dismounts the player before setPositionAndUpdate.
            (motion-effects/dismount-riding! player-id)

            (let [success (motion-effects/teleport-player! player-id

                                                          (:world-id target)

                                                          (:target-x target)

                                                          (:target-y target)

                                                          (:target-z target))]

              (when success

                ;; Original's s_execute sendToClient(MSG_SOUND) plays the
                ;; teleport sound for owner + nearby unconditionally — only
                ;; the aim-mark entity itself is isLocal-gated separately.
                (fx/send-local-and-nearby! ctx-id {:topic :mark-teleport/fx-perform :mode :perform} nil

                          (merge {:skill-id mark-teleport-skill-id

                                  :player-id player-id

                                  :ctx-id ctx-id}

                                 (or (build-target-fx-payload target) {})))

                (motion-effects/reset-fall-damage! player-id)

                (add-exp! player-id (* (cfg-double :progression.exp-per-distance)

                                       distance))

                (skill-effects/set-main-cooldown! player-id mark-teleport-skill-id (cooldown-ticks exp))

                (log/debug "MarkTeleport: Teleported" (int distance) "blocks")))))))))






(def ^:private release-cast-ops
  (release-cast/build-ops
    {:initial-state {:hold-ticks 0 :has-target false}
     :require-cost-on-down? false
     :down! mark-teleport-on-key-down-impl!
     :abort-log-label "MarkTeleport aborted"
     :tick! mark-teleport-on-key-tick-impl!
     :up! mark-teleport-on-key-up-impl!
     :abort! mark-teleport-on-key-abort-impl!}))

(defn mark-teleport-on-key-down [& args] (apply release-cast/down! release-cast-ops args))
(defn mark-teleport-on-key-tick [& args] (apply release-cast/tick! release-cast-ops args))
(defn mark-teleport-on-key-up [& args] (apply release-cast/up! release-cast-ops args))
(defn mark-teleport-on-key-abort [& args] (apply release-cast/abort! release-cast-ops args))

(defskill mark-teleport-skill

  :id :mark-teleport

  :category-id :teleporter

  :name-key "ability.skill.teleporter.mark_teleport"

  :description-key "ability.skill.teleporter.mark_teleport.desc"

  :icon "textures/abilities/teleporter/skills/mark_teleport.png"

  :ui-position [70 16]



  :ctrl-id :mark-teleport

  :cp-consume-speed 0.0

  :overload-consume-speed 0.0

  :cooldown-ticks (fn [_player-id _skill-id exp]

                    (cooldown-ticks (double (or exp 0.0))))

  :pattern :release-cast

  :cooldown {:mode :manual}

  :cost {:up {:cp mark-teleport-cost-up-cp

              :overload mark-teleport-cost-up-overload

              :creative? mark-teleport-cost-creative?}}

  :actions {:down! mark-teleport-on-key-down

            :tick! mark-teleport-on-key-tick

            :up! mark-teleport-on-key-up

            :abort! mark-teleport-on-key-abort}

  :fx {:start {:topic :mark-teleport/fx-start

              :payload (fn [_] {})}

       :update {:topic :mark-teleport/fx-update

               :payload mark-teleport-fx-update-payload}

       :end {:topic :mark-teleport/fx-end

             :payload (fn [_] {})}}

  :prerequisites [{:skill-id :threatening-teleport :min-exp 0.4}])

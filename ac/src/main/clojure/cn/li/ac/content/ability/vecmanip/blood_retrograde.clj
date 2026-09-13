(ns cn.li.ac.content.ability.vecmanip.blood-retrograde
  "BloodRetrograde skill port aligned to original AcademyCraft Blood Retrograde.

  Key alignment points:
  - Hold to charge up to 30 ticks; auto-release at max charge
  - Only living-entity hits within 2 blocks can perform the skill
  - Local walk speed slows from 0.1 to 0.007 over the first 20 ticks
  - Release consumes overload lerp(55,40) and CP lerp(280,350)
  - Damage lerp(30,60), cooldown lerp(90,40), exp gain 0.002
  - Successful hit plays blood splash / blood spray visuals and sound

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.ac.ability.registry.event :as ability-event]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.effects.geom :as geom]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.platform.world-effects :as world-effects]
            [cn.li.mcmod.platform.entity-damage :as entity-damage]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :blood-retrograde)
(def ^:private blood-retrograde-skill-id :blood-retrograde)

(def ^:private world-up {:x 0.0 :y 1.0 :z 0.0})

(defn- get-player-look [player-id]
  (or (when (raycast/available?)
        (some-> (raycast/player-look-vector player-id)
                geom/vnorm))
      {:x 0.0 :y 0.0 :z 1.0}))

(defn- get-target-info [world-id target-id fallback-hit]
  (let [fallback {:uuid target-id
                  :x (double (or (:x fallback-hit) 0.0))
                  :y (double (or (:y fallback-hit) 0.0))
                  :z (double (or (:z fallback-hit) 0.0))
                  :width (cfg-double :targeting.fallback-width)
                  :height (cfg-double :targeting.fallback-height)
                  :eye-height (cfg-double :targeting.fallback-eye-height)}]
    (or (when (and (world-effects/available?) target-id)
          (some (fn [{:keys [uuid] :as entity}]
                  (when (= uuid target-id)
                    (merge fallback entity)))
                (world-effects/find-entities-in-radius
                                                       world-id
                                                       (:x fallback)
                                                       (:y fallback)
                                                       (:z fallback)
                                                       (cfg-double :targeting.entity-search-radius))))
        fallback)))

(defn- overload-cost [exp]
  (cfg-lerp :cost.release.overload exp))

(defn- cp-cost [exp]
  (cfg-lerp :cost.release.cp exp))

(defn- damage-value [exp]
  (cfg-lerp :combat.damage exp))

(defn- cooldown-ticks [exp]
  (int (cfg-lerp :cooldown.ticks exp)))

(defn- update-skill-state-root!
  [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

(defn- spray-hit-payloads [player-id world-id target-info]
  (let [player-look (get-player-look player-id)
        base-look (let [horizontal {:x (:x player-look)
                                    :y 0.0
                                    :z (:z player-look)}]
                    (if (> (geom/vlen horizontal) 1.0e-5)
                      (geom/vnorm horizontal)
                      {:x 0.0 :y 0.0 :z 1.0}))
        head-pos {:x (:x target-info)
                  :y (+ (double (:y target-info)) (* (double (:height target-info)) 0.6))
                  :z (:z target-info)}]
    (->> (cfg-double-list :effect.spray-angles)
         (mapcat (fn [pitch-deg]
                   (let [yaw-jitter (- (* (rand) 40.0) 20.0)
                         yaw-turned (geom/rotate-around-axis base-look world-up yaw-jitter)
                         right-axis (let [raw (geom/vcross yaw-turned world-up)]
                                      (if (> (geom/vlen raw) 1.0e-5)
                                        (geom/vnorm raw)
                                        {:x 1.0 :y 0.0 :z 0.0}))
                         dir (geom/rotate-around-axis yaw-turned right-axis pitch-deg)
                         start (geom/v- head-pos (geom/v* dir 0.5))
                         hit (when (raycast/available?)
                               (raycast/raycast-blocks
                                                      world-id
                                                      (:x start) (:y start) (:z start)
                                                      (:x dir) (:y dir) (:z dir)
                                                      5.5))]
                     (when (map? hit)
                       (repeatedly 2
                                   (fn []
                                     {:x (double (or (:x hit) 0.0))
                                      :y (double (or (:y hit) 0.0))
                                      :z (double (or (:z hit) 0.0))
                                      :face (:face hit)
                                      :size (* (+ 1.1 (* (rand) 0.3))
                                               (if (contains? #{:up :down} (:face hit)) 1.0 0.8))
                                      :rotation (* (rand) 360.0)
                                      :offset-u (- (* (rand) 0.3) 0.15)
                                      :offset-v (- (* (rand) 0.3) 0.15)
                                      :texture-id (rand-int 3)}))))))
         (filter some?)
         vec)))

(defn- splash-payloads [look-dir target-info]
  (let [width (double (or (:width target-info) 0.6))
        height (double (or (:height target-info) 1.8))]
    (vec
      (repeatedly (+ 6 (rand-int 4))
                  (fn []
                    (let [dv {:x (* (- (* (rand) 2.0) 1.0) width)
                              :y (* (rand) height)
                              :z (* (- (* (rand) 2.0) 1.0) width)}
                          pos (geom/v+ (geom/v+ {:x (:x target-info)
                                                  :y (:y target-info)
                                                  :z (:z target-info)}
                                                dv)
                                       (geom/v* look-dir 0.2))]
                      {:x (:x pos)
                       :y (:y pos)
                       :z (:z pos)
                       :size (+ 1.4 (* (rand) 0.4))}))))))

(defn- send-fx-perform! [ctx-id player-id world-id target-id hit]
  (let [target-info (get-target-info world-id target-id hit)
        look-dir (get-player-look player-id)
        player-pos (or (when (raycast/available?)
                         (raycast/player-position player-id))
                       target-info)]
    ;; Original's s_perform sendToClient(MSG_PERFORM, targ) — c_perform spawns
    ;; the blood-splash particles unconditionally for owner + nearby; only the
    ;; caster's own walk-speed slowdown (no fx counterpart here) is isLocal.
    (fx/send-local-and-nearby! ctx-id {:topic :blood-retrograde/fx-perform :mode :perform} nil
              {:sound-pos {:x (:x player-pos)
                           :y (:y player-pos)
                           :z (:z player-pos)}
               :splashes (splash-payloads look-dir target-info)
               :sprays (spray-hit-payloads player-id world-id target-info)})))

(defn- finish! [ctx-id performed?]
  (update-skill-state-root! ctx-id
                            (fn [skill-state]
                              (assoc (or skill-state {})
                                     :executed? true
                                     :ended? true
                                     :performed? (boolean performed?))))
  (fx/send! ctx-id {:topic :blood-retrograde/fx-end :mode :end} nil {:performed? (boolean performed?)})
  (ctx/terminate-context! ctx-id nil))

(defn- scaled-damage
  [player-id target-id raw-damage]
  (skill-effects/scale-damage
   (skill-registry/get-skill blood-retrograde-skill-id)
   (ability-event/fire-calc-event!
    ability-event/CALC-SKILL-ATTACK
    raw-damage
    {:player-id player-id
     :target-id target-id
     :skill-id blood-retrograde-skill-id})))

(defn- try-perform! [player-id ctx-id hit]
  (let [target-id (:entity-id hit)
        world-id (geom/world-id-of player-id)
        exp (skill-exp player-id)]
    (when target-id
      (let [resource-result
            (skill-effects/perform-resource!
             player-id
             (overload-cost exp)
             (cp-cost exp)
             false)]
        (if (:success? resource-result)
          (do
            (skill-effects/set-main-cooldown!
             player-id :blood-retrograde (cooldown-ticks exp))
            (send-fx-perform! ctx-id player-id world-id target-id hit)
            (when (entity-damage/available?)
              (entity-damage/apply-direct-damage!
               world-id
               target-id
               (scaled-damage player-id target-id (damage-value exp))
               :skill
               {:attacker-uuid player-id}))
            (skill-effects/add-skill-exp!
             player-id
             blood-retrograde-skill-id
             (cfg-double :progression.exp-hit))
            true)
          false)))))

(defn blood-retrograde-on-key-down
  "Initialize charge state and local charge slowdown."
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (ctx-skill/replace-skill-state! ctx-id
                         {:ticks 0
                          :executed? false
                          :ended? false})
  (fx/send! ctx-id {:topic :blood-retrograde/fx-start :mode :start})
  (fx/send! ctx-id {:topic :blood-retrograde/fx-update :mode :update} nil {:ticks 0 :charge-ratio 0.0})
  (log/debug "BloodRetrograde charge started"))

(defn blood-retrograde-on-key-tick
  "Update charge progress and auto-release at max charge."
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx-data)
          executed? (boolean (:executed? skill-state false))]
      (when-not executed?
        (let [ticks (inc (long (or (:ticks skill-state) 0)))]
          (update-skill-state-root! ctx-id #(assoc % :ticks ticks))
          (fx/send! ctx-id {:topic :blood-retrograde/fx-update :mode :update} nil
                    {:ticks (long ticks)
                     :charge-ratio (scaling/clamp-exp (/ (double ticks) (cfg-double :charge.fx-ratio-ticks)))})
          (when (>= ticks (cfg-int :charge.max-ticks))
            (let [hit (when (raycast/available?)
                        (raycast/raycast-from-player player-id (cfg-double :targeting.distance) true))
                  performed? (boolean (and hit (try-perform! player-id ctx-id hit)))]
              (finish! ctx-id performed?)
              (log/debug "BloodRetrograde auto-release" "ticks" ticks "performed" performed?))))))))

(defn blood-retrograde-on-key-up
  "Execute the skill on release if a valid target is found."
  [ctx-id player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [skill-state (:skill-state ctx-data)
          executed? (boolean (:executed? skill-state false))]
      (when-not executed?
        (let [hit (when (raycast/available?)
              (raycast/raycast-from-player player-id (cfg-double :targeting.distance) true))
              performed? (boolean (and hit (try-perform! player-id ctx-id hit)))]
          (finish! ctx-id performed?)
          (log/debug "BloodRetrograde released" "performed" performed?))))))

(defn blood-retrograde-on-key-abort
  "Clean up charge state on abort."
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (when-not (get-in ctx-data [:skill-state :ended?])
      (fx/send! ctx-id {:topic :blood-retrograde/fx-end :mode :end} nil {:performed? false})))
  (ctx-skill/clear-skill-state! ctx-id)
  (log/debug "BloodRetrograde aborted"))

(defskill blood-retrograde
  :id :blood-retrograde
  :category-id :vecmanip
  :name-key "ability.skill.vecmanip.blood_retrograde"
  :description-key "ability.skill.vecmanip.blood_retrograde.desc"
  :icon "textures/abilities/vecmanip/skills/blood_retro.png"
  :ui-position [204 83]
  :ctrl-id :blood-retrograde
  :cp-consume-speed 0.0
  :overload-consume-speed 0.0
  :cooldown-ticks (fn [_player-id _skill-id exp]
                    (int (cfg-lerp :cooldown.ticks (double (or exp 0.0)))))
  :pattern :release-cast
  :cooldown {:mode :manual}
  :actions {:down! blood-retrograde-on-key-down
            :tick! blood-retrograde-on-key-tick
            :up! blood-retrograde-on-key-up
            :abort! blood-retrograde-on-key-abort}
  :prerequisites [{:skill-id :directed-blastwave :min-exp 0.0}])


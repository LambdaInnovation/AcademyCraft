(ns cn.li.ac.content.ability.vecmanip.vec-accel
  "VecAccel - dash/acceleration in look direction.

  Pattern: :hold-charge-release (max 20 ticks)
  Cost on up: CP lerp(120,80), overload lerp(30,15) by exp
  Cooldown: lerp(80,50) ticks (manual)
  Exp: +0.002 per use"
  (:require [cn.li.ac.ability.util.scaling :as scaling]
            [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.ability.service.context-skill-state :as ctx-skill]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.effects.motion :as motion-effects]
            [cn.li.ac.ability.fx :as fx]
            [cn.li.mcmod.platform.raycast :as raycast]
            [cn.li.mcmod.util.log :as log]))

(def-skill-config-ops :vec-accel)
(def ^:private vec-accel-skill-id :vec-accel)

(defn- calculate-speed [charge-ticks]
  (let [max-charge (cfg-int :charge.max-ticks)
        progress (scaling/clamp-exp (/ (double charge-ticks) (double max-charge)))
        prog (skill-config/lerp-double vec-accel-skill-id :movement.speed-progress progress)]
    (* (Math/sin prog) (cfg-double :movement.max-velocity))))

(defn- check-ground-raycast [player-id]
  (when (raycast/available?)
    (when-let [pos (raycast/player-position player-id)]
      (let [world-id (or (:world-id pos) "minecraft:overworld")]
        (some? (raycast/raycast-blocks
                                       world-id
                                       (double (:x pos)) (double (:y pos)) (double (:z pos))
                                       0 -1 0 (cfg-double :targeting.ground-check-distance)))))))

(defn- compute-init-vel [look-dir charge-ticks]
  (let [look-x   (double (:x look-dir))
        look-y   (double (:y look-dir))
        look-z   (double (:z look-dir))
        horiz-len (Math/sqrt (+ (* look-x look-x) (* look-z look-z)))
        safe-h    (if (pos? horiz-len) horiz-len 1.0)
        cur-pitch (Math/atan2 (- look-y) safe-h)
        new-pitch (+ cur-pitch (cfg-double :movement.pitch-offset-radians))
        cos-p     (Math/cos new-pitch)
        sin-p     (Math/sin new-pitch)
        hx        (/ look-x safe-h)
        hz        (/ look-z safe-h)
        speed     (calculate-speed charge-ticks)]
    {:x (* cos-p hx speed)
     :y (- (* sin-p speed))
     :z (* cos-p hz speed)}))

(defn- set-skill-state!
  [ctx-id k v]
  (ctx-skill/assoc-skill-state! ctx-id k v))

(defn- update-skill-state-root!
  [ctx-id f & args]
  (apply ctx-skill/update-skill-state-root! ctx-id f args))

(defn- vec-accel-down!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  ;; The original client context starts performable and only refreshes its
  ;; ground check on the first tick.
  (ctx-skill/replace-skill-state!
   ctx-id
   {:charge-ticks 0
    :can-perform? true
    :look-dir nil
    :init-vel nil
    :performed? false})
  (fx/send! ctx-id {:topic :vec-accel/fx-start :mode :start}))

(defn- vec-accel-tick!
  [ctx-id player-id _skill-id exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (let [charge-ticks (inc (long (or (get-in (ctx-skill/get-context ctx-id)
                                             [:skill-state :charge-ticks])
                                    0)))
        can-perform? (boolean (or (> (double (or exp 0.0)) (cfg-double :targeting.groundless-exp-threshold))
                                  (check-ground-raycast player-id)))
        look-dir     (when (raycast/available?)
                       (raycast/player-look-vector player-id))
        init-vel     (when look-dir (compute-init-vel look-dir charge-ticks))]
    (update-skill-state-root! ctx-id merge
                              {:charge-ticks charge-ticks
                               :can-perform? can-perform?
                               :look-dir     look-dir
                               :init-vel     init-vel
                               :performed?   false})
    (fx/send! ctx-id {:topic :vec-accel/fx-update :mode :update} nil
              {:charge-ticks charge-ticks
               :can-perform? can-perform?
               :look-dir (or look-dir {:x 0.0 :y 0.0 :z 1.0})
               :init-vel (or init-vel {:x 0.0 :y 0.0 :z 1.0})})))

(defn- vec-accel-perform!
  [ctx-id player-id _skill-id exp cost-ok? _hold-ticks _cost-stage _player-ref]
  (when-let [ctx-data (ctx-skill/get-context ctx-id)]
    (let [ss           (:skill-state ctx-data)
          can-perform? (boolean (:can-perform? ss))
          init-vel     (:init-vel ss)
          exp          (double (or exp 0.0))]
      (if (and cost-ok? can-perform? init-vel)
        (let [{:keys [x y z]} init-vel]
          (when (motion-effects/player-motion-available?)
            (motion-effects/set-player-velocity! player-id x y z))
          (when (motion-effects/teleportation-available?)
            (motion-effects/reset-fall-damage! player-id))
          (skill-effects/set-main-cooldown! player-id :vec-accel
                                          (int (cfg-lerp :cooldown.ticks exp)))
          (skill-effects/add-skill-exp! player-id :vec-accel (cfg-double :progression.exp-use))
          (update-skill-state-root! ctx-id merge
                                    {:performed? true :final-vel {:x x :y y :z z}})
          (fx/send! ctx-id {:topic :vec-accel/fx-perform :mode :perform})
          (fx/send! ctx-id {:topic :vec-accel/fx-end :mode :end} nil {:performed? true})
          (log/debug "VecAccel launched" x y z))
        (do
          (set-skill-state! ctx-id :performed? false)
          (fx/send! ctx-id {:topic :vec-accel/fx-end :mode :end} nil {:performed? false}))))))

(defn- vec-accel-abort!
  [ctx-id _player-id _skill-id _exp _cost-ok? _hold-ticks _cost-stage _player-ref]
  (set-skill-state! ctx-id :performed? false)
  (fx/send! ctx-id {:topic :vec-accel/fx-end :mode :end} nil {:performed? false}))

(defskill vec-accel
  :id          :vec-accel
  :category-id :vecmanip
  :name-key    "ability.skill.vecmanip.vec_accel"
  :description-key "ability.skill.vecmanip.vec_accel.desc"
  :icon        "textures/abilities/vecmanip/skills/vec_accel.png"
  :ui-position [76 40]
  :ctrl-id     :vec-accel
  :pattern     :release-cast
  :cooldown    {:mode :manual}
  :state       {:max-charge (fn [_] (cfg-int :charge.max-ticks))}
  :cost        {:up {:cp       (fn [_player-id _skill-id exp]
                                 (cfg-lerp :cost.up.cp (double (or exp 0.0))))
                     :overload (fn [_player-id _skill-id exp]
                                 (cfg-lerp :cost.up.overload (double (or exp 0.0))))}}
  :actions
  {:down!    vec-accel-down!
   :tick!    vec-accel-tick!
   :up!      vec-accel-perform!
   :abort!   vec-accel-abort!}
  :fx {:start   {:topic :vec-accel/fx-start   :payload (fn [_] {})}
       :update  {:topic :vec-accel/fx-update
                 :payload (fn [{:keys [ctx-id charge-ticks]}]
                            (let [st (:skill-state (ctx-skill/get-context ctx-id))]
                              {:charge-ticks (long (max 0 (or charge-ticks 0)))
                               :can-perform? (boolean (:can-perform? st))
                               :look-dir     (or (:look-dir st) {:x 0.0 :y 0.0 :z 1.0})
                               :init-vel     (or (:init-vel st) {:x 0.0 :y 0.0 :z 1.0})}))}
       :perform {:topic :vec-accel/fx-perform  :payload (fn [_] {})}
       :end     {:topic :vec-accel/fx-end
                 :payload (fn [{:keys [ctx-id]}]
                            {:performed? (boolean (get-in (ctx-skill/get-context ctx-id) [:skill-state :performed?]))})}}
  :prerequisites [{:skill-id :directed-shock :min-exp 0.0}])

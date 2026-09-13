(ns cn.li.ac.content.ability.meltdowner.mine-ray-basic
  "MineRayBasic - iron-tier mining beam, short range.

  Pattern: :hold-channel
  Range: 10 (flat, matching original — not exp-scaled)
  Break speed: lerp(0.2, 0.4, exp) per tick
  Tick cost: CP lerp(12, 7, exp)
  Down cost: overload lerp(200, 150, exp)
  Cooldown: lerp(40, 20, exp) ticks, applied on every termination path
  Exp: +0.0005 per block broken
  Tool tier: blocked from blocks requiring a diamond-tier tool (matches
  original's harvestLevel=2 cap)

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------


(def-skill-config-ops :mine-ray-basic)
(def ^:private mine-ray-basic-skill-id :mine-ray-basic)

(defn- cooldown-ticks-for [exp]
  (skill-config/lerp-int mine-ray-basic-skill-id :cooldown.ticks (double (or exp 0.0))))

(defn- make-cfg [player-id]
  (let [exp (skill-exp player-id)]
    {:range             (cfg-double :targeting.range)
     :break-speed       (cfg-lerp :mining.break-speed exp)
     :skill-id          mine-ray-basic-skill-id
     :exp-block         (cfg-double :progression.exp-block)
     :tool-tier-capped? true
     :cooldown-ticks    (cooldown-ticks-for exp)}))

;; ---------------------------------------------------------------------------
;; Actions
;; ---------------------------------------------------------------------------

(defn mine-ray-basic-down!      [& args] (apply base/mining-ray-down!      mine-ray-basic-skill-id args))
(defn mine-ray-basic-tick!      [& args] (apply base/mining-ray-tick!      (make-cfg (nth args 1)) args))
(defn mine-ray-basic-up!        [& args] (apply base/mining-ray-up!        (make-cfg (nth args 1)) args))
(defn mine-ray-basic-abort!     [& args] (apply base/mining-ray-abort!     (make-cfg (nth args 1)) args))
(defn mine-ray-basic-cost-fail! [& args] (apply base/mining-ray-cost-fail! (make-cfg (nth args 1)) args))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill mine-ray-basic
  :id             :mine-ray-basic
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.mine_ray_basic"
  :description-key "ability.skill.meltdowner.mine_ray_basic.desc"
  :icon           "textures/abilities/meltdowner/skills/mine_ray_basic.png"
  :ui-position    [140 70]
  :ctrl-id        :mine-ray-basic
  :pattern        :hold-channel
  :cooldown       {:mode :manual}
  :cost           {:down {:overload (fn [{:keys [player-id]}]
                (cfg-lerp :cost.down.overload (skill-exp player-id)))}
                   :tick {:cp (fn [{:keys [player-id]}]
              (cfg-lerp :cost.tick.cp (skill-exp player-id)))} }
  :cooldown-ticks (fn [{:keys [exp]}] (cooldown-ticks-for exp))  ;; matching original lerp(40,20,exp)
  :actions        {:down!      mine-ray-basic-down!
                   :tick!      mine-ray-basic-tick!
                   :up!        mine-ray-basic-up!
                   :abort!     mine-ray-basic-abort!
                   :cost-fail! mine-ray-basic-cost-fail!}
  :fx             {:start  {:topic :mine-ray/fx-start  :payload (fn [_] {:variant :basic})}
                   :end    {:topic :mine-ray/fx-end    :payload (fn [_] {})}}
  :prerequisites  [{:skill-id :meltdowner :min-exp 0.3}])

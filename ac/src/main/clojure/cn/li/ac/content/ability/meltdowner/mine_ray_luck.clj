(ns cn.li.ac.content.ability.meltdowner.mine-ray-luck
  "MineRayLuck - fortune-enhanced mining beam.

  Pattern: :hold-channel
  Range: 20 (flat, matching original — not exp-scaled)
  Break speed: lerp(0.5, 1.0, exp)
  Fortune effect: drops extra items via block manipulation fortune parameter
  Tick cost: CP lerp(50, 35, exp)
  Down cost: overload lerp(350, 300, exp)
  Cooldown: lerp(60, 30, exp) ticks, applied on every termination path
  Exp: +0.0003 per block broken
  Tool tier: uncapped (matches original's harvestLevel=5)

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------


(def-skill-config-ops :mine-ray-luck)
(def ^:private mine-ray-luck-skill-id :mine-ray-luck)
(def ^:private mine-ray-luck-fortune-level 3)

(defn- cooldown-ticks-for [exp]
  (skill-config/lerp-int mine-ray-luck-skill-id :cooldown.ticks (double (or exp 0.0))))

(defn- make-cfg [player-id]
  (let [exp (skill-exp player-id)]
    {:range          (cfg-double :targeting.range)
     :break-speed    (cfg-lerp :mining.break-speed exp)
     :skill-id       mine-ray-luck-skill-id
     :fortune-level  mine-ray-luck-fortune-level
     :tool-tier-capped? false
     :exp-block      (skill-config/tunable-double mine-ray-luck-skill-id :progression.exp-block)
     :cooldown-ticks (cooldown-ticks-for exp)}))

;; ---------------------------------------------------------------------------
;; Actions
;; ---------------------------------------------------------------------------

(defn mine-ray-luck-down!      [& args] (apply base/mining-ray-down!      mine-ray-luck-skill-id args))
(defn mine-ray-luck-tick!      [& args] (apply base/mining-ray-tick!      (make-cfg (nth args 1)) args))
(defn mine-ray-luck-up!        [& args] (apply base/mining-ray-up!        (make-cfg (nth args 1)) args))
(defn mine-ray-luck-abort!     [& args] (apply base/mining-ray-abort!     (make-cfg (nth args 1)) args))
(defn mine-ray-luck-cost-fail! [& args] (apply base/mining-ray-cost-fail! (make-cfg (nth args 1)) args))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill mine-ray-luck
  :id             :mine-ray-luck
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.mine_ray_luck"
  :description-key "ability.skill.meltdowner.mine_ray_luck.desc"
  :icon           "textures/abilities/meltdowner/skills/mine_ray_luck.png"
  :ui-position    [205 82]
  :ctrl-id        :mine-ray-luck
  :pattern        :hold-channel
  :cooldown       {:mode :manual}
  :cost           {:down {:overload (fn [{:keys [player-id]}]
                (cfg-lerp :cost.down.overload (skill-exp player-id)))}
                   :tick {:cp (fn [{:keys [player-id]}]
              (cfg-lerp :cost.tick.cp (skill-exp player-id)))} }
  :cooldown-ticks (fn [{:keys [exp]}] (cooldown-ticks-for exp))  ;; matching original lerp(60,30,exp)
  :actions        {:down!      mine-ray-luck-down!
                   :tick!      mine-ray-luck-tick!
                   :up!        mine-ray-luck-up!
                   :abort!     mine-ray-luck-abort!
                   :cost-fail! mine-ray-luck-cost-fail!}
  :fx             {:start  {:topic :mine-ray/fx-start  :payload (fn [_] {:variant :luck})}
                   :end    {:topic :mine-ray/fx-end    :payload (fn [_] {})}}
  :prerequisites  [{:skill-id :mine-ray-expert :min-exp 1.0}])

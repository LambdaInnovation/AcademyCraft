(ns cn.li.ac.content.ability.meltdowner.mine-ray-expert
  "MineRayExpert - all-tier mining beam, extended range.

  Pattern: :hold-channel
  Range: 20 (flat, matching original — not exp-scaled)
  Break speed: lerp(0.5, 1.0, exp) per tick
  Tick cost: CP lerp(25, 15, exp)
  Down cost: overload lerp(300, 200, exp)
  Cooldown: lerp(60, 30, exp) ticks, applied on every termination path
  Exp: +0.0003 per block broken
  Tool tier: uncapped (matches original's harvestLevel=5 — above any real
  vanilla tool tier, so effectively unrestricted)

  No Minecraft imports."
  (:require [cn.li.ac.ability.dsl :refer [defskill def-skill-config-ops]]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.ability.service.context-dispatcher :as ctx]
            [cn.li.ac.content.ability.meltdowner.mine-rays-base :as base]
            [cn.li.mcmod.util.log :as log]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------


(def-skill-config-ops :mine-ray-expert)
(def ^:private mine-ray-expert-skill-id :mine-ray-expert)

(defn- cooldown-ticks-for [exp]
  (skill-config/lerp-int mine-ray-expert-skill-id :cooldown.ticks (double (or exp 0.0))))

(defn- make-cfg [player-id]
  (let [exp (skill-exp player-id)]
    {:range          (cfg-double :targeting.range)
     :break-speed    (cfg-lerp :mining.break-speed exp)
     :skill-id       mine-ray-expert-skill-id
     :exp-block      (cfg-double :progression.exp-block)
     ;; Original harvestLevel=5 is effectively uncapped.
     :tool-tier-capped? false
     :fortune-level  0
     :cooldown-ticks (cooldown-ticks-for exp)}))

;; ---------------------------------------------------------------------------
;; Actions
;; ---------------------------------------------------------------------------

(defn mine-ray-expert-down!      [& args] (apply base/mining-ray-down!      mine-ray-expert-skill-id args))
(defn mine-ray-expert-tick!      [& args] (apply base/mining-ray-tick!      (make-cfg (nth args 1)) args))
(defn mine-ray-expert-up!        [& args] (apply base/mining-ray-up!        (make-cfg (nth args 1)) args))
(defn mine-ray-expert-abort!     [& args] (apply base/mining-ray-abort!     (make-cfg (nth args 1)) args))
(defn mine-ray-expert-cost-fail! [& args] (apply base/mining-ray-cost-fail! (make-cfg (nth args 1)) args))

;; ---------------------------------------------------------------------------
;; Skill registration
;; ---------------------------------------------------------------------------

(defskill mine-ray-expert
  :id             :mine-ray-expert
  :category-id    :meltdowner
  :name-key       "ability.skill.meltdowner.mine_ray_expert"
  :description-key "ability.skill.meltdowner.mine_ray_expert.desc"
  :icon           "textures/abilities/meltdowner/skills/mine_ray_expert.png"
  :ui-position    [172 70]
  :ctrl-id        :mine-ray-expert
  :pattern        :hold-channel
  :cooldown       {:mode :manual}
  :cost           {:down {:overload (fn [{:keys [player-id]}]
                (cfg-lerp :cost.down.overload (skill-exp player-id)))}
                   :tick {:cp (fn [{:keys [player-id]}]
              (cfg-lerp :cost.tick.cp (skill-exp player-id)))} }
  :cooldown-ticks (fn [{:keys [exp]}] (cooldown-ticks-for exp))  ;; matching original lerp(60,30,exp)
  :actions        {:down!      mine-ray-expert-down!
                   :tick!      mine-ray-expert-tick!
                   :up!        mine-ray-expert-up!
                   :abort!     mine-ray-expert-abort!
                   :cost-fail! mine-ray-expert-cost-fail!}
  :fx             {:start  {:topic :mine-ray/fx-start  :payload (fn [_] {:variant :expert})}
                   :end    {:topic :mine-ray/fx-end    :payload (fn [_] {})}}
  :prerequisites  [{:skill-id :mine-ray-basic :min-exp 0.8}])

(ns cn.li.ac.content.ability.meltdowner.rad-intensify-test
  (:require [clojure.test :refer [deftest is testing]]
            [cn.li.ac.ability.config :as cfg]
            [cn.li.ac.ability.skill-config :as skill-config]
            [cn.li.ac.ability.service.skill-effects :as skill-effects]
            [cn.li.ac.content.ability.meltdowner.rad-intensify :as rad]))

(deftest skill-exp-uses-init-cp-only-not-init-plus-add-test
  ;; Matches original's getSkillExp: clamp(0,1, maxCP / getInitCP(5)).
  ;; getInitCP(5) reads only the init_cp list (8000 by default) — NOT
  ;; init_cp + add_cp (20000), which is a different concept (achievable
  ;; ceiling including skill-usage bonuses).
  (testing "denominator is get-init-cp, unaffected by add-cp"
    (with-redefs [skill-effects/player-path (fn [_player-id path _default]
                                              (when (= path [:resource-data :max-cp]) 4000.0))
                  cfg/get-init-cp (fn [level] (when (= level 5) 8000.0))
                  cfg/get-add-cp (fn [_level] (throw (ex-info "add-cp must not be read" {})))]
      (is (= 0.5 (rad/skill-exp "p1"))))))

(deftest skill-exp-clamps-to-0-1-test
  (with-redefs [skill-effects/player-path (fn [_player-id path _default]
                                            (when (= path [:resource-data :max-cp]) 20000.0))
                cfg/get-init-cp (fn [level] (when (= level 5) 8000.0))]
    (is (= 1.0 (rad/skill-exp "p1")))))

(deftest rate-lerps-from-1_4-to-1_8-test
  (with-redefs [skill-effects/player-path (fn [_player-id path _default]
                                            (when (= path [:resource-data :max-cp]) 8000.0))
                cfg/get-init-cp (fn [_level] 8000.0)
                skill-config/tunable-double-list (fn [_skill-id field-id]
                                                    (case field-id
                                                      :combat.damage-rate [1.4 1.8]
                                                      [0.0 0.0]))]
    (is (= 1.8 (rad/rate "p1")))))

(deftest mark-duration-ticks-defaults-to-60-test
  (with-redefs [skill-config/tunable-int (fn [_skill-id field-id]
                                           (case field-id
                                             :effect.mark-duration-ticks 60
                                             0))]
    (is (= 60 (rad/mark-duration-ticks)))))

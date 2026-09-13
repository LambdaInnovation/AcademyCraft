(ns cn.li.ac.ability.client.hud-test
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.combat-notice :as combat-notice]
            [cn.li.ac.ability.client.delegate-state :as dstate]
            [cn.li.ac.ability.client.hud :as hud]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.model.cooldown :as cd-data]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.test.support.hud-render-data :as hud-rd]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(deftest build-hud-render-data-includes-combat-notice-when-inactive-test
  (let [component (combat-notice/create-combat-notice-component)
        session-id :test-session]
    (combat-notice/show-notice!
     component
     session-id
     :teleporter-crit
     {:text "Critical Hit x1.6"
      :duration-ms 2000
      :color [255 226 120]})
    (runtime-hooks/with-client-ctx-fn {:session-id session-id} (fn [] (let [render-data (hud-rd/build-hud-render-data {:activated false
                                                    :cp {:cur 0.0 :max 1.0}
                                                    :overload {:cur 0.0 :max 1.0 :fine true}
                                                    :active-slots []}
                                                   320 180 {}
                                                   :combat-notice-component component
                                                   :now-ms (System/currentTimeMillis))]
        (is (some? render-data))
        (is (= "Critical Hit x1.6" (get-in render-data [:combat-notice :text]))))))))

(deftest cp-bar-render-data-uses-texture-without-solid-bar-color-test
  (let [data (hud/build-cp-bar-render-data
              {:cp {:cur 100.0 :max 100.0}})]
    (is (= "academy:textures/guis/cpbar/cp.png" (:fg-texture data)))
    (is (nil? (:bar-color data)))))

(deftest cpbar-layout-matches-upstream-scaled-coordinates-test
  (let [layout (hud/cpbar-layout 320)
        hint-box (hud/cpbar-activation-hint-box 320 44.0)]
    (is (< (Math/abs (- 115.2 (get-in layout [:frame :x]))) 1.0e-9))
    (is (= 12.0 (get-in layout [:frame :y])))
    (is (= 192.8 (get-in layout [:frame :w])))
    (is (< (Math/abs (- 124.6 (get-in layout [:cp-lane :x]))) 1.0e-9))
    (is (= {:x-offset 162.0 :y-offset 2.6 :w 13.0 :h 13.0}
           (:category-icon layout)))
    (is (= 8.0 (get-in layout [:cp-numbers :font-size])))
    (is (< (Math/abs (- 231.2 (get-in layout [:preset-row :x]))) 1.0e-9))
    (is (< (Math/abs (- 39.2 (get-in layout [:preset-row :y]))) 1.0e-9))
    (is (< (Math/abs (- 169.6 (:x hint-box))) 1.0e-9))
    (is (< (Math/abs (- 38.4 (:y hint-box))) 1.0e-9))
    (is (< (Math/abs (- 47.2 (:w hint-box))) 1.0e-9))
    (is (= 12.0 (:h hint-box)))
    (is (= 1.0 (:glow-size hint-box)))))

(deftest cp-bar-consumption-hint-reports-original-and-predicted-levels-test
  (let [data (hud/build-cp-bar-render-data
              {:cp {:cur 80.0 :max 100.0}
               :consumption-hint 25.0})]
    (is (= 0.8 (:percent data)))
    (is (= 0.55 (:hint-percent data)))))

(deftest numbers-readout-release-fades-for-original-300ms-test
  (let [model {:cp {:cur 80.0 :max 100.0}
               :overload {:cur 20.0 :max 100.0 :overloaded false}}
        halfway (hud/build-numbers-texts-data model false 1000 1150)]
    (is (= 76 (get-in halfway [0 :color :a])))
    (is (nil? (hud/build-numbers-texts-data model false 1000 1300)))))

(deftest overload-render-data-separates-cap-flash-from-recovery-tail-test
  (let [cap (hud/build-overload-bar-render-data
             {:overload {:cur 100.0 :max 100.0 :overloaded true :recovering true}}
             1000)
        tail (hud/build-overload-bar-render-data
              {:overload {:cur 60.0 :max 100.0 :overloaded false :recovering true}}
              1000)]
    (is (true? (:overloaded cap)))
    (is (true? (:recovering cap)))
    (is (= "academy:textures/guis/cpbar/back_overload.png" (:bg-texture cap)))
    (is (false? (:overloaded tail)))
    (is (true? (:recovering tail)))
    (is (= "academy:textures/guis/cpbar/back_normal.png" (:bg-texture tail)))))

(deftest build-skill-slot-render-data-resolves-skill-id-spec-and-icon-test
  (with-redefs [read-model/get-player-contexts-for-player (fn [& _] [])
                skill-query/get-skill-by-controllable (fn [_ _] :railgun)
                skill-registry/get-skill (fn [_] {:name "Railgun"})
                skill-query/get-skill-icon-path (fn [_] "textures/skills/railgun.png")
                cd-data/in-cooldown? (fn [_ _ _] false)
                cd-data/get-remaining (fn [_ _ _] 0)
                dstate/delegate-state-for-slot (fn [_ _ _ _] {:state :idle :alpha 1.0 :glow-color nil :sin-effect? false})]
    (let [slots (hud-rd/build-skill-slot-render-data
                 {:active-slots [[:electromaster :railgun]]}
                 320
                 180
                 {}
                 "p1")
          first-slot (first slots)]
      (is (= 1 (count slots)))
      (is (= "Railgun" (:skill-name first-slot)))
      (is (= "textures/skills/railgun.png" (:skill-icon first-slot))))))

(deftest cooldown-wipe-divides-by-the-applied-duration-test
  ;; Upstream KeyHintUI: prog = tickLeft / maxTick. The HUD used to recompute
  ;; the denominator from the skill spec, so the unavailable-wipe drifted from
  ;; the countdown whenever the cooldown that was actually applied differed
  ;; from that estimate.
  (let [shapes [{:skill-id :railgun :cooldown-total-spec 100}]
        patched (hud/patch-skill-slot-cooldown
                 shapes
                 {[:railgun :main] {:ticks 30 :max 60}}
                 {:player-id "p1" :skill-exps {}})
        slot (first patched)]
    (is (true? (:in-cooldown slot)))
    (is (= 30 (:cooldown-remaining slot)))
    (is (= 60 (:cooldown-total slot)) "denominator is the recorded duration, not the 100-tick estimate")
    (is (= 1.5 (:cooldown-seconds slot)))
    (is (= 0.5 (/ (double (:cooldown-remaining slot)) (double (:cooldown-total slot))))
        "wipe progress agrees with the 1.5s of a 3.0s cooldown left on the clock")))

(deftest idle-slot-falls-back-to-the-spec-estimate-test
  ;; Nothing is on cooldown, so there is no recorded duration to divide by —
  ;; the spec estimate stands in, and nothing draws the wipe anyway.
  (let [slot (first (hud/patch-skill-slot-cooldown
                     [{:skill-id :railgun :cooldown-total-spec 100}]
                     {}
                     {:player-id "p1" :skill-exps {}}))]
    (is (false? (:in-cooldown slot)))
    (is (= 0 (:cooldown-remaining slot)))
    (is (= 100 (:cooldown-total slot)))))

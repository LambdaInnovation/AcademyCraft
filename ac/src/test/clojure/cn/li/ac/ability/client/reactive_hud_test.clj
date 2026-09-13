(ns cn.li.ac.ability.client.reactive-hud-test
  "Covers the per-owner frame-input cache added to build-snapshot: contexts/
   hud-model/background-mask keyed on player-state identity, skill-slot-shape
   keyed independently on preset-data identity (mirrors the Cache A/Cache B
   split already proven in client-ui-hooks' now-superseded overlay-plan
   builder — see client_ui_hooks_overlay_cache_test.clj)."
  (:require [clojure.test :refer [deftest is]]
            [cn.li.ac.ability.client.reactive-hud :as reactive-hud]
            [cn.li.ac.ability.client.level-effects :as level-effects]
            [cn.li.ac.ability.client.read-model :as read-model]
            [cn.li.ac.ability.client.keybinds :as keybinds]
            [cn.li.ac.ability.registry.category :as category]
            [cn.li.ac.ability.registry.skill :as skill-registry]
            [cn.li.ac.ability.registry.skill-query :as skill-query]
            [cn.li.ac.client.toast :as toast]
            [cn.li.ac.content.ability.electromaster.current-charging-fx :as current-charging-fx]
            [cn.li.ac.tutorial.client.notification :as tutorial-notification]
            [cn.li.mcmod.client.platform-bridge :as bridge]
            [cn.li.mcmod.hooks.core :as runtime-hooks]))

(def ^:private test-session :reactive-hud-test-session)

(defn- preset-data-with-slot [ctrl-id]
  {:active-preset 0 :slots {[0 0] [:electromaster ctrl-id]}})

(defn- player-state-with [preset-data cooldown-data]
  {:resource-data {:activated true :cur-cp 80.0 :max-cp 100.0
                   :cur-overload 0.0 :max-overload 100.0 :overload-fine true}
   :ability-data {:category-id :electromaster}
   :cooldown-data cooldown-data
   :preset-data preset-data})

(defmacro with-snapshot-stubs
  [extra-bindings & body]
  `(with-redefs [keybinds/get-preset-switch-state (fn [_#] nil)
                 keybinds/get-activate-hint (fn [_#] nil)
                 current-charging-fx/current-state (fn [& _#] {:active? false :blending? false
                                                                :is-item false :good? false
                                                                :charge-ticks 0 :charge-ratio 0.0})
                 bridge/call-adapter (fn [& _#] nil)
                 bridge/game-time-ms (fn [] 1000)
                 toast/build-toast-layouts (fn [& _#] [])
                 tutorial-notification/build-notification-layout (fn [& _#] nil)
                 ~@extra-bindings]
     ~@body))

(defn- fresh! []
  (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/clear-snapshot-cache-for-owner!
      (read-model/owner-key {:player-uuid "p1"} nil)))))

(deftest frame-inputs-cache-hit-when-player-state-identical-test
  (fresh!)
  (let [context-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)
        state (player-state-with preset-data {})]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] state)
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] :railgun)
       skill-registry/get-skill (fn [_#] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _#] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
        (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
        (is (= 1 @context-calls)
            "contexts must be fetched once per real player-state change, not once per frame"))))))

(deftest frame-inputs-cache-invalidates-on-player-state-change-test
  (fresh!)
  (let [context-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)]
    (with-snapshot-stubs
      ;; a fresh map each call simulates a new sync delta (e.g. cp regen)
      ;; replacing player-state wholesale even though the content is the same
      [read-model/get-player-state (fn [_#] (player-state-with preset-data {}))
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] :railgun)
       skill-registry/get-skill (fn [_#] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _#] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
        (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
        (is (= 2 @context-calls)
            "a genuinely new player-state value must invalidate the cached contexts"))))))

(deftest skill-shape-cache-hits-across-resource-data-ticks-test
  (fresh!)
  (let [shape-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)
        cp (atom 80.0)]
    (with-snapshot-stubs
      ;; new player-state object each call (cp regen), same preset-data object
      [read-model/get-player-state (fn [_#] (assoc-in (player-state-with preset-data {})
                                                       [:resource-data :cur-cp] @cp))
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_#] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _#] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
        (reset! cp 81.0)
        (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
        (is (= 1 @shape-calls)
            "cp/overload regen ticking must not invalidate the skill-slot-shape cache"))))))

(deftest skill-shape-cache-invalidates-on-preset-rebind-test
  (fresh!)
  (let [shape-calls (atom 0)
        preset-a (atom (preset-data-with-slot :railgun))]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] (player-state-with @preset-a {}))
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# ctrl-id#] (swap! shape-calls inc) ctrl-id#)
       skill-registry/get-skill (fn [_#] {:name "Skill"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/x.png")
       read-model/get-player-contexts-for-player (fn [& _#] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
        (reset! preset-a (preset-data-with-slot :body-intensify))
        (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
        (is (= 2 @shape-calls)
            "rebinding a skill slot (new preset-data) must rebuild the cached shape"))))))

(deftest cooldown-refreshes-every-frame-without-invalidating-caches-test
  (fresh!)
  (let [shape-calls (atom 0)
        context-calls (atom 0)
        cooldown (atom {})
        preset-data (preset-data-with-slot :railgun)]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] (player-state-with preset-data @cooldown))
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_#] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _#] (swap! context-calls inc) [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (let [snap-1 (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
              slot-1 (first (:skill-slots snap-1))]
          (is (= false (:in-cooldown slot-1))))
        ;; cooldown-data is nested inside player-state, so this still counts as
        ;; a "new" player-state object per the coarser contexts/hud-model key —
        ;; but must never touch the narrower preset-data-keyed shape cache.
        (reset! cooldown {[:railgun :main] {:ticks 40 :max 60}})
        (let [snap-2 (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
              slot-2 (first (:skill-slots snap-2))]
          (is (= true (:in-cooldown slot-2))
              "cooldown numeric fields must refresh every frame"))
        (is (= 1 @shape-calls) "cooldown ticking must not invalidate the skill-slot-shape cache"))))))

(deftest clear-snapshot-cache-for-owner-clears-cache-test
  (fresh!)
  (let [shape-calls (atom 0)
        preset-data (preset-data-with-slot :railgun)]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] (player-state-with preset-data {}))
       category/get-category (fn [_#] {:color [1.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] (swap! shape-calls inc) :railgun)
       skill-registry/get-skill (fn [_#] {:name "Railgun"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/railgun.png")
       read-model/get-player-contexts-for-player (fn [& _#] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session} (fn [] (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
        (fresh!)
        (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1050})
        (is (= 2 @shape-calls)
            "clearing the cache must force a rebuild, not leak stale shapes"))))))

(deftest build-snapshot-vm-waves-from-fx-state-test
  (fresh!)
  (let [preset-data (preset-data-with-slot :vec-deviation)]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] (player-state-with preset-data {}))
       category/get-category (fn [_#] {:color [0.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] :vec-deviation)
       skill-registry/get-skill (fn [_#] {:name "VecDeviation"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/vec_deviation.png")
       read-model/get-player-contexts-for-player (fn [& _#] [])]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session}
        (fn []
          (with-redefs [level-effects/effect-state-snapshot
                        (fn [effect-id]
                          (case effect-id
                            :vec-deviation {:effect-state {[:ctx "ctx-dev"] {:active? true :ticks 3}}}
                            {}))]
            ;; a wave circle born now so it is alive at now-ms
            (reactive-hud/seed-vm-wave-state-for-test!
             "p1" [{:x 10.0 :y 10.0 :born-ms 0 :life-ms 60000
                    :start-size 100.0 :end-size 200.0 :seed 0.0}]
             0)
            (let [snap (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
                  waves (:vm-waves snap)]
              (is (seq waves) "vm-waves rendered from fx state alone")
              (is (some? (:tint (first waves))) "wave carries the deviation tint"))))))))

(deftest build-snapshot-skill-slot-active-from-fx-state-test
  (fresh!)
  (let [preset-data (preset-data-with-slot :vec-deviation)]
    (with-snapshot-stubs
      [read-model/get-player-state (fn [_#] (player-state-with preset-data {}))
       category/get-category (fn [_#] {:color [0.0 0.0 0.0] :icon "textures/x.png"})
       skill-query/get-skill-by-controllable (fn [_# _#] :vec-deviation)
       skill-registry/get-skill (fn [_#] {:name "VecDeviation"})
       skill-query/get-skill-icon-path (fn [_#] "textures/skills/vec_deviation.png")
       read-model/get-player-contexts-for-player (fn [& _#] [])
       level-effects/effect-state-snapshot
       (fn [effect-id]
         (case effect-id
           :vec-deviation {:effect-state {[:ctx "ctx-dev"] {:active? true :ticks 3}}}
           {}))]
      (runtime-hooks/with-client-ctx-fn {:session-id test-session}
        (fn []
          (let [snap (reactive-hud/build-snapshot "p1" 320 180 {:now-ms 1000})
                slot (first (:skill-slots snap))]
            (is (some? slot))
            (is (= :active (:visual-state slot))
                "skill slot lights :active from the fx signal in the real build-snapshot path")
            (is (some? (:glow-color slot)) "glow present")))))))
